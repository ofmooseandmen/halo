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
import static io.omam.halo.MulticastDns.DISCOVERY;
import static io.omam.halo.MulticastDns.FLAGS_AA;
import static io.omam.halo.MulticastDns.TTL;
import static io.omam.halo.MulticastDns.TYPE_A;
import static io.omam.halo.MulticastDns.TYPE_AAAA;
import static io.omam.halo.MulticastDns.TYPE_ANY;
import static io.omam.halo.MulticastDns.TYPE_PTR;
import static io.omam.halo.MulticastDns.TYPE_SRV;
import static io.omam.halo.MulticastDns.TYPE_TXT;
import static io.omam.halo.MulticastDns.uniqueClass;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.omam.halo.DnsMessage.Builder;

@SuppressWarnings("javadoc")
final class HaloImpl extends HaloHelper implements Halo, Consumer<DnsMessage> {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(HaloImpl.class.getName());

    /** string for logging. */
    private static final String ON_DOMAIN = " on the local domain";

    /** pattern to match service instance name when making them unique. */
    private static final Pattern INSTANCE_NAME_PATTERN = Pattern.compile("([\\s\\S]*?)( \\((?<i>\\d+)\\))$");

    /** Service announcer. */
    private final Announcer announcer;

    /** DNS record cache. */
    private final Cache cache;

    /** channel. */
    private final HaloChannel channel;

    /** clock. */
    private final Clock clock;

    /** {@link ResponseListener listener}s. */
    private final List<ResponseListener> rls;

    /** map of all registered service indexed by {@link Service#instanceName()}. */
    private final Map<String, Service> services;

    /** number of services registered by {@link Service#registrationPointerName()}. */
    private final Map<String, Integer> registrationPointerNames;

    /**
     * Constructor.
     *
     * @param aClock clock
     * @param nics network interface(s)
     * @throws IOException in case of I/O error
     */
    HaloImpl(final Clock aClock, final Collection<NetworkInterface> nics) throws IOException {
        announcer = new Announcer(this);
        cache = new Cache();
        if (nics.isEmpty()) {
            channel = HaloChannel.allNetworkInterfaces(this, aClock);
        } else {
            channel = HaloChannel.networkInterfaces(this, aClock, nics);
        }
        clock = aClock;
        rls = new CopyOnWriteArrayList<>();

        services = new ConcurrentHashMap<>();
        registrationPointerNames = new ConcurrentHashMap<>();

        channel.enable();
    }

    /**
     * Computes the next instance name of the given one.
     * <p>
     * if given instance name ends with {@code (n)}, then the returned instance name ends with {@code (n+1)},
     * otherwise it ends with {@code (2)}.
     *
     * @param instanceName instance name
     * @return new instance name
     */
    private static String changeInstanceName(final String instanceName) {
        final Matcher m = INSTANCE_NAME_PATTERN.matcher(instanceName);
        final String result;
        if (!m.matches()) {
            result = instanceName + " (2)";
        } else {
            final int next = Integer.parseInt(m.group("i")) + 1;
            final int start = m.start("i");
            final int end = m.end("i");
            result = instanceName.substring(0, start) + next + instanceName.substring(end);
        }
        LOGGER.fine(() -> "Change service instance name from: [" + instanceName + "] to [" + result + "]");
        return result;
    }

    @Override
    public final void accept(final DnsMessage in) {
        if (in.isQuery()) {
            handleQuery(in);
        } else if (in.isResponse()) {
            handleResponse(in);
        } else {
            LOGGER.warning("Ignored received DNS message.");
        }
    }

    @Override
    public final void close() throws IOException {
        cache.clear();
        announcer.close();
        channel.close();
        rls.clear();
    }

    @Override
    public final Service register(final Service service, final Duration ttl, final boolean allowNameChange)
            throws IOException {
        LOGGER.fine(() -> "Registering " + service + ON_DOMAIN);
        final Service rservice = checkInstanceName(service, allowNameChange);
        add(rservice);

        final boolean announced = announcer.announce(rservice);
        if (!announced) {
            remove(rservice);
            final String msg = "Found conflicts while announcing " + rservice + " on network";
            LOGGER.warning(msg);
            throw new IOException(msg);
        }

        LOGGER.info(() -> "Registered " + rservice + ON_DOMAIN);
        return rservice;
    }

    @Override
    public final Optional<Service> resolve(final String instanceName, final String registrationType,
            final Duration timeout) {
        final ServiceImpl si = new ServiceImpl(instanceName, registrationType);
        LOGGER.fine(() -> "Resolving " + si.toString() + ON_DOMAIN);
        if (si.resolve(this, timeout)) {
            LOGGER.info(() -> "Resolved " + si.toString() + ON_DOMAIN);
            return Optional.of(si);
        }
        LOGGER.info(() -> "Could not resolve " + si.toString() + ON_DOMAIN);
        return Optional.empty();
    }

    @Override
    final void addResponseListener(final ResponseListener listener) {
        Objects.requireNonNull(listener);
        LOGGER.fine(() -> "Adding response listener " + listener);
        rls.add(listener);
    }

    @Override
    final Optional<DnsRecord> cachedRecord(final String name, final short type, final short clazz) {
        return cache.get(name, type, clazz);
    }

    @Override
    final Instant now() {
        return clock.instant();
    }

    @Override
    final void removeResponseListener(final ResponseListener listener) {
        Objects.requireNonNull(listener);
        LOGGER.fine(() -> "Removing response listener " + listener);
        rls.remove(listener);
    }

    @Override
    final void sendMessage(final DnsMessage msg) {
        channel.send(msg);
    }

    private void add(final Service s) {
        services.put(s.serviceName().toLowerCase(), s);
        final String rpn = s.registrationPointerName();
        final int current = registrationPointerNames.getOrDefault(rpn, 0);
        registrationPointerNames.put(rpn, current + 1);
    }

