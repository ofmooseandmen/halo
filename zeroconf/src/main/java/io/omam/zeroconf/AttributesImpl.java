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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link Attributes} implementation.
 */
final class AttributesImpl implements Attributes {

    /** key/value pairs. */
    private final Map<String, Optional<ByteBuffer>> map;

    /**
     * Constructor.
     *
     * @param aMap key/value pairs
     */
    AttributesImpl(final Map<String, Optional<ByteBuffer>> aMap) {
        Objects.requireNonNull(aMap);
        map = aMap;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return map.equals(((AttributesImpl) obj).map);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (map == null ? 0 : map.hashCode());
        return result;
    }

    @Override
    public final Set<String> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public final String toString() {
        return "["
            + keys().stream().map(k -> k + "=" + value(k, StandardCharsets.UTF_8)).collect(Collectors.joining(";"))
            + "]";
    }

    @Override
    public final Optional<ByteBuffer> value(final String key) {
        return map.getOrDefault(key, Optional.empty()).map(ByteBuffer::asReadOnlyBuffer);
    }

    @Override
    public final Optional<String> value(final String key, final Charset charset) {
        final Optional<ByteBuffer> buffer = value(key);
        if (!buffer.isPresent()) {
            return Optional.empty();
        }
        final ByteBuffer bb = buffer.get();
        final byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        return Optional.of(new String(bytes, charset));
    }

}
