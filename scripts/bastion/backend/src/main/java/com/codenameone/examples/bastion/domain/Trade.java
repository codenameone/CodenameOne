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

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// One match between a buy and a sell order: one journal entry, `entryId`.
@Entity
public class Trade {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(16)")
    private String market = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String buyOrder = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String sellOrder = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String buyer = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String seller = "";

    @Column(nullable = false, type = "VARCHAR(4)")
    private String takerSide = "";

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long quantity;

    /// Quote units out of the buyer's hold, fee included.
    @Column(nullable = false)
    private long buyerPaid;

    /// Quote units into the seller's account, fee taken.
    @Column(nullable = false)
    private long sellerGot;

    @Column(nullable = false)
    private long fee;

    @Column(nullable = false, type = "VARCHAR(32)")
    private String entryId = "";

    @Column(nullable = false)
    private long createdAt;

    public Trade() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getBuyOrder() {
        return buyOrder;
    }

    public void setBuyOrder(String buyOrder) {
        this.buyOrder = buyOrder;
    }

    public String getSellOrder() {
        return sellOrder;
    }

    public void setSellOrder(String sellOrder) {
        this.sellOrder = sellOrder;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getTakerSide() {
        return takerSide;
    }

    public void setTakerSide(String takerSide) {
        this.takerSide = takerSide;
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

    public long getBuyerPaid() {
        return buyerPaid;
    }

    public void setBuyerPaid(long buyerPaid) {
        this.buyerPaid = buyerPaid;
    }

    public long getSellerGot() {
        return sellerGot;
    }

    public void setSellerGot(long sellerGot) {
        this.sellerGot = sellerGot;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
