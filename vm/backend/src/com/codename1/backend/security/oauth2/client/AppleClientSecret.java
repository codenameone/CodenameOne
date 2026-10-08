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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import java.io.IOException;
import java.util.List;

/// The client secret of Sign in with Apple, which is not a text Apple issues
/// but a token this server signs: an ES256 JWT naming the team, the client and
/// Apple, signed with the `.p8` key downloaded from the developer account.
///
/// ```java
/// CommonOAuth2Provider.APPLE.getBuilder("apple")
///         .clientId("com.example.web")          // the Services ID
///         .clientSecretSupplier(AppleClientSecret.fromFile(teamId, keyId, "AuthKey.p8"))
///         .build();
/// ```
///
/// A secret is good for an hour and is made again when it has under five
/// minutes left, so signing costs one signature an hour and not one a sign-in.
public final class AppleClientSecret implements ClientRegistration.ClientSecretSupplier {
    /// Who the token is for.
    public static final String AUDIENCE = "https://appleid.apple.com";
    private static final long LIFETIME_SECONDS = 3600;
    private static final long MARGIN_SECONDS = 300;

    private final String teamId;
    private final String keyId;
    private final DefaultJwtEncoder encoder;
    private Clock clock = Clock.SYSTEM;
    private String cached;
    private String cachedFor;
    private long cachedUntil;

    /// @param teamId the ten-character Team ID of the developer account
    /// @param keyId the Key ID of the Sign in with Apple key
    /// @param privateKeyPem the text of the `.p8` file
    public AppleClientSecret(String teamId, String keyId, String privateKeyPem)
            throws IOException {
        if (teamId == null || teamId.length() == 0 || keyId == null || keyId.length() == 0) {
            throw new IllegalArgumentException("A team id and a key id are required");
        }
        this.teamId = teamId;
        this.keyId = keyId;
        Jwk key = Jwk.ofPrivateKey(KeyFiles.privateKey(privateKeyPem)).withKeyId(keyId);
        if (!"EC".equals(key.getKeyType()) || !"P-256".equals(key.getCurve())) {
            throw new IOException("Sign in with Apple signs with a P-256 key; this one is "
                    + key.getKeyType() + (key.getCurve() == null ? "" : " " + key.getCurve()));
        }
        this.encoder = new DefaultJwtEncoder(new OneKey(JwkSet.of(key).getKeys()));
    }

    /// The one key, as the encoder asks for keys.
    private static final class OneKey implements JwkSource {
        private final List<Jwk> keys;

        OneKey(List<Jwk> keys) {
            this.keys = keys;
        }

        @Override
        public List<Jwk> getKeys() {
            return keys;
        }
    }

    /// [#AppleClientSecret(String, String, String)] reading the key from a file.
    public static AppleClientSecret fromFile(String teamId, String keyId, String path)
            throws IOException {
        return new AppleClientSecret(teamId, keyId, KeyFiles.read(path));
    }

    /// The clock the secret's times are read from; for tests.
    public synchronized void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized String getClientSecret(ClientRegistration registration) {
        long now = clock.currentTimeMillis() / 1000L;
        String clientId = registration.getClientId();
        if (cached != null && clientId.equals(cachedFor) && now < cachedUntil - MARGIN_SECONDS) {
            return cached;
        }
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(teamId).issuedAt(now)
                .expiresAt(now + LIFETIME_SECONDS).audience(AUDIENCE).subject(clientId).build();
        cached = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).keyId(keyId).build(), claims))
                .getTokenValue();
        cachedFor = clientId;
        cachedUntil = now + LIFETIME_SECONDS;
        return cached;
    }
}
