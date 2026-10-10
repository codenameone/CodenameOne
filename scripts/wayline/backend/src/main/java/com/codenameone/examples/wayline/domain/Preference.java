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

/// How one person wants the app and their rides, when they have said.
@Entity(table = "wl_preferences")
public class Preference {
    @Id(autoIncrement = false)
    @Column(name = "username", nullable = false)
    public String username = "";

    @Column(name = "theme", nullable = false)
    public String theme = "";

    @Column(name = "units", nullable = false)
    public String units = "";

    @Column(name = "notify_rides", nullable = false)
    public boolean notifyRides;

    @Column(name = "notify_receipts", nullable = false)
    public boolean notifyReceipts;

    @Column(name = "notify_promotions", nullable = false)
    public boolean notifyPromotions;

    @Column(name = "driver_gender", nullable = false)
    public String driverGender = "";

    @Column(name = "quiet_ride", nullable = false)
    public boolean quietRide;

    @Column(name = "wheelchair", nullable = false)
    public boolean wheelchair;

    @Column(name = "pet_friendly", nullable = false)
    public boolean petFriendly;

    @Column(name = "default_tip", nullable = false)
    public int defaultTip;

    @Column(name = "share_trips_with", nullable = false)
    public String shareTripsWith = "";

    public Preference() {
    }
}
