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
import com.codename1.io.NetworkManager;
import com.codename1.io.RequestAuthorizer;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;

/// Sends an [OidcClient]'s access token with the application's requests, and renews it with the
/// refresh token when the service refuses it.
///
/// ```java
/// OidcRequestAuthorizer authorizer = new OidcRequestAuthorizer(client);
/// authorizer.install("https://api.example.com");
/// authorizer.load();          // a session saved by an earlier run, if there is one
/// ```
///
/// From then on every request under that base URL carries `Authorization: Bearer ...`,
/// generated `@RestClient` clients included. The authorizer follows its client: tokens obtained
/// by [OidcClient#authorize()], [OidcClient#refresh(String)] or the device grant are picked up
/// as they arrive, and [OidcClient#clearStoredTokens()] drops them.
///
/// #### When the service answers 401
///
/// The request is held and the refresh token is exchanged for a new set -- once, however many
/// requests were refused together; they all wait for the same exchange. Each is then sent again
/// with the new access token. See [RequestAuthorizer] for what the caller of a request sees.
///
/// When the authorization server refuses the refresh token, the session is over: the tokens
/// are dropped from memory and from the [TokenStore], the held requests deliver their `401`,
/// and every [SignInRequiredListener] is told so the application can show its sign-in screen.
/// A renewal that fails without an answer from the server -- no network -- keeps the tokens:
/// nothing has said they are bad.
///
/// #### Before the token expires
///
/// A refusal is the fallback, not the way a token is normally renewed. When a request is
/// queued and the access token is within [#setRefreshLeeway(int)] of its expiry -- sixty
/// seconds unless set -- the refresh token is exchanged first and the request is kept out
/// of the queue until the exchange is done. It is then sent once, with the new token.
/// Requests queued in the meantime wait for the same exchange.
///
/// Nothing blocks for this: the request has simply not been handed to a network thread
/// yet. Code that waits for it -- `addToQueueAndWait`, the blocking methods of
/// `RequestBuilder` -- returns the one final answer.
///
/// If that exchange is refused the session ends as described above, and the request goes
/// out with no token for the service to answer `401`. If it fails without an answer the
/// request is sent with the token it has, which may still be good, and no exchange is tried
/// ahead of time for the next few seconds.
///
/// A token whose response carried no `expires_in` has no known expiry, and is renewed only
/// when the service refuses it.
public final class OidcRequestAuthorizer implements RequestAuthorizer.Proactive {
    /// How long after an exchange failed without an answer before one is tried ahead of
    /// time again. A request refused in between is still renewed at once.
    private static final long RETRY_AHEAD_MILLIS = 5000;

    /// Told when the user has to sign in again.
    public interface SignInRequiredListener {
        /// The authorization server refused the refresh token, or there was none. Called on
        /// the event dispatch thread, after the tokens were dropped.
        ///
        /// #### Parameters
        ///
        /// - `authorizer`: the authorizer whose session ended
        ///
        /// - `reason`: what the server answered, or null when there was no refresh token
        void signInRequired(OidcRequestAuthorizer authorizer, Throwable reason);
    }

    private final OidcClient client;
    private final Object lock = new Object();
    private final ArrayList<SignInRequiredListener> listeners =
            new ArrayList<SignInRequiredListener>();
    /// Written on the EDT and read on network threads, under [#lock].
    private OidcTokens tokens;
    /// The exchange in progress, shared by every request refused or held while it runs.
    private AsyncResource<Boolean> renewal;
    private int refreshLeewaySeconds = 60;
    /// When an exchange started ahead of time last failed without an answer; under [#lock].
    private long aheadFailedAt;

    /// An authorizer for the tokens of `client`.
    ///
    /// A client has one authorizer: creating a second one for the same client takes its
    /// place.
    ///
    /// #### Parameters
    ///
    /// - `client`: a configured client
    public OidcRequestAuthorizer(OidcClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        this.client = client;
        client.setTokenListener(this);
    }

    /// Registers this authorizer for every request under a base URL. Shorthand for
    /// [NetworkManager#setAuthorizer(String, RequestAuthorizer)], whose matching rules apply.
    ///
    /// #### Parameters
    ///
    /// - `baseUrl`: the base URL of the application's service
    ///
    /// #### Returns
    ///
    /// this authorizer
    public OidcRequestAuthorizer install(String baseUrl) {
        NetworkManager.getInstance().setAuthorizer(baseUrl, this);
        return this;
    }

