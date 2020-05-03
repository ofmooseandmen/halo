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

import static io.omam.halo.MulticastDns.decodeClass;

import java.util.Objects;

/**
 * DNS entry base class.
 */
abstract class DnsEntry {

    /** entry name. */
    private final String name;

    /** entry type */
    private final short type;

    /** entry class */
    private final short clazz;

    /** whether the entry class is unique. */
    private final boolean unique;

    /**
     * Constructor.
     *
     * @param aName entry name
     * @param aType entry type
     * @param aClass entry class
     */
    protected DnsEntry(final String aName, final short aType, final short aClass) {
        Objects.requireNonNull(aName);
        name = aName;
        type = aType;
        final short[] arr = decodeClass(aClass);
        clazz = arr[0];
        unique = arr[1] != 0;
    }

    /**
     * @return entry class.
     */
    final short clazz() {
        return clazz;
    }

    /**
     * @return {@code true} iff the class of this entry is unique.
     */
    final boolean isUnique() {
        return unique;
    }

    /**
     * @return entry name.
     */
    final String name() {
        return name;
    }

    /**
     * @return entry type.
     */
    final short type() {
        return type;
    }

}
