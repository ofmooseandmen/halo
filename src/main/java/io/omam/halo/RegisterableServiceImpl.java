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

import static io.omam.halo.MulticastDnsSd.DOMAIN;

import java.net.InetAddress;
import java.util.Optional;

/**
 * RegisterableService implementation.
 */
final class RegisterableServiceImpl extends BaseRegistrableService implements RegisterableService {

    /** service attributes. */
    private final Attributes attributes;

    /**
     * Constructor.
     *
     * @param anInstanceName new instance name
     * @param original original service
     */
    RegisterableServiceImpl(final String anInstanceName, final RegisterableService original) {
        this(anInstanceName, original.registrationType(), original.hostname(), original.ipv4Address(),
             original.ipv6Address(), original.port(), original.attributes());
    }

    /**
     *
     * Constructor.
     *
     * @param anInstanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param aRegistrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     * @param aHostname service local hostname
     * @param anIpv4Address service IPv4 address
     * @param anIpv6Address service IPv6 address
     * @param aPort service port
     * @param someAttributes service attributes
     */
    RegisterableServiceImpl(final String anInstanceName, final String aRegistrationType, final String aHostname,
            final Optional<InetAddress> anIpv4Address, final Optional<InetAddress> anIpv6Address,
            final short aPort, final Attributes someAttributes) {
        super(anInstanceName, aRegistrationType, actualHostname(aHostname), anIpv4Address, anIpv6Address, aPort);
        attributes = someAttributes;
    }

    /**
     * Computes the actual hostname, appending '.local.' if needed.
     *
     * @param hostname the hostname
     * @return the actual hostname
     */
    private static String actualHostname(final String hostname) {
        String localHostname = hostname;
        final int index = localHostname.indexOf("." + DOMAIN);
        if (index > 0) {
            localHostname = localHostname.substring(0, index);
        }
        localHostname = localHostname.replaceAll("[:%\\.]", "-");
        localHostname += "." + DOMAIN + ".";
        return localHostname;
    }

    @Override
    public final Attributes attributes() {
        return attributes;
    }

}
