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
import com.codename1.io.RequestAuthorizer;
import com.codename1.io.Util;
import com.codename1.security.Jwt;
import com.codename1.security.SecureRandom;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
/// ### What is checked before tokens are handed over
///
/// - The authorization response: its `state`, and the issuer it names. A response that
///   names another issuer than this client's is refused, and so is one that names none
///   when the provider's discovery document says it always does (RFC 9207). That is what
///   stops a response from one provider being taken for another's.
/// - The ID token's claims: `iss` is the provider, `aud` is this client, `exp` has not
///   passed, `nonce` is the one the request carried, and `at_hash`, when the token has
///   one, is the hash of the access token it came with.
/// - The ID token's signature, against the provider's keys. The keys are fetched from the
///   configuration's `jwks_uri` once and kept, and fetched again when a token names a key
///   that is not among them. `RS256`, `RS384`, `RS512`, `ES256` and `ES384` are accepted;
///   an unsigned token, or one signed with the client secret, is not.
///
/// A token that fails any of this is not stored and not returned: the resource fails
/// with [OidcException#INVALID_ID_TOKEN], [OidcException#NONCE_MISMATCH] or
/// [OidcException#ISSUER_MISMATCH]. That includes a signature this platform has no way
/// to check. Nothing is skipped quietly -- see [#setVerifyIdTokenSignature(boolean)] for
/// the one switch there is, and what turning it off gives up.
///
/// ### Things this class deliberately does NOT do
///
/// - **Implicit and hybrid flows.** Use the lower-level
///   [com.codename1.io.ConnectionRequest] APIs if you need those.
///
/// ### A device without a browser or a keyboard
///
/// [#requestDeviceAuthorization()] and [#pollDeviceToken(OidcDeviceAuthorization)]
/// run the device authorization grant (RFC 8628): the device shows a short
/// code, the user approves it on a phone or a computer, and the device
/// receives its tokens.
///
/// ### Using the tokens
///
/// [OidcRequestAuthorizer] attaches the access token to the application's
/// requests and renews it with the refresh token when the service refuses it.
///
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
    private final List<StoreWrite> storeWrites = new ArrayList<StoreWrite>();
    private String responseMode;
    private boolean enforceNonce = true;
    private boolean verifyIdTokenSignature = true;
    private int idTokenClockSkewSeconds = 300;
    /// The provider's JWK Set as it was last fetched, and when. Read and written where
    /// token responses are read. Two responses read at the same moment can both fetch the
    /// set; each keeps a whole one, and nothing is guarded for the sake of that.
    private List<Map<String, Object>> jwks;
    private long jwksFetchedAt;
    private OidcRequestAuthorizer tokenListener;
    /// How long one second of a device grant's polling interval lasts. A test shortens it.
    int devicePollUnitMillis = 1000;

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
    /// Trailing slashes are removed when building the discovery request URL. The
    /// metadata's `issuer` must still exactly match the supplied issuer, including
    /// its trailing slashes, before any discovered endpoints are accepted.
    public static AsyncResource<OidcClient> discover(final String issuer) {
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
                    // Discovery establishes the identity trusted by subsequent token
                    // verification. Do not let metadata select a different issuer.
                    if (!issuer.equals(parsed.get("issuer"))) {
                        out.error(new OidcException(OidcException.DISCOVERY_FAILED,
                                "Discovery document issuer does not match the requested issuer"));
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

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // The answer's body carries the error and readResponse() reports it. The
                // default handling would put an error dialog in front of the user for what
                // is an ordinary answer here -- a refused refresh token, a device not yet
                // approved.
            }
        };
        req.setUrl(url);
        req.setPost(false);
        req.setReadResponseForErrors(true);
        // Never the application's own authorizer: these are the requests that fetch its
        // token, and one of them waiting on a renewal would be waiting on itself.
        req.setAuthorizer(RequestAuthorizer.NONE);
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

    /// Whether an ID token's signature is verified against the provider's keys before the
    /// token is accepted. True unless set.
    ///
    /// With it on, a token whose signature cannot be checked is refused -- because it does
    /// not verify, because the configuration names no `jwks_uri`, or because the platform
    /// the app is running on cannot verify a signature of that algorithm. The last one is
    /// the reason this switch exists: turn it off for a provider whose ID tokens cannot be
    /// verified on a platform you ship to, and for no other reason.
    ///
    /// With it off, the claims are still checked, but they are only as good as the TLS
    /// connection to the token endpoint. Do not make a decision on your server from an ID
    /// token the app forwards: verify it there.
    public OidcClient setVerifyIdTokenSignature(boolean verify) {
        this.verifyIdTokenSignature = verify;
        return this;
    }

    /// How far the device's clock may be from the provider's when an ID token's `exp` is
    /// checked. Five minutes unless set: phones are set by hand more often than servers.
    public OidcClient setIdTokenClockSkew(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds must not be negative");
        }
        this.idTokenClockSkewSeconds = seconds;
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
    /// The stored session supplies the original subject for any refreshed ID token.
    /// A refresh response cannot change that subject. The new tokens are persisted
    /// via the current [TokenStore].
    ///
    /// Cancelling the returned resource before it completes drops the answer: nothing is
    /// stored and no [OidcRequestAuthorizer] is given the new tokens.
    public AsyncResource<OidcTokens> refresh(final String refreshToken) {
        requireRefresh(refreshToken);
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        try {
            loadStoredTokens().ready(new SuccessCallback<OidcTokens>() {
                @Override
                public void onSucess(OidcTokens stored) {
                    if (out.isCancelled()) {
                        return;
                    }
                    if (stored != null && !refreshToken.equals(stored.getRefreshToken())) {
                        out.error(new OidcException(OidcException.INVALID_GRANT,
                                "The refresh token does not belong to the stored session"));
                        return;
                    }
                    refreshTokens(refreshToken, stored, out);
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable error) {
                    out.error(error);
                }
            });
        } catch (Throwable error) {
            out.error(error);
        }
        return out;
    }

    /// Refreshes a session already loaded by the authorizer without reading storage again.
    AsyncResource<OidcTokens> refreshTokens(OidcTokens previous) {
        String refreshToken = previous == null ? null : previous.getRefreshToken();
        requireRefresh(refreshToken);
        AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        refreshTokens(refreshToken, previous, out);
        return out;
    }

    private void requireRefresh(String refreshToken) {
        // Only what the exchange itself uses. A client that signed in with the device grant
        // has no redirect URI, and asking for one here would leave it unable to refresh.
        if (clientId == null) {
            throw new IllegalStateException("clientId is required");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("refreshToken must not be null");
        }
        if (configuration.getTokenEndpoint() == null) {
            throw new IllegalStateException("OIDC configuration is missing tokenEndpoint");
        }
    }

    private void refreshTokens(String refreshToken, OidcTokens previous, AsyncResource<OidcTokens> out) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("grant_type", "refresh_token");
        args.put("refresh_token", refreshToken);
        if (scopes != null && scopes.length > 0) {
            args.put("scope", join(scopes));
        }
        appendBaseTokenArgs(args);
        postToTokenEndpoint(args, refreshToken, null, out, out, previous);
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
                        refreshTokens(stored)
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
    /// A refused request reports the OAuth error, or a transport error when the
    /// non-success HTTP response contains no OAuth error.
    public AsyncResource<Boolean> revoke(final String token) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (token == null || configuration.getRevocationEndpoint() == null) {
            out.complete(Boolean.FALSE);
            return out;
        }
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] body = Util.readInputStream(input);
                int status = getResponseCode();
                if (status < 200 || status >= 300) {
                    Map<String, Object> parsed = null;
                    Exception parseFailure = null;
                    try {
                        parsed = new JSONParser().parseJSON(new StringReader(StringUtil.newString(body)));
                    } catch (Exception malformed) {
                        // An empty body or a gateway page still reports the HTTP failure.
                        parseFailure = malformed;
                    }
                    if (parsed != null && parsed.get("error") instanceof String) {
                        Object description = parsed.get("error_description");
                        out.error(new OidcException((String) parsed.get("error"),
                                description == null ? null : description.toString()));
                    } else {
                        out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                                "Token revocation answered HTTP " + status + " without an OAuth error", parseFailure));
                    }
                    return;
                }
                out.complete(Boolean.TRUE);
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Token revocation failed: " + err.getMessage(), err));
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // readResponse() reports the status and any OAuth error without a dialog.
            }
        };
        req.setUrl(configuration.getRevocationEndpoint());
        req.setPost(true);
        req.setReadResponseForErrors(true);
        // Never the application's own authorizer: these are the requests that fetch its
        // token, and one of them waiting on a renewal would be waiting on itself.
        req.setAuthorizer(RequestAuthorizer.NONE);
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
    /// proper sign-out. The returned resource completes after earlier saves and
    /// this clear finish, so a delayed save cannot restore the cleared session.
    public AsyncResource<Boolean> clearStoredTokens() {
        tokensChanged(null);
        return queueStoreWrite(null);
    }

    /// Starts the device authorization grant (RFC 8628), for a device with no browser or no
    /// practical way to type: a television, a watch, a kiosk, a command line.
    ///
    /// The answer holds a short code. Show it with the verification address; the user opens
    /// that address on another device, signs in and types the code. Meanwhile pass the answer
    /// to [#pollDeviceToken(OidcDeviceAuthorization)], which completes once they have.
    ///
    /// Needs a client id, the scopes, and a provider whose configuration names a
    /// `device_authorization_endpoint`. No redirect URI is involved.
    ///
    /// #### Returns
    ///
    /// a resource that completes with the codes, or fails with an [OidcException] carrying the
    /// server's error code
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when the client id is missing, or the provider does not
    ///   offer the grant
    public AsyncResource<OidcDeviceAuthorization> requestDeviceAuthorization() {
        requireDeviceGrant();
        final AsyncResource<OidcDeviceAuthorization> out =
                new AsyncResource<OidcDeviceAuthorization>();
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                String json = StringUtil.newString(Util.readInputStream(input));
                Map<String, Object> parsed;
                try {
                    parsed = new JSONParser().parseJSON(new StringReader(json));
                } catch (Exception e) {
                    parsed = null;
                }
                if (parsed == null || parsed.isEmpty()) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Device authorization endpoint returned no JSON (HTTP "
                                    + getResponseCode() + ")"));
                    return;
                }
                if (parsed.get("error") != null) {
                    Object desc = parsed.get("error_description");
                    out.error(new OidcException(parsed.get("error").toString(),
                            desc != null ? desc.toString() : null));
                    return;
                }
                try {
                    out.complete(OidcDeviceAuthorization.fromJson(parsed));
                } catch (IllegalArgumentException incomplete) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            incomplete.getMessage(), incomplete));
                }
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Device authorization request failed: " + err.getMessage(), err));
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // The body says why, and readResponse() reports it.
            }
        };
        req.setUrl(configuration.getDeviceAuthorizationEndpoint());
        req.setPost(true);
        req.setReadResponseForErrors(true);
        req.setAuthorizer(RequestAuthorizer.NONE);
        req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        req.addRequestHeader("Accept", "application/json");
        req.addArgument("client_id", clientId);
        if (clientSecret != null) {
            req.addArgument("client_secret", clientSecret);
        }
        if (scopes != null && scopes.length > 0) {
            req.addArgument("scope", join(scopes));
        }
        NetworkManager.getInstance().addToQueue(req);
        return out;
    }

    /// Waits for the user to approve a device, by asking the token endpoint at the pace the
    /// server set.
    ///
    /// The wait is a timer, not a thread: each request is queued when the previous answer's
    /// interval has passed. The interval starts at
    /// [OidcDeviceAuthorization#getInterval()] and grows by five seconds every time the
    /// server answers `slow_down`, and doubles after a request that failed to reach the server
    /// at all.
    ///
    /// The resource completes with the tokens, which are saved to the [TokenStore] the way
    /// [#authorize()] saves them. It fails with an [OidcException] whose code is
    /// [OidcException#ACCESS_DENIED] when the user refused, [OidcException#EXPIRED_TOKEN]
    /// when the codes ran out -- by the server's word or by the clock -- or whatever other
    /// code the server sent. Cancelling the resource stops the polling.
    ///
    /// #### Parameters
    ///
    /// - `authorization`: the answer of [#requestDeviceAuthorization()]
    ///
    /// #### Returns
    ///
    /// a resource that completes with the tokens
    public AsyncResource<OidcTokens> pollDeviceToken(final OidcDeviceAuthorization authorization) {
        if (authorization == null) {
            throw new IllegalArgumentException("authorization must not be null");
        }
        requireDeviceGrant();
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        scheduleDevicePoll(authorization, authorization.getInterval(), out);
        return out;
    }

    private void scheduleDevicePoll(final OidcDeviceAuthorization authorization,
            final int intervalSeconds, final AsyncResource<OidcTokens> out) {
        CN.setTimeout(intervalSeconds * devicePollUnitMillis, new Runnable() {
            @Override
            public void run() {
                devicePoll(authorization, intervalSeconds, out);
            }
        });
    }

    private void devicePoll(final OidcDeviceAuthorization authorization,
            final int intervalSeconds, final AsyncResource<OidcTokens> out) {
        if (out.isDone()) {
            // Cancelled while the timer ran.
            return;
        }
        if (authorization.isExpired()) {
            out.error(new OidcException(OidcException.EXPIRED_TOKEN,
                    "The device code expired before the user approved it"));
            return;
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        args.put("device_code", authorization.getDeviceCode());
        appendBaseTokenArgs(args);
        AsyncResource<OidcTokens> attempt = new AsyncResource<OidcTokens>();
        attempt.ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens tokens) {
                if (!out.isDone()) {
                    out.complete(tokens);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                if (out.isDone()) {
                    return;
                }
                String code = err instanceof OidcException ? ((OidcException) err).getError() : null;
                if (OidcException.AUTHORIZATION_PENDING.equals(code)) {
                    scheduleDevicePoll(authorization, intervalSeconds, out);
                } else if (OidcException.SLOW_DOWN.equals(code)) {
                    scheduleDevicePoll(authorization, intervalSeconds + 5, out);
                } else if (OidcException.TRANSPORT_ERROR.equals(code)) {
                    // The server was not reached, so it has not said no. Asking less often
                    // is what the grant requires of a device that cannot get through.
                    scheduleDevicePoll(authorization, Math.min(intervalSeconds * 2, 60), out);
                } else {
                    out.error(err);
                }
            }
        });
        postToTokenEndpoint(args, null, null, attempt, out);
    }

    private void requireDeviceGrant() {
        if (clientId == null) {
            throw new IllegalStateException("clientId is required");
        }
        if (configuration.getDeviceAuthorizationEndpoint() == null) {
            throw new IllegalStateException(
                    "deviceAuthorizationEndpoint missing from configuration");
        }
        if (configuration.getTokenEndpoint() == null) {
            throw new IllegalStateException("OIDC configuration is missing tokenEndpoint");
        }
    }

    /// Lets one [OidcRequestAuthorizer] follow the tokens this client obtains and clears.
    /// Called on the event dispatch thread.
    void setTokenListener(OidcRequestAuthorizer listener) {
        this.tokenListener = listener;
    }

    /// Takes a verified token set: stores it, tells the authorizer and completes `out`.
    ///
    /// All three happen on the event dispatch thread, and none of them if `waiting` was
    /// cancelled first. A token response is read on a network thread, and whoever asked
    /// for it may have stopped wanting it while it was on its way -- the user signed out
    /// during a refresh, or the application gave up on a device code. The set used to be
    /// saved on the network thread as it arrived, so that sign-out was undone a moment
    /// later: the store held tokens again and the authorizer was handed them. Cancelling
    /// happens on the EDT and so does this, which makes "was it cancelled" a question with
    /// one answer rather than a race.
    private void accept(final OidcTokens tokens, final AsyncResource<OidcTokens> waiting,
            final AsyncResource<OidcTokens> out) {
        Runnable take = new Runnable() {
            @Override
            public void run() {
                if (waiting.isCancelled() || out.isCancelled()) {
                    return;
                }
                queueStoreWrite(tokens)
                        .except(new SuccessCallback<Throwable>() {
                            @Override
                            public void onSucess(Throwable t) {
                                // Token persistence failure is non-fatal; tokens are still valid in-memory.
                            }
                        });
                tellTokenListener(tokens);
                out.complete(tokens);
            }
        };
        if (Display.isInitialized() && !CN.isEdt()) {
            CN.callSerially(take);
        } else {
            take.run();
        }
    }

    /// Serialize writes, including clears, so an asynchronous save cannot restore a
    /// signed-out session after clear has completed. Cancellation of a resource does
    /// not prove a custom store stopped writing, so clear waits for prior writes.
    private AsyncResource<Boolean> queueStoreWrite(OidcTokens tokens) {
        StoreWrite write = new StoreWrite(tokenStore, storageKey(), tokens);
        boolean start;
        synchronized (storeWrites) {
            start = storeWrites.isEmpty();
            storeWrites.add(write);
        }
        if (start) {
            write.start();
        }
        return write.result;
    }

    private final class StoreWrite {
        private final TokenStore store;
        private final String key;
        private final OidcTokens tokens;
        private final AsyncResource<Boolean> result = new AsyncResource<Boolean>();

        StoreWrite(TokenStore store, String key, OidcTokens tokens) {
            this.store = store;
            this.key = key;
            this.tokens = tokens;
        }

        void start() {
            try {
                AsyncResource<Boolean> operation = tokens == null ? store.clear(key) : store.save(key, tokens);
                operation.ready(new SuccessCallback<Boolean>() {
                    @Override
                    public void onSucess(Boolean value) {
                        finish(value, null);
                    }
                }).except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable error) {
                        finish(null, error);
                    }
                });
            } catch (Throwable error) {
                finish(null, error);
            }
        }

        @SuppressWarnings("PMD.CompareObjectsWithEquals") // Only this queued operation may remove itself.
        void finish(Boolean value, Throwable error) {
            StoreWrite next;
            synchronized (storeWrites) {
                // A synchronous callback can throw back through start(). It must not
                // remove the next operation or complete this one a second time.
                if (storeWrites.isEmpty() || storeWrites.get(0) != this) {
                    return;
                }
                storeWrites.remove(0);
                next = storeWrites.isEmpty() ? null : storeWrites.get(0);
            }
            try {
                if (error == null) {
                    result.complete(value);
                } else {
                    result.error(error);
                }
            } finally {
                if (next != null) {
                    next.start();
                }
            }
        }
    }

    /// Tells the authorizer following this client that the tokens were cleared, on the
    /// event dispatch thread, where the authorizer's state lives.
    private void tokensChanged(final OidcTokens tokens) {
        if (Display.isInitialized() && !CN.isEdt()) {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    tellTokenListener(tokens);
                }
            });
            return;
        }
        tellTokenListener(tokens);
    }

    private void tellTokenListener(OidcTokens tokens) {
        if (tokenListener != null) {
            tokenListener.tokensChanged(tokens);
        }
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

    void handleRedirect(String redirectUrl,
                                String expectedState,
                                String expectedNonce,
                                PkceChallenge pkce,
                                final AsyncResource<OidcTokens> out) {
        Map<String, String> params = parseRedirectParams(redirectUrl);
        // Before anything the response says is believed, who it says it is from (RFC 9207).
        // A response another provider produced -- one this app also signs in with, and an
        // attacker steered the browser to -- names that provider here, and its code must
        // not be taken to this provider's token endpoint.
        String issuer = configuration.getIssuer();
        String named = params.get("iss");
        if (named != null && issuer != null && !named.equals(issuer)) {
            out.error(new OidcException(OidcException.ISSUER_MISMATCH,
                    "The authorization response is from " + named + ", not from " + issuer));
            return;
        }
        if (named == null && configuration.isAuthorizationResponseIssParameterSupported()) {
            out.error(new OidcException(OidcException.ISSUER_MISMATCH,
                    "The authorization response names no issuer, and " + issuer
                            + " says it always names itself"));
            return;
        }
        // The state before the error, not after it. The state is what ties a response to
        // the request this client made, and an error is as much a claim about that request
        // as a code is: the server returns the state with both (RFC 6749 section 4.1.2.1).
        // Read the other way round, a link anyone could send -- the redirect URI with
        // error=access_denied -- was reported to the application as the provider's refusal.
        String returnedState = params.get("state");
        if (returnedState == null || !returnedState.equals(expectedState)) {
            out.error(new OidcException(OidcException.STATE_MISMATCH,
                    "Authorization server returned a different 'state' than the one we sent"));
            return;
        }
        String error = params.get("error");
        if (error != null) {
            String description = params.get("error_description");
            String code = "access_denied".equals(error) ? OidcException.ACCESS_DENIED : error;
            out.error(new OidcException(code,
                    description != null ? description : error));
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
        postToTokenEndpoint(args, refreshTokenFallback, expectedNonce, out, out);
    }

    /// `waiting` is the resource the caller of this client holds, when that is not `out`:
    /// the device grant polls with a resource of its own per request.
    private void postToTokenEndpoint(final Map<String, String> args,
                                     final String refreshTokenFallback,
                                     final String expectedNonce,
                                     final AsyncResource<OidcTokens> out,
                                     final AsyncResource<OidcTokens> waiting) {
        postToTokenEndpoint(args, refreshTokenFallback, expectedNonce, out, waiting, null);
    }

    private void postToTokenEndpoint(final Map<String, String> args,
                                     final String refreshTokenFallback,
                                     final String expectedNonce,
                                     final AsyncResource<OidcTokens> out,
                                     final AsyncResource<OidcTokens> waiting,
                                     final OidcTokens previous) {
        final boolean[] completed = new boolean[1];
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                if (completed[0]) {
                    return;
                }
                byte[] body = Util.readInputStream(input);
                String json = StringUtil.newString(body);
                Map<String, Object> parsed = null;
                Exception malformed = null;
                try {
                    parsed = new JSONParser().parseJSON(new StringReader(json));
                } catch (Exception e) {
                    malformed = e;
                }
                completed[0] = true;
                if (parsed != null && parsed.get("error") != null) {
                    Object desc = parsed.get("error_description");
                    out.error(new OidcException(parsed.get("error").toString(),
                            desc != null ? desc.toString() : null));
                    return;
                }
                // From here on the body is not an OAuth error, so it is a token set or it
                // is nothing this client can use. A status that is not a success with no
                // OAuth error in it -- a gateway's 502 page, a 500 with a message of the
                // server's own -- is the server failing, not the server refusing: it is
                // reported as a failure to get an answer, so that a session is kept and a
                // device keeps polling. It used to be read as tokens whenever it parsed.
                int status = getResponseCode();
                if (status < 200 || status >= 300) {
                    out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                            "Token endpoint answered HTTP " + status + " without an OAuth error"));
                    return;
                }
                if (malformed != null) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Token endpoint returned malformed JSON: " + json, malformed));
                    return;
                }
                if (parsed == null) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Token endpoint returned no body"));
                    return;
                }
                // A success carries an access token (RFC 6749 section 5.1). One without was
                // stored and returned as a session nothing could be sent with, and on a
                // refresh it replaced tokens that still worked.
                Object access = parsed.get("access_token");
                if (!(access instanceof String) || ((String) access).length() == 0) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Token endpoint response has no access_token"));
                    return;
                }
                OidcTokens received = OidcTokens.fromTokenResponse(parsed, refreshTokenFallback);
                final OidcTokens tokens = received.getIdToken() == null && previous != null
                        ? received.withIdentityFrom(previous) : received;
                if (refreshTokenFallback == null && requestsOpenId() && tokens.getIdToken() == null) {
                    out.error(new OidcException(OidcException.INVALID_ID_TOKEN,
                            "An initial OpenID Connect response must contain an ID token"));
                    return;
                }
                // Nothing is stored, told or returned until the ID token has been held to
                // its issuer, its audience and its signature.
                checkIdToken(tokens, enforceNonce ? expectedNonce : null, new Runnable() {
                    @Override
                    public void run() {
                        if (refreshTokenFallback != null && tokens.getIdToken() != null
                                && (previous == null || previous.getSubject() == null
                                || !previous.getSubject().equals(tokens.getSubject()))) {
                            out.error(new OidcException(OidcException.INVALID_ID_TOKEN,
                                    "The refreshed ID token must match the previous subject"));
                            return;
                        }
                        accept(tokens, waiting, out);
                    }
                }, out);
            }

            @Override
            protected void handleException(Exception err) {
                if (completed[0]) {
                    return;
                }
                completed[0] = true;
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Token endpoint request failed: " + err.getMessage(), err));
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // The answer's body carries the error and readResponse() reports it. The
                // default handling would put an error dialog in front of the user for what
                // is an ordinary answer here -- a refused refresh token, a device not yet
                // approved.
            }
        };
        req.setUrl(configuration.getTokenEndpoint());
        req.setPost(true);
        req.setReadResponseForErrors(true);
        // Never the application's own authorizer: these are the requests that fetch its
        // token, and one of them waiting on a renewal would be waiting on itself.
        req.setAuthorizer(RequestAuthorizer.NONE);
        req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        req.addRequestHeader("Accept", "application/json");
        for (Map.Entry<String, String> e : args.entrySet()) {
            req.addArgument(e.getKey(), e.getValue());
        }
        NetworkManager.getInstance().addToQueue(req);
    }

    /// How long a fetched JWK Set is trusted to be complete: a token that names a key it
    /// does not hold has the set fetched again, but not more often than this. A test
    /// shortens it.
    long jwksRefetchMillis = 60000;

    private boolean requestsOpenId() {
        if (scopes != null) {
            for (String scope : scopes) {
                if ("openid".equals(scope)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Holds the ID token of a token response to what [IdTokenVerifier] asks of one, then
    /// runs `accepted`; fails `out` otherwise. A response with no ID token has nothing to
    /// check. Called on a network thread.
    private void checkIdToken(final OidcTokens tokens, String expectedNonce,
            final Runnable accepted, final AsyncResource<OidcTokens> out) {
        String idToken = tokens.getIdToken();
        if (idToken == null) {
            accepted.run();
            return;
        }
        final Jwt jwt;
        try {
            jwt = Jwt.parse(idToken);
        } catch (RuntimeException malformed) {
            out.error(new OidcException(OidcException.INVALID_ID_TOKEN,
                    "The ID token is not a JWT: " + malformed.getMessage(), malformed));
            return;
        }
        OidcException refused = IdTokenVerifier.checkClaims(jwt, configuration.getIssuer(),
                clientId, expectedNonce, tokens.getAccessToken(), idTokenClockSkewSeconds,
                System.currentTimeMillis());
        if (refused != null) {
            out.error(refused);
            return;
        }
        if (!verifyIdTokenSignature) {
            accepted.run();
            return;
        }
        if (IdTokenVerifier.keyType(jwt.getAlgorithm()) == null) {
            out.error(IdTokenVerifier.verifySignature(jwt, null));
            return;
        }
        if (configuration.getJwksUri() == null) {
            out.error(new OidcException(OidcException.INVALID_ID_TOKEN, "The configuration "
                    + "names no jwksUri, so the ID token's signature cannot be verified and "
                    + "the token was not accepted. Discover the provider, or give the "
                    + "configuration its jwksUri; setVerifyIdTokenSignature(false) accepts "
                    + "the token unverified."));
            return;
        }
        List<Map<String, Object>> known = jwks;
        long fetchedAt = jwksFetchedAt;
        List<Map<String, Object>> candidates = IdTokenVerifier.candidates(jwt, known);
        if (!candidates.isEmpty() || (known != null
                && System.currentTimeMillis() - fetchedAt < jwksRefetchMillis)) {
            finishIdToken(jwt, candidates, accepted, out);
            return;
        }
        // No key for this token yet: the first sign-in, or the provider has rotated.
        fetchJwks(new SuccessCallback<List<Map<String, Object>>>() {
            @Override
            public void onSucess(List<Map<String, Object>> fetched) {
                finishIdToken(jwt, IdTokenVerifier.candidates(jwt, fetched), accepted, out);
            }
        }, out);
    }

    private static void finishIdToken(Jwt jwt, List<Map<String, Object>> candidates,
            Runnable accepted, AsyncResource<OidcTokens> out) {
        OidcException refused = IdTokenVerifier.verifySignature(jwt, candidates);
        if (refused != null) {
            out.error(refused);
        } else {
            accepted.run();
        }
    }

    /// Fetches the provider's JWK Set and keeps it. A set that cannot be fetched or read
    /// fails `out`: a signature nothing could check is not a signature that was checked.
    private void fetchJwks(final SuccessCallback<List<Map<String, Object>>> then,
            final AsyncResource<OidcTokens> out) {
        final String url = configuration.getJwksUri();
        final boolean[] answered = new boolean[1];
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                if (answered[0]) {
                    return;
                }
                answered[0] = true;
                List<Map<String, Object>> keys = new ArrayList<Map<String, Object>>();
                Map<String, Object> parsed = null;
                if (getResponseCode() == 200) {
                    try {
                        parsed = new JSONParser().parseJSON(new StringReader(
                                StringUtil.newString(Util.readInputStream(input))));
                    } catch (Exception unreadable) {
                        parsed = null;
                    }
                }
                Object listed = parsed == null ? null : parsed.get("keys");
                if (!(listed instanceof List)) {
                    out.error(new OidcException(OidcException.INVALID_ID_TOKEN, "The provider's "
                            + "keys at " + url + " could not be read, so the ID token's "
                            + "signature was not verified and the token was not accepted"));
                    return;
                }
                for (Object key : (List) listed) {
                    if (key instanceof Map) {
                        keys.add((Map<String, Object>) key);
                    }
                }
                jwksFetchedAt = System.currentTimeMillis();
                jwks = keys;
                then.onSucess(keys);
            }

            @Override
            protected void handleException(Exception err) {
                if (answered[0]) {
                    return;
                }
                answered[0] = true;
                out.error(new OidcException(OidcException.TRANSPORT_ERROR, "The provider's keys "
                        + "at " + url + " could not be fetched, so the ID token was not "
                        + "accepted: " + err.getMessage(), err));
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // readResponse reports it: an answer that is not a key set.
            }
        };
        req.setUrl(url);
        req.setPost(false);
        req.setReadResponseForErrors(true);
        req.setAuthorizer(RequestAuthorizer.NONE);
        req.addRequestHeader("Accept", "application/json");
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
            if (eq < 0) {
                continue;
            }
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
            if (i > 0) {
                b.append(' ');
            }
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
            if (c == '=' || c == '\n' || c == '\r') {
                continue;
            }
            b.append(c);
        }
        return b.toString();
    }
}
