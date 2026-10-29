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

import com.codename1.io.JSONParser;
import com.codename1.crypto.rpc.RpcTransport;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Prices from CoinGecko's `simple/price` API. Tickers map to CoinGecko's coin ids; the common
/// ones are built in and [#mapTicker] adds others. The quote currency is any CoinGecko
/// `vs_currency` (`usd`, `eur`, `btc`, ...).
///
/// The public API works without a key at a low rate limit. Pass a demo key, or a Pro key with
/// `pro` set, for production; the backend should hold that key, not the application.
@com.codename1.impl.SharedWithBackend
public class CoinGeckoFeed implements PriceFeed {
    private static final String PUBLIC = "https://api.coingecko.com/api/v3";
    private static final String PRO = "https://pro-api.coingecko.com/api/v3";

    private final RpcTransport transport;
    private final String apiKey;
    private final boolean pro;
    private final Map<String, String> ids = new HashMap<String, String>();

    /// A feed on the keyless public API.
    public CoinGeckoFeed(RpcTransport transport) {
        this(transport, null, false);
    }

    /// A feed with an API key; `pro` selects the Pro endpoint and header, otherwise the key is a
    /// demo key.
    public CoinGeckoFeed(RpcTransport transport, String apiKey, boolean pro) {
        if (transport == null) {
            throw new IllegalArgumentException("transport");
        }
        this.transport = transport;
        this.apiKey = apiKey;
        this.pro = pro;
        String[] builtIn = {
            "BTC", "bitcoin", "ETH", "ethereum", "SOL", "solana", "USDC", "usd-coin", "USDT", "tether",
            "DAI", "dai", "WBTC", "wrapped-bitcoin", "WETH", "weth", "BNB", "binancecoin", "POL",
            "polygon-ecosystem-token", "AVAX", "avalanche-2", "ARB", "arbitrum", "OP", "optimism", "LINK",
            "chainlink", "UNI", "uniswap", "LTC", "litecoin", "DOGE", "dogecoin", "XRP", "ripple", "ADA",
            "cardano", "TRX", "tron", "TON", "the-open-network", "BONK", "bonk", "JUP", "jupiter-exchange-solana",
        };
        for (int i = 0; i < builtIn.length; i += 2) {
            ids.put(builtIn[i], builtIn[i + 1]);
        }
    }

    /// Maps a ticker to a CoinGecko coin id, replacing a built-in one.
    public CoinGeckoFeed mapTicker(String ticker, String coinGeckoId) {
        ids.put(PriceFeeds.upper(ticker), coinGeckoId);
        return this;
    }

    @Override
    public Quote getQuote(String base, String quote) throws IOException {
        String id = ids.get(PriceFeeds.upper(base));
        if (id == null) {
            throw new IOException("no CoinGecko id for " + base + "; call mapTicker");
        }
        String vs = PriceFeeds.lower(quote);
        StringBuilder url = new StringBuilder(pro ? PRO : PUBLIC);
        url.append("/simple/price?ids=").append(PriceFeeds.encode(id)).append("&vs_currencies=")
                .append(PriceFeeds.encode(vs)).append("&include_last_updated_at=true");
        if (apiKey != null) {
            url.append(pro ? "&x_cg_pro_api_key=" : "&x_cg_demo_api_key=").append(PriceFeeds.encode(apiKey));
        }
        String body = transport.send("GET", url.toString(), null, null);
        Object parsed;
        try {
            parsed = JSONParser.parseStrict(body, true);
        } catch (IOException e) {
            throw new IOException("CoinGecko answered something that is not JSON", e);
        }
        Object coin = parsed instanceof Map ? ((Map<?, ?>) parsed).get(id) : null;
        if (!(coin instanceof Map)) {
            throw new IOException("CoinGecko has no price for " + base);
        }
        Map<?, ?> c = (Map<?, ?>) coin;
        double price = PriceFeeds.number(c.get(vs), "CoinGecko " + base + "/" + quote);
        Object at = c.get("last_updated_at");
        long ts = at instanceof Number ? ((Number) at).longValue() * 1000L : System.currentTimeMillis();
        return new Quote(base, quote, price, ts, "coingecko");
    }
}
