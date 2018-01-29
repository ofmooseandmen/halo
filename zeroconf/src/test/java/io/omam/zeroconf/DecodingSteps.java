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

import static io.omam.zeroconf.MulticastDnsHelper.flagsForName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.omam.zeroconf.Attributes;
import io.omam.zeroconf.AttributesCodec;
import io.omam.zeroconf.DnsMessage;
import io.omam.zeroconf.MessageInputStream;

/**
 * Steps to tests DNS message decoding.
 */
@SuppressWarnings("javadoc")
public final class DecodingSteps {

    private final DnsFactory factory;

    private Attributes attributes;

    private DnsMessage msg;

    private Instant now;

    private byte[] packet;

    public DecodingSteps(final DnsFactory aFactory) {
        factory = aFactory;
    }

    @After
    public final void after() {
        attributes = null;
        msg = null;
        now = null;
        packet = null;
    }

    @Given("^the following packet has been received:$")
    public final void givenPacketReceived(final DataTable data) {
        packet = Bytes.parse(data);
    }

    @Then("^the following attributes shall be returned:$")
    public final void thenAttributes(final List<Pair> pairs) {
        assertEquals(pairs.size(), attributes.keys().size());
        for (final Pair pair : pairs) {
            if (pair.value().isEmpty()) {
                assertEquals(Optional.empty(), attributes.value(pair.key(), StandardCharsets.UTF_8));
            } else {
                assertEquals(Optional.of(pair.value()), attributes.value(pair.key(), StandardCharsets.UTF_8));
            }
        }
    }

    @Then("^it contains the following answers:$")
    public final void thenContainsAnswers(final List<Record> records) {
        records.stream().map(r -> factory.newRecord(r, now)).forEach(
                r -> assertEquals(1, msg.answers().stream().filter(a -> r.equals(a)).count()));
    }

    @Then("^it contains the following authorities:$")
    public final void thenContainsAuthorities(final List<Record> records) {
        records.stream().map(r -> factory.newRecord(r, now)).forEach(
                r -> assertEquals(1, msg.authorities().stream().filter(a -> r.equals(a)).count()));
    }

    @Then("^it contains no additional$")
    public final void thenContainsNoAdditional() {
        assertTrue(msg.additional().isEmpty());
    }

    @Then("^it contains no answer$")
    public final void thenContainsNoAnswer() {
        assertTrue(msg.answers().isEmpty());
    }

    @Then("^it contains no authority$")
    public final void thenContainsNoAuthority() {
        assertTrue(msg.authorities().isEmpty());
    }

    @Then("^it contains no question$")
    public final void thenContainsNoQuestion() {
        assertTrue(msg.questions().isEmpty());
    }

    @Then("^it contains the following questions:$")
    public final void thenContainsQuestions(final List<Question> questions) {
        questions.stream().map(factory::newQuestion).forEach(
                q -> assertEquals(1, msg.questions().stream().filter(mq -> q.equals(mq)).count()));
    }

    @Then("^a DNS (response|query) with \"(.+)\" flags shall be returned$")
    public final void thenDnsMessage(final String type, final String flags) {
        if (type.equals("response")) {
            assertTrue(msg.isResponse());
            assertFalse(msg.isQuery());
        } else {
            assertTrue(msg.isQuery());
            assertFalse(msg.isResponse());
        }
        assertEquals(flagsForName(flags), msg.flags());
    }

    @When("^the packet is decoded into attributes$")
    public final void whenDecodePacketAttributes() throws IOException {
        try (final MessageInputStream is = new MessageInputStream(packet)) {
            attributes = AttributesCodec.decode(is, packet.length);
        }
    }

    @When("^the packet is decoded into a DNS message$")
    public final void whenDecodePacketDnsMessage() throws IOException {
        now = Clock.systemUTC().instant();
        msg = DnsMessage.decode(packet, now);
    }
}
