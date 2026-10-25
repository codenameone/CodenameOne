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
package com.codenameone.examples.bastion.domain;

import java.time.Instant;
import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// One order on the exchange. Prices are in quote ledger units (cents) per
/// one whole base unit; quantities are in base ledger units (satoshi).
///
/// `notional` is the running sum of price times quantity over the fills, the
/// exact value before any division, from which `settled` -- the quote paid or
/// received so far -- is derived by rounding once, in the bank's favour. `held`
/// is what is still in the owner's HOLD account on this order's behalf.
@Entity
public class TradeOrder {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String owner = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String market = "";

    @Column(nullable = false, type = "VARCHAR(4)")
    private String side = "";

    @Column(nullable = false, type = "VARCHAR(6)")
    private String type = "";

    /// The limit; 0 for a market order.
    @Column(nullable = false)
    private long price;

    /// Base units asked for; 0 for a market buy, which names `budget`.
    @Column(nullable = false)
    private long quantity;

    /// A market buy's quote amount, fee included.
    @Column(nullable = false)
    private long budget;

    @Column(nullable = false)
    private long filled;

    @Column(nullable = false)
    private long notional;

    @Column(nullable = false)
    private long settled;

    @Column(nullable = false)
    private long fee;

    @Column(nullable = false)
    private long held;

    @Column(nullable = false, type = "VARCHAR(10)")
    private String status = "";

    @Column(nullable = false)
    private long seq;

    @Column(nullable = false, type = "VARCHAR(80)")
    private String clientKey = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    public TradeOrder() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getBudget() {
        return budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }

    public long getFilled() {
        return filled;
    }

    public void setFilled(long filled) {
        this.filled = filled;
    }

    public long getNotional() {
        return notional;
    }

    public void setNotional(long notional) {
        this.notional = notional;
    }

    public long getSettled() {
        return settled;
    }

    public void setSettled(long settled) {
        this.settled = settled;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getHeld() {
        return held;
    }

    public void setHeld(long held) {
        this.held = held;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
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
