/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.net;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Util;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.TokenStore;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The start finds a saved session under the key the session itself saves
/// it with -- what decides between the lock and the sign-in form after a
/// restart or a browser reload.
class StoredSessionTest {
    @BeforeAll
    static void port() {
        Util.setImplementation(new JavaSEPort());
    }

    /// Accepts every save, as the platform store does once the key is open.
    static final class Accepting implements TokenStore {
        @Override
        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> r = new AsyncResource<OidcTokens>();
            r.complete(null);
            return r;
        }

        @Override
        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }

        @Override
        public AsyncResource<Boolean> clear(String key) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }
    }

    @Test
    void aSavedSessionIsFoundAndAClearedOneIsNot() {
        TokenStore marked = new Session.Marked(new Accepting());
        String key = Session.storeKey("http://localhost:18080");
        marked.clear(key);
        assertFalse(Session.hasStoredSession("http://localhost:18080"));
        marked.save(key, null);
        assertTrue(Session.hasStoredSession("http://localhost:18080"), "the start sees the session the sign-in saved");
        marked.clear(key);
        assertFalse(Session.hasStoredSession("http://localhost:18080"), "and not one signed out of");
    }

    @Test
    void aMarkerWithNothingBehindItIsDropped() {
        TokenStore marked = new Session.Marked(new Accepting());
        String key = Session.storeKey("http://localhost:18081");
        marked.save(key, null);
        assertTrue(Session.hasStoredSession("http://localhost:18081"));
        // Unlocked and read: the store holds nothing, so the next start must
        // go straight to the sign-in form rather than to the lock.
        marked.load(key);
        assertFalse(Session.hasStoredSession("http://localhost:18081"), "the stale marker is gone");
    }
}
