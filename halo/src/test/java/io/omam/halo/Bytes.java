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

import cucumber.api.DataTable;

/**
 * Transforms {@link DataTable} into {@code byte[]}.
 */
final class Bytes {

    /**
     * Constructor.
     */
    private Bytes() {
        // empty.
    }

    /**
     * Parses the given data table into an array of bytes.
     * <p>
     * Expected format:
     *
     * <pre>
     * | 0x0 | 0x0 | 0x0 | 0x0 |...
     * ...
     * </pre>
     *
     * @param data data table
     * @return array of bytes
     */
    public static byte[] parse(final DataTable data) {
        final Integer[] bytes = data
            .raw()
            .stream()
            .flatMap(l -> l.stream())
            .filter(s -> !s.isEmpty())
            .map(s -> Integer.decode(s))
            .toArray(Integer[]::new);
        int i = 0;
        final byte[] res = new byte[bytes.length];
        for (final Integer b : bytes) {
            res[i++] = b.byteValue();
        }
        return res;

    }

}