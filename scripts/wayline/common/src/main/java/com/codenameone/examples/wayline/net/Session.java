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
package com.codenameone.examples.wayline.net;

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
import com.codename1.io.oidc.SecureStorageTokenStore;
import com.codename1.io.oidc.TokenStore;
import com.codename1.ui.CN;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.wayline.AppConfig;
import com.codenameone.examples.wayline.api.UserDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/// Who is signed in, and the tokens that say so.
///
/// Signing in is OAuth's authorization-code flow with PKCE against the app's
/// own server, made without a browser: the app shows its own sign-in screen,
/// posts the password to the server for a short-lived session, exchanges the
/// session for an authorization code and the code for tokens. The password is
/// sent once, to the server that owns it, and is never stored; what is stored
/// is the token pair, in the device's secure storage.
///
/// From then on [OidcRequestAuthorizer] does the work: it puts the access token
/// on every request under `/api`, renews it shortly before it expires, and
/// renews and retries when the server refuses one. When renewing fails -- the
/// refresh token ran out, or the account was suspended -- it says so, and the
/// app goes back to the welcome screen.
public final class Session {
    private static final String STORE_KEY = "wayline";
    private static final String SESSION_COOKIE = "CN1SESSION";

    private static String base;
    private static OidcClient client;
    private static OidcRequestAuthorizer authorizer;
    private static TokenStore store;
    private static UserDto user;
    private static Runnable onSignedOut;

    private Session() {
    }

    /// Points the app at a server. Called at start-up, and again when the
    /// server address is changed.
    public static void start(String baseUrl) {
        if (base != null) {
            NetworkManager.getInstance().setAuthorizer(base + "/api", null);
        }
        base = baseUrl;
        user = null;
        if (store == null) {
            // Falls back to ordinary storage where the platform has no keystore
            // to offer, which is the simulator.
            store = new SecureStorageTokenStore().allowPlainStorageFallback(true);
        }
        client = OidcClient.create(OidcConfiguration.newBuilder()
                        .issuer(base)
                        .authorizationEndpoint(base + "/oauth2/authorize")
                        .tokenEndpoint(base + "/oauth2/token")
                        .revocationEndpoint(base + "/oauth2/revoke")
                        .jwksUri(base + "/oauth2/jwks")
                        .build())
                .setClientId(AppConfig.CLIENT_ID)
                .setRedirectUri(AppConfig.redirectUri())
                .setScopes("openid", "profile")
                .setTokenStore(store)
                .setStoreKey(STORE_KEY);
        authorizer = new OidcRequestAuthorizer(client);
        authorizer.install(base + "/api");
        authorizer.addSignInRequiredListener((source, reason) -> {
            user = null;
            if (onSignedOut != null) {
                onSignedOut.run();
            }
        });
        Api.connect(base);
    }

    /// Keeps the tokens somewhere else. The tests keep them in memory, so one
    /// run leaves nothing behind for the next.
    public static void useStore(TokenStore tokens) {
        store = tokens;
    }

    /// What to do when the session ends without the user asking: go back to
    /// the welcome screen.
    public static void onSignedOut(Runnable handler) {
        onSignedOut = handler;
    }

    public static String serverUrl() {
        return base;
    }

    /// The signed-in user as the server last described them, or null.
    public static UserDto user() {
        return user;
    }

    public static void setUser(UserDto described) {
        user = described;
    }

