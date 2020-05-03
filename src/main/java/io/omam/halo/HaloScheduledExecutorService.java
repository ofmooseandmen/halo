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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory) Single threqd scheduled
 * executor} for querying/probing the network by batches.
 */
final class HaloScheduledExecutorService {

    /**
     * Tasks to schedule batches.
     */
    private static class SchedulingTask implements Callable<Void> {

        /** task to execute */
        private final Callable<Void> c;

        /** scheduled executor service. */
        private final ScheduledExecutorService ses;

        /** number of time the task shall be executed per batch. */
        private final int s;

        /** delay before the first execution and between each subsequent execution(s). */
        private final Duration d;

        /** duration between each batch. */
        private final Duration p;

        /**
         * Constructor.
         *
         * @param callable task to execute
         * @param scheduledExecutorService scheduled executor service
         * @param size number of time the task shall be executed per batch
         * @param delay delay before the first execution and between each subsequent execution(s)
         * @param pause duration between each batch
         */
        SchedulingTask(final Callable<Void> callable, final ScheduledExecutorService scheduledExecutorService,
                final int size, final Duration delay, final Duration pause) {
            c = callable;
            ses = scheduledExecutorService;
            s = size;
            d = delay;
            p = pause;
        }

        @Override
        public final Void call() throws Exception {
            for (int i = 0; i < s; i++) {
                final long dl = (i + 1) * d.toMillis();
                ses.schedule(c, dl, TimeUnit.MILLISECONDS);
            }
            ses.schedule(this, p.toMillis(), TimeUnit.MILLISECONDS);
            return null;
        }

    }

    /** the scheduled executor service. */
    private final ScheduledExecutorService ses;

    /**
     * Constructor.
     *
     * @param name thread name suffix
     */
    HaloScheduledExecutorService(final String name) {
        ses = Executors.newSingleThreadScheduledExecutor(new HaloThreadFactory(name));
    }

    /**
     * Schedules the {@code size} execution of the given task. The first execution will happen after the given
     * delay, and every subsequent execution will be spaced by the given delay.
     *
     * @see ScheduledExecutorService#schedule(Callable, long, TimeUnit)
     * @param callable task to execute
     * @param size number of time the task shall be executed
     * @param delay delay before the first execution and between each subsequent execution(s)
     * @return all scheduled futures
     */
    final Collection<ScheduledFuture<Void>> scheduleBatch(final Callable<Void> callable, final int size,
            final Duration delay) {
        final Collection<ScheduledFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final long d = (i + 1) * delay.toMillis();
            futures.add(ses.schedule(callable, d, TimeUnit.MILLISECONDS));
        }
        return futures;
    }

    /**
     * Schedules a infinite number of {@link #scheduleBatch(Callable, int, Duration) batch}es each spaced by the
     * given {@code pause}.
     *
     * @param callable task to execute
     * @param size number of time the task shall be executed per batch
     * @param delay delay before the first execution and between each subsequent execution(s)
     * @param pause duration between each batch
     * @return a Future to cancel the scheduling task
     */
    final Future<Void> scheduleBatches(final Callable<Void> callable, final int size, final Duration delay,
            final Duration pause) {
        return ses.submit(new SchedulingTask(callable, ses, size, delay, pause));
    }

    /**
     * @see ScheduledExecutorService#shutdownNow()
     */
    void shutdownNow() {
        ses.shutdownNow();
    }

    /**
     * Submits the given task for execution.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a Future representing pending completion of the task
     * @see ScheduledExecutorService#submit(Callable)
     */
    final <T> Future<T> submit(final Callable<T> task) {
        return ses.submit(task);
    }

}
