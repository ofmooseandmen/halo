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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Properties;

/**
 * Configurable system properties related to multicast-DNS service discovery.
 * <p>
 * The following parameters can be configured by system properties:
 * <table>
 * <caption>Summary of Halo system properties</caption>
 * <tr>
 * <td><b>Property Key</b></td>
 * <td><b>Description</b></td>
 * <td><b>Default</b></td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.mdns.ipv4</td>
 * <td>mDNS IPV4 address</td>
 * <td>224.0.0.251</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.mdns.ipv6</td>
 * <td>mDNS IPV6 address</td>
 * <td>FF02::FB</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.mdns.port</td>
 * <td>mDNS port</td>
 * <td>5353</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.resolution.timeout</td>
 * <td>resolution timeout in milliseconds</td>
 * <td>6000</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.resolution.interval</td>
 * <td>interval between resolution questions in milliseconds</td>
 * <td>200</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.probing.timeout</td>
 * <td>probing timeout in milliseconds</td>
 * <td>6000</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.probing.interval</td>
 * <td>interval between probe messages in milliseconds</td>
 * <td>250</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.probing.number</td>
 * <td>number of probing messages before announcing a registered service</td>
 * <td>3</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.querying.delay</td>
 * <td>delay before transmitting a browsing query in milliseconds</td>
 * <td>120</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.querying.interval</td>
 * <td>interval between browsing queries in milliseconds</td>
 * <td>1200000</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.querying.number</td>
 * <td>number of browsing queries</td>
 * <td>3</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.cancellation.interval</td>
 * <td>interval between goodbye messages in milliseconds</td>
 * <td>250</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.cancellation.number</td>
 * <td>number of goodbye messages sent when de-registering a service</td>
 * <td>3</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.reaper.interval</td>
 * <td>cache record reaper interval in milliseconds</td>
 * <td>10000</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.ttl.default</td>
 * <td>DNS record default time to live in milliseconds</td>
 * <td>3600000</td>
 * </tr>
 * <tr>
 * <td>io.omam.halo.ttl.expiry</td>
 * <td>DNS record time to live after expiry in milliseconds</td>
 * <td>1000</td>
 * </tr>
 * </table>
 */
public final class HaloProperties {

    /** mDNS IPV4 address. */
    public static final InetAddress IPV4_ADDR;

    /** mDNS IPV6 address. */
    public static final InetAddress IPV6_ADDR;

    /** mDNS port. */
    public static final int MDNS_PORT;

    /** IPV4 socket address. */
    public static final InetSocketAddress IPV4_SOA;

    /** IPV6 socket address. */
    public static final InetSocketAddress IPV6_SOA;

    /** interval between probe message. */
    public static final Duration PROBING_INTERVAL;

    /** probing timeout. */
    public static final Duration PROBING_TIMEOUT;

    /** number of probes before announcing a registered service. */
    public static final int PROBE_NUM;

    /** interval between goodbyes messages. */
    public static final Duration CANCELLING_INTERVAL;

    /** number of cancel message sent when de-registering a service. */
    public static final int CANCEL_NUM;

    /** cache record reaper interval. */
    public static final Duration REAPING_INTERVAL;

    /** default resolution timeout. */
    public static final Duration RESOLUTION_TIMEOUT;

    /** interval between resolution question. */
    public static final Duration RESOLUTION_INTERVAL;

    /** number of queries. */
    public static final int QUERY_NUM;

    /** interval between browsing query. */
    public static final Duration QUERYING_DELAY;

    /** interval between browsing query. */
    public static final Duration QUERYING_INTERVAL;

    /** time to live: 1 hour. */
    public static final Duration TTL;

    /** time to live after expiry: 1 second. */
    public static final Duration EXPIRY_TTL;

    static {
        try (final InputStream input =
                HaloProperties.class.getClassLoader().getResourceAsStream("halo.properties")) {
            final Properties props = new Properties();
            props.load(input);
            IPV4_ADDR = InetAddress.getByName(stringProp("io.omam.halo.mdns.ipv4", props));
            IPV6_ADDR = InetAddress.getByName(stringProp("io.omam.halo.mdns.ipv6", props));
            MDNS_PORT = intProp("io.omam.halo.mdns.port", props);
            IPV4_SOA = new InetSocketAddress(IPV4_ADDR, MDNS_PORT);
            IPV6_SOA = new InetSocketAddress(IPV6_ADDR, MDNS_PORT);

            RESOLUTION_TIMEOUT = durationProp("io.omam.halo.resolution.timeout", props);
            RESOLUTION_INTERVAL = durationProp("io.omam.halo.resolution.interval", props);

            PROBING_TIMEOUT = durationProp("io.omam.halo.probing.timeout", props);
            PROBING_INTERVAL = durationProp("io.omam.halo.probing.interval", props);
            PROBE_NUM = intProp("io.omam.halo.probing.number", props);

            QUERYING_DELAY = durationProp("io.omam.halo.querying.delay", props);
            QUERYING_INTERVAL = durationProp("io.omam.halo.querying.interval", props);
            QUERY_NUM = intProp("io.omam.halo.querying.number", props);

            CANCELLING_INTERVAL = durationProp("io.omam.halo.cancellation.interval", props);
            CANCEL_NUM = intProp("io.omam.halo.cancellation.number", props);

            REAPING_INTERVAL = durationProp("io.omam.halo.reaper.interval", props);

            TTL = durationProp("io.omam.halo.ttl.default", props);
            EXPIRY_TTL = durationProp("io.omam.halo.ttl.expiry", props);

        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Constructor.
     */
    private HaloProperties() {
        // empty.
    }

    /**
     * Returns the {@code Duration} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static Duration durationProp(final String key, final Properties props) {
        return Duration.ofMillis(Long.parseLong(stringProp(key, props)));
    }

    /**
     * Returns the {@code int} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static int intProp(final String key, final Properties props) {
        return Integer.parseInt(stringProp(key, props));
    }

    /**
     * Returns the {@code String} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static String stringProp(final String key, final Properties props) {
        return System.getProperty(key, props.getProperty(key));
    }

}
