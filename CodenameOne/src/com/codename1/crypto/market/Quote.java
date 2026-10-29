/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.crypto.market;

import com.codename1.crypto.UInt256;

/// A price: one unit of `base` costs `price` units of `quote`, as a source reported it at a
/// time. Prices are floating point, as every price API publishes them; amounts stay exact in
/// [UInt256] and only their display value is computed through a quote.
@com.codename1.impl.SharedWithBackend
public final class Quote {
    private final String base;
    private final String quote;
    private final double price;
    private final long timestamp;
    private final String source;

    /// A quote.
    ///
    /// #### Parameters
    ///
    /// - `base`: the asset priced, such as `BTC`
    /// - `quote`: the currency of the price, such as `USD`
    /// - `price`: the price of one unit of `base`
    /// - `timestamp`: when the source last updated it, in epoch milliseconds
    /// - `source`: a short name of the feed
    public Quote(String base, String quote, double price, long timestamp, String source) {
        this.base = PriceFeeds.upper(base);
        this.quote = PriceFeeds.upper(quote);
        this.price = price;
        this.timestamp = timestamp;
        this.source = source;
    }

    /// The asset priced, upper case.
    public String getBase() {
        return base;
    }

    /// The currency of the price, upper case.
    public String getQuote() {
        return quote;
    }

    /// The price of one whole unit of the base asset.
    public double getPrice() {
        return price;
    }

    /// When the source updated the price, in epoch milliseconds.
    public long getTimestamp() {
        return timestamp;
    }

    /// The feed that answered.
    public String getSource() {
        return source;
    }

    /// The value of an amount in base units (wei, satoshis, lamports, token units) with the
    /// asset's decimals, in the quote currency.
    public double valueOf(UInt256 amount, int decimals) {
        return Double.parseDouble(amount.formatUnits(decimals)) * price;
    }

    /// The quote with base and quote swapped.
    public Quote invert() {
        return new Quote(quote, base, 1 / price, timestamp, source);
    }

    @Override
    public String toString() {
        return base + "/" + quote + " " + price + " (" + source + ")";
    }
}
