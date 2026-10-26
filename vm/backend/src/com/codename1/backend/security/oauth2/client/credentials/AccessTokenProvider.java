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
package com.codename1.backend.security.oauth2.client.credentials;

import com.codename1.backend.VirtualThread;
import com.codename1.backend.security.Clock;
import com.codename1.impl.backend.BackendAccess;

/// An access token for calling another service, kept fresh.
///
/// Wraps an [AccessTokenGrant] and holds the one token it last issued. That
/// token is handed out until `refreshSkewSeconds` before it expires (60 by
/// default), so a request that leaves with it still has that margin for clock
/// differences and time in flight; then the next caller refreshes it.
///
/// Concurrent callers share the refresh work. Exactly one runs the exchange; the
/// others keep using the current token while it has not expired, and wait for the
/// exchange only when there is no unexpired token to give them. A failed refresh
/// is shared the same way: the waiters get the failure, not an expired token.
///
/// A failure is not retried on the spot. Retrying in a loop would hold the
/// request that hit it, and on a virtual thread hold its host, for the whole
/// backoff. Instead the provider backs off: after `n` consecutive failures no
/// exchange is attempted for `min(maxBackoff, 2^(n-1))` seconds, jittered
/// downwards by up to half, and callers in that window get the token still in
/// hand if it has not expired, or the last failure if it has. The first success
/// resets the count. A refused credential (`invalid_client`, `invalid_grant`)
/// backs off the same way, so a revoked key cannot turn every request into an
/// exchange.
///
/// One provider holds one token, so its memory is bounded by construction; a
/// server that calls several services, or one service with several scope sets,
/// keeps one provider for each.
public final class AccessTokenProvider {
    private static final long WAIT_TIMEOUT_MILLIS = 30000;

    private final AccessTokenGrant grant;
    private Clock clock = Clock.SYSTEM;
    private long refreshSkewMillis = 60000;
    private long maxBackoffMillis = 60000;

    private AccessToken current;
    private long currentFetchedAt;
    private boolean refreshing;
    private int failures;
    private long nextAttemptAt;
    private TokenAcquisitionException lastFailure;
    private long jitterSeed = 0x2545F4914F6CDD1DL;

    public AccessTokenProvider(AccessTokenGrant grant) {
        if (grant == null) {
            throw new IllegalArgumentException("A grant is required");
        }
        this.grant = grant;
    }

    public synchronized void setClock(Clock clock) {
        this.clock = clock == null ? Clock.SYSTEM : clock;
    }

