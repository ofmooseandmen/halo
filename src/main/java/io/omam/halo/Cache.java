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

import static io.omam.halo.HaloProperties.EXPIRY_TTL;
import static io.omam.halo.MulticastDnsSd.CLASS_ANY;
import static io.omam.halo.MulticastDnsSd.TYPE_ANY;
import static io.omam.halo.MulticastDnsSd.toLowerCase;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * A cache of DNS records.
 */
final class Cache {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Cache.class.getName());

    /** maps a DNS record key to all cached DNS entries. */
    private final Map<String, Collection<DnsRecord>> map;

    /**
     * Constructor.
     */
    Cache() {
        map = new ConcurrentHashMap<>();
    }

    /**
     * Returns true if both DNS records have the same service class (or one has a service class of
     * {@link #CLASS_ANY}).
     *
     * @param record1 DNS record
     * @param record2 DNS record
     * @return as described above
     */
    private static boolean isSameClass(final DnsRecord record1, final DnsRecord record2) {
        return isSameClass(record1, record2.clazz());
    }

    /**
     * Returns true if given DNS record has given service class (or record/given class is {@link #CLASS_ANY}).
     *
     * @param record DNS record
     * @param clazz service class
     * @return as described above
     */
    private static boolean isSameClass(final DnsRecord record, final short clazz) {
        return record.clazz() == CLASS_ANY || clazz == CLASS_ANY || record.clazz() == clazz;
    }

    /**
     * Returns true if both DNS records have the same service type (or one has a service type of
     * {@link #TYPE_ANY}).
     *
     * @param record1 DNS record
     * @param record2 DNS record
     * @return as described above
     */
    private static boolean isSameType(final DnsRecord record1, final DnsRecord record2) {
        return isSameType(record1, record2.type());
    }

    /**
     * Returns true if given DNS record has given service type (or record/given type is {@link #TYPE_ANY}).
     *
     * @param record DNS record
     * @param type service type
     * @return as described above
     */
    private static boolean isSameType(final DnsRecord record, final short type) {
        return record.type() == TYPE_ANY || type == TYPE_ANY || record.type() == type;
    }

    /**
     * Returns the association key for the given record.
     *
     * @param record record
     * @return key
     */
    private static String key(final DnsRecord record) {
        return toLowerCase(record.name());
    }

    /**
     * Logs search result.
     *
     * @param result search result
     */
    private static void logResult(final Optional<? extends DnsRecord> result) {
        if (result.isPresent()) {
            LOGGER.fine(() -> "Found cached " + result.get());
        } else {
            LOGGER.fine(() -> "No cached record found");
        }
    }

    /**
     * Determines whether both given DNS records have the same type and class.
     * 
     * @param record1 first DNS record
     * @param record2 second DNS record
     * @return {@code true} if both DNS records have the same type and class, {@code false} otherwise
     */
    private static boolean matches(final DnsRecord record1, final DnsRecord record2) {
        return isSameType(record1, record2) && isSameClass(record1, record2);
    }

    /**
     * Adds the given DNS record to this cache.
     * <p>
     * If a DNS record matching the given record name (ignoring case), type and class already exists, it is
     * replaced with the given one.
     *
     * @param record DNS record to add
     */
    final void add(final DnsRecord record) {
        Objects.requireNonNull(record);
        final boolean existed = entries(record.name()).removeIf(other -> matches(other, record));
        final String key = key(record);
        if (existed) {
            LOGGER.fine(() -> "Adding " + record + " to cache");
        } else {
            LOGGER.fine(() -> "Replacing cached record " + key + " with " + record);
        }
        map.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(record);
    }

    /**
     * Removes all expired DNS records.
     *
     * @param now current instant
     */
    final void clean(final Instant now) {
        final Set<String> services = new HashSet<>();
        for (final Entry<String, Collection<DnsRecord>> e : map.entrySet()) {
            e.getValue().removeIf(r -> r.isExpired(now));
            if (e.getValue().isEmpty()) {
                services.add(e.getKey());
            }
        }
        services.forEach(map::remove);
    }

    /**
     * Clears all DNS records.
     */
    final void clear() {
        LOGGER.fine("Clearing cache");
        map.clear();
    }

    /**
     * Returns all DNS records matching the given name.
     *
     * @param name record name
     * @return all DNS records matching the given name
     */
    final Collection<DnsRecord> entries(final String name) {
        return map.getOrDefault(toLowerCase(name), Collections.emptyList());
    }

    /**
     * Sets the TTL of the given cached record to {@link HaloProperties#EXPIRY_TTL} in order for the reaper to
     * remove it later.
     *
     * @param record DNS record to remove
     */
    final void expire(final DnsRecord record) {
        Objects.requireNonNull(record);
        entries(record.name()).stream().filter(other -> matches(record, other)).forEach(r -> {
            LOGGER.fine(() -> "Setting TTL of " + record + " to " + EXPIRY_TTL);
            r.setTtl(EXPIRY_TTL);
        });
    }

    /**
     * Returns the DNS record matching the given name, type and class if it exists.
     *
     * @param name record name
     * @param type record type
     * @param clazz record class
     * @return an Optional describing the matching DNS record or empty
     */
    final Optional<DnsRecord> get(final String name, final short type, final short clazz) {
        LOGGER
            .fine(() -> "Searching cache for DNS record matching [Name="
                + name
                + "; type="
                + type
                + "; class="
                + clazz
                + "]");
        final Optional<DnsRecord> result =
                entries(name).stream().filter(r -> isSameType(r, type) && isSameClass(r, clazz)).findFirst();
        logResult(result);
        return result;
    }

    /**
     * Removes all DNS records associated with the given name.
     *
     * @param name service name
     */
    final void removeAll(final String name) {
        Objects.requireNonNull(name);
        LOGGER.fine(() -> "Removing all DNS records associated with" + name + " from cache");
        map.remove(toLowerCase(name));
    }

}
