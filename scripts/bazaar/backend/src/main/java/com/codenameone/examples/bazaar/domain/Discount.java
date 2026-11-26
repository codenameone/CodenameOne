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

/// A price reduction, entered as a code or applied automatically.
@Entity
public class Discount {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(64)")
    private String code = "";

    @Column(nullable = false, type = "VARCHAR(120)")
    private String title = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String kind = "";

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long minSubtotalCents;

    @Column(nullable = false)
    private int buyQty;

    @Column(nullable = false)
    private int getQty;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String appliesTo = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String targetId = "";

    @Column(nullable = false)
    private boolean automatic;

    @Column(nullable = false)
    private int usageLimit;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean oncePerCustomer;

    @Column(nullable = false)
    private boolean combinable;

    @Column(nullable = false)
    private Instant startsAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant endsAt = Instant.EPOCH;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    /// PRODUCT, ORDER or SHIPPING; empty keeps the older rule, where
    /// `combinable` alone decides.
    @Column(nullable = false, type = "VARCHAR(16)")
    private String discountClass = "";

    @Column(nullable = false)
    private boolean combinesProduct;

    @Column(nullable = false)
    private boolean combinesOrder;

    @Column(nullable = false)
    private boolean combinesShipping;

    /// Who may use it: empty for everyone, `TIER:<id>` or `CLUB:<id>`.
    @Column(nullable = false, type = "VARCHAR(80)")
    private String eligibility = "";

    /// The parameters of TIERED, VOLUME and BUNDLE, as JSON.
    @Column(nullable = false, type = "VARCHAR(2000)")
    private String rules = "";

    /// The discount a bulk-generated single-use code was made from; empty for others.
    @Column(nullable = false, type = "VARCHAR(32)")
    private String batchId = "";

    public Discount() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getMinSubtotalCents() {
        return minSubtotalCents;
    }

    public void setMinSubtotalCents(long minSubtotalCents) {
        this.minSubtotalCents = minSubtotalCents;
    }

    public int getBuyQty() {
        return buyQty;
    }

    public void setBuyQty(int buyQty) {
        this.buyQty = buyQty;
    }

    public int getGetQty() {
        return getQty;
    }

    public void setGetQty(int getQty) {
        this.getQty = getQty;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isOncePerCustomer() {
        return oncePerCustomer;
    }

    public void setOncePerCustomer(boolean oncePerCustomer) {
        this.oncePerCustomer = oncePerCustomer;
    }

    public boolean isCombinable() {
        return combinable;
    }

    public void setCombinable(boolean combinable) {
        this.combinable = combinable;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getDiscountClass() {
        return discountClass;
    }

    public void setDiscountClass(String discountClass) {
        this.discountClass = discountClass;
    }

    public boolean isCombinesProduct() {
        return combinesProduct;
    }

    public void setCombinesProduct(boolean combinesProduct) {
        this.combinesProduct = combinesProduct;
    }

    public boolean isCombinesOrder() {
        return combinesOrder;
    }

    public void setCombinesOrder(boolean combinesOrder) {
        this.combinesOrder = combinesOrder;
    }

    public boolean isCombinesShipping() {
        return combinesShipping;
    }

    public void setCombinesShipping(boolean combinesShipping) {
        this.combinesShipping = combinesShipping;
    }

    public String getEligibility() {
        return eligibility;
    }

    public void setEligibility(String eligibility) {
        this.eligibility = eligibility;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}
