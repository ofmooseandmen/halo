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
package io.omam.zeroconf;

import static io.omam.zeroconf.MulticastDns.TYPE_SRV;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * DNS service record.
 */
@SuppressWarnings("javadoc")
final class SrvRecord extends DnsRecord {

    private final short port;

    private final short priority;

    private final String server;

    private final short weight;

    SrvRecord(final String aName, final short aClass, final Duration aTtl, final Instant now, final short aPort,
            final short aPriority, final String aServer, final short aWeight) {
        super(aName, TYPE_SRV, aClass, aTtl, now);
        Objects.requireNonNull(aServer);
        port = aPort;
        priority = aPriority;
        server = aServer;
        weight = aWeight;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SrvRecord other = (SrvRecord) obj;
        return port == other.port
            && priority == other.priority
            && server.equals(other.server)
            && weight == other.weight;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + port;
        result = prime * result + priority;
        result = prime * result + server.hashCode();
        result = prime * result + weight;
        return result;
    }

    @Override
    public final String toString() {
        return "SrvRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", server="
            + server
            + ", port="
            + port
            + ", priority="
            + priority
            + ", weight="
            + weight
            + "]";
    }

    @Override
    protected final void write(final MessageOutputStream mos) {
        mos.writeShort(priority);
        mos.writeShort(weight);
        mos.writeShort(port);
        mos.writeName(server);
    }

    final short port() {
        return port;
    }

    final short priority() {
        return priority;
    }

    final String server() {
        return server;
    }

    final short weight() {
        return weight;
    }

}
