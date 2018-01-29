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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Multicast-DNS constants.
 */
final class MulticastDns {

    /** discovery service. */
    static final String DISCOVERY = "_services._dns-sd._udp.local";

    /** maximum size of DNS message in bytes. */
    static final int MAX_DNS_MESSAGE_SIZE = 65536;

    /** mDNS IPV4 address. */
    static final InetAddress IPV4_ADDR;

    /** mDNS IPV6 address. */
    static final InetAddress IPV6_ADDR;

    /** mDNS port. */
    static final int MDNS_PORT = 5353;

    /** IPV4 socket address. */
    static final InetSocketAddress IPV4_SOA;

    /** IPV6 socket address. */
    static final InetSocketAddress IPV6_SOA;

    static {
        try {
            IPV4_ADDR = InetAddress.getByName("224.0.0.251");
            IPV6_ADDR = InetAddress.getByName("FF02::FB");
            IPV4_SOA = new InetSocketAddress(IPV4_ADDR, MDNS_PORT);
            IPV6_SOA = new InetSocketAddress(IPV6_ADDR, MDNS_PORT);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** initial random delay. */
    static final Duration PROBE_INTERVAL = Duration.ofMillis(250);

    /** number of probe packets. */
    static final int PROBE_NUM = 3;

    /** time to live: 1 hour. */
    static final Duration TTL = Duration.ofHours(1);

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

    /** class mask (unsigned). */
    static final short CLASS_MASK = 0x7FFF;

    /** unique class (unsigned). */
    static final short CLASS_UNIQUE = (short) 0x8000;

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

    /**
     * Constructor.
     */
    private MulticastDns() {
        // empty.
    }

}
