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
package com.codenameone.examples.savor.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import com.codenameone.examples.savor.api.DeliveryState;
import java.time.Instant;

// tag::savor-entity[]
/// A delivery, from the request to its end.
///
/// `customer` and `courier` are account names and not references: a delivery outlives the
/// accounts that took it, and `courier` is empty until one is offered it.
@Entity
public class Delivery {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(24)")
    private DeliveryState state = DeliveryState.REQUESTED;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String customer = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String courier = "";
    // end::savor-entity[]

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

    @Column(nullable = false, type = "VARCHAR(32)")
    private String methodId = "cash";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String methodLabel = "Cash";

    @Column(nullable = false)
    private long feeCents;

    @Column(nullable = false)
    private long tipCents;

    @Column(nullable = false)
    private long courierCents;

    @Column(nullable = false, type = "VARCHAR(255)")
    private String note = "";

    public Delivery() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DeliveryState getState() {
        return state;
    }

    public void setState(DeliveryState state) {
        this.state = state;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getCourier() {
        return courier;
    }

    public void setCourier(String courier) {
        this.courier = courier;
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

    public long getCourierCents() {
        return courierCents;
    }

    public void setCourierCents(long courierCents) {
        this.courierCents = courierCents;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
