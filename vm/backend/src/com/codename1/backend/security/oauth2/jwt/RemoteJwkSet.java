/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.Web;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import java.io.IOException;
import java.util.List;

/// The keys another server publishes at its `jwks_uri`, fetched when they are
/// first needed and kept.
///
/// - The set is kept for five minutes, then fetched again by the next request
///   that needs it.
/// - A token naming a key the set does not have makes it fetch early -- that is
///   how a rotated key is picked up -- but no more than once in thirty seconds,
///   so tokens with invented key ids cannot turn this server into a stream of
///   requests at the issuer.
/// - One request fetches at a time. The others carry on with the set they have,
///   or fail if the first fetch has not supplied any keys yet. Failed initial
///   fetches observe the same retry interval. No request waits for another
///   request's network operation.
/// - When a fetch fails and there is a set, the set goes on being used and the
///   fetch is tried again later. An issuer that is briefly unreachable does not
///   sign everybody out.
///
/// Use an `https` address. The keys are what every token is trusted by, and
/// they are only as trustworthy as the connection they came over.
public final class RemoteJwkSet implements JwkSource {
    /// Fetches the text at an address. The default goes through the runtime's
    /// HTTP client.
    public interface Fetcher {
        /// The body of a successful response.
        ///
        /// @throws IOException for anything else
        String fetch(String uri) throws IOException;
    }

    /// The runtime's HTTP client, asking for JSON.
    public static final Fetcher WEB = new Fetcher() {
        @Override
        public String fetch(String uri) throws IOException {
            Web.Result result = Web.getJson(uri, null);
            if (!result.isSuccess()) {
                throw new IOException("GET " + uri + " answered " + result.getStatus()
                        + (result.getError() == null ? "" : ": " + result.getError()));
            }
            return result.getBodyAsString();
        }
    };

    private final String uri;
    private final Fetcher fetcher;
    private boolean fetching;
    private long cacheMillis = 5 * 60 * 1000L;
    private long refreshIntervalMillis = 30 * 1000L;
    private Clock clock = Clock.SYSTEM;
    private List<Jwk> cached;
    private long fetchedAt;
    private long attemptedAt;
    private boolean attempted;

    public RemoteJwkSet(String uri) {
        this(uri, WEB);
    }

    public RemoteJwkSet(String uri, Fetcher fetcher) {
        if (uri == null || uri.length() == 0 || fetcher == null) {
            throw new IllegalArgumentException("An address and a fetcher are required");
        }
        this.uri = uri;
        this.fetcher = fetcher;
    }

    /// How long a fetched set is used before it is fetched again; five minutes
    /// unless set.
    public synchronized void setCacheSeconds(long seconds) {
        this.cacheMillis = seconds * 1000L;
    }

    /// The least time between two fetches, whatever asks for them; thirty
    /// seconds unless set.
    public synchronized void setRefreshIntervalSeconds(long seconds) {
        this.refreshIntervalMillis = seconds * 1000L;
    }

    public synchronized void setClock(Clock clock) {
        this.clock = clock;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public List<Jwk> getKeys() throws IOException {
        return keys(false);
    }

    /// The keys, fetched again unless a fetch is in progress or the refresh
    /// interval has not elapsed.
    public List<Jwk> refresh() throws IOException {
        return keys(true);
    }

    private List<Jwk> keys(boolean refresh) throws IOException {
        long now;
        List<Jwk> have;
        synchronized (this) {
            now = clock.currentTimeMillis();
            have = cached;
            if (!refresh && have != null && now - fetchedAt < cacheMillis) {
                return have;
            }
            // Claim the attempt in the same critical section as the interval
            // check. Even the first fetch and its failures are single-flight.
            if (fetching || (attempted && now - attemptedAt < refreshIntervalMillis)) {
                if (have != null) {
                    return have;
                }
                throw new IOException("The keys at " + uri + " are unavailable; "
                        + "a fetch is in progress or the retry interval has not elapsed");
            }
            fetching = true;
            attempted = true;
            attemptedAt = now;
        }
        try {
            return store(load(), now);
        } catch (IOException err) {
            if (have != null) {
                return have;
            }
            throw err;
        } finally {
            synchronized (this) {
                fetching = false;
            }
        }
    }

    private List<Jwk> load() throws IOException {
        String body;
        try {
            body = fetcher.fetch(uri);
        } catch (RuntimeException err) {
            throw new IOException("Could not fetch the keys at " + uri + ": " + err.getMessage(),
                    err);
        }
        return JwkSet.parse(body).getKeys();
    }

    private synchronized List<Jwk> store(List<Jwk> keys, long now) {
        cached = keys;
        fetchedAt = now;
        attempted = true;
        attemptedAt = now;
        return keys;
    }
}
