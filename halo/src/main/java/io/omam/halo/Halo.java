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

import static io.omam.halo.MulticastDns.TTL;

import java.io.Closeable;
import java.io.IOException;
import java.net.NetworkInterface;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * A multicast DNS Service Discovery, supporting {@link Service named service} registration, resolution and
 * browsing.
 * <p>
 * All registered services are de-registered upon {@link #close()}.
 * <h3>Registration</h3>
 *
 * <pre>
 * <code>
 * try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
 *     // allowing service instance name to be changed and with a default TTL of 1 hour.
 *     Service service = halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8009).get());
 *     // registered service is returned.
 *     System.err.println(service);
 *
 *     // registering again the service instance and registration type will return a service
 *     // with an instance name of "Foo Bar (2)".
 *     service = halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8010).get());
 *     System.err.println(service);
 *
 *     // not allowing service instance name to be changed will throw an IOException at this point.
 *     halo.register(Service.create("Foo Bar", "_http._udp.", (short) 8011).get(), false);
 * }
 * </code>
 * </pre>
 *
 * <h3>Resolution</h3>
 *
 * <pre>
 * <code>
 * try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
 *     // default timeout of 6 seconds.
 *     Optional<Service> service = halo.resolve("Foo Bar", "_http._udp.");
 *     // Optional contains the service if it could be resolved, empty otherwise.
 *     System.err.println(service);
 *
 *     // user defined timeout.
 *     service = halo.resolve("Foo Bar", "_http._udp.", Duration.ofSeconds(1));
 *     System.err.println(service);
 * }
 * </code>
 * </pre>
 */
public interface Halo extends Closeable {

    /**
     * Returns a new {@link Halo} instance sending/receiving mDNS messages on all network interfaces on this
     * machine.
     * <p>
     * The {@link NetworkInterface#isLoopback() loopback interface} is used only if no other network interface is
     * {@link NetworkInterface#isUp() up}.
     *
     * @param clock the clock providing access to the current instant for a time zone
     * @return a new {@link Halo}
     * @throws IOException in case of I/O error
     */
    public static Halo allNetworkInterfaces(final Clock clock) throws IOException {
        return new HaloImpl(clock, Collections.emptyList());
    }

    /**
     * Returns a new {@link Halo} instance sending/receiving mDNS messages on the given network interface(s) on
     * this machine.
     * <p>
     * The {@link NetworkInterface#isLoopback() loopback interface} is used only if no other network interface is
     * {@link NetworkInterface#isUp() up}.
     *
     * @param clock the clock providing access to the current instant for a time zone
     * @param nic network interface
     * @param nics other network interface
     * @return a new {@link Halo}
     * @throws IOException in case of I/O error
     */
    public static Halo networkInterfaces(final Clock clock, final NetworkInterface nic,
            final NetworkInterface... nics) throws IOException {
        final Collection<NetworkInterface> c = new ArrayList<>();
        c.add(nic);
        for (final NetworkInterface n : nics) {
            c.add(n);
        }
        return new HaloImpl(clock, c);
    }

    /**
     * De-register the given service.
     * <p>
     * This method performs no function, nor does it throw an exception, if the given service was not previously
     * registered or has already been de-registered.
     *
     * @param service service to de-register
     * @throws IOException if the service cannot be de-registered for any reason
     */
    void deregister(final Service service) throws IOException;

    /**
     * Registers the given service on the <strong>local</strong> domain with a TTL of 1 hour.
     * <p>
     * The {@link Service#instanceName() instance name} of the service will be changed to be unique if possible.
     *
     * @see #register(Service, Duration, boolean)
     * @param service service to register
     * @return the service that was successfully registered (instance name may have been changed)
     * @throws IOException if the service cannot be registered for any reason
     */
    default Service register(final Service service) throws IOException {
        return register(service, true);
    }

    /**
     * Registers the given service on the <strong>local</strong> domain with a TTL of 1 hour.
     * <p>
     * If {@code allowNameChange} is {@code true} the {@link Service#instanceName() instance name} of the service
     * will be changed to be unique if possible.
     *
     * @see #register(Service, Duration, boolean)
     * @param service service to register
     * @param allowNameChange {@code true} if {@link Service#instanceName() instance name} can be changed to be
     *            made unique
     * @return the service that was successfully registered (instance name may have been changed)
     * @throws IOException if the service cannot be registered for any reason
     */
    default Service register(final Service service, final boolean allowNameChange) throws IOException {
        return register(service, TTL, allowNameChange);
    }

    /**
     * Registers the given service on the <strong>local</strong> domain with the given TTL.
     * <p>
     * If {@code allowNameChange} is {@code true} the {@link Service#instanceName() instance name} of the service
     * will be changed to be unique if possible.
     *
     * @param service service to register
     * @param ttl service time-to-live
     * @param allowNameChange {@code true} if {@link Service#instanceName() instance name} can be changed to be
     *            made unique
     * @return the service that was successfully registered (instance name may have been changed)
     * @throws IOException if the service cannot be registered for any reason
     */
    Service register(final Service service, final Duration ttl, final boolean allowNameChange) throws IOException;

    /**
     * Resolves a service of the <strong>local</strong> domain by its instance name and registration type to a
     * target host, port and text record if it exits.
     * <p>
     * Resolution will timeout after 6 seconds.
     *
     * @see #resolve(String, String, Duration)
     * @param instanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     * @return the resolved service unless the timeout expired
     */
    default Optional<Service> resolve(final String instanceName, final String registrationType) {
        return resolve(instanceName, registrationType, Duration.ofSeconds(6));
    }

    /**
     * Resolves a service of the <strong>local</strong> domain by its instance name and registration type to a
     * target host, port and text record if it exits.
     *
     * @param instanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param registrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     * @param timeout for resolution
     * @return the resolved service unless the timeout expired
     */
    Optional<Service> resolve(final String instanceName, final String registrationType, final Duration timeout);

}
