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
package com.codenameone.examples.wayline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// How one person wants the app and their rides, when they have said.
@Entity
public class Preference {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(8)")
    private String theme = "";

    @Column(nullable = false, type = "VARCHAR(4)")
    private String units = "";

    @Column(nullable = false)
    private boolean notifyRides;

    @Column(nullable = false)
    private boolean notifyReceipts;

    @Column(nullable = false)
    private boolean notifyPromotions;

    @Column(nullable = false, type = "VARCHAR(8)")
    private String driverGender = "";

    @Column(nullable = false)
    private boolean quietRide;

    @Column(nullable = false)
    private boolean wheelchair;

    @Column(nullable = false)
    private boolean petFriendly;

    @Column(nullable = false)
    private int defaultTip;

    @Column(nullable = false, type = "VARCHAR(32)")
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
