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

// tag::wayline-contract-dto[]
/// Where a ride starts and ends, and how the rider wants it, for a quote and for
/// the request itself.
///
/// @param distanceMeters The length of the route the app drew, when it has one. The server prices
///     from this and [#durationSeconds()] only while they are plausible for the two points;
///     otherwise it prices from its own estimate.
/// @param durationSeconds How long the app's route takes, when it has one.
/// @param paymentMethodId A saved method's id, or `cash`. Empty means the rider's default method,
///     and cash when there is none.
/// @param product `standard`, `comfort` or `xl`; empty reads as standard.
/// @param driverGender `any` or `women`; empty reads as any.
/// @param quietRide Whether the rider would rather not talk.
/// @param accessibleVehicle Whether the car has to take a wheelchair.
/// @param petFriendly Whether a pet is coming: only drivers who take pets are offered the ride.
/// @param note A few words for the driver: which door, what to look for.
@Mapped
public record RideRequestDto(
        double pickupLat,
        double pickupLng,
        String pickupAddress,
        double dropoffLat,
        double dropoffLng,
        String dropoffAddress,
        double distanceMeters,
        double durationSeconds,
        String paymentMethodId,
        String product,
        String driverGender,
        boolean quietRide,
        boolean accessibleVehicle,
        boolean petFriendly,
        String note) {
}
// end::wayline-contract-dto[]
