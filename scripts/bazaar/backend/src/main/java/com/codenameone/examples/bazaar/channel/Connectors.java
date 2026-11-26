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
package com.codenameone.examples.bazaar.channel;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// The connector of each channel kind. `EBAY` is the [EbayConnector] when the
/// server is configured for it, and the [SimulatedConnector] otherwise, so a
/// store without credentials still sees the whole flow:
///
/// - `bazaar.channels.ebay.client-id`, `bazaar.channels.ebay.client-secret`:
///   the marketplace application's keys (never a seller's).
/// - `bazaar.channels.ebay.api-base` (default `https://api.ebay.com`) and
///   `bazaar.channels.ebay.identity-base` (default the same): a sandbox
///   points both at the marketplace's sandbox hosts. Server configuration,
///   never something a merchant can type, so a channel cannot aim the
///   seller's token at another host.
/// - `bazaar.channels.ebay.marketplace-id` (default `EBAY_US`).
@Component
public class Connectors {
    public static final String[] KINDS = {SimulatedConnector.KIND, EbayConnector.KIND};

    private final SimulatedConnector simulated = new SimulatedConnector();
    private final Map<String, MarketplaceConnector> byKind = new HashMap<String, MarketplaceConnector>();

    public Connectors(Config config) throws IOException {
        String api = config.get("bazaar.channels.ebay.api-base", "https://api.ebay.com");
        byKind.put(EbayConnector.KIND, new EbayConnector(api,
                config.get("bazaar.channels.ebay.identity-base", api),
                config.get("bazaar.channels.ebay.client-id", ""),
                config.get("bazaar.channels.ebay.client-secret", ""),
                config.get("bazaar.channels.ebay.marketplace-id", "EBAY_US"),
                new ChannelHttp.Live()));
        byKind.put(SimulatedConnector.KIND, simulated);
    }

    /// The connector a channel of `kind` talks to.
    public synchronized MarketplaceConnector of(String kind) {
        MarketplaceConnector c = byKind.get(kind);
        return c == null || !c.configured() ? simulated : c;
    }

    /// Whether `kind` reaches the real marketplace.
    public synchronized boolean live(String kind) {
        MarketplaceConnector c = byKind.get(kind);
        return c != null && c.configured() && !(c instanceof SimulatedConnector);
    }

    /// The in-memory marketplace.
    public SimulatedConnector simulated() {
        return simulated;
    }

    /// Replaces the connector of `kind`: an integration plugs a marketplace in
    /// here, and tests a connector over local fakes.
    public synchronized void use(String kind, MarketplaceConnector connector) {
        byKind.put(kind, connector);
    }

    static boolean known(String kind) {
        for (int iter = 0; iter < KINDS.length; iter++) {
            if (KINDS[iter].equals(kind)) {
                return true;
            }
        }
        return false;
    }
}
