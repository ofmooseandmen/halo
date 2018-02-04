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

import static io.omam.halo.MulticastDns.TYPE_A;
import static io.omam.halo.MulticastDns.TYPE_AAAA;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * Address record (A or AAAA).
 */
final class AddressRecord extends DnsRecord {

    /** IP address. */
    private final InetAddress address;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     * @param anAddress IP address
     */
    AddressRecord(final String aName, final short aClass, final Duration aTtl, final Instant now,
            final InetAddress anAddress) {
        super(aName, type(anAddress), aClass, aTtl, now);
        address = anAddress;
    }

    /**
     * Returns the record type for the given address.
     *
     * @param address address
     * @return record type
     */
    private static short type(final InetAddress address) {
        return address instanceof Inet4Address ? TYPE_A : TYPE_AAAA;
    }

    @Override
    public final String toString() {
        return "AddressRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", address="
            + address
            + "]";
    }

    @Override
    protected void write(final MessageOutputStream mos) {
        mos.writeBytes(address.getAddress());
    }

    /**
     * @return the IP address.
     */
    final InetAddress address() {
        return address;
    }

}
