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
package com.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Base64;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.ModularPbkdf2PasswordEncoder;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.migration.IdentityMigration;
import com.codename1.backend.security.migration.MigrationReport;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProvider;
import com.codename1.backend.security.oauth2.client.credentials.ClientCredentialsGrant;
import com.codename1.backend.security.oauth2.client.credentials.GoogleServiceAccountCredentials;
import com.codename1.backend.security.oauth2.client.credentials.JwtBearerGrant;
import com.codename1.backend.security.oauth2.client.credentials.TokenAcquisitionException;
import com.codename1.backend.security.oauth2.client.credentials.TokenEndpoint;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtTimestampValidator;

/// The outbound credential grants on this runtime: the same form, the same
/// assertion claims, the same caching and the same refusal on both, against an
/// endpoint in process. The assertions carry a random jti, so they are checked by
/// verifying them rather than compared as VALUE lines.
final class AuthCheckCredentials {
    private static final String TOKEN_URI = "https://auth.example.com/oauth/token";

    private AuthCheckCredentials() {
    }

    static final class Endpoint implements TokenEndpoint.Transport {
        final List forms = new ArrayList();
        final List headers = new ArrayList();
        int status = 200;
        String body;
        int calls;

        public TokenEndpoint.Response post(String uri, List h, String form) {
            calls++;
            forms.add(form);
            headers.add(h);
            return new TokenEndpoint.Response(status, body != null ? body
                    : "{\"access_token\":\"tok-" + calls + "\",\"token_type\":\"Bearer\","
                    + "\"expires_in\":3600}");
        }

        Map lastForm() {
            return OAuth2Parameters.parse((String) forms.get(forms.size() - 1));
        }
    }

    static final class FixedClock implements Clock {
        long now = 1700000000000L;

        public long currentTimeMillis() {
            return now;
        }
    }

    static void run() throws Exception {
        clientCredentials();
        jwtBearer();
        caching();
        migration();
    }

