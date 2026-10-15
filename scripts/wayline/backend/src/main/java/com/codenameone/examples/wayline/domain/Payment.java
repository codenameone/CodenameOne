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
