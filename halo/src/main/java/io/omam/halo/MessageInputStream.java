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
@SuppressWarnings("javadoc")
final class MessageInputStream extends ByteArrayInputStream {

    private final Map<Integer, String> pointers;

    MessageInputStream(final byte[] buffer) {
        super(buffer);
        pointers = new HashMap<>();
    }

    final int readByte() {
        return read();
    }

    final byte[] readBytes(final int length) {
        final byte[] bytes = new byte[length];
        read(bytes, 0, length);
        return bytes;
    }

    final int readInt() {
        return readShort() << 16 | readShort();
    }

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

    final int readShort() {
        return readByte() << 8 | readByte();
    }

    final String readString(final int length) {
        final byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
