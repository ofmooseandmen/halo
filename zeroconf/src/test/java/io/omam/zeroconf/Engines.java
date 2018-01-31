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

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;

/**
 * Steps pertaining to the creation of the JmDNS or Zeroconf engine.
 */
@SuppressWarnings("javadoc")
public final class Engines {

    private JmDNS jmdns;

    private Zeroconf zc;

    /**
     * Constructor.
     */
    public Engines() {
        // empty.
    }

    @After
    public final void after() throws IOException {
        try {
            if (jmdns != null) {
                jmdns.close();
            }
        } finally {
            if (zc != null) {
                zc.close();
            }
        }
    }

    @Given("^a \"(Zeroconf|JmDNS)\" instance has been created$")
    public final void givenInstanceCreated(final String engine) throws IOException {
        if (engine.equals("Zeroconf")) {
            if (zc != null) {
                throw new AssertionError("Zeroconf already created");
            }
            zc = Zeroconf.allNetworkInterfaces(Clock.systemDefaultZone());
        } else {
            if (jmdns != null) {
                throw new AssertionError("JmDNS already created");
            }
            jmdns = JmDNS.create();
        }
    }

    final Map<String, String> attributes(final ServiceInfo actual) {
        final Map<String, String> atts = new HashMap<>();
        for (final Enumeration<String> keys = actual.getPropertyNames(); keys.hasMoreElements();) {
            final String key = keys.nextElement();
            final String value = actual.getPropertyString(key);
            atts.put(key, value);
        }
        return atts;
    }

    final JmDNS jmdns() {
        return jmdns;
    }

    final ServiceInfo toJmdns(final ServiceDetails sd) {
        assertFalse(sd.hostname().isPresent());
        return ServiceInfo.create(sd.registrationType() + "local.", sd.instanceName(), sd.port(), sd.weight(),
                sd.priority(), toJmdns(sd.text()));
    }

    final Map<String, String> toJmdns(final String attributes) {
        final Map<String, String> atts = new HashMap<>();
        /* for some reason JmDNS returns true if the attribute has no value. */
        atts.put(attributes, "true");
        return atts;
    }

    final Service toZc(final ServiceDetails sd) throws UnknownHostException {
        /*
         * fake hostname and IP to trigger probing conflict.
         */
        final Optional<String> hostname = sd.hostname();
        final Optional<InetAddress> ip = hostname.isPresent()
                ? Optional.of(InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
                : Optional.empty();
        final Service.Builder s = Service
            .create(sd.instanceName(), sd.registrationType(), sd.port())
            .priority(sd.priority())
            .attributes(toZc(sd.text()))
            .weight(sd.weight());
        hostname.ifPresent(h -> s.hostname(h));
        ip.ifPresent(i -> s.ipv6Address((Inet6Address) i));
        return s.get();
    }

    final Attributes toZc(final String attributes) {
        /* for some reason JmDNS returns true if the attribute has no value. */
        return Attributes.create().with(attributes, "true", StandardCharsets.UTF_8).get();
    }

    final Zeroconf zc() {
        return zc;
    }

}
