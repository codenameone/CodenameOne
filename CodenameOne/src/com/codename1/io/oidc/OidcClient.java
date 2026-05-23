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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.security.SecureRandom;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Modern OpenID Connect / OAuth 2.0 client. Built around the
/// authorization-code flow with PKCE (RFC 7636) and the system browser. Use
/// it as the foundation for all new sign-in integrations:
///
/// ```java
/// OidcClient.discover("https://accounts.google.com").ready(new SuccessCallback<OidcClient>() {
///     public void onSucess(OidcClient client) {
///         client.setClientId("YOUR_CLIENT_ID")
///               .setRedirectUri("com.example.app:/oauth2redirect")
///               .setScopes("openid", "email", "profile");
///         client.authorize().ready(new SuccessCallback<OidcTokens>() {
///             public void onSucess(OidcTokens tokens) {
///                 // use tokens.getAccessToken() / tokens.getIdToken()
///             }
///         });
///     }
/// });
/// ```
///
/// ### What this gives you that [com.codename1.io.Oauth2] does not
///
/// - Discovery via `.well-known/openid-configuration` so you only configure
///   the issuer URL, not five separate endpoints
/// - PKCE S256 on every flow (mandatory; many providers now require it)
/// - System-browser sign-in via [SystemBrowser] (the previous class used
///   an in-app WebView that modern IdPs reject)
/// - Refresh-token flow surfaced as a first-class method
/// - ID-token claim decoding via [OidcTokens#getClaim(String)]
/// - Pluggable [TokenStore] persistence
/// - Nonce + state verification on every authorization round-trip
///
/// ### Things this class deliberately does NOT do
///
/// - **Verify the ID token signature.** This requires the provider's JWKS
///   and ECDSA/RSA verification, which is not feasible on every supported
///   platform without pulling in a heavy dep. The remedy is: trust the
///   TLS connection to the well-known issuer (i.e. always discover, never
///   pass tokens to a server without re-validating server-side).
/// - **Implicit / hybrid / device flows.** Use the lower-level
///   [com.codename1.io.ConnectionRequest] APIs if you need those.
///
/// @since 8.0
public final class OidcClient {

    private final OidcConfiguration configuration;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String[] scopes;
    private String[] additionalAuthParams = new String[0];
    private String[] additionalTokenParams = new String[0];
    private TokenStore tokenStore = new TokenStore.DefaultStorageTokenStore();
    private String storeKey;
    private String responseMode;
    private boolean enforceNonce = true;

    private OidcClient(OidcConfiguration configuration) {
        this.configuration = configuration;
    }

