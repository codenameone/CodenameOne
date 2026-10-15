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
import com.codenameone.examples.wayline.api.RideState;

/// A ride, from the request to its end.
///
/// `rider` and `driver` are account names and not references: a ride outlives the
/// accounts that took it, and `driver` is empty until one is offered it.
@Entity
public class Ride {
    @Id(autoIncrement = false)
    @Column(nullable = false)
    private String id = "";

    @Column(nullable = false)
    private RideState state = RideState.REQUESTED;

    @Column(nullable = false)
    private String rider = "";

    @Column(nullable = false)
    private String driver = "";

    @Column(nullable = false)
    private double pickupLat;

    @Column(nullable = false)
    private double pickupLng;

    @Column(nullable = false)
    private String pickupAddress = "";

    @Column(nullable = false)
    private double dropoffLat;

    @Column(nullable = false)
    private double dropoffLng;

    @Column(nullable = false)
    private String dropoffAddress = "";

    @Column(nullable = false)
    private long fareCents;

    @Column(nullable = false)
    private String currency = "";

    @Column(nullable = false)
    private double distanceMeters;

    @Column(nullable = false)
    private double durationSeconds;

    @Column(nullable = false)
    private long requestedAt;

    @Column(nullable = false)
    private long updatedAt;

    @Column(nullable = false)
    private long offerExpiresAt;

    @Column(nullable = false)
    private long searchUntil;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = false)
    private String paymentStatus = "";

    @Column(nullable = false)
    private String product = "standard";

    @Column(nullable = false)
    private String methodId = "cash";

    @Column(nullable = false)
    private String methodLabel = "Cash";

    @Column(nullable = false)
    private long baseCents;

    @Column(nullable = false)
    private long distanceCents;

    @Column(nullable = false)
    private long timeCents;

    @Column(nullable = false)
    private long feeCents;

    @Column(nullable = false)
    private long tipCents;

    @Column(nullable = false)
    private long driverCents;

    @Column(nullable = false)
    private boolean womenOnly;

    @Column(nullable = false)
    private boolean quiet;

    @Column(nullable = false)
    private boolean wheelchair;

    @Column(nullable = false)
    private boolean pets;

    @Column(nullable = false)
    private String note = "";

    public Ride() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RideState getState() {
        return state;
    }

    public void setState(RideState state) {
        this.state = state;
    }

    public String getRider() {
        return rider;
    }

    public void setRider(String rider) {
        this.rider = rider;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public double getDropoffLat() {
        return dropoffLat;
    }

    public void setDropoffLat(double dropoffLat) {
        this.dropoffLat = dropoffLat;
    }

    public double getDropoffLng() {
        return dropoffLng;
    }

    public void setDropoffLng(double dropoffLng) {
        this.dropoffLng = dropoffLng;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public long getFareCents() {
        return fareCents;
    }

    public void setFareCents(long fareCents) {
        this.fareCents = fareCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public void setOfferExpiresAt(long offerExpiresAt) {
        this.offerExpiresAt = offerExpiresAt;
    }

    public long getSearchUntil() {
        return searchUntil;
    }

    public void setSearchUntil(long searchUntil) {
        this.searchUntil = searchUntil;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public String getMethodLabel() {
        return methodLabel;
    }

    public void setMethodLabel(String methodLabel) {
        this.methodLabel = methodLabel;
    }

    public long getBaseCents() {
        return baseCents;
    }

    public void setBaseCents(long baseCents) {
        this.baseCents = baseCents;
    }

    public long getDistanceCents() {
        return distanceCents;
    }

    public void setDistanceCents(long distanceCents) {
        this.distanceCents = distanceCents;
    }

    public long getTimeCents() {
        return timeCents;
    }

    public void setTimeCents(long timeCents) {
        this.timeCents = timeCents;
    }

    public long getFeeCents() {
        return feeCents;
    }

    public void setFeeCents(long feeCents) {
        this.feeCents = feeCents;
    }

    public long getTipCents() {
        return tipCents;
    }

    public void setTipCents(long tipCents) {
        this.tipCents = tipCents;
    }

    public long getDriverCents() {
        return driverCents;
    }

    public void setDriverCents(long driverCents) {
        this.driverCents = driverCents;
    }

    public boolean isWomenOnly() {
        return womenOnly;
    }

    public void setWomenOnly(boolean womenOnly) {
        this.womenOnly = womenOnly;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public boolean isWheelchair() {
        return wheelchair;
    }

    public void setWheelchair(boolean wheelchair) {
        this.wheelchair = wheelchair;
    }

    public boolean isPets() {
        return pets;
    }

    public void setPets(boolean pets) {
        this.pets = pets;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
