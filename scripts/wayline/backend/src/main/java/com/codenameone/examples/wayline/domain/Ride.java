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
package com.codenameone.examples.wayline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// A ride, from the request to its end.
///
/// `rider` and `driver` are account names and not references: a ride outlives the
/// accounts that took it, and `driver` is empty until one is offered it.
@Entity(table = "wl_ride")
public class Ride {
    @Id(autoIncrement = false)
    @Column(name = "id", nullable = false)
    public String id = "";

    @Column(name = "state", nullable = false)
    public String state = "";

    @Column(name = "rider", nullable = false)
    public String rider = "";

    @Column(name = "driver", nullable = false)
    public String driver = "";

    @Column(name = "pickup_lat", nullable = false)
    public double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    public double pickupLng;

    @Column(name = "pickup_address", nullable = false)
    public String pickupAddress = "";

    @Column(name = "dropoff_lat", nullable = false)
    public double dropoffLat;

    @Column(name = "dropoff_lng", nullable = false)
    public double dropoffLng;

    @Column(name = "dropoff_address", nullable = false)
    public String dropoffAddress = "";

    @Column(name = "fare_cents", nullable = false)
    public long fareCents;

    @Column(name = "currency", nullable = false)
    public String currency = "";

    @Column(name = "distance_m", nullable = false)
    public double distanceMeters;

    @Column(name = "duration_s", nullable = false)
    public double durationSeconds;

    @Column(name = "requested_at", nullable = false)
    public long requestedAt;

    @Column(name = "updated_at", nullable = false)
    public long updatedAt;

    @Column(name = "offer_expires_at", nullable = false)
    public long offerExpiresAt;

    @Column(name = "search_until", nullable = false)
    public long searchUntil;

    @Column(name = "stars", nullable = false)
    public int stars;

    @Column(name = "payment_status", nullable = false)
    public String paymentStatus = "";

    @Column(name = "product", nullable = false)
    public String product = "standard";

    @Column(name = "method_id", nullable = false)
    public String methodId = "cash";

    @Column(name = "method_label", nullable = false)
    public String methodLabel = "Cash";

    @Column(name = "base_cents", nullable = false)
    public long baseCents;

    @Column(name = "distance_cents", nullable = false)
    public long distanceCents;

    @Column(name = "time_cents", nullable = false)
    public long timeCents;

    @Column(name = "fee_cents", nullable = false)
    public long feeCents;

    @Column(name = "tip_cents", nullable = false)
    public long tipCents;

    @Column(name = "driver_cents", nullable = false)
    public long driverCents;

    @Column(name = "women_only", nullable = false)
    public boolean womenOnly;

    @Column(name = "quiet", nullable = false)
    public boolean quiet;

    @Column(name = "wheelchair", nullable = false)
    public boolean wheelchair;

    @Column(name = "pets", nullable = false)
    public boolean pets;

    @Column(name = "note", nullable = false)
    public String note = "";

    public Ride() {
    }
}
