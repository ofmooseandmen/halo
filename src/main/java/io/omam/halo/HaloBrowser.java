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

import static io.omam.halo.HaloProperties.QUERYING_DELAY;
import static io.omam.halo.HaloProperties.QUERYING_FIRST;
import static io.omam.halo.HaloProperties.QUERYING_INCREASE;
import static io.omam.halo.HaloProperties.QUERYING_MAX;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import io.omam.halo.HaloScheduledExecutorService.IncreasingRateTask;

/**
 * Base class for browsing.
 */
abstract class HaloBrowser implements ResponseListener, AutoCloseable {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(HaloBrowser.class.getName());

    /** halo helper. */
    private final HaloHelper halo;

    /** browser name. */
    private final String name;

    /** discoverer executor service. */
    private final HaloScheduledExecutorService ses;

    /** querying task. */
    private IncreasingRateTask task;

    /**
     * Constructor.
     *
     * @param aName browser name (use to prefix query background thread)
     * @param haloHelper halo helper
     */
    HaloBrowser(final String aName, final HaloHelper haloHelper) {
        name = aName;
        halo = haloHelper;
        ses = new HaloScheduledExecutorService(aName);
        task = null;
    }

    /**
     * Closes this browser.
     * <p>
     * If this browser is not started this method has no effect.
     */
    @Override
    public final void close() {
        halo.removeResponseListener(this);
        if (task != null) {
            task.cancel();
        }
        ses.shutdownNow();
        doClose();
    }

    /**
     * Resets the query interval to the base value.
     */
    final void resetQueryInterval() {
        if (task != null) {
            LOGGER.info(() -> "Reset browsing interval for " + name + " to ");
            task.reset();
        }
    }

    /**
     * Starts browsing.
     * <p>
     * If this browser is already started this method has no effect.
     */
    final void start() {
        if (task == null) {
            halo.addResponseListener(this);
            /*
             * RFC 6762: the interval between the first two queries MUST be at least one second, the intervals
             * between successive queries MUST increase by at least a factor of two. [...]
             *
             * When the interval between queries reaches or exceeds 60 minutes, a querier MAY cap the interval to a
             * maximum of 60 minutes. [...]
             *
             * a Multicast DNS querier SHOULD also delay the first query of the series by a randomly chosen amount
             * in the range 20-120 ms.[...]
             */
            task = ses
                .scheduleIncreasingly(queryTask(), QUERYING_FIRST, QUERYING_DELAY, QUERYING_INCREASE,
                        QUERYING_MAX);
        }
    }

    /**
     * Called at the end of {@link #close()}.
     */
    protected abstract void doClose();

    /**
     * @return the query task to execute.
     */
    protected abstract Callable<Void> queryTask();

}
