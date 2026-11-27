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

/// A store's point-of-sale rules. A manager's PIN is needed for a sale discounted past
/// `discountApprovalBp` of its list price (0: never), for any hand-changed price when
/// `approvePriceOverrides`, for refunds when `approveRefunds` and for order edits when
/// `approveOrderEdits`. The tip presets are basis points of the total (0: unused). A layaway needs
/// a deposit of `layawayDepositBp` of its total and keeps `layawayCancelFeeBp` when cancelled.
@Mapped
public record PosPolicyDto(
        long discountApprovalBp,
        boolean approvePriceOverrides,
        boolean approveRefunds,
        boolean approveOrderEdits,
        long tip1Bp,
        long tip2Bp,
        long tip3Bp,
        long layawayDepositBp,
        long layawayCancelFeeBp) {
}
