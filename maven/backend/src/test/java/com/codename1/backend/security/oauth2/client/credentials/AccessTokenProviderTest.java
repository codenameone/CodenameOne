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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessTokenProviderTest {
    private static final String TOKEN_URI = "https://auth.example.com/oauth/token";
    private static final String SECRET = "s3cr3t-never-printed";

    /// A token endpoint in process: records what was posted, answers what it is told.
    static final class FakeEndpoint implements TokenEndpoint.Transport {
        final List<String> forms = Collections.synchronizedList(new ArrayList<String>());
        final List<List<String>> headers = Collections.synchronizedList(new ArrayList<List<String>>());
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<TokenEndpoint.Response> answer = new AtomicReference<TokenEndpoint.Response>();
        volatile CountDownLatch gate;
        volatile IOException unreachable;

        FakeEndpoint answering(int status, String body) {
            answer.set(new TokenEndpoint.Response(status, body));
            return this;
        }

        @Override
        public TokenEndpoint.Response post(String uri, List<String> h, String form) throws IOException {
            int n = calls.incrementAndGet();
            forms.add(form);
            headers.add(h);
            CountDownLatch g = gate;
            if (g != null) {
                try {
                    g.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
            if (unreachable != null) {
                throw unreachable;
            }
            TokenEndpoint.Response r = answer.get();
            if (r == null) {
                return new TokenEndpoint.Response(200, "{\"access_token\":\"tok-" + n
                        + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
            }
            return r;
        }
    }

    static final class FakeClock implements Clock {
        final AtomicLong now = new AtomicLong(System.currentTimeMillis());

        @Override
        public long currentTimeMillis() {
            return now.get();
        }

        void advanceSeconds(long seconds) {
            now.addAndGet(seconds * 1000L);
        }
    }

    private static Jwk rsaKey() throws IOException {
        return Jwk.ofPrivateKey(KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM)).withKeyId("key-1");
    }

    /// A decoder that knows the public key under the id the assertion names.
    static DefaultJwtDecoder verifier(String publicPem, String keyId) throws IOException {
        final List<Jwk> keys = JwkSet.of(Jwk.ofPublicKey(KeyFiles.publicKey(publicPem)).withKeyId(keyId))
                .getKeys();
        return DefaultJwtDecoder.withJwkSource(() -> keys).build();
    }

    private static Map<String, String> form(String body) {
        return OAuth2Parameters.parse(body);
    }

    @Test
    @DisplayName("client_secret_basic sends the secret in a header, form-encoded, and never in the form")
    void clientCredentialsBasic() {
        FakeEndpoint endpoint = new FakeEndpoint();
        AccessToken token = ClientCredentialsGrant.builder(TOKEN_URI, "orders svc")
                .clientSecret("p@ss:word").scopes("a", "b").audience("https://api.example.com")
                .transport(endpoint).build().fetch();
        assertEquals("tok-1", token.getTokenValue());
        Map<String, String> sent = form(endpoint.forms.get(0));
        assertEquals("client_credentials", sent.get("grant_type"));
        assertEquals("a b", sent.get("scope"));
        assertEquals("https://api.example.com", sent.get("audience"));
        assertFalse(sent.containsKey("client_secret"));
        String auth = null;
        for (String h : endpoint.headers.get(0)) {
            if (h.startsWith("Authorization: Basic ")) {
                auth = new String(Base64.decode(h.substring(21)), StandardCharsets.UTF_8);
            }
        }
        // RFC 6749 2.3.1: each half form-encoded before joining.
        assertEquals("orders%20svc:p%40ss%3Aword", auth);
        assertTrue(endpoint.headers.get(0).contains("Content-Type: application/x-www-form-urlencoded"));
    }

    @Test
    @DisplayName("client_secret_post puts the id and secret in the form")
    void clientCredentialsPost() {
        FakeEndpoint endpoint = new FakeEndpoint();
        ClientCredentialsGrant.builder(TOKEN_URI, "orders").clientSecret(SECRET,
                ClientCredentialsGrant.Authentication.CLIENT_SECRET_POST).transport(endpoint)
                .build().fetch();
        Map<String, String> sent = form(endpoint.forms.get(0));
        assertEquals("orders", sent.get("client_id"));
        assertEquals(SECRET, sent.get("client_secret"));
        for (String h : endpoint.headers.get(0)) {
            assertFalse(h.startsWith("Authorization"), h);
        }
    }

    @Test
    @DisplayName("private_key_jwt sends a client assertion that verifies against the public key")
    void privateKeyJwt() throws IOException {
        FakeEndpoint endpoint = new FakeEndpoint();
        FakeClock clock = new FakeClock();
        ClientCredentialsGrant.builder(TOKEN_URI, "orders").privateKeyJwt(rsaKey())
                .transport(endpoint).clock(clock).build().fetch();
        Map<String, String> sent = form(endpoint.forms.get(0));
        assertEquals(ClientCredentialsGrant.JWT_BEARER_ASSERTION_TYPE, sent.get("client_assertion_type"));
        Jwt assertion = verifier(KeyFixtures.RSA_PUBLIC_PEM, "key-1").decode(sent.get("client_assertion"));
        assertEquals("orders", assertion.getIssuer());
        assertEquals("orders", assertion.getSubject());
        assertEquals(Arrays.asList(TOKEN_URI), assertion.getClaimAsStringList("aud"));
        assertEquals("key-1", assertion.getHeaders().get("kid"));
        assertEquals("RS256", assertion.getHeaders().get("alg"));
        assertNotNull(assertion.getClaimAsString("jti"));
    }

    @Test
    @DisplayName("the JWT bearer assertion carries the claims, scopes and lifetime, signed with ES256 or RS256")
    void jwtBearerAssertion() throws IOException {
        for (String[] key : new String[][] {{KeyFixtures.RSA_PKCS8_PEM, KeyFixtures.RSA_PUBLIC_PEM, "RS256"},
            {KeyFixtures.EC256_PKCS8_PEM, KeyFixtures.EC256_PUBLIC_PEM, "ES256"}}) {
            FakeEndpoint endpoint = new FakeEndpoint();
            FakeClock clock = new FakeClock();
            JwtBearerGrant.builder(TOKEN_URI, "svc@example.iam", Jwk.ofPrivateKey(KeyFiles.privateKey(key[0])))
                    .subject("ada@example.com").scopes("s1", "s2").scopesInAssertion(true)
                    .transport(endpoint).clock(clock).build().fetch();
            Map<String, String> sent = form(endpoint.forms.get(0));
            assertEquals(JwtBearerGrant.GRANT_TYPE, sent.get("grant_type"));
            assertFalse(sent.containsKey("scope"), "scopes asked to go in the assertion");
            Jwt jwt = DefaultJwtDecoder.withPublicKey(KeyFiles.publicKey(key[1])).build()
                    .decode(sent.get("assertion"));
            assertEquals(key[2], jwt.getHeaders().get("alg"));
            assertEquals("svc@example.iam", jwt.getIssuer());
            assertEquals("ada@example.com", jwt.getSubject());
            assertEquals("s1 s2", jwt.getClaimAsString("scope"));
            long iat = jwt.getClaimAsLong("iat").longValue();
            assertEquals(clock.now.get() / 1000L, iat);
            assertEquals(iat + 3600, jwt.getClaimAsLong("exp").longValue());
        }
        FakeEndpoint endpoint = new FakeEndpoint();
        JwtBearerGrant.builder(TOKEN_URI, "svc", rsaKey()).scopes("s1")
                .transport(endpoint).build().fetch();
        // RFC 7521 section 4.1: by default the scope is a form parameter.
        assertEquals("s1", form(endpoint.forms.get(0)).get("scope"));
        assertEquals(null, verifier(KeyFixtures.RSA_PUBLIC_PEM, "key-1").decode(
                form(endpoint.forms.get(0)).get("assertion")).getClaimAsString("scope"));
    }

    @Test
    @DisplayName("a Google service-account file becomes a JWT bearer grant against its token_uri, with its key id")
    void googleServiceAccount() throws IOException {
        Map<String, Object> file = new LinkedHashMap<String, Object>();
        file.put("type", "service_account");
        file.put("project_id", "demo-project");
        file.put("private_key_id", "abc123");
        file.put("private_key", KeyFixtures.RSA_PKCS8_PEM);
        file.put("client_email", "svc@demo-project.iam.gserviceaccount.com");
        file.put("token_uri", "https://oauth2.googleapis.com/token");
        GoogleServiceAccountCredentials account = GoogleServiceAccountCredentials.fromJson(Json.write(file));
        assertEquals("demo-project", account.getProjectId());
        assertFalse(account.toString().contains("PRIVATE"));
        FakeEndpoint endpoint = new FakeEndpoint();
        account.builder().scopes("https://www.googleapis.com/auth/cloud-platform").transport(endpoint)
                .build().fetch();
        Jwt jwt = verifier(KeyFixtures.RSA_PUBLIC_PEM, "abc123")
                .decode(form(endpoint.forms.get(0)).get("assertion"));
        assertEquals("abc123", jwt.getHeaders().get("kid"));
        assertEquals("svc@demo-project.iam.gserviceaccount.com", jwt.getIssuer());
        assertEquals(Arrays.asList("https://oauth2.googleapis.com/token"), jwt.getClaimAsStringList("aud"));

        file.put("type", "authorized_user");
        IOException refused = assertThrows(IOException.class,
                () -> GoogleServiceAccountCredentials.fromJson(Json.write(file)));
        assertTrue(refused.getMessage().contains("authorized_user"));
        file.put("type", "service_account");
        file.put("private_key", "-----BEGIN PRIVATE KEY-----\nnot a key\n-----END PRIVATE KEY-----\n");
        refused = assertThrows(IOException.class,
                () -> GoogleServiceAccountCredentials.fromJson(Json.write(file)));
        assertFalse(refused.getMessage().contains("not a key"), refused.getMessage());
    }

    @Test
    @DisplayName("the token is cached until the skew before expiry, then refreshed once")
    void refreshTiming() {
        FakeEndpoint endpoint = new FakeEndpoint();
        FakeClock clock = new FakeClock();
        AccessTokenProvider provider = new AccessTokenProvider(ClientCredentialsGrant.builder(TOKEN_URI, "c")
                .clientSecret(SECRET).transport(endpoint).clock(clock).build());
        provider.setClock(clock);
        assertEquals("tok-1", provider.getTokenValue());
        clock.advanceSeconds(3600 - 61);
        assertEquals("tok-1", provider.getTokenValue());
        assertEquals(1, endpoint.calls.get());
        clock.advanceSeconds(2);
        assertEquals("tok-2", provider.getTokenValue());
        assertEquals(2, endpoint.calls.get());
        provider.invalidate();
        assertEquals("tok-3", provider.getTokenValue());

        // No expires_in: the conservative default, not forever.
        endpoint.answering(200, "{\"access_token\":\"short\",\"token_type\":\"Bearer\"}");
        provider.invalidate();
        AccessToken t = provider.getToken();
        assertEquals(clock.now.get() + TokenEndpoint.DEFAULT_LIFETIME_SECONDS * 1000L, t.getExpiresAtMillis());
    }

    @Test
    @DisplayName("concurrent callers share one exchange")
    void concurrentCallersShareOneRefresh() throws Exception {
        FakeEndpoint endpoint = new FakeEndpoint();
        endpoint.gate = new CountDownLatch(1);
        final AccessTokenProvider provider = new AccessTokenProvider(ClientCredentialsGrant
                .builder(TOKEN_URI, "c").clientSecret(SECRET).transport(endpoint).build());
        final List<String> seen = Collections.synchronizedList(new ArrayList<String>());
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 16; i++) {
            Thread t = new Thread(() -> seen.add(provider.getTokenValue()));
            threads.add(t);
            t.start();
        }
        Thread.sleep(200);
        endpoint.gate.countDown();
        for (Thread t : threads) {
            t.join(10000);
        }
        assertEquals(1, endpoint.calls.get());
        assertEquals(16, seen.size());
        for (String s : seen) {
            assertEquals("tok-1", s);
        }
    }

    @Test
    @DisplayName("a failed refresh never hands out an expired token, and backs off before trying again")
    void failedRefreshAndBackoff() {
        FakeEndpoint endpoint = new FakeEndpoint();
        FakeClock clock = new FakeClock();
        AccessTokenProvider provider = new AccessTokenProvider(ClientCredentialsGrant.builder(TOKEN_URI, "c")
                .clientSecret(SECRET).transport(endpoint).clock(clock).build());
        provider.setClock(clock);
        assertEquals("tok-1", provider.getTokenValue());

        // Inside the skew, the endpoint down: the token still valid is served.
        endpoint.answering(503, "{\"error\":\"temporarily_unavailable\"}");
        clock.advanceSeconds(3600 - 30);
        assertEquals("tok-1", provider.getTokenValue());
        assertEquals(2, endpoint.calls.get());
        assertEquals(1, provider.getConsecutiveFailures());
        // Backing off: no new exchange.
        assertEquals("tok-1", provider.getTokenValue());
        assertEquals(2, endpoint.calls.get());

        // Expired: an exception, never tok-1.
        clock.advanceSeconds(31);
        TokenAcquisitionException err = assertThrows(TokenAcquisitionException.class, provider::getToken);
        assertTrue(err.isRetryable());
        assertEquals(503, err.getStatus());

        // Backoff grows; the first success resets it.
        assertEquals(2, provider.getConsecutiveFailures());
        int before = endpoint.calls.get();
        assertThrows(TokenAcquisitionException.class, provider::getToken);
        assertEquals(before, endpoint.calls.get(), "inside the backoff no exchange is made");
        endpoint.answer.set(null);
        clock.advanceSeconds(120);
        assertNotNull(provider.getToken());
        assertEquals(0, provider.getConsecutiveFailures());
    }

    @Test
    @DisplayName("a refused credential is not retryable, and a 429 or an unreachable endpoint is")
    void retryClassification() {
        String[][] cases = {{"400", "{\"error\":\"invalid_grant\"}", "false", "invalid_grant"},
            {"401", "{\"error\":\"invalid_client\"}", "false", "invalid_client"},
            {"429", "{}", "true", null}, {"500", "oops", "true", null}};
        for (String[] c : cases) {
            FakeEndpoint endpoint = new FakeEndpoint().answering(Integer.parseInt(c[0]), c[1]);
            TokenAcquisitionException err = assertThrows(TokenAcquisitionException.class,
                    () -> ClientCredentialsGrant.builder(TOKEN_URI, "c").clientSecret(SECRET)
                            .transport(endpoint).build().fetch());
            assertEquals(Boolean.parseBoolean(c[2]), err.isRetryable(), c[0]);
            assertEquals(c[3], err.getErrorCode());
        }
        FakeEndpoint down = new FakeEndpoint();
        down.unreachable = new IOException("connection refused");
        TokenAcquisitionException err = assertThrows(TokenAcquisitionException.class,
                () -> ClientCredentialsGrant.builder(TOKEN_URI, "c").clientSecret(SECRET)
                        .transport(down).build().fetch());
        assertTrue(err.isRetryable());
        assertEquals(-1, err.getStatus());
    }

    @Test
    @DisplayName("no secret, assertion or token body appears in an exception or a toString")
    void diagnosticsAreSanitized() throws IOException {
        String leakyBody = "{\"error\":\"invalid_client\",\"error_description\":\"bad " + SECRET
                + "\",\"access_token\":\"leaked-token\"}";
        FakeEndpoint endpoint = new FakeEndpoint().answering(401, leakyBody);
        ClientCredentialsGrant grant = ClientCredentialsGrant.builder(
                "https://user:pw@auth.example.com/token?key=" + SECRET, "c").clientSecret(SECRET)
                .transport(endpoint).build();
        AccessTokenProvider provider = new AccessTokenProvider(grant);
        TokenAcquisitionException err = assertThrows(TokenAcquisitionException.class, provider::getToken);
        String everything = err.getMessage() + " " + err + " " + provider + " " + grant;
        for (String secret : new String[] {SECRET, "leaked-token", "pw@", "error_description"}) {
            assertFalse(everything.contains(secret), secret + " in: " + everything);
        }
        assertTrue(err.getMessage().contains("https://auth.example.com/token"), err.getMessage());

        AccessToken token = new AccessToken("the-value", "Bearer", 0, null);
        assertFalse(token.toString().contains("the-value"));
        assertEquals("Bearer the-value", token.toAuthorizationHeader());

        FakeEndpoint jwt = new FakeEndpoint().answering(400, "{\"error\":\"invalid_grant\"}");
        JwtBearerGrant bearer = JwtBearerGrant.builder(TOKEN_URI, "svc", rsaKey()).transport(jwt).build();
        err = assertThrows(TokenAcquisitionException.class, bearer::fetch);
        String assertion = form(jwt.forms.get(0)).get("assertion");
        assertFalse((err.getMessage() + bearer).contains(assertion.substring(0, 20)));
    }

    @Test
    @DisplayName("configuration builds each grant, and names a missing key without printing values")
    void fromConfig() throws IOException {
        File pem = File.createTempFile("cn1-cc", ".pem");
        pem.deleteOnExit();
        Files.write(pem.toPath(), KeyFixtures.RSA_PKCS8_PEM.getBytes(StandardCharsets.US_ASCII));
        Properties p = new Properties();
        String k = AccessTokenProviders.PREFIX;
        p.setProperty(k + "billing.grant", "client_credentials");
        p.setProperty(k + "billing.token-uri", TOKEN_URI);
        p.setProperty(k + "billing.client-id", "orders");
        p.setProperty(k + "billing.client-secret", SECRET);
        p.setProperty(k + "billing.scopes", "a,b");
        p.setProperty(k + "signer.grant", "jwt_bearer");
        p.setProperty(k + "signer.token-uri", TOKEN_URI);
        p.setProperty(k + "signer.issuer", "svc");
        p.setProperty(k + "signer.key-file", pem.getPath());
        p.setProperty(k + "signer.key-id", "kid-9");
        p.setProperty(k + "broken.grant", "client_credentials");
        p.setProperty(k + "broken.token-uri", TOKEN_URI);
        p.setProperty(k + "broken.client-id", "x");
        Config config = Config.of(p, "prod");
        assertTrue(AccessTokenProviders.fromConfig(config, "billing").toString().contains("CLIENT_SECRET_BASIC"));
        assertTrue(AccessTokenProviders.fromConfig(config, "signer").toString().contains("iss=svc"));
        IOException missing = assertThrows(IOException.class,
                () -> AccessTokenProviders.fromConfig(config, "broken"));
        assertEquals(k + "broken.client-secret is not set", missing.getMessage());
        assertThrows(IOException.class, () -> AccessTokenProviders.fromConfig(config, "absent"));
    }

    @Test
    @DisplayName("a grant that throws something else is reported without its message")
    void foreignFailure() {
        AccessTokenProvider provider = new AccessTokenProvider(() -> {
            throw new IllegalStateException(SECRET);
        });
        TokenAcquisitionException err = assertThrows(TokenAcquisitionException.class, provider::getToken);
        assertFalse(err.getMessage().contains(SECRET));
        // Not even as the cause, which a stack trace would print.
        assertEquals(null, err.getCause());
    }

    @Test
    @DisplayName("an Error from the grant is rethrown and does not leave the provider waiting on it")
    void errorDoesNotWedge() {
        final int[] calls = {0};
        FakeClock clock = new FakeClock();
        AccessTokenProvider provider = new AccessTokenProvider(() -> {
            if (calls[0]++ == 0) {
                throw new StackOverflowError();
            }
            return new AccessToken("t", "Bearer", clock.now.get() + 3600000L, null);
        });
        provider.setClock(clock);
        assertThrows(StackOverflowError.class, provider::getToken);
        clock.advanceSeconds(10);
        assertEquals("t", provider.getTokenValue());
    }

    @Test
    @DisplayName("a token living no longer than the skew is reused for half its life")
    void shortLivedToken() {
        final int[] calls = {0};
        FakeClock clock = new FakeClock();
        AccessTokenProvider provider = new AccessTokenProvider(() -> {
            calls[0]++;
            return new AccessToken("t" + calls[0], "Bearer", clock.now.get() + 60000L, null);
        });
        provider.setClock(clock);
        provider.getToken();
        clock.advanceSeconds(29);
        assertEquals("t1", provider.getTokenValue());
        clock.advanceSeconds(2);
        assertEquals("t2", provider.getTokenValue());
        assertEquals(2, calls[0]);
    }
}
