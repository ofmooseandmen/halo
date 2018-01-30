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

import static io.omam.zeroconf.MulticastDns.CLASS_ANY;
import static io.omam.zeroconf.MulticastDns.TYPE_A;
import static io.omam.zeroconf.MulticastDns.TYPE_AAAA;
import static io.omam.zeroconf.MulticastDns.TYPE_PTR;
import static io.omam.zeroconf.MulticastDns.TYPE_SRV;
import static io.omam.zeroconf.MulticastDns.TYPE_TXT;
import static io.omam.zeroconf.MulticastDnsHelper.classForName;
import static io.omam.zeroconf.MulticastDnsHelper.typeForName;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import io.omam.zeroconf.DnsMessage.Builder;

/**
 * Static methods to build instance of {@link DnsRecord} and {@link DnsMessage}.
 */
@SuppressWarnings("javadoc")
public final class DnsFactory {

    private final Engines engines;

    private Attributes attributes;

    private Builder builder;

    private DnsRecord otherRecord;

    private DnsRecord record;

    /**
     * Constructor.
     *
     * @param someEngines engines
     */
    public DnsFactory(final Engines someEngines) {
        engines = someEngines;
    }

    @After
    public final void after() {
        attributes = null;
        builder = null;
        otherRecord = null;
        record = null;
    }

    @Given("^the following additional have been added:$")
    public final void givenAdditionalAdded(final List<Record> records) {
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAdditional(newRecord(r, now)));
    }

    @Given("^the following answers have been added:$")
    public final void givenAnswersAdded(final List<Record> records) {
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAnswer(null, newRecord(r, now)));
    }

    @Given("^attributes are created with the following key/value pairs:$")
    public final void givenAttributesCreated(final List<Pair> pairs) {
        final Attributes.Builder b = Attributes.create();
        pairs.forEach(e -> {
            final String key = e.key().trim();
            final String value = e.value().trim();
            if (value.isEmpty()) {
                b.with(key);
            } else {
                b.with(key, value, StandardCharsets.UTF_8);
            }

        });
        attributes = b.get();
    }

    @Given("^the following authorities have been added:$")
    public final void givenAuthoritiesAdded(final List<Record> records) {
        final Instant now = Instant.now();
        records.forEach(r -> builder.addAuthority(newRecord(r, now)));
    }

    @Given("^a DNS query has been created$")
    public final void givenDnsQueryCreated() {
        builder = DnsMessage.query();
    }

    @Given("^the following DNS record has been created:$")
    public final void givenDnsRecordCreated(final List<Record> rec) {
        assertEquals(1, rec.size());
        record = newRecord(rec.get(0), Instant.now());
    }

    @Given("^a DNS (\\w+) record has been created at '(.+)' with a ttl of '(.+)'$")
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
                record = new SrvRecord(name, clazz, ttlDur, instant, (short) 0, (short) 0, "", (short) 0);
                break;
            case "TXT":
                record = new TxtRecord(name, clazz, ttlDur, instant, Attributes.create().with("fake").get());
                break;
            default:
                throw new AssertionError("Unsupported record: " + type);
        }
    }

    @Given("^a DNS response has been created$")
    public final void givenDnsResponseCreated() {
        builder = DnsMessage.response();
    }

    @Given("^the following other DNS record has been created:$")
    public final void givenOtherDnsRecordCreated(final List<Record> rec) {
        assertEquals(1, rec.size());
        otherRecord = newRecord(rec.get(0), Instant.now());
    }

    @Given("^the following questions have been added:$")
    public final void givenQuestionsAdded(final List<Question> questions) {
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
                    return new SrvRecord(rec.name(), clazz, ttlDur, now, rec.port(), rec.priority(), rec.server(),
                                         rec.weight());
                case TYPE_TXT:
                    return new TxtRecord(rec.name(), clazz, ttlDur, now, engines.toZc(rec.text()));
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
