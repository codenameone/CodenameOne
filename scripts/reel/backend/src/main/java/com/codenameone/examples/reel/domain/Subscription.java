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
package com.codenameone.examples.reel.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// An account's subscription. One row per account, kept after it ends.
@Entity
public class Subscription {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String planCode = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false)
    private long startedAt;

    @Column(nullable = false)
    private long periodEndsAt;

    @Column(nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String pendingPlanCode = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String couponCode = "";

    @Column(nullable = false)
    private int couponPeriodsLeft;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String paymentMethodId = "";

    @Column(nullable = false)
    private int failedAttempts;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String store = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String storeRef = "";

    @Column(nullable = false)
    private long updatedAt;

    public Subscription() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getPeriodEndsAt() {
        return periodEndsAt;
    }

    public void setPeriodEndsAt(long periodEndsAt) {
        this.periodEndsAt = periodEndsAt;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public String getPendingPlanCode() {
        return pendingPlanCode;
    }

    public void setPendingPlanCode(String pendingPlanCode) {
        this.pendingPlanCode = pendingPlanCode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public int getCouponPeriodsLeft() {
        return couponPeriodsLeft;
    }

    public void setCouponPeriodsLeft(int couponPeriodsLeft) {
        this.couponPeriodsLeft = couponPeriodsLeft;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getStoreRef() {
        return storeRef;
    }

    public void setStoreRef(String storeRef) {
        this.storeRef = storeRef;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
