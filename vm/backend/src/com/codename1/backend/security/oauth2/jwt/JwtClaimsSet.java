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

/// The claims of a token that is about to be signed.
///
/// ```java
/// long now = System.currentTimeMillis() / 1000;
/// JwtClaimsSet claims = JwtClaimsSet.builder()
///         .issuer("https://orders.example.com")
///         .subject(user.getId())
///         .audience("orders-api")
///         .issuedAt(now)
///         .expiresAt(now + 300)
///         .claim("scope", "orders:read")
///         .build();
/// ```
public final class JwtClaimsSet {
    private final Map<String, Object> claims;

    private JwtClaimsSet(Map<String, Object> claims) {
        this.claims = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(claims));
    }

    public static Builder builder() {
        return new Builder();
    }

    /// A builder that starts from these claims.
    public static Builder from(JwtClaimsSet claims) {
        Builder builder = new Builder();
        builder.claims.putAll(claims.claims);
        return builder;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public Object getClaim(String name) {
        return claims.get(name);
    }

    /// Builds a [JwtClaimsSet].
    public static final class Builder {
        private final Map<String, Object> claims = new LinkedHashMap<String, Object>();

        private Builder() {
        }

        /// `iss`.
        public Builder issuer(String issuer) {
            return claim(JwtClaimNames.ISS, issuer);
        }

        /// `sub`.
        public Builder subject(String subject) {
            return claim(JwtClaimNames.SUB, subject);
        }

        /// `aud`: one audience is written as text, several as a list.
        public Builder audience(String... audience) {
            List<String> list = new ArrayList<String>();
            for (String one : audience) {
                list.add(one);
            }
            return audience(list);
        }

        /// `aud`.
        public Builder audience(List<String> audience) {
            return claim(JwtClaimNames.AUD, audience.size() == 1 ? (Object) audience.get(0)
                    : new ArrayList<String>(audience));
        }

        /// `exp`, in seconds since the epoch.
        public Builder expiresAt(long epochSeconds) {
            return claim(JwtClaimNames.EXP, Long.valueOf(epochSeconds));
        }

        /// `nbf`, in seconds since the epoch.
        public Builder notBefore(long epochSeconds) {
            return claim(JwtClaimNames.NBF, Long.valueOf(epochSeconds));
        }

        /// `iat`, in seconds since the epoch.
        public Builder issuedAt(long epochSeconds) {
            return claim(JwtClaimNames.IAT, Long.valueOf(epochSeconds));
        }

        /// `jti`.
        public Builder id(String jti) {
            return claim(JwtClaimNames.JTI, jti);
        }

        /// Any claim: text, a number, a boolean, or a List or Map of those.
        public Builder claim(String name, Object value) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("A claim needs a name");
            }
            if (value == null) {
                throw new IllegalArgumentException("The claim " + name + " needs a value");
            }
            claims.put(name, value);
            return this;
        }

        public JwtClaimsSet build() {
            if (claims.isEmpty()) {
                throw new IllegalArgumentException("claims cannot be empty");
            }
            return new JwtClaimsSet(claims);
        }
    }
}
