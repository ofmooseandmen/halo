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

import static io.omam.zeroconf.MulticastDns.CLASS_ANY;
import static io.omam.zeroconf.MulticastDns.TYPE_ANY;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A cache of DNS records.
 */
final class Cache {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Cache.class.getName());

    /** maps a DNS record key to all cached DNS entries. */
    private final ConcurrentHashMap<String, List<DnsRecord>> map;

    /**
     * Constructor.
     */
    public Cache() {
        map = new ConcurrentHashMap<>();
    }

    /**
     * Adds the given DNS record to this cache.
     *
     * @param record DNS record to add
     */
    final void add(final DnsRecord record) {
        Objects.requireNonNull(record);
        LOGGER.fine(() -> "Adding " + record + " to cache");
        map.computeIfAbsent(key(record), k -> new CopyOnWriteArrayList<>()).add(record);
    }

    /**
     * Clears all DNS records.
     */
    final void clear() {
        LOGGER.fine("Clearing cache");
        map.clear();
    }

    /**
     * @return all DNS records of this cache.
     */
    final Collection<DnsRecord> entries() {
        return map.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Returns all DNS records matching the given service name.
     *
     * @param serviceName service name
     * @return all DNS records matching the given service name
     */
    final Collection<DnsRecord> entries(final String serviceName) {
        return map.getOrDefault(serviceName.toLowerCase(), Collections.emptyList());
    }

    /**
     * Returns the DNS record matching the given DNS record if it exists.
     *
     * @param record DNS record
     * @return an Optional describing the matching DNS record or empty
     */
    final Optional<DnsRecord> get(final DnsRecord record) {
        LOGGER.fine(() -> "Searching cache for DNS record matching " + record);
        final Optional<DnsRecord> result = map
            .get(key(record))
            .stream()
            .filter(r -> r.name().equals(record.name()) && isSameType(r, record) && isSameClass(r, record))
            .findFirst();
        logResult(result);
        return result;
    }

    /**
     * Returns the DNS record matching the given service name, record type and class if it exists.
     *
     * @param serviceName service name
     * @param recordType service type
     * @param recordClass service class
     * @return an Optional describing the matching DNS record or empty
     */
    final Optional<DnsRecord> get(final String serviceName, final short recordType, final short recordClass) {
        LOGGER.fine(() -> "Searching cache for DNS record matching [Name="
            + serviceName
            + "; type="
            + recordType
            + "; class="
            + recordClass
            + "]");
        final Optional<DnsRecord> result = entries(serviceName)
            .stream()
            .filter(r -> r.name().equals(serviceName) && isSameType(r, recordType) && isSameClass(r, recordClass))
            .findFirst();
        logResult(result);
        return result;
    }

    /**
     * Returns the {@link PtrRecord PTR record} matching the given service name and target which is not yet
     * {@link DnsRecord#isExpired(Instant) expired} if it exists.
     *
     * @param serviceName service name
     * @param target pointer target
     * @param now current time
     * @return an Optional describing the matching DNS record or empty
     */
    final Optional<PtrRecord> pointer(final String serviceName, final String target, final Instant now) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(now);
        LOGGER.fine(() -> "Searching cache for DNS pointer matching [Name="
            + serviceName
            + "; target="
            + target
            + "; now="
            + now
            + "]");
        final Optional<PtrRecord> result = entries(serviceName)
            .stream()
            .filter(e -> e instanceof PtrRecord)
            .map(e -> (PtrRecord) e)
            .filter(e -> e.target().equals(target) && !e.isExpired(now))
            .findFirst();
        logResult(result);
        return result;
    }

    /**
     * Removes the given DNS record from this cache.
     *
     * @param record DNS record to remove
     */
    final void remove(final DnsRecord record) {
        Objects.requireNonNull(record);
        LOGGER.fine(() -> "Removing " + record + " from cache");
        map.remove(key(record));
    }

    /**
     * Returns true if both DNS records have the same service class (or one has a service class of
     * {@link #CLASS_ANY}).
     *
     * @param r1 DNS record
     * @param r2 DNS record
     * @return as described above
     */
    private boolean isSameClass(final DnsRecord r1, final DnsRecord r2) {
        return isSameClass(r1, r2.clazz());
    }

    /**
     * Returns true if given DNS record has given service class (or record/given class is {@link #CLASS_ANY}).
     *
     * @param r DNS record
     * @param clazz service class
     * @return as described above
     */
    private boolean isSameClass(final DnsRecord r, final short clazz) {
        return r.clazz() == CLASS_ANY || clazz == CLASS_ANY || r.clazz() == clazz;
    }

    /**
     * Returns true if both DNS records have the same service type (or one has a service type of
     * {@link #TYPE_ANY}).
     *
     * @param r1 DNS record
     * @param r2 DNS record
     * @return as described above
     */
    private boolean isSameType(final DnsRecord r1, final DnsRecord r2) {
        return isSameType(r1, r2.type());
    }

    /**
     * Returns true if given DNS record has given service type (or record/given type is {@link #TYPE_ANY}).
     *
     * @param r DNS record
     * @param type service type
     * @return as described above
     */
    private boolean isSameType(final DnsRecord r, final short type) {
        return r.type() == TYPE_ANY || type == TYPE_ANY || r.type() == type;
    }

    /**
     * Returns the association key for the given record.
     *
     * @param record record
     * @return key
     */
    private String key(final DnsRecord record) {
        return record.name().toLowerCase();
    }

    /**
     * Logs search result.
     *
     * @param result search result
     */
    private void logResult(final Optional<? extends DnsRecord> result) {
        if (result.isPresent()) {
            LOGGER.fine("Found cached " + result.get());
        } else {
            LOGGER.fine("No cached record found");
        }
    }

}
