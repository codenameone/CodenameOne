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

/// The prices an admin set. One row, with the id 1, once they have set any.
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
    private long perMinuteCents;

    @Column(nullable = false)
    private long minimumCents;

    @Column(nullable = false)
    private double feePercent;

    @Column(nullable = false)
    private double commissionPercent;

    @Column(nullable = false)
    private double surge;

    @Column(nullable = false)
    private double comfort;

    @Column(nullable = false)
    private double xl;

    @Column(nullable = false)
    private long updatedAt;

    @Column(nullable = false)
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

    public long getPerMinuteCents() {
        return perMinuteCents;
    }

    public void setPerMinuteCents(long perMinuteCents) {
        this.perMinuteCents = perMinuteCents;
    }

    public long getMinimumCents() {
        return minimumCents;
    }

    public void setMinimumCents(long minimumCents) {
        this.minimumCents = minimumCents;
    }

    public double getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(double feePercent) {
        this.feePercent = feePercent;
    }

    public double getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(double commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public double getSurge() {
        return surge;
    }

    public void setSurge(double surge) {
        this.surge = surge;
    }

    public double getComfort() {
        return comfort;
    }

    public void setComfort(double comfort) {
        this.comfort = comfort;
    }

    public double getXl() {
        return xl;
    }

    public void setXl(double xl) {
        this.xl = xl;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