    /// Hashes from other frameworks (computed with Python's hashlib) and an
    /// import that keeps one, refuses one and reports the rest.
    private static void migration() {
        String pw = "correct horse";
        String[][] vectors = {
            {"DJANGO", "pbkdf2_sha256$1000$seasalt$mQnueSakb748zqBAC1tmWVZsZbi2zPGZarEzTGdfmso="},
            {"DJANGO", "pbkdf2_sha1$1000$seasalt$iQvkNOF1wEL4Khh8eogJ8rUhipM="},
            {"PASSLIB", "$pbkdf2-sha256$1000$AAECAwQFBgcICQoLDA0ODw$"
                + "yRTMTwbMbo9G0VfjobWqerzuuxe7BETNTErBbKKumGQ"},
            {"WERKZEUG", "pbkdf2:sha256:1000$NaCl1234$09648d88c553b8723dc823d15e17163d3110a5d75b"
                + "040381a3557783e98e82ef"}};
        for (int iter = 0 ; iter < vectors.length ; iter++) {
            ModularPbkdf2PasswordEncoder encoder = new ModularPbkdf2PasswordEncoder(
                    ModularPbkdf2PasswordEncoder.Format.valueOf(vectors[iter][0]));
            AuthCheck.value("legacy hash " + iter, encoder.matches(pw, vectors[iter][1]) + " "
                    + encoder.matches("wrong", vectors[iter][1]));
        }
        Map encoders = new java.util.LinkedHashMap();
        encoders.put("pbkdf2-sha256", new Pbkdf2Sha256PasswordEncoder());
        encoders.put("django", new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.DJANGO));
        DelegatingPasswordEncoder passwords = new DelegatingPasswordEncoder("pbkdf2-sha256", encoders);
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        IdentityMigration migration = new IdentityMigration(users, passwords);
        migration.setPasswordPrefix("django");
        migration.user("Ada@Example.com", vectors[0][1], java.util.Arrays.asList(
                new String[] {"ROLE_ADMIN"}), true);
        migration.user("old", "{md4}00", null, true);
        migration.user("old", vectors[0][1], null, true);
        MigrationReport report = migration.finish();
        AuthCheck.value("migration report", report.count(MigrationReport.Outcome.MIGRATED) + " "
                + report.count(MigrationReport.Outcome.NEEDS_ACTION) + " "
                + report.count(MigrationReport.Outcome.SKIPPED) + " "
                + report.count(MigrationReport.Outcome.NOT_CARRIED));
        String stored = users.loadUserByUsername("Ada@Example.com").getPassword();
        AuthCheck.check("imported hash verifies", "true", String.valueOf(passwords.matches(pw, stored)));
        AuthCheck.check("unverifiable hash replaced", "false", String.valueOf(passwords.matches(
                "00", users.loadUserByUsername("old").getPassword())));
    }

    private static DefaultJwtDecoder verifier(String publicPem, String keyId) throws IOException {
        final List keys = JwkSet.of(new Jwk[] {Jwk.ofPublicKey(KeyFiles.publicKey(publicPem))
                .withKeyId(keyId)}).getKeys();
        return DefaultJwtDecoder.withJwkSource(new JwkSource() {
            public List getKeys() {
                return keys;
            }
        }).jwsAlgorithms(new com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm[] {
            SignatureAlgorithm.RS256, SignatureAlgorithm.ES256}).build();
    }

    private static void clientCredentials() throws IOException {
        Endpoint endpoint = new Endpoint();
        String token = ClientCredentialsGrant.builder(TOKEN_URI, "orders svc")
                .clientSecret("p@ss:word").scopes(new String[] {"a", "b"}).transport(endpoint)
                .build().fetch().getTokenValue();
        AuthCheck.check("client credentials token", "tok-1", token);
        AuthCheck.value("client credentials form", (String) endpoint.forms.get(0));
        List h = (List) endpoint.headers.get(0);
        String basic = null;
        for (int iter = 0 ; iter < h.size() ; iter++) {
            String line = (String) h.get(iter);
            if (line.startsWith("Authorization: Basic ")) {
                basic = new String(Base64.decode(line.substring(21)), "UTF-8");
            }
        }
        AuthCheck.check("client_secret_basic header", "orders%20svc:p%40ss%3Aword", basic);

        Endpoint keyed = new Endpoint();
        FixedClock clock = new FixedClock();
        ClientCredentialsGrant.builder(TOKEN_URI, "orders")
                .privateKeyJwt(Jwk.ofPrivateKey(KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM))
                        .withKeyId("key-1")).transport(keyed).clock(clock).build().fetch();
        Map form = keyed.lastForm();
        Jwt assertion = verifierAt(KeyFixtures.RSA_PUBLIC_PEM, "key-1",
                (String) form.get("client_assertion"));
        AuthCheck.check("private_key_jwt iss", "orders", assertion == null ? null : assertion.getIssuer());
        AuthCheck.check("private_key_jwt sub", "orders", assertion == null ? null : assertion.getSubject());
    }

    /// Decodes against the fixed clock the assertions were signed at.
    private static Jwt verifierAt(String publicPem, String keyId, String token) throws IOException {
        DefaultJwtDecoder decoder = verifier(publicPem, keyId);
        JwtTimestampValidator time = new JwtTimestampValidator();
        time.setClock(new FixedClock());
        decoder.setJwtValidator(time);
        try {
            return decoder.decode(token);
        } catch (RuntimeException err) {
            AuthCheck.fail("assertion verifies", err.getMessage());
            return null;
        }
    }

    private static void jwtBearer() throws IOException {
        String[][] keys = {{KeyFixtures.RSA_PKCS8_PEM, KeyFixtures.RSA_PUBLIC_PEM, "RS256"},
            {KeyFixtures.EC256_PKCS8_PEM, KeyFixtures.EC256_PUBLIC_PEM, "ES256"}};
        for (int iter = 0 ; iter < keys.length ; iter++) {
            Endpoint endpoint = new Endpoint();
            FixedClock clock = new FixedClock();
            JwtBearerGrant.builder(TOKEN_URI, "svc@example.iam",
                    Jwk.ofPrivateKey(KeyFiles.privateKey(keys[iter][0])).withKeyId("k" + iter))
                    .subject("ada@example.com").scopes(new String[] {"s1", "s2"})
                    .transport(endpoint).clock(clock).build().fetch();
            Map form = endpoint.lastForm();
            AuthCheck.value("jwt bearer grant_type " + keys[iter][2], (String) form.get("grant_type"));
            Jwt jwt = verifierAt(keys[iter][1], "k" + iter, (String) form.get("assertion"));
            if (jwt == null) {
                continue;
            }
            AuthCheck.value("jwt bearer claims " + keys[iter][2], jwt.getHeaders().get("alg") + " "
                    + jwt.getIssuer() + " " + jwt.getSubject() + " " + jwt.getClaimAsString("scope")
                    + " " + jwt.getClaimAsLong("iat") + " " + jwt.getClaimAsLong("exp")
                    + " " + jwt.getClaimAsStringList("aud"));
        }
        StringBuilder file = new StringBuilder("{\"type\":\"service_account\",\"client_email\":"
                + "\"svc@demo.iam.gserviceaccount.com\",\"private_key_id\":\"abc123\","
                + "\"private_key\":\"");
        String pem = KeyFixtures.RSA_PKCS8_PEM;
        for (int iter = 0 ; iter < pem.length() ; iter++) {
            char c = pem.charAt(iter);
            if (c == '\n') {
                file.append("\\n");
            } else {
                file.append(c);
            }
        }
        file.append("\"}");
        GoogleServiceAccountCredentials account = GoogleServiceAccountCredentials.fromJson(
                file.toString());
        Endpoint google = new Endpoint();
        account.builder().scopes(new String[] {"https://www.googleapis.com/auth/cloud-platform"})
                .transport(google).clock(new FixedClock()).build().fetch();
        Jwt jwt = verifierAt(KeyFixtures.RSA_PUBLIC_PEM, "abc123",
                (String) google.lastForm().get("assertion"));
        AuthCheck.value("google assertion", jwt == null ? "unverified" : jwt.getIssuer() + " "
                + jwt.getClaimAsStringList("aud") + " " + jwt.getClaimAsString("scope"));
    }

    private static void caching() {
        Endpoint endpoint = new Endpoint();
        FixedClock clock = new FixedClock();
        AccessTokenProvider provider = new AccessTokenProvider(ClientCredentialsGrant
                .builder(TOKEN_URI, "c").clientSecret("secret-value").transport(endpoint)
                .clock(clock).build());
        provider.setClock(clock);
        provider.getTokenValue();
        clock.now += (3600 - 61) * 1000L;
        AuthCheck.check("cached until the skew", "tok-1", provider.getTokenValue());
        clock.now += 2000L;
        AuthCheck.check("refreshed inside the skew", "tok-2", provider.getTokenValue());
        endpoint.status = 503;
        endpoint.body = "{\"error\":\"temporarily_unavailable\"}";
        clock.now += 3600 * 1000L;
        try {
            provider.getToken();
            AuthCheck.fail("expired token after a failed refresh", "a token was returned");
        } catch (TokenAcquisitionException refused) {
            AuthCheck.value("failed refresh", refused.getStatus() + " " + refused.getErrorCode()
                    + " " + refused.isRetryable() + " " + refused.getMessage());
            AuthCheck.check("failure names no secret", "-1",
                    String.valueOf(refused.getMessage().indexOf("secret-value")));
        }
        int calls = endpoint.calls;
        try {
            provider.getToken();
        } catch (TokenAcquisitionException backingOff) {
            AuthCheck.check("backs off", String.valueOf(calls), String.valueOf(endpoint.calls));
        }
    }
}
