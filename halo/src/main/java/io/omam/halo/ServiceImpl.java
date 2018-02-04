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
package io.omam.halo;

import static io.omam.halo.MulticastDns.CLASS_IN;
import static io.omam.halo.MulticastDns.TYPE_A;
import static io.omam.halo.MulticastDns.TYPE_AAAA;
import static io.omam.halo.MulticastDns.TYPE_SRV;
import static io.omam.halo.MulticastDns.TYPE_TXT;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("javadoc")
final class ServiceImpl implements Service, ResponseListener {

    /** the domain: always local. */
    private static final String DOMAIN = "local";

    private static final Duration INIT_REQ_DELAY = Duration.ofMillis(200);

    private static final Logger LOGGER = Logger.getLogger(ServiceImpl.class.getName());

    private static final String UPDATED_TO = "] updated to ";

    private Attributes attributes;

    private volatile boolean awaitingResponse;

    private String hostname;

    private final String instanceName;

    private Optional<Inet4Address> ipv4Address;

    private Optional<Inet6Address> ipv6Address;

    private final Lock lock;

    private short port;

    private final String registrationType;

    private final Condition responded;

    ServiceImpl(final String anInstanceName, final Service other) {
        instanceName = anInstanceName;
        registrationType = other.registrationType();

        attributes = other.attributes();
        ipv4Address = other.ipv4Address();
        ipv6Address = other.ipv6Address();
        port = other.port();
        hostname = other.hostname();

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

        attributes = null;
        ipv4Address = Optional.empty();
        ipv6Address = Optional.empty();
        port = -1;
        hostname = null;

        awaitingResponse = false;
        lock = new ReentrantLock();
        responded = lock.newCondition();
    }

    @Override
    public final Attributes attributes() {
        return attributes;
    }

    @Override
    public final String hostname() {
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
    public final String registrationPointerName() {
        return registrationType + DOMAIN + ".";
    }

    @Override
    public final String registrationType() {
        return registrationType;
    }

    @Override
    public final void responseReceived(final DnsMessage response, final HaloHelper halo) {
        lock.lock();
        LOGGER.fine(() -> "Handling " + response);
        try {
            response.answers().forEach(a -> updateRecord(halo, a));
            awaitingResponse = resolved();
            if (!awaitingResponse) {
                LOGGER.fine("Received response resolving service");
                responded.signalAll();
            }
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

    /**
     * Returns true if the service could be discovered on the network, and updates this object with details
     * discovered.
     *
     * @param halo halo helper
     * @param timeout resolution timeout
     */
    final boolean resolve(final HaloHelper halo, final Duration timeout) {
        final String serviceName = serviceName();

        /* look for a cached SRV record. */
        halo.cachedRecord(serviceName, TYPE_SRV, CLASS_IN).ifPresent(c -> updateRecord(halo, c));

        /* look for a cached TXT record. */
        halo.cachedRecord(serviceName, TYPE_TXT, CLASS_IN).ifPresent(c -> updateRecord(halo, c));

        if (hostname != null) {
            /* look for a cached A record. */
            halo.cachedRecord(hostname, TYPE_A, CLASS_IN).ifPresent(c -> updateRecord(halo, c));
            /* look for a cached AAAA record. */
            halo.cachedRecord(hostname, TYPE_AAAA, CLASS_IN).ifPresent(c -> updateRecord(halo, c));
        }

        if (resolved()) {
            return true;
        }

        final Queue<Duration> delays = delays(timeout);
        halo.addResponseListener(this);
        try {
            while (!resolved() && !delays.isEmpty()) {
                final Optional<Instant> now = Optional.of(halo.now());
                final DnsMessage.Builder b = DnsMessage.query();
                b.addQuestion(new DnsQuestion(serviceName, TYPE_SRV, CLASS_IN));
                halo.cachedRecord(serviceName, TYPE_SRV, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));

                b.addQuestion(new DnsQuestion(serviceName, TYPE_TXT, CLASS_IN));
                halo.cachedRecord(serviceName, TYPE_TXT, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));

                if (hostname != null) {
                    b.addQuestion(new DnsQuestion(hostname, TYPE_A, CLASS_IN));
                    halo.cachedRecord(hostname, TYPE_A, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));
                    b.addQuestion(new DnsQuestion(hostname, TYPE_AAAA, CLASS_IN));
                    halo.cachedRecord(hostname, TYPE_AAAA, CLASS_IN).ifPresent(r -> b.addAnswer(r, now));
                }
                halo.sendMessage(b.get());
                awaitResponse(delays.poll());
            }
        } finally {
            halo.removeResponseListener(this);
        }
        return resolved();
    }

    final void setAttributes(final Attributes someAttributes) {
        attributes = someAttributes;
    }

    /**
     * Sets the hostname, appending '.local.' if needed.
     *
     * @param name the hostname
     */
    final void setHostname(final String name) {
        String aHostname = name;
        final int index = aHostname.indexOf("." + DOMAIN);
        if (index > 0) {
            aHostname = aHostname.substring(0, index);
        }
        aHostname = aHostname.replaceAll("[:%\\.]", "-");
        aHostname += "." + DOMAIN + ".";
        hostname = aHostname;
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

    private void awaitResponse(final Duration dur) {
        lock.lock();
        awaitingResponse = true;
        boolean response = false;
        try {
            final Timeout timeout = Timeout.of(dur);
            Duration remaining = timeout.remaining();
            while (awaitingResponse && !remaining.isZero()) {
                response = responded.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                remaining = timeout.remaining();
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
        return hostname != null && (ipv4Address.isPresent() || ipv6Address.isPresent()) && attributes != null;
    }

    /* Updates service information from a DNS record. */
    private void updateRecord(final HaloHelper halo, final DnsRecord record) {
        if (!record.isExpired(halo.now())) {
            final String serviceName = serviceName();
            final boolean matchesHost = record.name().equals(hostname);
            if (record.type() == TYPE_A && matchesHost) {
                ipv4Address = Optional.of((Inet4Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "IPV4 address of service [" + serviceName + UPDATED_TO + ipv4Address.get());
            } else if (record.type() == TYPE_AAAA && matchesHost) {
                ipv6Address = Optional.of((Inet6Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "Address of service [" + serviceName + UPDATED_TO + ipv6Address.get());
            } else if (record.type() == TYPE_SRV && record.name().equalsIgnoreCase(serviceName)) {
                final SrvRecord srv = (SrvRecord) record;
                port = srv.port();
                hostname = srv.server();
                LOGGER.fine(() -> "Port of service [" + serviceName + UPDATED_TO + port);
                LOGGER.fine(() -> "Server of service [" + serviceName + UPDATED_TO + hostname);
                halo.cachedRecord(hostname, TYPE_A, CLASS_IN).ifPresent(r -> updateRecord(halo, r));
                halo.cachedRecord(hostname, TYPE_AAAA, CLASS_IN).ifPresent(r -> updateRecord(halo, r));
            } else if (record.type() == TYPE_TXT && record.name().equalsIgnoreCase(serviceName)) {
                attributes = ((TxtRecord) record).attributes();
                LOGGER.fine(() -> "Attributes of service [" + serviceName + UPDATED_TO + attributes);
            } else {
                LOGGER.fine(() -> "Ignored irrelevant answer of type " + record.type() + " for " + record.name());
            }
        } else {
            LOGGER.info(() -> "Ignored expired answer of type " + record.type());
        }
    }

}
