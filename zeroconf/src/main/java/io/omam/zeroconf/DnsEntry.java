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

import static io.omam.zeroconf.MulticastDns.CLASS_MASK;
import static io.omam.zeroconf.MulticastDns.CLASS_UNIQUE;

import java.util.Objects;

/**
 * DNS entry base class.
 */
@SuppressWarnings("javadoc")
abstract class DnsEntry {

    private final String name;

    private final short type;

    private final short clazz;

    protected DnsEntry(final String aName, final short aType, final short aClass) {
        Objects.requireNonNull(aName);
        name = aName;
        type = aType;
        clazz = (short) (aClass & CLASS_MASK);
    }

    final short clazz() {
        return clazz;
    }

    final boolean isUnique() {
        return (clazz & CLASS_UNIQUE) != 0;
    }

    final String name() {
        return name;
    }

    final short type() {
        return type;
    }

}
