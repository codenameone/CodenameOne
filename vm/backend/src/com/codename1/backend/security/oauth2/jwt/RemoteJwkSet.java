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
import java.util.concurrent.atomic.AtomicBoolean;

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
///   and only when there is none at all -- the very first tokens after a start
///   -- does each fetch for itself. Nothing waits on a lock: a request's thread
///   parks on its socket and on nothing else.
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
    private final AtomicBoolean fetching = new AtomicBoolean();
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
        long now;
        List<Jwk> have;
        synchronized (this) {
            now = clock.currentTimeMillis();
            have = cached;
            if (have != null && now - fetchedAt < cacheMillis) {
                return have;
            }
            // Out of date, and tried too recently to try again: what there is.
            if (have != null && attempted && now - attemptedAt < refreshIntervalMillis) {
                return have;
            }
        }
        return fetch(have, now);
    }

    /// The keys, fetched again unless they were fetched within the refresh
    /// interval: what a decoder asks for when a token names a key the set does
    /// not have.
    public List<Jwk> refresh() throws IOException {
        long now;
        List<Jwk> have;
        synchronized (this) {
            now = clock.currentTimeMillis();
            have = cached;
            if (attempted && now - attemptedAt < refreshIntervalMillis) {
                if (have != null) {
                    return have;
                }
                throw new IOException("The keys at " + uri + " could not be fetched, and "
                        + "fetching is not tried again so soon");
            }
        }
        return fetch(have, now);
    }

    private List<Jwk> fetch(List<Jwk> have, long now) throws IOException {
        if (!fetching.compareAndSet(false, true)) {
            // Somebody is at it. With a set in hand that is the answer for now;
            // with none there is nothing to wait on, so this request asks too.
            if (have != null) {
                return have;
            }
            return store(load(), now);
        }
        try {
            synchronized (this) {
                attempted = true;
                attemptedAt = now;
            }
            return store(load(), now);
        } catch (IOException err) {
            if (have != null) {
                return have;
            }
            throw err;
        } finally {
            fetching.set(false);
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
