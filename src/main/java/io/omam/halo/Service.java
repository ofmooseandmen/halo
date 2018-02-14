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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A named service (printer, speaker, etc). The service {@link Service#hostname()} always ends with the '.local'
 * top-level domain.
 *
 * <pre>
 * <code>
 * Service.create("Living Room", "_music._udp.", sd.port(8009))
 *        .ipv4Address(InetAddress.getByName("192.168.0.154"))
 *        .ipv6Address(InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
 *        .attributes(Attributes.create().with("foo").get())
 *        .get();
 * </code>
 * </pre>
 *
 * @see Attributes
 */
public interface Service {

    /**
     * {@link Service} builder.
     * <p>
     * Only the service {@link Service#instanceName() instance name}, {@link Service#registrationType()
     * registration type} and {@link Service#port() port} are mandatory. Other fields default to:
     * <ul>
     * <li>{@link Service#hostname() hostname}: local hostname, appended with '.local.' if needed
     * <li>{@link Service#ipv4Address() IPv4} or {@link Service#ipv6Address() IPv6}: local hostname address
     * <li>{@link Service#attributes() attributes}: empty key with no value
     * </ul>
     */
    public static final class Builder implements Supplier<Service> {

        /** address of the local host. */
        private static final InetAddress LOCAL_HOST;
        static {
            try {
                LOCAL_HOST = InetAddress.getLocalHost();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }

        /** service being built> */
        private final ServiceImpl si;

        /**
         * Constructor.
         *
         * @param instanceName the service instance name, a human-readable string, e.g. 'Living Room Printer'
         * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. '_ftp._tcp.' or
         *            {@code _http._udp.}
         * @param port service port number
         */
        Builder(final String instanceName, final String registrationType, final int port) {
            si = new ServiceImpl(instanceName, registrationType);
            si.setPort((short) port);
            si.setAttributes(Attributes.create().with("").get());
            si.setHostname(LOCAL_HOST.getHostName());
            if (LOCAL_HOST instanceof Inet4Address) {
                si.setIpv4Address((Inet4Address) LOCAL_HOST);
            } else if (LOCAL_HOST instanceof Inet6Address) {
                si.setIpv6Address((Inet6Address) LOCAL_HOST);
            }
        }

        /**
         * Sets the {@link Service#attributes() attributes} of the service being built to the given value.
         *
         * @param attributes attributes
         * @return this builder
         */
        public final Builder attributes(final Attributes attributes) {
            si.setAttributes(attributes);
            return this;
        }

        @Override
        public final Service get() {
            return si;
        }

        /**
         * Sets the {@link Service#hostname() hostname} of the service being built to the given value.
         * <p>
         * {@code .local} is appended to the given hostname if it does not end with it.
         *
         * @param hostname hostname
         * @return this builder
         */
        public final Builder hostname(final String hostname) {
            si.setHostname(hostname);
            return this;
        }

        /**
         * Sets the {@link Service#ipv4Address() IPv4 address} of the service being built to the given value.
         *
         * @param address IPv4 address
         * @return this builder
         */
        public final Builder ipv4Address(final Inet4Address address) {
            si.setIpv4Address(address);
            return this;
        }

        /**
         * Sets the {@link Service#ipv6Address() IPv6 address} of the service being built to the given value.
         *
         * @param address IPv6 address
         * @return this builder
         */
        public final Builder ipv6Address(final Inet6Address address) {
            si.setIpv6Address(address);
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
