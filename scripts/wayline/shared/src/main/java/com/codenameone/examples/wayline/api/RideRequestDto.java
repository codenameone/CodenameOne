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

/// Where a ride starts and ends, and how the rider wants it, for a quote and for
/// the request itself.
@Mapped
public class RideRequestDto {
    public double pickupLat;
    public double pickupLng;
    public String pickupAddress;
    public double dropoffLat;
    public double dropoffLng;
    public String dropoffAddress;
    /// The length and duration of the route the app drew, when it has one. The
    /// server prices from these only while they are plausible for the two points;
    /// otherwise it prices from its own estimate.
    public double distanceMeters;
    public double durationSeconds;
    /// A saved method's id, or `cash`. Empty means the rider's default method, and
    /// cash when there is none.
    public String paymentMethodId;
    /// `standard`, `comfort` or `xl`; empty reads as standard.
    public String product;
    /// `any` or `women`; empty reads as any.
    public String driverGender;
    public boolean quietRide;
    public boolean accessibleVehicle;
    /// Whether a pet is coming: only drivers who take pets are offered the ride.
    public boolean petFriendly;
    /// A few words for the driver: which door, what to look for.
    public String note;

    public RideRequestDto() {
    }
}
