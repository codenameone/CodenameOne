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

/// The headline numbers of a period.
///
/// @param grossSalesCents Line subtotals before discounts of orders that were not cancelled.
/// @param refundsCents Refunds recorded in the period, whenever their order was placed.
/// @param netSalesCents Gross less discounts less refunds.
/// @param averageOrderCents Gross less discounts, per order.
/// @param customers Distinct customers who ordered.
/// @param returningCustomerBp Basis points of those customers who had ordered before, or ordered twice.
/// @param carts Carts opened in the period.
/// @param conversionBp Online orders per cart opened, in basis points.
@Mapped
public record KpiDto(
        long grossSalesCents,
        long discountsCents,
        long refundsCents,
        long netSalesCents,
        long taxCents,
        long shippingCents,
        int orders,
        long averageOrderCents,
        int customers,
        int returningCustomerBp,
        int carts,
        int conversionBp) {
}
