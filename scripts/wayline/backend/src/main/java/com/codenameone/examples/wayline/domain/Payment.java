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

/// What a completed ride was charged, and what was given back.
@Entity
public class Payment {
    @Id(autoIncrement = false)
    @Column(nullable = false)
    private String rideId = "";

    @Column(nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private String currency = "";

    @Column(nullable = false)
    private String status = "";

    @Column(nullable = false)
    private String providerRef = "";

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private String tipRef = "";

    @Column(nullable = false)
    private String refundRef = "";

    @Column(nullable = false)
    private String tipRefundRef = "";

    @Column(nullable = false)
    private String refundReason = "";

    @Column(nullable = false)
    private long refundedAt;

    public Payment() {
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public void setProviderRef(String providerRef) {
        this.providerRef = providerRef;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTipRef() {
        return tipRef;
    }

    public void setTipRef(String tipRef) {
        this.tipRef = tipRef;
    }

    public String getRefundRef() {
        return refundRef;
    }

    public void setRefundRef(String refundRef) {
        this.refundRef = refundRef;
    }

    public String getTipRefundRef() {
        return tipRefundRef;
    }

    public void setTipRefundRef(String tipRefundRef) {
        this.tipRefundRef = tipRefundRef;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public long getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(long refundedAt) {
        this.refundedAt = refundedAt;
    }
}
