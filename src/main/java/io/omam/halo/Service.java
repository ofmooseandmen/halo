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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Optional;

/**
 * A named service (printer, speaker, etc). The service {@link Service#hostname()} always ends with the '.local'
 * top-level domain.
 *
 * @see Attributes
 */
public interface Service {

    /**
     * Returns the service attributes.
     *
     * @return the service attributes.
     */
    Attributes attributes();

    /**
     * Returns the name of the service host, always ending with '.local.'.
     *
     * @return the name of the service host
     */
    String hostname();

    /**
     * Returns the service instance name, e.g. 'Living Room Printer'.
     *
     * @return the service instance name
     */
    String instanceName();

    /**
     * Returns the IPv4 address of the service if any.
     * <p>
     * The returned address is guaranteed to be instance of {@link Inet4Address}.
     * <p>
     * A service has at least one IP address (v4 or v6 or both).
     *
     * @return the IPv4 address of the service if any
     */
    Optional<InetAddress> ipv4Address();

    /**
     * Returns the IPv4 address of the service if any.
     * <p>
     * The returned address is guaranteed to be instance of {@link Inet6Address}.
     * <p>
     * A service has at least one IP address (v4 or v6 or both).
     *
     * @return the IPv4 address of the service if any
     */
    Optional<InetAddress> ipv6Address();

    /**
     * Returns the service qualified name, e.g. 'Living Room Printer._music._udp.local.'.
     *
     * @return the service qualified name
     */
    String name();

    /**
     * @return the port number of the service
     */
    short port();

    /**
     * Returns the registration pointer name of the service, e.g. '_music._udp.local.'.
     *
     * @return the registration pointer name of the service
     */
    String registrationPointerName();

    /**
     * Returns the registration type of the service, e.g. '_music._udp.'.
     *
     * @return the registration type of the service
     */
    String registrationType();

}
