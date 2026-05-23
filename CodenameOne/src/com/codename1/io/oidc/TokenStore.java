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

import com.codename1.io.JSONParser;
import com.codename1.io.Storage;
import com.codename1.util.AsyncResource;
import com.codename1.util.regex.StringReader;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// Pluggable persistence for an [OidcClient]'s tokens. Implement this and pass
/// to [OidcClient#setTokenStore(TokenStore)] when you want a custom strategy
/// (e.g. cross-device sync, encrypted-at-rest with your own key, in-memory
/// only). The default is [DefaultStorageTokenStore], which serialises tokens
/// to the standard [Storage] under a per-issuer key. For biometric-gated
/// persistence on iOS / Android, use [SecureStorageTokenStore].
///
/// All methods are asynchronous and may run network or biometric prompts on
/// the calling thread.
///
/// @since 8.0
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
                String stored = (String) Storage.getInstance().readObject(PREFIX + key);
                if (stored == null) {
                    r.complete(null);
                    return r;
                }
                Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader(stored));
                if (parsed == null) {
                    r.complete(null);
                    return r;
                }
                Map<String, Object> tokenJson = subMap(parsed, "token");
                Map<String, Object> claims = subMap(parsed, "claims");
                Object expiresMs = parsed.get("expiresAt");
                Date expiresAt = null;
                if (expiresMs != null) {
                    try {
                        String raw = expiresMs.toString();
                        int dot = raw.indexOf('.');
                        if (dot >= 0) {
                            raw = raw.substring(0, dot);
                        }
                        expiresAt = new Date(Long.parseLong(raw));
                    } catch (NumberFormatException ignored) {
                        // Malformed expiry timestamp in persisted storage --
                        // treat as "unknown expiry" so the caller can decide
                        // whether to refresh; never let a parse failure tank
                        // the load.
                    }
                }
                OidcTokens tokens = new OidcTokens(
                        str(tokenJson.get("access_token")),
                        str(tokenJson.get("id_token")),
                        str(tokenJson.get("refresh_token")),
                        str(tokenJson.get("token_type")),
                        str(tokenJson.get("scope")),
                        expiresAt,
                        claims,
                        tokenJson);
                r.complete(tokens);
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
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"token\":");
                appendJsonStringMap(sb, tokens.getRawResponse());
                sb.append(",\"claims\":");
                appendJsonStringMap(sb, tokens.getIdTokenClaims());
                if (tokens.getExpiresAt() != null) {
                    sb.append(",\"expiresAt\":").append(tokens.getExpiresAt().getTime());
                }
                sb.append("}");
                Storage.getInstance().writeObject(PREFIX + key, sb.toString());
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

        @SuppressWarnings("unchecked")
        private static Map<String, Object> subMap(Map<String, Object> root, String key) {
            Object v = root.get(key);
            if (v instanceof Map) {
                return (Map<String, Object>) v;
            }
            return new HashMap<String, Object>();
        }

        private static String str(Object o) {
            return o instanceof String ? (String) o : (o == null ? null : o.toString());
        }

        private static void appendJsonStringMap(StringBuilder sb, Map<String, Object> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Number || v instanceof Boolean) {
                    sb.append(v.toString());
                } else {
                    sb.append('"').append(escape(v.toString())).append('"');
                }
            }
            sb.append('}');
        }

        private static String escape(String s) {
            StringBuilder b = new StringBuilder(s.length() + 8);
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': b.append("\\\""); break;
                    case '\\': b.append("\\\\"); break;
                    case '\n': b.append("\\n"); break;
                    case '\r': b.append("\\r"); break;
                    case '\t': b.append("\\t"); break;
                    case '\b': b.append("\\b"); break;
                    case '\f': b.append("\\f"); break;
                    default:
                        if (c < 0x20) {
                            String hex = Integer.toHexString(c);
                            b.append("\\u");
                            for (int p = hex.length(); p < 4; p++) {
                                b.append('0');
                            }
                            b.append(hex);
                        } else {
                            b.append(c);
                        }
                }
            }
            return b.toString();
        }
    }
}
