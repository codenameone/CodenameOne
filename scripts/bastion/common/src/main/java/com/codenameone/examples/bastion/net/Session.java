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
package com.codenameone.examples.bastion.net;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.RequestAuthorizer;
import com.codename1.io.Util;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcConfiguration;
import com.codename1.io.oidc.OidcRequestAuthorizer;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.PkceChallenge;
import com.codename1.io.oidc.SecureStorageTokenStore;
import com.codename1.security.SecureStorage;
import com.codenameone.examples.bastion.security.SealedStorage;
import com.codename1.io.oidc.TokenStore;
import com.codename1.io.webauthn.PublicKeyCredential;
import com.codename1.io.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.io.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.io.webauthn.WebAuthnClient;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.bastion.AppConfig;
import com.codenameone.examples.bastion.api.UserDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/// Who is signed in, and the tokens that say so.
///
/// Signing in is OAuth's authorization-code flow with PKCE against the bank's
/// own server, made without a browser, as in the Savor sample: the app posts
/// the password for a short-lived session, exchanges the session for an
/// authorization code and the code for tokens. What Bastion adds:
///
/// - **A second factor.** An account with an authenticator app answers the
///   password with a redirect to `/login/mfa`: the session is held back until
///   the six-digit code is posted to it ([#signIn] answers a [Pending]).
/// - **Passkeys.** [#signInWithPasskey] runs the WebAuthn ceremony through
///   the platform's passkey UI ([WebAuthnClient]); a passkey whose
///   authenticator verified the user is both factors at once.
/// - **Short tokens.** The server issues five-minute access tokens and a
///   rotating refresh token; [OidcRequestAuthorizer] renews them, and keeps
///   the pair in the platform's secure storage (Keychain / Keystore), never in
///   plain preferences outside the simulator.
/// - **The same flow in a browser.** There the app sees neither the session
///   cookie nor a redirect: the browser keeps the first in its own jar and
///   follows the second. So in a browser every step is read from its answer
///   instead -- the server's JSON for the password, the code and the passkey,
///   and the authorization code from the page the redirect ends on
///   ([AppConfig#redirectUri]) -- and no `Cookie` header is ever written.
public final class Session {
    private static final String STORE_PREFIX = "bastion";
    private static final String SESSION_COOKIE = "CN1SESSION";

    private static String base;
    private static OidcClient client;
    private static OidcRequestAuthorizer authorizer;
    private static TokenStore store;
    private static UserDto user;
    private static Runnable onSignedOut;
    /// Moved on whenever the server changes or an account signs in or out:
    /// an answer from an earlier session is dropped, never installed.
    private static int session;
    /// A signed-in server session kept for one passkey ceremony.
    private static boolean keepForPasskey;
    private static String ceremonyServer;
    private static String ceremonyCookie;

    private Session() {
    }

    /// A password the server accepted, waiting for its second factor.
    public static final class Pending {
        final String server;
        final String cookie;

        Pending(String server, String cookie) {
            this.server = server;
            this.cookie = cookie;
        }
    }

    /// Points the app at a server.
    public static void start(String baseUrl) {
        if (store == null) {
            // Sealed under the app key (Keys), which a biometric or the PIN
            // opens: the refresh token is behind the same lock as the payment
            // key. Plain storage only in the simulator, which has no keystore.
            // On a device without one the tokens are not kept at all: the
            // user signs in at every start, which is the right failure for a
            // bank.
            store = new Marked(new SecureStorageTokenStore(
                    new SealedStorage(SecureStorage.getInstance()))
                    .allowPlainStorageFallback(CN.isSimulator()));
        }
        if (base != null) {
            NetworkManager.getInstance().setAuthorizer(base + "/api", null);
            if (!base.equals(baseUrl)) {
                store.clear(storeKey(base));
            }
        }
        base = baseUrl;
        user = null;
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
        authorizer.addSignInRequiredListener((source, reason) -> {
            user = null;
            session++;
            if (onSignedOut != null) {
                onSignedOut.run();
            }
        });
        Api.connect(base);
    }

    /// The name tokens from one server are kept under: one name per server,
    /// so a refresh token is never sent to a server that did not issue it.
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

    /// Says who is signed in without asking the server (the tests).
    public static void useUser(UserDto signedIn) {
        user = signedIn;
    }

    /// Keeps the tokens somewhere else (the tests keep them in memory).
    public static void useStore(TokenStore tokens) {
        store = tokens == null ? null : new Marked(tokens);
    }

    private static final String HELD = "bastion.session.";

