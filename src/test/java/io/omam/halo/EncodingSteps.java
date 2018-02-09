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

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to tests DNS message encoding.
 */
@SuppressWarnings("javadoc")
public final class EncodingSteps {

    private byte[] packet;

    private final DnsFactory factory;

    public EncodingSteps(final DnsFactory aFactory) {
        factory = aFactory;
    }

    @After
    public final void after() {
        packet = null;
    }

    @Then("^the packet shall contain the following bytes:$")
    public final void thenPacketBytes(final DataTable data) {
        final byte[] bytes = Bytes.parse(data);
        assertArrayEquals(bytes, packet);
    }

    @When("^the attributes are encoded$")
    public final void whenAttributesEncoded() throws IOException {
        try (final MessageOutputStream os = new MessageOutputStream()) {
            AttributesCodec.encode(factory.attributes(), os);
            packet = os.toByteArray();
        }
    }

    @When("^the DNS message is encoded$")
    public final void whenDnsMessageEncoded() {
        packet = factory.message().encode();
    }

}
