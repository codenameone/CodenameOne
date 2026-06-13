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

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link OidcClient} end to end against the mock network layer in
 * {@link TestCodenameOneImplementation}. Covers discovery, the token-endpoint
 * POST (via {@link OidcClient#refresh}), revocation, and the configuration
 * guards -- the network-bound code paths that pure-logic tests cannot reach.
 */
public class OidcClientTest extends UITestBase {

    private static final String ISSUER = "https://issuer.example.com";
    private static final String AUTH_EP = ISSUER + "/auth";
    private static final String TOKEN_EP = ISSUER + "/token";
    private static final String REVOKE_EP = ISSUER + "/revoke";

    @AfterEach
    void clearMocks() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void mock(String url, int code, String body) {
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(url, code, code == 200 ? "OK" : "ERR", utf8(body));
    }

    private OidcConfiguration fullConfig() {
        return OidcConfiguration.newBuilder()
                .issuer(ISSUER)
                .authorizationEndpoint(AUTH_EP)
                .tokenEndpoint(TOKEN_EP)
                .revocationEndpoint(REVOKE_EP)
                .build();
    }

    private OidcClient configuredClient(OidcConfiguration cfg) {
        return OidcClient.create(cfg)
                .setClientId("client-123")
                .setRedirectUri("com.example.app:/cb")
                .setScopes("openid", "email")
                .setTokenStore(new MemoryTokenStore());
    }

    /** Blocks (driving the EDT) until the resource settles, then returns it. */
    private <T> Outcome<T> await(AsyncResource<T> resource) {
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);
        resource.ready(new SuccessCallback<T>() {
            public void onSucess(T v) {
                value.set(v);
                latch.countDown();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });
        int budget = 20000;
        while (latch.getCount() > 0 && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 10;
        }
        assertTrue(resource.isDone(), "async resource did not settle within the timeout");
        return new Outcome<T>(value.get(), error.get());
    }

    // ---- configuration / guards (no network) -------------------------

    @Test
    void createRejectsNullConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> OidcClient.create(null));
    }

    @Test
    void createExposesSuppliedConfiguration() {
        OidcConfiguration cfg = fullConfig();
        assertSame(cfg, OidcClient.create(cfg).getConfiguration());
    }

    @Test
    void settersReturnSameInstanceForChaining() {
        OidcClient c = OidcClient.create(fullConfig());
        assertSame(c, c.setClientId("x"));
        assertSame(c, c.setClientSecret("s"));
        assertSame(c, c.setRedirectUri("r"));
        assertSame(c, c.setScopes("openid"));
        assertSame(c, c.setScopes(java.util.Arrays.asList("openid", "email")));
        assertSame(c, c.setScopes((String[]) null));
        assertSame(c, c.setStoreKey("k"));
        assertSame(c, c.setEnforceNonce(false));
        assertSame(c, c.setResponseMode("form_post"));
        assertSame(c, c.setTokenStore(null));
    }

    @Test
    void authorizationParametersRejectOddCount() {
        OidcClient c = OidcClient.create(fullConfig());
        assertThrows(IllegalArgumentException.class,
                () -> c.setAuthorizationParameters("prompt"));
    }

    @Test
    void tokenParametersRejectOddCount() {
        OidcClient c = OidcClient.create(fullConfig());
        assertThrows(IllegalArgumentException.class,
                () -> c.setTokenParameters("audience"));
    }

    @Test
    void authorizeRequiresClientId() {
        OidcClient c = OidcClient.create(fullConfig()).setRedirectUri("r");
        assertThrows(IllegalStateException.class, c::authorize);
    }

    @Test
    void authorizeRequiresRedirectUri() {
        OidcClient c = OidcClient.create(fullConfig()).setClientId("x");
        assertThrows(IllegalStateException.class, c::authorize);
    }

    @Test
    void refreshRejectsNullToken() {
        OidcClient c = configuredClient(fullConfig());
        assertThrows(IllegalArgumentException.class, () -> c.refresh(null));
    }

    @Test
    void refreshRequiresTokenEndpoint() {
        OidcConfiguration noToken = OidcConfiguration.newBuilder()
                .issuer(ISSUER).authorizationEndpoint(AUTH_EP).build();
        OidcClient c = OidcClient.create(noToken).setClientId("x").setRedirectUri("r");
        assertThrows(IllegalStateException.class, () -> c.refresh("rt"));
    }

    // ---- discovery ---------------------------------------------------

    @Test
    void discoverRejectsNullIssuer() {
        assertThrows(IllegalArgumentException.class, () -> OidcClient.discover(null));
    }

    @Test
    void discoverPopulatesEndpointsFromWellKnownDocument() {
        mock(ISSUER + "/.well-known/openid-configuration", 200,
                "{\"issuer\":\"" + ISSUER + "\","
                        + "\"authorization_endpoint\":\"" + AUTH_EP + "\","
                        + "\"token_endpoint\":\"" + TOKEN_EP + "\","
                        + "\"revocation_endpoint\":\"" + REVOKE_EP + "\"}");

        Outcome<OidcClient> r = await(OidcClient.discover(ISSUER));
        assertNull(r.error);
        assertNotNull(r.value);
        OidcConfiguration cfg = r.value.getConfiguration();
        assertEquals(AUTH_EP, cfg.getAuthorizationEndpoint());
        assertEquals(TOKEN_EP, cfg.getTokenEndpoint());
        assertEquals(REVOKE_EP, cfg.getRevocationEndpoint());
    }

    @Test
    void discoverToleratesTrailingSlashesOnIssuer() {
        mock(ISSUER + "/.well-known/openid-configuration", 200,
                "{\"issuer\":\"" + ISSUER + "\",\"authorization_endpoint\":\""
                        + AUTH_EP + "\"}");

        Outcome<OidcClient> r = await(OidcClient.discover(ISSUER + "///"));
        assertNull(r.error);
        assertEquals(AUTH_EP, r.value.getConfiguration().getAuthorizationEndpoint());
    }

    @Test
    void discoverFailsOnEmptyDocument() {
        mock(ISSUER + "/.well-known/openid-configuration", 200, "{}");

        Outcome<OidcClient> r = await(OidcClient.discover(ISSUER));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.DISCOVERY_FAILED, ((OidcException) r.error).getError());
    }

    // ---- refresh / token-endpoint POST -------------------------------

    @Test
    void refreshExchangesRefreshTokenAndPersistsResult() {
        MemoryTokenStore store = new MemoryTokenStore();
        OidcClient c = OidcClient.create(fullConfig())
                .setClientId("client-123")
                .setRedirectUri("com.example.app:/cb")
                .setScopes("openid")
                .setTokenStore(store);

        mock(TOKEN_EP, 200,
                "{\"access_token\":\"AT-new\",\"token_type\":\"Bearer\","
                        + "\"expires_in\":3600,\"refresh_token\":\"RT-new\"}");

        Outcome<OidcTokens> r = await(c.refresh("RT-old"));
        assertNull(r.error);
        assertNotNull(r.value);
        assertEquals("AT-new", r.value.getAccessToken());
        assertEquals("RT-new", r.value.getRefreshToken());
        // postToTokenEndpoint must persist through the configured store.
        assertNotNull(store.saved, "refreshed tokens should be persisted");
        assertEquals("AT-new", store.saved.getAccessToken());
    }

    @Test
    void refreshFallsBackToSuppliedRefreshTokenWhenResponseOmitsIt() {
        OidcClient c = configuredClient(fullConfig());
        mock(TOKEN_EP, 200,
                "{\"access_token\":\"AT\",\"token_type\":\"Bearer\",\"expires_in\":60}");

        Outcome<OidcTokens> r = await(c.refresh("RT-original"));
        assertNull(r.error);
        // The response carried no refresh_token, so the original is retained.
        assertEquals("RT-original", r.value.getRefreshToken());
    }

    @Test
    void refreshSurfacesOAuthErrorResponse() {
        OidcClient c = configuredClient(fullConfig());
        // The token endpoint signals failure via the OAuth `error` field; the
        // client surfaces it regardless of the HTTP status line.
        mock(TOKEN_EP, 200,
                "{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}");

        Outcome<OidcTokens> r = await(c.refresh("RT"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        OidcException ex = (OidcException) r.error;
        assertEquals("invalid_grant", ex.getError());
        assertEquals("expired", ex.getErrorDescription());
    }

    // ---- revocation --------------------------------------------------

    @Test
    void revokeCompletesFalseForNullToken() {
        Outcome<Boolean> r = await(configuredClient(fullConfig()).revoke(null));
        assertNull(r.error);
        assertEquals(Boolean.FALSE, r.value);
    }

    @Test
    void revokeCompletesFalseWhenNoRevocationEndpointAdvertised() {
        OidcConfiguration noRevoke = OidcConfiguration.newBuilder()
                .issuer(ISSUER).authorizationEndpoint(AUTH_EP).tokenEndpoint(TOKEN_EP).build();
        Outcome<Boolean> r = await(configuredClient(noRevoke).revoke("tok"));
        assertNull(r.error);
        assertEquals(Boolean.FALSE, r.value);
    }

    @Test
    void revokeCompletesTrueOnSuccessfulResponse() {
        OidcClient c = configuredClient(fullConfig());
        mock(REVOKE_EP, 200, "");
        Outcome<Boolean> r = await(c.revoke("tok"));
        assertNull(r.error);
        assertEquals(Boolean.TRUE, r.value);
    }

    // ---- stored-token helpers ----------------------------------------

    @Test
    void refreshIfExpiredCompletesNullWhenNothingStored() {
        OidcClient c = configuredClient(fullConfig());
        Outcome<OidcTokens> r = await(c.refreshIfExpired(60));
        assertNull(r.error);
        assertNull(r.value);
    }

    @Test
    void loadStoredTokensReturnsWhatTheStoreHolds() {
        MemoryTokenStore store = new MemoryTokenStore();
        java.util.Map<String, Object> json = new java.util.HashMap<String, Object>();
        json.put("access_token", "stored-AT");
        store.saved = OidcTokens.fromTokenResponse(json, null);

        OidcClient c = OidcClient.create(fullConfig())
                .setClientId("client-123").setRedirectUri("r").setTokenStore(store);

        Outcome<OidcTokens> r = await(c.loadStoredTokens());
        assertNull(r.error);
        assertEquals("stored-AT", r.value.getAccessToken());
    }

    // ---- helpers -----------------------------------------------------

    private static final class Outcome<T> {
        final T value;
        final Throwable error;

        Outcome(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }

    /** In-memory {@link TokenStore} so persistence assertions stay deterministic. */
    private static final class MemoryTokenStore implements TokenStore {
        OidcTokens saved;

        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> r = new AsyncResource<OidcTokens>();
            r.complete(saved);
            return r;
        }

        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            this.saved = tokens;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }

        public AsyncResource<Boolean> clear(String key) {
            this.saved = null;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }
    }
}
