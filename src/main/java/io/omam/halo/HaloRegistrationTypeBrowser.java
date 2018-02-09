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
import static io.omam.halo.MulticastDns.DOMAIN;
import static io.omam.halo.MulticastDns.QUERYING_INTERVAL;
import static io.omam.halo.MulticastDns.RT_DISCOVERY;
import static io.omam.halo.MulticastDns.TTL;
import static io.omam.halo.MulticastDns.TYPE_PTR;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.omam.halo.DnsMessage.Builder;

/**
 * Halo registration type browser.
 * <p>
 * Network (local domain) is queried at regular intervals with a {@link MulticastDns#RT_DISCOVERY} question.
 */
final class HaloRegistrationTypeBrowser {

    /**
     * Task to query the cache and network about registration types.
     */
    @SuppressWarnings("synthetic-access")
    private final class QueryingTask implements Runnable, ResponseListener {

        /**
         * Constructor.
         */
        QueryingTask() {
            // empty.
        }

        @Override
        public final void responseReceived(final DnsMessage response, final HaloHelper haloHelper) {
            LOGGER.fine(() -> "Handling " + response);
            response
                .answers()
                .stream()
                .filter(r -> r.type() == TYPE_PTR && r.name().equalsIgnoreCase(RT_DISCOVERY))
                .map(r -> (PtrRecord) r)
                .forEach(this::handlePointer);
        }

        @Override
        public final void run() {
            final Builder b = DnsMessage.query().addQuestion(new DnsQuestion(RT_DISCOVERY, TYPE_PTR, CLASS_IN));
            final Instant now = halo.now();
            final Optional<Instant> onow = Optional.of(now);
            for (final String rt : rts) {
                final String pointer = rt + DOMAIN + ".";
                b.addAnswer(new PtrRecord(RT_DISCOVERY, CLASS_IN, TTL, now, pointer), onow);
            }
            halo.sendMessage(b.get());
        }

        /**
         * Handles a response PTR record.
         *
         * @param p pointer
         */
        private void handlePointer(final PtrRecord p) {
            final int end = p.target().indexOf(DOMAIN);
            if (end != -1) {
                final String rt = p.target().substring(0, end);
                if (!rts.contains(rt)) {
                    LOGGER.fine(() -> "Discovered new registration type [" + p.target() + "]");
                    rts.add(rt);
                    listeners.forEach(l -> l.registrationTypeDiscovered(rt));
                }
            } else {
                LOGGER.warning(() -> "Ignored pointer to [" + p.target() + "]");
            }
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(HaloRegistrationTypeBrowser.class.getName());

    /** halo helper. */
    private final HaloHelper halo;

    /** all listeners. */
    private final List<RegistrationTypeBrowserListener> listeners;

    /** all discovered registration types. */
    private final List<String> rts;

    /** querying task. */
    private final QueryingTask qt;

    /** discoverer executor service. */
    private final ScheduledExecutorService qes;

    /** future representing the querying task. */
    private Future<?> future;

    /**
     * Constructor.
     *
     * @param haloHelper halo helper
     */
    HaloRegistrationTypeBrowser(final HaloHelper haloHelper) {
        halo = haloHelper;
        listeners = new CopyOnWriteArrayList<>();
        rts = new CopyOnWriteArrayList<>();
        qt = new QueryingTask();
        qes = Executors.newSingleThreadScheduledExecutor(new HaloThreadFactory("registration-discoverer"));
        future = null;
    }

    /**
     * Adds the given listener.
     * <p>
     * Registration types already discovered are notified to the listener.
     *
     * @param listener listener
     */
    final void addListener(final RegistrationTypeBrowserListener listener) {
        Objects.requireNonNull(listener);
        rts.forEach(listener::registrationTypeDiscovered);
        listeners.add(listener);
    }

    /**
     * Removes the given listener.
     *
     * @param listener listener
     */
    final void removeListener(final RegistrationTypeBrowserListener listener) {
        Objects.requireNonNull(listener);
        listeners.remove(listener);
    }

    /**
     * Starts browsing for registration types.
     * <p>
     * If this browser is already started this method has no effect.
     */
    final void start() {
        if (future == null) {
            halo.addResponseListener(qt);
            future = qes.scheduleAtFixedRate(qt, QUERYING_INTERVAL.toMillis(), QUERYING_INTERVAL.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops browsing for services.
     */
    final void stop() {
        halo.removeResponseListener(qt);
        if (future != null) {
            future.cancel(true);
        }
        qes.shutdownNow();
    }

}
