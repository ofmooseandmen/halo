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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("javadoc")
public interface Service {

    public static final class Builder implements Supplier<Service> {

        private final ServiceImpl si;

        Builder(final String instanceName, final String registrationType, final short port) {
            si = new ServiceImpl(instanceName, registrationType);
            si.setPort(port);
            si.setPriority((short) 0);
            si.setWeight((short) 0);
            try {
                final InetAddress local = InetAddress.getLocalHost();
                si.setHostname(local.getHostName());
                if (local instanceof Inet4Address) {
                    si.setIpv4Address((Inet4Address) local);
                } else if (local instanceof Inet6Address) {
                    si.setIpv6Address((Inet6Address) local);
                }
            } catch (final UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }

        public final Builder attributes(final Attributes attributes) {
            si.setAttributes(attributes);
            return this;
        }

        @Override
        public final Service get() {
            return si;
        }

        public final Builder hostname(final String hostname) {
            si.setHostname(hostname);
            return this;
        }

        public final Builder ipv4Address(final Inet4Address address) {
            si.setIpv4Address(address);
            return this;
        }

        public final Builder ipv6Address(final Inet6Address address) {
            si.setIpv6Address(address);
            return this;
        }

        public final Builder port(final short port) {
            si.setPort(port);
            return this;
        }

        public final Builder priority(final short priority) {
            si.setPriority(priority);
            return this;
        }

        public final Builder weight(final short weight) {
            si.setWeight(weight);
            return this;
        }

    }

    /**
     * @param instanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     */
    public static Builder create(final String instanceName, final String registrationType, final short port) {
        return new Builder(instanceName, registrationType, port);
    }

    Optional<Attributes> attributes();

    Optional<String> hostname();

    String instanceName();

    Optional<Inet4Address> ipv4Address();

    Optional<Inet6Address> ipv6Address();

    short port();

    short priority();

    String registrationPointerName();

    String registrationType();

    String serviceName();

    short weight();

}
