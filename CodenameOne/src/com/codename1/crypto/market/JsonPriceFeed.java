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

import com.codename1.crypto.encoding.Json;
import com.codename1.crypto.rpc.RpcTransport;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/// A feed over any HTTP API that answers one price in JSON. The URL is a template in which
/// `{base}`/`{quote}` become the lower-case tickers and `{BASE}`/`{QUOTE}` the upper-case ones;
/// the path names the price in the reply, member names and array indices separated by dots.
///
/// ```java
/// // Coinbase: {"data":{"amount":"67187.33","base":"BTC","currency":"USD"}}
/// RpcTransport transport = new NetworkTransport();
/// PriceFeed coinbase = new JsonPriceFeed(transport,
///         "https://api.coinbase.com/v2/prices/{BASE}-{QUOTE}/spot", "data.amount", "coinbase");
/// // Kraken: {"result":{"XXBTZUSD":{"c":["67187.30000","0.01"]}}} -- the pair key varies, so
/// // pass a path per pair or use a feed that knows the exchange.
/// ```
///
/// The price may be a JSON number or a numeric string.
@com.codename1.impl.SharedWithBackend
public class JsonPriceFeed implements PriceFeed {
    private final RpcTransport transport;
    private final String template;
    private final String[] path;
    private final String name;

    /// A feed.
    ///
    /// #### Parameters
    ///
    /// - `transport`: how to send the request
    /// - `urlTemplate`: the URL with `{base}`, `{quote}`, `{BASE}`, `{QUOTE}` placeholders
    /// - `pricePath`: where the price is in the reply, such as `data.amount`
    /// - `name`: the source name the quotes carry
    public JsonPriceFeed(RpcTransport transport, String urlTemplate, String pricePath, String name) {
        if (transport == null || urlTemplate == null || pricePath == null) {
            throw new IllegalArgumentException("transport, urlTemplate and pricePath are required");
        }
        this.transport = transport;
        this.template = urlTemplate;
        this.path = split(pricePath);
        this.name = name == null ? "json" : name;
    }

    private static String[] split(String p) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= p.length(); i++) {
            if (i == p.length() || p.charAt(i) == '.') {
                out.add(p.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public Quote getQuote(String base, String quote) throws IOException {
        String url = replace(template, "{base}", PriceFeeds.encode(PriceFeeds.lower(base)));
        url = replace(url, "{quote}", PriceFeeds.encode(PriceFeeds.lower(quote)));
        url = replace(url, "{BASE}", PriceFeeds.encode(PriceFeeds.upper(base)));
        url = replace(url, "{QUOTE}", PriceFeeds.encode(PriceFeeds.upper(quote)));
        Object node;
        try {
            node = Json.parse(transport.send("GET", url, null, null));
        } catch (IllegalArgumentException e) {
            throw new IOException(name + " answered something that is not JSON", e);
        }
        for (String pathItem : path) {
            if (node instanceof Map) {
                node = ((Map<?, ?>) node).get(pathItem);
            } else if (node instanceof List) {
                int index;
                try {
                    index = Integer.parseInt(pathItem);
                } catch (NumberFormatException e) {
                    throw new IOException(name + ": " + pathItem + " indexes an array", e);
                }
                List<?> l = (List<?>) node;
                node = index >= 0 && index < l.size() ? l.get(index) : null;
            } else {
                node = null;
            }
        }
        return new Quote(base, quote, PriceFeeds.number(node, name + " " + base + "/" + quote),
                System.currentTimeMillis(), name);
    }

    private static String replace(String s, String what, String with) {
        StringBuilder b = new StringBuilder();
        int from = 0;
        while (true) {
            int i = s.indexOf(what, from);
            if (i < 0) {
                b.append(s.substring(from));
                return b.toString();
            }
            b.append(s.substring(from, i)).append(with);
            from = i + what.length();
        }
    }
}
