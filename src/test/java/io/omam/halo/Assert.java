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

import static io.omam.halo.Engines.attributes;
import static io.omam.halo.Engines.toHalo;
import static io.omam.halo.Engines.toJmdns;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.jmdns.ServiceInfo;

/**
 * A set of assertion methods for Halo and JmDNS objects.
 */
final class Assert {

    /**
     * Constructor.
     */
    private Assert() {
        // empty.
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
            assertEquals(srve.server(), srva.server());
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

    /**
     * Asserts that given {@link ServiceDetails} equals Halo {@link Service}.
     *
     * @param expected expected service
     * @param actual actual service
     */
    static void assertServiceEquals(final ServiceDetails expected, final Service actual) {
        assertEquals(expected.instanceName(), actual.instanceName());
        assertEquals(expected.registrationType(), actual.registrationType());
        assertEquals(expected.port(), actual.port());
        assertAttributesEquals(toHalo(expected.text()), actual.attributes());
    }

    /**
     * Asserts that given {@link ServiceDetails} equals JmDNS {@link ServiceInfo}.
     *
     * @param expected expected service
     * @param actual actual service
     */
    static void assertServiceEquals(final ServiceDetails expected, final ServiceInfo actual) {
        assertEquals(expected.instanceName(), actual.getName());
        assertEquals(expected.registrationType() + "local.", actual.getType());
        assertEquals(expected.port(), actual.getPort());
        assertEquals(toJmdns(expected.text()), attributes(actual));
    }

    /**
     * Asserts that two {@link Attributes} instances are equal.
     *
     * @param expected expected value
     * @param actual the value to check against {@code expected}
     */
    private static void assertAttributesEquals(final Attributes expected, final Attributes actual) {
        assertTrue(expected.keys().equals(actual.keys()));
        for (final String key : expected.keys()) {
            assertEquals(expected.value(key), actual.value(key));
        }
    }

}
