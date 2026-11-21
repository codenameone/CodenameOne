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

/// A level of a loyalty programme, reached by spend or points in the last twelve months.
@Entity
public class LoyaltyTier {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(80)")
    private String name = "";

    @Column(nullable = false)
    private long threshold;

    @Column(nullable = false)
    private int memberBps;

    @Column(nullable = false)
    private boolean freeShipping;

    @Column(nullable = false)
    private int earlyAccessHours;

    @Column(nullable = false, type = "VARCHAR(16)")
    private String colour = "";

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String discountId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String shippingDiscountId = "";

    public LoyaltyTier() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public int getMemberBps() {
        return memberBps;
    }

    public void setMemberBps(int memberBps) {
        this.memberBps = memberBps;
    }

    public boolean isFreeShipping() {
        return freeShipping;
    }

    public void setFreeShipping(boolean freeShipping) {
        this.freeShipping = freeShipping;
    }

    public int getEarlyAccessHours() {
        return earlyAccessHours;
    }

    public void setEarlyAccessHours(int earlyAccessHours) {
        this.earlyAccessHours = earlyAccessHours;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getDiscountId() {
        return discountId;
    }

    public void setDiscountId(String discountId) {
        this.discountId = discountId;
    }

    public String getShippingDiscountId() {
        return shippingDiscountId;
    }

    public void setShippingDiscountId(String shippingDiscountId) {
        this.shippingDiscountId = shippingDiscountId;
    }
}
