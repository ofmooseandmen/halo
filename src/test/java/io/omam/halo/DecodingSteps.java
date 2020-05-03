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

import static io.omam.halo.Assert.assertDnsQuestionsEquals;
import static io.omam.halo.Assert.assertDnsRecordsEquals;
import static io.omam.halo.MulticastDnsHelper.flagsForName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps to tests DNS message decoding.
 */
@SuppressWarnings("javadoc")
public final class DecodingSteps {

    private final DnsFactory factory;

    private final Exceptions exceptions;

    private Attributes attributes;

    private DnsMessage msg;

    private Instant now;

    private byte[] packet;

    public DecodingSteps(final DnsFactory aFactory, final Exceptions someExceptions) {
        factory = aFactory;
        exceptions = someExceptions;
    }

    @After
    public final void after() {
        attributes = null;
        msg = null;
        now = null;
        packet = null;
    }

    @Given("the following packet has been received:")
    public final void givenPacketReceived(final DataTable data) {
        packet = Bytes.parse(data);
    }

    @Then("the following attributes shall be returned:")
    public final void thenAttributes(final DataTable data) {
        final List<String> pairs = data.asList();
        assertEquals(pairs.size(), attributes.keys().size());
        pairs
            .stream()
            .map(Pair::parse)
            .forEach(pair -> assertEquals("for key: " + pair.key(), pair.value(),
                    attributes.value(pair.key(), StandardCharsets.UTF_8)));
    }

    @Then("it contains the following answers:")
    public final void thenContainsAnswers(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        final List<DnsRecord> expecteds =
                records.stream().map(r -> factory.newRecord(r, now)).collect(Collectors.toList());
        assertDnsRecordsEquals(expecteds, msg.answers());
    }

    @Then("it contains no answer")
    public final void thenContainsNoAnswer() {
        assertTrue(msg.answers().isEmpty());
    }

    @Then("^it contains no question$")
    public final void thenContainsNoQuestion() {
        assertTrue(msg.questions().isEmpty());
    }

    @Then("it contains the following questions:")
    public final void thenContainsQuestions(final DataTable data) {
        final List<Question> questions = Parser.parse(data, Question::new);
        final List<DnsQuestion> expecteds =
                questions.stream().map(factory::newQuestion).collect(Collectors.toList());
        assertDnsQuestionsEquals(expecteds, msg.questions());
    }

    @Then("a DNS {word} with {string} flags shall be returned")
    public final void thenDnsMessage(final String type, final String flags) {
        if (type.equals("response")) {
            assertTrue(msg.isResponse());
            assertFalse(msg.isQuery());
        } else if (type.equals("query")) {
            assertTrue(msg.isQuery());
            assertFalse(msg.isResponse());
        } else {
            throw new AssertionError("Expected response or query, got: " + type);
        }
        assertEquals(flagsForName(flags), msg.flags());
    }

    @When("the packet is decoded into attributes")
    public final void whenDecodePacketAttributes() {
        try (final MessageInputStream is = new MessageInputStream(packet)) {
            attributes = AttributesCodec.decode(is, packet.length);
        }
    }

    @When("the packet is decoded into a DNS message")
    public final void whenDecodePacketDnsMessage() {
        now = Clock.systemUTC().instant();
        try {
            msg = DnsMessage.decode(packet, now);
        } catch (final IOException e) {
            exceptions.thrown(e);
        }
    }
}
