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

import static io.omam.halo.Assert.assertServiceEquals;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.jmdns.ServiceListener;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests browsing by registration type.
 */
@SuppressWarnings("javadoc")
public final class BrowsingSteps {

    private static class CollectingBrowserListener implements BrowserListener {

        private final Queue<Service> ups;

        private final Queue<Service> downs;

        CollectingBrowserListener() {
            ups = new ArrayDeque<>();
            downs = new ArrayDeque<>();
        }

        @Override
        public final void down(final Service service) {
            downs.add(service);
        }

        @Override
        public final void up(final Service service) {
            ups.add(service);
        }

        final int events() {
            return downs.size() + ups.size();
        }

        final Service pollDown() {
            return downs.poll();
        }

        final Service pollUp() {
            return ups.poll();
        }

    }

    private final Engines engines;

    private final Map<String, CollectingBrowserListener> hls;

    private final Map<String, Browser> hbs;

    private final Map<String, ServiceListener> jls;

    private String browsedBy;

    public BrowsingSteps(final Engines someEngines) {
        engines = someEngines;
        hls = new HashMap<>();
        hbs = new HashMap<>();
        jls = new HashMap<>();
        browsedBy = null;
    }

    @After
    public final void after() {
        assertTrue(hls.isEmpty());
        assertTrue(jls.isEmpty());
        assertTrue(hbs.isEmpty());
        browsedBy = null;
    }

    @Given("^the following registration types are being browsed with \"([^\"]*)\":$")
    public final void givenRegistrationTypesBrowsed(final String engine, final List<RegistrationType> types) {
        whenRegistrationTypesBrowsed(engine, types);
    }

    @Then("^the listener \"([^\"]*)\" shall be notified of the following \"([^\"]*)\" services:$")
    public final void thenListenerNotified(final String listener, final String eventType,
            final List<ServiceDetails> services) {
        if (browsedBy.equals("Halo")) {
            final CollectingBrowserListener l = hls.remove(listener);
            /* await for listener to have received expected number of events. */
            await().atMost(5, SECONDS).until(() -> l.events() == services.size());
            for (final ServiceDetails expected : services) {
                final Service actual;
                if (eventType.equals("up")) {
                    actual = l.pollUp();
                } else {
                    actual = l.pollDown();
                }
                assertNotNull(actual);
                assertServiceEquals(expected, actual);
            }
            hbs.remove(listener).stop();
        } else {
            fail("Implement tests browsing with JmDNS to ensure Halo behave correctly.");
        }
        browsedBy = null;
    }

    @When("^the following registration types are browsed with \"([^\"]*)\":$")
    public final void whenRegistrationTypesBrowsed(final String engine, final List<RegistrationType> types) {
        if (engine.equals("Halo")) {
            for (final RegistrationType rt : types) {
                final CollectingBrowserListener l = new CollectingBrowserListener();
                hls.put(rt.listenerName(), l);
                final Browser b = engines.halo().browse(rt.registrationType(), l);
                hbs.put(rt.listenerName(), b);
            }
        } else {
            fail("Implement tests browsing with JmDNS to ensure Halo behave correctly.");
        }
        browsedBy = engine;
    }

}
