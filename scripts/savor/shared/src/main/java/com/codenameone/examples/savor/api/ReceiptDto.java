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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.Mapped;

/// What an order cost and how it was paid. Every amount is in cents.
///
/// @param subtotal The food, extras included, before the discount.
/// @param smallOrderFee Charged when the subtotal is under the small-order threshold.
/// @param total Everything the card was charged: the subtotal less the discount, the fees,
///     the tax and the tip.
/// @param status `pending`, `paid`, `failed`, `refunded` or `cash`; while a refund the processor
///     took has not finished, `refund_pending`, and `refund_failed` when it then did not return the
///     money.
/// @param paidAt Milliseconds since the epoch, or 0 while unpaid.
/// @param customerUsername Who paid. Filled for an admin; a customer's own receipts leave it empty.
@Mapped
public record ReceiptDto(
        String orderId,
        String currency,
        String restaurantName,
        long subtotal,
        long deliveryFee,
        long serviceFee,
        long smallOrderFee,
        long tax,
        long discount,
        long tip,
        long total,
        String methodLabel,
        String status,
        long paidAt,
        String dropoffName,
        String courierName,
        String vehicle,
        String customerUsername) {
}
