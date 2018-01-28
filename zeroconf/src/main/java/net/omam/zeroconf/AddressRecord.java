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

import static net.omam.zeroconf.MulticastDns.TYPE_A;
import static net.omam.zeroconf.MulticastDns.TYPE_AAAA;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * A DNS address record.
 */
@SuppressWarnings("javadoc")
final class AddressRecord extends DnsRecord {

    private final InetAddress address;

    private AddressRecord(final String aName, final short aType, final short aClass, final Duration aTtl,
            final Instant now, final InetAddress anAddress) {
        super(aName, aType, aClass, aTtl, now);
        address = anAddress;
    }

    static AddressRecord ipv4(final String name, final short clazz, final Duration ttl, final Instant now,
            final InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            throw new IllegalArgumentException("address must be IPV4");
        }
        return new AddressRecord(name, TYPE_A, clazz, ttl, now, address);
    }

    static AddressRecord ipv6(final String name, final short clazz, final Duration ttl, final Instant now,
            final InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            throw new IllegalArgumentException("address must be IPV6");
        }
        return new AddressRecord(name, TYPE_AAAA, clazz, ttl, now, address);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return address.equals(((AddressRecord) obj).address);
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + address.hashCode();
        return result;
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

    final InetAddress address() {
        return address;
    }

    final boolean ipv4() {
        return type() == TYPE_A;
    }

    final boolean ipv6() {
        return type() == TYPE_AAAA;
    }

}
