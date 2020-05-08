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
import java.util.logging.Logger;

/**
 * A {@link Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory) Single threqd scheduled
 * executor} for querying/probing the network by batches.
 */
final class HaloScheduledExecutorService {

    /**
     * Tasks that is submitted an increasing rate.
     */
    static final class IncreasingRateTask {

        /** callable. */
        private final IncreasingRateCallable callable;

        /** future. */
        private final Future<Void> future;

        /**
         * Constructor.
         *
         * @param aCallable callable
         * @param aFuture future
         */
        IncreasingRateTask(final IncreasingRateCallable aCallable, final Future<Void> aFuture) {
            callable = aCallable;
            future = aFuture;
        }

        /**
         * Attempts to cancel the execution of this task.
         *
         * @see Future#cancel(boolean)
         */
        final void cancel() {
            future.cancel(true);
        }

        /**
         * Resets the delay between two consecutive execution of the task to the initial value.
         */
        final void reset() {
            callable.reset();
        }

    }

    /**
     * Callable that executes a task and re-submit itself for run after an increasing delay.
     */
    private static class IncreasingRateCallable implements Callable<Void> {

        /** logger. */
        private static final Logger LOGGER = Logger.getLogger(IncreasingRateCallable.class.getName());

        /** task to execute */
        private final Callable<Void> task;

        /** scheduled executor service. */
        private final ScheduledExecutorService executor;

        /** initial delay between two consecutive executions of the task in milliseconds. */
        private final long initialDelay;

        /** current delay between two consecutive executions of the task in milliseconds. */
        private long currentDelay;

        /** increase factor of delay between two consecutive executions of the task. */
        private final int increaseFactor;

        /** maximum delay between two consecutive execution of the task in milliseconds. */
        private final long maxDelay;

        /**
         * Constructor.
         *
         * @param aTask task to execute
         * @param anExecutor scheduled executor service
         * @param anInitialDelay initial delay between two consecutive executions of the task
         * @param anIncreaseFactor increase factor of delay between two consecutive executions
         * @param aMaxDelay maximum delay between two consecutive execution of the task
         */
        IncreasingRateCallable(final Callable<Void> aTask, final ScheduledExecutorService anExecutor,
                final Duration anInitialDelay, final int anIncreaseFactor, final Duration aMaxDelay) {
            task = aTask;
            executor = anExecutor;
            initialDelay = anInitialDelay.toMillis();
            currentDelay = initialDelay;
            increaseFactor = anIncreaseFactor;
            maxDelay = aMaxDelay.toMillis();
        }

        @Override
        public final Void call() throws Exception {
            task.call();
            final long delay = currentDelay;
            LOGGER.fine(() -> "Scheduling next task in " + Duration.ofMillis(delay));
            currentDelay = Math.min(maxDelay, currentDelay * increaseFactor);
            executor.schedule(this, delay, TimeUnit.MILLISECONDS);
            return null;
        }

        /**
         * Resets the delay between two consecutive execution of the task to the initial value.
         */
        final void reset() {
            currentDelay = initialDelay;
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
     * @param task task to execute
     * @param size number of time the task shall be executed
     * @param delay delay before the first execution and between each subsequent execution(s)
     * @return all scheduled futures
     */
    final Collection<ScheduledFuture<Void>> scheduleBatch(final Callable<Void> task, final int size,
            final Duration delay) {
        final Collection<ScheduledFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final long currentDelay = (i + 1) * delay.toMillis();
            futures.add(ses.schedule(task, currentDelay, TimeUnit.MILLISECONDS));
        }
        return futures;
    }

    /**
     * Submits the given task for periodic execution starting after the given initial delay and then with an
     * increasing rate using the given base delay and increase factor.
     *
     * @param task task to run
     * @param initialDelay initial delay before first execution
     * @param baseDelay base delay between 2 executions
     * @param increaseFactor increase factor applied repeatedly to the base delay
     * @param maxDelay maximum delay
     * @return {@link IncreasingRateTask}
     */
    final IncreasingRateTask scheduleIncreasingly(final Callable<Void> task, final Duration initialDelay,
            final Duration baseDelay, final int increaseFactor, final Duration maxDelay) {
        final IncreasingRateCallable callable =
                new IncreasingRateCallable(task, ses, baseDelay, increaseFactor, maxDelay);
        final ScheduledFuture<Void> future =
                ses.schedule(callable, initialDelay.toMillis(), TimeUnit.MILLISECONDS);
        return new IncreasingRateTask(callable, future);
    }

    /**
     * @see ScheduledExecutorService#shutdownNow()
     */
    final void shutdownNow() {
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
