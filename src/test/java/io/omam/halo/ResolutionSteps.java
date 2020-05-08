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

import static io.omam.halo.Assert.assertServiceInfosEquals;
import static io.omam.halo.Assert.assertServicesEquals;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.jmdns.ServiceInfo;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps to tests service resolution.
 */
@SuppressWarnings("javadoc")
public final class ResolutionSteps {

    private final Engines engines;

    private final List<Service> hss;

    private final List<ServiceInfo> jss;

    private String resolvedBy;

    public ResolutionSteps(final Engines someEngines) {
        engines = someEngines;
        hss = new ArrayList<>();
        jss = new ArrayList<>();
        resolvedBy = null;
    }

    @After
    public final void after() {
        hss.clear();
        jss.clear();
        resolvedBy = null;
    }

    @Given("the service {string} has been resolved by \"Halo\"")
    public final void givenServiceResolved(final String service) {
        final String[] split = split(service);
        assertNotNull(engines.halo().resolve(split[0], split[1]));
    }

    @Then("after at least {string}, the service {string} cannot resolved by {string}")
    public final void thenServiceEventuallyNotResolved(final String dur, final String service,
            final String engine) {
        final String[] split = split(service);
        final Callable<Boolean> c;
        if (engine.equals("Halo")) {
            c = () -> !engines.halo().resolve(split[0], split[1]).isPresent();
        } else if (engine.equals("JmDNS")) {
            c = () -> engines.jmdns().getServiceInfo(split[1] + "local.", split[0]) == null;
        } else {
            throw new AssertionError("Unsupported engine " + engine);
        }
        await().atLeast(Duration.parse(dur)).until(c);
    }

    @Then("no resolved service shall be returned")
    public final void thenServiceNotResolved() {
        assertNotNull(resolvedBy);
        if (resolvedBy.equals("Halo")) {
            assertTrue(hss.isEmpty());
        } else {
            assertTrue(jss.isEmpty());
        }
    }

    @Then("the service {string} shall be resolved by \"JmDNS\"")
    public final void thenServiceResolved(final String service) {
        final String[] split = split(service);
        assertNotNull(engines.jmdns().getServiceInfo(split[1] + "local.", split[0]));
    }

    @Then("the following resolved services shall be returned:")
    public final void thenServiceReturned(final DataTable data) {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        assertNotNull(resolvedBy);
        if (resolvedBy.equals("Halo")) {
            assertServicesEquals(services, hss);
        } else {
            assertServiceInfosEquals(services, jss);
        }
    }

    @When("the service {string} is resolved by {string}")
    public final void whenServiceResolved(final String service, final String engine) {
        final String[] split = split(service);
        final String instanceName = split[0];
        final String registrationType = split[1];
        if (engine.equals("Halo")) {
            engines.halo().resolve(instanceName, registrationType).ifPresent(hss::add);
        } else if (engine.equals("JmDNS")) {
            Optional
                .ofNullable(engines.jmdns().getServiceInfo(registrationType + "local.", instanceName))
                .ifPresent(jss::add);
        } else {
            throw new AssertionError("Unsupported engine " + engine);
        }
        resolvedBy = engine;
    }

    private String[] split(final String service) {
        final int firstDot = service.indexOf('.');
        final String instanceName = service.substring(0, firstDot);
        final String registrationType = service.substring(firstDot + 1, service.length());
        return new String[] { instanceName, registrationType };
    }

}
