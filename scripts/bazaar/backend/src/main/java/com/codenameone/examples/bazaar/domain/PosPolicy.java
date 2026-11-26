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

/// A store's point-of-sale rules: when a manager must approve, tip presets and layaway terms.
@Entity
public class PosPolicy {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false)
    private long discountApprovalBp;

    @Column(nullable = false)
    private boolean approvePriceOverrides;

    @Column(nullable = false)
    private boolean approveRefunds;

    @Column(nullable = false)
    private boolean approveOrderEdits;

    @Column(nullable = false, type = "VARCHAR(80)")
    private String tipPresets = "";

    @Column(nullable = false)
    private long layawayDepositBp;

    @Column(nullable = false)
    private long layawayCancelFeeBp;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public PosPolicy() {
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

    public long getDiscountApprovalBp() {
        return discountApprovalBp;
    }

    public void setDiscountApprovalBp(long discountApprovalBp) {
        this.discountApprovalBp = discountApprovalBp;
    }

    public boolean isApprovePriceOverrides() {
        return approvePriceOverrides;
    }

    public void setApprovePriceOverrides(boolean approvePriceOverrides) {
        this.approvePriceOverrides = approvePriceOverrides;
    }

    public boolean isApproveRefunds() {
        return approveRefunds;
    }

    public void setApproveRefunds(boolean approveRefunds) {
        this.approveRefunds = approveRefunds;
    }

    public boolean isApproveOrderEdits() {
        return approveOrderEdits;
    }

    public void setApproveOrderEdits(boolean approveOrderEdits) {
        this.approveOrderEdits = approveOrderEdits;
    }

    public String getTipPresets() {
        return tipPresets;
    }

    public void setTipPresets(String tipPresets) {
        this.tipPresets = tipPresets;
    }

    public long getLayawayDepositBp() {
        return layawayDepositBp;
    }

    public void setLayawayDepositBp(long layawayDepositBp) {
        this.layawayDepositBp = layawayDepositBp;
    }

    public long getLayawayCancelFeeBp() {
        return layawayCancelFeeBp;
    }

    public void setLayawayCancelFeeBp(long layawayCancelFeeBp) {
        this.layawayCancelFeeBp = layawayCancelFeeBp;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