    /// Picks up the session a previous run left, if it is still good.
    ///
    /// @param signedIn there is a session, and [#user] is set
    /// @param signedOut there is none
    /// @param unreachable there is one, but the server did not answer
    public static void restore(final Runnable signedIn, final Runnable signedOut,
            final Runnable unreachable) {
        authorizer.load().ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens tokens) {
                if (tokens == null || tokens.getAccessToken() == null) {
                    signedOut.run();
                    return;
                }
                refreshUser(signedIn, (status, message) -> {
                    if (status == 0) {
                        unreachable.run();
                    } else {
                        signedOut.run();
                    }
                });
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                signedOut.run();
            }
        });
    }

    /// Asks the server who this is. The roles in the answer decide which modes
    /// the app offers, so it is asked again whenever that could have changed.
    public static void refreshUser(final Runnable done, Net.Failed failed) {
        Api.account().me(Net.to(described -> {
            user = described;
            done.run();
        }, failed));
    }

    /// Signs in with an e-mail address and password.
    public static void signIn(String email, String password, final Runnable done,
            final Net.Failed failed) {
        authenticate(email, password, tokens -> {
            authorizer.setTokens(tokens);
            store.save(STORE_KEY, tokens);
            refreshUser(done, failed);
        }, failed);
    }

    /// Exchanges an e-mail address and password for tokens, and does nothing
    /// with them: [#signIn] is this, and then keeping what comes back.
    public static void authenticate(final String email, final String password,
            final Net.Ok<OidcTokens> got, final Net.Failed failed) {
        final PkceChallenge pkce = PkceChallenge.generate();
        // Any unguessable value does for the state; this is one.
        final String state = PkceChallenge.generate().getVerifier();
        // In a browser the app sees neither the session cookie nor a redirect:
        // the browser keeps the first and follows the second. So there the
        // password's answer is its status, and the code is read from the page
        // the redirect ends on -- see AppConfig.redirectUri().
        final boolean web = AppConfig.pageOrigin() != null;
        final String redirect = AppConfig.redirectUri();

        // 1. The password, for a session.
        final Step login = new Step("POST", base + "/login");
        login.addArgument("username", email);
        login.addArgument("password", password);
        login.send(() -> {
            if (login.status == 0) {
                failed.failed(0, "Could not reach the server. Check your connection.");
                return;
            }
            if (login.status == 429) {
                failed.failed(429, "Too many attempts. Wait a minute and try again.");
                return;
            }
            if (web ? login.status != 200 : login.session == null) {
                failed.failed(401, "That e-mail and password do not match an account.");
                return;
            }
            // 2. The session, for an authorization code.
            final Step authorize = new Step("GET", base + "/oauth2/authorize");
            if (web) {
                // Or a session that did not take is answered with a page.
                authorize.addRequestHeader("Accept", "application/json");
            } else {
                authorize.addRequestHeader("Cookie", login.session);
            }
            authorize.addArgument("response_type", "code");
            authorize.addArgument("client_id", AppConfig.CLIENT_ID);
            authorize.addArgument("redirect_uri", redirect);
            authorize.addArgument("scope", "openid profile");
            authorize.addArgument("state", state);
            authorize.addArgument("code_challenge", pkce.getChallenge());
            authorize.addArgument("code_challenge_method", pkce.getMethod());
            authorize.send(() -> {
                String code = web ? authorize.answer("code")
                        : parameter(authorize.location, "code");
                String returned = web ? authorize.answer("state")
                        : parameter(authorize.location, "state");
                // The state has to come back as it went: a code that arrives
                // with another was not issued for this sign-in.
                if (code == null || !state.equals(returned)) {
                    failed.failed(authorize.status, "Signing in failed. Try again.");
                    return;
                }
                // 3. The code, for tokens.
                final Step token = new Step("POST", base + "/oauth2/token");
                token.addArgument("grant_type", "authorization_code");
                token.addArgument("code", code);
                token.addArgument("redirect_uri", redirect);
                token.addArgument("code_verifier", pkce.getVerifier());
                token.addArgument("client_id", AppConfig.CLIENT_ID);
                token.send(() -> {
                    OidcTokens tokens = token.status == 200 ? token.tokens() : null;
                    if (tokens == null || tokens.getAccessToken() == null) {
                        failed.failed(token.status, "Signing in failed. Try again.");
                        return;
                    }
                    // 4. The session has done its work, and is ended rather
                    // than left to run out: in a browser it is a cookie that
                    // would go on signing this user in to whoever sat down
                    // next. Nothing waits on the answer; the tokens stand
                    // without it.
                    Step end = new Step("POST", base + "/logout");
                    if (!web) {
                        end.addRequestHeader("Cookie", login.session);
                    }
                    end.send(() -> { });
                    got.got(tokens);
                });
            });
        });
    }

    /// Signs out: the refresh token is revoked at the server, so the session
    /// is over there too and not only forgotten here.
    public static void signOut(final Runnable done) {
        OidcTokens tokens = authorizer.getTokens();
        if (tokens != null && tokens.getRefreshToken() != null) {
            client.revoke(tokens.getRefreshToken());
        }
        user = null;
        authorizer.signOut().ready(new SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean cleared) {
                done.run();
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                done.run();
            }
        });
    }

    /// A parameter of the query string of `url`, or null.
    static String parameter(String url, String name) {
        int query = url == null ? -1 : url.indexOf('?');
        if (query < 0) {
            return null;
        }
        int at = query + 1;
        while (at < url.length()) {
            int end = url.indexOf('&', at);
            if (end < 0) {
                end = url.length();
            }
            if (url.startsWith(name + "=", at)) {
                return Util.decode(url.substring(at + name.length() + 1, end), "UTF-8", false);
            }
            at = end + 1;
        }
        return null;
    }

    /// One request of the sign-in. It follows no redirect and keeps no cookie
    /// jar, because the redirect and the cookie are the answers it is after;
    /// and it carries no bearer token, because there is not one yet.
    private static final class Step extends ConnectionRequest {
        int status;
        String location;
        String session;
        private byte[] body;
        private Runnable then;
        private boolean reported;

        Step(String method, String url) {
            setUrl(url);
            setHttpMethod(method);
            setPost(!"GET".equals(method));
            setFollowRedirects(false);
            setCookiesEnabled(false);
            setReadResponseForErrors(true);
            setDuplicateSupported(true);
            setFailSilently(true);
            setAuthorizer(RequestAuthorizer.NONE);
        }

        void send(Runnable next) {
            then = next;
            NetworkManager.getInstance().addToQueue(this);
        }

        /// A text member of the JSON this request was answered with, or null.
        String answer(String name) {
            if (status != 200 || body == null) {
                return null;
            }
            try {
                Map<String, Object> json = new JSONParser().parseJSON(new InputStreamReader(
                        new ByteArrayInputStream(body), "UTF-8"));
                Object value = json == null ? null : json.get(name);
                return value instanceof String ? (String) value : null;
            } catch (IOException malformed) {
                return null;
            } catch (RuntimeException malformed) {
                return null;
            }
        }

        OidcTokens tokens() {
            try {
                Map<String, Object> json = new JSONParser().parseJSON(new InputStreamReader(
                        new ByteArrayInputStream(body == null ? new byte[0] : body), "UTF-8"));
                return OidcTokens.fromTokenResponse(json, null);
            } catch (IOException malformed) {
                return null;
            } catch (RuntimeException malformed) {
                return null;
            }
        }

        @Override
        protected void readHeaders(Object connection) throws IOException {
            location = getHeader(connection, "Location");
            String[] cookies = getHeaders(connection, "Set-Cookie");
            for (int iter = 0; cookies != null && iter < cookies.length; iter++) {
                if (cookies[iter] != null && cookies[iter].startsWith(SESSION_COOKIE + "=")) {
                    int end = cookies[iter].indexOf(';');
                    session = end < 0 ? cookies[iter] : cookies[iter].substring(0, end);
                }
            }
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            status = code;
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            if (status == 0) {
                status = getResponseCode();
            }
            body = Util.readInputStream(input);
        }

        @Override
        protected void postResponse() {
            if (status == 0) {
                status = getResponseCode();
            }
            report();
        }

        @Override
        protected void handleException(Exception err) {
            status = 0;
            report();
        }

        private void report() {
            if (!reported) {
                reported = true;
                CN.callSerially(then);
            }
        }
    }
}
