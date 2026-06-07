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

import com.codename1.io.AccessToken;
import com.codename1.io.JSONParser;
import com.codename1.util.Base64;
import com.codename1.util.regex.StringReader;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// The tokens returned by an OpenID Connect token endpoint, with convenience
/// accessors for the OIDC ID token claims. Immutable.
///
/// To bridge into the older [AccessToken] API used by [com.codename1.social.Login],
/// call [#toAccessToken()].
///
/// @since 7.0.245
public final class OidcTokens {

    private final String accessToken;
    private final String idToken;
    private final String refreshToken;
    private final String tokenType;
    private final String scope;
    private final Date expiresAt;
    private final Map<String, Object> idTokenClaims;
    private final Map<String, Object> raw;

    OidcTokens(String accessToken,
               String idToken,
               String refreshToken,
               String tokenType,
               String scope,
               Date expiresAt,
               Map<String, Object> idTokenClaims,
               Map<String, Object> raw) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.idTokenClaims = idTokenClaims == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(idTokenClaims));
        this.raw = raw == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(raw));
    }

    /// Builds an [OidcTokens] from a parsed JSON token-endpoint response,
    /// optionally merging in a refresh token from a previous response (token
    /// endpoints are allowed to omit `refresh_token` on a refresh call).
    public static OidcTokens fromTokenResponse(Map<String, Object> json,
                                               String refreshTokenFallback) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        String accessToken = stringOrNull(json.get("access_token"));
        String idToken = stringOrNull(json.get("id_token"));
        String refreshToken = stringOrNull(json.get("refresh_token"));
        if (refreshToken == null) {
            refreshToken = refreshTokenFallback;
        }
        String tokenType = stringOrNull(json.get("token_type"));
        String scope = stringOrNull(json.get("scope"));
        Date expiresAt = null;
        Object expiresIn = json.get("expires_in");
        if (expiresIn != null) {
            try {
                String raw = expiresIn.toString().trim();
                int dot = raw.indexOf('.');
                if (dot >= 0) {
                    raw = raw.substring(0, dot);
                }
                long seconds = Long.parseLong(raw);
                expiresAt = new Date(System.currentTimeMillis() + seconds * 1000L);
            } catch (NumberFormatException ignored) {
                // Provider returned a non-numeric `expires_in`; treat the
                // expiry as unknown rather than failing the whole token
                // response. `expiresAt` stays null and callers fall back to
                // a 401 retry.
            }
        }
        Map<String, Object> claims = idToken != null ? decodeIdTokenClaims(idToken) : null;
        return new OidcTokens(accessToken, idToken, refreshToken, tokenType, scope,
                expiresAt, claims, json);
    }

    /// Decodes the payload of a compact JWS without verifying the signature.
    /// Suitable for reading OIDC ID-token claims; do NOT use the returned
    /// values for authorization decisions on the server.
    public static Map<String, Object> decodeIdTokenClaims(String compactJwt) {
        if (compactJwt == null) {
            return Collections.emptyMap();
        }
        int firstDot = compactJwt.indexOf('.');
        int secondDot = firstDot >= 0 ? compactJwt.indexOf('.', firstDot + 1) : -1;
        if (firstDot < 0 || secondDot < 0) {
            return Collections.emptyMap();
        }
        String payloadB64 = compactJwt.substring(firstDot + 1, secondDot);
        // Pad to a multiple of 4 for the decoder. Append via StringBuilder
        // rather than `+= "="` so SpotBugs SBSC_USE_STRINGBUFFER_CONCATENATION
        // stays quiet (and we avoid up to 3 String allocations on the hot path).
        int pad = (4 - (payloadB64.length() & 0x3)) & 0x3;
        if (pad != 0) {
            StringBuilder padded = new StringBuilder(payloadB64.length() + pad)
                    .append(payloadB64);
            for (int i = 0; i < pad; i++) {
                padded.append('=');
            }
            payloadB64 = padded.toString();
        }
        byte[] payload;
        try {
            payload = Base64.decodeUrlSafe(payloadB64);
        } catch (RuntimeException re) {
            return Collections.emptyMap();
        }
        if (payload == null) {
            return Collections.emptyMap();
        }
        try {
            String json = new String(payload, "UTF-8");
            Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader(json));
            return parsed != null ? parsed : Collections.<String, Object>emptyMap();
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 always available -- defensive only.
            return Collections.emptyMap();
        } catch (java.io.IOException e) {
            // JSONParser surfaces IOException for malformed payloads.
            return Collections.emptyMap();
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    /// Absolute expiry instant, or `null` if the token endpoint did not
    /// return `expires_in`.
    public Date getExpiresAt() {
        return expiresAt;
    }

    /// `true` if [#getExpiresAt()] is non-null and in the past.
    public boolean isExpired() {
        return expiresAt != null && expiresAt.getTime() < System.currentTimeMillis();
    }

    /// `true` if [#getExpiresAt()] is non-null and within `leewaySeconds` of
    /// the current time. Pass a small leeway (60 -- 120 seconds) when deciding
    /// whether to refresh proactively.
    public boolean isExpiringWithin(int leewaySeconds) {
        return expiresAt != null &&
                expiresAt.getTime() - System.currentTimeMillis() < leewaySeconds * 1000L;
    }

    /// Read-only view of the ID token claims (empty if no ID token was returned).
    public Map<String, Object> getIdTokenClaims() {
        return idTokenClaims;
    }

    /// Convenience accessor for a single ID-token claim. Returns `null` when
    /// the claim is absent or the ID token is missing.
    public Object getClaim(String name) {
        return idTokenClaims.get(name);
    }

    /// Convenience accessor for a string-valued claim.
    public String getStringClaim(String name) {
        Object v = idTokenClaims.get(name);
        return v == null ? null : v.toString();
    }

    /// The full, unmodified token-endpoint JSON. Useful for inspecting
    /// provider-specific fields (e.g. `nonce_supported` from Apple).
    public Map<String, Object> getRawResponse() {
        return raw;
    }

    /// `sub` claim from the ID token -- the stable, opaque user identifier
    /// within the issuer.
    public String getSubject() {
        return getStringClaim("sub");
    }

    /// `email` claim from the ID token, when present.
    public String getEmail() {
        return getStringClaim("email");
    }

    /// `name` claim from the ID token, when present.
    public String getName() {
        return getStringClaim("name");
    }

    /// Bridges into the legacy [AccessToken] API used by
    /// [com.codename1.social.Login]. The expiry is the absolute instant from
    /// [#getExpiresAt()].
    public AccessToken toAccessToken() {
        AccessToken t = new AccessToken(accessToken, null, refreshToken, idToken);
        if (expiresAt != null) {
            t.setExpiryDate(expiresAt);
        }
        return t;
    }

    private static String stringOrNull(Object o) {
        return o instanceof String ? (String) o : null;
    }
}
