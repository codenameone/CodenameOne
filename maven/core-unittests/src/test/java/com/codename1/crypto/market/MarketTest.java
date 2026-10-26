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
import com.codename1.crypto.rpc.RpcException;
import com.codename1.crypto.rpc.RpcTransport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The price feeds against recorded replies.
class MarketTest {
    private static final class Stub implements RpcTransport {
        final List<String> urls = new ArrayList<String>();
        String reply;
        boolean fail;

        @Override
        public String send(String method, String url, String contentType, String body) throws IOException {
            urls.add(url);
            if (fail) {
                throw RpcException.http(429, "slow down");
            }
            return reply;
        }
    }

    @Test
    void coinGecko() throws IOException {
        Stub s = new Stub();
        s.reply = "{\"ethereum\":{\"eur\":3120.5,\"last_updated_at\":1700000000}}";
        Quote q = new CoinGeckoFeed(s, "k", false).getQuote("eth", "EUR");
        assertEquals("https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=eur"
                + "&include_last_updated_at=true&x_cg_demo_api_key=k", s.urls.get(0));
        assertEquals("ETH", q.getBase());
        assertEquals("EUR", q.getQuote());
        assertEquals(3120.5, q.getPrice(), 0);
        assertEquals(1700000000000L, q.getTimestamp());
        assertEquals(4680.75, q.valueOf(UInt256.parse("1500000000000000000"), 18), 1e-9);
        assertEquals(1 / 3120.5, q.invert().getPrice(), 1e-15);
        assertThrows(IOException.class, () -> new CoinGeckoFeed(s).getQuote("NOPE", "usd"));
    }

    @Test
    void jsonTemplate() throws IOException {
        Stub s = new Stub();
        s.reply = "{\"data\":{\"amount\":\"67187.33\",\"base\":\"BTC\",\"currency\":\"USD\"}}";
        PriceFeed f = new JsonPriceFeed(s, "https://api.coinbase.com/v2/prices/{BASE}-{QUOTE}/spot", "data.amount",
                "coinbase");
        assertEquals(67187.33, f.getQuote("btc", "usd").getPrice(), 0);
        assertEquals("https://api.coinbase.com/v2/prices/BTC-USD/spot", s.urls.get(0));
        s.reply = "{\"result\":{\"XXBTZUSD\":{\"c\":[\"67187.30000\",\"0.01\"]}}}";
        PriceFeed k = new JsonPriceFeed(s, "https://api.kraken.com/0/public/Ticker?pair={base}{quote}",
                "result.XXBTZUSD.c.0", "kraken");
        assertEquals(67187.3, k.getQuote("XBT", "USD").getPrice(), 1e-9);
        assertEquals("https://api.kraken.com/0/public/Ticker?pair=xbtusd", s.urls.get(1));
        s.reply = "{\"result\":{}}";
        assertThrows(IOException.class, () -> k.getQuote("XBT", "USD"));
    }

    @Test
    void cachingServesFreshThenStale() throws IOException {
        Stub s = new Stub();
        s.reply = "{\"data\":{\"amount\":1.5}}";
        final long[] clock = {1000};
        CachingPriceFeed f = new CachingPriceFeed(new JsonPriceFeed(s, "https://x/{base}", "data.amount", "x"),
                10000, 60000) {
            @Override
            protected long now() {
                return clock[0];
            }
        };
        f.getQuote("a", "b");
        f.getQuote("A", "B");
        assertEquals(1, s.urls.size());
        clock[0] += 20000;
        s.fail = true;
        assertEquals(1.5, f.getQuote("a", "b").getPrice(), 0);
        assertEquals(2, s.urls.size());
        clock[0] += 60000;
        assertThrows(RpcException.class, () -> f.getQuote("a", "b"));
    }
}
