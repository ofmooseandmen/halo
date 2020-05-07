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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A service that can be registered on the <strong>local</strong> domain.
 *
 * <pre>
 * <code>
 * RegisterableService
 *     .create("Living Room", "_music._udp.", 8009)
 *     .ipv4Address(InetAddress.getByName("192.168.0.154"))
 *     .ipv6Address(InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
 *     .attributes(Attributes.create().with("foo").get())
 *     .get();
 * </code>
 * </pre>
 */
public interface RegisterableService extends Service {

    /**
     * {@link Service} builder.
     * <p>
     * Only the service {@link RegisterableService#instanceName() instance name},
     * {@link RegisterableService#registrationType() registration type} and {@link Service#port() port} are
     * mandatory. Other fields default to:
     * <ul>
     * <li>{@link RegisterableService#hostname() hostname}: local hostname, appended with '.local.' if needed
     * <li>{@link RegisterableService#ipv4Address() IPv4} or {@link Service#ipv6Address() IPv6}: local hostname
     * address
     * <li>{@link RegisterableService#attributes() attributes}: empty key with no value
     * </ul>
     */
    public static final class Builder implements Supplier<RegisterableService> {

        /** address of the local host. */
        private static final InetAddress LOCAL_HOST;
        static {
            try {
                LOCAL_HOST = InetAddress.getLocalHost();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }

        /** service instance name. */
        private final String instanceName;

        /** service registration type. */
        private final String registrationType;

        /** service hostname. */
        private String hostname;

        /** service IPv4 address. */
        private Optional<InetAddress> ipv4Address;

        /** service IPv6 address. */
        private Optional<InetAddress> ipv6Address;

        /** service port. */
        private final short port;

        /** service attributes. */
        private Attributes attributes;

        /**
         * Constructor.
         *
         * @param anInstanceName the service instance name, a human-readable string, e.g. 'Living Room Printer'
         * @param aRegistrationType service type (IANA) and transport protocol (udp or tcp), e.g. '_ftp._tcp.' or
         *            {@code _http._udp.}
         * @param aPort service port number
         */
        Builder(final String anInstanceName, final String aRegistrationType, final int aPort) {
            instanceName = anInstanceName;
            registrationType = aRegistrationType;
            port = (short) aPort;
            hostname = LOCAL_HOST.getHostName();
            if (LOCAL_HOST instanceof Inet4Address) {
                ipv4Address = Optional.of((Inet4Address) LOCAL_HOST);
                ipv6Address = Optional.empty();
            } else if (LOCAL_HOST instanceof Inet6Address) {
                ipv4Address = Optional.empty();
                ipv6Address = Optional.of((Inet6Address) LOCAL_HOST);
            }
            attributes = Attributes.empty();
        }

        /**
         * Sets the {@link Service#attributes() attributes} of the service being built to the given value.
         *
         * @param someAttributes attributes
         * @return this builder
         */
        public final Builder attributes(final Attributes someAttributes) {
            attributes = someAttributes;
            return this;
        }

        @Override
        public final RegisterableService get() {
            return new RegisterableServiceImpl(instanceName, registrationType, hostname, ipv4Address, ipv6Address,
                                               port, attributes);
        }

        /**
         * Sets the {@link Service#hostname() hostname} of the service being built to the given value.
         * <p>
         * {@code .local} is appended to the given hostname if it does not end with it.
         *
         * @param aHostname hostname
         * @return this builder
         */
        public final Builder hostname(final String aHostname) {
            hostname = aHostname;
            return this;
        }

        /**
         * Sets the {@link Service#ipv4Address() IPv4 address} of the service being built to the given value.
         *
         * @param address IPv4 address
         * @return this builder
         */
        public final Builder ipv4Address(final Inet4Address address) {
            ipv4Address = Optional.of(address);
            return this;
        }

        /**
         * Sets the {@link Service#ipv6Address() IPv6 address} of the service being built to the given value.
         *
         * @param address IPv6 address
         * @return this builder
         */
        public final Builder ipv6Address(final Inet6Address address) {
            ipv6Address = Optional.of(address);
            return this;
        }

    }

    /**
     * Returns a new {@code builder} to create a new {@link Service service} with the given mandatory parameters.
     *
     * @param instanceName the service instance name, a human-readable string, e.g. 'Living Room Printer'
     * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. '_ftp._tcp. or
     *            '_http._udp.'
     * @param port service port, e.g. {@code 8009}
     * @return a new {@code Builder}
     */
    public static Builder create(final String instanceName, final String registrationType, final int port) {
        return new Builder(instanceName, registrationType, port);
    }

}
