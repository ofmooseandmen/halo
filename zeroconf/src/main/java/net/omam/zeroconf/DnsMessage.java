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

import static net.omam.zeroconf.MulticastDns.CLASS_UNIQUE;
import static net.omam.zeroconf.MulticastDns.FLAGS_QR_MASK;
import static net.omam.zeroconf.MulticastDns.FLAGS_QR_QUERY;
import static net.omam.zeroconf.MulticastDns.FLAGS_QR_RESPONSE;
import static net.omam.zeroconf.MulticastDns.TYPE_A;
import static net.omam.zeroconf.MulticastDns.TYPE_AAAA;
import static net.omam.zeroconf.MulticastDns.TYPE_PTR;
import static net.omam.zeroconf.MulticastDns.TYPE_SRV;
import static net.omam.zeroconf.MulticastDns.TYPE_TXT;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A DNS message.
 *
 * see https://www.ietf.org/rfc/rfc1035.txt
 */
@SuppressWarnings("javadoc")
final class DnsMessage {

    static final class Builder implements Supplier<DnsMessage> {

        private final short flags;

        private final List<DnsQuestion> questions;

        private final List<DnsAnswer> answers;

        private final List<DnsRecord> authorities;

        private final List<DnsRecord> additional;

        Builder(final short flag, final short[] otherFlags) {
            short f = flag;
            for (final short of : otherFlags) {
                f = (short) (f | of);
            }
            flags = f;
            questions = new ArrayList<>();
            answers = new ArrayList<>();
            authorities = new ArrayList<>();
            additional = new ArrayList<>();
        }

        @Override
        public final DnsMessage get() {
            return new DnsMessage(flags, questions, answers, authorities, additional);
        }

        final Builder addAdditional(final DnsRecord record) {
            additional.add(record);
            return this;
        }

        final Builder addAnswer(final DnsMessage msg, final DnsRecord record) {
            if (msg == null || !record.suppressedBy(msg)) {
                answers.add(DnsAnswer.unstamped(record));
            }
            return this;
        }

        final Builder addAnswer(final DnsRecord record, final Optional<Instant> stamp) {
            if (stamp.isPresent()) {
                if (!record.isExpired(stamp.get())) {
                    answers.add(DnsAnswer.stamped(record, stamp.get()));
                }
            } else {
                answers.add(DnsAnswer.unstamped(record));
            }
            return this;
        }

        final Builder addAuthority(final DnsRecord record) {
            authorities.add(record);
            return this;

        }

        final Builder addQuestion(final DnsQuestion question) {
            questions.add(question);
            return this;
        }

    }

    static final class DnsAnswer {

        private final Optional<Instant> stamp;

        private final DnsRecord record;

        private DnsAnswer(final DnsRecord aRecord, final Optional<Instant> aStamp) {
            stamp = aStamp;
            record = aRecord;
        }

        static DnsAnswer stamped(final DnsRecord record, final Instant stamp) {
            return new DnsAnswer(record, Optional.of(stamp));
        }

        static DnsAnswer unstamped(final DnsRecord record) {
            return new DnsAnswer(record, Optional.empty());
        }

        @Override
        public final String toString() {
            return record.toString();
        }

        final DnsRecord record() {
            return record;
        }

        final Optional<Instant> stamp() {
            return stamp;
        }

    }

    private final short flags;

    private final List<DnsQuestion> questions;

    private final List<DnsAnswer> answers;

    private final List<DnsRecord> authorities;

    private final List<DnsRecord> additional;

    DnsMessage(final short someFlags, final List<DnsQuestion> someQuestions, final List<DnsAnswer> someAnswers,
            final List<DnsRecord> someAuthorities, final List<DnsRecord> someAdditional) {
        flags = someFlags;
        questions = someQuestions;
        answers = someAnswers;
        authorities = someAuthorities;
        additional = someAdditional;
    }

    static DnsMessage decode(final byte[] bytes, final Instant now) throws IOException {
        try (final MessageInputStream is = new MessageInputStream(bytes)) {
            /*
             * header is 6 shorts for the ID, flags, number of questions, number of answers, number of authorities
             * and number of additional. ID is irrelevant for mDNS.
             */
            is.readShort();
            final short flags = (short) is.readShort();
            final short numQuestions = (short) is.readShort();
            final short numAnswers = (short) is.readShort();
            final short numAuthorities = (short) is.readShort();
            final short numAdditional = (short) is.readShort();

            final List<DnsQuestion> questions = new ArrayList<>();
            for (int i = 0; i < numQuestions; i++) {
                final String name = is.readName();
                final short type = (short) is.readShort();
                final short clazz = (short) is.readShort();
                final DnsQuestion question = new DnsQuestion(name, type, clazz);
                questions.add(question);
            }
            final List<DnsAnswer> answers =
                    readRecords(is, numAnswers, now).stream().map(DnsAnswer::unstamped).collect(
                            Collectors.toList());
            final List<DnsRecord> authorities = readRecords(is, numAuthorities, now);
            final List<DnsRecord> additional = readRecords(is, numAdditional, now);
            return new DnsMessage(flags, questions, answers, authorities, additional);
        } catch (final BufferUnderflowException e) {
            throw new IOException(e);
        }
    }

