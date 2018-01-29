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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

/**
 * A set of assertion methods for zeroconf objects.
 */
final class ZeroconfAssert {

    /**
     * Constructor.
     */
    private ZeroconfAssert() {
        // empty.
    }

    /**
     * Asserts that two {@link Attributes} instances are equal.
     *
     * @param expected expected value
     * @param actual the value to check against {@code expected}
     */
    static void assertAttributesEquals(final Attributes expected, final Attributes actual) {
        assertTrue(expected.keys().equals(actual.keys()));
        for (final String key : expected.keys()) {
            assertEquals(expected.value(key), actual.value(key));
        }
    }

    /**
     * Asserts that two {@link DnsQuestion} instances are equals.
     *
     * @param expected expected value
     * @param actual the value to check against {@code expected}
     */
    static void assertDnsQuestionEquals(final DnsQuestion expected, final DnsQuestion actual) {
        /* DnsEntry */
        assertEquals(expected.clazz(), actual.clazz());
        assertEquals(expected.name(), actual.name());
        assertEquals(expected.type(), actual.type());
    }

    /**
     * Asserts that two List of {@link DnsQuestion}s are equals using
     * {@link #assertDnsQuestionEquals(DnsQuestion, DnsQuestion)}.
     *
     * @param expecteds expected values
     * @param actuals the values to check against {@code expecteds}
     */
    static void assertDnsQuestionsEquals(final List<DnsQuestion> expecteds, final List<DnsQuestion> actuals) {
        assertEquals(expecteds.size(), actuals.size());
        for (int i = 0; i < expecteds.size(); i++) {
            assertDnsQuestionEquals(expecteds.get(i), actuals.get(i));
        }
    }

    /**
     * Asserts that two {@link DnsRecord} instances are equals.
     *
     * @param expected expected value
     * @param actual the value to check against {@code expected}
     */
    static void assertDnsRecordEquals(final DnsRecord expected, final DnsRecord actual) {
        /* DnsEntry */
        assertEquals(expected.clazz(), actual.clazz());
        assertEquals(expected.name(), actual.name());
        assertEquals(expected.type(), actual.type());

        /* DnsRecord. */
        assertEquals(expected.expirationTime(0), actual.expirationTime(0));
        assertEquals(expected.ttl(), actual.ttl());

        if (expected instanceof AddressRecord && actual instanceof AddressRecord) {
            /* AddressRecord */
            assertEquals(((AddressRecord) expected).address(), ((AddressRecord) actual).address());
        } else if (expected instanceof PtrRecord && actual instanceof PtrRecord) {
            /* PtrRecord */
            assertEquals(((PtrRecord) expected).target(), ((PtrRecord) actual).target());
        } else if (expected instanceof SrvRecord && actual instanceof SrvRecord) {
            /* SrvRecord */
            final SrvRecord srve = (SrvRecord) expected;
            final SrvRecord srva = (SrvRecord) actual;
            assertEquals(srve.port(), srva.port());
            assertEquals(srve.priority(), srva.priority());
            assertEquals(srve.server(), srva.server());
            assertEquals(srve.weight(), srva.weight());
        } else if (expected instanceof TxtRecord && actual instanceof TxtRecord) {
            /* TxtRecord */
            assertAttributesEquals(((TxtRecord) expected).attributes(), ((TxtRecord) actual).attributes());
        } else {
            fail("Could not assert equality of expected ["
                + expected.getClass().getName()
                + "] & actual ["
                + actual.getClass().getName()
                + "]");
        }
    }

    /**
     * Asserts that two List of {@link DnsRecord}s are equals using
     * {@link #assertDnsRecordEquals(DnsRecord, DnsRecord)}.
     *
     * @param expecteds expected values
     * @param actuals the values to check against {@code expecteds}
     */
    static void assertDnsRecordsEquals(final List<DnsRecord> expecteds, final List<DnsRecord> actuals) {
        assertEquals(expecteds.size(), actuals.size());
        for (int i = 0; i < expecteds.size(); i++) {
            assertDnsRecordEquals(expecteds.get(i), actuals.get(i));
        }
    }

}
