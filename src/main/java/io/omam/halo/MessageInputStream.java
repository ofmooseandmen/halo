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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An input stream to read the data received in a single {@link DnsMessage}.
 * <p>
 * Data is read from a {@link ByteArrayInputStream} using big-endian ordering.
 * <p>
 * This class implements the compression algorithm used for names described in https://www.ietf.org/rfc/rfc1035.txt
 */
final class MessageInputStream extends ByteArrayInputStream {

    /** pointers for decompression: index of a string in this stream. */
    private final Map<Integer, String> pointers;

    /**
     * Creates a {@code MessageInputStream} so that it uses {@code buffer} as its buffer array. The buffer array is
     * not copied.
     *
     * @see ByteArrayInputStream#ByteArrayInputStream(byte[])
     * @param buffer the input buffer.
     */
    MessageInputStream(final byte[] buffer) {
        super(buffer);
        pointers = new HashMap<>();
    }

    /**
     * Reads the next byte of data from this input stream.
     *
     * @see ByteArrayInputStream#read()
     * @return the next byte of data, or {@code -1} if the end of the stream has been reached.
     */
    final int readByte() {
        return read();
    }

    /**
     * Reads up to {@code length} bytes of data into an array of bytes from this input stream.
     *
     * @see ByteArrayInputStream#read(byte[], int, int)
     * @param length the maximum number of bytes to read
     * @return an array containing the read bytes
     */
    final byte[] readBytes(final int length) {
        final byte[] bytes = new byte[length];
        read(bytes, 0, length);
        return bytes;
    }

    /**
     * Reads the next integer (4 bytes) from this input stream.
     *
     * @return the next integer, or {@code -1} if the end of the stream has been reached.
     */
    final int readInt() {
        return readShort() << 16 | readShort();
    }

    /**
     * Reads the next name ({@link StandardCharsets#UTF_8 UTF8} String) from this input stream.
     *
     * @return the name
     */
    final String readName() {
        final Map<Integer, StringBuilder> names = new HashMap<>();
        final StringBuilder sb = new StringBuilder();
        boolean finished = false;
        while (!finished) {
            final int b = readByte();
            if (b == 0) {
                break;
            }
            if ((b & 0xC0) == 0x00) {
                final int offset = pos - 1;
                final String label = readString(b) + ".";
                sb.append(label);
                for (final StringBuilder previousLabel : names.values()) {
                    previousLabel.append(label);
                }
                names.put(offset, new StringBuilder(label));

            } else {
                final int index = (b & 0x3F) << 8 | readByte();
                final String compressedLabel = pointers.get(Integer.valueOf(index));
                sb.append(compressedLabel);
                for (final StringBuilder previousLabel : names.values()) {
                    previousLabel.append(compressedLabel);
                }
                finished = true;
            }
        }

        for (final Map.Entry<Integer, StringBuilder> entry : names.entrySet()) {
            final Integer index = entry.getKey();
            pointers.put(index, entry.getValue().toString());
        }
        return sb.toString();
    }

    /**
     * Reads the next short (2 bytes) from this input stream.
     *
     * @return the next short, or {@code -1} if the end of the stream has been reached.
     */
    final int readShort() {
        return readByte() << 8 | readByte();
    }

    /**
     * Reads up to {@code length} bytes of data into an {@link StandardCharsets#UTF_8 UTF8} String from this input
     * stream
     *
     * @param length the maximum number of bytes to read
     * @return a String
     */
    private String readString(final int length) {
        final byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