    /// Notes, in the preferences, which keys hold a saved session, so the
    /// start can tell "signed in, behind the lock" from "never signed in"
    /// before the app key is open -- the tokens themselves cannot be read
    /// until then. Kept here, under the key the session itself uses: the
    /// account name the token store writes under is its own business, and
    /// guessing it is how every reload used to land on the sign-in form.
    static final class Marked implements TokenStore {
        private final TokenStore inner;

        Marked(TokenStore inner) {
            this.inner = inner;
        }

        @Override
        public AsyncResource<OidcTokens> load(final String key) {
            AsyncResource<OidcTokens> loaded = inner.load(key);
            loaded.ready(tokens -> {
                // Read with the key open and nothing there: the marker is out
                // of date (the store was wiped, or the sealing key replaced),
                // and keeping it would send every start to a lock that guards
                // nothing and then on to the sign-in form.
                if (tokens == null || tokens.getAccessToken() == null) {
                    Preferences.delete(HELD + key);
                }
            });
            return loaded;
        }

        @Override
        public AsyncResource<Boolean> save(final String key, OidcTokens tokens) {
            AsyncResource<Boolean> saved = inner.save(key, tokens);
            saved.ready(ok -> {
                if (ok != null && ok.booleanValue()) {
                    Preferences.set(HELD + key, true);
                }
            });
            return saved;
        }

        @Override
        public AsyncResource<Boolean> clear(String key) {
            Preferences.delete(HELD + key);
            return inner.clear(key);
        }
    }

    public static void onSignedOut(Runnable handler) {
        onSignedOut = handler;
    }

    public static String serverUrl() {
        return base;
    }

    public static UserDto user() {
        return user;
    }

    /// The session epoch: moves on at every sign-in, sign-out, server change
    /// and lock. A callback made in one epoch is dropped in another
    /// ([Net#to]), so an answer that arrives after the lock never paints an
    /// account screen.
    public static int epoch() {
        return session;
    }

    /// The app locked: the tokens are dropped from memory (not from the
    /// sealed store), which also abandons a renewal in progress, and the
    /// epoch moves on. [#restore] picks them up again after the unlock.
    ///
    /// A renewal that had already reached the server when the lock came is
    /// lost with its answer; the unlock then finds the stored refresh token
    /// spent and asks for a sign-in -- the safe failure, and a narrow one.
    public static void suspend() {
        endPasskeySession();
        user = null;
        session++;
        if (authorizer != null) {
            authorizer.setTokens(null);
        }
    }

    /// Seals the tokens in use again: after `Keys.claim` replaced the key
    /// they were sealed under.
    public static void resealTokens() {
        OidcTokens tokens = authorizer == null ? null : authorizer.getTokens();
        if (tokens != null && base != null) {
            store.save(storeKey(base), tokens);
        }
    }

    /// The signed-in account's name, or null.
    public static String username() {
        return user == null ? null : user.username();
    }

    /// Whether this device keeps a sealed session for the current server,
    /// readable once the app key is open.
    public static boolean hasStoredSession() {
        return base != null && hasStoredSession(base);
    }

    static boolean hasStoredSession(String server) {
        return Preferences.get(HELD + storeKey(server), false);
    }

    /// Whether a refresh token is held: what the lock screen protects.
    public static boolean hasTokens() {
        OidcTokens tokens = authorizer == null ? null : authorizer.getTokens();
        return tokens != null && tokens.getAccessToken() != null;
    }

