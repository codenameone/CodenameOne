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

import com.codenameone.examples.savor.api.OrderState;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// An order: what a customer bought from one restaurant, what it cost, and where
/// it stands. Its lines, prices and address are copied in when it is placed, so a
/// later change of menu or of prices does not rewrite it. Its delivery, once the
/// restaurant has accepted it, has the same id.
@Entity
public class CustomerOrder {
    @Id(autoIncrement = false)
    @Column(nullable = false)
    private String id = "";

    @Column(nullable = false)
    private String customer = "";

    @Column(nullable = false)
    private String restaurantId = "";

    @Column(nullable = false)
    private String restaurantName = "";

    @Column(nullable = false)
    private OrderState state;

    @Column(nullable = false)
    private long subtotalCents;

    @Column(nullable = false)
    private long deliveryFeeCents;

    @Column(nullable = false)
    private long serviceFeeCents;

    @Column(nullable = false)
    private long smallOrderCents;

    @Column(nullable = false)
    private long taxCents;

    @Column(nullable = false)
    private long discountCents;

    @Column(nullable = false)
    private long tipCents;

    @Column(nullable = false)
    private long totalCents;

    @Column(nullable = false)
    private long commissionCents;

    @Column(nullable = false)
    private String currency = "";

    @Column(nullable = false)
    private String promoCode = "";

    @Column(nullable = false)
    private double dropoffLat;

    @Column(nullable = false)
    private double dropoffLng;

    @Column(nullable = false)
    private String dropoffAddress = "";

    @Column(nullable = false)
    private String note = "";

    @Column(nullable = false)
    private String methodId = "";

    @Column(nullable = false)
    private String methodLabel = "";

    @Column(nullable = false)
    private String authorizationRef = "";

    @Column(nullable = false)
    private String paymentStatus = "";

    @Column(nullable = false)
    private int prepMinutes;

    @Column(nullable = false)
    private long placedAt;

    @Column(nullable = false)
    private long acceptedAt;

    @Column(nullable = false)
    private long readyAt;

    @Column(nullable = false)
    private long deliveredAt;

    @Column(nullable = false)
    private long updatedAt;

    @Column(nullable = false)
    private String reason = "";

    @Column(nullable = false)
    private int stars;

    public CustomerOrder() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public long getDeliveryFeeCents() {
        return deliveryFeeCents;
    }

    public void setDeliveryFeeCents(long deliveryFeeCents) {
        this.deliveryFeeCents = deliveryFeeCents;
    }

    public long getServiceFeeCents() {
        return serviceFeeCents;
    }

    public void setServiceFeeCents(long serviceFeeCents) {
        this.serviceFeeCents = serviceFeeCents;
    }

    public long getSmallOrderCents() {
        return smallOrderCents;
    }

    public void setSmallOrderCents(long smallOrderCents) {
        this.smallOrderCents = smallOrderCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(long discountCents) {
        this.discountCents = discountCents;
    }

    public long getTipCents() {
        return tipCents;
    }

    public void setTipCents(long tipCents) {
        this.tipCents = tipCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public long getCommissionCents() {
        return commissionCents;
    }

    public void setCommissionCents(long commissionCents) {
        this.commissionCents = commissionCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getAuthorizationRef() {
        return authorizationRef;
    }

    public void setAuthorizationRef(String authorizationRef) {
        this.authorizationRef = authorizationRef;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public long getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(long placedAt) {
        this.placedAt = placedAt;
    }

    public long getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(long acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public long getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(long readyAt) {
        this.readyAt = readyAt;
    }

    public long getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }
}
