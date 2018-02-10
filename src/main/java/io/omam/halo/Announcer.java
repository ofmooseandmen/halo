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
import static io.omam.halo.MulticastDns.FLAGS_AA;
import static io.omam.halo.MulticastDns.PROBE_NUM;
import static io.omam.halo.MulticastDns.PROBING_INTERVAL;
import static io.omam.halo.MulticastDns.PROBING_TIMEOUT;
import static io.omam.halo.MulticastDns.TTL;
import static io.omam.halo.MulticastDns.TYPE_ANY;
import static io.omam.halo.MulticastDns.TYPE_SRV;
import static io.omam.halo.MulticastDns.uniqueClass;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.DnsMessage.Builder;

/**
 * Announces {@link Service}s on the network.
 */
final class Announcer implements AutoCloseable {

    /**
     * Announce task.
     */
    private static final class AnnounceTask implements Callable<Void> {

        /** the service to announce. */
        private final Service s;

        /** halo helper. */
        private final HaloHelper halo;

        /**
         * Constructor.
         *
         * @param service service to announce
         * @param haloHelper halo helper
         */
        AnnounceTask(final Service service, final HaloHelper haloHelper) {
            s = service;
            halo = haloHelper;
        }

        @Override
        public final Void call() throws Exception {
            final Instant now = halo.now();
            final String hostname = s.hostname();
            final Attributes attributes = s.attributes();
            final String serviceName = s.serviceName();
            final short unique = uniqueClass(CLASS_IN);
            /* no stamp when announcing, TTL will be the one given. */
            final Optional<Instant> stamp = Optional.empty();
            final Builder b = DnsMessage
                .response(FLAGS_AA)
                .addAnswer(new PtrRecord(s.registrationPointerName(), CLASS_IN, TTL, now, s.instanceName()), stamp)
                .addAnswer(new SrvRecord(serviceName, unique, TTL, now, s.port(), hostname), stamp)
                .addAnswer(new TxtRecord(serviceName, unique, TTL, now, attributes), stamp);

            s.ipv4Address().ifPresent(a -> b.addAnswer(new AddressRecord(hostname, unique, TTL, now, a), stamp));

            s.ipv6Address().ifPresent(a -> b.addAnswer(new AddressRecord(hostname, unique, TTL, now, a), stamp));

            halo.sendMessage(b.get());
            return null;
        }
    }

    /**
     * Listener for response during probe.
     */
    private static final class ProbeListener implements ResponseListener {

        /** condition to signal if matching response is received. */
        private final Condition cdt;

        /** lock. */
        private final Lock lock;

        /** whether a response matching the probe query was received. */
        private final AtomicBoolean match;

        /** the service being probed. */
        private final Service s;

        /**
         * Constructor.
         *
         * @param service the service being probed
         */
        ProbeListener(final Service service) {
            s = service;
            match = new AtomicBoolean(false);
            lock = new ReentrantLock();
            cdt = lock.newCondition();
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void responseReceived(final DnsMessage response, final HaloHelper halo) {
            lock.lock();
            LOGGER.fine(() -> "Handling " + response);
            try {
                if (response.answers().stream().anyMatch(
                        a -> a.name().equalsIgnoreCase(s.serviceName()) && a.type() == TYPE_SRV)) {
                    match.set(true);
                    LOGGER.fine("Received matching response");
                    cdt.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * Awaits for a response matching the probe query.
         * <p>
         * A response matches the probe query iff it relates to the {@link Service#serviceName() service} being
         * probed and it contains a {@link SrvRecord SRV record}.
         *
         * @return {@code true} iff a response matching the probe query has been received before the
         *         {@link MulticastDns#PROBING_TIMEOUT probing timeout} has elapsed
         */
        @SuppressWarnings("synthetic-access")
        final boolean await() {
            lock.lock();
            boolean signalled = false;
            try {
                final Timeout timeout = Timeout.of(PROBING_TIMEOUT);
                Duration remaining = timeout.remaining();
                while (!match.get() && !remaining.isZero()) {
                    signalled = cdt.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                    remaining = timeout.remaining();
                }
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for match", e);
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
            if (!signalled) {
                LOGGER.fine(() -> "No matching response received within " + PROBING_TIMEOUT);
            }
            return match.get();
        }

    }

    /**
     * Probe task.
     */
    private static final class ProbeTask implements Callable<Void> {

        /** the service being probed. */
        private final Service s;

        /** halo helper. */
        private final HaloHelper halo;

        /**
         * Constructor.
         *
         * @param service the service being probed
         * @param haloHelper halo helper
         */
        ProbeTask(final Service service, final HaloHelper haloHelper) {
            s = service;
            halo = haloHelper;
        }

        @Override
        public final Void call() throws Exception {
            final Instant now = halo.now();
            final String hostname = s.hostname();
            final String serviceName = s.serviceName();
            final Builder b = DnsMessage
                .query()
                .addQuestion(new DnsQuestion(hostname, TYPE_ANY, CLASS_IN))
                .addQuestion(new DnsQuestion(serviceName, TYPE_ANY, CLASS_IN))
                .addAuthority(new SrvRecord(serviceName, CLASS_IN, TTL, now, s.port(), hostname));

            s.ipv4Address().ifPresent(a -> b.addAuthority(new AddressRecord(hostname, CLASS_IN, TTL, now, a)));
            s.ipv6Address().ifPresent(a -> b.addAuthority(new AddressRecord(hostname, CLASS_IN, TTL, now, a)));

            halo.sendMessage(b.get());
            return null;
        }
    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Announcer.class.getName());

    /** halo helper. */
    private final HaloHelper halo;

    /** scheduled executor service. */
    private final HaloScheduledExecutorService ses;

    /**
     *
     * Constructor.
     *
     * @param haloHelper halo helper
     */
    Announcer(final HaloHelper haloHelper) {
        halo = haloHelper;
        ses = new HaloScheduledExecutorService("announcer");
    }

    @Override
    public final void close() {
        ses.shutdownNow();
    }

    /**
     * Probes the network for the hostname and port of the given service and announces the service if no conflict
     * have been discovered.
     * <p>
     * This method does not check whether the service has already been announced.
     *
     * @param service service
     * @return {@code true} iff no conflicts have been discovered while probing and the service was successfully
     *         announced on the network
     * @throws IOException if an exception occurs while probing
     */
    final boolean announce(final Service service) throws IOException {
        LOGGER.fine(() -> "Start probing for " + service);
        final ProbeListener listener = new ProbeListener(service);
        halo.addResponseListener(listener);
        final ProbeTask task = new ProbeTask(service, halo);
        try {
            final Collection<ScheduledFuture<Void>> probes = ses.scheduleBatch(task, PROBE_NUM, PROBING_INTERVAL);
            final boolean conflictFree = !listener.await();
            probes.forEach(p -> p.cancel(true));
            LOGGER.fine(() -> "Done probing for " + service + "; found conflicts? " + !conflictFree);
            if (conflictFree) {
                /* announce */
                LOGGER.fine(() -> "Announcing " + service);
                ses.submit(new AnnounceTask(service, halo)).get();
                LOGGER.info(() -> "Announced " + service);
            }
            return conflictFree;
        } catch (final ExecutionException e) {
            throw new IOException(e);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while probing service", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            halo.removeResponseListener(listener);
        }
    }

}
