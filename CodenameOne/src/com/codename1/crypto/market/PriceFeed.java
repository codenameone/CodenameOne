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

import java.io.IOException;

/// A source of prices. [CoinGeckoFeed] covers most assets, [JsonPriceFeed] adapts any HTTP API
/// that answers a price in JSON, and [CachingPriceFeed] keeps answers for a while so a screen
/// that refreshes does not spend a rate limit.
@com.codename1.impl.SharedWithBackend
public interface PriceFeed {
    /// The price of `base` in `quote`, by ticker (`BTC`, `ETH`, `USD`, `EUR`); case does not
    /// matter.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the source cannot be reached or does not know the pair
    Quote getQuote(String base, String quote) throws IOException;
}