    private void addIpv4Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        services
            .values()
            .stream()
            .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
            .filter(h -> h.ipv4Address().isPresent())
            .forEach(s -> {
                final Inet4Address addr = s.ipv4Address().get();
                builder.addAnswer(query,
                        new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
            });
    }

    private void addIpv6Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        services
            .values()
            .stream()
            .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
            .filter(h -> h.ipv6Address().isPresent())
            .forEach(s -> {
                final Inet6Address addr = s.ipv6Address().get();
                builder.addAnswer(query,
                        new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
            });
    }

    private void addPtrAnswer(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        if (question.name().equals(DISCOVERY)) {
            for (final String rpn : registrationPointerNames.keySet()) {
                builder.addAnswer(query, new PtrRecord(DISCOVERY, CLASS_IN, TTL, now, rpn));
            }
        }
        for (final Service s : services.values()) {
            if (question.name().equalsIgnoreCase(s.registrationPointerName())) {
                builder.addAnswer(query,
                        new PtrRecord(s.registrationPointerName(), CLASS_IN, TTL, now, s.serviceName()));
            }
        }
    }

    private void addServiceAnswer(final DnsMessage query, final DnsQuestion question, final Service service,
            final Builder builder, final Instant now) {
        final short unique = uniqueClass(CLASS_IN);
        final String hostname = service.hostname();
        if (question.type() == TYPE_SRV || question.type() == TYPE_ANY) {
            builder.addAnswer(query, new SrvRecord(question.name(), unique, TTL, now, service.port(), hostname));
        }

        if (question.type() == TYPE_TXT || question.type() == TYPE_ANY) {
            builder.addAnswer(query, new TxtRecord(question.name(), unique, TTL, now, service.attributes()));
        }

        if (question.type() == TYPE_SRV) {
            service.ipv4Address().ifPresent(
                    a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
            service.ipv6Address().ifPresent(
                    a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
        }
    }

    private DnsMessage buildResponse(final DnsMessage query) {
        final Builder builder = DnsMessage.response(FLAGS_AA);
        final Instant now = now();
        for (final DnsQuestion question : query.questions()) {
            if (question.type() == TYPE_PTR) {
                addPtrAnswer(query, question, builder, now);
            } else {
                if (question.type() == TYPE_A || question.type() == TYPE_ANY) {
                    addIpv4Address(query, question, builder, now);
                }
                if (question.type() == TYPE_AAAA || question.type() == TYPE_ANY) {
                    addIpv6Address(query, question, builder, now);
                }

                final Service s = services.get(question.name().toLowerCase());
                if (s != null) {
                    addServiceAnswer(query, question, s, builder, now);
                }
            }
        }
        return builder.get();
    }

    /**
     * Checks the network for a unique instance name, returning a new {@link Service} if it is not unique.
     *
     * @param service service to check
     * @param allowNameChange whether the instance name can be changed if not unique
     * @return the service with a unique instance name
     * @throws if service instance name cannot be made unique
     */
    private Service checkInstanceName(final Service service, final boolean allowNameChange) throws IOException {
        final String hostname = service.hostname();
        final int port = service.port();
        boolean collision = false;
        Service result = service;
        do {
            collision = false;
            final Instant now = now();
            /* check cache. */
            final Optional<SrvRecord> rec = cache
                .entries(result.serviceName())
                .stream()
                .filter(e -> e instanceof SrvRecord)
                .filter(e -> !e.isExpired(now))
                .map(e -> (SrvRecord) e)
                .filter(e -> e.port() != port || !e.server().equals(hostname))
                .findFirst();
            if (rec.isPresent()) {
                collision = true;
                final String msg = "Cache collision: " + rec.get();
                result = tryResolveCollision(result, allowNameChange, msg);
            }

            /* check own services. */
            final Service own = services.get(result.serviceName().toLowerCase());
            if (own != null) {
                final String otherHostname = own.hostname();
                collision = own.port() != port || !otherHostname.equals(hostname);
                if (collision) {
                    final String msg = "Registered service collision: " + own;
                    result = tryResolveCollision(result, allowNameChange, msg);
                }
            }
        } while (collision);
        return result;
    }

    private void handleQuery(final DnsMessage query) {
        LOGGER.fine(() -> "Trying to respond to " + query);
        final DnsMessage response = buildResponse(query);
        if (!response.answers().isEmpty()) {
            LOGGER.fine(() -> "Responding with " + response);
            channel.send(response);
        } else {
            LOGGER.fine("Ignoring query");
        }
    }

    private void handleResponse(final DnsMessage response) {
        LOGGER.fine(() -> "Handling response " + response);
        final Instant now = now();
        for (final DnsRecord record : response.answers()) {
            if (record.isExpired(now)) {
                cache.remove(record);
            } else {
                cache.add(record);
            }
        }
        if (rls.isEmpty()) {
            LOGGER.fine(() -> "No listener registered for " + response);
        } else {
            rls.forEach(l -> l.responseReceived(response, this));
        }
    }

    private void remove(final Service s) {
        final String rpn = s.registrationPointerName();
        if (registrationPointerNames.get(rpn) == 1) {
            registrationPointerNames.remove(rpn);
        } else {
            registrationPointerNames.put(rpn, registrationPointerNames.get(rpn) - 1);
        }
        services.remove(s.serviceName().toLowerCase());
    }

    private Service tryResolveCollision(final Service service, final boolean allowNameChange, final String msg)
            throws IOException {
        if (!allowNameChange) {
            throw new IOException(msg);
        }
        LOGGER.info(msg);
        final String instanceName = changeInstanceName(service.instanceName());
        return new ServiceImpl(instanceName, service);
    }

}
