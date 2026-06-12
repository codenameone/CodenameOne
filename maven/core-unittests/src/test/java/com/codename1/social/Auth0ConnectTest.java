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

import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.webauthn.WebAuthnException;
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
 * Exercises {@link Auth0Connect} against the mock network layer in
 * {@link TestCodenameOneImplementation}. The interactive {@code signIn} /
 * passkey-assertion success branches open a browser / native WebAuthn sheet
 * that cannot complete head-less, so those are driven only as far as the
 * deterministic guard, parse and error paths; the configuration, builder and
 * {@code /passkey/*} request handling are covered end to end.
 */
class Auth0ConnectTest extends UITestBase {

    private static final String DOMAIN = "dev-xyz.us.auth0.com";
    private static final String BASE = "https://" + DOMAIN;

    @AfterEach
    void cleanup() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
        // reset the shared singleton's mutable config so tests stay independent
        Auth0Connect.getInstance().withDomain(null).withAudience(null);
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

    // ---- configuration / builders -----------------------------------

    @Test
    void getInstanceIsSingleton() {
        assertSame(Auth0Connect.getInstance(), Auth0Connect.getInstance());
    }

    @Test
    void builderStoresDomainAndAudienceAndChains() {
        Auth0Connect a = Auth0Connect.getInstance();
        assertSame(a, a.withDomain(DOMAIN));
        assertSame(a, a.withAudience("https://api.example.com"));
        assertEquals(DOMAIN, a.getDomain());
        assertEquals("https://api.example.com", a.getAudience());
    }

    @Test
    void nativeLoginNotSupported() {
        assertFalse(Auth0Connect.getInstance().isNativeLoginSupported());
    }

    // ---- domain guard on every entry point --------------------------

    @Test
    void signInWithoutDomainErrorsImmediately() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(null);
        Outcome<OidcTokens> r = await(a.signIn("client", "app:/cb", "openid"));
        assertNull(r.value);
        assertInstanceOf(IllegalStateException.class, r.error);
    }

    @Test
    void signInWithPasskeyWithoutDomainErrorsImmediately() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(null);
        Outcome<OidcTokens> r = await(a.signInWithPasskey("client", "realm"));
        assertNull(r.value);
        assertInstanceOf(IllegalStateException.class, r.error);
    }

    @Test
    void registerPasskeyWithoutDomainErrorsImmediately() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(null);
        Outcome<OidcTokens> r = await(a.registerPasskey("client", "realm", "a@b.com", "A B"));
        assertNull(r.value);
        assertInstanceOf(IllegalStateException.class, r.error);
    }

    // ---- discovery failure surfaced through signIn ------------------

    @Test
    void signInSurfacesDiscoveryFailure() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        // empty well-known document -> OidcClient.discover fails with DISCOVERY_FAILED
        mock(BASE + "/.well-known/openid-configuration", 200, "{}");
        Outcome<OidcTokens> r = await(a.signIn("client", "app:/cb", "openid"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.DISCOVERY_FAILED, ((OidcException) r.error).getError());
    }

    // ---- signInWithPasskey: /passkey/challenge handling -------------

    @Test
    void passkeyChallengeOAuthErrorIsSurfaced() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        mock(BASE + "/passkey/challenge", 200,
                "{\"error\":\"invalid_request\",\"error_description\":\"bad realm\"}");
        Outcome<OidcTokens> r = await(a.signInWithPasskey("client", "realm"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        OidcException ex = (OidcException) r.error;
        assertEquals("invalid_request", ex.getError());
        assertEquals("bad realm", ex.getErrorDescription());
    }

    @Test
    void passkeyChallengeEmptyBodyIsSurfaced() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        mock(BASE + "/passkey/challenge", 200, "");
        Outcome<OidcTokens> r = await(a.signInWithPasskey("client", "realm"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.INVALID_GRANT, ((OidcException) r.error).getError());
    }

    @Test
    void passkeyChallengeMissingFieldsIsInvalidGrant() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        // valid JSON, but missing auth_session / authn_params_public_key
        mock(BASE + "/passkey/challenge", 200, "{\"foo\":\"bar\"}");
        Outcome<OidcTokens> r = await(a.signInWithPasskey("client", "realm"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.INVALID_GRANT, ((OidcException) r.error).getError());
    }

    @Test
    void passkeyChallengeWellFormedReachesWebAuthnLayer() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        // a well-formed challenge drives runPasskeyAssertion all the way into
        // WebAuthnClient.get(); with no native provider that fails with
        // NOT_IMPLEMENTED, which proves the serialise + dispatch path ran.
        mock(BASE + "/passkey/challenge", 200,
                "{\"auth_session\":\"sess-123\",\"authn_params_public_key\":"
                        + "{\"rpId\":\"" + DOMAIN + "\",\"challenge\":\"abc\","
                        + "\"userVerification\":\"required\"}}");
        Outcome<OidcTokens> r = await(a.signInWithPasskey("client", "realm", "openid", "email"));
        assertNull(r.value);
        assertInstanceOf(WebAuthnException.class, r.error);
        assertEquals(WebAuthnException.NOT_IMPLEMENTED, ((WebAuthnException) r.error).getError());
    }

    // ---- registerPasskey: /passkey/register handling ----------------

    @Test
    void registerPasskeyOAuthErrorIsSurfaced() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        mock(BASE + "/passkey/register", 200,
                "{\"error\":\"signup_disabled\"}");
        Outcome<OidcTokens> r = await(a.registerPasskey("client", "realm", "a@b.com", "A B"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals("signup_disabled", ((OidcException) r.error).getError());
    }

    @Test
    void registerPasskeyMissingFieldsIsInvalidGrant() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        mock(BASE + "/passkey/register", 200, "{\"auth_session\":\"s\"}");
        Outcome<OidcTokens> r = await(a.registerPasskey("client", "realm", "a@b.com", "A B"));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.INVALID_GRANT, ((OidcException) r.error).getError());
    }

    @Test
    void registerPasskeyWellFormedReachesWebAuthnLayer() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        mock(BASE + "/passkey/register", 200,
                "{\"auth_session\":\"sess-9\",\"authn_params_public_key\":"
                        + "{\"rp\":{\"id\":\"" + DOMAIN + "\"},\"challenge\":\"xyz\"}}");
        Outcome<OidcTokens> r = await(
                a.registerPasskey("client", null, "user@example.com", "User Name", "openid"));
        assertNull(r.value);
        assertInstanceOf(WebAuthnException.class, r.error);
        assertEquals(WebAuthnException.NOT_IMPLEMENTED, ((WebAuthnException) r.error).getError());
    }

    @Test
    void registerPasskeyHandlesNullEmailAndName() {
        Auth0Connect a = Auth0Connect.getInstance().withDomain(DOMAIN);
        // exercises the empty user_profile branch (both email and name null)
        mock(BASE + "/passkey/register", 200, "{\"foo\":\"bar\"}");
        Outcome<OidcTokens> r = await(a.registerPasskey("client", "realm", null, null));
        assertNull(r.value);
        assertInstanceOf(OidcException.class, r.error);
        assertEquals(OidcException.INVALID_GRANT, ((OidcException) r.error).getError());
    }

    // ---- helpers ----------------------------------------------------

    private static final class Outcome<T> {
        final T value;
        final Throwable error;

        Outcome(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }
}
