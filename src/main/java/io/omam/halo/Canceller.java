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

import static io.omam.halo.HaloProperties.CANCELLING_INTERVAL;
import static io.omam.halo.HaloProperties.CANCEL_NUM;
import static io.omam.halo.MulticastDnsSd.CLASS_IN;
import static io.omam.halo.MulticastDnsSd.FLAGS_AA;
import static io.omam.halo.MulticastDnsSd.uniqueClass;
import static java.time.Duration.ZERO;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.DnsMessage.Builder;

/**
 * Cancels {@link Service}s on the network by sending goodbyes packet (i.e. packet with a TTL of 0).
 */
final class Canceller implements AutoCloseable {

    /**
     * Cancel task.
     */
    private static final class CancelTask implements Callable<Void> {

        /** the service to cancel. */
        private final Service service;

        /** halo helper. */
        private final HaloHelper halo;

        /**
         * Constructor.
         *
         * @param aService service to cancel
         * @param haloHelper halo helper
         */
        CancelTask(final Service aService, final HaloHelper haloHelper) {
            service = aService;
            halo = haloHelper;
        }

        @Override
        public final Void call() throws Exception {
            final Instant now = halo.now();
            final String hostname = service.hostname();
            final Attributes attributes = service.attributes();
            final String serviceName = service.name();
            final short unique = uniqueClass(CLASS_IN);
            final Builder builder = DnsMessage
                .response(FLAGS_AA)
                .addAnswer(new PtrRecord(service.registrationPointerName(), CLASS_IN, ZERO, now, serviceName),
                        Optional.empty())
                .addAnswer(new SrvRecord(serviceName, unique, ZERO, now, service.port(), hostname),
                        Optional.empty())
                .addAnswer(new TxtRecord(serviceName, unique, ZERO, now, attributes), Optional.empty());

            service
                .ipv4Address()
                .ifPresent(a -> builder
                    .addAnswer(new AddressRecord(hostname, unique, ZERO, now, a), Optional.empty()));

            service
                .ipv6Address()
                .ifPresent(a -> builder
                    .addAnswer(new AddressRecord(hostname, unique, ZERO, now, a), Optional.empty()));

            halo.sendMessage(builder.get());
            return null;
        }
    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Canceller.class.getName());

    /** halo helper. */
    private final HaloHelper halo;

    /** executor. */
    private final SequentialBatchExecutor executor;

    /**
     *
     * Constructor.
     *
     * @param haloHelper halo helper
     * @param anExecutor executor
     */
    Canceller(final HaloHelper haloHelper, final SequentialBatchExecutor anExecutor) {
        halo = haloHelper;
        executor = anExecutor;
    }

    @Override
    public final void close() {
        executor.shutdownNow();
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
        LOGGER.fine(() -> "Cancelling " + service);
        try {
            final CancelTask task = new CancelTask(service, halo);
            executor.scheduleBatch(service.name(), task, CANCEL_NUM, CANCELLING_INTERVAL).awaitFirst();
            LOGGER.info(() -> "Cancelled " + service);
        } catch (final ExecutionException e) {
            throw new IOException(e);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while cancelling service", e);
            Thread.currentThread().interrupt();
        }

    }

}
