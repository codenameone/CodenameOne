/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.io.Storage;
import com.codename1.util.AsyncResource;

/// Pluggable persistence for an [OidcClient]'s tokens. Implement this and pass
/// to [OidcClient#setTokenStore(TokenStore)] when you want a custom strategy
/// (e.g. cross-device sync, encrypted-at-rest with your own key, in-memory
/// only). The default is [DefaultStorageTokenStore], which serialises tokens
/// to the standard [Storage] under a per-issuer key. [SecureStorageTokenStore]
/// keeps them in the platform keychain or keystore instead, optionally behind
/// a biometric prompt.
///
/// All methods are asynchronous and may run network or biometric prompts on
/// the calling thread.
///
public interface TokenStore {

    /// Reads previously-saved tokens for `key`, or completes with `null` if
    /// nothing is stored.
    AsyncResource<OidcTokens> load(String key);

    /// Persists `tokens` under `key`. Implementations should overwrite any
    /// existing entry atomically.
    AsyncResource<Boolean> save(String key, OidcTokens tokens);

    /// Removes the entry for `key`. Completing with `Boolean.FALSE` means
    /// nothing was stored; completing with an error means the underlying
    /// store failed.
    AsyncResource<Boolean> clear(String key);

    /// The default store. Serialises the token JSON to [Storage] under a
    /// `"cn1.oidc."`-prefixed key. Convenient and zero-config, but not
    /// encrypted-at-rest -- the underlying storage on Android is the app's
    /// internal files directory, which is sandboxed but not protected against
    /// a rooted device with backups enabled.
    final class DefaultStorageTokenStore implements TokenStore {
        private static final String PREFIX = "cn1.oidc.";

        @Override
        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> r = new AsyncResource<OidcTokens>();
            try {
                // Checked rather than cast: a failed cast does not throw on every platform, so
                // an entry some other code wrote under this name must be told apart here.
                Object stored = Storage.getInstance().readObject(PREFIX + key);
                r.complete(stored instanceof String ? TokenJson.fromJson((String) stored) : null);
            } catch (Throwable t) {
                r.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Failed to load stored tokens", t));
            }
            return r;
        }

        @Override
        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            try {
                Storage.getInstance().writeObject(PREFIX + key, TokenJson.toJson(tokens));
                r.complete(Boolean.TRUE);
            } catch (Throwable t) {
                r.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Failed to save tokens", t));
            }
            return r;
        }

        @Override
        public AsyncResource<Boolean> clear(String key) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            try {
                Storage.getInstance().deleteStorageFile(PREFIX + key);
                r.complete(Boolean.TRUE);
            } catch (Throwable t) {
                r.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Failed to clear tokens", t));
            }
            return r;
        }
    }
}
