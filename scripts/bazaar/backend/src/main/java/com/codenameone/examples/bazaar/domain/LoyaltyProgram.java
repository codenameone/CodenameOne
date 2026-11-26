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

package com.codenameone.examples.bazaar.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import java.time.Instant;

/// A store's points programme: how points are earned, what they are worth and when they lapse.
@Entity
public class LoyaltyProgram {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int pointsPerUnit;

    @Column(nullable = false)
    private long unitCents;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String rounding = "";

    @Column(nullable = false)
    private int pointsPerReward;

    @Column(nullable = false)
    private long rewardCents;

    @Column(nullable = false)
    private int minRedeemPoints;

    @Column(nullable = false)
    private int expiryDays;

    @Column(nullable = false)
    private boolean earnOnPos;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String tierBasis = "";

    @Column(nullable = false)
    private int referrerPoints;

    @Column(nullable = false)
    private int refereePoints;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public LoyaltyProgram() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPointsPerUnit() {
        return pointsPerUnit;
    }

    public void setPointsPerUnit(int pointsPerUnit) {
        this.pointsPerUnit = pointsPerUnit;
    }

    public long getUnitCents() {
        return unitCents;
    }

    public void setUnitCents(long unitCents) {
        this.unitCents = unitCents;
    }

    public String getRounding() {
        return rounding;
    }

    public void setRounding(String rounding) {
        this.rounding = rounding;
    }

    public int getPointsPerReward() {
        return pointsPerReward;
    }

    public void setPointsPerReward(int pointsPerReward) {
        this.pointsPerReward = pointsPerReward;
    }

    public long getRewardCents() {
        return rewardCents;
    }

    public void setRewardCents(long rewardCents) {
        this.rewardCents = rewardCents;
    }

    public int getMinRedeemPoints() {
        return minRedeemPoints;
    }

    public void setMinRedeemPoints(int minRedeemPoints) {
        this.minRedeemPoints = minRedeemPoints;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(int expiryDays) {
        this.expiryDays = expiryDays;
    }

    public boolean isEarnOnPos() {
        return earnOnPos;
    }

    public void setEarnOnPos(boolean earnOnPos) {
        this.earnOnPos = earnOnPos;
    }

    public String getTierBasis() {
        return tierBasis;
    }

    public void setTierBasis(String tierBasis) {
        this.tierBasis = tierBasis;
    }

    public int getReferrerPoints() {
        return referrerPoints;
    }

    public void setReferrerPoints(int referrerPoints) {
        this.referrerPoints = referrerPoints;
    }

    public int getRefereePoints() {
        return refereePoints;
    }

    public void setRefereePoints(int refereePoints) {
        this.refereePoints = refereePoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
