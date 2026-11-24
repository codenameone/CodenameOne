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
import com.codenameone.examples.bazaar.api.BundleDto;
import com.codenameone.examples.bazaar.api.BundlePartDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.ChannelDto;
import com.codenameone.examples.bazaar.api.ChannelListingDto;
import com.codenameone.examples.bazaar.api.ChannelScheduleDto;
import com.codenameone.examples.bazaar.api.ChannelSetupDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.catalog.Schedules;
import com.codenameone.examples.bazaar.channel.ChannelSync;
import com.codenameone.examples.bazaar.channel.Channels;
import com.codenameone.examples.bazaar.channel.Connectors;
import com.codenameone.examples.bazaar.channel.SimulatedConnector;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Bundles, whose sales move their components' stock, and the publishing
/// window of a product on each channel.
@BackendTest
class BundlesSchedulesTest {
    private static final long HOUR = 3600000L;

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
    private Catalog catalog;
    @Autowired
    private Schedules schedules;
    @Autowired
    private Orders orders;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private Channels channels;
    @Autowired
    private Connectors connectors;
    @Autowired
    private ChannelSync sync;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// A $25 "Gift set" of two of the shop's mugs; answers its variant id.
    private String giftSet(Shop s) throws Exception {
        String productId = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Gift set', '', '', '', "
                + "'', 'ACTIVE', '', '', '', 0, 0, 0, 0)",
                new Object[] {productId, s.storeId, s.storeId + "-set"});
        String variantId = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'Default', '', '', '', ?, '', 2500, 0, 800, 700, 1, 0, 0, 0)",
                new Object[] {variantId, productId, s.storeId, s.sku + "-SET"});
        BundleDto b = catalog.setBundle(s.owner, s.storeId, productId, new BundleDto(productId,
                true, Arrays.asList(new BundlePartDto(null, s.sku, 2, "", 0)), 0));
        assertTrue(b.bundle());
        assertEquals(1, b.parts().size());
        return variantId;
    }

    private String productOf(String variantId) throws Exception {
        return (String) db.queryOne("SELECT product_id FROM variant WHERE id = ?",
                new Object[] {variantId}).get("product_id");
    }

    private OrderDto buy(Shop s, String user, String variantId, int qty) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(variantId, qty));
        return place(s, user);
    }

    private OrderDto place(Shop s, String user) throws Exception {
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        String rate = null;
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("Express".equals(o.name())) {
                rate = o.rateId();
            }
        }
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY, rate,
                (String) card.get("id"), user + "@example.com", "", null, 0, null));
    }

    private int status(String path) throws Exception {
        return mvc.perform(get(path)).andReturn().getResponse().getStatus();
    }

    private String page(String path) throws Exception {
        return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }

    private String register(Shop s, String base) throws Exception {
        return (String) Rest.json(mvc, People.asMerchant(Rest.body(post(base + "/registers"),
                "{\"locationId\":\"" + s.locationId + "\",\"name\":\"Front\",\"active\":true,"
                + "\"taxCountry\":\"US\",\"taxRegion\":\"NY\"}"), s.owner)).get("id");
    }

    // ================================================================ bundles

    @Test
    void checkoutTakesTheComponentsAndCancelGivesThemBack() throws Exception {
        Shop s = shop("bdl-buy", 10);
        String set = giftSet(s);
        BundleDto b = catalog.bundle(s.owner, s.storeId, productOf(set));
        assertEquals(5, b.available(), "ten mugs make five sets of two");
        String user = People.customer(accounts, "bdl-buyer");
        OrderDto o = buy(s, user, set, 2);
        assertEquals(10 - 4, inventory.available(s.variantId, s.locationId));
        assertEquals(3, catalog.bundle(s.owner, s.storeId, productOf(set)).available());
        orders.cancelMine(user, o.id());
        assertEquals(10, inventory.available(s.variantId, s.locationId));
    }

    /// On hand of the shop's mug where it is stocked.
    private long onHand(Shop s) throws Exception {
        return Rows.number(db.queryOne("SELECT on_hand FROM inventory_level WHERE variant_id = ? "
                + "AND location_id = ?", new Object[] {s.variantId, s.locationId}).get("on_hand"));
    }

    @Test
    void aRefundedBundleRestocksItsComponents() throws Exception {
        Shop s = shop("bdl-refund", 10);
        String set = giftSet(s);
        String user = People.customer(accounts, "bdl-refund-buyer");
        OrderDto o = buy(s, user, set, 3);
        String line = o.lines().get(0).id();
        assertEquals(10 - 6, inventory.available(s.variantId, s.locationId));
        // Two sets ship: four mugs leave the building.
        String fid = fulfillments.create(s.owner, s.storeId, o.id(), new NewFulfillmentDto(
                s.locationId, "LOCAL", Arrays.asList(new FulfilLineDto(line, null, null, 2))))
                .id();
        fulfillments.ship(s.owner, s.storeId, fid);
        assertEquals(10 - 4, onHand(s));
        // Refunding one set without restocking releases the unshipped set
        // first: its two mugs were only promised, and the shelf is unchanged.
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(Arrays.asList(
                new RefundLineDto(line, 1)), 0, false, "damaged"));
        assertEquals(10 - 4, onHand(s));
        assertEquals(10 - 4, inventory.available(s.variantId, s.locationId));
        // Refunding the two shipped sets with restock puts their four mugs
        // back on hand, as moves of the component.
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(Arrays.asList(
                new RefundLineDto(line, 2)), 0, true, "returned"));
        assertEquals(10, onHand(s));
        assertEquals(10, inventory.available(s.variantId, s.locationId));
        // The bundle itself never holds stock.
        assertEquals(0, Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM inventory_level "
                + "WHERE variant_id = ? AND on_hand <> 0", new Object[] {set}).get("n")));
    }

    @Test
    void aShortComponentRefusesTheSale() throws Exception {
        Shop s = shop("bdl-short", 3);
        String set = giftSet(s);
        String user = People.customer(accounts, "bdl-short-buyer");
        assertThrows(ResponseStatusException.class, () -> buy(s, user, set, 2));
        assertEquals(3, inventory.available(s.variantId, s.locationId));
    }

    @Test
    void theProductPageListsWhatTheBundleIncludes() throws Exception {
        Shop s = shop("bdl-page", 4);
        giftSet(s);
        String html = page("/s/bdl-page/products/" + s.storeId + "-set");
        assertTrue(html.contains("Includes"), html);
        assertTrue(html.contains("2 x Mug - Blue"), html);
    }

    @Test
    void aBundleCannotBeNestedAndCanBeTurnedOff() throws Exception {
        Shop s = shop("bdl-nest", 4);
        String set = giftSet(s);
        ResponseStatusException nested = assertThrows(ResponseStatusException.class,
                () -> catalog.setBundle(s.owner, s.storeId, s.productId, new BundleDto(
                    s.productId, true, Arrays.asList(new BundlePartDto(set, "", 1, "", 0)), 0)));
        assertEquals(400, nested.getStatus());
        BundleDto off = catalog.setBundle(s.owner, s.storeId, productOf(set),
                new BundleDto(productOf(set), false, null, 0));
        assertFalse(off.bundle());
        assertTrue(off.parts().isEmpty());
    }

    @Test
    void anOfflinePosSaleOfABundleTakesItsComponents() throws Exception {
        Shop s = shop("bdl-pos", 10);
        String set = giftSet(s);
        String base = "/api/merchant/stores/" + s.storeId + "/pos";
        String registerId = register(s, base);
        String shiftId = (String) Rest.json(mvc, People.asMerchant(Rest.body(post(base
                + "/registers/" + registerId + "/shifts"), "{\"openingCents\":10000,\"note\":\"\"}"),
                s.owner)).get("id");
        Map catalogue = Rest.json(mvc, People.asMerchant(get(base + "/registers/" + registerId
                + "/catalog"), s.owner));
        boolean listed = false;
        for (Object item : (List) catalogue.get("items")) {
            if (set.equals(((Map) item).get("variantId"))) {
                listed = true;
                assertEquals(5, Rest.number((Map) item, "available"));
            }
        }
        assertTrue(listed, String.valueOf(catalogue));
        String sale = "{\"clientId\":\"set-1\",\"shiftId\":\"" + shiftId + "\",\"lines\":[{"
                + "\"variantId\":\"" + set + "\",\"quantity\":1,\"unitCents\":0}],\"tenders\":["
                + "{\"kind\":\"CASH\",\"amountCents\":5000,\"reference\":\"\"}]}";
        Map out = Rest.json(mvc, People.asMerchant(Rest.body(post(base + "/registers/" + registerId
                + "/sales"), "{\"sales\":[" + sale + "," + sale + "]}"), s.owner));
        List results = (List) out.get("results");
        assertEquals("OK", ((Map) results.get(0)).get("status"), String.valueOf(results));
        assertEquals("DUPLICATE", ((Map) results.get(1)).get("status"));
        assertEquals(8, inventory.available(s.variantId, s.locationId));
    }

    // ============================================================== schedules

    @Test
    void aProductBeforeItsOnlineWindowIsNowhereOnTheStorefront() throws Exception {
        Shop s = shop("sch-web", 5);
        long now = System.currentTimeMillis();
        String mugPage = "/s/sch-web/products/sch-web-mug";
        assertEquals(200, status(mugPage));
        assertTrue(page("/s/sch-web").contains("sch-web-mug"));
        assertTrue(page("/s/sch-web/search?q=mug").contains("sch-web-mug"));
        assertTrue(page("/sitemap.xml").contains("sch-web-mug"));
        assertTrue(page("/s/sch-web/feeds/default/products.xml").contains("sch-web-mug"));
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now + HOUR, 0, now);
        assertEquals(404, status(mugPage));
        assertFalse(page("/s/sch-web").contains("sch-web-mug"));
        assertFalse(page("/s/sch-web/search?q=mug").contains("sch-web-mug"));
        assertFalse(page("/sitemap.xml").contains("sch-web-mug"));
        assertFalse(page("/s/sch-web/feeds/default/products.xml").contains("sch-web-mug"));
        String user = People.customer(accounts, "sch-web-buyer");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> carts.add(s.storeId,
                user, null, new CartLineChangeDto(s.variantId, 1))).getStatus());
        // The job opens the window when its time comes: the same window, as
        // set an hour and a half ago, is opened by the clock reaching it.
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now - HOUR, 0, now - 2 * HOUR);
        assertEquals(404, status(mugPage));
        assertTrue(schedules.tick(now) >= 1);
        assertEquals(200, status(mugPage));
        assertTrue(page("/sitemap.xml").contains("sch-web-mug"));
        assertTrue(page("/s/sch-web/feeds/default/products.xml").contains("sch-web-mug"));
    }

    @Test
    void checkoutRefusesAnItemWhoseWindowClosedInTheCart() throws Exception {
        Shop s = shop("sch-cart", 5);
        String user = People.customer(accounts, "sch-cart-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        String rate = c.shippingOptions().get(0).rateId();
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        // A window that closed an hour ago, set before it did.
        long now = System.currentTimeMillis();
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now - 2 * HOUR, now - HOUR,
                now - 90 * 60000L);
        schedules.tick(now);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY, rate,
                    (String) card.get("id"), user + "@example.com", "", null, 0, null)));
        // The cart drops a line no longer sold, or the quote refuses it.
        String why = String.valueOf(refused.getMessage());
        assertTrue(why.contains("no longer sold") || why.contains("cart is empty"), why);
        assertTrue(carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null)).lines()
                .isEmpty(), "the cart lets go of a line no longer sold");
        assertEquals(5, inventory.available(s.variantId, s.locationId));
    }

    @Test
    void thePosCatalogLeavesOutWhatIsNotOnSaleThere() throws Exception {
        Shop s = shop("sch-pos", 5);
        String base = "/api/merchant/stores/" + s.storeId + "/pos";
        String url = base + "/registers/" + register(s, base) + "/catalog";
        assertEquals(1, ((List) Rest.json(mvc, People.asMerchant(get(url), s.owner))
                .get("items")).size());
        long now = System.currentTimeMillis();
        catalog.setSchedule(s.owner, s.storeId, s.productId, new ChannelScheduleDto(Schedules.POS,
                "", now + HOUR, 0, true));
        assertTrue(((List) Rest.json(mvc, People.asMerchant(get(url), s.owner)).get("items"))
                .isEmpty());
        // The online store is not affected.
        assertEquals(200, status("/s/sch-pos/products/sch-pos-mug"));
    }

    @Test
    void aMarketplaceListingOutsideItsWindowIsPushedAsNoneLeft() throws Exception {
        Shop s = shop("sch-chan", 30);
        List<ChannelDto> cs = channels.connect(s.owner, s.storeId, new ChannelSetupDto(
                SimulatedConnector.KIND, "Market", "", ChannelSyncTest.SECRET));
        String ch = cs.get(cs.size() - 1).id();
        channels.listVariant(s.owner, s.storeId, ch, new ChannelListingDto(null, s.variantId, null,
                null, null, null, 0, 0, 0, null));
        long now = System.currentTimeMillis();
        sync.tick(now);
        assertEquals(30, connectors.simulated().listing(ch, s.sku)[0]);
        schedules.set(s.storeId, s.productId, ch, now - 2 * HOUR, now - HOUR, now - 90 * 60000L);
        schedules.tick(now);
        sync.run(ch, now + 1000, true);
        assertEquals(0, connectors.simulated().listing(ch, s.sku)[0]);
        boolean found = false;
        for (ChannelScheduleDto w : catalog.schedule(s.owner, s.storeId, s.productId)) {
            if (ch.equals(w.channel())) {
                found = true;
                assertFalse(w.visible());
            }
        }
        assertTrue(found);
    }
}
