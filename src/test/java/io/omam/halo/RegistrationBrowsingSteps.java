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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceTypeListener;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps to tests browsing by registration type.
 */
@SuppressWarnings("javadoc")
public final class RegistrationBrowsingSteps {

    private static final class CollectingBrowserListener implements RegistrationTypeBrowserListener {

        private final Collection<String> types;

        CollectingBrowserListener() {
            types = new ConcurrentLinkedQueue<>();
        }

        @Override
        public final void registrationTypeDiscovered(final String registrationType) {
            types.add(registrationType);
        }

        final Collection<String> types() {
            return types;
        }

    }

    private static final class CollectingTypeListener implements ServiceTypeListener {

        private final Collection<String> types;

        CollectingTypeListener() {
            types = new ConcurrentLinkedQueue<>();
        }

        @Override
        public final void serviceTypeAdded(final ServiceEvent event) {
            final String type = event.getType();
            types.add(type.substring(0, type.indexOf("local")));
        }

        @Override
        public final void subTypeForServiceTypeAdded(final ServiceEvent event) {
            // ignore: not tested.

        }

        final Collection<String> types() {
            return types;
        }

    }

    private final Engines engines;

    private Optional<CollectingBrowserListener> hl;

    private Optional<Browser> hb;

    private Optional<CollectingTypeListener> jl;

    private String browsedBy;

    public RegistrationBrowsingSteps(final Engines someEngines) {
        engines = someEngines;
        hl = Optional.empty();
        hb = Optional.empty();
        jl = Optional.empty();
        browsedBy = null;
    }

    @After
    public final void after() {
        hl = Optional.empty();
        jl = Optional.empty();
        hb.ifPresent(Browser::close);
        hb = Optional.empty();
        browsedBy = null;
    }

    @Then("the listener shall be notified of the following registration types:")
    public final void thenListenerNotified(final DataTable data) {
        final List<String> expecteds = data.asList();
        final Collection<String> actuals;
        if (browsedBy.equals("Halo")) {
            final CollectingBrowserListener l = hl.orElseThrow(AssertionError::new);
            actuals = l.types();
        } else {
            final CollectingTypeListener l = jl.orElseThrow(AssertionError::new);
            actuals = l.types();
        }
        /*
         * Other events may be fired if other DNS services are running on the machine though, just assert the
         * expected ones.
         *
         */

        final Duration timeout = Duration.ofSeconds(5);
        for (final String expected : expecteds) {
            await()
                .atMost(timeout)
                .untilAsserted(() -> assertTrue(actuals.contains(expected),
                        "Expected to contain [" + expected + "] but was " + actuals));
        }
    }

    @When("the registration types are browsed with {string}")
    public final void whenTypesBrowsed(final String engine) throws IOException {
        if (engine.equals("Halo")) {
            final CollectingBrowserListener l = new CollectingBrowserListener();
            hl = Optional.of(l);
            final Browser b = engines.halo().browse(l);
            hb = Optional.of(b);
        } else if (engine.equals("JmDNS")) {
            final CollectingTypeListener l = new CollectingTypeListener();
            jl = Optional.of(l);
            engines.jmdns().addServiceTypeListener(l);
        } else {
            throw new AssertionError("Unsupported engine " + engine);
        }
        browsedBy = engine;
    }
}
