/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.kv;

import java.io.Closeable;
import java.io.IOException;

/// A key/value store that several server instances can share: the place a
/// cache, the sessions, rate-limit counters, scheduler locks and pub/sub
/// notifications live once an application runs more than one process.
///
/// Two implementations ship, chosen by configuration and never by code:
///
/// ```
///   cn1.kv.url=memory:                       # the default: this process only
///   cn1.kv.url=redis://user:pw@host:6379/0   # Redis, Valkey or KeyDB
///   cn1.kv.url=rediss://host:6380            # the same over TLS
/// ```
///
/// Spring Boot's `spring.data.redis.url` (and `.host`, `.port`, `.password`,
/// `.database`, `.ssl.enabled`) is read when `cn1.kv.url` is not set, so a
/// configuration written for a Spring application needs no change.
/// [KeyValueStores#current()] answers the calling thread's server's store.
///
/// Keys are strings and values are bytes; what a value means is the caller's.
/// Every method may block on the network, and parks only its own virtual
/// thread when it does.
public interface KeyValueStore extends Closeable {
    /// The value under `key`, or null when there is none or it expired.
    byte[] get(String key) throws IOException;

    /// Stores `value` under `key`, replacing what was there.
    ///
    /// - `ttlMillis`: how long it lives; zero or less keeps it until it is
    ///   deleted
    void set(String key, byte[] value, long ttlMillis) throws IOException;

    /// Stores `value` only if `key` holds nothing; answers whether it did.
    /// This is the primitive a lock is built on.
    boolean setIfAbsent(String key, byte[] value, long ttlMillis) throws IOException;

    /// Replaces the value under `key` only while it still equals `expected`
    /// (null: while there is none); answers whether it did. A null `value`
    /// deletes the key instead. What an optimistic read-modify-write loops on.
    boolean compareAndSet(String key, byte[] expected, byte[] value, long ttlMillis)
            throws IOException;

    /// Removes `key`; answers whether it existed.
    boolean delete(String key) throws IOException;

    /// Removes every key that starts with `prefix`; answers how many. Walks the
    /// key space incrementally on a shared server rather than blocking it, so
    /// keys written while it runs may survive.
    int deleteByPrefix(String prefix) throws IOException;

    /// Adds `delta` to the integer under `key` and answers the result; a missing
    /// key counts from zero and is given `ttlMillis` (zero or less: none). The
    /// expiry of an existing key is left as it is, so a fixed window counts
    /// from its first request.
    long increment(String key, long delta, long ttlMillis) throws IOException;

    /// How many milliseconds `key` has left: -1 when it never expires, -2 when
    /// it does not exist.
    long ttl(String key) throws IOException;

    /// Sends `message` to everybody subscribed to `channel`, on every instance;
    /// answers how many subscribers received it (on Redis, how many
    /// connections). Delivery is at most once: a subscriber that is not
    /// connected at that moment never sees it.
    long publish(String channel, byte[] message) throws IOException;

    /// Calls `listener` for every message published to `channel` until the
    /// returned subscription is closed. The listener runs on the store's own
    /// receiving thread, one message at a time; it should hand slow work off.
    Subscription subscribe(String channel, MessageListener listener) throws IOException;

    /// Whether the store answers right now; what a health check asks.
    boolean ping();

    /// Whether this store is shared with other processes. A local store's
    /// locks and counters are only good within one process.
    boolean isShared();

    /// Receives published messages.
    interface MessageListener {
        /// One message, in the order the channel delivered them.
        void onMessage(String channel, byte[] message);
    }

    /// An active subscription; closing it stops the deliveries.
    interface Subscription extends Closeable {
        /// Stops the deliveries. Never throws.
        @Override
        void close();
    }
}
