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
package com.codename1.ai;

import java.util.Random;

/// Decides whether and how long to wait before retrying a failed
/// [LlmClient] call. Default policy retries [LlmRateLimitException]
/// (honouring `Retry-After`) and [LlmModelOverloadedException] with
/// exponential backoff + jitter; other failures are surfaced
/// immediately.
///
/// Wire a policy onto a request like this:
///
/// ```
/// AsyncResource<ChatResponse> r = RetryPolicy.exponentialBackoff()
///         .runChat(client, request);
/// ```
///
/// The synchronous block runs on the calling thread. On the EDT it
/// uses `Display.invokeAndBlock` automatically so the UI stays
/// responsive between attempts.
public final class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    private final boolean jitter;

    private static final Random RNG = new Random();

    private RetryPolicy(int maxAttempts, long initialDelayMs, long maxDelayMs,
                        double multiplier, boolean jitter) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialDelayMs = Math.max(1, initialDelayMs);
        this.maxDelayMs = Math.max(initialDelayMs, maxDelayMs);
        this.multiplier = Math.max(1.0, multiplier);
        this.jitter = jitter;
    }

    /// 4 attempts, starting at 500 ms, doubling, capped at 30 s, with
    /// jitter. Good default for chat workloads.
    public static RetryPolicy exponentialBackoff() {
        return new RetryPolicy(4, 500L, 30000L, 2.0, true);
    }

    /// No retries -- failures are returned to the caller as-is.
    public static RetryPolicy none() {
        return new RetryPolicy(1, 0L, 0L, 1.0, false);
    }

    public static RetryPolicy custom(int maxAttempts, long initialDelayMs,
                                     long maxDelayMs, double multiplier, boolean jitter) {
        return new RetryPolicy(maxAttempts, initialDelayMs, maxDelayMs, multiplier, jitter);
    }

    /// Inspect a thrown exception and decide whether to retry. Apps
    /// can override to add provider-specific rules (e.g. retry on a
    /// custom 5xx code).
    public boolean shouldRetry(Throwable t, int attemptsSoFar) {
        if (attemptsSoFar >= maxAttempts) {
            return false;
        }
        if (t instanceof LlmRateLimitException) {
            return true;
        }
        if (t instanceof LlmModelOverloadedException) {
            return true;
        }
        if (t instanceof LlmServerException) {
            // 5xx server errors typically reflect transient state.
            return true;
        }
        return t instanceof LlmNetworkException;
    }

    /// Returns the delay to wait before the next attempt, honouring
    /// `Retry-After` from rate-limit exceptions when present.
    public long computeDelayMs(Throwable t, int attemptIndex /* 0-based */) {
        if (t instanceof LlmRateLimitException) {
            int retryAfter = ((LlmRateLimitException) t).getRetryAfterSeconds();
            if (retryAfter > 0) {
                return retryAfter * 1000L;
            }
        }
        double delay = initialDelayMs;
        for (int i = 0; i < attemptIndex; i++) {
            delay *= multiplier;
            if (delay >= maxDelayMs) {
                delay = maxDelayMs;
                break;
            }
        }
        if (jitter) {
            // Full jitter: pick a random value in [0, delay].
            // Keeps thundering-herd risk down on shared endpoints.
            delay = RNG.nextDouble() * delay;
        }
        return (long) delay;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
