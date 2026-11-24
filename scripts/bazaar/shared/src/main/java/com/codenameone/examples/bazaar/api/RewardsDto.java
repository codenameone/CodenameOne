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

/// The shopper's standing, for the rewards on the Account tab and the points
/// burst after checkout. Every store runs its own programme, so points are
/// never summed across stores: `balances` holds one entry per store the
/// shopper is a member of, best first (the one where they have qualified
/// most), and the top-level points and tier fields are that store's
/// (`storeName`; `qualifying` reached of `qualifying + toNextTier`). The
/// counts feed the badges. `orderPoints` is what the loyalty ledger credited
/// for the order asked about (0 without one, or before it is credited), and
/// `orderBalance` the balance at that order's store.
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
        long orderPoints,
        String storeName,
        java.util.List<StoreBalanceDto> balances,
        long orderBalance) {
}
