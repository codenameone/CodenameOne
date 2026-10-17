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
package com.codenameone.examples.morsel.api;

import com.codename1.annotations.Mapped;

/// What deliveries cost. Amounts are cents; the percentages are of the fare.
///
/// @param serviceFeePercent Added to the fare and paid by the customer.
/// @param commissionPercent Kept from the fare; the rest is the courier's.
/// @param surgeMultiplier Multiplies every fare while demand is high; 1 otherwise.
@Mapped
public record PricingDto(
        String currency,
        long baseFare,
        long perKm,
        long perMinute,
        long minimumFare,
        double serviceFeePercent,
        double commissionPercent,
        double surgeMultiplier,
        double comfortMultiplier,
        double xlMultiplier) {
}
