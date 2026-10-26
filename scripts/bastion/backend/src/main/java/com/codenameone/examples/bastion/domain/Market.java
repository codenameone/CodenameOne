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

/// A pair customers trade on the exchange: `base` priced in `quote`.
///
/// The row is also the market's lock. Placing or cancelling an order holds
/// it first ([com.codenameone.examples.bastion.repository.MarketRepository#hold]),
/// so one market matches one order at a time, and `seq` is the time half of
/// price-time priority: a counter, not a clock, so two orders placed in the
/// same millisecond still have an order.
@Entity
public class Market {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(16)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(12)")
    private String base = "";

    @Column(nullable = false, type = "VARCHAR(12)")
    private String quote = "";

    /// The smallest quantity of `base` an order may name, in its ledger units.
    /// Every quantity is a whole number of lots.
    @Column(nullable = false)
    private long lot;

    /// The simulated reference price, in `quote` ledger units per one whole
    /// `base` unit: what limits are valued at. Not the last trade, which two
    /// colluding customers could print at any price they like.
    @Column(nullable = false)
    private long indexPrice;

    @Column(nullable = false)
    private long lastPrice;

    @Column(nullable = false)
    private long seq;

    public Market() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public long getLot() {
        return lot;
    }

    public void setLot(long lot) {
        this.lot = lot;
    }

    public long getIndexPrice() {
        return indexPrice;
    }

    public void setIndexPrice(long indexPrice) {
        this.indexPrice = indexPrice;
    }

    public long getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(long lastPrice) {
        this.lastPrice = lastPrice;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }
}
