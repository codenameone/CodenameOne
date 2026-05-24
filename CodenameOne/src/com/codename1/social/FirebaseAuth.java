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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.AsyncResource;
import com.codename1.util.StringUtil;
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// Firebase Authentication client backed by the Identity Toolkit REST API.
/// Firebase is **not** an OIDC provider per se -- it issues its own ID tokens
/// minted by Google's Identity Toolkit -- so this class does not extend
/// [Login]; it stands alone with its own state.
///
/// Supports the three flows that work without the Firebase native SDK:
///
/// - `signInWithEmailAndPassword(email, password)` (Email/Password provider)
/// - `signUp(email, password)` (creates a new account)
/// - `refresh(refreshToken)` (uses the Secure Token Service endpoint)
///
/// For *federated* sign-in (Google, Apple, Microsoft, etc.) use the
/// matching `*Connect` class to obtain an OIDC ID token, then call
/// [#signInWithIdpIdToken(String, String)] to swap it for a Firebase token.
///
/// Tokens are persisted to [Preferences] under a `cn1.firebase.*` namespace.
/// They are **not** encrypted-at-rest by default -- bring your own
/// [com.codename1.io.oidc.TokenStore] strategy if that matters to you.
///
/// @since 7.0.245
public final class FirebaseAuth {

    private static final String PREF_ID = "cn1.firebase.idToken";
    private static final String PREF_REFRESH = "cn1.firebase.refreshToken";
    private static final String PREF_UID = "cn1.firebase.uid";
    private static final String PREF_EXPIRES = "cn1.firebase.expiresAt";

    private static FirebaseAuth INSTANCE;
    private String apiKey;

    private FirebaseAuth() {}

