/*
Copyright 2020-2020 Cedric Liegeois

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

import java.util.Locale;

/**
 * Constants and utility methods related to multicast-DNS service discovery.
 */
final class MulticastDnsSd {

    /** the domain: always local. */
    static final String DOMAIN = "local";

    /** registration types discovery. */
    static final String RT_DISCOVERY = "_services._dns-sd._udp.local.";

    /** maximum size of DNS message in bytes. */
    static final int MAX_DNS_MESSAGE_SIZE = 65_536;

    /** query or response mask (unsigned). */
    static final short FLAGS_QR_MASK = (short) 0x8000;

    /** query flag (unsigned). */
    static final short FLAGS_QR_QUERY = 0x0000;

    /** response flag (unsigned). */
    static final short FLAGS_QR_RESPONSE = (short) 0x8000;

    /** authoritative answer flag (unsigned). */
    static final short FLAGS_AA = 0x0400;

    /** Internet class. */
    static final short CLASS_IN = 1;

    /** any class. */
    static final short CLASS_ANY = 255;

    /** type A (IPV4 address) record. */
    static final short TYPE_A = 1;

    /** pointer record. */
    static final short TYPE_PTR = 12;

    /** text record. */
    static final short TYPE_TXT = 16;

    /** type AAAA (IPV6 address) record. */
    static final short TYPE_AAAA = 28;

    /** server record. */
    static final short TYPE_SRV = 33;

    /** any record. */
    static final short TYPE_ANY = 255;

    /** class mask (unsigned). */
    private static final short CLASS_MASK = 0x7FFF;

    /** unique class (unsigned). */
    private static final short CLASS_UNIQUE = (short) 0x8000;

    /**
     * Constructor.
     */
    private MulticastDnsSd() {
        // empty.
    }

    /**
     * Decodes the given class and returns an array with the class index and whether the class is unique (a value
     * different from 0 denotes a unique class).
     *
     * @param clazz class
     * @return an array of 2 shorts, first is class index, second whether class is unique
     */
    static short[] decodeClass(final short clazz) {
        return new short[] { (short) (clazz & CLASS_MASK), (short) (clazz & CLASS_UNIQUE) };
    }

    /**
     * Encodes the given class index and whether it is unique into a class. This is the reverse operation of
     * {@link #encodeClass(short, boolean)}.
     *
     * @param classIndex class index
     * @param unique whether the class is unique
     * @return encoded class
     */
    static short encodeClass(final short classIndex, final boolean unique) {
        return (short) (classIndex | (unique ? CLASS_UNIQUE : 0));
    }

    /**
     * Returns {@code value.toLowerCase(Locale.ROOT)}.
     *
     * @param value string
     * @return lower case string using ROOT locale
     */
    static String toLowerCase(final String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Makes the given class unique.
     *
     * @param classIndex class index
     * @return unique class
     */
    static short uniqueClass(final short classIndex) {
        return (short) (classIndex | CLASS_UNIQUE);
    }

}
