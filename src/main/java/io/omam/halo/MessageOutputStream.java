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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An output stream to write data of a single {@link DnsMessage} that allows to skip bytes and write at selected
 * position.
 * <p>
 * Data is written into a {@link ByteArrayOutputStream} using big-endian ordering.
 * <p>
 * This class implements the compression algorithm used for names described in https://www.ietf.org/rfc/rfc1035.txt
 */
final class MessageOutputStream extends ByteArrayOutputStream {

    /** pointers for decompression: string to index in this stream. */
    private final Map<String, Integer> pointers;

    /**
     * Creates a new byte array output stream. The buffer capacity is initially 32 bytes, though its size increases
     * if necessary.
     *
     * @see ByteArrayOutputStream#ByteArrayOutputStream()
     */
    MessageOutputStream() {
        pointers = new HashMap<>();
    }

    /**
     * @return the number of valid bytes in the buffer.
     */
    final int position() {
        return count;
    }

    /**
     * Skips over the given number of bytes.
     *
     * @param length number of bytes to skip over
     */
    final void skip(final int length) {
        count += length;
    }

    /**
     * Writes the given byte to this output stream.
     *
     * @see ByteArrayOutputStream#write(int)
     * @param b byte
     */
    final void writeByte(final int b) {
        write(b & 0xFF);
    }

    /**
     * Writes the given byte array to this output stream.
     *
     * @see ByteArrayOutputStream#write(byte[], int, int)
     * @param bytes bytes
     */
    final void writeBytes(final byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    /**
     * Writes the given integer to this output stream.
     *
     * @param i integer
     */
    final void writeInt(final int i) {
        writeShort(i >> 16);
        writeShort(i);
    }

    /**
     * Writes the given name, using compression, to this output stream.
     *
     * @param name name
     */
    final void writeName(final String name) {
        String sub = name;
        while (true) {
            int n = sub.indexOf('.');
            if (n < 0) {
                n = sub.length();
            }
            if (n <= 0) {
                writeByte(0);
                return;
            }
            final String label = sub.substring(0, n);
            final Integer offset = pointers.get(sub);
            if (offset != null) {
                final int val = offset.intValue();
                writeByte(val >> 8 | 0xC0);
                writeByte(val & 0xFF);
                return;
            }
            pointers.put(sub, Integer.valueOf(size()));
            writeCharacterString(label);
            sub = sub.substring(n);
            if (sub.startsWith(".")) {
                sub = sub.substring(1);
            }
        }
    }

    /**
     * Writes the given short to this output stream.
     *
     * @param s short
     */
    final void writeShort(final int s) {
        writeByte(s >> 8);
        writeByte(s);
    }

    /**
     * Writes the given short at the given index to this output stream without advancing the position.
     *
     * @param index index
     * @param s short
     */
    final void writeShort(final int index, final short s) {
        buf[index] = (byte) (s >> 8 & 0xFF);
        buf[index + 1] = (byte) (s & 0xFF);
    }

    /**
     * Writes the given {@link StandardCharsets#UTF_8 UTF8} String to this output stream.
     *
     * @param str UTF8 string
     */
    final void writeString(final String str) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeBytes(bytes);
    }

    /**
     * Writes the size of the given {@link StandardCharsets#UTF_8 UTF8} String, followed by the given String to
     * this output stream.
     *
     * @param str UTF8 string
     */
    private void writeCharacterString(final String str) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeByte(bytes.length);
        writeBytes(bytes);
    }

}
