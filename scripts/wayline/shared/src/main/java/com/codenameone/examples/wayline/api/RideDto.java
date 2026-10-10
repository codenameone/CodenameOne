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

/// A ride, as each party to it sees it. The same object answers the rider, the
/// driver and the admin; nothing in it is private to one of them.
@Mapped
public class RideDto {
    public String id;
    /// One of the names in RideStates. NONE means "there is no such ride", which is
    /// how a question like "what is my active ride?" is answered when there is not one.
    public String state;
    public String riderUsername;
    public String riderName;
    public String driverUsername;
    public String driverName;
    public String vehicle;
    public String plate;
    public double driverRating;
    public double pickupLat;
    public double pickupLng;
    public String pickupAddress;
    public double dropoffLat;
    public double dropoffLng;
    public String dropoffAddress;
    /// Where the driver was last seen. Meaningful once a driver is attached.
    public double driverLat;
    public double driverLng;
    public double driverHeading;
    /// The fare, before the service fee and any tip.
    public long fareCents;
    public String currency;
    public double distanceMeters;
    public double durationSeconds;
    /// Milliseconds since the epoch.
    public long requestedAt;
    public long updatedAt;
    /// The rider's rating of the ride, 1 to 5, or 0 before there is one.
    public int rating;
    /// PENDING until the ride completes, then PAID or FAILED; REFUNDED once an
    /// admin has returned the money. A cash ride is PAID when it completes.
    public String paymentStatus;
    /// For a driver looking at an offer: how long is left to answer it.
    public int offerExpiresInSeconds;
    /// `standard`, `comfort` or `xl`.
    public String product;
    /// How the ride is paid for, as the rider would recognise it: "Cash", "Visa 4242".
    public String paymentMethodLabel;
    /// True when the rider pays the driver in cash, so that the driver's screen
    /// can say to collect it. The label above is for people and is not compared.
    public boolean cash;
    /// Cents, like every amount here. [#total] is what the rider pays: the fare,
    /// the service fee and the tip.
    public long serviceFee;
    public long tip;
    public long total;
    /// The driver's initials, for a badge where a photo would go.
    public String driverPhotoInitials;
    /// Minutes until the driver reaches the pickup, or the drop-off once the trip
    /// has started; 0 when there is nothing to estimate.
    public int etaMinutes;
    /// What the rider asked for, for the driver to read.
    public String driverGender;
    public boolean quietRide;
    public boolean accessibleVehicle;
    public boolean petFriendly;
    public String note;
    /// The number to call or text the other party at: the driver's, when the
    /// rider asks; the rider's, when the driver does. Filled only while a
    /// driver has the ride and it is not over, and never for anyone else.
    public String driverPhone;
    public String riderPhone;

    public RideDto() {
    }
}
