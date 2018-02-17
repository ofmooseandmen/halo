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

import static io.omam.halo.MulticastDns.QUERYING_DELAY;
import static io.omam.halo.MulticastDns.QUERYING_INTERVAL;
import static io.omam.halo.MulticastDns.QUERY_NUM;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Base class for browsing.
 */
abstract class HaloBrowser implements ResponseListener, AutoCloseable {

    /** halo helper. */
    private final HaloHelper halo;

    /** discoverer executor service. */
    private final HaloScheduledExecutorService ses;

    /** future representing the querying task. */
    private Future<Void> qFuture;

    /**
     * Constructor.
     *
     * @param name browser name (use to prefix query background thread)
     * @param haloHelper halo helper
     */
    HaloBrowser(final String name, final HaloHelper haloHelper) {
        halo = haloHelper;
        ses = new HaloScheduledExecutorService(name);
        qFuture = null;
    }

    /**
     * Closes this browser.
     * <p>
     * If this browser is not started this method has no effect.
     */
    @Override
    public final void close() {
        halo.removeResponseListener(this);
        if (qFuture != null) {
            qFuture.cancel(true);
        }
        ses.shutdownNow();
        doClose();
    }

    /**
     * Called at the end of {@link #close()}.
     */
    protected abstract void doClose();

    /**
     * @return the query task to execute.
     */
    protected abstract Callable<Void> queryTask();

    /**
     * Starts browsing.
     * <p>
     * If this browser is already started this method has no effect.
     */
    final void start() {
        if (qFuture == null) {
            halo.addResponseListener(this);
            qFuture = ses.scheduleBatches(queryTask(), QUERY_NUM, QUERYING_DELAY, QUERYING_INTERVAL);
        }
    }

}
