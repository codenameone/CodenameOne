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
package com.codename1.social;

import com.codename1.io.AccessToken;
import com.codename1.io.Preferences;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link AppleSignIn}. The iOS native sheet itself is unreachable
 * head-less, but the surrounding logic is fully covered with a fake
 * {@link AppleSignInNative} provider: nonce hashing, the pipe-delimited packed
 * result parser, Preferences backfill / persistence, the cancel / no-token /
 * thrown-error branches, and the configuration / web-fallback guards. The web
 * OIDC discovery error path is driven against the mock network layer.
 */
class AppleSignInTest extends UITestBase {

    @AfterEach
    void cleanup() {
        AppleSignIn.setProvider(null);
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
        // wipe the persisted profile so backfill tests stay deterministic
        Preferences.delete("cn1.applesignin.name");
        Preferences.delete("cn1.applesignin.email");
        Preferences.delete("cn1.applesignin.userid");
        Preferences.delete("cn1.applesignin.loggedIn");
        // clear mutable singleton config
        AppleSignIn.getInstance()
                .withServiceId(null).withRedirectUri(null)
                .withClientSecret(null).withDefaultScopes("name email");
    }

    /** Records a captured terminal outcome from {@link AppleSignInCallback}. */
    private static final class CapturingCallback implements AppleSignInCallback {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<AppleSignInResult> success = new AtomicReference<>();
        final AtomicReference<String> error = new AtomicReference<>();
        volatile boolean cancelled;

        public void onSuccess(AppleSignInResult result) {
            success.set(result);
            latch.countDown();
        }

        public void onError(String e) {
            error.set(e);
            latch.countDown();
        }

        public void onCancel() {
            cancelled = true;
            latch.countDown();
        }

