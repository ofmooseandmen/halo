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

import static io.omam.halo.HaloProperties.RESOLUTION_TIMEOUT;
import static io.omam.halo.MulticastDnsSd.CLASS_IN;
import static io.omam.halo.MulticastDnsSd.DOMAIN;
import static io.omam.halo.MulticastDnsSd.TYPE_PTR;
import static io.omam.halo.MulticastDnsSd.toLowerCase;
import static java.util.stream.Collectors.groupingBy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.DnsMessage.Builder;

/**
 * Halo service browser by registration type.
 * <p>
 * Network (local domain) is queried at regular intervals for named services.
 */
final class HaloServiceBrowser extends HaloBrowser {

    /**
     * Task to query the cache and network about service of the browsed registration type.
     */
    @SuppressWarnings("synthetic-access")
    private final class QueryTask implements Callable<Void> {

        /**
         * Constructor.
         */
        QueryTask() {
            // empty.
        }

        @Override
        public final Void call() {
            final Set<String> rpns = listeners.keySet();
            final Builder builder = DnsMessage.query();
            for (final String rpn : rpns) {
                builder.addQuestion(new DnsQuestion(rpn, TYPE_PTR, CLASS_IN));
            }
            halo.sendMessage(builder.get());
            return null;
        }

    }

    /**
     * Task to resolve services that have been discovered during query.
     */
    @SuppressWarnings("synthetic-access")
    private final class ResolveTask implements Runnable {

        /** registration pointer name of the service being resolved. */
        private final String rpn;

        /** service to resolve. */
        private final ResolvableService service;

        /**
         * Constructor.
         *
         * @param registrationPointerName registration pointer name of the service being resolved
         * @param aService service to resolve
         */
        ResolveTask(final String registrationPointerName, final ResolvableService aService) {
            rpn = registrationPointerName;
            service = aService;
        }

