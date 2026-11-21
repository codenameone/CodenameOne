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

import com.codenameone.examples.bazaar.marketing.Tokens;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A marketplace kept in memory, per channel: what a channel connects to when
/// the server holds no credentials for its kind, so a demo store can list,
/// sell and ship end to end without an account anywhere. [#place] puts an
/// order on it as a buyer would. Pushes are idempotent by key, like a real
/// marketplace's, and the counts ([#applied]) let a test see that.
public class SimulatedConnector implements MarketplaceConnector {
    public static final String KIND = "SIMULATED";

    private final Map<String, Map<String, long[]>> listings = new HashMap<String, Map<String, long[]>>();
    private final Map<String, List<ExternalOrder>> orders = new HashMap<String, List<ExternalOrder>>();
    private final Map<String, List<Shipment>> shipments = new HashMap<String, List<Shipment>>();
    private final Set<String> keys = new HashSet<String>();
    private int applied;
    private int calls;

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public synchronized Token refresh(String refreshToken) {
        return new Token("sim-" + Integer.toHexString(refreshToken.hashCode()),
                System.currentTimeMillis() + 3600000L);
    }

    @Override
    public synchronized PushResult upsert(Session session, ListingPush push) {
        calls++;
        if (keys.add(session.channelId + "|" + push.idempotencyKey)) {
            applied++;
            Map<String, long[]> mine = listings.get(session.channelId);
            if (mine == null) {
                mine = new HashMap<String, long[]>();
                listings.put(session.channelId, mine);
            }
            mine.put(push.sku, new long[] {push.quantity, push.priceCents});
        }
        return new PushResult("sim-" + push.sku, "");
    }

    @Override
    public synchronized List<ExternalOrder> orders(Session session, long sinceMillis) {
        List<ExternalOrder> all = orders.get(session.channelId);
        List<ExternalOrder> out = new ArrayList<ExternalOrder>();
        for (int iter = 0; all != null && iter < all.size(); iter++) {
            if (all.get(iter).createdAt >= sinceMillis) {
                out.add(all.get(iter));
            }
        }
        return out;
    }

    @Override
    public synchronized void ship(Session session, Shipment shipment) {
        List<Shipment> mine = shipments.get(session.channelId);
        if (mine == null) {
            mine = new ArrayList<Shipment>();
            shipments.put(session.channelId, mine);
        }
        mine.add(shipment);
    }

    @Override
    public boolean verify(String secret, byte[] body, String signature) {
        return SignedBodies.verify(secret, body, signature);
    }

    /// Places an order on the simulated marketplace for a channel.
    public synchronized void place(String channelId, ExternalOrder order) {
        List<ExternalOrder> mine = orders.get(channelId);
        if (mine == null) {
            mine = new ArrayList<ExternalOrder>();
            orders.put(channelId, mine);
        }
        mine.add(order);
    }

    /// What the simulated marketplace shows for `sku`: {quantity, price}, or null.
    public synchronized long[] listing(String channelId, String sku) {
        Map<String, long[]> mine = listings.get(channelId);
        return mine == null ? null : mine.get(sku);
    }

    /// The shipments told to a channel.
    public synchronized List<Shipment> shipments(String channelId) {
        List<Shipment> mine = shipments.get(channelId);
        return mine == null ? new ArrayList<Shipment>() : new ArrayList<Shipment>(mine);
    }

    /// Pushes that changed something (a repeated key changes nothing).
    public synchronized int applied() {
        return applied;
    }

    /// Every push call, repeated or not.
    public synchronized int calls() {
        return calls;
    }

    /// Signs `body` as a notification for a channel with `secret` would be.
    public static String sign(String secret, byte[] body) throws IOException {
        return "sha256=" + Tokens.sign(secret, new String(body, "UTF-8"));
    }
}
