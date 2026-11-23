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

/// A profit and loss statement for a period, every amount in the store's
/// currency, in cents. Net sales are gross sales less discounts and the
/// merchandise part of refunds; cost of goods is each unit kept (sold less
/// refunded) at its variant's recorded cost; gross margin is net sales less
/// that cost. Shipping and tax are what shoppers paid and are not sales.
/// `uncostedUnits` counts kept units whose variant has no cost recorded, so a
/// margin that looks too good says why. Payment processor fees are not
/// recorded by this sample, so the statement has no fee line.
@Mapped
public record ProfitLossDto(
        String from,
        String to,
        int orders,
        long grossSalesCents,
        long discountsCents,
        long refundsCents,
        long netSalesCents,
        long cogsCents,
        long grossMarginCents,
        int marginBp,
        long shippingCents,
        long taxCents,
        int uncostedUnits) {
}
