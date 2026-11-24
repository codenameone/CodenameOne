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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

/// The shopper's standing across every store, for the rewards on the Account
/// tab and the points burst after checkout. `points` is the spendable balance
/// summed over the stores' programmes; the tier fields are those of the store
/// where the shopper has qualified most (`qualifying` reached of
/// `qualifying + toNextTier`). The counts feed the badges. `orderPoints` is
/// what the order asked about earns, or will earn once paid; 0 without one.
@Mapped
public record RewardsDto(
        long points,
        long lifetime,
        String tierName,
        String nextTierName,
        long qualifying,
        long toNextTier,
        int orders,
        int delivered,
        int reviews,
        int stores,
        int wishlist,
        long orderPoints) {
}
