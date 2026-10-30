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
import java.time.Instant;

/// What an order costs on top of its food, and what the service keeps of it.
/// There is one row; see V6.
@Entity
public class Pricing {
    @Id(autoIncrement = false)
    @Column(nullable = false)
    private int id;

    @Column(nullable = false)
    private long baseCents;

    @Column(nullable = false)
    private long perKmCents;

    @Column(nullable = false)
    private double surge;

    @Column(nullable = false)
    private long smallOrderCents;

    @Column(nullable = false)
    private long smallOrderFeeCents;

    @Column(nullable = false)
    private double feePercent;

    @Column(nullable = false)
    private double taxPercent;

    @Column(nullable = false)
    private double commissionPercent;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    @Column(nullable = false, type = "VARCHAR(190)")
    private String updatedBy = "";

    public Pricing() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBaseCents() {
        return baseCents;
    }

    public void setBaseCents(long baseCents) {
        this.baseCents = baseCents;
    }

    public long getPerKmCents() {
        return perKmCents;
    }

    public void setPerKmCents(long perKmCents) {
        this.perKmCents = perKmCents;
    }

    public double getSurge() {
        return surge;
    }

    public void setSurge(double surge) {
        this.surge = surge;
    }

    public long getSmallOrderCents() {
        return smallOrderCents;
    }

    public void setSmallOrderCents(long smallOrderCents) {
        this.smallOrderCents = smallOrderCents;
    }

    public long getSmallOrderFeeCents() {
        return smallOrderFeeCents;
    }

    public void setSmallOrderFeeCents(long smallOrderFeeCents) {
        this.smallOrderFeeCents = smallOrderFeeCents;
    }

    public double getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(double feePercent) {
        this.feePercent = feePercent;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }

    public double getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(double commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