    static Builder query(final short... flags) {
        return new Builder(FLAGS_QR_QUERY, flags);
    }

    static Builder response(final short... flags) {
        return new Builder(FLAGS_QR_RESPONSE, flags);
    }

    private static Optional<DnsRecord> readRecord(final MessageInputStream is, final Instant now)
            throws IOException {
        final String name = is.readName();
        final short type = (short) is.readShort();
        final short clazz = (short) is.readShort();
        final Duration ttl = Duration.ofSeconds(is.readInt());
        final short length = (short) is.readShort();

        final DnsRecord record;
        switch (type) {
            case TYPE_A:
                record = AddressRecord.ipv4(name, clazz, ttl, now, InetAddress.getByAddress(is.readBytes(length)));
                break;
            case TYPE_AAAA:
                record = AddressRecord.ipv6(name, clazz, ttl, now, InetAddress.getByAddress(is.readBytes(length)));
                break;
            case TYPE_PTR:
                record = new PtrRecord(name, clazz, ttl, now, is.readName());
                break;
            case TYPE_SRV:
                final short priority = (short) is.readShort();
                final short weight = (short) is.readShort();
                final short port = (short) is.readShort();
                final String server = is.readName();
                record = new SrvRecord(name, clazz, ttl, now, port, priority, server, weight);
                break;
            case TYPE_TXT:
                record = new TxtRecord(name, clazz, ttl, now, AttributesCodec.decode(is, length));
                break;
            default:
                /*
                 * ignore unknown types: skip the payload for the resource record so the next records can be parsed
                 * correctly.
                 */
                is.skip(length);
                record = null;
                break;
        }
        return Optional.ofNullable(record);
    }

    private static List<DnsRecord> readRecords(final MessageInputStream is, final int size, final Instant now)
            throws IOException {
        final List<DnsRecord> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            readRecord(is, now).ifPresent(records::add);
        }
        return records;
    }

    private static void write(final DnsQuestion question, final MessageOutputStream mos) {
        mos.writeName(question.name());
        mos.writeShort(question.type());
        mos.writeShort(question.clazz());
    }

    private static void write(final DnsRecord record, final Optional<Instant> stamp,
            final MessageOutputStream mos) {
        mos.writeName(record.name());
        mos.writeShort(record.type());
        if (record.isUnique()) {
            mos.writeShort((short) (record.clazz() | CLASS_UNIQUE));
        } else {
            mos.writeShort(record.clazz());
        }
        if (stamp.isPresent()) {
            mos.writeInt((int) record.remainingTtl(stamp.get()).getSeconds());
        } else {
            mos.writeInt((int) record.ttl().getSeconds());
        }

        /*
         * next two bytes is size of record specific payload. first write the record, then calculate the size.
         */
        final int sizePos = mos.position();
        mos.skip(2);
        final int startPos = mos.position();
        record.write(mos);
        final int endPos = mos.position();
        mos.writeShort(sizePos, (short) (endPos - startPos));
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder("DNS ");
        if (isQuery()) {
            sb.append("query with ").append(questions.size()).append(" question(s): ").append(
                    questions.toString());
        } else {
            sb.append("response with ").append(answers.size()).append(" answer(s): ").append(answers.toString());
        }
        return sb.toString();
    }

    final List<DnsRecord> additional() {
        return Collections.unmodifiableList(additional);
    }

    final List<DnsRecord> answers() {
        return Collections.unmodifiableList(answers.stream().map(DnsAnswer::record).collect(Collectors.toList()));
    }

    final List<DnsRecord> authorities() {
        return Collections.unmodifiableList(authorities);
    }

    final byte[] encode() {
        try (final MessageOutputStream mos = new MessageOutputStream()) {
            mos.writeShort((short) 0);

            mos.writeShort(flags);
            mos.writeShort((short) questions.size());
            mos.writeShort((short) answers.size());
            mos.writeShort((short) authorities.size());
            mos.writeShort((short) additional.size());

            questions.forEach(q -> write(q, mos));
            answers.forEach(a -> write(a.record(), a.stamp(), mos));
            authorities.forEach(a -> write(a, Optional.empty(), mos));
            additional.forEach(a -> write(a, Optional.empty(), mos));

            return mos.toByteArray();
        } catch (final IOException e) {
            /* not possible. */
            throw new IllegalStateException(e);
        }
    }

    final short flags() {
        return flags;
    }

    /* Returns true if this is a query. */
    final boolean isQuery() {
        return (flags & FLAGS_QR_MASK) == FLAGS_QR_QUERY;
    }

    /* Returns true if this is a response. */
    final boolean isResponse() {
        return (flags & FLAGS_QR_MASK) == FLAGS_QR_RESPONSE;
    }

    final List<DnsQuestion> questions() {
        return Collections.unmodifiableList(questions);
    }

}
