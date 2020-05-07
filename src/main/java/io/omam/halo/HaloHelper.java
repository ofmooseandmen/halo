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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Internal helper methods. This is an abstract class in order to reduce the visibility of the methods.
 */
abstract class HaloHelper {

    /**
     * Constructor.
     */
    protected HaloHelper() {
        // empty.
    }

    /**
     * Adds the given listener to receive DNS responses.
     *
     * @param listener listener not null
     */
    abstract void addResponseListener(final ResponseListener listener);

    /**
     * Returns the cached DNS record matching the given name, type and class if it exists.
     *
     * @param name record name
     * @param type record type
     * @param clazz record class
     * @return an Optional describing the matching cached DNS record or empty
     */
    abstract Optional<DnsRecord> cachedRecord(final String name, final short type, final short clazz);

    /**
     * @return the current instant.
     */
    abstract Instant now();

    /**
     * Re-announces the given registered service after its attributes have been changed.
     * 
     * @param service the service
     * @param ttl the TTL
     * @throws IOException in case of I/O error
     */
    abstract void reannounce(final RegisteredService service, final Duration ttl) throws IOException;

    /**
     * Removes the given listener so that it no longer receives DNS responses.
     *
     * @param listener listener not null
     */
    abstract void removeResponseListener(final ResponseListener listener);

    /**
     * Sends the given DNS message.
     *
     * @param msg DNS message
     */
    abstract void sendMessage(final DnsMessage msg);
}