    /// Reads the tokens an earlier run saved into memory. Call it when the application
    /// starts; with a [SecureStorageTokenStore] that requires biometrics, this is the one
    /// moment the user is prompted.
    ///
    /// #### Returns
    ///
    /// a resource that completes with the tokens, or with null when nothing was stored
    public AsyncResource<OidcTokens> load() {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        client.loadStoredTokens().ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens stored) {
                if (stored != null) {
                    setTokens(stored);
                }
                out.complete(stored);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                out.error(err);
            }
        });
        return out;
    }

    /// The tokens in use.
    ///
    /// #### Returns
    ///
    /// the tokens, or null when nobody is signed in
    public OidcTokens getTokens() {
        synchronized (lock) {
            return tokens;
        }
    }

    /// Replaces the tokens in use. Tokens the client obtains arrive here by themselves; this
    /// is for a set that came from somewhere else.
    ///
    /// #### Parameters
    ///
    /// - `tokens`: the tokens, or null for none
    public void setTokens(OidcTokens tokens) {
        synchronized (lock) {
            this.tokens = tokens;
        }
    }

    /// Whether there is an access token to send.
    public boolean isSignedIn() {
        OidcTokens current = getTokens();
        return current != null && current.getAccessToken() != null;
    }

    /// Drops the tokens from memory and from the client's [TokenStore]. Requests go out
    /// without a header from then on. This doesn't tell the server; call
    /// [OidcClient#revoke(String)] with the refresh token first for that.
    ///
    /// #### Returns
    ///
    /// the result of clearing the store
    public AsyncResource<Boolean> signOut() {
        setTokens(null);
        return client.clearStoredTokens();
    }

    /// Adds a listener told when the session can't be renewed.
    public void addSignInRequiredListener(SignInRequiredListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /// Removes a listener.
    public void removeSignInRequiredListener(SignInRequiredListener listener) {
        listeners.remove(listener);
    }

    /// How close to its expiry an access token is renewed before a request is sent with it.
    /// Sixty seconds unless set: long enough for the request to reach a service whose clock
    /// runs a little ahead. Zero renews a token only once it has expired, and a negative
    /// value turns renewing ahead of time off, leaving the `401` as the only trigger.
    ///
    /// #### Parameters
    ///
    /// - `seconds`: the leeway in seconds
    ///
    /// #### Returns
    ///
    /// this authorizer
    public OidcRequestAuthorizer setRefreshLeeway(int seconds) {
        synchronized (lock) {
            this.refreshLeewaySeconds = seconds;
        }
        return this;
    }

    /// The leeway set with [#setRefreshLeeway(int)].
    public int getRefreshLeeway() {
        synchronized (lock) {
            return refreshLeewaySeconds;
        }
    }

    @Override
    public String getAuthorization(ConnectionRequest request) {
        return headerOf(getTokens());
    }

    @Override
    public AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request) {
        final OidcTokens current;
        synchronized (lock) {
            current = tokens;
            if (refreshLeewaySeconds < 0 || current == null || current.getAccessToken() == null
                    || current.getRefreshToken() == null
                    || !current.isExpiringWithin(refreshLeewaySeconds)
                    || System.currentTimeMillis() - aheadFailedAt < RETRY_AHEAD_MILLIS) {
                return null;
            }
        }
        if (CN.isEdt()) {
            return renewAhead(current);
        }
        // The exchange in progress is kept on the event dispatch thread, where a refusal
        // is handled too; a request queued from another thread joins it from there.
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                AsyncResource<Boolean> joined = renewAhead(current);
                if (joined == null) {
                    out.complete(Boolean.TRUE);
                    return;
                }
                joined.ready(new SuccessCallback<Boolean>() {
                    @Override
                    public void onSucess(Boolean renewed) {
                        out.complete(renewed);
                    }
                }).except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.complete(Boolean.FALSE);
                    }
                });
            }
        });
        return out;
    }

    /// Joins the exchange in progress or starts one for `expiring`, on the event dispatch
    /// thread. Null when the token has been replaced since it was looked at.
    private AsyncResource<Boolean> renewAhead(OidcTokens expiring) {
        if (renewal != null) {
            return renewal;
        }
        OidcTokens current = getTokens();
        if (current != expiring) { //NOPMD CompareObjectsWithEquals - the set that was looked at
            // Renewed, or dropped, between the look and now.
            return null;
        }
        return exchange(current, true);
    }

    @Override
    public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
            String rejectedAuthorization) {
        if (renewal != null) {
            return renewal;
        }
        OidcTokens current = getTokens();
        String header = headerOf(current);
        if (header != null && !header.equals(rejectedAuthorization)) {
            // Refused with a token that has been replaced since: this request left before
            // an earlier renewal finished. The new token is already here.
            return done(true);
        }
        final String refreshToken = current == null ? null : current.getRefreshToken();
        if (refreshToken == null) {
            if (current != null) {
                endSession(null);
            }
            return done(false);
        }
        return exchange(current, false);
    }

    /// Exchanges the refresh token of `current` for a new set, as the one exchange every
    /// request that needs it shares until it is done. `ahead` says that nothing has refused
    /// the token yet.
    private AsyncResource<Boolean> exchange(OidcTokens current, final boolean ahead) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        renewal = out;
        AsyncResource<OidcTokens> exchange;
        try {
            exchange = client.refresh(current.getRefreshToken());
        } catch (RuntimeException misconfigured) {
            renewal = null;
            out.complete(Boolean.FALSE);
            return out;
        }
        exchange.ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens fresh) {
                // The client has handed the new set over already; this covers a client
                // whose listener is another authorizer by now.
                setTokens(fresh);
                renewal = null;
                out.complete(Boolean.TRUE);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                renewal = null;
                boolean refused = err instanceof OidcException
                        && !OidcException.TRANSPORT_ERROR.equals(((OidcException) err).getError());
                if (refused) {
                    endSession(err);
                } else if (ahead) {
                    // Nothing said the token is bad; it is sent as it is, and the next
                    // request does not try again at once.
                    synchronized (lock) {
                        aheadFailedAt = System.currentTimeMillis();
                    }
                }
                out.complete(Boolean.FALSE);
            }
        });
        return out;
    }

    /// Called by the client for every token set it obtains, and with null when it clears them.
    void tokensChanged(OidcTokens fresh) {
        setTokens(fresh);
    }

    private void endSession(Throwable reason) {
        setTokens(null);
        client.clearStoredTokens();
        SignInRequiredListener[] told =
                listeners.toArray(new SignInRequiredListener[listeners.size()]);
        for (SignInRequiredListener l : told) {
            l.signInRequired(this, reason);
        }
    }

    private static AsyncResource<Boolean> done(boolean renewed) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(renewed ? Boolean.TRUE : Boolean.FALSE);
        return out;
    }

    private static String headerOf(OidcTokens t) {
        if (t == null || t.getAccessToken() == null) {
            return null;
        }
        return "Bearer " + t.getAccessToken();
    }
}
