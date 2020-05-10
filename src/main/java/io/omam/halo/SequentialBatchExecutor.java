/*
Copyright 2020-2020 Cedric Liegeois

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory) Single thread scheduled
 * executor} for sending batches of messages on the network: each batch is associated to a unique name, all tasks
 * associated to the same name are executed in order.
 */
final class SequentialBatchExecutor {

    /**
     * A future batch of task.
     */
    static final class FutureBatch {

        /** future representing the first task of the batch. */
        private final Future<Void> first;

        /** futures representing the next tasks of the batch. */
        private final Collection<ScheduledFuture<Void>> nexts;

        /**
         * Constructor.
         *
         * @param aFirst future representing the first task of the batch
         * @param someNexts futures representing the next tasks of the batch
         */
        FutureBatch(final Future<Void> aFirst, final Collection<ScheduledFuture<Void>> someNexts) {
            first = aFirst;
            nexts = someNexts;
        }

        /**
         * Awaits until all tasks of this batch have been executed.
         *
         * @throws InterruptedException if interrupted while waiting
         * @throws ExecutionException if task failed to execute
         */
        final void awaitAll() throws InterruptedException, ExecutionException {
            awaitFirst();
            for (final ScheduledFuture<Void> next : nexts) {
                next.get();
            }
        }

        /**
         * Awaits until the first task of this batch has been executed.
         *
         * @throws InterruptedException if interrupted while waiting
         * @throws ExecutionException if the task failed to execute
         */
        final void awaitFirst() throws InterruptedException, ExecutionException {
            first.get();
        }

        /**
         * Cancels all the task of this batch.
         */
        final void cancelAll() {
            first.cancel(true);
            nexts.forEach(f -> f.cancel(true));
        }

    }

    /** scheduled executor service. */
    private final ScheduledExecutorService executor;

    /** current batch indexed by name. */
    private final Map<String, FutureBatch> batches;

    /** lock. */
    private final Lock lock;

    /**
     * Constructor.
     *
     * @param name name of this executor
     */
    SequentialBatchExecutor(final String name) {
        executor = Executors.newSingleThreadScheduledExecutor(new HaloThreadFactory(name));
        batches = new HashMap<>();
        lock = new ReentrantLock();
    }

    /**
     * Schedules the {@code size} execution of the given task. The first execution will happen after the given
     * delay, and every subsequent execution will be spaced by the given delay.
     *
     * @param name name of the batch
     * @see ScheduledExecutorService#schedule(Callable, long, TimeUnit)
     * @param task task to execute
     * @param size number of time the task shall be executed
     * @param delay between consecutive executions
     * @return the future representing the batch
     * @throws InterruptedException if interrupted while waiting for the current batch associated to the given name
     *             to complete
     * @throws ExecutionException if any task of the current batch have failed
     */
    final FutureBatch scheduleBatch(final String name, final Callable<Void> task, final int size,
            final Duration delay) throws InterruptedException, ExecutionException {
        lock.lock();
        try {
            final FutureBatch currentBatch = batches.get(name);
            if (currentBatch != null) {
                currentBatch.awaitAll();
            }
            final Future<Void> first = executor.submit(task);
            final Collection<ScheduledFuture<Void>> next = new ArrayList<>();
            for (int i = 1; i < size; i++) {
                final long currentDelay = i * delay.toMillis();
                next.add(executor.schedule(task, currentDelay, TimeUnit.MILLISECONDS));
            }
            final FutureBatch batch = new FutureBatch(first, next);
            batches.put(name, batch);
            return batch;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @see ScheduledExecutorService#shutdownNow()
     */
    final void shutdownNow() {
        executor.shutdownNow();
    }

}
