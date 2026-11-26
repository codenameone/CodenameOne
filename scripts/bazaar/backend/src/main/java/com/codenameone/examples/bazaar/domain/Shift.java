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

/// A cashier's session at a register, from opening float to cash count.
@Entity
public class Shift {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String registerId = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String username = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String status = "";

    @Column(nullable = false)
    private long openingCents;

    @Column(nullable = false)
    private long cashSalesCents;

    @Column(nullable = false)
    private long cardSalesCents;

    @Column(nullable = false)
    private long otherSalesCents;

    @Column(nullable = false)
    private long cashMovesCents;

    @Column(nullable = false)
    private long expectedCents;

    @Column(nullable = false)
    private long countedCents;

    @Column(nullable = false)
    private int salesCount;

    @Column(nullable = false, type = "VARCHAR(500)")
    private String note = "";

    @Column(nullable = false)
    private Instant openedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant closedAt = Instant.EPOCH;

    public Shift() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getOpeningCents() {
        return openingCents;
    }

    public void setOpeningCents(long openingCents) {
        this.openingCents = openingCents;
    }

    public long getCashSalesCents() {
        return cashSalesCents;
    }

    public void setCashSalesCents(long cashSalesCents) {
        this.cashSalesCents = cashSalesCents;
    }

    public long getCardSalesCents() {
        return cardSalesCents;
    }

    public void setCardSalesCents(long cardSalesCents) {
        this.cardSalesCents = cardSalesCents;
    }

    public long getOtherSalesCents() {
        return otherSalesCents;
    }

    public void setOtherSalesCents(long otherSalesCents) {
        this.otherSalesCents = otherSalesCents;
    }

    public long getCashMovesCents() {
        return cashMovesCents;
    }

    public void setCashMovesCents(long cashMovesCents) {
        this.cashMovesCents = cashMovesCents;
    }

    public long getExpectedCents() {
        return expectedCents;
    }

    public void setExpectedCents(long expectedCents) {
        this.expectedCents = expectedCents;
    }

    public long getCountedCents() {
        return countedCents;
    }

    public void setCountedCents(long countedCents) {
        this.countedCents = countedCents;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
