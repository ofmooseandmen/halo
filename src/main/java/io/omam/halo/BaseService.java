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

import static io.omam.halo.MulticastDnsSd.DOMAIN;

/**
 * Base class for all service implementation.
 */
abstract class BaseService implements Service {

    /** service instance name. */
    private final String instanceName;

    /** service registration type. */
    private final String registrationType;

    /**
     * Constructor.
     *
     * @param anInstanceName the service instance name, a human-readable string, e.g. {@code Living Room Printer}
     * @param aRegistrationType service type (IANA) and transport protocol (udp or tcp), e.g. {@code _ftp._tcp.} or
     *            {@code _http._udp.}
     */
    protected BaseService(final String anInstanceName, final String aRegistrationType) {
        instanceName = anInstanceName;
        registrationType = aRegistrationType;
    }

    @Override
    public final String instanceName() {
        return instanceName;
    }

    @Override
    public final String name() {
        return instanceName + "." + registrationPointerName();
    }

    @Override
    public final String registrationPointerName() {
        return registrationType + DOMAIN + ".";
    }

    @Override
    public final String registrationType() {
        return registrationType;
    }

    @Override
    public final String toString() {
        return "Service [instance name=" + instanceName + "; registration type=" + registrationType + "]";
    }

}
