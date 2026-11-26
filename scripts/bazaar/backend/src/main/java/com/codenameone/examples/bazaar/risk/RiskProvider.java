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

import com.codenameone.examples.bazaar.domain.StoreOrder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An outside opinion of an order's risk: a payment processor's risk
/// signals or a fraud service. [Fraud] asks it once per order, after its own
/// rules, and adds what it answers to the order's score.
///
/// The score is on the rules' scale, 0 to 100: 60 or more holds the order
/// for a review by itself, 30 or more shows it as MEDIUM. A provider that
/// throws or answers null adds nothing but a reason saying it was not
/// reached; checkout never fails because of it.
///
/// The call is made outside any database transaction, so a slow service
/// holds no lock. `bazaar.risk.provider` selects one (`none` by default).
public interface RiskProvider {
    /// What a provider says about one order.
    final class Signal {
        public final int score;
        public final List<String> reasons = new ArrayList<String>();

        public Signal(int score) {
            this.score = Math.max(0, Math.min(100, score));
        }

        /// Adds a reason a merchant reads beside the score.
        public Signal reason(String reason) {
            if (reason != null && reason.trim().length() > 0) {
                reasons.add(reason.trim());
            }
            return this;
        }
    }

    /// The provider that asks nobody.
    RiskProvider NONE = new RiskProvider() {
        @Override
        public String name() {
            return "none";
        }

        @Override
        public Signal assess(StoreOrder order, String clientCountry) {
            return null;
        }
    };

    /// The provider's name, shown before its reasons.
    String name();

    /// The provider's view of `order`, placed from `clientCountry` (or null
    /// when unknown); null for no opinion.
    Signal assess(StoreOrder order, String clientCountry) throws IOException;
}
