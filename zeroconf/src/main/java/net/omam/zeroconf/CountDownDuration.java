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
package net.omam.zeroconf;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A facility to count down from an initial duration.
 */
final class CountDownDuration {

    /** remaing duration. */
    private final Duration duration;

    /** when the timer was started, milliseconds from the epoch of 1970-01-01T00:00Z. */
    private long start;

    /**
     * Class constructor.
     *
     * @param initialDuration initial duration, not null and not negative
     */
    private CountDownDuration(final Duration initialDuration) {
        duration = initialDuration;
        start = -1;
    }

    /**
     * Returns a new {@link CountDownDuration timer} that count downs from the given {@code initialDuration}.
     *
     * @param initialDuration initial duration, not null and not negative
     * @return a new {@link CountDownDuration timer} that count downs from the given {@code initialDuration}
     */
    static CountDownDuration of(final Duration initialDuration) {
        Objects.requireNonNull(initialDuration);
        if (initialDuration.isNegative()) {
            throw new IllegalArgumentException("Invalid initial duration: " + initialDuration);
        }
        return new CountDownDuration(initialDuration);
    }

    /**
     * Assess and returns the remaining duration or {@link Duration#ZERO} is the initial duration has elapsed.
     * <p>
     * This methods throws an {@link IllegalStateException} if the timer has not been {@link #start() started}.
     *
     * @return the remaining duration
     */
    final Duration remaining() {
        if (start == -1) {
            throw new IllegalStateException("CountDownTimer not started");
        }
        final long elapsedNs = System.nanoTime() - start;
        Duration remaining = duration.minus(elapsedNs, ChronoUnit.NANOS);
        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }
        return remaining;
    }

    /**
     * Starts this timer.
     *
     * @return this timer
     */
    final CountDownDuration start() {
        start = System.nanoTime();
        return this;
    }

}
