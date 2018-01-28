/*
Copyright 2018 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package net.omam.zeroconf;

import static net.omam.zeroconf.MulticastDns.CLASS_IN;
import static net.omam.zeroconf.MulticastDns.TYPE_A;
import static net.omam.zeroconf.MulticastDns.TYPE_AAAA;
import static net.omam.zeroconf.MulticastDns.TYPE_SRV;
import static net.omam.zeroconf.MulticastDns.TYPE_TXT;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("javadoc")
final class ServiceImpl implements Service, ResponseListener {

    private static final Duration INIT_REQ_DELAY = Duration.ofMillis(200);

    private static final Logger LOGGER = Logger.getLogger(ServiceImpl.class.getName());

    private static final String UPDATED_TO = "] updated to ";

    private Optional<Attributes> attributes;

    private volatile boolean awaitingResponse;

    private Optional<String> hostname;

    private final String instanceName;

    private Optional<Inet4Address> ipv4Address;

    private Optional<Inet6Address> ipv6Address;

    private final Lock lock;

    private short port;

    private short priority;

    private final String registrationType;

    private final Condition responded;

    private short weight;

    ServiceImpl(final String anInstanceName, final Service other) {
        instanceName = anInstanceName;
        registrationType = other.registrationType();

        attributes = other.attributes();
        ipv4Address = other.ipv4Address();
        ipv6Address = other.ipv6Address();
        port = other.port();
        priority = other.priority();
        hostname = other.hostname();
        weight = other.weight();

        awaitingResponse = false;
        lock = new ReentrantLock();
        responded = lock.newCondition();
    }

    /**
     * Constructor.
     *
     * @param instanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     */
    ServiceImpl(final String anInstanceName, final String aRegistrationType) {
        instanceName = anInstanceName;
        registrationType = aRegistrationType;

        attributes = Optional.empty();
        ipv4Address = Optional.empty();
        ipv6Address = Optional.empty();
        port = -1;
        priority = -1;
        hostname = Optional.empty();
        weight = -1;

        awaitingResponse = false;
        lock = new ReentrantLock();
        responded = lock.newCondition();
    }

    @Override
    public final Optional<Attributes> attributes() {
        return attributes;
    }

    @Override
    public final Optional<String> hostname() {
        return hostname;
    }

    @Override
    public final String instanceName() {
        return instanceName;
    }

    @Override
    public final Optional<Inet4Address> ipv4Address() {
        return ipv4Address;
    }

    @Override
    public final Optional<Inet6Address> ipv6Address() {
        return ipv6Address;
    }

    @Override
    public final short port() {
        return port;
    }

    @Override
    public final short priority() {
        return priority;
    }

    @Override
    public final String registrationPointerName() {
        return registrationType + "local.";
    }

    @Override
    public final String registrationType() {
        return registrationType;
    }

    @Override
    public final void responseReceived(final DnsMessage response, final ZeroconfHelper zc) {
        lock.lock();
        try {
            response.answers().forEach(a -> updateRecord(zc, a));
            awaitingResponse = false;
            responded.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final String serviceName() {
        return instanceName + "." + registrationPointerName();
    }

    @Override
    public final String toString() {
        return "Service [instance name=" + instanceName + "; registration type=" + registrationType + "]";
    }

    @Override
    public final short weight() {
        return weight;
    }

    /*
     * Returns true if the service could be discovered on the network, and updates this object with details
     * discovered.
     */
    final boolean resolve(final ZeroconfImpl zc, final Duration timeout) {
        final Set<Short> recTypes = new HashSet<>();
        recTypes.add(TYPE_SRV);
        recTypes.add(TYPE_TXT);
        if (hostname.isPresent()) {
            if (zc.ipv4Supported()) {
                recTypes.add(TYPE_A);
            }
            if (zc.ipv6Supported()) {
                recTypes.add(TYPE_AAAA);
            }
        }

        final String serviceName = serviceName();

        for (final short recType : recTypes) {
            zc.cachedRecord(serviceName, recType, CLASS_IN).ifPresent(c -> updateRecord(zc, c));
        }

        if (resolved()) {
            return true;
        }

        final Queue<Duration> delays = delays(timeout);
        zc.addResponseListener(this);
        try {
            while (!resolved() && !delays.isEmpty()) {
                final Optional<Instant> now = Optional.of(zc.now());
                final DnsMessage.Builder b = DnsMessage.query();
                b.addQuestion(new DnsQuestion(serviceName, TYPE_SRV, CLASS_IN));
                zc.cachedRecord(serviceName, TYPE_SRV, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));

                b.addQuestion(new DnsQuestion(serviceName, TYPE_TXT, CLASS_IN));
                zc.cachedRecord(serviceName, TYPE_TXT, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));

                if (hostname.isPresent()) {
                    final String host = hostname.get();
                    if (zc.ipv4Supported()) {
                        b.addQuestion(new DnsQuestion(host, TYPE_A, CLASS_IN));
                        zc.cachedRecord(host, TYPE_A, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));
                    }
                    if (zc.ipv6Supported()) {
                        b.addQuestion(new DnsQuestion(host, TYPE_AAAA, CLASS_IN));
                        zc.cachedRecord(host, TYPE_AAAA, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));
                    }
                }
                zc.sendMessage(b.get());
                awaitResponse(delays.poll());
            }
        } finally {
            zc.removeResponseListener(this);
        }
        return resolved();
    }

    final void setAttributes(final Attributes someAttributes) {
        attributes = Optional.of(someAttributes);
    }

    final void setHostname(final String aHostname) {
        hostname = Optional.of(aHostname);
    }

    final void setIpv4Address(final Inet4Address anAddress) {
        ipv4Address = Optional.of(anAddress);
    }

    final void setIpv6Address(final Inet6Address anAddress) {
        ipv6Address = Optional.of(anAddress);
    }

    final void setPort(final short aPort) {
        port = aPort;
    }

    final void setPriority(final short aPriority) {
        priority = aPriority;
    }

    final void setWeight(final short aWeight) {
        weight = aWeight;
    }

    private void awaitResponse(final Duration dur) {
        lock.lock();
        awaitingResponse = true;
        boolean response = false;
        try {
            final CountDownDuration cdd = CountDownDuration.of(dur).start();
            Duration remaining = cdd.remaining();
            while (awaitingResponse && !remaining.isZero()) {
                response = responded.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                remaining = cdd.remaining();
            }
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for response", e);
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        if (!response) {
            LOGGER.fine(() -> "No response received within " + dur);
        }
    }

    private Queue<Duration> delays(final Duration timeout) {
        final Queue<Duration> q = new ArrayDeque<>();
        if (timeout.compareTo(INIT_REQ_DELAY) <= 0) {
            return q;
        }
        int factor = 1;
        Duration delay = INIT_REQ_DELAY;
        Duration total = Duration.ZERO;
        do {
            q.add(delay);
            total = total.plus(delay);
            factor = factor * 2;
            delay = INIT_REQ_DELAY.multipliedBy(factor);
        } while (total.plus(delay).compareTo(timeout) <= 0);
        delay = timeout.minus(total);
        if (!delay.isZero()) {
            q.add(delay);
        }
        return q;
    }

    private boolean resolved() {
        return hostname.isPresent()
            && (ipv4Address.isPresent() || ipv6Address.isPresent())
            && attributes.isPresent();
    }

    /* Updates service information from a DNS record. */
    private void updateRecord(final ZeroconfHelper zc, final DnsRecord record) {
        if (!record.isExpired(zc.now())) {
            final String serviceName = serviceName();
            if (record.type() == TYPE_A
                && zc.ipv4Supported()
                && hostname.map(s -> s.equals(record.name())).orElse(false)) {
                ipv4Address = Optional.of((Inet4Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "IPV4 address of service [" + serviceName + UPDATED_TO + ipv4Address.get());
            } else if (record.type() == TYPE_AAAA
                && zc.ipv6Supported()
                && hostname.map(s -> s.equals(record.name())).orElse(false)) {
                ipv6Address = Optional.of((Inet6Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "Address of service [" + serviceName + UPDATED_TO + ipv6Address.get());

            } else if (record.type() == TYPE_SRV && record.name().equalsIgnoreCase(serviceName)) {
                final SrvRecord srv = (SrvRecord) record;
                port = srv.port();
                priority = srv.priority();
                hostname = Optional.of(srv.server());
                weight = srv.weight();
                LOGGER.fine(() -> "Port of service [" + serviceName + UPDATED_TO + port);
                LOGGER.fine(() -> "Priority of service [" + serviceName + UPDATED_TO + priority);
                LOGGER.fine(() -> "Server of service [" + serviceName + UPDATED_TO + hostname.get());
                LOGGER.fine(() -> "Weight of service [" + serviceName + UPDATED_TO + weight);
                zc.cachedRecord(hostname.get(), TYPE_A, CLASS_IN).ifPresent(r -> updateRecord(zc, r));
                zc.cachedRecord(hostname.get(), TYPE_AAAA, CLASS_IN).ifPresent(r -> updateRecord(zc, r));
            } else if (record.type() == TYPE_TXT && record.name().equalsIgnoreCase(serviceName)) {
                attributes = Optional.of(((TxtRecord) record).attributes());
                LOGGER.fine(() -> "Attributes of service [" + serviceName + UPDATED_TO + attributes.get());
            } else {
                LOGGER.fine(() -> "Ignored irrelevant answer of type " + record.type() + " for " + record.name());
            }
        } else {
            LOGGER.info(() -> "Ignored expired answer of type " + record.type());
        }
    }

}
