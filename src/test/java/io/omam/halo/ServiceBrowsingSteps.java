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

import static io.omam.halo.Assert.assertContainsAllServiceInfos;
import static io.omam.halo.Assert.assertContainsAllServices;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps to tests browsing by registration type.
 */
@SuppressWarnings("javadoc")
public final class ServiceBrowsingSteps {

    private static final class CollectingBrowserListener implements ServiceBrowserListener {

        private final Collection<ResolvedService> added;

        private final Collection<ResolvedService> removed;

        private final Collection<ResolvedService> updated;

        CollectingBrowserListener() {
            added = new ConcurrentLinkedQueue<>();
            removed = new ConcurrentLinkedQueue<>();
            updated = new ConcurrentLinkedQueue<>();
        }

        @Override
        public final void serviceAdded(final ResolvedService service) {
            added.add(service);
        }

        @Override
        public final void serviceRemoved(final ResolvedService service) {
            removed.add(service);
        }

        @Override
        public final void serviceUpdated(final ResolvedService service) {
            updated.add(service);
        }

        final Collection<ResolvedService> added() {
            return added;
        }

        final Collection<ResolvedService> removed() {
            return removed;
        }

        final Collection<ResolvedService> updated() {
            return updated;
        }

    }

    private static final class CollectingServiceListener implements ServiceListener {

        private final Collection<ServiceInfo> added;

        CollectingServiceListener() {
            added = new ConcurrentLinkedQueue<>();
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
            added.add(event.getInfo());
        }

        final Collection<ServiceInfo> added() {
            return added;
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
        hbs.values().forEach(Browser::close);
        hbs.clear();
        browsedBy = null;
    }

    @Given("the browser associated with the listener {string} has been stopped")
    public final void givenBrowserStopped(final String listener) {
        hbs.remove(listener).close();
    }

    @Given("the listener {string} has been notified of the following {string} services:")
    public final void givenListenerNotified(final String listener, final String eventType, final DataTable data) {
        thenListenerNotified(listener, eventType, data);
    }

    @Given("the following registration types are being browsed with {string}:")
    public final void givenRegistrationTypesBrowsed(final String engine, final DataTable data) {
        whenRegistrationTypesBrowsed(engine, data);
    }

    @Then("the listener {string} shall be notified of the following {string} services:")
    public final void thenListenerNotified(final String listener, final String eventType, final DataTable data) {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        /* sort expecteds and actuals by instance name. */
        final List<ServiceDetails> expecteds = new ArrayList<>(services);
        expecteds.sort(Comparator.comparing(ServiceDetails::instanceName));
        /* Halo and JmDNS service resolution timeout is 6 seconds. */
        final Duration timeout = Duration.ofSeconds(services.size() * 6);
        if (browsedBy.equals("Halo")) {
            final CollectingBrowserListener l = hls.get(listener);
            final Collection<ResolvedService> rservices;
            if (eventType.equals("added")) {
                rservices = l.added();
            } else if (eventType.equals("removed")) {
                rservices = l.removed();
            } else {
                rservices = l.updated();
            }
            await().atMost(timeout).untilAsserted(() -> assertContainsAllServices(expecteds, rservices));
        } else {
            assertEquals("added", eventType);
            final CollectingServiceListener l = jls.get(listener);
            await().atMost(timeout).untilAsserted(() -> assertContainsAllServiceInfos(expecteds, l.added()));
        }
    }

    @When("the following registration types are browsed with {string}:")
    public final void whenRegistrationTypesBrowsed(final String engine, final DataTable data) {
        final List<RegistrationType> types = Parser.parse(data, RegistrationType::new);
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
