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
import static io.omam.halo.Engines.toHalo;
import static io.omam.halo.Engines.toJmdns;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private final List<ServiceInfo> jss;

    private final List<Service> hss;

    private String registeredBy;

    public RegistrationSteps(final Engines someEngines, final Exceptions someExceptions) {
        engines = someEngines;
        exceptions = someExceptions;
        jss = new ArrayList<>();
        hss = new ArrayList<>();
        registeredBy = null;
    }

    @After
    public final void after() {
        jss.clear();
        hss.clear();
        registeredBy = null;
    }

    @Given("^the following services have been registered with \"(Halo|JmDNS)\":$")
    public final void givenServicesRegistered(final String engine, final List<ServiceDetails> service)
            throws IOException {
        whenServicesRegistered(engine, service);
    }

    @When("the service \"([^\"]*)\" is de-registered$")
    public final void thenDeregisterService(final String service) throws IOException {
        assertNotNull(registeredBy);
        if (registeredBy.equals("Halo")) {
            final Service s =
                    hss.stream().filter(hs -> hs.serviceName().equals(service + "local.")).findFirst().orElseThrow(
                            AssertionError::new);
            engines.halo().deregister(s);
            hss.remove(s);
        } else {
            final ServiceInfo s = jss
                .stream()
                .filter(js -> js.getQualifiedName().equals(service + "local."))
                .findFirst()
                .orElseThrow(AssertionError::new);
            engines.jmdns().unregisterService(s);
            jss.remove(s);
        }
    }

    @Then("^the following registered services shall be returned:$")
    public final void thenServicesReturned(final List<ServiceDetails> service) {
        assertNotNull(registeredBy);
        if (registeredBy.equals("Halo")) {
            assertEquals(service.size(), hss.size());
            for (int i = 0; i < hss.size(); i++) {
                assertServiceEquals(service.get(i), hss.get(i));
            }
        } else {
            assertEquals(service.size(), jss.size());
            for (int i = 0; i < jss.size(); i++) {
                assertServiceEquals(service.get(i), jss.get(i));
            }
        }
    }

    @When("^the following service is registered with \"Halo\"( not)? allowing instance name change:$")
    public final void whenServiceRegistered(final String nameChangeNotAllowed,
            final List<ServiceDetails> service) {
        try {
            final boolean allowNameChange = nameChangeNotAllowed == null;
            hss.add(engines.halo().register(toHalo(service.get(0)), allowNameChange));
        } catch (final IOException e) {
            exceptions.thrown(e);
        }
    }

    @When("^the following services are registered with \"(Halo|JmDNS)\":$")
    public final void whenServicesRegistered(final String engine, final List<ServiceDetails> services)
            throws IOException {
        if (engine.equals("Halo")) {
            for (final ServiceDetails service : services) {
                hss.add(engines.halo().register(toHalo(service), false));
            }
        } else {
            for (final ServiceDetails service : services) {
                final ServiceInfo s = toJmdns(service);
                engines.jmdns().registerService(s);
                jss.add(s);
            }
        }
        registeredBy = engine;
    }

}
