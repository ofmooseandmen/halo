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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests DNS record suppression.
 */
@SuppressWarnings("javadoc")
public final class SuppressionSteps {

    private final DnsFactory factory;

    private Boolean suppression;

    public SuppressionSteps(final DnsFactory aFactory) {
        factory = aFactory;
    }

    @After
    public final void after() {
        suppression = null;
    }

    @Then("^the DNS record (shall|shall not) be suppressed$")
    public final void thenDnsRecordSuppressedByOtherRecord(final String suppressed) {
        if (suppressed.endsWith("not")) {
            assertFalse(suppression);
        } else {
            assertTrue(suppression);
        }
    }

    @When("^the record to message suppression check is performed$")
    public void whenRecordToMessageSuppression() {
        suppression = factory.record().suppressedBy(factory.message());
    }

    @When("^the record to record suppression check is performed$")
    public void whenRecordToRecordSuppression() {
        suppression = factory.record().suppressedBy(factory.otherRecord());
    }

}