    /// Constructs a client from an already-known [OidcConfiguration]. Use
    /// [#discover(String)] when you'd rather pull the endpoints from the
    /// provider's `.well-known/openid-configuration` document.
    public static OidcClient create(OidcConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }
        return new OidcClient(configuration);
    }

    /// Fetches `<issuer>/.well-known/openid-configuration` and resolves with
    /// an [OidcClient] pre-populated with the discovered endpoints. The
    /// returned client still needs `clientId`, `redirectUri` and `scopes`
    /// before [#authorize()] will work.
    ///
    /// Trailing slashes on `issuer` are tolerated.
    public static AsyncResource<OidcClient> discover(String issuer) {
        if (issuer == null) {
            throw new IllegalArgumentException("issuer must not be null");
        }
        final AsyncResource<OidcClient> out = new AsyncResource<OidcClient>();
        String base = issuer;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        final String url = base + "/.well-known/openid-configuration";
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                try {
                    byte[] body = Util.readInputStream(input);
                    String json = StringUtil.newString(body);
                    Map<String, Object> parsed = new JSONParser()
                            .parseJSON(new StringReader(json));
                    if (parsed == null || parsed.isEmpty()) {
                        out.error(new OidcException(OidcException.DISCOVERY_FAILED,
                                "Discovery document was empty"));
                        return;
                    }
                    OidcConfiguration cfg = OidcConfiguration.fromDiscoveryJson(parsed);
                    out.complete(new OidcClient(cfg));
                } catch (Throwable t) {
                    out.error(new OidcException(OidcException.DISCOVERY_FAILED,
                            "Failed to parse discovery document: " + t.getMessage(), t));
                }
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Failed to fetch discovery document at " + url + ": "
                                + err.getMessage(), err));
            }
        };
        req.setUrl(url);
        req.setPost(false);
        req.setReadResponseForErrors(true);
        NetworkManager.getInstance().addToQueue(req);
        return out;
    }

    public OidcConfiguration getConfiguration() {
        return configuration;
    }

    public OidcClient setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OidcClient setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public OidcClient setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OidcClient setScopes(String... scopes) {
        if (scopes == null) {
            this.scopes = null;
        } else {
            this.scopes = (String[]) scopes.clone();
        }
        return this;
    }

    public OidcClient setScopes(List<String> scopes) {
        if (scopes == null) {
            this.scopes = null;
        } else {
            this.scopes = scopes.toArray(new String[0]);
        }
        return this;
    }

    /// Extra `name=value` parameters appended to the authorization-endpoint
    /// URL. Use for provider-specific options like Google's `prompt=consent`
    /// or Apple's `response_mode=form_post`. Values are URL-encoded.
    public OidcClient setAuthorizationParameters(String... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        this.additionalAuthParams = (String[]) kv.clone();
        return this;
    }

    /// Extra `name=value` parameters sent as form data on every token-endpoint
    /// POST.
    public OidcClient setTokenParameters(String... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        this.additionalTokenParams = (String[]) kv.clone();
        return this;
    }

    /// Swaps the token persistence strategy. Defaults to
    /// [TokenStore.DefaultStorageTokenStore].
    public OidcClient setTokenStore(TokenStore store) {
        this.tokenStore = store == null
                ? new TokenStore.DefaultStorageTokenStore()
                : store;
        return this;
    }

    /// Override the key under which tokens are stored. Defaults to the
    /// issuer + client-id pair so that multiple clients can coexist.
    public OidcClient setStoreKey(String key) {
        this.storeKey = key;
        return this;
    }

    /// `false` skips the `nonce` claim check on the returned ID token. Only
    /// disable when you have a very good reason (e.g. provider known not to
    /// echo the nonce); the default is to enforce.
    public OidcClient setEnforceNonce(boolean enforce) {
        this.enforceNonce = enforce;
        return this;
    }

    /// Sets the `response_mode` parameter sent on the authorization URL
    /// (e.g. `"form_post"` for Apple Sign-In with the web fallback).
    public OidcClient setResponseMode(String mode) {
        this.responseMode = mode;
        return this;
    }

    /// Launches an authorization-code flow with PKCE. The user is sent to the
    /// system browser to sign in; the returned [AsyncResource] completes with
    /// the token set or errors with [OidcException] (e.g. `USER_CANCELLED`,
    /// `STATE_MISMATCH`).
    public AsyncResource<OidcTokens> authorize() {
        requireConfigured();
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        final PkceChallenge pkce = PkceChallenge.generate();
        final String state = randomToken(16);
        final String nonce = randomToken(16);
        String authUrl = buildAuthorizationUrl(state, nonce, pkce);
        SystemBrowser.authenticate(authUrl, redirectUri)
                .ready(new SuccessCallback<String>() {
                    @Override
                    public void onSucess(String redirectUrl) {
                        handleRedirect(redirectUrl, state, nonce, pkce, out);
                    }
                })
                .except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.error(err);
                    }
                });
        return out;
    }

    /// Exchanges a stored refresh token for a fresh access token. Pass the
    /// value returned from [OidcTokens#getRefreshToken()] on a previous flow.
    /// The new tokens are persisted via the current [TokenStore].
    public AsyncResource<OidcTokens> refresh(final String refreshToken) {
        requireConfigured();
        if (refreshToken == null) {
            throw new IllegalArgumentException("refreshToken must not be null");
        }
        if (configuration.getTokenEndpoint() == null) {
            throw new IllegalStateException("OIDC configuration is missing tokenEndpoint");
        }
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        Map<String, String> args = new HashMap<String, String>();
        args.put("grant_type", "refresh_token");
        args.put("refresh_token", refreshToken);
        if (scopes != null && scopes.length > 0) {
            args.put("scope", join(scopes));
        }
        appendBaseTokenArgs(args);
        postToTokenEndpoint(args, refreshToken, null, out);
        return out;
    }

    /// Returns previously-saved tokens for this client (or `null`). Combine
    /// with [#refreshIfExpired(int)] to silently bring the session back to
    /// life on app launch.
    public AsyncResource<OidcTokens> loadStoredTokens() {
        return tokenStore.load(storageKey());
    }

    /// Loads stored tokens; if they are within `leewaySeconds` of expiring,
    /// runs a refresh and saves the new tokens. Completes with `null` when
    /// nothing is stored or when the stored token has no refresh token and
    /// has already expired.
    public AsyncResource<OidcTokens> refreshIfExpired(final int leewaySeconds) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        loadStoredTokens()
                .ready(new SuccessCallback<OidcTokens>() {
                    @Override
                    public void onSucess(OidcTokens stored) {
                        if (stored == null) {
                            out.complete(null);
                            return;
                        }
                        if (!stored.isExpiringWithin(leewaySeconds)) {
                            out.complete(stored);
                            return;
                        }
                        String rt = stored.getRefreshToken();
                        if (rt == null) {
                            out.complete(null);
                            return;
                        }
                        refresh(rt)
                                .ready(new SuccessCallback<OidcTokens>() {
                                    @Override
                                    public void onSucess(OidcTokens fresh) {
                                        out.complete(fresh);
                                    }
                                })
                                .except(new SuccessCallback<Throwable>() {
                                    @Override
                                    public void onSucess(Throwable err) {
                                        out.error(err);
                                    }
                                });
                    }
                })
                .except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.error(err);
                    }
                });
        return out;
    }

    /// Sends a token-revocation request to the issuer (RFC 7009). Silently
    /// no-ops when the issuer does not advertise a `revocation_endpoint`.
    public AsyncResource<Boolean> revoke(final String token) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (token == null || configuration.getRevocationEndpoint() == null) {
            out.complete(Boolean.FALSE);
            return out;
        }
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                Util.readInputStream(input);
                out.complete(Boolean.TRUE);
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Token revocation failed: " + err.getMessage(), err));
            }
        };
        req.setUrl(configuration.getRevocationEndpoint());
        req.setPost(true);
        req.setReadResponseForErrors(true);
        req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        req.addArgument("token", token);
        req.addArgument("client_id", clientId);
        if (clientSecret != null) {
            req.addArgument("client_secret", clientSecret);
        }
        NetworkManager.getInstance().addToQueue(req);
        return out;
    }

    /// Clears any stored tokens for this client. Does not call the issuer's
    /// revocation endpoint -- combine with [#revoke(String)] if you want a
    /// proper sign-out.
    public AsyncResource<Boolean> clearStoredTokens() {
        return tokenStore.clear(storageKey());
    }

    // -----------------------------------------------------------
    // internals

    private void requireConfigured() {
        if (clientId == null) {
            throw new IllegalStateException("clientId is required");
        }
        if (redirectUri == null) {
            throw new IllegalStateException("redirectUri is required");
        }
        if (configuration.getAuthorizationEndpoint() == null) {
            throw new IllegalStateException("authorizationEndpoint missing from configuration");
        }
    }

    private String storageKey() {
        if (storeKey != null) {
            return storeKey;
        }
        String issuer = configuration.getIssuer();
        if (issuer == null) {
            issuer = configuration.getAuthorizationEndpoint();
        }
        return issuer + "|" + clientId;
    }

    private String buildAuthorizationUrl(String state, String nonce, PkceChallenge pkce) {
        StringBuilder b = new StringBuilder(configuration.getAuthorizationEndpoint());
        b.append(configuration.getAuthorizationEndpoint().indexOf('?') >= 0 ? '&' : '?');
        appendParam(b, "response_type", "code");
        appendParam(b, "client_id", clientId);
        appendParam(b, "redirect_uri", redirectUri);
        if (scopes != null && scopes.length > 0) {
            appendParam(b, "scope", join(scopes));
        }
        appendParam(b, "state", state);
        if (enforceNonce) {
            appendParam(b, "nonce", nonce);
        }
        appendParam(b, "code_challenge", pkce.getChallenge());
        appendParam(b, "code_challenge_method", pkce.getMethod());
        if (responseMode != null) {
            appendParam(b, "response_mode", responseMode);
        }
        for (int i = 0; i + 1 < additionalAuthParams.length; i += 2) {
            appendParam(b, additionalAuthParams[i], additionalAuthParams[i + 1]);
        }
        return b.toString();
    }

    private static void appendParam(StringBuilder b, String k, String v) {
        char last = b.charAt(b.length() - 1);
        if (last != '?' && last != '&') {
            b.append('&');
        }
        b.append(Util.encodeUrl(k)).append('=').append(Util.encodeUrl(v));
    }

    private void handleRedirect(String redirectUrl,
                                String expectedState,
                                String expectedNonce,
                                PkceChallenge pkce,
                                final AsyncResource<OidcTokens> out) {
        Map<String, String> params = parseRedirectParams(redirectUrl);
        String error = params.get("error");
        if (error != null) {
            String description = params.get("error_description");
            String code = "access_denied".equals(error) ? OidcException.ACCESS_DENIED : error;
            out.error(new OidcException(code,
                    description != null ? description : error));
            return;
        }
        String returnedState = params.get("state");
        if (returnedState == null || !returnedState.equals(expectedState)) {
            out.error(new OidcException(OidcException.STATE_MISMATCH,
                    "Authorization server returned a different 'state' than the one we sent"));
            return;
        }
        String code = params.get("code");
        if (code == null) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "Authorization redirect was missing the 'code' parameter"));
            return;
        }
        exchangeCode(code, expectedNonce, pkce, out);
    }

    private void exchangeCode(String code,
                              final String expectedNonce,
                              PkceChallenge pkce,
                              final AsyncResource<OidcTokens> out) {
        if (configuration.getTokenEndpoint() == null) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "OIDC configuration is missing tokenEndpoint"));
            return;
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put("grant_type", "authorization_code");
        args.put("code", code);
        args.put("redirect_uri", redirectUri);
        args.put("code_verifier", pkce.getVerifier());
        appendBaseTokenArgs(args);
        postToTokenEndpoint(args, null, expectedNonce, out);
    }

    private void appendBaseTokenArgs(Map<String, String> args) {
        args.put("client_id", clientId);
        if (clientSecret != null) {
            args.put("client_secret", clientSecret);
        }
        for (int i = 0; i + 1 < additionalTokenParams.length; i += 2) {
            args.put(additionalTokenParams[i], additionalTokenParams[i + 1]);
        }
    }

    private void postToTokenEndpoint(final Map<String, String> args,
                                     final String refreshTokenFallback,
                                     final String expectedNonce,
                                     final AsyncResource<OidcTokens> out) {
        final boolean[] completed = new boolean[1];
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                if (completed[0]) { return; }
                byte[] body = Util.readInputStream(input);
                String json = StringUtil.newString(body);
                Map<String, Object> parsed;
                try {
                    parsed = new JSONParser().parseJSON(new StringReader(json));
                } catch (Exception e) {
                    completed[0] = true;
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Token endpoint returned malformed JSON: " + json, e));
                    return;
                }
                if (parsed == null) {
                    completed[0] = true;
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Token endpoint returned no body"));
                    return;
                }
                if (parsed.get("error") != null) {
                    completed[0] = true;
                    Object desc = parsed.get("error_description");
                    out.error(new OidcException(parsed.get("error").toString(),
                            desc != null ? desc.toString() : null));
                    return;
                }
                final OidcTokens tokens = OidcTokens.fromTokenResponse(parsed, refreshTokenFallback);
                if (enforceNonce && expectedNonce != null && tokens.getIdToken() != null) {
                    Object nonceClaim = tokens.getClaim("nonce");
                    if (nonceClaim != null && !expectedNonce.equals(nonceClaim.toString())) {
                        completed[0] = true;
                        out.error(new OidcException(OidcException.NONCE_MISMATCH,
                                "ID token nonce did not match"));
                        return;
                    }
                }
                tokenStore.save(storageKey(), tokens)
                        .except(new SuccessCallback<Throwable>() {
                            @Override
                            public void onSucess(Throwable t) {
                                // Token persistence failure is non-fatal; tokens are still valid in-memory.
                            }
                        });
                completed[0] = true;
                out.complete(tokens);
            }

            @Override
            protected void handleException(Exception err) {
                if (completed[0]) { return; }
                completed[0] = true;
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Token endpoint request failed: " + err.getMessage(), err));
            }
        };
        req.setUrl(configuration.getTokenEndpoint());
        req.setPost(true);
        req.setReadResponseForErrors(true);
        req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        req.addRequestHeader("Accept", "application/json");
        for (Map.Entry<String, String> e : args.entrySet()) {
            req.addArgument(e.getKey(), e.getValue());
        }
        NetworkManager.getInstance().addToQueue(req);
    }

    private static Map<String, String> parseRedirectParams(String url) {
        Map<String, String> out = new HashMap<String, String>();
        int qm = url.indexOf('?');
        int hash = url.indexOf('#');
        String tail = null;
        if (qm >= 0) {
            tail = url.substring(qm + 1);
            int h2 = tail.indexOf('#');
            if (h2 >= 0) {
                String fragment = tail.substring(h2 + 1);
                tail = tail.substring(0, h2);
                merge(out, fragment);
            }
        } else if (hash >= 0) {
            tail = url.substring(hash + 1);
        }
        if (tail != null) {
            merge(out, tail);
        }
        return out;
    }

    private static void merge(Map<String, String> out, String query) {
        String[] pairs = Util.split(query, "&");
        for (String p : pairs) {
            int eq = p.indexOf('=');
            if (eq < 0) { continue; }
            String k = decode(p.substring(0, eq));
            String v = decode(p.substring(eq + 1));
            out.put(k, v);
        }
    }

    private static String decode(String s) {
        StringBuilder b = new StringBuilder(s.length());
        int i = 0;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '+') {
                b.append(' ');
                i++;
            } else if (c == '%' && i + 2 < len) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    b.append((char) ((hi << 4) | lo));
                    i += 3;
                } else {
                    b.append(c);
                    i++;
                }
            } else {
                b.append(c);
                i++;
            }
        }
        return b.toString();
    }

    private static String join(String[] items) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) { b.append(' '); }
            b.append(items[i]);
        }
        return b.toString();
    }

    private static String randomToken(int byteLength) {
        byte[] bytes = SecureRandom.bytes(byteLength);
        String s = Base64.encodeUrlSafe(bytes);
        StringBuilder b = new StringBuilder(s.length());
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '=' || c == '\n' || c == '\r') { continue; }
            b.append(c);
        }
        return b.toString();
    }
}
