/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.RequestAuthorizer;
import com.codename1.io.Util;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcConfiguration;
import com.codename1.io.oidc.OidcRequestAuthorizer;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.PkceChallenge;
import com.codename1.io.oidc.TokenStore;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/// What the tests of the backend's security share: the server's fixtures, a
/// request that reports every answer including a refused or redirected one, and
/// a sign-in that needs no browser.
///
/// The server (scripts/hellocodenameone/backend, `SecurityConfig`) is an
/// authorization server and a resource server at once. Its issuer is whatever
/// address the request arrived on, so every endpoint here is the base URL the
/// device already uses plus a path -- the discovery document is deliberately not
/// consulted, because a device behind the emulator's 10.0.2.2 must not depend on
/// what the server believes its own name to be.
///
/// The sign-in is the authorization code flow with PKCE, driven by plain requests:
/// the authorization request carries HTTP Basic credentials, which the server's
/// sign-in chain accepts, is not allowed to follow its redirect, and the code is
/// read out of the `Location` it answers with. That is every step of what
/// `OidcClient.authorize()` does except the system browser, which a test cannot
/// operate.
public abstract class BackendAuthTestBase extends BackendClientTest {
    static final String CLIENT_ID = "hellocodenameone-app";
    static final String REDIRECT = "http://127.0.0.1/callback";
    static final String SCOPE_READ = "notes:read";
    static final String SCOPE_WRITE = "notes:write";
    static final String USER = "ada";
    static final String USER_PASSWORD = "ada-test-password";
    static final String MFA_USER = "grace";
    static final String MFA_USER_PASSWORD = "grace-test-password";
    /// The test server's fixed TOTP secret for [#MFA_USER]. Not a secret of anything.
    static final String MFA_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    static final String SESSION_COOKIE = "CN1SESSION";
    static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    /// Receives the answer of a [Call].
    interface Answer {
        void got(Call call);
    }

    /// Receives the tokens of a sign-in.
    interface SignedIn {
        void got(OidcTokens tokens);
    }

