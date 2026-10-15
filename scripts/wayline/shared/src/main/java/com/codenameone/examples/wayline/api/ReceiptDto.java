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

/// What a ride cost and how it was paid. Every amount is in cents.
///
/// @param baseFare The part of the fare that does not depend on the trip.
/// @param distanceFare The part of the fare charged by distance.
/// @param timeFare The part of the fare charged by time.
/// @param total The fare, the service fee and the tip, less the discount.
/// @param status `pending`, `paid`, `failed`, `refunded` or `cash`; while a refund the processor
///     took has not finished, `refund_pending`, and `refund_failed` when it then did not return the
///     money.
/// @param paidAt Milliseconds since the epoch, or 0 while unpaid.
/// @param riderUsername Who paid. Filled for an admin; a rider's own receipts leave it empty.
@Mapped
public record ReceiptDto(
        String rideId,
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
        String driverName,
        String vehicle,
        String riderUsername) {
}
