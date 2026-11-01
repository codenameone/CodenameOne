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
import com.codenameone.examples.wayline.api.RideState;
import java.time.Instant;

// tag::wayline-entity[]
/// A ride, from the request to its end.
///
/// `rider` and `driver` are account names and not references: a ride outlives the
/// accounts that took it, and `driver` is empty until one is offered it.
@Entity
public class Ride {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(24)")
    private RideState state = RideState.REQUESTED;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String rider = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String driver = "";
    // end::wayline-entity[]

    @Column(nullable = false)
    private double pickupLat;

    @Column(nullable = false)
    private double pickupLng;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String pickupAddress = "";

    @Column(nullable = false)
    private double dropoffLat;

    @Column(nullable = false)
    private double dropoffLng;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String dropoffAddress = "";

    @Column(nullable = false)
    private long fareCents;

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false)
    private double distanceMeters;

    @Column(nullable = false)
    private double durationSeconds;

    @Column(nullable = false)
    private Instant requestedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant offerExpiresAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant searchUntil = Instant.EPOCH;

    @Column(nullable = false, type = "SMALLINT")
    private int stars;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String paymentStatus = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String product = "standard";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String methodId = "cash";

    @Column(nullable = false, type = "VARCHAR(64)")
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

    // tag::wayline-column-entity[]
    @Column(nullable = false)
    private boolean pets;
    // end::wayline-column-entity[]

    @Column(nullable = false, type = "VARCHAR(255)")
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

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public void setOfferExpiresAt(Instant offerExpiresAt) {
        this.offerExpiresAt = offerExpiresAt;
    }

    public Instant getSearchUntil() {
        return searchUntil;
    }

    public void setSearchUntil(Instant searchUntil) {
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
