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

import static io.omam.halo.HaloProperties.TTL;
import static io.omam.halo.MulticastDnsSd.CLASS_IN;
import static io.omam.halo.MulticastDnsSd.FLAGS_AA;
import static io.omam.halo.MulticastDnsSd.RT_DISCOVERY;
import static io.omam.halo.MulticastDnsSd.TYPE_A;
import static io.omam.halo.MulticastDnsSd.TYPE_AAAA;
import static io.omam.halo.MulticastDnsSd.TYPE_ANY;
import static io.omam.halo.MulticastDnsSd.TYPE_PTR;
import static io.omam.halo.MulticastDnsSd.TYPE_SRV;
import static io.omam.halo.MulticastDnsSd.TYPE_TXT;
import static io.omam.halo.MulticastDnsSd.toLowerCase;
import static io.omam.halo.MulticastDnsSd.uniqueClass;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.omam.halo.DnsMessage.Builder;

/**
 * Halo implementation.
 */
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

    /** Service canceller. */
    private final Canceller canceller;

    /** channel. */
    private final HaloChannel channel;

    /** clock. */
    private final Clock clock;

    /** cache record reaper. */
    private final Reaper reaper;

    /** {@link ResponseListener listener}s. */
    private final Collection<ResponseListener> rls;

    /** Registration Types browser. */
    private final HaloRegistrationTypeBrowser rBrowser;

    /** Service browser. */
    private final HaloServiceBrowser sBrowser;

    /** map of all services being registered indexed by {@link Service#instanceName()}. */
    private final Map<String, RegisterableService> announcing;

    /** map of all registered services indexed by {@link Service#instanceName()}. */
    private final Map<String, RegisteredService> registered;

    /** set of all registration pointer names. */
    private final Set<String> registrationPointerNames;

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
        canceller = new Canceller(this);
        if (nics.isEmpty()) {
            channel = HaloChannel.allNetworkInterfaces(this, aClock);
        } else {
            channel = HaloChannel.networkInterfaces(this, aClock, nics);
        }
        clock = aClock;
        reaper = new Reaper(cache, clock);
        rls = new ConcurrentLinkedQueue<>();

        rBrowser = new HaloRegistrationTypeBrowser(this);
        sBrowser = new HaloServiceBrowser(this);

        announcing = new ConcurrentHashMap<>();
        registered = new ConcurrentHashMap<>();
        registrationPointerNames = ConcurrentHashMap.newKeySet();

        channel.enable();
        reaper.start();
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
        final Matcher matcher = INSTANCE_NAME_PATTERN.matcher(instanceName);
        final String result;
        if (matcher.matches()) {
            final int next = Integer.parseInt(matcher.group("i")) + 1;
            final int start = matcher.start("i");
            final int end = matcher.end("i");
            result = instanceName.substring(0, start) + next + instanceName.substring(end);
        } else {
            result = instanceName + " (2)";
        }
        LOGGER.info(() -> "Change service instance name from: [" + instanceName + "] to [" + result + "]");
        return result;
    }

    @Override
    public final void accept(final DnsMessage message) {
        if (message.isQuery()) {
            handleQuery(message);
        } else if (message.isResponse()) {
            handleResponse(message);
        } else {
            LOGGER.warning("Ignored received DNS message.");
        }
    }

    @Override
    public final Browser browse(final RegistrationTypeBrowserListener listener) {
        rBrowser.addListener(listener);
        /* start in case this is the first time this method is called. */
        rBrowser.start();
        return () -> rBrowser.removeListener(listener);
    }

    @Override
    public final Browser browse(final String registrationType, final ServiceBrowserListener listener) {
        sBrowser.addListener(registrationType, listener);
        /* start in case this is the first time this method is called. */
        sBrowser.start();
        return () -> sBrowser.removeListener(registrationType, listener);
    }

    @Override
    public final void close() {
        /* close browsers. */
        sBrowser.close();
        rBrowser.close();

        /* close cache reaper. */
        reaper.close();

        try {
            /* de-register all services. */
            deregisterAll();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "I/O error when de-registering all services", e);
        } finally {
            /* close service background threads. */
            announcer.close();
            canceller.close();
            channel.close();
            cache.clear();
            rls.clear();
        }
    }

    @Override
    public final void deregister(final RegisteredService service) throws IOException {
        final String serviceKey = toLowerCase(service.name());
        if (registered.containsKey(serviceKey)) {
            canceller.cancel(service);
            registrationPointerNames.remove(service.registrationPointerName());
            registered.remove(serviceKey);
            cache.removeAll(service.name());
        } else {
            LOGGER.info(() -> service + " is not registered");
        }
    }

    @Override
    public final void deregisterAll() throws IOException {
        final List<String> messages = new ArrayList<>();
        for (final RegisteredService service : registered.values()) {
            try {
                deregister(service);
            } catch (final IOException e) {
                messages.add(e.getMessage());
            }
        }
        if (!messages.isEmpty()) {
            throw new IOException(messages.stream().collect(Collectors.joining(",")));
        }
    }

    @Override
    public final RegisteredService register(final RegisterableService service, final Duration ttl,
            final boolean allowNameChange) throws IOException {
        LOGGER.fine(() -> "Registering " + service + ON_DOMAIN);
        final RegisterableService toRegister = checkInstanceName(service, allowNameChange);
        final String serviceKey = toLowerCase(service.name());
        announcing.put(serviceKey, service);
        final String rpn = service.registrationPointerName();
        registrationPointerNames.add(rpn);

        final boolean announced = announcer.announce(toRegister, ttl);
        announcing.remove(serviceKey);
        if (!announced) {
            registrationPointerNames.remove(service.registrationPointerName());
            final String msg = "Found conflicts while announcing " + toRegister + " on network";
            LOGGER.warning(msg);
            throw new IOException(msg);
        }
        LOGGER.info(() -> "Registered " + toRegister + ON_DOMAIN);
        final RegisteredService rservice = new RegisteredServiceImpl(toRegister, this);
        registered.put(serviceKey, rservice);
        return rservice;
    }

    @Override
    public final void resetBrowsingInterval() {
        rBrowser.resetQueryInterval();
        sBrowser.resetQueryInterval();
    }

    @Override
    public final Optional<ResolvedService> resolve(final String instanceName, final String registrationType,
            final Duration timeout) {
        cache.clean(now());
        final ResolvableService service = new ResolvableService(instanceName, registrationType);
        LOGGER.fine(() -> "Resolving " + service.toString() + ON_DOMAIN);
        try {
            final boolean resolved = service.resolve(this, timeout);
            if (resolved) {
                LOGGER.info(() -> "Resolved " + service.toString() + ON_DOMAIN);
                return Optional.of(service);
            }
        } catch (final InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while waiting for response", e);
            Thread.currentThread().interrupt();
        }
        LOGGER.info(() -> "Could not resolve " + service.toString() + ON_DOMAIN);
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
    final void reannounce(final RegisteredService service, final Duration ttl) throws IOException {
        announcer.reannounce(service, ttl);
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

    /**
     * Adds a DNS record type A corresponding to an answer to the given question if it exits.
     *
     * @param query query being answered
     * @param question host IPv4 question (from the query)
     * @param builder builder
     * @param now current instant
     */
    private void addIpv4Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        announcingOrRegistered()
            .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
            .filter(h -> h.ipv4Address().isPresent())
            .forEach(s -> {
                final InetAddress addr = s.ipv4Address().get();
                builder
                    .addAnswer(query, new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
            });
    }

    /**
     * Adds a DNS record type AAAA corresponding to an answer to the given question if it exits.
     *
     * @param query query being answered
     * @param question host IPv6 question (from the query)
     * @param builder builder
     * @param now current instant
     */
    private void addIpv6Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        announcingOrRegistered()
            .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
            .filter(h -> h.ipv6Address().isPresent())
            .forEach(s -> {
                final InetAddress addr = s.ipv6Address().get();
                builder
                    .addAnswer(query, new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
            });
    }

    /**
     * Adds a DNS record type PTR corresponding to an answer to the given question if it exits.
     *
     * @param query query being answered
     * @param question discovery or service registration pointer question (from the query)
     * @param builder builder
     * @param now current instant
     */
    private void addPtrAnswer(final DnsMessage query, final DnsQuestion question, final Builder builder,
            final Instant now) {
        if (question.name().equals(RT_DISCOVERY)) {
            for (final String rpn : registrationPointerNames) {
                builder.addAnswer(query, new PtrRecord(RT_DISCOVERY, CLASS_IN, TTL, now, rpn));
            }
        } else {
            announcingOrRegistered().forEach(service -> {
                if (question.name().equalsIgnoreCase(service.registrationPointerName())) {
                    builder
                        .addAnswer(query, new PtrRecord(service.registrationPointerName(), CLASS_IN, TTL, now,
                                                        service.name()));
                }
            });
        }
    }

    /**
     * Adds DNS record types SRV, TXT, A and AAAA corresponding to an answer to the given question if it exits.
     *
     * @param query query being answered
     * @param question service resolution question (from the query)
     * @param service service matching the query
     * @param builder builder
     * @param now current instant
     */
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
            service
                .ipv4Address()
                .ifPresent(a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
            service
                .ipv6Address()
                .ifPresent(a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
        }
    }

    /**
     * @return stream of announcing or registered services.
     */
    private Stream<Service> announcingOrRegistered() {
        return Stream.concat(announcing.values().stream(), registered.values().stream());
    }

    /**
     * Returns the announcing or registered service for the given name.
     *
     * @param name name of the sevice
     * @return announcing or registered service or null if not found.
     */
    private Service announcingOrRegistered(final String name) {
        final String lcName = toLowerCase(name);
        final RegisterableService aService = announcing.get(lcName);
        if (aService != null) {
            return aService;
        }
        final RegisteredService rService = registered.get(lcName);
        if (rService != null) {
            return rService;
        }
        LOGGER.fine(() -> "No announcing or registered service matching: " + name);
        return null;

    }

    /**
     * Builds a response to the given query.
     *
     * @param query DNS query
     * @return DNS response
     */
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

                final Service service = announcingOrRegistered(question.name());
                if (service != null) {
                    addServiceAnswer(query, question, service, builder, now);
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
     * @throws IOException if service instance name cannot be made unique
     */
    private RegisterableService checkInstanceName(final RegisterableService service, final boolean allowNameChange)
            throws IOException {
        final String hostname = service.hostname();
        final int port = service.port();
        boolean collision = false;
        RegisterableService result = service;
        do {
            collision = false;
            final Instant now = now();

            /* check own services. */
            final Service own = announcingOrRegistered(result.name());
            if (own != null) {
                final String otherHostname = own.hostname();
                collision = own.port() != port || !otherHostname.equals(hostname);
                if (collision) {
                    final String msg = "Own registered service collision: " + own;
                    result = tryResolveCollision(result, allowNameChange, msg);
                }
            }

            /* check cache. */
            final Optional<SrvRecord> rec = cache
                .entries(result.name())
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

        } while (collision);
        return result;
    }

    /**
     * Handles the given query.
     *
     * @param query query
     */
    private void handleQuery(final DnsMessage query) {
        LOGGER.fine(() -> "Trying to respond to " + query);
        final DnsMessage response = buildResponse(query);
        if (response.answers().isEmpty()) {
            LOGGER.fine(() -> "Ignoring query");
        } else {
            LOGGER.fine(() -> "Responding with " + response);
            channel.send(response);
        }
    }

    /**
     * Handles the given response.
     *
     * @param response response
     */
    private void handleResponse(final DnsMessage response) {
        LOGGER.fine(() -> "Handling response " + response);
        for (final DnsRecord record : response.answers()) {
            if (record.ttl().isZero()) {
                cache.expire(record);
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

    /**
     * Tries to resolve a service instance name collision by changing its instance name if allowed.
     *
     * @param service service
     * @param allowNameChange {@code true} if {@link Service#instanceName() instance name} can be changed to be
     *            made unique
     * @param msg message to log if collision cannot be resolved
     * @return a new service with the same properties as the given one except for its instance name
     * @throws IOException if {@code allowNameChange} is {@code false}
     */
    private RegisterableService tryResolveCollision(final RegisterableService service,
            final boolean allowNameChange, final String msg) throws IOException {
        if (!allowNameChange) {
            throw new IOException(msg);
        }
        LOGGER.info(msg);
        final String instanceName = changeInstanceName(service.instanceName());
        return new RegisterableServiceImpl(instanceName, service);
    }

}
