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

import java.lang.reflect.Field;

/**
 * Helper class to retrieve multicast DNS constants from their name.
 */
final class MulticastDnsHelper {

    /**
     * Constructor.
     */
    private MulticastDnsHelper() {
        // empty.
    }

    /**
     * Returns the value corresponding to the given class name.
     * <p>
     * Example: {@code classForName("IN")} returns {@link MulticastDns#CLASS_IN}.
     *
     * @param name constant name
     * @return constant value
     */
    static final short classForName(final String name) {
        return (short) forName("CLASS_" + name);
    }

    /**
     * Returns the value corresponding to the given flags name.
     * <p>
     * Example: {@code typeForName("QR_QUERY")} returns {@link MulticastDns#FLAGS_QR_QUERY}.
     *
     * @param name constant name
     * @return constant value
     */
    static final short flagsForName(final String name) {
        return (short) forName("FLAGS_" + name);
    }

    /**
     * Returns the value corresponding to the given type name.
     * <p>
     * Example: {@code typeForName("AAAA")} returns {@link MulticastDns#TYPE_AAAA}.
     *
     * @param name constant name
     * @return constant value
     */
    static final short typeForName(final String name) {
        return (short) forName("TYPE_" + name);
    }

    /**
     * Returns the value corresponding to the given constant name.
     *
     * @param name constant name
     * @return constant value
     */
    private static int forName(final String name) {
        try {
            final Field field = MulticastDns.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
