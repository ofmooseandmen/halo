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

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("javadoc")
abstract class DnsRecord extends DnsEntry {

    private Duration ttl;

    private Instant doc;

    protected DnsRecord(final String aName, final short aType, final short aClass, final Duration aTtl,
            final Instant now) {
        super(aName, aType, aClass);
        Objects.requireNonNull(aTtl);
        Objects.requireNonNull(now);
        ttl = aTtl;
        doc = now;
    }

    protected abstract void write(final MessageOutputStream mos);

    /*
     * Returns the time at which this record will have expired by a certain percentage (0 .. 100).
     */
    final Instant expirationTime(final int percent) {
        final long ttlPercent = (long) (ttl.toNanos() * (percent / 100.0));
        return doc.plus(Duration.ofNanos(ttlPercent));
    }

    /* Returns true if this record has expired. */
    final boolean isExpired(final Instant now) {
        final Instant t = expirationTime(100);
        return t.equals(now) || t.isBefore(now);
    }

    /* Returns true if this record is at least half way expired. */
    final boolean isStale(final Instant now) {
        final Instant t = expirationTime(50);
        return t.equals(now) || t.isBefore(now);
    }

    /* Returns the remaining TTL duration. */
    final Duration remainingTtl(final Instant now) {
        final Duration dur = Duration.between(now, expirationTime(100));
        if (dur.isNegative()) {
            return Duration.ZERO;
        }
        return dur;
    }

    /*
     * Sets this record's TTL and created time to that of another record.
     */
    final void resetTtl(final DnsRecord other) {
        doc = other.doc;
        ttl = other.ttl;
    }

    /*
     * Returns true if any answer, authority or additional in a message can suffice for the information held in
     * this record.
     */
    final boolean suppressedBy(final DnsMessage msg) {
        return Stream.of(msg.answers(), msg.authorities(), msg.additional()).flatMap(Collection::stream).anyMatch(
                this::suppressedByAnswer);
    }

    /*
     * Returns true if another record has same name, type and class, and if its TTL is at least half of this
     * record's.
     */
    final boolean suppressedByAnswer(final DnsRecord other) {
        return name().equals(other.name())
            && type() == other.type()
            && clazz() == other.clazz()
            && other.ttl.compareTo(ttl.dividedBy(2)) >= 0;
    }

    final Duration ttl() {
        return ttl;
    }

}
