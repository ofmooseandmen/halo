/*
Copyright 2018 - 2020 Cedric Liegeois

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

import static io.omam.halo.HaloProperties.RESOLUTION_INTERVAL;
import static io.omam.halo.MulticastDnsSd.CLASS_IN;
import static io.omam.halo.MulticastDnsSd.DOMAIN;
import static io.omam.halo.MulticastDnsSd.TYPE_A;
import static io.omam.halo.MulticastDnsSd.TYPE_AAAA;
import static io.omam.halo.MulticastDnsSd.TYPE_SRV;
import static io.omam.halo.MulticastDnsSd.TYPE_TXT;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Service implementation.
 */
final class ServiceImpl implements Service, ResponseListener {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ServiceImpl.class.getName());

    /** string for log. */
    private static final String UPDATED_TO = "] updated to ";

    /** service attributes */
    private Attributes attributes;

    /** whether this service is awaiting resolution. */
    private volatile boolean awaitingResolution;

    /** service hostname. */
    private String hostname;

    /** service instance name. */
    private final String instanceName;

    /** service IPv4 address. */
    private Optional<InetAddress> ipv4Address;

    /** service IPv6 address. */
    private Optional<InetAddress> ipv6Address;

    /** lock (see #resolved condition). */
    private final Lock lock;

    /** service port. */
    private short port;

    /** service registration type. */
    private final String registrationType;

    /** condition signaled when service has been resolved. */
    private final Condition resolved;

    /**
     * Constructor enabling to create a new service from another one but with a new instance name.
     *
     * @param anInstanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param other other service
     */
    ServiceImpl(final String anInstanceName, final Service other) {
        instanceName = anInstanceName;
        registrationType = other.registrationType();

        attributes = other.attributes();
        ipv4Address = other.ipv4Address();
        ipv6Address = other.ipv6Address();
        port = other.port();
        hostname = other.hostname();

        awaitingResolution = false;
        lock = new ReentrantLock();
        resolved = lock.newCondition();
    }

    /**
     * Constructor.
     *
     * @param anInstanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param aRegistrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
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

        awaitingResolution = false;
        lock = new ReentrantLock();
        resolved = lock.newCondition();
    }

    /**
     * Determines the instance name of the given service name.
     *
     * @param serviceName service name
     * @return instance name
     */
    static Optional<String> instanceNameOf(final String serviceName) {
        /* everything until first dot. */
        final int end = serviceName.indexOf('.');
        return end == -1 ? Optional.empty() : Optional.of(serviceName.substring(0, end));
    }

    /**
     * Determines the registration type of the given service name.
     *
     * @param serviceName service name
     * @return registration type
     */
    static Optional<String> registrationTypeOf(final String serviceName) {
        final int begin = serviceName.indexOf('.');
        final int end = serviceName.indexOf(DOMAIN);
        /* everything after first dot and until local. */
        return begin == -1 || end == -1 ? Optional.empty() : Optional.of(serviceName.substring(begin + 1, end));
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
    public final Optional<InetAddress> ipv4Address() {
        return ipv4Address;
    }

    @Override
    public final Optional<InetAddress> ipv6Address() {
        return ipv6Address;
    }

    @Override
    public final String name() {
        return instanceName + "." + registrationPointerName();
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
            response.answers().forEach(a -> update(halo, a));
            awaitingResolution = !resolved();
            if (!awaitingResolution) {
                LOGGER.fine("Received response resolving service");
                resolved.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final String toString() {
        return "Service [instance name=" + instanceName + "; registration type=" + registrationType + "]";
    }

    /**
     * Tries to resolve this service on the network. Updates service IP address(es), server and attributes if the
     * service is discovered.
     *
     * @param halo halo helper
     * @param timeout resolution timeout
     * @return {@code true} iff service has been resolved
     * @throws InterruptedException if interrupted while waiting for resolution
     */
    final boolean resolve(final HaloHelper halo, final Duration timeout) throws InterruptedException {
        final String serviceName = name();

        /* look for a cached SRV record. */
        final Optional<DnsRecord> cachedSrv = halo.cachedRecord(serviceName, TYPE_SRV, CLASS_IN);
        cachedSrv.ifPresent(c -> update(halo, c));

        /* look for a cached TXT record. */
        final Optional<DnsRecord> cachedTxt = halo.cachedRecord(serviceName, TYPE_TXT, CLASS_IN);
        cachedTxt.ifPresent(c -> update(halo, c));

        final Optional<DnsRecord> cachedIpV4;
        final Optional<DnsRecord> cachedIpV6;
        if (hostname == null) {
            cachedIpV4 = Optional.empty();
            cachedIpV6 = Optional.empty();
        } else {
            /* look for a cached A record. */
            cachedIpV4 = halo.cachedRecord(hostname, TYPE_A, CLASS_IN);
            cachedIpV4.ifPresent(c -> update(halo, c));
            /* look for a cached AAAA record. */
            cachedIpV6 = halo.cachedRecord(hostname, TYPE_AAAA, CLASS_IN);
            cachedIpV6.ifPresent(c -> update(halo, c));
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
                cachedSrv.ifPresent(r -> b.addAnswer(r, now));

                b.addQuestion(new DnsQuestion(serviceName, TYPE_TXT, CLASS_IN));
                cachedTxt.ifPresent(r -> b.addAnswer(r, now));

                if (hostname != null) {
                    b.addQuestion(new DnsQuestion(hostname, TYPE_A, CLASS_IN));
                    cachedIpV4.ifPresent(r -> b.addAnswer(r, now));
                    b.addQuestion(new DnsQuestion(hostname, TYPE_AAAA, CLASS_IN));
                    cachedIpV6.ifPresent(r -> b.addAnswer(r, now));
                }
                halo.sendMessage(b.get());
                awaitResolution(delays.poll());
            }
        } finally {
            halo.removeResponseListener(this);
        }
        return resolved();
    }

    /**
     * Sets the attributes.
     *
     * @param someAttributes attributes
     */
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

    /**
     * Sets the IPv4 address.
     *
     * @param anAddress address
     */
    final void setIpv4Address(final Inet4Address anAddress) {
        ipv4Address = Optional.of(anAddress);
    }

    /**
     * Sets the IPv6 address.
     *
     * @param anAddress address
     */
    final void setIpv6Address(final Inet6Address anAddress) {
        ipv6Address = Optional.of(anAddress);
    }

    /**
     * Sets the port.
     *
     * @param aPort port
     */
    final void setPort(final short aPort) {
        port = aPort;
    }

    /**
     * Awaits until this service is resolved or the given timeout has elapsed whichever occurs first.
     *
     * @param dur timeout
     * @throws InterruptedException if interrupted while waiting for resolution
     */
    private void awaitResolution(final Duration dur) throws InterruptedException {
        lock.lock();
        awaitingResolution = true;
        boolean response = false;
        try {
            final Timeout timeout = Timeout.ofDuration(dur);
            Duration remaining = timeout.remaining();
            while (awaitingResolution && !remaining.isZero()) {
                response = resolved.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                remaining = timeout.remaining();
            }
        } finally {
            lock.unlock();
        }
        if (!response) {
            LOGGER.fine(() -> "No response received within " + dur);
        }
    }

    /**
     * Computes delays covering the given timeout.
     * <p>
     * First delay is always {@link #RESOLUTION_INTERVAL}, following are twice the previous delay (in order to
     * space more and more the sent messages and avoid over-flooding receiver).
     *
     * @param timeout timeout
     * @return delays
     */
    private Queue<Duration> delays(final Duration timeout) {
        final Queue<Duration> result = new ArrayDeque<>();
        if (timeout.compareTo(RESOLUTION_INTERVAL) <= 0) {
            return result;
        }
        int factor = 1;
        Duration delay = RESOLUTION_INTERVAL;
        Duration total = Duration.ZERO;
        do {
            result.add(delay);
            total = total.plus(delay);
            factor = factor * 2;
            delay = RESOLUTION_INTERVAL.multipliedBy(factor);
        } while (total.plus(delay).compareTo(timeout) <= 0);
        delay = timeout.minus(total);
        if (!delay.isZero()) {
            result.add(delay);
        }
        return result;
    }

    /**
     * Determines whether this service is resolved: hostname and attributes are not null and at least an IPv4 or
     * IPv6 address is present.
     *
     * @return {@code true} if this service is resolved
     */
    private boolean resolved() {
        return hostname != null && (ipv4Address.isPresent() || ipv6Address.isPresent()) && attributes != null;
    }

    /**
     * Updates this service with data of the given DNS record.
     *
     * @param halo halo helper
     * @param record DNS record
     */
    private void update(final HaloHelper halo, final DnsRecord record) {
        if (record.isExpired(halo.now())) {
            LOGGER.info(() -> "Ignored expired " + record);
        } else {
            final String serviceName = name();
            final boolean matchesService = record.name().equalsIgnoreCase(serviceName);
            final boolean matchesHost = record.name().equalsIgnoreCase(hostname);
            if (record.type() == TYPE_A && matchesHost) {
                ipv4Address = Optional.of((Inet4Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "IPV4 address of service [" + serviceName + UPDATED_TO + ipv4Address.get());

            } else if (record.type() == TYPE_AAAA && matchesHost) {
                ipv6Address = Optional.of((Inet6Address) ((AddressRecord) record).address());
                LOGGER.fine(() -> "Address of service [" + serviceName + UPDATED_TO + ipv6Address.get());

            } else if (record.type() == TYPE_SRV && matchesService) {
                final SrvRecord srv = (SrvRecord) record;
                port = srv.port();
                hostname = srv.server();
                LOGGER.fine(() -> "Port of service [" + serviceName + UPDATED_TO + port);
                LOGGER.fine(() -> "Server of service [" + serviceName + UPDATED_TO + hostname);
                halo.cachedRecord(hostname, TYPE_A, CLASS_IN).ifPresent(r -> update(halo, r));
                halo.cachedRecord(hostname, TYPE_AAAA, CLASS_IN).ifPresent(r -> update(halo, r));

            } else if (record.type() == TYPE_TXT && matchesService) {
                attributes = ((TxtRecord) record).attributes();
                LOGGER.fine(() -> "Attributes of service [" + serviceName + UPDATED_TO + attributes);

            } else {
                LOGGER.fine(() -> "Ignored irrelevant " + record);
            }
        }
    }

}
