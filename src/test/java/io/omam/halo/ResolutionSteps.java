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

import static io.omam.halo.HaloAssert.assertAttributesEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import javax.jmdns.ServiceInfo;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests service resolution.
 */
@SuppressWarnings("javadoc")
public final class ResolutionSteps {

    private final Engines engines;

    private Optional<Service> hs;

    private Optional<ServiceInfo> jms;

    private String resolvedBy;

    public ResolutionSteps(final Engines someEngines) {
        engines = someEngines;
        hs = Optional.empty();
        jms = Optional.empty();
        resolvedBy = null;
    }

    @After
    public final void after() {
        hs = Optional.empty();
        jms = Optional.empty();
        resolvedBy = null;
    }

    @Given("^the service \"([^\"]*)\" has been resolved by \"Halo\"$")
    public final void givenServiceResolved(final String service) {
        final String[] split = split(service);
        assertNotNull(engines.halo().resolve(split[0], split[1]));
    }

    @Then("^no resolved service shall be returned$")
    public final void thenServiceNotResolved() {
        assertNotNull(resolvedBy);
        if (resolvedBy.equals("Halo")) {
            assertFalse(hs.isPresent());
        } else {
            assertFalse(jms.isPresent());
        }
    }

    @Then("^the service \"([^\"]*)\" shall be resolved by \"JmDNS\"$")
    public final void thenServiceResolved(final String service) {
        final String[] split = split(service);
        assertNotNull(engines.jmdns().getServiceInfo(split[1] + "local.", split[0]));
    }

    @Then("^the following resolved service shall be returned:$")
    public final void thenServiceReturned(final List<ServiceDetails> service) {
        assertEquals(1, service.size());
        assertNotNull(resolvedBy);
        final ServiceDetails expected = service.get(0);
        if (resolvedBy.equals("Halo")) {
            assertTrue(hs.isPresent());
            final Service actual = hs.get();
            assertEquals(expected.instanceName(), actual.instanceName());
            assertEquals(expected.registrationType(), actual.registrationType());
            assertEquals(expected.port(), actual.port());
            assertAttributesEquals(engines.toHalo(expected.text()), actual.attributes());
        } else {
            assertTrue(jms.isPresent());
            final ServiceInfo actual = jms.get();
            assertEquals(expected.instanceName(), actual.getName());
            assertEquals(expected.registrationType() + "local.", actual.getType());
            assertEquals(expected.port(), actual.getPort());
            assertEquals(engines.toJmdns(expected.text()), engines.attributes(actual));
        }
    }

    @When("^the service \"([^\"]*)\" is resolved by \"(Halo|JmDNS)\"$")
    public final void whenServiceResolved(final String service, final String engine) {
        final int firstDot = service.indexOf('.');
        final String instanceName = service.substring(0, firstDot);
        final String registrationType = service.substring(firstDot + 1, service.length());
        if (engine.equals("Halo")) {
            hs = engines.halo().resolve(instanceName, registrationType);
        } else {
            jms = Optional.ofNullable(engines.jmdns().getServiceInfo(registrationType + "local.", instanceName));
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
