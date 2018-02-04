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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.jmdns.ServiceInfo;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests service registration/de-registration.
 */
@SuppressWarnings("javadoc")
public final class RegistrationSteps {

    private final Engines engines;

    private final Exceptions exceptions;

    private Optional<ServiceInfo> js;

    private Optional<Service> hs;

    public RegistrationSteps(final Engines someEngines, final Exceptions someExceptions) {
        engines = someEngines;
        exceptions = someExceptions;
        js = Optional.empty();
        hs = Optional.empty();
    }

    @After
    public final void after() {
        js = Optional.empty();
        hs = Optional.empty();
    }

    @Given("the service has been de-registered$")
    public final void givenServiceDeregistered() throws IOException {
        if (!(js.isPresent() ^ hs.isPresent())) {
            fail("No unique service previously registered");
        } else if (js.isPresent()) {
            engines.jmdns().unregisterService(js.get());
            js = Optional.empty();
        } else {
            engines.halo().deregister(hs.get());
            hs = Optional.empty();
        }
    }

    @Given("^the following service has been registered with \"(Halo|JmDNS)\":$")
    public final void givenServiceRegistered(final String engine, final List<ServiceDetails> service)
            throws IOException {
        assertEquals(1, service.size());
        if (engine.equals("Halo")) {
            hs = Optional.of(engines.halo().register(engines.toHalo(service.get(0)), false));
        } else {
            final ServiceInfo s = engines.toJmdns(service.get(0));
            engines.jmdns().registerService(s);
            js = Optional.of(s);
        }
    }

    @Then("^the following registered service shall be returned:$")
    public final void thenServiceReturned(final List<ServiceDetails> service) {
        assertEquals(1, service.size());
        final ServiceDetails expected = service.get(0);
        assertTrue(hs.isPresent());
        final Service actual = hs.get();
        assertEquals(expected.instanceName(), actual.instanceName());
        assertEquals(expected.registrationType(), actual.registrationType());
        assertEquals(expected.port(), actual.port());
        assertAttributesEquals(engines.toHalo(expected.text()), actual.attributes());
    }

    @When("^the following service is registered with \"Halo\"( not)? allowing instance name change:$")
    public final void whenServiceRegistered(final String nameChangeNotAllowed,
            final List<ServiceDetails> service) {
        try {
            final boolean allowNameChange = nameChangeNotAllowed == null;
            hs = Optional.of(engines.halo().register(engines.toHalo(service.get(0)), allowNameChange));
        } catch (final IOException e) {
            hs = Optional.empty();
            exceptions.thrown(e);
        }
    }

}
