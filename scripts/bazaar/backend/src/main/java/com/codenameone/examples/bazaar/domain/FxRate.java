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

/// The merchant's exchange rate from the store currency to another: units of it per unit of the store's, in millionths.
@Entity
public class FxRate {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    @Column(nullable = false, type = "VARCHAR(32)")
    private String storeId = "";

    @Column(nullable = false, type = "VARCHAR(3)")
    private String currency = "";

    @Column(nullable = false)
    private long rateMicros;

    @Column(nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    /// The feed's rate, in the same millionths, or 0 before the first fetch.
    @Column(nullable = false)
    private long feedMicros;

    /// Which feed set the feed rate (`ecb`), or empty.
    @Column(nullable = false, type = "VARCHAR(16)")
    private String feedSource = "";

    /// The day the feed published that rate, in epoch milliseconds.
    @Column(nullable = false)
    private long feedAt;

    public FxRate() {
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getRateMicros() {
        return rateMicros;
    }

    public void setRateMicros(long rateMicros) {
        this.rateMicros = rateMicros;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getFeedMicros() {
        return feedMicros;
    }

    public void setFeedMicros(long feedMicros) {
        this.feedMicros = feedMicros;
    }

    public String getFeedSource() {
        return feedSource;
    }

    public void setFeedSource(String feedSource) {
        this.feedSource = feedSource;
    }

    public long getFeedAt() {
        return feedAt;
    }

    public void setFeedAt(long feedAt) {
        this.feedAt = feedAt;
    }
}
