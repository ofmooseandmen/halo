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

import java.util.Map;

@SuppressWarnings("javadoc")
final class Record {

    private final String address;

    private final short port;

    private final short priority;

    private final String recordClass;

    private final String recordType;

    private final String server;

    private final String serviceName;

    private final String target;

    private final String text;

    private final String ttl;

    private final short weight;

    Record(final Map<String, String> row) {
        address = row.get("address");
        port = Parser.parseShort(row.get("port"));
        priority = Parser.parseShort(row.get("priority"));
        recordClass = row.get("recordClass");
        recordType = row.get("recordType");
        server = row.get("server");
        serviceName = row.get("serviceName");
        target = row.get("target");
        text = row.get("text");
        ttl = row.get("ttl");
        weight = Parser.parseShort(row.get("weight"));
    }

    final String address() {
        return address;
    }

    final String clazz() {
        return recordClass;
    }

    final String name() {
        return serviceName;
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

    final String target() {
        return target;
    }

    final String text() {
        return text;
    }

    final String ttl() {
        return ttl;
    }

    final String type() {
        return recordType;
    }

    final short weight() {
        return weight;
    }

}
