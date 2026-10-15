/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
