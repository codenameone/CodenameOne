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

/// What a delivery cost and how it was paid. Every amount is in cents.
///
/// @param baseFare The part of the fare that does not depend on the trip.
/// @param distanceFare The part of the fare charged by distance.
/// @param timeFare The part of the fare charged by time.
/// @param total The fare, the service fee and the tip, less the discount.
/// @param status `pending`, `paid`, `failed`, `refunded` or `cash`; while a refund the processor
///     took has not finished, `refund_pending`, and `refund_failed` when it then did not return the
///     money.
/// @param paidAt Milliseconds since the epoch, or 0 while unpaid.
/// @param customerUsername Who paid. Filled for an admin; a customer's own receipts leave it empty.
@Mapped
public record ReceiptDto(
        String deliveryId,
        String currency,
        long baseFare,
        long distanceFare,
        long timeFare,
        long serviceFee,
        long tip,
        long discount,
        long total,
        String methodLabel,
        String status,
        long paidAt,
        String pickupName,
        String dropoffName,
        double distanceMeters,
        double durationSeconds,
        String courierName,
        String vehicle,
        String customerUsername) {
}
