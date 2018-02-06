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

import static io.omam.halo.MulticastDns.REAPING_INTERVAL;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically removes expired DNS records from the cache.
 */
final class Reaper {

    /** cache. */
    private final Cache cache;

    /** clock. */
    private final Clock clock;

    /** scheduled executor service. */
    private final ScheduledExecutorService ses;

    /** future to cancel the background reaping task. */
    private Future<?> f;

    /**
     * Constructor.
     *
     * @param aCache cache
     * @param aClock clock
     */
    Reaper(final Cache aCache, final Clock aClock) {
        cache = aCache;
        clock = aClock;
        ses = Executors.newSingleThreadScheduledExecutor(new HaloThreadFactory("reaper"));
    }

    /**
     * Starts a background task to remove expired records.
     */
    final void start() {
        f = ses.scheduleAtFixedRate(() -> cache.clean(clock.instant()), REAPING_INTERVAL.toMillis(),
                REAPING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the background task that removes expired records.
     */
    final void stop() {
        if (f != null) {
            f.cancel(true);
            f = null;
        }
        ses.shutdownNow();
    }

}