        void await() {
            int budget = 20000;
            while (latch.getCount() > 0 && budget > 0) {
                DisplayTest.flushEdt();
                try {
                    if (latch.await(10, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                budget -= 10;
            }
            assertEquals(0, latch.getCount(), "AppleSignIn callback did not settle in time");
        }
    }

    /** Configurable fake of the native bridge. */
    private static final class FakeNative implements AppleSignInNative {
        boolean supported = true;
        boolean loggedIn;
        boolean signOutCalled;
        String packedResult;
        RuntimeException toThrow;
        volatile String lastScopes;
        volatile String lastNonce;

        public boolean isSupported() {
            return supported;
        }

        public String signIn(String scopes, String nonce) {
            lastScopes = scopes;
            lastNonce = nonce;
            if (toThrow != null) {
                throw toThrow;
            }
            return packedResult;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }

        public void signOut() {
            signOutCalled = true;
        }
    }

    // ---- builders / config ------------------------------------------

    @Test
    void getInstanceIsSingleton() {
        assertSame(AppleSignIn.getInstance(), AppleSignIn.getInstance());
    }

    @Test
    void appleIssuerConstantIsApplePublicIssuer() {
        assertEquals("https://appleid.apple.com", AppleSignIn.APPLE_ISSUER);
    }

    @Test
    void buildersChainAndPropagateToLogin() {
        AppleSignIn a = AppleSignIn.getInstance();
        assertSame(a, a.withServiceId("com.example.web"));
        assertSame(a, a.withRedirectUri("https://example.com/cb"));
        assertSame(a, a.withClientSecret("jwt-secret"));
        assertSame(a, a.withDefaultScopes("email"));
        // validateToken always trusts Apple's exp claim
        assertTrue(a.validateToken(null));
        assertTrue(a.validateToken("anything"));
    }

    // ---- native support / state queries -----------------------------

    @Test
    void nativeLoginUnsupportedWithoutProvider() {
        AppleSignIn.setProvider(null);
        assertFalse(AppleSignIn.getInstance().isNativeLoginSupported());
    }

    @Test
    void nativeLoginSupportedWhenProviderSupported() {
        FakeNative n = new FakeNative();
        n.supported = true;
        AppleSignIn.setProvider(n);
        assertTrue(AppleSignIn.getInstance().isNativeLoginSupported());

        n.supported = false;
        assertFalse(AppleSignIn.getInstance().isNativeLoginSupported());
    }

    @Test
    void nativeIsLoggedInFallsBackToPreferencesWithoutProvider() {
        AppleSignIn.setProvider(null);
        assertFalse(AppleSignIn.getInstance().nativeIsLoggedIn());
        Preferences.set("cn1.applesignin.loggedIn", true);
        assertTrue(AppleSignIn.getInstance().nativeIsLoggedIn());
    }

    @Test
    void nativeIsLoggedInDelegatesToSupportedProvider() {
        FakeNative n = new FakeNative();
        n.loggedIn = true;
        AppleSignIn.setProvider(n);
        assertTrue(AppleSignIn.getInstance().nativeIsLoggedIn());
        n.loggedIn = false;
        assertFalse(AppleSignIn.getInstance().nativeIsLoggedIn());
    }

    @Test
    void nativeLogoutClearsPreferencesAndCallsProvider() {
        FakeNative n = new FakeNative();
        AppleSignIn.setProvider(n);
        Preferences.set("cn1.applesignin.loggedIn", true);
        Preferences.set("cn1.applesignin.email", "x@y.com");

        AppleSignIn.getInstance().nativeLogout();

        assertTrue(n.signOutCalled);
        assertFalse(Preferences.get("cn1.applesignin.loggedIn", false));
        assertNull(Preferences.get("cn1.applesignin.email", (String) null));
    }

    // ---- signIn argument guards -------------------------------------

    @Test
    void signInRejectsNullCallback() {
        assertThrows(IllegalArgumentException.class,
                () -> AppleSignIn.getInstance().signIn("name email", null));
    }

    // ---- web fallback (no native provider) --------------------------

    @Test
    void webFallbackWithoutConfigReportsError() {
        AppleSignIn.setProvider(null);
        AppleSignIn a = AppleSignIn.getInstance().withServiceId(null).withRedirectUri(null);
        CapturingCallback cb = new CapturingCallback();
        a.signIn("name email", cb);
        cb.await();
        assertNotNull(cb.error.get());
        assertTrue(cb.error.get().contains("setServiceId"));
    }

    @Test
    void webFallbackSurfacesDiscoveryFailure() {
        AppleSignIn.setProvider(null);
        AppleSignIn a = AppleSignIn.getInstance()
                .withServiceId("com.example.web")
                .withRedirectUri("https://example.com/cb");
        // empty well-known document -> discover fails -> onError("Apple OIDC discovery failed: ...")
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(
                AppleSignIn.APPLE_ISSUER + "/.well-known/openid-configuration",
                200, "OK", "{}".getBytes());
        CapturingCallback cb = new CapturingCallback();
        a.signIn("name email", cb);
        cb.await();
        assertNotNull(cb.error.get());
        assertTrue(cb.error.get().startsWith("Apple OIDC discovery failed"),
                "got: " + cb.error.get());
    }

    // ---- native path via the fake provider --------------------------

    @Test
    void nativeSignInParsesPackedResultAndPersists() {
        FakeNative n = new FakeNative();
        // idToken|authCode|user|given|family|email
        n.packedResult = "TOKID|AUTHCODE|USER42|Jane|Doe|jane@example.com";
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name email", cb);
        cb.await();

        AppleSignInResult r = cb.success.get();
        assertNotNull(r, "expected onSuccess; error=" + cb.error.get());
        assertEquals("TOKID", r.getIdentityToken());
        assertEquals("AUTHCODE", r.getAuthorizationCode());
        assertEquals("USER42", r.getUserId());
        assertEquals("jane@example.com", r.getEmail());
        assertEquals("Jane Doe", r.getFullName());

        // a SHA-256 hashed nonce (base64url, no padding) was handed to the native layer
        assertNotNull(n.lastNonce);
        assertFalse(n.lastNonce.contains("="));
        assertEquals("name email", n.lastScopes);

        // profile + login flag persisted, and the access token carries id token + auth code
        assertTrue(Preferences.get("cn1.applesignin.loggedIn", false));
        assertEquals("jane@example.com", Preferences.get("cn1.applesignin.email", (String) null));
        AccessToken at = AppleSignIn.getInstance().getAccessToken();
        assertNotNull(at);
        assertEquals("AUTHCODE", at.getToken());
        assertEquals("TOKID", at.getIdentityToken());
    }

    @Test
    void nativeSignInGivenNameOnlyProducesTrimmedFullName() {
        FakeNative n = new FakeNative();
        n.packedResult = "TOK||USER|OnlyGiven||a@b.com";
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name", cb);
        cb.await();

        AppleSignInResult r = cb.success.get();
        assertNotNull(r, "error=" + cb.error.get());
        assertEquals("OnlyGiven", r.getFullName());
        assertNull(r.getAuthorizationCode());
    }

    @Test
    void nativeSignInBackfillsEmailAndNameFromPreferences() {
        // a prior login persisted the profile
        Preferences.set("cn1.applesignin.email", "stored@example.com");
        Preferences.set("cn1.applesignin.name", "Stored Name");

        FakeNative n = new FakeNative();
        // Apple omits profile on subsequent logins -> only the identity token comes back
        n.packedResult = "TOK2|CODE2|USER2|||";
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name email", cb);
        cb.await();

        AppleSignInResult r = cb.success.get();
        assertNotNull(r, "error=" + cb.error.get());
        assertEquals("stored@example.com", r.getEmail());
        assertEquals("Stored Name", r.getFullName());
    }

    @Test
    void nativeSignInEmptyResultIsCancel() {
        FakeNative n = new FakeNative();
        n.packedResult = "";
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name email", cb);
        cb.await();

        assertTrue(cb.cancelled);
        assertNull(cb.success.get());
        assertNull(cb.error.get());
    }

    @Test
    void nativeSignInNullResultIsCancel() {
        FakeNative n = new FakeNative();
        n.packedResult = null;
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn(null, cb); // null scopes -> default scopes
        cb.await();

        assertTrue(cb.cancelled);
        assertEquals("name email", n.lastScopes, "null scopes resolve to defaultScopes");
    }

    @Test
    void nativeSignInWithoutIdentityTokenIsError() {
        FakeNative n = new FakeNative();
        // first segment (identity token) empty -> parsed to null -> onError
        n.packedResult = "|CODE|USER|G|F|e@e.com";
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name email", cb);
        cb.await();

        assertNotNull(cb.error.get());
        assertTrue(cb.error.get().contains("no identity token"));
    }

    @Test
    void nativeSignInPropagatesProviderException() {
        FakeNative n = new FakeNative();
        n.toThrow = new RuntimeException("keychain blew up");
        AppleSignIn.setProvider(n);

        CapturingCallback cb = new CapturingCallback();
        AppleSignIn.getInstance().signIn("name email", cb);
        cb.await();

        assertEquals("keychain blew up", cb.error.get());
        assertNull(cb.success.get());
    }
}