    /// Keeps tokens in memory, so a test leaves nothing in the device's storage.
    static final class MemoryTokenStore implements TokenStore {
        OidcTokens saved;
        int clears;

        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> r = new AsyncResource<OidcTokens>();
            r.complete(saved);
            return r;
        }

        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            saved = tokens;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }

        public AsyncResource<Boolean> clear(String key) {
            saved = null;
            clears++;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }
    }

    /// One request whose answer -- any status, with the headers these tests read --
    /// reaches [Answer] exactly once. It never takes the application's authorizer:
    /// a test that wants a token on one of these puts it there itself.
    final class Call extends ConnectionRequest implements ReportsOwnErrors {
        int code;
        byte[] data;
        String location;
        String challenge;
        /// The session cookie the answer set, as `name=value`, or null.
        String session;
        private Answer answer;
        private boolean reported;

        Call(String method, String path) {
            setUrl(url(path));
            setHttpMethod(method);
            setPost(!"GET".equals(method));
            setReadResponseForErrors(true);
            setDuplicateSupported(true);
            setAuthorizer(RequestAuthorizer.NONE);
            // No cookie jar. The jar is the device's, shared by every test and kept
            // between runs, so a session one test left there would decide who the
            // next test's request is from. A test that needs a session carries it
            // from one request to the next with session and cookie().
            setCookiesEnabled(false);
        }

        /// Sends a session cookie an earlier answer set.
        Call cookie(String session) {
            if (session != null) {
                addRequestHeader("Cookie", session);
            }
            return this;
        }

        Call basic(String user, String password) {
            addRequestHeader("Authorization", "Basic "
                    + Base64.encodeNoNewline((user + ":" + password).getBytes()));
            return this;
        }

        Call bearer(String token) {
            addRequestHeader("Authorization", "Bearer " + token);
            return this;
        }

        Call form(String name, String value) {
            addArgument(name, value);
            return this;
        }

        /// The redirect is the answer, not something to follow.
        Call unfollowed() {
            setFollowRedirects(false);
            return this;
        }

        void send(Answer answer) {
            this.answer = answer;
            NetworkManager.getInstance().addToQueue(this);
        }

        String text() {
            try {
                return data == null ? "" : new String(data, "UTF-8");
            } catch (IOException err) {
                return "";
            }
        }

        Map<String, Object> json() {
            try {
                return new JSONParser().parseJSON(new InputStreamReader(
                        new ByteArrayInputStream(data == null ? new byte[0] : data), "UTF-8"));
            } catch (IOException err) {
                return null;
            }
        }

        @Override
        protected void readHeaders(Object connection) throws IOException {
            location = getHeader(connection, "Location");
            challenge = getHeader(connection, "WWW-Authenticate");
            String[] set = getHeaders(connection, "Set-Cookie");
            for (int iter = 0; set != null && iter < set.length; iter++) {
                if (set[iter] != null && set[iter].startsWith(SESSION_COOKIE + "=")) {
                    int end = set[iter].indexOf(';');
                    session = end < 0 ? set[iter] : set[iter].substring(0, end);
                }
            }
        }

        @Override
        protected void handleErrorResponseCode(int responseCode, String message) {
            code = responseCode;
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            if (code == 0) {
                code = getResponseCode();
            }
            data = Util.readInputStream(input);
        }

        @Override
        protected void postResponse() {
            if (code == 0) {
                code = getResponseCode();
            }
            if (!reported) {
                reported = true;
                answer.got(this);
            }
        }

        @Override
        protected void handleException(Exception err) {
            if (!reported) {
                reported = true;
                failStep(getHttpMethod() + " " + getUrl() + " failed: " + err);
            }
        }
    }

    /// The flows here send credentials, read redirects and keep cookies, all of
    /// which belong to the browser on the browser port. Reports the test skipped
    /// there, and says whether it did.
    protected final boolean skippedOnBrowser() {
        if (!isBrowser()) {
            return false;
        }
        System.out.println("CN1SS:INFO:test=" + getClass().getSimpleName()
                + " status=SKIPPED reason=credentials-redirects-and-cookies-belong-to-the-browser");
        return true;
    }

    /// A client of the server's authorization endpoints, at the address the device uses.
    protected static OidcClient newClient(TokenStore store) {
        String base = baseUrl();
        return OidcClient.create(OidcConfiguration.newBuilder()
                        .issuer(base)
                        .authorizationEndpoint(base + "/oauth2/authorize")
                        .tokenEndpoint(base + "/oauth2/token")
                        .revocationEndpoint(base + "/oauth2/revoke")
                        .deviceAuthorizationEndpoint(base + "/oauth2/device_authorization")
                        .jwksUri(base + "/oauth2/jwks")
                        .build())
                .setClientId(CLIENT_ID)
                .setRedirectUri(REDIRECT)
                .setTokenStore(store);
    }

    /// The base URL the authorizer of these tests covers: the secured API and
    /// nothing else of the server, so the pet API and the probes the other backend
    /// tests use never see a token.
    protected static String securedBase() {
        return baseUrl() + "/api/secure";
    }

    /// Takes the authorizer of a test off the network again.
    protected static void uninstall(OidcRequestAuthorizer authorizer) {
        if (authorizer != null) {
            NetworkManager.getInstance().setAuthorizer(securedBase(), null);
        }
    }

    /// Signs [#USER] in for `scope` with an authorization code and PKCE, and hands
    /// the tokens over. Fails the step itself when any part of it goes wrong.
    protected final void signIn(final String scope, final SignedIn then) {
        final PkceChallenge pkce = PkceChallenge.generate();
        final String state = "s" + System.currentTimeMillis();
        String authorize = "/oauth2/authorize?response_type=code"
                + "&client_id=" + Util.encodeUrl(CLIENT_ID)
                + "&redirect_uri=" + Util.encodeUrl(REDIRECT)
                + "&scope=" + Util.encodeUrl(scope)
                + "&state=" + state
                + "&nonce=n" + state
                + "&code_challenge=" + Util.encodeUrl(pkce.getChallenge())
                + "&code_challenge_method=" + pkce.getMethod();
        new Call("GET", authorize).basic(USER, USER_PASSWORD).unfollowed()
                .send(new Answer() {
                    public void got(Call asked) {
                        if (!expect(asked.code == 302, "authorize answered " + asked.code + " "
                                + asked.text())) {
                            return;
                        }
                        String code = parameter(asked.location, "code");
                        if (!expect(asked.location != null && asked.location.startsWith(REDIRECT + "?")
                                && code != null, "authorize redirected to " + asked.location)
                                || !expect(state.equals(parameter(asked.location, "state")),
                                "the state came back as " + parameter(asked.location, "state"))) {
                            return;
                        }
                        new Call("POST", "/oauth2/token")
                                .form("grant_type", "authorization_code")
                                .form("code", code)
                                .form("redirect_uri", REDIRECT)
                                .form("code_verifier", pkce.getVerifier())
                                .form("client_id", CLIENT_ID)
                                .send(new Answer() {
                                    public void got(Call exchanged) {
                                        Map<String, Object> json = exchanged.json();
                                        if (expect(exchanged.code == 200 && json != null
                                                && json.get("access_token") != null,
                                                "the code exchange answered " + exchanged.code + " "
                                                + exchanged.text())) {
                                            then.got(OidcTokens.fromTokenResponse(json, null));
                                        }
                                    }
                                });
                    }
                });
    }

    /// A query parameter of a redirect, percent-decoded, or null.
    protected static String parameter(String url, String name) {
        if (url == null) {
            return null;
        }
        int query = url.indexOf('?');
        String[] pairs = Util.split(query < 0 ? "" : url.substring(query + 1), "&");
        for (int iter = 0; iter < pairs.length; iter++) {
            int eq = pairs[iter].indexOf('=');
            if (eq > 0 && name.equals(pairs[iter].substring(0, eq))) {
                return Util.decode(pairs[iter].substring(eq + 1), "UTF-8", false);
            }
        }
        return null;
    }

    /// A token nobody issued: the given one with its signature's last character changed.
    protected static String forged(String accessToken) {
        char last = accessToken.charAt(accessToken.length() - 1);
        return accessToken.substring(0, accessToken.length() - 1) + (last == 'A' ? 'B' : 'A');
    }
}
