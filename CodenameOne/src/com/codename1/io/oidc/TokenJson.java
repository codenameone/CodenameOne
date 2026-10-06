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
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// The text form every [TokenStore] in this package keeps a token set in, so that a set saved by
/// one store can be read by another -- an application that moves from [TokenStore.DefaultStorageTokenStore]
/// to [SecureStorageTokenStore] can copy its entry across unchanged.
///
/// The document has three members: `token`, the token endpoint's own response; `claims`, the
/// decoded ID token; and `expiresAt`, the absolute expiry in milliseconds, because the
/// response's `expires_in` is relative to a moment that is gone by the time the entry is read.
final class TokenJson {

    private TokenJson() {
    }

    /// Writes a token set.
    ///
    /// The refresh token is written from [OidcTokens#getRefreshToken()] rather than from the raw
    /// response alone. A token endpoint may leave `refresh_token` out of a refresh response,
    /// meaning "keep using the one you have", and the set then carries the old one in its field
    /// and nothing in its raw response -- so writing only the raw response lost the refresh token
    /// the first time a session was refreshed and saved.
    static String toJson(OidcTokens tokens) {
        Map<String, Object> token = new HashMap<String, Object>(tokens.getRawResponse());
        if (tokens.getRefreshToken() != null) {
            token.put("refresh_token", tokens.getRefreshToken());
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"token\":");
        appendJsonStringMap(sb, token);
        sb.append(",\"claims\":");
        appendJsonStringMap(sb, tokens.getIdTokenClaims());
        if (tokens.getExpiresAt() != null) {
            sb.append(",\"expiresAt\":").append(tokens.getExpiresAt().getTime());
        }
        sb.append("}");
        return sb.toString();
    }

    /// Reads a token set written by [#toJson(OidcTokens)].
    ///
    /// #### Returns
    ///
    /// the tokens, or null when `stored` is null or holds no document
    ///
    /// #### Throws
    ///
    /// - `IOException`: when `stored` is not JSON
    static OidcTokens fromJson(String stored) throws IOException {
        if (stored == null) {
            return null;
        }
        Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader(stored));
        if (parsed == null) {
            return null;
        }
        Map<String, Object> tokenJson = subMap(parsed, "token");
        Map<String, Object> claims = subMap(parsed, "claims");
        Object expiresMs = parsed.get("expiresAt");
        Date expiresAt = null;
        if (expiresMs instanceof Number) {
            // The parser hands a number back as a Double, whose text form is "1.79E12" for a
            // time in milliseconds. Reading that text up to its dot made every stored set
            // expire one millisecond into 1970, so a session loaded from storage always
            // looked expired.
            expiresAt = new Date(((Number) expiresMs).longValue());
        } else if (expiresMs != null) {
            try {
                expiresAt = new Date(Long.parseLong(expiresMs.toString().trim()));
            } catch (NumberFormatException ignored) {
                // Malformed expiry timestamp in persisted storage --
                // treat as "unknown expiry" so the caller can decide
                // whether to refresh; never let a parse failure tank
                // the load.
            }
        }
        return new OidcTokens(
                str(tokenJson.get("access_token")),
                str(tokenJson.get("id_token")),
                str(tokenJson.get("refresh_token")),
                str(tokenJson.get("token_type")),
                str(tokenJson.get("scope")),
                expiresAt,
                claims,
                tokenJson);
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
