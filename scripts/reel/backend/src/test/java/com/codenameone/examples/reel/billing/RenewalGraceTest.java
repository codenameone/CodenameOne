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
package com.codenameone.examples.reel.billing;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.reel.domain.Subscription;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A declined renewal waits a day before the card is tried again, so the
/// three tries spread over the grace period instead of three sweeps.
@BackendTest
class RenewalGraceTest {
    @Autowired
    private Billing billing;

    @Test
    void aPastDueSubscriptionIsNotRetriedBeforeItsTime() {
        Instant now = Instant.now();
        Subscription sub = new Subscription();
        sub.setUsername("nobody");
        sub.setPlanCode("basic");
        sub.setStatus(Entitlements.PAST_DUE);
        sub.setPeriodEndsAt(now.minusMillis(60000L));
        sub.setFailedAttempts(1);
        sub.setNextRetryAt(now.plusMillis(Billing.RETRY_MILLIS));
        billing.renewOne(sub, now);
        assertEquals(Entitlements.PAST_DUE, sub.getStatus());
        assertEquals(1, sub.getFailedAttempts());
    }
}
