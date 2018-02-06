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

import static io.omam.halo.MulticastDns.CANCELLING_INTERVAL;
import static io.omam.halo.MulticastDns.CANCEL_NUM;
import static io.omam.halo.MulticastDns.CLASS_IN;
import static io.omam.halo.MulticastDns.FLAGS_AA;
import static io.omam.halo.MulticastDns.uniqueClass;
import static java.time.Duration.ZERO;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.DnsMessage.Builder;

/**
 * Cancels {@link Service}s on the network by sending goodbyes packet (i.e. packet with a TTL of 0).
 */
final class Canceller implements AutoCloseable {

    /**
     * Cancelling task.
     */
    private static final class CancellingTask implements Callable<Void> {

        /** the service to cancel. */
        private final Service s;

        /** halo helper. */
        private final HaloHelper halo;

        /**
         * Constructor.
         *
         * @param service service to cancel
         * @param haloHelper halo helper
         */
        CancellingTask(final Service service, final HaloHelper haloHelper) {
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
            final Builder b = DnsMessage
                .response(FLAGS_AA)
                .addAnswer(new PtrRecord(s.registrationPointerName(), CLASS_IN, ZERO, now, s.instanceName()),
                        Optional.empty())
                .addAnswer(new SrvRecord(serviceName, unique, ZERO, now, s.port(), hostname), Optional.empty())
                .addAnswer(new TxtRecord(serviceName, unique, ZERO, now, attributes), Optional.empty());

            s.ipv4Address().ifPresent(
                    a -> b.addAnswer(new AddressRecord(hostname, unique, ZERO, now, a), Optional.empty()));

            s.ipv6Address().ifPresent(
                    a -> b.addAnswer(new AddressRecord(hostname, unique, ZERO, now, a), Optional.empty()));

            halo.sendMessage(b.get());
            return null;
        }
    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Canceller.class.getName());

    /** halo helper. */
    private final HaloHelper halo;

    /** scheduled executor service. */
    private final ScheduledExecutorService ses;

    /**
     *
     * Constructor.
     *
     * @param haloHelper halo helper
     */
    Canceller(final HaloHelper haloHelper) {
        halo = haloHelper;
        ses = Executors.newSingleThreadScheduledExecutor(new HaloThreadFactory("canceler"));
    }

    @Override
    public final void close() {
        ses.shutdownNow();
    }

    /**
     * Cancels the given service.
     * <p>
     * This method does not check whether the service has been announced or already canceled.
     *
     * @param service service
     * @throws IOException if an exception occurs while probing
     */
    final void cancel(final Service service) throws IOException {
        LOGGER.fine(() -> "Canceling " + service);
        try {
            final CancellingTask task = new CancellingTask(service, halo);
            final List<Future<?>> cancels = new ArrayList<>();
            for (int i = 0; i < CANCEL_NUM; i++) {
                cancels.add(ses.schedule(task, CANCELLING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS));
            }
            for (final Future<?> cancel : cancels) {
                cancel.get();
            }
            LOGGER.info(() -> "Canceled " + service);
        } catch (final ExecutionException e) {
            throw new IOException(e);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while probing service", e);
            Thread.currentThread().interrupt();
        }

    }

}
