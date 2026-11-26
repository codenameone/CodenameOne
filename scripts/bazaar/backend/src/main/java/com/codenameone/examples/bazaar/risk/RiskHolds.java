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
package com.codenameone.examples.bazaar.risk;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.CheckoutFailure;
import com.codenameone.examples.bazaar.domain.OrderRisk;
import com.codenameone.examples.bazaar.repository.CheckoutFailureRepository;
import com.codenameone.examples.bazaar.repository.OrderRiskRepository;
import java.io.IOException;
import java.time.Instant;

/// The two things other services ask of fraud screening, kept apart from
/// [Fraud] so that fulfilment and checkout can depend on it without a cycle:
/// whether an order's fulfilment waits for a person, and recording a
/// refused payment for the next score's failed-attempts rule.
@Component
public class RiskHolds {
    private final OrderRiskRepository risks;
    private final CheckoutFailureRepository failures;

    public RiskHolds(OrderRiskRepository risks, CheckoutFailureRepository failures) {
        this.risks = risks;
        this.failures = failures;
    }

    /// Refuses with 409 while `orderId` is held for review.
    public void requireReleased(String orderId) throws IOException {
        OrderRisk r = risks.findByOrderId(orderId);
        if (r != null && Fraud.HELD.equals(r.getStatus())) {
            throw new ResponseStatusException(409, "The order is held for a fraud review; "
                    + "approve or cancel it first");
        }
    }

    /// Records that `username`'s payment at a store was refused.
    public void recordFailure(String storeId, String username) throws IOException {
        if (storeId == null || username == null) {
            return;
        }
        CheckoutFailure f = new CheckoutFailure();
        f.setId(Ids.next());
        f.setStoreId(storeId);
        f.setUsername(username);
        f.setCreatedAt(Instant.now());
        failures.insert(f);
    }
}
