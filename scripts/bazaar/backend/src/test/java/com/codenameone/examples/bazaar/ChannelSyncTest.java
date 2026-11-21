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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.ChannelDto;
import com.codenameone.examples.bazaar.api.ChannelListingDto;
import com.codenameone.examples.bazaar.api.ChannelSetupDto;
import com.codenameone.examples.bazaar.api.SyncLogDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.channel.ChannelException;
import com.codenameone.examples.bazaar.channel.ChannelHttp;
import com.codenameone.examples.bazaar.channel.ChannelSync;
import com.codenameone.examples.bazaar.channel.ChannelWebhook;
import com.codenameone.examples.bazaar.channel.Channels;
import com.codenameone.examples.bazaar.channel.Connectors;
import com.codenameone.examples.bazaar.channel.EbayConnector;
import com.codenameone.examples.bazaar.channel.MarketplaceConnector;
import com.codenameone.examples.bazaar.channel.SimulatedConnector;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Live marketplace sync: pushes are debounced and idempotent, a pulled
/// order is made once however often it is replayed, a pull that oversells
/// is accepted, flagged and pushes zero, a notification needs the channel's
/// signature, shipments go back once, and the eBay-style connector speaks
/// its shape against a local fake.
@BackendTest
class ChannelSyncTest {
    static final String SECRET = "test-notify-secret-0123456789";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Channels channels;
    @Autowired
    private ChannelSync sync;
    @Autowired
    private Connectors connectors;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private String connect(Shop s) throws Exception {
        List<ChannelDto> cs = channels.connect(s.owner, s.storeId, new ChannelSetupDto(
                SimulatedConnector.KIND, "Market", "", SECRET));
        String id = cs.get(cs.size() - 1).id();
        channels.listVariant(s.owner, s.storeId, id, new ChannelListingDto(null, s.variantId, null,
                null, null, null, 0, 0, 0, null));
        return id;
    }

    private SimulatedConnector sim() {
        return connectors.simulated();
    }

    private static MarketplaceConnector.ExternalOrder order(String id, String sku, int qty) {
        MarketplaceConnector.ExternalOrder o = new MarketplaceConnector.ExternalOrder();
        o.id = id;
        o.createdAt = System.currentTimeMillis();
        o.currency = "USD";
        o.shipName = "Grace Hopper";
        o.line1 = "1 Navy Way";
        o.city = "Arlington";
        o.country = "US";
        o.lines.add(new MarketplaceConnector.ExternalLine(id + "-1", sku, qty, 1000));
        return o;
    }

