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
package com.codename1.backend.security.ratelimit;

import com.codename1.backend.kv.KeyValueStore;
import com.codename1.backend.kv.KeyValueStores;
import java.io.IOException;

/// Counts in the server's key/value store -- Redis once `cn1.kv.url` names one
/// -- so every process of a deployment shares one limit, at one round trip per
/// request and with no table to clean up.
///
/// ```java
/// http.rateLimit("/login", RateLimitKeys.clientAddress(),
///         new KeyValueRateLimiter("login", 5, 60));
/// ```
///
/// A fixed window, as [JdbcRateLimiter] counts: `permits` requests in each
/// `periodSeconds`, counted from the first request of the window. A request is
/// one atomic increment of a counter that expires with its window, so two
/// processes counting at once cannot both take the last permit. A store that
/// cannot be reached refuses nobody, as the database limiter does not.
public final class KeyValueRateLimiter implements RateLimiter {
    private final KeyValueStore fixed;
    private final String name;
    private final int permits;
    private final long periodMillis;

    /// A limiter in the calling thread's server's store.
    ///
    /// - `name`: what keeps this limiter's counts apart from another's; two
    ///   limiters of one name share their counts on purpose
    public KeyValueRateLimiter(String name, int permits, long periodSeconds) {
        this(null, name, permits, periodSeconds);
    }

    /// A limiter in `store`; null means the calling thread's server's store.
    public KeyValueRateLimiter(KeyValueStore store, String name, int permits,
                               long periodSeconds) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A name is required");
        }
        if (permits < 1 || periodSeconds < 1 || periodSeconds > Long.MAX_VALUE / 1000L) {
            throw new IllegalArgumentException("A rate limit needs at least one permit and a "
                    + "period of at least a second");
        }
        this.fixed = store;
        this.name = name;
        this.permits = permits;
        this.periodMillis = periodSeconds * 1000L;
    }

    private KeyValueStore store() throws IOException {
        return fixed != null ? fixed : KeyValueStores.current();
    }

    private String key(String key) {
        // The same bounded form the database limiter keys its rows by, so a
        // client-chosen key cannot grow without limit.
        return "cn1:rl:" + JdbcRateLimiter.row(name, key);
    }

    @Override
    public RateLimiter derive(String name, int permits, long periodSeconds) {
        return new KeyValueRateLimiter(fixed, name, permits, periodSeconds);
    }

    @Override
    public boolean tryAcquire(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        try {
            return store().increment(key(key), 1, periodMillis) <= permits;
        } catch (IOException err) {
            System.err.println("cn1: the rate limit store could not be reached, so the request "
                    + "is not counted: " + err.getMessage());
            return true;
        }
    }

    @Override
    public void reset(String key) {
        if (key == null) {
            return;
        }
        try {
            store().delete(key(key));
        } catch (IOException err) {
            System.err.println("cn1: the rate limit store could not be reached, so a count "
                    + "was not cleared: " + err.getMessage());
        }
    }

    @Override
    public long retryAfterSeconds(String key) {
        if (key == null) {
            return 1;
        }
        try {
            long left = store().ttl(key(key));
            long seconds = (left + 999) / 1000;
            return seconds < 1 ? 1 : seconds;
        } catch (IOException err) {
            return 1;
        }
    }
}