        @Override
        public final void run() {
            try {
                final boolean resolved = service.resolve(halo, RESOLUTION_TIMEOUT);
                if (resolved) {
                    if (alreadyResolved()) {
                        LOGGER
                            .fine(() -> "Ignoring already resolved "
                                + service
                                + " attributes: "
                                + service.attributes());
                    } else {
                        final String skey = toLowerCase(service.name());
                        final boolean added = services.get(rpn).get(skey) == null;
                        services.get(rpn).put(skey, service);
                        final Collection<ServiceBrowserListener> rlisteners = listeners.get(rpn);
                        if (added) {
                            LOGGER.info(() -> "Resolved (added) " + service);
                            rlisteners.forEach(l -> l.serviceAdded(service));
                        } else {
                            LOGGER.info(() -> "Resolved (updated) " + service);
                            rlisteners.forEach(l -> l.serviceUpdated(service));
                        }
                    }
                } else {
                    LOGGER.warning(() -> "Could not resolve " + service);
                }
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for response", e);
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Determines whether this service has already been resolved - the attribute of service other than
         * name/registration type can be updated.
         *
         * @return true if already resolved.
         */
        private boolean alreadyResolved() {
            final String skey = toLowerCase(service.name());
            final ResolvableService existing = services.get(rpn).get(skey);
            if (existing == null) {
                return false;
            }
            return service.hostname().equals(existing.hostname())
                && service.ipv4Address().equals(existing.ipv4Address())
                && service.ipv6Address().equals(existing.ipv6Address())
                && service.port() == existing.port()
                && service.attributes().equals(existing.attributes());
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(HaloServiceBrowser.class.getName());

    /** halo helper */
    private final HaloHelper halo;

    /** listeners, indexed by registration pointer name. */
    private final Map<String, Collection<ServiceBrowserListener>> listeners;

    /**
     * resolved services, indexed by registration pointer name, indexed by service name.
     */
    private final Map<String, Map<String, ResolvableService>> services;

    /** single thread executor in which all requests are executed. */
    private final ExecutorService executor;

    /**
     * Constructor.
     *
     * @param haloHelper halo helper
     */
    HaloServiceBrowser(final HaloHelper haloHelper) {
        super("service-discoverer", haloHelper);
        halo = haloHelper;
        listeners = new ConcurrentHashMap<>();
        services = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadExecutor(new HaloThreadFactory("service-resolver"));
    }

    /**
     * Returns the registration pointer name for the given registration type.
     *
     * @param registrationType registration type
     * @return registration pointer name (lower case)
     */
    private static String toRpn(final String registrationType) {
        return toLowerCase(registrationType + DOMAIN + ".");
    }

    @Override
    public final void responseReceived(final DnsMessage response, final HaloHelper haloHelper) {
        pointers(response).forEach((rpn, ptr) -> executor.execute(() -> handleResponse(rpn, ptr)));
    }

    /**
     * Adds the given listener for the given service registration type.
     * <p>
     * Services already discovered and resolved are notified to the listener.
     *
     * @param registrationType service registration type
     * @param listener listener
     */
    final void addListener(final String registrationType, final ServiceBrowserListener listener) {
        Objects.requireNonNull(registrationType);
        Objects.requireNonNull(listener);
        final String rpn = toRpn(registrationType);
        final Collection<ServiceBrowserListener> rls =
                listeners.computeIfAbsent(rpn, k -> new ConcurrentLinkedQueue<>());
        final Map<String, ResolvableService> resolved =
                services.computeIfAbsent(rpn, k -> new ConcurrentHashMap<>());
        resolved.values().forEach(listener::serviceAdded);
        rls.add(listener);
    }

    /**
     * Removes the given listener for the given registration type.
     *
     * @param registrationType service registration type
     * @param listener listener
     */
    final void removeListener(final String registrationType, final ServiceBrowserListener listener) {
        Objects.requireNonNull(registrationType);
        Objects.requireNonNull(listener);
        final String rpn = toRpn(registrationType);
        final Collection<ServiceBrowserListener> rls = listeners.get(rpn);
        if (rls == null) {
            LOGGER.warning(() -> registrationType + " is not being browsed.");
        } else if (rls.size() == 1) {
            listeners.remove(rpn);
        } else {
            rls.remove(listener);
        }
    }

    @Override
    protected final void doClose() {
        executor.shutdownNow();
    }

    @Override
    protected final Callable<Void> queryTask() {
        return new QueryTask();
    }

    /**
     * Handles an expired PTR record.
     *
     * @param rservices already resolved services for the registration type
     * @param rlisteners listeners for the registration type
     * @param serviceName service name associated to the expired PTR record
     */
    private void handlePtrExpiry(final Map<String, ResolvableService> rservices,
            final Collection<ServiceBrowserListener> rlisteners, final String serviceName) {
        final String skey = toLowerCase(serviceName);
        final ResolvableService service = rservices.remove(skey);
        if (service != null) {
            LOGGER.info(() -> "Service [" + serviceName + "] has been removed");
            rlisteners.forEach(l -> l.serviceRemoved(service));
        }
    }

    /**
     * Handles answers for the given registration pointer name.
     *
     * @param rpn registration pointer name
     * @param pointers PTR records
     */
    private void handleResponse(final String rpn, final Collection<PtrRecord> pointers) {
        final Map<String, ResolvableService> rservices = services.get(rpn);
        final Collection<ServiceBrowserListener> rlisteners = listeners.get(rpn);
        final Instant now = halo.now();
        for (final PtrRecord ptr : pointers) {
            final String serviceName = ptr.target();
            if (ptr.isExpired(now)) {
                handlePtrExpiry(rservices, rlisteners, serviceName);
            } else {
                submitResolution(rpn, serviceName);
            }
        }
    }

    /**
     * Extracts all PTR records related to browsed service types.
     *
     * @param response DNS response
     * @return map of PTR records indexed by browsed registration pointer name in lower case
     */
    private Map<String, List<PtrRecord>> pointers(final DnsMessage response) {
        final Set<String> rpns = listeners.keySet();
        return response
            .answers()
            .stream()
            .filter(r -> r.type() == TYPE_PTR && rpns.contains(toLowerCase(r.name())))
            .map(r -> (PtrRecord) r)
            .collect(groupingBy(r -> toLowerCase(r.name())));
    }

    /**
     * Submits a task to resolve the given service
     *
     * @param rpn registration pointer name
     * @param serviceName service name
     */
    private void submitResolution(final String rpn, final String serviceName) {
        final Optional<String> instanceName = ResolvableService.instanceNameOf(serviceName);
        final Optional<String> registrationType = ResolvableService.registrationTypeOf(serviceName);
        if (instanceName.isPresent() && registrationType.isPresent()) {
            LOGGER.fine(() -> "Discovered [" + serviceName + "]");
            final ResolvableService service = new ResolvableService(instanceName.get(), registrationType.get());
            executor.execute(new ResolveTask(rpn, service));
        } else {
            LOGGER.warning(() -> "Could not decode service name [" + serviceName + "]");
        }
    }

}
