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

import static io.omam.halo.Engines.toHalo;
import static io.omam.halo.MulticastDnsHelper.classForName;
import static io.omam.halo.MulticastDnsHelper.typeForName;
import static io.omam.halo.MulticastDnsSd.CLASS_ANY;
import static io.omam.halo.MulticastDnsSd.TYPE_A;
import static io.omam.halo.MulticastDnsSd.TYPE_AAAA;
import static io.omam.halo.MulticastDnsSd.TYPE_PTR;
import static io.omam.halo.MulticastDnsSd.TYPE_SRV;
import static io.omam.halo.MulticastDnsSd.TYPE_TXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.omam.halo.DnsMessage.Builder;

/**
 * Static methods to build instance of {@link DnsRecord} and {@link DnsMessage}.
 */
@SuppressWarnings("javadoc")
public final class DnsFactory {

    private Attributes attributes;

    private Builder builder;

    private DnsRecord otherRecord;

    private DnsRecord record;

    /**
     * Constructor.
     */
    public DnsFactory() {
        // empty.
    }

    @After
    public final void after() {
        attributes = null;
        builder = null;
        otherRecord = null;
        record = null;
    }

    @Given("the following additional have been added:")
    public final void givenAdditionalAdded(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAdditional(newRecord(r, now)));
    }

    @Given("the following answers have been added:")
    public final void givenAnswersAdded(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAnswer(null, newRecord(r, now)));
    }

    @Given("attributes are created with the following key and value pairs:")
    public final void givenAttributesCreated(final DataTable data) {
        final List<String> pairs = data.asList();
        final Attributes.Builder b = Attributes.create();
        pairs.stream().map(Pair::parse).forEach(e -> {
            final String key = e.key().trim();
            final Optional<String> value = e.value();
            if (value.isPresent()) {
                final String v = value.get();
                if (v.isEmpty()) {
                    b.with(key, "", StandardCharsets.UTF_8);
                } else {
                    b.with(key, v, StandardCharsets.UTF_8);
                }
            } else {
                b.with(key);
            }
        });
        attributes = b.get();
    }

    @Given("the following authorities have been added:")
    public final void givenAuthoritiesAdded(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAuthority(newRecord(r, now)));
    }

    @Given("a DNS query has been created")
    public final void givenDnsQueryCreated() {
        builder = DnsMessage.query();
    }

    @Given("the following DNS record has been created:")
    public final void givenDnsRecordCreated(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        assertEquals(1, records.size());
        record = newRecord(records.get(0), Instant.now());
    }

    @Given("a DNS {word} record has been created at '{word}' with a ttl of '{word}'")
    public final void givenDnsRecordCreated(final String type, final String when, final String ttl) {
        final String name = "foo.bar.";
        final short clazz = CLASS_ANY;
        final Duration ttlDur = Duration.parse(ttl);
        final Instant instant = Instant.parse(when);
        switch (type) {
            case "A":
                try {
                    record = new AddressRecord(name, clazz, ttlDur, instant,
                                               InetAddress.getByName("192.168.154.0"));
                } catch (final UnknownHostException e) {
                    throw new AssertionError(e);
                }
                break;
            case "AAAA":
                try {
                    record = new AddressRecord(name, clazz, ttlDur, instant,
                                               InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
                } catch (final UnknownHostException e) {
                    throw new AssertionError(e);
                }
                break;
            case "PTR":
                record = new PtrRecord(name, clazz, ttlDur, instant, "");
                break;
            case "SRV":
                record = new SrvRecord(name, clazz, ttlDur, instant, (short) 0, "");
                break;
            case "TXT":
                record = new TxtRecord(name, clazz, ttlDur, instant, Attributes.create().with("fake").get());
                break;
            default:
                throw new AssertionError("Unsupported record: " + type);
        }
    }

    @Given("a DNS response has been created")
    public final void givenDnsResponseCreated() {
        builder = DnsMessage.response();
    }

    @Given("the following other DNS record has been created:")
    public final void givenOtherDnsRecordCreated(final DataTable data) {
        final List<Record> records = Parser.parse(data, Record::new);
        assertEquals(1, records.size());
        otherRecord = newRecord(records.get(0), Instant.now());
    }

    @Given("the following questions have been added:")
    public final void givenQuestionsAdded(final DataTable data) {
        final List<Question> questions = Parser.parse(data, Question::new);
        questions.forEach(q -> builder.addQuestion(newQuestion(q)));
    }

    final Attributes attributes() {
        return attributes;
    }

    final DnsMessage message() {
        return builder.get();
    }

    final DnsQuestion newQuestion(final Question question) {
        return new DnsQuestion(question.name(), typeForName(question.type()), classForName(question.clazz()));
    }

    final DnsRecord newRecord(final Record rec, final Instant now) {
        try {
            final short clazz = classForName(rec.clazz());
            final Duration ttlDur = Duration.parse(rec.ttl());
            switch (typeForName(rec.type())) {
                case TYPE_A:
                    return new AddressRecord(rec.name(), clazz, ttlDur, now, InetAddress.getByName(rec.address()));
                case TYPE_AAAA:
                    return new AddressRecord(rec.name(), clazz, ttlDur, now, InetAddress.getByName(rec.address()));
                case TYPE_PTR:
                    return new PtrRecord(rec.name(), clazz, ttlDur, now, rec.target());
                case TYPE_SRV:
                    return new SrvRecord(rec.name(), clazz, ttlDur, now, rec.port(), rec.server());
                case TYPE_TXT:
                    return new TxtRecord(rec.name(), clazz, ttlDur, now, toHalo(rec.text()));
                default:
                    throw new IllegalArgumentException();
            }
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    final DnsRecord otherRecord() {
        return otherRecord;
    }

    final DnsRecord record() {
        return record;
    }

}
