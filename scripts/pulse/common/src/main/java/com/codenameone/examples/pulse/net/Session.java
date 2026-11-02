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
package com.codenameone.examples.pulse.net;

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
import com.codenameone.examples.pulse.AppConfig;
import com.codenameone.examples.pulse.Prefs;
import com.codenameone.examples.pulse.api.UserDto;

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
    private static final String STORE_PREFIX = "pulse";
    private static final String SESSION_COOKIE = "CN1SESSION";

    private static String base;
    private static OidcClient client;
    private static OidcRequestAuthorizer authorizer;
    private static TokenStore store;
    private static UserDto user;
    private static Runnable onSignedOut;
    /// Counts the sessions: moved on whenever the server changes, an account
    /// signs in, or the session ends. An answer to a request made in an
    /// earlier session describes somebody who is no longer signed in here --
    /// another server's account, or the account before a sign-out on this
    /// one -- and is dropped rather than installed for whoever is now.
    private static int session;

    private Session() {
    }

    /// Points the app at a server. Called at start-up, and again when the
    /// server address is changed.
    public static void start(String baseUrl) {
        if (store == null) {
            // Falls back to ordinary storage where the platform has no keystore
            // to offer, which is the simulator.
            store = new SecureStorageTokenStore().allowPlainStorageFallback(true);
        }
        if (base != null) {
            NetworkManager.getInstance().setAuthorizer(base + "/api", null);
            if (!base.equals(baseUrl)) {
                // Changing the server ends the session with the old one, and
                // its tokens go with it.
                store.clear(storeKey(base));
            }
        }
        base = baseUrl;
        setUser(null);
        session++;
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
                .setStoreKey(storeKey(base));
        authorizer = new OidcRequestAuthorizer(client);
        authorizer.install(base + "/api");
        // Not tied to this start(). A request to an earlier server still in
        // flight when the app switched, answered 401 with its refresh refused,
        // signs the new server out too: one more sign-in, after a server
        // switch that already asked for one, and nothing of either account is
        // exposed by it.
        authorizer.addSignInRequiredListener((source, reason) -> {
            setUser(null);
            session++;
            if (onSignedOut != null) {
                onSignedOut.run();
            }
        });
        Api.connect(base);
    }

    /// Where the tokens issued by one server are kept: under a name made from
    /// that server's address.
    ///
    /// One name for every server would hand the tokens of the last one to
    /// whichever address is typed next -- the app would restore them and send
    /// them, refresh token included, to a server that never issued them.
    /// Every character that is not a letter or a digit is written as its code,
    /// so two addresses never share a name and the name is one any platform's
    /// storage accepts.
    static String storeKey(String server) {
        StringBuilder key = new StringBuilder(STORE_PREFIX).append('-');
        for (int iter = 0; iter < server.length(); iter++) {
            char c = server.charAt(iter);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                key.append(c);
            } else {
                key.append('_').append(Integer.toHexString(c)).append('_');
            }
        }
        return key.toString();
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
        if (described != null && described.username() != null) {
            // What the device keeps about one person is not the next one's.
            Prefs.ownedBy(owner(described.username()));
        }
    }

    /// Who what the device keeps belongs to: the signed-in account, or null.
    /// See [#owner(String)].
    public static String owner() {
        return user == null || user.username() == null ? null : owner(user.username());
    }

    /// The device's name for the account `username` on the server in use. A
    /// name is unique on one server only: the same e-mail address signed in
    /// to two servers is two accounts, and what one left -- recent places,
    /// a change it has not sent yet -- is not the other's to see or send.
    public static String owner(String username) {
        return storeKey(base == null ? "" : base) + "/" + accountName(username);
    }

    /// Picks up the session a previous run left, if it is still good.
    ///
    /// @param signedIn there is a session, and [#user] is set
    /// @param signedOut there is none
    /// @param unreachable there is one, but the server did not answer
    public static void restore(final Runnable signedIn, final Runnable signedOut,
            final Runnable unreachable) {
        // Loading the tokens can outlast the session it was asked in -- the
        // server is changed while it reads -- and then neither answer is this
        // session's to act on: see [#session].
        final int asked = session;
        authorizer.load().ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens tokens) {
                if (asked != session) {
                    return;
                }
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
                if (asked == session) {
                    signedOut.run();
                }
            }
        });
    }

    /// Asks the server who this is. The roles in the answer decide which modes
    /// the app offers, so it is asked again whenever that could have changed.
    public static void refreshUser(final Runnable done, Net.Failed failed) {
        // An answer from a session that has since ended -- the server was
        // changed while an approval poll waited, or the account signed out
        // and another signed in -- describes that account, not this one;
        // it is dropped. See [#session].
        final int asked = session;
        Api.account().me(Net.to(described -> {
            if (asked != session) {
                return;
            }
            setUser(described);
            done.run();
        }, (status, message) -> {
            if (asked == session) {
                failed.failed(status, message);
            }
        }));
    }

    /// Signs in with an e-mail address and password.
    /// An e-mail address the way the server names its account: trimmed, with
    /// its ASCII letters in lower case (`Accounts.email` on the server). Signing
    /// in as `Courier@Pulse.example` must reach, and record its mode under, the
    /// same account as `courier@pulse.example`. Folded by hand: `toLowerCase()`
    /// follows the device locale, and under Turkish the `I` would not fold to `i`.
    public static String accountName(String email) {
        if (email == null) {
            return null;
        }
        char[] chars = email.trim().toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    public static void signIn(String email, String password, final Runnable done,
            final Net.Failed failed) {
        // The server can be changed while the sign-in is under way, and
        // start() then puts a new authorizer in place. Tokens the old server
        // issued are dropped rather than installed in the new one, which would
        // send them -- the refresh token too -- to a server that never issued
        // them. See [#session].
        final int asked = session;
        authenticate(email, password, tokens -> {
            if (asked != session) {
                return;
            }
            session++;
            authorizer.setTokens(tokens);
            store.save(storeKey(base), tokens);
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

        // Every step goes to the server the sign-in started with, and none goes
        // anywhere once the user has changed server: the session cookie of
        // step 1 and the verifier of step 3 are only for the first server, and
        // a new one that received them could replay them there for tokens.
        final String server = base;
        // 1. The password, for a session.
        final Step login = new Step("POST", server + "/login");
        login.addArgument("username", accountName(email));
        login.addArgument("password", password);
        login.send(() -> {
            if (!server.equals(base)) {
                failed.failed(0, "The server changed. Sign in again.");
                return;
            }
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
            final Step authorize = new Step("GET", server + "/oauth2/authorize");
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
                if (!server.equals(base)) {
                    failed.failed(0, "The server changed. Sign in again.");
                    return;
                }
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
                final Step token = new Step("POST", server + "/oauth2/token");
                token.addArgument("grant_type", "authorization_code");
                token.addArgument("code", code);
                token.addArgument("redirect_uri", redirect);
                token.addArgument("code_verifier", pkce.getVerifier());
                token.addArgument("client_id", AppConfig.CLIENT_ID);
                token.send(() -> {
                    if (!server.equals(base)) {
                        failed.failed(0, "The server changed. Sign in again.");
                        return;
                    }
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
                    Step end = new Step("POST", server + "/logout");
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
            // Sent and not waited for. Signing out is something the user does
            // to this phone, and it is done here whether or not the server
            // answers: the tokens are gone from the device either way. A
            // revocation that is lost leaves a refresh token only someone who
            // already copied it off the phone could use, and it expires on
            // its own; holding the sign-out until the network allows would
            // trap the user signed in on a phone with no connection.
            client.revoke(tokens.getRefreshToken());
        }
        setUser(null);
        session++;
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
