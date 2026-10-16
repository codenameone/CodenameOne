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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

/// A ride, as each party to it sees it. The same object answers the rider, the
/// driver and the admin; nothing in it is private to one of them.
///
/// @param state NONE means "there is no such ride", which is how a question like "what is my active
///     ride?" is answered when there is not one.
/// @param driverLat Where the driver was last seen. Meaningful once a driver is attached.
/// @param driverLng See [#driverLat()].
/// @param driverHeading The way the driver's car points, in degrees clockwise from north.
/// @param fareCents The fare, before the service fee and any tip.
/// @param requestedAt Milliseconds since the epoch.
/// @param updatedAt Milliseconds since the epoch.
/// @param rating The rider's rating of the ride, 1 to 5, or 0 before there is one.
/// @param paymentStatus PENDING until the ride completes, then PAID or FAILED; REFUNDED once an
///     admin has returned the money. A cash ride is PAID when it completes. A refund the processor
///     has not finished is REFUND_PENDING until it ends REFUNDED, or REFUND_FAILED when the money
///     did not go back.
/// @param offerExpiresInSeconds For a driver looking at an offer: how long is left to answer it.
/// @param product `standard`, `comfort` or `xl`.
/// @param paymentMethodLabel How the ride is paid for, as the rider would recognise it: "Cash",
///     "Visa 4242".
/// @param cash True when the rider pays the driver in cash, so that the driver's screen can say to
///     collect it. The label above is for people and is not compared.
/// @param serviceFee Cents, like every amount here.
/// @param total What the rider pays: the fare, the service fee and the tip.
/// @param driverPhotoInitials The driver's initials, for a badge where a photo would go.
/// @param etaMinutes Minutes until the driver reaches the pickup, or the drop-off once the trip has
///     started; 0 when there is nothing to estimate.
/// @param driverGender What the rider asked for, for the driver to read: this and the four that
///     follow.
/// @param driverPhone The number to call or text the driver at, given to the rider. Filled only
///     while a driver has the ride and it is not over, and never for anyone else.
/// @param riderPhone The number to call or text the rider at, given to the driver, on the same
///     terms as [#driverPhone()].
@Mapped
public record RideDto(
        String id,
        RideState state,
        String riderUsername,
        String riderName,
        String driverUsername,
        String driverName,
        String vehicle,
        String plate,
        double driverRating,
        double pickupLat,
        double pickupLng,
        String pickupAddress,
        double dropoffLat,
        double dropoffLng,
        String dropoffAddress,
        double driverLat,
        double driverLng,
        double driverHeading,
        long fareCents,
        String currency,
        double distanceMeters,
        double durationSeconds,
        long requestedAt,
        long updatedAt,
        int rating,
        String paymentStatus,
        int offerExpiresInSeconds,
        String product,
        String paymentMethodLabel,
        boolean cash,
        long serviceFee,
        long tip,
        long total,
        String driverPhotoInitials,
        int etaMinutes,
        String driverGender,
        boolean quietRide,
        boolean accessibleVehicle,
        boolean petFriendly,
        String note,
        String driverPhone,
        String riderPhone) {

    /// The answer when there is no ride to give: a question like "what is my
    /// active ride?" asked by someone who has none. Its state is
    /// [RideState#NONE] and every other component is empty.
    public static RideDto none() {
        return new RideDto(null, RideState.NONE, null, null, null, null, null, null, 0d, 0d, 0d,
                null, 0d, 0d, null, 0d, 0d, 0d, 0L, null, 0d, 0d, 0L, 0L, 0, null, 0, null, null,
                false, 0L, 0L, 0L, null, 0, null, false, false, false, null, null, null);
    }
}
