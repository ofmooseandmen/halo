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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Codec to read/write {@link Attributes} from/to byte stream
 */
final class AttributesCodec {

    /**
     * Constructor
     */
    private AttributesCodec() {
        // empty.
    }

    /**
     * Obtains an instance of {@code Attributes} by reading the given number of bytes from the given stream.
     *
     * @param input stream of bytes
     * @param length number of bytes to read
     * @return attributes, not null
     */
    static Attributes decode(final MessageInputStream input, final int length) {
        final Map<String, Optional<ByteBuffer>> map = new HashMap<>();
        int readBytes = 0;
        while (readBytes < length) {
            final int pairLength = input.readByte();
            final byte[] bytes = input.readBytes(pairLength);
            final int sep = separator(bytes);
            final String key;
            final Optional<ByteBuffer> value;
            if (sep == -1) {
                key = new String(bytes, StandardCharsets.UTF_8);
                value = Optional.empty();
            } else {
                key = new String(subarray(bytes, 0, sep), StandardCharsets.UTF_8);
                value = Optional.of(value(bytes, sep));
            }
            if (!key.isEmpty() && !map.containsKey(key)) {
                map.put(key, value);
            }
            readBytes = readBytes + pairLength + 1;
        }
        return new AttributesImpl(map);
    }

    /**
     * Writes the given {@code Attributes} to the given stream.
     *
     * @param attributes attributes
     * @param output stream of bytes
     */
    static void encode(final Attributes attributes, final MessageOutputStream output) {
        final Set<String> keys = attributes.keys();
        for (final String key : keys) {
            try (final MessageOutputStream attos = new MessageOutputStream()) {
                attos.writeString(key);
                final Optional<ByteBuffer> value = attributes.value(key);
                if (value.isPresent()) {
                    attos.writeString("=");
                    final byte[] bytes = new byte[value.get().remaining()];
                    value.get().get(bytes);
                    attos.writeAllBytes(bytes);
                }
                output.writeByte(attos.size());
                output.writeAllBytes(attos.toByteArray());
            }
        }
    }

    /**
     * Returns the index of the key/value separator ('=').
     *
     * @param bytes array of bytes
     * @return index of the key/value separator ('=') or {@code -1} if no separator was found
     */
    private static int separator(final byte[] bytes) {
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] == '=') {
                return index;
            }
        }
        return -1;
    }

    /**
     * Sub array of given array from begin inclusive to end exclusive.
     *
     * @param bytes array of bytes
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return sub array
     */
    private static byte[] subarray(final byte[] bytes, final int beginIndex, final int endIndex) {
        final int length = endIndex - beginIndex;
        final byte[] arr = new byte[length];
        System.arraycopy(bytes, beginIndex, arr, 0, length);
        return arr;
    }

    /**
     * Reads the value: everything after the given index.
     *
     * @param bytes array of bytes
     * @param sep index of the key/value separator
     * @return value
     */
    private static ByteBuffer value(final byte[] bytes, final int sep) {
        final int length = bytes.length - sep;
        if (length == 1) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(subarray(bytes, sep + 1, bytes.length));
    }

}