    /// Picks up the session a previous run left, if it is still good.
    public static void restore(final Runnable signedIn, final Runnable signedOut,
            final Runnable unreachable) {
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

    public static void refreshUser(final Runnable done, final Net.Failed failed) {
        final int asked = session;
        Api.account().me(Net.to(described -> {
            if (asked != session) {
                return;
            }
            user = described;
            done.run();
        }, (status, message) -> {
            if (asked == session) {
                failed.failed(status, message);
            }
        }));
    }

    /// An e-mail address the way the server names its account: trimmed, ASCII
    /// letters folded by hand (`toLowerCase()` follows the device locale).
    public static String accountName(String email) {
        if (email == null) {
            return null;
        }
        return AppConfig.asciiLower(email.trim());
    }

    /// What a password sign-in to an account with a passkey is answered with.
    static final String PASSKEY_REQUIRED = "This account signs in with its passkey. "
            + "Use the passkey, or ask the bank to help you recover the account.";

    /// Signs in with a password. An account with an authenticator app is
    /// answered with `needsCode`, and [#submitCode] finishes it.
    public static void signIn(String email, String password, final Net.Ok<Pending> needsCode,
            final Runnable done, final Net.Failed failed) {
        final String server = base;
        final Step login = new Step("POST", server + "/login");
        login.addArgument("username", accountName(email));
        login.addArgument("password", password);
        login.send(() -> {
            if (!server.equals(base)) {
                failed.failed(0, "The server changed. Sign in again.");
            } else if (login.status == 0) {
                failed.failed(0, "Could not reach the server. Check your connection.");
            } else if (login.status == 429) {
                failed.failed(429, "Too many attempts. Wait a minute and try again.");
            } else if (login.status == 423) {
                failed.failed(423, "This account is locked for a while after too many attempts.");
            } else if (login.status == 403) {
                // The account has a passkey and no recovery codes to stand in
                // for it (SignInGuard on the server).
                failed.failed(403, PASSKEY_REQUIRED);
            } else if (web() ? login.status == 200 && !login.signedIn()
                    : login.session != null && login.status == 302 && login.location != null
                    && login.location.indexOf("/login/mfa") >= 0) {
                // In a browser the redirect to /login/mfa was followed: the
                // answer is that page, not the handler's `signedIn` JSON.
                needsCode.got(new Pending(server, login.session));
            } else if (login.status == 200 && (web() ? login.signedIn() : login.session != null)) {
                authorize(server, login.session, done, failed);
            } else {
                failed.failed(401, "That e-mail and password do not match an account.");
            }
        });
    }

    /// Posts the authenticator app's code (or a recovery code) for a password
    /// that is waiting for it.
    public static void submitCode(final Pending pending, String code, final Runnable done,
            final Net.Failed failed) {
        final Step mfa = new Step("POST", pending.server + "/login/mfa");
        mfa.cookie(pending.cookie);
        mfa.addArgument("code", code == null ? "" : code.trim());
        mfa.send(() -> {
            if (!pending.server.equals(base)) {
                failed.failed(0, "The server changed. Sign in again.");
            } else if (mfa.status == 0) {
                failed.failed(0, "Could not reach the server. Check your connection.");
            } else if (mfa.status == 429) {
                failed.failed(429, "Too many attempts. Wait a minute and try again.");
            } else if (mfa.status == 403) {
                // A passkey account: the app's code is not enough, a recovery
                // code is (SignInGuard#passkeyFirst on the server).
                failed.failed(403, "This account signs in with its passkey. Without it, enter "
                        + "one of your recovery codes.");
            } else if (mfa.status == 200 && (!web() || mfa.signedIn())) {
                // A wrong code is a redirect back to the code page, which a
                // browser follows to a 200 without `signedIn`.
                // The session id changes once the user is fully signed in.
                authorize(pending.server, mfa.session != null ? mfa.session : pending.cookie,
                        done, failed);
            } else {
                failed.failed(401, "That code is not right. Codes change every 30 seconds.");
            }
        });
    }

    /// Whether this platform can sign in with a passkey.
    public static boolean passkeysSupported() {
        return WebAuthnClient.isSupported();
    }

    /// Signs in with a passkey: the server's challenge, the platform's passkey
    /// sheet, and the assertion back to the server.
    public static void signInWithPasskey(final Runnable done, final Net.Failed failed) {
        final String server = base;
        final Step options = new Step("POST", server + "/webauthn/authenticate/options");
        options.json("{}");
        options.send(() -> {
            if (!server.equals(base) || options.status != 200
                    || (!web() && options.session == null)) {
                failed.failed(options.status, "Passkey sign-in is not available right now.");
                return;
            }
            PublicKeyCredentialRequestOptions request;
            try {
                request = PublicKeyCredentialRequestOptions.fromJson(options.text());
            } catch (RuntimeException malformed) {
                failed.failed(options.status, "Passkey sign-in is not available right now.");
                return;
            }
            WebAuthnClient.getInstance().get(request).ready(new SuccessCallback<PublicKeyCredential>() {
                @Override
                public void onSucess(PublicKeyCredential credential) {
                    final Step login = new Step("POST", server + "/login/webauthn");
                    login.cookie(options.session);
                    login.json(credential.toJson());
                    login.send(() -> {
                        if (!server.equals(base)) {
                            failed.failed(0, "The server changed. Sign in again.");
                        } else if (login.status == 200 && "true".equals(login.answer("authenticated"))) {
                            authorize(server, login.session != null ? login.session
                                    : options.session, done, failed);
                        } else {
                            failed.failed(401, "That passkey was not accepted.");
                        }
                    });
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable err) {
                    failed.failed(-1, "Passkey sign-in was cancelled.");
                }
            });
        });
    }

    /// The signed-in session, for an authorization code, for tokens.
    private static void authorize(final String server, final String cookie, final Runnable done,
            final Net.Failed failed) {
        final int asked = session;
        final PkceChallenge pkce = PkceChallenge.generate();
        final String state = PkceChallenge.generate().getVerifier();
        final Step authorize = new Step("GET", server + "/oauth2/authorize");
        authorize.cookie(cookie);
        if (web()) {
            // Or a session that did not take is answered with a page.
            authorize.addRequestHeader("Accept", "application/json");
        }
        authorize.addArgument("response_type", "code");
        authorize.addArgument("client_id", AppConfig.CLIENT_ID);
        authorize.addArgument("redirect_uri", AppConfig.redirectUri());
        authorize.addArgument("scope", "openid profile");
        authorize.addArgument("state", state);
        authorize.addArgument("code_challenge", pkce.getChallenge());
        authorize.addArgument("code_challenge_method", pkce.getMethod());
        authorize.send(() -> {
            // In a browser the redirect was followed to SignInCodePage, which
            // answers the code and state as JSON.
            String code = web() ? authorize.answer("code") : parameter(authorize.location, "code");
            String returned = web() ? authorize.answer("state")
                    : parameter(authorize.location, "state");
            // A code that comes back with another state was not issued for
            // this sign-in.
            if (!server.equals(base) || code == null || !state.equals(returned)) {
                failed.failed(authorize.status, "Signing in failed. Try again.");
                return;
            }
            final Step token = new Step("POST", server + "/oauth2/token");
            token.addArgument("grant_type", "authorization_code");
            token.addArgument("code", code);
            token.addArgument("redirect_uri", AppConfig.redirectUri());
            token.addArgument("code_verifier", pkce.getVerifier());
            token.addArgument("client_id", AppConfig.CLIENT_ID);
            token.send(() -> {
                OidcTokens tokens = token.status == 200 ? token.tokens() : null;
                if (!server.equals(base) || asked != session || tokens == null
                        || tokens.getAccessToken() == null) {
                    failed.failed(token.status, "Signing in failed. Try again.");
                    return;
                }
                // The server session has done its work; it is ended rather
                // than left to run out -- unless onboarding asked to keep it
                // for one passkey ceremony, which only a session can run.
                if (keepForPasskey) {
                    keepForPasskey = false;
                    ceremonyServer = server;
                    // In a browser the session is the browser's cookie; an
                    // empty marker says one is kept.
                    ceremonyCookie = cookie == null ? "" : cookie;
                } else {
                    endServerSession(server, cookie);
                }
                session++;
                authorizer.setTokens(tokens);
                store.save(storeKey(base), tokens);
                refreshUser(done, failed);
            });
        });
    }

    /// Asks the next sign-in to keep its server session for one passkey
    /// ceremony ([#addPasskey]). Passkeys are registered against the signed-in
    /// session cookie, not the API's bearer token, so onboarding asks for this
    /// before the sign-in that follows registration.
    public static void keepSessionForPasskey() {
        keepForPasskey = true;
    }

    /// Whether [#addPasskey] can run now: a kept session, on a platform with
    /// a passkey UI.
    public static boolean canAddPasskey() {
        return ceremonyCookie != null && base != null && base.equals(ceremonyServer)
                && passkeysSupported();
    }

    /// Creates a passkey for the signed-in account: the server's creation
    /// options, the platform's passkey sheet, and the new credential back to
    /// the server under `label`. The kept session is ended either way.
    public static void addPasskey(final String label, final Runnable done,
            final Net.Failed failed) {
        final String server = ceremonyServer;
        final String cookie = ceremonyCookie;
        if (!canAddPasskey()) {
            failed.failed(0, "Sign in again to add a passkey.");
            return;
        }
        final Step options = new Step("POST", server + "/webauthn/register/options");
        options.cookie(cookie);
        options.json("{}");
        options.send(() -> {
            PublicKeyCredentialCreationOptions create = null;
            if (options.status == 200) {
                try {
                    create = PublicKeyCredentialCreationOptions.fromJson(options.text());
                } catch (RuntimeException malformed) {
                    create = null;
                }
            }
            if (create == null || !server.equals(base)) {
                endPasskeySession();
                failed.failed(options.status, "Passkeys are not available right now.");
                return;
            }
            WebAuthnClient.getInstance().create(create).ready(new SuccessCallback<PublicKeyCredential>() {
                @Override
                public void onSucess(PublicKeyCredential credential) {
                    final Step register = new Step("POST", server + "/webauthn/register");
                    register.cookie(cookie);
                    register.json("{\"publicKey\":{\"credential\":" + credential.toJson()
                            + ",\"label\":\"" + jsonText(label) + "\"}}");
                    register.send(() -> {
                        endPasskeySession();
                        if (register.status == 200 && "true".equals(register.answer("success"))) {
                            done.run();
                        } else {
                            failed.failed(register.status, "That passkey was not saved.");
                        }
                    });
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable err) {
                    endPasskeySession();
                    failed.failed(-1, "Creating the passkey was cancelled.");
                }
            });
        });
    }

    /// Ends a session kept for [#addPasskey] that was not used.
    public static void endPasskeySession() {
        keepForPasskey = false;
        if (ceremonyCookie != null) {
            endServerSession(ceremonyServer, ceremonyCookie);
        }
        ceremonyCookie = null;
        ceremonyServer = null;
    }

    private static void endServerSession(String server, String cookie) {
        Step end = new Step("POST", server + "/logout");
        end.cookie(cookie);
        end.send(() -> { });
    }

    /// How the current sign-in was made, as the server wrote it into the
    /// access token's `amr` claim (`pwd`, `otp`, `hwk`, `mfa`). The token is
    /// the server's word and is checked there; this reads it only to know
    /// what the server will ask -- a passkey sign-in adds a device without
    /// the password.
    public static boolean signedInWith(String method) {
        OidcTokens tokens = authorizer == null ? null : authorizer.getTokens();
        String token = tokens == null ? null : tokens.getAccessToken();
        int first = token == null ? -1 : token.indexOf('.');
        int second = first < 0 ? -1 : token.indexOf('.', first + 1);
        if (second < 0) {
            return false;
        }
        try {
            byte[] payload = com.codename1.util.Base64.decodeUrlSafe(
                    token.substring(first + 1, second));
            if (payload == null) {
                return false;
            }
            Map<String, Object> claims = new JSONParser().parseJSON(new InputStreamReader(
                    new ByteArrayInputStream(payload), "UTF-8"));
            Object amr = claims == null ? null : claims.get("amr");
            return amr instanceof java.util.List && ((java.util.List) amr).contains(method);
        } catch (IOException malformed) {
            return false;
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    /// Whether the app runs in a browser.
    static boolean web() {
        return AppConfig.pageOrigin() != null;
    }

    /// `text` as the inside of a JSON string.
    static String jsonText(String text) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// Signs out: the refresh token is revoked at the server and the tokens
    /// are wiped here, whether or not the server answers.
    public static void signOut(final Runnable done) {
        endPasskeySession();
        OidcTokens tokens = authorizer.getTokens();
        if (tokens != null && tokens.getRefreshToken() != null) {
            client.revoke(tokens.getRefreshToken());
        }
        user = null;
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

    /// One request of the sign-in: no redirects followed, no cookie jar, no
    /// bearer token -- the redirect and the cookie are the answers it is after.
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

        /// The server session to send: a `Cookie` header outside a browser.
        /// In one the browser sends its own cookie and a page may not set
        /// that header, so nothing is written.
        void cookie(String value) {
            if (!web() && value != null && value.length() > 0) {
                addRequestHeader("Cookie", value);
            }
        }

        /// Whether the answer is the sign-in handlers' `{"signedIn":true}`.
        boolean signedIn() {
            return status == 200 && "true".equals(answer("signedIn"));
        }

        void json(String text) {
            setContentType("application/json");
            addRequestHeader("Accept", "application/json");
            setRequestBody(text);
        }

        void send(Runnable next) {
            then = next;
            NetworkManager.getInstance().addToQueue(this);
        }

        String text() {
            try {
                return body == null ? "" : new String(body, "UTF-8");
            } catch (IOException impossible) {
                return "";
            }
        }

        /// A member of the JSON this request was answered with, as text, or
        /// null.
        String answer(String name) {
            Map<String, Object> json = parse();
            Object value = json == null ? null : json.get(name);
            return value == null ? null : String.valueOf(value);
        }

        OidcTokens tokens() {
            Map<String, Object> json = parse();
            return json == null ? null : OidcTokens.fromTokenResponse(json, null);
        }

        private Map<String, Object> parse() {
            if (body == null) {
                return null;
            }
            try {
                return new JSONParser().parseJSON(new InputStreamReader(
                        new ByteArrayInputStream(body), "UTF-8"));
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
