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

/// A delivery, as each party to it sees it. The same object answers the customer, the
/// courier and the admin; nothing in it is private to one of them.
///
/// @param state NONE means "there is no such delivery", which is how a question like "what is my active
///     delivery?" is answered when there is not one.
/// @param courierLat Where the courier was last seen. Meaningful once a courier is attached.
/// @param courierLng See [#courierLat()].
/// @param courierHeading The way the courier's car points, in degrees clockwise from north.
/// @param fareCents The fare, before the service fee and any tip.
/// @param requestedAt Milliseconds since the epoch.
/// @param updatedAt Milliseconds since the epoch.
/// @param rating The customer's rating of the delivery, 1 to 5, or 0 before there is one.
/// @param paymentStatus PENDING until the delivery completes, then PAID or FAILED; REFUNDED once an
///     admin has returned the money. A cash delivery is PAID when it completes. A refund the processor
///     has not finished is REFUND_PENDING until it ends REFUNDED, or REFUND_FAILED when the money
///     did not go back.
/// @param offerExpiresInSeconds For a courier looking at an offer: how long is left to answer it.
/// @param paymentMethodLabel How the delivery is paid for, as the customer would recognise it: "Cash",
///     "Visa 4242".
/// @param cash True when the customer pays the courier in cash, so that the courier's screen can say to
///     collect it. The label above is for people and is not compared.
/// @param serviceFee Cents, like every amount here.
/// @param total What the customer pays: the order's whole total plus any tip given after arrival.
///     A courier collects this for a cash order.
/// @param courierPhotoInitials The courier's initials, for a badge where a photo would go.
/// @param etaMinutes Minutes until the courier reaches the pickup, or the drop-off once the trip has
///     started; 0 when there is nothing to estimate.
/// @param courierPhone The number to call or text the courier at, given to the customer. Filled only
///     while a courier has the delivery and it is not over, and never for anyone else.
/// @param customerPhone The number to call or text the customer at, given to the courier, on the same
///     terms as [#courierPhone()].
@Mapped
public record DeliveryDto(
        String id,
        DeliveryState state,
        String customerUsername,
        String customerName,
        String courierUsername,
        String courierName,
        String vehicle,
        String plate,
        double courierRating,
        double pickupLat,
        double pickupLng,
        String pickupAddress,
        double dropoffLat,
        double dropoffLng,
        String dropoffAddress,
        double courierLat,
        double courierLng,
        double courierHeading,
        long fareCents,
        String currency,
        double distanceMeters,
        double durationSeconds,
        long requestedAt,
        long updatedAt,
        int rating,
        String paymentStatus,
        int offerExpiresInSeconds,
        String paymentMethodLabel,
        boolean cash,
        long serviceFee,
        long tip,
        long total,
        String courierPhotoInitials,
        int etaMinutes,
        String note,
        String courierPhone,
        String customerPhone) {

    /// The answer when there is no delivery to give: a question like "what is my
    /// active delivery?" asked by someone who has none. Its state is
    /// [DeliveryState#NONE] and every other component is empty.
    public static DeliveryDto none() {
        return new DeliveryDto(null, DeliveryState.NONE, null, null, null, null, null, null, 0d, 0d, 0d,
                null, 0d, 0d, null, 0d, 0d, 0d, 0L, null, 0d, 0d, 0L, 0L, 0, null, 0, null,
                false, 0L, 0L, 0L, null, 0, null, null, null);
    }
}
