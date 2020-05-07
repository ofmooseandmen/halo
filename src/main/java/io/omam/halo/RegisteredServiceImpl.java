/*
Copyright 2020-2020 Cedric Liegeois

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

import static io.omam.halo.HaloProperties.TTL;

import java.io.IOException;

/**
 * RegisteredService implementation.
 */
final class RegisteredServiceImpl extends BaseRegistrableService implements RegisteredService {

    /** service attributes. */
    private Attributes attributes;

    private final HaloHelper halo;

    /**
     * Constructor.
     *
     * @param original registerable service
     * @param haloHelper halo helper
     */
    RegisteredServiceImpl(final RegisterableService original, final HaloHelper haloHelper) {
        super(original.instanceName(), original.registrationType(), original.hostname(), original.ipv4Address(),
              original.ipv6Address(), original.port());
        attributes = original.attributes();
        halo = haloHelper;
    }

    @Override
    public final Attributes attributes() {
        return attributes;
    }

    @Override
    public final void changeAttributes(final Attributes newAttributes) throws IOException {
        attributes = newAttributes;
        halo.reannounce(this, TTL);

    }

}
