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
package io.omam.zeroconf;

import static io.omam.zeroconf.MulticastDns.CLASS_IN;
import static io.omam.zeroconf.MulticastDns.FLAGS_AA;
import static io.omam.zeroconf.MulticastDns.PROBE_INTERVAL;
import static io.omam.zeroconf.MulticastDns.PROBE_NUM;
import static io.omam.zeroconf.MulticastDns.TTL;
import static io.omam.zeroconf.MulticastDns.TYPE_ANY;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.zeroconf.DnsMessage.Builder;

/**
 * Announces {@link Service}s on the network.
 * <p>
 * Announcing a service first requires to probe the network for the hostname and port of {@link Service service}s.
 */
@SuppressWarnings("javadoc")
final class Announcer implements Closeable {

    /**
     * Announcing task.
     */
    private static final class AnnouncingTask implements Callable<Void> {

        /** the service being probed. */
        private final Service s;

        /** zeroconf helper. */
        private final ZeroconfHelper zc;

        AnnouncingTask(final Service service, final ZeroconfHelper zeroconf) {
            s = service;
            zc = zeroconf;
        }

        @Override
        public final Void call() throws Exception {
            final Instant now = zc.now();
            final String hostname = s.hostname().orElseThrow(() -> new IOException("Unknown service hostname"));
            final Attributes attributes =
                    s.attributes().orElseThrow(() -> new IOException("Unknown service attributes"));
            final String serviceName = s.serviceName();
            final Builder b = DnsMessage
                .response(FLAGS_AA)
                .addAnswer(new PtrRecord(s.registrationPointerName(), CLASS_IN, TTL, now, s.instanceName()),
                        Optional.empty())
                .addAnswer(new SrvRecord(serviceName, CLASS_IN, TTL, now, s.port(), s.priority(), hostname,
                                         s.weight()),
                        Optional.empty())
                .addAnswer(new TxtRecord(serviceName, CLASS_IN, TTL, now, attributes), Optional.empty());

            s.ipv4Address().ifPresent(
                    a -> b.addAnswer(AddressRecord.ipv4(serviceName, CLASS_IN, TTL, now, a), Optional.empty()));

            s.ipv6Address().ifPresent(
                    a -> b.addAnswer(AddressRecord.ipv6(serviceName, CLASS_IN, TTL, now, a), Optional.empty()));

            zc.sendMessage(b.get());
            return null;
        }
    }

    private static final class ProbeListener implements ResponseListener {

        private final Condition cdt;

        private final Lock lock;

        private final AtomicBoolean match;

        private final Service s;

        ProbeListener(final Service service) {
            s = service;
            match = new AtomicBoolean(false);
            lock = new ReentrantLock();
            cdt = lock.newCondition();
        }

        @Override
        public final void responseReceived(final DnsMessage response, final ZeroconfHelper zc) {
            if (response.answers().stream().anyMatch(a -> a.name().equalsIgnoreCase(s.serviceName()))) {
                match.set(true);
                lock.lock();
                try {
                    cdt.signalAll();
                } finally {
                    lock.unlock();
                }
            }

        }

        @SuppressWarnings("synthetic-access")
        final boolean await() {
            lock.lock();
            boolean signalled = false;
            try {
                final CountDownDuration cdd = CountDownDuration.of(PROBE_INTERVAL).start();
                Duration remaining = cdd.remaining();
                while (!match.get() && !remaining.isZero()) {
                    signalled = cdt.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                    remaining = cdd.remaining();
                }
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for match", e);
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
            if (!signalled) {
                LOGGER.fine(() -> "No matching response received within " + PROBE_INTERVAL);
            }
            return match.get();
        }

    }

    /**
     * Probe task.
     */
    private static final class ProbingTask implements Callable<Void> {

        /** the service being probed. */
        private final Service s;

        /** zeroconf helper. */
        private final ZeroconfHelper zc;

        ProbingTask(final Service service, final ZeroconfHelper zeroconf) {
            s = service;
            zc = zeroconf;
        }

        @Override
        public final Void call() throws Exception {
            final Instant now = zc.now();
            final String hostname = s.hostname().orElseThrow(() -> new IOException("Unknown service hostname"));
            final Attributes attributes =
                    s.attributes().orElseThrow(() -> new IOException("Unknown service attributes"));
            final String serviceName = s.serviceName();
            final Builder b = DnsMessage
                .query()
                .addQuestion(new DnsQuestion(hostname, TYPE_ANY, CLASS_IN))
                .addAuthority(new PtrRecord(s.registrationPointerName(), CLASS_IN, TTL, now, s.instanceName()))
                .addAuthority(new SrvRecord(serviceName, CLASS_IN, TTL, now, s.port(), s.priority(), hostname,
                                            s.weight()))
                .addAuthority(new TxtRecord(serviceName, CLASS_IN, TTL, now, attributes));

            s.ipv4Address().ifPresent(a -> b.addAuthority(AddressRecord.ipv4(serviceName, CLASS_IN, TTL, now, a)));

            s.ipv6Address().ifPresent(a -> b.addAuthority(AddressRecord.ipv6(serviceName, CLASS_IN, TTL, now, a)));

            zc.sendMessage(b.get());
            return null;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Announcer.class.getName());

    /** zeroconf helper. */
    private final ZeroconfHelper zc;

    /** scheduled executor service. */
    private final ScheduledExecutorService ses;

    /**
     *
     * Constructor.
     *
     * @param zeroconf zeroconf helper
     */
    Announcer(final ZeroconfHelper zeroconf) {
        zc = zeroconf;
        ses = Executors.newSingleThreadScheduledExecutor(new ZeroconfThreadFactory("announcer"));
    }

    @Override
    public final void close() {
        ses.shutdownNow();
    }

    /**
     * Probes the network for the hostname and port of the given service and announces the service if no conflict
     * have been discovered.
     *
     * @param service service
     * @return {@code true} iff no conflicts have been discovered while probing and the service was successfully
     *         announced on the network
     * @throws IOException
     */
    final boolean announce(final Service service) throws IOException {
        LOGGER.fine(() -> "Start probing for " + service);
        final ProbeListener listener = new ProbeListener(service);
        zc.addResponseListener(listener);
        final ProbingTask task = new ProbingTask(service, zc);
        boolean conflictFree = true;
        try {
            for (int i = 0; i < PROBE_NUM && conflictFree; i++) {
                final Future<?> probe = ses.schedule(task, PROBE_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
                probe.get();
                conflictFree = !listener.await();
            }
            final boolean result = conflictFree;
            LOGGER.fine(() -> "Done probing for " + service + "; found conflicts? " + result);
            if (result) {
                /* announce */
                LOGGER.fine(() -> "Announcing " + service);
                ses.submit(new AnnouncingTask(service, zc)).get();
                LOGGER.info(() -> "Announced " + service);
            }
            return result;
        } catch (final ExecutionException e) {
            throw new IOException(e);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while probing service", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            zc.removeResponseListener(listener);
        }

    }

}
