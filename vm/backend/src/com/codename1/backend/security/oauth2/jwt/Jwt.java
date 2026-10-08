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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A JSON Web Token whose signature has been made or verified: its text, the
/// headers of its signature and its claims.
///
/// ```java
/// Jwt jwt = decoder.decode(token);
/// String user = jwt.getSubject();
/// List<String> groups = jwt.getClaimAsStringList("groups");
/// ```
///
/// Times are seconds since the epoch, as the token carries them. This is not
/// `com.codename1.backend.Jwt`, the helper for HS256 tokens a server issues to
/// itself.
public final class Jwt {
    private final String tokenValue;
    private final Map<String, Object> headers;
    private final Map<String, Object> claims;

    /// @param tokenValue the token as it travels
    /// @param headers the JOSE header
    /// @param claims the payload
    public Jwt(String tokenValue, Map<String, Object> headers, Map<String, Object> claims) {
        if (tokenValue == null || tokenValue.length() == 0) {
            throw new IllegalArgumentException("tokenValue cannot be empty");
        }
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("headers cannot be empty");
        }
        if (claims == null) {
            throw new IllegalArgumentException("claims cannot be null");
        }
        this.tokenValue = tokenValue;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(headers));
        this.claims = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(claims));
    }

    /// The token as it travels: three base64url parts joined by dots.
    public String getTokenValue() {
        return tokenValue;
    }

    /// The JOSE header: `alg`, and `kid` and `typ` when the token has them.
    public Map<String, Object> getHeaders() {
        return headers;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    /// A claim as the JSON had it: a String, a Long or Double, a Boolean, a
    /// List or a Map; null when the token has none of that name.
    public Object getClaim(String claim) {
        return claims.get(claim);
    }

    public boolean hasClaim(String claim) {
        return claims.containsKey(claim);
    }

    /// A claim as text; null when the token has none of that name.
    public String getClaimAsString(String claim) {
        Object value = claims.get(claim);
        return value == null ? null : value.toString();
    }

    /// A claim that is true or false, or the text of either; null otherwise.
    public Boolean getClaimAsBoolean(String claim) {
        Object value = claims.get(claim);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        return "false".equals(value) ? Boolean.FALSE : null;
    }

    /// A numeric claim as a whole number; null when the token has none of that
    /// name or it is not a number.
    public Long getClaimAsLong(String claim) {
        Object value = claims.get(claim);
        return value instanceof Number ? Long.valueOf(((Number) value).longValue()) : null;
    }

    /// A claim that is a list, or one value standing for a list of one: every
    /// element as text. Null when the token has none of that name.
    public List<String> getClaimAsStringList(String claim) {
        Object value = claims.get(claim);
        if (value == null) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            for (Object element : (List<?>) value) {
                if (element != null) {
                    out.add(element.toString());
                }
            }
        } else {
            out.add(value.toString());
        }
        return out;
    }

    /// `iss`: who issued the token.
    public String getIssuer() {
        return getClaimAsString(JwtClaimNames.ISS);
    }

    /// `sub`: whom the token is about.
    public String getSubject() {
        return getClaimAsString(JwtClaimNames.SUB);
    }

    /// `aud`: whom the token is for. Empty when the token does not say.
    public List<String> getAudience() {
        List<String> audience = getClaimAsStringList(JwtClaimNames.AUD);
        return audience == null ? new ArrayList<String>() : audience;
    }

    /// `exp` in seconds since the epoch, or null.
    public Long getExpiresAt() {
        return getClaimAsLong(JwtClaimNames.EXP);
    }

    /// `nbf` in seconds since the epoch, or null.
    public Long getNotBefore() {
        return getClaimAsLong(JwtClaimNames.NBF);
    }

    /// `iat` in seconds since the epoch, or null.
    public Long getIssuedAt() {
        return getClaimAsLong(JwtClaimNames.IAT);
    }

    /// `jti`: the token's own id.
    public String getId() {
        return getClaimAsString(JwtClaimNames.JTI);
    }

    @Override
    public String toString() {
        return "Jwt [sub=" + getSubject() + ", iss=" + getIssuer() + "]";
    }
}