    /// How long before expiry a token stops being handed out. Default 60.
    public synchronized void setRefreshSkewSeconds(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("The skew cannot be negative");
        }
        this.refreshSkewMillis = seconds * 1000L;
    }

    /// The longest pause between failed exchanges. Default 60.
    public synchronized void setMaxBackoffSeconds(long seconds) {
        if (seconds < 1) {
            throw new IllegalArgumentException("The backoff is at least a second");
        }
        this.maxBackoffMillis = seconds * 1000L;
    }

    /// A token that has not expired.
    ///
    /// @throws TokenAcquisitionException when none can be had: the exchange
    /// failed, or failed recently and the provider is backing off
    public AccessToken getToken() {
        long waitUntil = -1;
        while (true) {
            boolean mine = false;
            synchronized (this) {
                long now = clock.currentTimeMillis();
                if (current != null && now < current.getExpiresAtMillis() - skew()) {
                    return current;
                }
                boolean usable = current != null && !current.isExpired(now);
                if (refreshing) {
                    if (usable) {
                        // Inside the skew but still good: the refresh in flight
                        // is someone else's, and this caller need not wait for it.
                        return current;
                    }
                    if (waitUntil < 0) {
                        waitUntil = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS;
                    }
                    if (System.currentTimeMillis() >= waitUntil) {
                        throw new TokenAcquisitionException("No access token: a refresh has "
                                + "been running for " + (WAIT_TIMEOUT_MILLIS / 1000)
                                + " seconds", -1, null, true, null);
                    }
                    if (!VirtualThread.isVirtual()) {
                        try {
                            wait(Math.max(1, waitUntil - System.currentTimeMillis()));
                        } catch (InterruptedException err) {
                            Thread.currentThread().interrupt();
                            throw new TokenAcquisitionException("Interrupted while waiting "
                                    + "for an access token", -1, null, true, err);
                        }
                        continue;
                    }
                } else if (lastFailure != null && now < nextAttemptAt) {
                    if (usable) {
                        return current;
                    }
                    throw lastFailure;
                } else {
                    refreshing = true;
                    mine = true;
                }
            }
            if (mine) {
                return refresh();
            }
            // A virtual thread, outside the monitor: wait() would block the host
            // and every other virtual thread on it, so it naps between looks, as
            // AsyncTask.get does.
            BackendAccess.get().napUntil(System.currentTimeMillis() + 5);
        }
    }

    /// The value of the current token, for an `Authorization: Bearer` header.
    public String getTokenValue() {
        return getToken().getTokenValue();
    }

    /// Forgets the current token, so the next call exchanges again. For a token
    /// the other service refused (401) before it expired, such as after a key
    /// rotation there. Does not reset a backoff in progress.
    public synchronized void invalidate() {
        current = null;
    }

    /// The skew, but never more than half the current token's lifetime: a
    /// token issued for no longer than the skew would otherwise be refreshed
    /// on every call. Called with the monitor held.
    private long skew() {
        long half = (current.getExpiresAtMillis() - currentFetchedAt) / 2;
        return half > 0 && half < refreshSkewMillis ? half : refreshSkewMillis;
    }

    private AccessToken refresh() {
        AccessToken fresh = null;
        TokenAcquisitionException failure = null;
        Error fatal = null;
        try {
            fresh = grant.fetch();
        } catch (TokenAcquisitionException err) {
            failure = err;
        } catch (RuntimeException err) {
            // A grant's own bug, or a signing failure: reported without its
            // message, which the provider cannot vouch for.
            // Not even as the cause: a stack trace prints the cause's message.
            failure = new TokenAcquisitionException("The grant failed: "
                    + err.getClass().getName(), -1, null, false, null);
        } catch (Error err) {
            // Recorded like any failure, so the flag below is cleared and the
            // waiters woken; otherwise the provider would wait on a refresh
            // that ended long ago until the process restarts. Then rethrown.
            fatal = err;
            failure = new TokenAcquisitionException("The grant failed: "
                    + err.getClass().getName(), -1, null, false, null);
        }
        synchronized (this) {
            refreshing = false;
            notifyAll();
            long now = clock.currentTimeMillis();
            if (fresh != null) {
                current = fresh;
                currentFetchedAt = now;
                failures = 0;
                lastFailure = null;
                nextAttemptAt = 0;
                return fresh;
            }
            failures++;
            lastFailure = failure;
            nextAttemptAt = now + backoff(failures);
            if (fatal != null) {
                throw fatal;
            }
            if (current != null && !current.isExpired(now)) {
                return current;
            }
            throw failure;
        }
    }

    /// `min(max, 2^(n-1) s)`, minus up to half of it. Jitter keeps a fleet of
    /// servers that failed together from retrying together. Not a security
    /// random: it only spreads load.
    private long backoff(int n) {
        long base = 1000L << Math.min(20, n - 1);
        long capped = Math.min(maxBackoffMillis, base);
        jitterSeed ^= jitterSeed << 13;
        jitterSeed ^= jitterSeed >>> 7;
        jitterSeed ^= jitterSeed << 17;
        long half = capped / 2;
        return capped - (half == 0 ? 0 : Math.abs(jitterSeed % half));
    }

    /// Consecutive failed exchanges since the last success.
    public synchronized int getConsecutiveFailures() {
        return failures;
    }

    @Override
    public String toString() {
        return "AccessTokenProvider[" + grant + "]";
    }
}
