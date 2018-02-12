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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests browsing by registration type.
 */
@SuppressWarnings("javadoc")
public final class ServiceBrowsingSteps {

    private static final class CollectingBrowserListener implements ServiceBrowserListener {

        private final List<Service> ups;

        private final List<Service> downs;

        CollectingBrowserListener() {
            ups = new ArrayList<>();
            downs = new ArrayList<>();
        }

        @Override
        public final void down(final Service service) {
            downs.add(service);
        }

        @Override
        public final void up(final Service service) {
            ups.add(service);
        }

        final List<Service> downs() {
            return downs;
        }

        final List<Service> ups() {
            return ups;
        }

    }

    private static final class CollectingServiceListener implements ServiceListener {

        private final List<ServiceInfo> ups;

        CollectingServiceListener() {
            ups = new ArrayList<>();
        }

        @Override
        public final void serviceAdded(final ServiceEvent event) {
            // ignore: wait for resolved.
        }

        @Override
        public final void serviceRemoved(final ServiceEvent event) {
            // ignore: not tested.
        }

        @Override
        public final void serviceResolved(final ServiceEvent event) {
            /* for some reason JmDNS sometimes notified several times for the same service. */
            if (!ups.contains(event.getInfo())) {
                ups.add(event.getInfo());
            }
        }

        final List<ServiceInfo> ups() {
            return ups;
        }

    }

    private final Engines engines;

    private final Map<String, CollectingBrowserListener> hls;

    private final Map<String, Browser> hbs;

    private final Map<String, CollectingServiceListener> jls;

    private String browsedBy;

    public ServiceBrowsingSteps(final Engines someEngines) {
        engines = someEngines;
        hls = new HashMap<>();
        hbs = new HashMap<>();
        jls = new HashMap<>();
        browsedBy = null;
    }

    @After
    public final void after() {
        hls.clear();
        jls.clear();
        hbs.values().forEach(Browser::stop);
        hbs.clear();
        browsedBy = null;
    }

    @Given("the browser associated with the listener \"([^\"]*)\" has been stopped")
    public final void givenBrowserStopped(final String listener) {
        hbs.remove(listener).stop();
    }

    @Given("^the following registration types are being browsed with \"([^\"]*)\":$")
    public final void givenRegistrationTypesBrowsed(final String engine, final List<RegistrationType> types) {
        whenRegistrationTypesBrowsed(engine, types);
    }

    @Then("^the listener \"([^\"]*)\" shall be notified of the following \"([^\"]*)\" services:$")
    public final void thenListenerNotified(final String listener, final String eventType,
            final List<ServiceDetails> services) {
        /* sort expecteds and actuals by instance name. */
        final List<ServiceDetails> expecteds = new ArrayList<>(services);
        Collections.sort(expecteds, (s1, s2) -> s1.instanceName().compareTo(s2.instanceName()));

        final boolean up = eventType.equals("up");
        /* Halo and JmDNS service resolution timeout is 6 seconds. */
        final long timeout = services.size() * 6000;
        if (browsedBy.equals("Halo")) {
            final CollectingBrowserListener l = hls.get(listener);
            final List<Service> actuals = up ? l.ups() : l.downs();
            /* await for listener to have received expected number of events. */
            await().atMost(timeout, SECONDS).until(() -> actuals.size(), equalTo(expecteds.size()));
            Collections.sort(actuals, (s1, s2) -> s1.instanceName().compareTo(s2.instanceName()));
            for (int i = 0; i < expecteds.size(); i++) {
                assertServiceEquals(expecteds.get(i), actuals.get(i));
            }
        } else {
            assertTrue(up);
            final CollectingServiceListener l = jls.get(listener);
            final List<ServiceInfo> actuals = l.ups();
            /* await for listener to have received expected number of events. */
            await().atMost(timeout, SECONDS).until(() -> actuals.size(), equalTo(expecteds.size()));
            Collections.sort(actuals, (s1, s2) -> s1.getName().compareTo(s2.getName()));
            for (int i = 0; i < expecteds.size(); i++) {
                assertServiceEquals(expecteds.get(i), actuals.get(i));
            }
        }
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
            for (final RegistrationType rt : types) {
                final CollectingServiceListener l = new CollectingServiceListener();
                jls.put(rt.listenerName(), l);
                engines.jmdns().addServiceListener(rt.registrationType() + "local.", l);
            }
        }
        browsedBy = engine;
    }

}
