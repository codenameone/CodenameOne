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
package com.codenameone.examples.brickyard.economy;

import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtClaimValidator;
import com.codename1.backend.security.oauth2.jwt.JwtException;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;
import java.io.IOException;

/// Authenticates a Pub/Sub push: the OIDC token Google sends in
/// `Authorization: Bearer`, signed by a key of Google's JWKS, issued by
/// `https://accounts.google.com`, for the audience the push subscription was
/// given, and -- when one is configured -- by the subscription's service
/// account (`email`, with `email_verified`).
final class PubSubPushAuth {
    static final String ISSUER = "https://accounts.google.com";
    static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";

    private final DefaultJwtDecoder decoder;

    PubSubPushAuth(JwkSource keys, String audience, final String serviceAccount) {
        // Google signs with RS256; ES256 is accepted too so a test can sign with a
        // small key. Which algorithm is usable is still settled by the key itself.
        decoder = DefaultJwtDecoder.withJwkSource(keys)
                .jwsAlgorithms(SignatureAlgorithm.RS256, SignatureAlgorithm.ES256).build();
        OAuth2TokenValidator<Jwt> issuer = new JwtIssuerValidator(ISSUER);
        OAuth2TokenValidator<Jwt> aud = new JwtAudienceValidator(audience);
        if (serviceAccount == null || serviceAccount.length() == 0) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(issuer, aud));
        } else {
            OAuth2TokenValidator<Jwt> email = new JwtClaimValidator("email", v -> serviceAccount.equals(v));
            OAuth2TokenValidator<Jwt> verified = new JwtClaimValidator("email_verified",
                    v -> Boolean.TRUE.equals(v) || "true".equals(v));
            decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(issuer, aud, email, verified));
        }
    }

    /// Google's published keys, fetched and cached.
    static PubSubPushAuth google(String audience, String serviceAccount) {
        return new PubSubPushAuth(new RemoteJwkSet(GOOGLE_KEYS), audience, serviceAccount);
    }

    /// @throws IOException when the header is not a token Google issued for this endpoint
    void check(String authorization) throws IOException {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new IOException("No bearer token");
        }
        try {
            decoder.decode(authorization.substring(7).trim());
        } catch (JwtException refused) {
            throw new IOException(refused.getMessage(), refused);
        }
    }
}
