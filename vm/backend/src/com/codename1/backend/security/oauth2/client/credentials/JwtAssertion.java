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
package com.codename1.backend.security.oauth2.client.credentials;

import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import java.util.Collection;

/// Signs the short-lived JWTs of RFC 7523: the assertion a JWT bearer grant
/// exchanges, and the `private_key_jwt` client authentication.
final class JwtAssertion {
    private final Jwk key;
    private final SignatureAlgorithm algorithm;
    private final DefaultJwtEncoder encoder;

    JwtAssertion(Jwk key) {
        if (key == null || !key.isPrivate()) {
            throw new IllegalArgumentException("An assertion is signed with a private key");
        }
        this.key = key;
        this.algorithm = algorithmFor(key);
        this.encoder = new DefaultJwtEncoder(JwkSet.of(key));
    }

    static SignatureAlgorithm algorithmFor(Jwk key) {
        if ("RSA".equals(key.getKeyType())) {
            return SignatureAlgorithm.RS256;
        }
        if ("EC".equals(key.getKeyType()) && "P-256".equals(key.getCurve())) {
            return SignatureAlgorithm.ES256;
        }
        if ("EC".equals(key.getKeyType()) && "P-384".equals(key.getCurve())) {
            return SignatureAlgorithm.ES384;
        }
        throw new IllegalArgumentException("An assertion is signed with an RSA, P-256 or "
                + "P-384 key, not " + key.getKeyType());
    }

    /// @param issuer `iss`
    /// @param subject `sub`
    /// @param audience `aud`, normally the token endpoint
    /// @param scopes a `scope` claim, as Google's JWT bearer flow wants; null for none
    /// @param nowSeconds `iat`
    /// @param lifetimeSeconds how long after `iat` the assertion expires
    String sign(String issuer, String subject, String audience, Collection<String> scopes,
                long nowSeconds, long lifetimeSeconds) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer(issuer).subject(subject)
                .audience(audience).issuedAt(nowSeconds).expiresAt(nowSeconds + lifetimeSeconds)
                .id(OAuth2Parameters.random(16));
        if (scopes != null && !scopes.isEmpty()) {
            claims.claim("scope", OAuth2Parameters.scopes(scopes));
        }
        JwsHeader.Builder header = JwsHeader.with(algorithm);
        if (key.getKeyId() != null) {
            header.keyId(key.getKeyId());
        }
        return encoder.encode(JwtEncoderParameters.from(header.build(), claims.build()))
                .getTokenValue();
    }
}
