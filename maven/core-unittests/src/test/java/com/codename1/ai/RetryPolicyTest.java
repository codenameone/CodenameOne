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
package com.codename1.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for {@link RetryPolicy}: factory defaults, the
 * retry-classification rules in {@code shouldRetry}, and the backoff math in
 * {@code computeDelayMs} (deterministic with jitter disabled, bounded with
 * jitter enabled).
 */
class RetryPolicyTest {

    private static LlmException llm(LlmException.ErrorType type) {
        return new LlmException("x", 500, null, null, null, type);
    }

    private static LlmException rateLimited(int retryAfterSeconds) {
        return new LlmException("rl", 429, null, null, null,
                LlmException.ErrorType.RATE_LIMIT, retryAfterSeconds);
    }

    @Test
    void exponentialBackoffDefaultsToFourAttempts() {
        assertEquals(4, RetryPolicy.exponentialBackoff().getMaxAttempts());
    }

    @Test
    void noneMeansASingleAttempt() {
        RetryPolicy p = RetryPolicy.none();
        assertEquals(1, p.getMaxAttempts());
        // With one attempt already spent there is nothing left to retry.
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.SERVER), 1));
    }

    @Test
    void customClampsDegenerateArguments() {
        // maxAttempts floors at 1, multiplier floors at 1.0, delays stay positive.
        RetryPolicy p = RetryPolicy.custom(0, 0L, 0L, 0.0, false);
        assertEquals(1, p.getMaxAttempts());
    }

    @Test
    void shouldRetryStopsWhenAttemptsExhausted() {
        RetryPolicy p = RetryPolicy.custom(3, 100, 1000, 2.0, false);
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.SERVER), 3));
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.SERVER), 4));
    }

    @Test
    void shouldRetryTransientErrorTypes() {
        RetryPolicy p = RetryPolicy.custom(5, 100, 1000, 2.0, false);
        assertTrue(p.shouldRetry(llm(LlmException.ErrorType.RATE_LIMIT), 1));
        assertTrue(p.shouldRetry(llm(LlmException.ErrorType.MODEL_OVERLOADED), 1));
        assertTrue(p.shouldRetry(llm(LlmException.ErrorType.SERVER), 1));
        assertTrue(p.shouldRetry(llm(LlmException.ErrorType.NETWORK), 1));
    }

    @Test
    void shouldNotRetryPermanentErrorTypes() {
        RetryPolicy p = RetryPolicy.custom(5, 100, 1000, 2.0, false);
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.AUTH), 1));
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.INVALID_REQUEST), 1));
        assertFalse(p.shouldRetry(llm(LlmException.ErrorType.CONTEXT_LENGTH), 1));
    }

    @Test
    void shouldNotRetryNonLlmExceptions() {
        RetryPolicy p = RetryPolicy.custom(5, 100, 1000, 2.0, false);
        assertFalse(p.shouldRetry(new RuntimeException("boom"), 1));
    }

    @Test
    void computeDelayHonoursRetryAfterForRateLimit() {
        RetryPolicy p = RetryPolicy.custom(5, 100, 100000, 2.0, false);
        assertEquals(7000L, p.computeDelayMs(rateLimited(7), 0));
    }

    @Test
    void computeDelayIgnoresRetryAfterWhenNotPositive() {
        RetryPolicy p = RetryPolicy.custom(5, 100, 100000, 2.0, false);
        // retryAfter <= 0 falls through to the exponential schedule (attempt 0 -> initial delay).
        assertEquals(100L, p.computeDelayMs(rateLimited(0), 0));
    }

    @Test
    void computeDelayGrowsExponentiallyWithoutJitter() {
        RetryPolicy p = RetryPolicy.custom(6, 100, 100000, 2.0, false);
        assertEquals(100L, p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 0));
        assertEquals(200L, p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 1));
        assertEquals(400L, p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 2));
        assertEquals(800L, p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 3));
    }

    @Test
    void computeDelayIsCappedAtMaxDelay() {
        RetryPolicy p = RetryPolicy.custom(20, 100, 1000, 2.0, false);
        // 100,200,400,800,1000(capped) -> never exceeds maxDelay.
        assertEquals(1000L, p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 10));
    }

    @Test
    void jitterKeepsDelayWithinBounds() {
        RetryPolicy p = RetryPolicy.custom(10, 1000, 1000, 1.0, true);
        // multiplier 1.0 keeps the pre-jitter delay at 1000; full jitter picks [0,1000].
        for (int i = 0; i < 100; i++) {
            long d = p.computeDelayMs(llm(LlmException.ErrorType.SERVER), 3);
            assertTrue(d >= 0 && d <= 1000, "jittered delay out of bounds: " + d);
        }
    }
}
