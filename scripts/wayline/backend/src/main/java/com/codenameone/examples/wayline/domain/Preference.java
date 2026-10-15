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
@Entity
public class Preference {
    @Id(autoIncrement = false)
    @Column(nullable = false)
    private String username = "";

    @Column(nullable = false)
    private String theme = "";

    @Column(nullable = false)
    private String units = "";

    @Column(nullable = false)
    private boolean notifyRides;

    @Column(nullable = false)
    private boolean notifyReceipts;

    @Column(nullable = false)
    private boolean notifyPromotions;

    @Column(nullable = false)
    private String driverGender = "";

    @Column(nullable = false)
    private boolean quietRide;

    @Column(nullable = false)
    private boolean wheelchair;

    @Column(nullable = false)
    private boolean petFriendly;

    @Column(nullable = false)
    private int defaultTip;

    @Column(nullable = false)
    private String shareTripsWith = "";

    public Preference() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public boolean isNotifyRides() {
        return notifyRides;
    }

    public void setNotifyRides(boolean notifyRides) {
        this.notifyRides = notifyRides;
    }

    public boolean isNotifyReceipts() {
        return notifyReceipts;
    }

    public void setNotifyReceipts(boolean notifyReceipts) {
        this.notifyReceipts = notifyReceipts;
    }

    public boolean isNotifyPromotions() {
        return notifyPromotions;
    }

    public void setNotifyPromotions(boolean notifyPromotions) {
        this.notifyPromotions = notifyPromotions;
    }

    public String getDriverGender() {
        return driverGender;
    }

    public void setDriverGender(String driverGender) {
        this.driverGender = driverGender;
    }

    public boolean isQuietRide() {
        return quietRide;
    }

    public void setQuietRide(boolean quietRide) {
        this.quietRide = quietRide;
    }

    public boolean isWheelchair() {
        return wheelchair;
    }

    public void setWheelchair(boolean wheelchair) {
        this.wheelchair = wheelchair;
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public void setPetFriendly(boolean petFriendly) {
        this.petFriendly = petFriendly;
    }

    public int getDefaultTip() {
        return defaultTip;
    }

    public void setDefaultTip(int defaultTip) {
        this.defaultTip = defaultTip;
    }

    public String getShareTripsWith() {
        return shareTripsWith;
    }

    public void setShareTripsWith(String shareTripsWith) {
        this.shareTripsWith = shareTripsWith;
    }
}
