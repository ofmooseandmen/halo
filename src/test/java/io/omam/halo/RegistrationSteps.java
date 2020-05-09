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
import static io.omam.halo.Engines.toHalo;
import static io.omam.halo.Engines.toJmdns;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps to tests service registration/de-registration.
 */
@SuppressWarnings("javadoc")
public final class RegistrationSteps {

    private final Engines engines;

    private final Exceptions exceptions;

    private final List<ServiceInfo> jss;

    private final List<RegisteredService> hss;

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

    @Given("the attributes of the service {string} have been updated to {string}")
    public final void givenAttributesUpdated(final String service, final String attributeKey) throws IOException {
        whenAttributesUpdate(service, attributeKey);
    }

    @Given("the following services have been registered with {string}:")
    public final void givenServicesRegistered(final String engine, final DataTable data) throws IOException {
        whenServicesRegistered(engine, data);
    }

    @When("the service {string} is de-registered")
    public final void thenDeregisterService(final String service) throws IOException {
        assertNotNull(registeredBy);
        if (registeredBy.equals("Halo")) {
            final RegisteredService s = haloService(service);
            engines.halo().deregister(s);
            hss.remove(s);
        } else {
            final ServiceInfo s = jmdnsService(service);
            engines.jmdns().unregisterService(s);
            jss.remove(s);
        }
    }

    @Then("the following registered services shall be returned:")
    public final void thenServicesReturned(final DataTable data) {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        assertNotNull(registeredBy);
        if (registeredBy.equals("Halo")) {
            assertServicesEquals(services, hss);
        } else {
            assertServiceInfosEquals(services, jss);
        }
    }

    @When("the attributes of the service {string} are updated to {string}")
    public final void whenAttributesUpdate(final String service, final String attributeKey) throws IOException {
        assertNotNull(registeredBy);
        if (registeredBy.equals("Halo")) {
            final RegisteredService s = haloService(service);
            s.changeAttributes(toHalo(attributeKey));
        } else {
            final ServiceInfo s = jmdnsService(service);
            s.setText(toJmdns(attributeKey));
            /*
             * ServiceInfo#setText returns before the service has been re-announced.
             */
            ((ServiceInfoImpl) s).waitForAnnounced(DNSConstants.SERVICE_INFO_TIMEOUT);
        }
    }

    @When("the following service is registered with \"Halo\" not allowing instance name change:")
    public final void whenServiceRegistered(final DataTable data) {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        try {
            for (final ServiceDetails service : services) {
                hss.add(engines.halo().register(toHalo(service), false));
            }
        } catch (final IOException e) {
            exceptions.thrown(e);
        }
    }

    @When("the following service is registered with \"Halo\" allowing instance name change:")
    public final void whenServiceRegisteredAllowingNameChange(final DataTable data) {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        try {
            for (final ServiceDetails service : services) {
                hss.add(engines.halo().register(toHalo(service), true));
            }
        } catch (final IOException e) {
            exceptions.thrown(e);
        }
    }

    @When("the following services are registered with {string}:")
    public final void whenServicesRegistered(final String engine, final DataTable data) throws IOException {
        final List<ServiceDetails> services = Parser.parse(data, ServiceDetails::new);
        if (engine.equals("Halo")) {
            for (final ServiceDetails service : services) {
                hss.add(engines.halo().register(toHalo(service), false));
            }
        } else if (engine.equals("JmDNS")) {
            for (final ServiceDetails service : services) {
                final ServiceInfo serviceInfo = toJmdns(service);
                engines.jmdns().registerService(serviceInfo);
                /*
                 * As of 3.5.5, JmDNS#registerService returns before the service has been announced.
                 */
                ((ServiceInfoImpl) serviceInfo).waitForAnnounced(DNSConstants.SERVICE_INFO_TIMEOUT);
                jss.add(serviceInfo);
            }
        } else {
            throw new AssertionError("Unsupported engine " + engine);
        }
        registeredBy = engine;
    }

    private RegisteredService haloService(final String service) {
        return hss
            .stream()
            .filter(hs -> hs.name().equals(service + "local."))
            .findFirst()
            .orElseThrow(AssertionError::new);
    }

    private ServiceInfo jmdnsService(final String service) {
        return jss
            .stream()
            .filter(js -> js.getQualifiedName().equals(service + "local."))
            .findFirst()
            .orElseThrow(AssertionError::new);
    }

}