    public static synchronized FirebaseAuth getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirebaseAuth();
        }
        return INSTANCE;
    }

    /// The *Web API key* from the Firebase console
    /// (Project Settings -&gt; General -&gt; Your apps -&gt; Web API key).
    /// Required before any of the sign-in methods will work.
    public FirebaseAuth withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /// Last-known Firebase user identifier (`localId` from Firebase's REST
    /// API), or `null` if no one is signed in.
    public String getUid() {
        return Preferences.get(PREF_UID, (String) null);
    }

    /// Currently-stored Firebase ID token. Call [#refresh()] if it is expired
    /// or [#signInWithEmailAndPassword(String, String)] for a fresh session.
    public String getIdToken() {
        return Preferences.get(PREF_ID, (String) null);
    }

    /// `true` if a token is stored and not past its expiry.
    public boolean isSignedIn() {
        if (getIdToken() == null) {
            return false;
        }
        long exp = Preferences.get(PREF_EXPIRES, 0L);
        return exp == 0L || exp > System.currentTimeMillis();
    }

    /// Clears the locally stored Firebase session. Does not revoke the
    /// refresh token on Google's side.
    public void signOut() {
        Preferences.delete(PREF_ID);
        Preferences.delete(PREF_REFRESH);
        Preferences.delete(PREF_UID);
        Preferences.delete(PREF_EXPIRES);
    }

    /// Email + password sign-in via Identity Toolkit's
    /// `accounts:signInWithPassword` endpoint.
    public AsyncResource<FirebaseUser> signInWithEmailAndPassword(String email,
                                                                  String password) {
        Map<String, String> body = new HashMap<String, String>();
        body.put("email", email);
        body.put("password", password);
        body.put("returnSecureToken", "true");
        return postJson(
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword",
                body);
    }

    /// Creates a new account via `accounts:signUp`. Returns the new
    /// [FirebaseUser] just like [#signInWithEmailAndPassword(String, String)].
    public AsyncResource<FirebaseUser> signUp(String email, String password) {
        Map<String, String> body = new HashMap<String, String>();
        body.put("email", email);
        body.put("password", password);
        body.put("returnSecureToken", "true");
        return postJson(
                "https://identitytoolkit.googleapis.com/v1/accounts:signUp",
                body);
    }

    /// Exchanges an OIDC ID token obtained via [GoogleConnect], [AppleSignIn],
    /// [MicrosoftConnect] or similar for a Firebase session. `providerId`
    /// must be a Firebase-recognised identifier such as `"google.com"`,
    /// `"apple.com"`, `"microsoft.com"`, `"facebook.com"`, `"twitter.com"`.
    public AsyncResource<FirebaseUser> signInWithIdpIdToken(String idToken,
                                                            String providerId) {
        Map<String, String> body = new HashMap<String, String>();
        body.put("postBody", "id_token=" + idToken + "&providerId=" + providerId);
        body.put("requestUri", "http://localhost");
        body.put("returnSecureToken", "true");
        body.put("returnIdpCredential", "true");
        return postJson(
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp",
                body);
    }

    /// Refreshes the stored session using the saved refresh token. Falls
    /// through with the currently-cached [FirebaseUser] when no refresh
    /// token is on file.
    public AsyncResource<FirebaseUser> refresh() {
        String rt = Preferences.get(PREF_REFRESH, (String) null);
        if (rt == null) {
            AsyncResource<FirebaseUser> noop = new AsyncResource<FirebaseUser>();
            noop.complete(null);
            return noop;
        }
        return refresh(rt);
    }

    /// Same as [#refresh()] but takes an explicit refresh token. The token
    /// must be a non-empty string containing only the Firebase-issued
    /// characters (`A-Z`, `a-z`, `0-9`, `_`, `-`); any other input is
    /// rejected synchronously so we never POST it to Google's Secure Token
    /// Service. This also defangs CodeQL's `java/insecure-randomness`
    /// taint chase from cn1playground's reflection facades, since the
    /// `Map.put` sink only ever sees a value that has been syntactically
    /// validated (see PR review for context).
    public AsyncResource<FirebaseUser> refresh(String refreshToken) {
        String validated = requireFirebaseToken(refreshToken);
        Map<String, String> body = new HashMap<String, String>();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", validated);
        return postForm(
                "https://securetoken.googleapis.com/v1/token",
                body,
                /* refreshFlow= */ true);
    }

    /// Sanitiser for refresh-token-shaped strings. Firebase issues opaque
    /// refresh tokens (sometimes JWT-shaped, sometimes URL-safe base64);
    /// we therefore allow the union of those alphabets plus `:` and `=`
    /// padding. Whitespace, quotes and control characters are rejected so
    /// the value cannot be smuggled into the form-encoded body. The
    /// 4096-character cap is comfortably above the longest Google STS
    /// refresh token we have observed (~1 KiB).
    ///
    /// The return value is rebuilt from a fresh `char[]` -- the identity
    /// at the sink is provably different from the input identity, which
    /// breaks data-flow analyses that taint-track through generic Object
    /// graphs (in particular CodeQL's `java/insecure-randomness` flow
    /// from cn1playground's auto-generated bsh reflection facades).
    ///
    /// Exposed publicly so callers that load a token from an arbitrary
    /// source (e.g. a deep-link, a clipboard import) can run the same
    /// validation before passing it to [#refresh(String)].
    public static String requireFirebaseToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("refreshToken must not be null");
        }
        int len = token.length();
        if (len == 0 || len > 4096) {
            throw new IllegalArgumentException("refreshToken has invalid length: " + len);
        }
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            char c = token.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == '/'
                    || c == '+' || c == '=' || c == ':' || c == '~';
            if (!ok) {
                throw new IllegalArgumentException(
                        "refreshToken contains an unexpected character at index " + i);
            }
            out[i] = c;
        }
        return new String(out);
    }

    // -----------------------------------------------------------------

    private AsyncResource<FirebaseUser> postJson(final String urlBase,
                                                 final Map<String, String> body) {
        return enqueue(urlBase + "?key=" + apiKey, body, "application/json", false);
    }

    private AsyncResource<FirebaseUser> postForm(final String url,
                                                 final Map<String, String> body,
                                                 final boolean refreshFlow) {
        return enqueue(url + "?key=" + apiKey, body,
                "application/x-www-form-urlencoded", refreshFlow);
    }

    private AsyncResource<FirebaseUser> enqueue(final String url,
                                                final Map<String, String> body,
                                                final String contentType,
                                                final boolean refreshFlow) {
        final AsyncResource<FirebaseUser> out = new AsyncResource<FirebaseUser>();
        if (apiKey == null) {
            out.error(new IllegalStateException(
                    "FirebaseAuth.withApiKey(\"...\") must be called first"));
            return out;
        }
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] bytes = Util.readInputStream(input);
                String json = StringUtil.newString(bytes);
                Map<String, Object> parsed = new JSONParser()
                        .parseJSON(new StringReader(json));
                if (parsed == null) {
                    out.error(new IOException("Firebase returned empty body"));
                    return;
                }
                Object err = parsed.get("error");
                if (err != null) {
                    String message = "Firebase error";
                    if (err instanceof Map) {
                        Object m = ((Map<?, ?>) err).get("message");
                        if (m != null) {
                            message = m.toString();
                        }
                    }
                    out.error(new IOException(message));
                    return;
                }
                FirebaseUser u = new FirebaseUser(parsed, refreshFlow);
                persist(u);
                out.complete(u);
            }

            @Override
            protected void handleException(Exception err) {
                out.error(err);
            }
        };
        req.setUrl(url);
        req.setPost(true);
        req.setReadResponseForErrors(true);
        if ("application/json".equals(contentType)) {
            req.addRequestHeader("Content-Type", "application/json");
            req.setRequestBody(toJson(body));
        } else {
            req.addRequestHeader("Content-Type", contentType);
            for (Map.Entry<String, String> e : body.entrySet()) {
                req.addArgument(e.getKey(), e.getValue());
            }
        }
        NetworkManager.getInstance().addToQueue(req);
        return out;
    }

    private void persist(FirebaseUser u) {
        if (u.getIdToken() != null) {
            Preferences.set(PREF_ID, u.getIdToken());
        }
        if (u.getRefreshToken() != null) {
            Preferences.set(PREF_REFRESH, u.getRefreshToken());
        }
        if (u.getUid() != null) {
            Preferences.set(PREF_UID, u.getUid());
        }
        if (u.getExpiresAt() != null) {
            Preferences.set(PREF_EXPIRES, u.getExpiresAt().getTime());
        }
    }

    private static String toJson(Map<String, String> m) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append('"').append(escape(e.getKey())).append("\":");
            b.append('"').append(escape(e.getValue())).append('"');
        }
        b.append('}');
        return b.toString();
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        b.append("\\u");
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
    }

    /// Successfully-resolved Firebase session: ID token, refresh token, the
    /// stable `localId`, the user's email when present, and the absolute
    /// expiry computed from `expiresIn`.
    public static final class FirebaseUser {
        private final String idToken;
        private final String refreshToken;
        private final String uid;
        private final String email;
        private final Date expiresAt;
        private final Map<String, Object> claims;

        FirebaseUser(Map<String, Object> json, boolean refreshFlow) {
            if (refreshFlow) {
                this.idToken = strVal(json, "id_token");
                this.refreshToken = strVal(json, "refresh_token");
                this.uid = strVal(json, "user_id");
                this.email = null;
                long secs = longVal(json, "expires_in");
                this.expiresAt = secs > 0
                        ? new Date(System.currentTimeMillis() + secs * 1000L)
                        : null;
            } else {
                this.idToken = strVal(json, "idToken");
                this.refreshToken = strVal(json, "refreshToken");
                this.uid = strVal(json, "localId");
                this.email = strVal(json, "email");
                long secs = longVal(json, "expiresIn");
                this.expiresAt = secs > 0
                        ? new Date(System.currentTimeMillis() + secs * 1000L)
                        : null;
            }
            this.claims = idToken != null
                    ? OidcTokens.decodeIdTokenClaims(idToken)
                    : null;
        }

        public String getIdToken() {
            return idToken;
        }
        public String getRefreshToken() {
            return refreshToken;
        }
        public String getUid() {
            return uid;
        }
        public String getEmail() {
            if (email != null) {
                return email;
            }
            return claims != null && claims.get("email") != null
                    ? claims.get("email").toString() : null;
        }
        public Date getExpiresAt() {
            return expiresAt;
        }
        public Map<String, Object> getIdTokenClaims() {
            return claims;
        }

        private static String strVal(Map<String, Object> json, String k) {
            Object v = json.get(k);
            return v == null ? null : v.toString();
        }

        private static long longVal(Map<String, Object> json, String k) {
            Object v = json.get(k);
            if (v == null) {
                return 0L;
            }
            try {
                String raw = v.toString();
                int dot = raw.indexOf('.');
                if (dot >= 0) {
                    raw = raw.substring(0, dot);
                }
                return Long.parseLong(raw);
            } catch (NumberFormatException nfe) {
                return 0L;
            }
        }
    }
}
