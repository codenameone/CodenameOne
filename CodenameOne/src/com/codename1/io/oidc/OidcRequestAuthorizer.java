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
import com.codename1.ui.Display;
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
///
/// #### Threads
///
/// Everything an authorizer holds -- the tokens, the exchange in progress, the listeners --
/// belongs to the event dispatch thread and is read and changed nowhere else. Nothing here
/// is locked. A network thread never calls an authorizer: it sends the header the EDT put
/// on the request when the request was queued. Tokens that arrive on a network thread, and
/// a `401` seen there, are passed to the EDT before the authorizer hears of them.
///
/// Call this class on the EDT. The methods that read or change its state can also be
/// called from another thread, and then wait for the EDT to do the work -- so not from a
/// thread the EDT is itself waiting for.
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
    // Everything below is read and written on the event dispatch thread only.
    private final ArrayList<SignInRequiredListener> listeners =
            new ArrayList<SignInRequiredListener>();
    private OidcTokens tokens;
    /// Changes when tokens are replaced or a newer load starts; EDT-owned.
    private long tokenGeneration;
    /// The exchange in progress, shared by every request refused or held while it runs.
    private AsyncResource<Boolean> renewal;
    /// The client's exchange behind `renewal`, kept so that signing out can abandon it.
    private AsyncResource<OidcTokens> exchanging;
    /// Set to true, for that exchange's callbacks to read, when it is abandoned.
    private boolean[] exchangeAbandoned;
    private int refreshLeewaySeconds = 60;
    /// When an exchange started ahead of time last failed without an answer.
    private long aheadFailedAt;

    /// Runs `work` on the event dispatch thread and returns when it has run: at once on
    /// the EDT itself, and before there is one.
    private static void onEdt(Runnable work) {
        if (!Display.isInitialized() || CN.isEdt()) {
            work.run();
        } else {
            CN.callSeriallyAndWait(work);
        }
    }

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
        onEdt(new Runnable() {
            @Override
            public void run() {
                OidcRequestAuthorizer.this.client.setTokenListener(OidcRequestAuthorizer.this);
            }
        });
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
        final long[] generation = new long[1];
        onEdt(new Runnable() {
            @Override
            public void run() {
                tokenGeneration++;
                generation[0] = tokenGeneration;
            }
        });
        client.loadStoredTokens().ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(final OidcTokens stored) {
                Runnable take = new Runnable() {
                    @Override
                    public void run() {
                        if (generation[0] != tokenGeneration) {
                            out.complete(null);
                            return;
                        }
                        if (stored != null) {
                            tokens = stored;
                        }
                        out.complete(stored);
                    }
                };
                if (!Display.isInitialized() || CN.isEdt()) {
                    take.run();
                } else {
                    // A store may answer on a thread of its own, which is not made to wait.
                    CN.callSerially(take);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(final Throwable err) {
                Runnable fail = new Runnable() {
                    @Override
                    public void run() {
                        if (generation[0] == tokenGeneration) {
                            out.error(err);
                        } else {
                            out.complete(null);
                        }
                    }
                };
                if (!Display.isInitialized() || CN.isEdt()) {
                    fail.run();
                } else {
                    CN.callSerially(fail);
                }
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
        final OidcTokens[] out = new OidcTokens[1];
        onEdt(new Runnable() {
            @Override
            public void run() {
                out[0] = tokens;
            }
        });
        return out[0];
    }

    /// Replaces the tokens in use. Tokens the client obtains arrive here by themselves; this
    /// is for a set that came from somewhere else.
    ///
    /// #### Parameters
    ///
    /// - `tokens`: the tokens, or null for none
    public void setTokens(final OidcTokens tokens) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                OidcRequestAuthorizer.this.tokens = tokens;
                tokenGeneration++;
                if (tokens == null) {
                    abandonRenewal();
                }
            }
        });
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
    /// A renewal in progress is abandoned: whatever it comes back with is dropped, and the
    /// requests waiting for it go out with no token.
    ///
    /// #### Returns
    ///
    /// the result of clearing the store
    public AsyncResource<Boolean> signOut() {
        // setTokens(null) abandons a renewal in progress before the store is cleared.
        setTokens(null);
        return client.clearStoredTokens();
    }

    /// Adds a listener told when the session can't be renewed.
    public void addSignInRequiredListener(final SignInRequiredListener listener) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                if (listener != null && !listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        });
    }

    /// Removes a listener.
    public void removeSignInRequiredListener(final SignInRequiredListener listener) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                listeners.remove(listener);
            }
        });
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
    public OidcRequestAuthorizer setRefreshLeeway(final int seconds) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                refreshLeewaySeconds = seconds;
            }
        });
        return this;
    }

    /// The leeway set with [#setRefreshLeeway(int)].
    public int getRefreshLeeway() {
        final int[] out = new int[1];
        onEdt(new Runnable() {
            @Override
            public void run() {
                out[0] = refreshLeewaySeconds;
            }
        });
        return out[0];
    }

    /// Called on the event dispatch thread as a request is queued; the request carries the
    /// answer to the network thread.
    @Override
    public String getAuthorization(ConnectionRequest request) {
        return headerOf(tokens);
    }

    /// Called on the event dispatch thread as a request is queued.
    @Override
    public AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request) {
        OidcTokens current = tokens;
        if (refreshLeewaySeconds < 0 || current == null || current.getAccessToken() == null
                || current.getRefreshToken() == null
                || !current.isExpiringWithin(refreshLeewaySeconds)
                || System.currentTimeMillis() - aheadFailedAt < RETRY_AHEAD_MILLIS) {
            return null;
        }
        if (renewal != null) {
            return renewal;
        }
        return exchange(current, true);
    }

    @Override
    public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
            String rejectedAuthorization) {
        if (renewal != null) {
            return renewal;
        }
        OidcTokens current = tokens;
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
        final AsyncResource<OidcTokens> exchange;
        try {
            exchange = client.refreshTokens(current);
        } catch (RuntimeException misconfigured) {
            renewal = null;
            out.complete(Boolean.FALSE);
            return out;
        }
        final boolean[] abandoned = new boolean[1];
        exchanging = exchange;
        exchangeAbandoned = abandoned;
        exchange.ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens fresh) {
                if (abandoned[0]) {
                    // Abandoned by a sign-out; see abandonRenewal().
                    return;
                }
                // The client has handed the new set over already; this covers a client
                // whose listener is another authorizer by now.
                tokens = fresh;
                exchanging = null;
                exchangeAbandoned = null;
                renewal = null;
                out.complete(Boolean.TRUE);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                if (abandoned[0]) {
                    // Abandoned by a sign-out, which ended the session already: a refusal
                    // arriving now must not end whatever session came after it.
                    return;
                }
                exchanging = null;
                exchangeAbandoned = null;
                renewal = null;
                boolean refused = err instanceof OidcException
                        && !OidcException.TRANSPORT_ERROR.equals(((OidcException) err).getError());
                if (refused) {
                    endSession(err);
                } else if (ahead) {
                    // Nothing said the token is bad; it is sent as it is, and the next
                    // request does not try again at once.
                    aheadFailedAt = System.currentTimeMillis();
                }
                out.complete(Boolean.FALSE);
            }
        });
        return out;
    }

    /// Called by the client for every token set it obtains, and with null when it clears
    /// them. On the event dispatch thread: the client passes over there what a network
    /// thread read.
    void tokensChanged(OidcTokens fresh) {
        tokens = fresh;
        if (fresh == null) {
            abandonRenewal();
        }
    }

    /// Gives up on the exchange in progress, if there is one, because the tokens it would
    /// renew have just been dropped -- the user signed out. On the event dispatch thread.
    ///
    /// Dropping the tokens was not enough. The exchange was still on its way, and when it
    /// succeeded the client saved the new set and handed it over, so the user who had
    /// signed out was signed in again a moment later. Cancelling the client's resource is
    /// what makes the client drop that answer -- it looks, on this thread, before it stores
    /// or tells anyone. The requests held for the renewal are released as not renewed.
    private void abandonRenewal() {
        AsyncResource<OidcTokens> inFlight = exchanging;
        AsyncResource<Boolean> held = renewal;
        if (exchangeAbandoned != null) {
            exchangeAbandoned[0] = true;
        }
        exchanging = null;
        exchangeAbandoned = null;
        renewal = null;
        if (inFlight != null) {
            inFlight.cancel(false);
        }
        if (held != null && !held.isDone()) {
            held.complete(Boolean.FALSE);
        }
    }

    private void endSession(Throwable reason) {
        tokens = null;
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