    private long orders(String storeId) throws Exception {
        return Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ? AND "
                + "channel = ?", new Object[] {storeId, ChannelSync.MARKETPLACE}).get("n"));
    }

    @Test
    void pushesAreDebouncedAndIdempotent() throws Exception {
        Shop s = shop("ch-push", 30);
        String ch = connect(s);
        long now = System.currentTimeMillis();
        int before = sim().applied();
        sync.tick(now);
        assertEquals(before + 1, sim().applied(), "a new listing is pushed at once");
        assertEquals(30, sim().listing(ch, s.sku)[0]);
        assertEquals(1000, sim().listing(ch, s.sku)[1]);
        int calls = sim().calls();
        sync.tick(now + 1000);
        sync.tick(now + 100000);
        assertEquals(calls, sim().calls(), "nothing changed, nothing pushed");

        // A stock change waits for the debounce, then goes once.
        inventory.count(s.storeId, s.variantId, s.locationId, 25, "count", "test");
        sync.tick(now + 101000);
        assertEquals(calls, sim().calls(), "debounced");
        sync.tick(now + 101000 + 25000);
        assertEquals(calls + 1, sim().calls());
        assertEquals(25, sim().listing(ch, s.sku)[0]);
        sync.tick(now + 200000);
        assertEquals(calls + 1, sim().calls());
        ChannelListingDto l = channels.listings(s.owner, s.storeId, ch).get(0);
        assertEquals(2, l.version());
        assertEquals("OK", l.status());

        // A repeated key is applied once by the marketplace.
        int applied = sim().applied();
        MarketplaceConnector.Session session = new MarketplaceConnector.Session(ch, "x");
        sim().upsert(session, new MarketplaceConnector.ListingPush(s.sku, "", 1, 1, "USD",
                l.id() + ":2"));
        assertEquals(applied, sim().applied());
        assertEquals(25, sim().listing(ch, s.sku)[0]);
    }

    @Test
    void aPulledOrderIsMadeOnceHoweverOftenItIsReplayed() throws Exception {
        Shop s = shop("ch-pull", 30);
        String ch = connect(s);
        sim().place(ch, order("EXT-1001", s.sku, 2));
        long now = System.currentTimeMillis();
        sync.tick(now);
        assertEquals(1, orders(s.storeId));
        sync.run(ch, now + 1000, true);
        sync.run(ch, now + 2000, true);
        byte[] body = "{\"orderId\":\"EXT-1001\"}".getBytes("UTF-8");
        sync.notification(ch, body, SimulatedConnector.sign(SECRET, body), now + 3000);
        sync.notification(ch, body, SimulatedConnector.sign(SECRET, body), now + 4000);
        assertEquals(1, orders(s.storeId), "replays make no second order");
        Map o = db.queryOne("SELECT id, financial, total_cents FROM store_order WHERE store_id = ? AND "
                + "channel = ?", new Object[] {s.storeId, ChannelSync.MARKETPLACE});
        assertEquals("PAID", Rows.text(o.get("financial")));
        assertEquals(2000, Rows.number(o.get("total_cents")));
        assertEquals(2, inventory.level(s.storeId, s.variantId, s.locationId).getCommitted());
        // The order took two units: every listing goes down without waiting.
        sync.tick(now + 5000);
        assertEquals(28, sim().listing(ch, s.sku)[0]);

        // A foreign currency or an unknown SKU is an error in the log, not an order.
        MarketplaceConnector.ExternalOrder euro = order("EXT-1002", s.sku, 1);
        euro.currency = "EUR";
        sim().place(ch, euro);
        sim().place(ch, order("EXT-1003", "NO-SUCH-SKU", 1));
        sync.run(ch, now + 6000, true);
        assertEquals(1, orders(s.storeId));
        List<SyncLogDto> log = channels.log(s.owner, s.storeId, ch);
        int errors = 0;
        for (int iter = 0; iter < log.size(); iter++) {
            if ("ORDER".equals(log.get(iter).action()) && "ERROR".equals(log.get(iter).outcome())) {
                errors++;
            }
        }
        assertTrue(errors >= 2, "both refused orders are logged");
    }

    @Test
    void aPullThatOversellsIsAcceptedFlaggedAndPushesZero() throws Exception {
        Shop s = shop("ch-over", 3);
        String ch = connect(s);
        long now = System.currentTimeMillis();
        sync.tick(now);
        assertEquals(3, sim().listing(ch, s.sku)[0]);
        sim().place(ch, order("EXT-2001", s.sku, 5));
        sync.run(ch, now + 1000, true);
        assertEquals(1, orders(s.storeId), "the marketplace already sold it");
        assertEquals(5, inventory.level(s.storeId, s.variantId, s.locationId).getCommitted());
        assertTrue(db.queryOne("SELECT id FROM audit_event WHERE store_id = ? AND action = ?",
                new Object[] {s.storeId, "CHANNEL_OVERSOLD"}) != null);
        assertEquals(0, sim().listing(ch, s.sku)[0], "zero pushed at once");
    }

    @Test
    void aNotificationNeedsTheChannelsSignature() throws Exception {
        Shop s = shop("ch-sig", 10);
        String ch = connect(s);
        sim().place(ch, order("EXT-3001", s.sku, 1));
        String body = "{\"orderId\":\"EXT-3001\"}";
        assertEquals(401, notify(ch, body, "sha256=00").getStatus());
        assertEquals(401, notify(ch, body, SimulatedConnector.sign("another-secret-0123456",
                body.getBytes("UTF-8"))).getStatus());
        assertEquals(401, notify("nosuchchannel", body, SimulatedConnector.sign(SECRET,
                body.getBytes("UTF-8"))).getStatus());
        assertEquals(0, orders(s.storeId), "a refused notification does nothing");
        assertEquals(200, notify(ch, body, SimulatedConnector.sign(SECRET,
                body.getBytes("UTF-8"))).getStatus());
        assertEquals(1, orders(s.storeId));
    }

    private MockResponse notify(String channel, String body, String signature) throws Exception {
        return mvc.perform(Rest.body(post(ChannelWebhook.PATH + "?channel=" + channel), body)
                .header(MarketplaceConnector.SIGNATURE_HEADER, signature)).andReturn().getResponse();
    }

    @Test
    void aShipmentGoesBackOnceWithItsTracking() throws Exception {
        Shop s = shop("ch-ship", 10);
        String ch = connect(s);
        sim().place(ch, order("EXT-4001", s.sku, 1));
        long now = System.currentTimeMillis();
        sync.run(ch, now, true);
        Map o = db.queryOne("SELECT id FROM store_order WHERE store_id = ? AND channel = ?",
                new Object[] {s.storeId, ChannelSync.MARKETPLACE});
        String orderId = Rows.text(o.get("id"));
        Map line = db.queryOne("SELECT id FROM order_line WHERE order_id = ?", new Object[] {orderId});
        String f = Ids.next();
        db.execute("INSERT INTO fulfillment (id, order_id, store_id, location_id, status, carrier, "
                + "service, tracking_number, label_cents, weight_grams, created_at, updated_at) VALUES "
                + "(?, ?, ?, ?, 'SHIPPED', 'USPS', 'Ground', '9400TEST', 0, 0, 0, 0)",
                new Object[] {f, orderId, s.storeId, s.locationId});
        db.execute("INSERT INTO fulfillment_line (id, fulfillment_id, order_line_id, quantity) VALUES "
                + "(?, ?, ?, 1)", new Object[] {Ids.next(), f, Rows.text(line.get("id"))});
        sync.run(ch, now + 1000, true);
        sync.run(ch, now + 2000, true);
        List<MarketplaceConnector.Shipment> sent = sim().shipments(ch);
        assertEquals(1, sent.size());
        assertEquals("EXT-4001", sent.get(0).orderId);
        assertEquals("9400TEST", sent.get(0).tracking);
        assertEquals("EXT-4001-1", sent.get(0).lines.get(0).lineId);
    }

    @Test
    void anotherStoresChannelIsRefused() throws Exception {
        Shop s = shop("ch-idor", 10);
        String ch = connect(s);
        Shop o = shop("ch-idor-o", 10);
        try {
            channels.listings(o.owner, o.storeId, ch);
            throw new AssertionError("read another store's channel");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
        try {
            channels.listVariant(s.owner, s.storeId, ch, new ChannelListingDto(null, o.variantId,
                    null, null, null, null, 0, 0, 0, null));
            throw new AssertionError("listed another store's variant");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }

    // ------------------------------------------------- eBay-style connector

    /// Answers the connector's requests from a script and records them.
    static final class FakeEbay implements ChannelHttp {
        final List<String> seen = new ArrayList<String>();
        int refusals;

        @Override
        public Reply send(String method, String url, List<String> headers, String body) {
            seen.add(method + " " + url + (body == null ? "" : " " + body));
            boolean bearerOk = headers.contains("Authorization: Bearer fresh-token");
            if (url.endsWith("/identity/v1/oauth2/token")) {
                return new Reply(200, "{\"access_token\":\"fresh-token\",\"expires_in\":7200}", "");
            }
            if (!bearerOk) {
                refusals++;
                return new Reply(401, "{}", "");
            }
            if (url.contains("/sell/inventory/v1/offer?sku=")) {
                return new Reply(200, "{\"offers\":[{\"offerId\":\"OFFER-9\"}]}", "");
            }
            if (url.endsWith("/bulk_update_price_quantity")) {
                return new Reply(200, "{\"responses\":[{\"statusCode\":200}]}", "\"v7\"");
            }
            if (url.contains("/sell/fulfillment/v1/order?")) {
                return new Reply(200, "{\"orders\":[{\"orderId\":\"12-34\",\"creationDate\":"
                        + "\"2026-11-20T10:00:00.000Z\",\"pricingSummary\":{\"total\":{\"value\":"
                        + "\"21.50\",\"currency\":\"USD\"},\"deliveryCost\":{\"value\":\"1.5\"}},"
                        + "\"lineItems\":[{\"lineItemId\":\"L1\",\"sku\":\"S1\",\"quantity\":2,"
                        + "\"lineItemCost\":{\"value\":\"20.00\"}}],\"fulfillmentStartInstructions\":"
                        + "[{\"shippingStep\":{\"shipTo\":{\"fullName\":\"Ada\",\"contactAddress\":"
                        + "{\"addressLine1\":\"1 Main\",\"city\":\"Albany\",\"countryCode\":\"US\"}}}}]}],"
                        + "\"next\":\"https://evil.example/steal\"}", "");
            }
            if (url.endsWith("/shipping_fulfillment")) {
                return new Reply(201, "", "");
            }
            return new Reply(404, "{}", "");
        }
    }

    @Test
    void theEbayStyleConnectorRefreshesUpsertsPullsAndShips() throws Exception {
        FakeEbay http = new FakeEbay();
        EbayConnector c = new EbayConnector("https://api.test", "https://auth.test", "id",
                "secret", "EBAY_US", http);
        assertTrue(c.configured());
        MarketplaceConnector.Token t = c.refresh("refresh-1");
        assertEquals("fresh-token", t.accessToken);
        assertTrue(http.seen.get(0).contains("grant_type=refresh_token&refresh_token=refresh-1"));
        MarketplaceConnector.Session ok = new MarketplaceConnector.Session("c", t.accessToken);
        MarketplaceConnector.PushResult r = c.upsert(ok, new MarketplaceConnector.ListingPush("S1",
                "", 4, 1299, "USD", "k:1"));
        assertEquals("OFFER-9", r.externalId);
        assertEquals("\"v7\"", r.etag);
        String update = http.seen.get(http.seen.size() - 1);
        assertTrue(update.contains("\"offerId\":\"OFFER-9\"") && update.contains("\"12.99\"")
                && update.contains("\"availableQuantity\":4"), update);
        List<MarketplaceConnector.ExternalOrder> os = c.orders(ok, 0);
        assertEquals(1, os.size(), "the foreign next page is not followed");
        MarketplaceConnector.ExternalOrder o = os.get(0);
        assertEquals("12-34", o.id);
        assertEquals(2150, o.totalCents);
        assertEquals(150, o.shippingCents);
        assertEquals(1000, o.lines.get(0).unitCents);
        assertEquals("Albany", o.city);
        c.ship(ok, new MarketplaceConnector.Shipment("12-34", "USPS", "94", 0, o.lines));
        assertTrue(http.seen.get(http.seen.size() - 1).contains("\"trackingNumber\":\"94\""));
        try {
            c.upsert(new MarketplaceConnector.Session("c", "stale"), new MarketplaceConnector
                    .ListingPush("S1", "OFFER-9", 1, 1, "USD", "k:2"));
            throw new AssertionError("a stale token was accepted");
        } catch (ChannelException e) {
            assertTrue(e.isAuth() && e.isRetryable());
        }
        assertTrue(c.verify(SECRET, "x".getBytes("UTF-8"), SimulatedConnector.sign(SECRET,
                "x".getBytes("UTF-8"))));
        assertTrue(!c.verify(SECRET, "y".getBytes("UTF-8"), SimulatedConnector.sign(SECRET,
                "x".getBytes("UTF-8"))));
    }

    @Test
    void theEngineRefreshesAStaleTokenAndRetriesThroughTheConnector() throws Exception {
        Shop s = shop("ch-ebay", 7);
        FakeEbay http = new FakeEbay();
        connectors.use(EbayConnector.KIND, new EbayConnector("https://api.test", "https://auth.test",
                "id", "secret", "EBAY_US", http));
        try {
            List<ChannelDto> cs = channels.connect(s.owner, s.storeId, new ChannelSetupDto(
                    EbayConnector.KIND, "eBay", "refresh-1", SECRET));
            String ch = cs.get(0).id();
            assertTrue(cs.get(0).configured());
            channels.listVariant(s.owner, s.storeId, ch, new ChannelListingDto(null, s.variantId,
                    null, "S1", null, null, 0, 0, 0, null));
            db.execute("UPDATE sales_channel SET access_token = 'stale', access_expires_at = ? WHERE "
                    + "id = ?", new Object[] {Long.valueOf(System.currentTimeMillis() + 3600000L), ch});
            sync.run(ch, System.currentTimeMillis(), true);
            ChannelListingDto l = channels.listings(s.owner, s.storeId, ch).get(0);
            assertEquals("OK", l.status(), l.lastError());
            assertEquals("OFFER-9", l.externalId());
            // The fake marketplace also sold two of S1 (order 12-34), pulled first.
            assertEquals(5, l.pushedQty());
            assertTrue(http.refusals >= 1, "the stale token was tried first");
        } finally {
            connectors.use(EbayConnector.KIND, new EbayConnector("", "", "", "", "",
                    new ChannelHttp.Live()));
        }
    }
}
