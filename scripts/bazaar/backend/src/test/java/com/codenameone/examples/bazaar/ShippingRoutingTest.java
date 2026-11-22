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
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.api.RoutingDto;
import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingRateDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.routing.OrderRouting;
import com.codenameone.examples.bazaar.routing.Router;
import com.codenameone.examples.bazaar.routing.SimulatedFulfilmentService;
import com.codenameone.examples.bazaar.routing.ThirdPartyFulfilment;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import com.codenameone.examples.bazaar.api.WebhookDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Shipping profiles priced per profile and added up, order routing rules,
/// the simulated fulfilment service shipping with tracking, the prepaid
/// return label on approval and the customs form of an international parcel.
@BackendTest
class ShippingRoutingTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Orders orders;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private Returns returns;
    @Autowired
    private RateSettings rates;
    @Autowired
    private OrderRouting routing;
    @Autowired
    private ThirdPartyFulfilment thirdParty;
    @Autowired
    private Documents documents;
    @Autowired
    private OrderLineRepository lines;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private Webhooks webhooks;

    private final MailSender mail = MailSender.current();

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    /// A second, heavy product with one tracked variant and stock.
    private String[] crate(Shop s, String name) throws Exception {
        String product = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Crate', '', '', '', '', "
                + "'ACTIVE', '', '', '', 0, 0, 0, 0)", new Object[] {product, s.storeId, name});
        String variant = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'Oak', '', '', '', ?, '', 3000, 0, 900, 9000, 1, 1, 0, 0)",
                new Object[] {variant, product, s.storeId, name + "-SKU"});
        inventory.restock(s.storeId, variant, s.locationId, 10, "opening", "test",
                Inventory.RESTOCK);
        return new String[] {product, variant};
    }

    private static long price(CartDto c, String name) {
        for (ShippingOptionDto o : c.shippingOptions()) {
            if (name.equals(o.name())) {
                return o.priceCents();
            }
        }
        return -1;
    }

    private static void expect(int status, Action a) throws Exception {
        try {
            a.run();
            fail("expected " + status);
        } catch (ResponseStatusException e) {
            assertEquals(status, e.getStatus(), e.getReason());
        }
    }

    /// The one mail whose text names `text`, failing with every subject sent.
    private Mail mailWith(String text) throws Exception {
        Mail found = null;
        StringBuilder had = new StringBuilder();
        for (Mail m : mail.sent()) {
            had.append(m.getTo()).append(' ').append(m.getSubject()).append("; ");
            String body = m.getText() == null ? "" : m.getText();
            String html = m.getHtml() == null ? "" : m.getHtml();
            if (body.contains(text) || html.contains(text)) {
                found = m;
            }
        }
        if (found == null) {
            fail("no mail names " + text + " (sent: " + had + ")");
        }
        return found;
    }

    private interface Action {
        void run() throws Exception;
    }

    // ------------------------------------------------------------- profiles

    @Test
    void aMixedCartPaysEachProfilesRate() throws Exception {
        Shop s = shop("profiles", 10);
        String[] crate = crate(s, "profiles-crate");
        List<String> ids = new ArrayList<String>();
        ids.add(crate[0]);
        ShippingProfileDto big = rates.saveProfile(s.owner, s.storeId,
                new ShippingProfileDto(null, "Oversized", 0, ids));
        assertEquals(1, rates.profiles(s.owner, s.storeId).size());
        ShippingZoneDto zone = rates.saveZone(s.owner, s.storeId, new ShippingZoneDto(null,
                "Oversized US", "US", 0, new ArrayList<ShippingRateDto>(), big.id()));
        assertEquals(big.id(), zone.profileId());
        rates.saveRate(s.owner, s.storeId, new ShippingRateDto(null, zone.id(), "Freight",
                ShippingCalculator.FLAT, 2500, 0, 0, "", "", 3, 8, "", 0, 0, 0, 0));

        String user = People.customer(accounts, "profiles-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto mugOnly = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        assertEquals(1999, price(mugOnly, "Express"));
        assertEquals(-1, price(mugOnly, "Freight"), "the general zones price the mug");

        carts.add(s.storeId, user, null, new CartLineChangeDto(crate[1], 1));
        CartDto mixed = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        // The mug's Express plus the crate's cheapest rate in its own profile.
        assertEquals(1999 + 2500, price(mixed, "Express"));
        for (ShippingOptionDto o : mixed.shippingOptions()) {
            if (!ShippingCalculator.PICKUP.equals(o.kind())) {
                assertTrue(o.etaDaysMax() >= 8, o.name() + " waits for the slower profile");
            }
        }

        String alone = People.customer(accounts, "profiles-crate-only");
        carts.add(s.storeId, alone, null, new CartLineChangeDto(crate[1], 1));
        CartDto crateOnly = carts.estimate(s.storeId, alone, null,
                new EstimateDto(Shop.NY, null));
        assertEquals(2500, price(crateOnly, "Freight"));
        assertEquals(-1, price(crateOnly, "Express"), "only the profile's own zones");

        // A profile with no zone for the address: nothing can be delivered.
        rates.deleteZone(s.owner, s.storeId, zone.id());
        CartDto none = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        for (ShippingOptionDto o : none.shippingOptions()) {
            assertEquals(ShippingCalculator.PICKUP, o.kind(), o.name());
        }

        // Another store's product cannot be put in a profile, nor a zone in
        // another store's profile.
        Shop other = shop("profiles-other", 1);
        List<String> foreign = new ArrayList<String>();
        foreign.add(other.productId);
        expect(404, () -> rates.saveProfile(s.owner, s.storeId,
                new ShippingProfileDto(null, "Theirs", 1, foreign)));
        expect(404, () -> rates.saveZone(other.owner, other.storeId, new ShippingZoneDto(null,
                "Sneaky", "US", 0, new ArrayList<ShippingRateDto>(), big.id())));
        rates.deleteProfile(s.owner, s.storeId, big.id());
        assertEquals(0, rates.profiles(s.owner, s.storeId).size());
        CartDto back = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        assertEquals(1999, price(back, "Express"), "the crate ships under the general zones");
    }

    // -------------------------------------------------------------- routing

    @Test
    void routingRulesChooseTheLocation() throws Exception {
        Shop s = shop("routing", 5);
        String floor = stores.addLocation(s.owner, s.storeId, new LocationDto(null, "Shop floor",
                "RETAIL", "2 High St", true, true, 1)).id();
        inventory.restock(s.storeId, s.variantId, floor, 2, "opening", "test",
                Inventory.RESTOCK);
        RoutingDto start = routing.routing(s.owner, s.storeId);
        assertTrue(start.allowSplit());
        assertTrue(start.rules().isEmpty());

        // No rules: the first location in the store's order holding the units.
        OrderDto first = s.buy(People.customer(accounts, "routing-a"), 1);
        assertEquals(s.locationId, lineOf(first).getLocationId());

        routing.saveRule(s.owner, s.storeId, new RoutingRuleDto(null, Router.PRIORITY, floor, 0));
        OrderDto second = s.buy(People.customer(accounts, "routing-b"), 1);
        assertEquals(floor, lineOf(second).getLocationId(), "the priority list wins");

        // More than the preferred location holds: the one that has them all.
        OrderDto third = s.buy(People.customer(accounts, "routing-c"), 3);
        assertEquals(s.locationId, lineOf(third).getLocationId());

        expect(400, () -> routing.saveRule(s.owner, s.storeId,
                new RoutingRuleDto(null, "NEAREST", "", 0)));
        Shop other = shop("routing-other", 0);
        expect(404, () -> routing.saveRule(s.owner, s.storeId,
                new RoutingRuleDto(null, Router.PRIORITY, other.locationId, 1)));
        expect(404, () -> routing.routing(People.customer(accounts, "routing-stranger"),
                s.storeId));
        RoutingDto read = routing.routing(s.owner, s.storeId);
        assertEquals(1, read.rules().size());
        routing.deleteRule(s.owner, s.storeId, read.rules().get(0).id());
        assertTrue(routing.routing(s.owner, s.storeId).rules().isEmpty());
    }

    private OrderLine lineOf(OrderDto o) {
        return lines.findByOrderId(o.id()).get(0);
    }

    // ---------------------------------------------------- fulfilment service

    @Test
    void theFulfilmentServiceShipsWithTracking() throws Exception {
        Shop s = shop("tpl", 5);
        expect(400, () -> routing.save(s.owner, s.storeId, new RoutingDto(true, "",
                SimulatedFulfilmentService.NAME, new ArrayList<RoutingRuleDto>())));
        routing.save(s.owner, s.storeId, new RoutingDto(true, s.locationId,
                SimulatedFulfilmentService.NAME, new ArrayList<RoutingRuleDto>()));
        WebhookDto hook = webhooks.create(s.owner, s.storeId, new WebhookDto(null,
                Webhooks.ORDERS_FULFILLED, "https://example.com/tpl-hook",
                "tpl-secret-0123456789", true, 0));
        String user = People.customer(accounts, "tpl-buyer");
        OrderDto o = s.buy(user, 2, user);
        long now = System.currentTimeMillis();
        thirdParty.tick(now);
        Map handed = db.queryOne("SELECT status, external_id FROM fulfilment_service_order "
                + "WHERE order_id = ?", new Object[] {o.id()});
        assertNotNull(handed, "the paid order was handed over");
        assertEquals(ThirdPartyFulfilment.ACCEPTED, Rows.text(handed.get("status")));
        thirdParty.tick(now + 1000);
        assertEquals(1, Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM "
                + "fulfilment_service_order WHERE order_id = ?", new Object[] {o.id()}).get("n")),
                "handed over once");

        long step = Long.getLong("bazaar.fulfilment.simulated.step.millis", 60000L).longValue();
        thirdParty.tick(now + step + 1);
        Map shipped = db.queryOne("SELECT status, carrier, tracking_number, fulfillment_id FROM "
                + "fulfilment_service_order WHERE order_id = ?", new Object[] {o.id()});
        assertEquals(ThirdPartyFulfilment.SHIPPED, Rows.text(shipped.get("status")));
        String tracking = Rows.text(shipped.get("tracking_number"));
        assertEquals(SimulatedFulfilmentService.trackingNumber(Rows.text(handed.get(
                "external_id"))), tracking);
        assertTrue(tracking.startsWith("3PL"), tracking);
        FulfillmentDto f = fulfillments.detail(Rows.text(shipped.get("fulfillment_id")));
        assertEquals(Fulfillments.SHIPPED, f.status());
        assertEquals(tracking, f.trackingNumber());
        assertEquals(SimulatedFulfilmentService.CARRIER, f.carrier());
        assertEquals(Orders.FULFILLED, orders.detailOf(o.id()).fulfilment());
        assertEquals(3, inventory.level(s.storeId, s.variantId, s.locationId).getOnHand());
        assertNotNull(mailWith(tracking), "the shopper is mailed the tracking number");
        Map fired = db.queryOne("SELECT COUNT(*) AS n FROM webhook_delivery WHERE webhook_id = ? "
                + "AND topic = ?", new Object[] {hook.id(), Webhooks.ORDERS_FULFILLED});
        assertEquals(1, Rows.number(fired.get("n")), "orders/fulfilled was sent");

        thirdParty.tick(now + 3 * step + 1);
        assertEquals(ThirdPartyFulfilment.DELIVERED, Rows.text(db.queryOne("SELECT status FROM "
                + "fulfilment_service_order WHERE order_id = ?", new Object[] {o.id()})
                .get("status")));
        assertEquals(Fulfillments.DELIVERED, fulfillments.detail(f.id()).status());
        assertEquals(1, routing.serviceOrders(s.owner, s.storeId).size());
    }

    // ------------------------------------------------- returns and customs

    @Test
    void anApprovedReturnGetsAPrepaidLabel() throws Exception {
        Shop s = shop("rlabel", 5);
        String user = People.customer(accounts, "rlabel-buyer");
        OrderDto o = s.buy(user, 1, user);
        String line = lineOf(o).getId();
        List<FulfilLineDto> one = new ArrayList<FulfilLineDto>();
        one.add(new FulfilLineDto(line, null, null, 1));
        FulfillmentDto f = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", one));
        fulfillments.ship(s.owner, s.storeId, f.id());
        fulfillments.delivered(s.owner, s.storeId, f.id());
        ReturnDto r = returns.request(user, o.id(), new ReturnRequestDto("Too big", "", one));
        expect(409, () -> documents.returnLabel(s.storeId, r.id()));
        returns.approve(s.owner, s.storeId, r.id());
        Map row = db.queryOne("SELECT return_carrier, return_tracking, return_label_cents FROM "
                + "return_request WHERE id = ?", new Object[] {r.id()});
        String tracking = Rows.text(row.get("return_tracking"));
        assertTrue(tracking.startsWith("SIM"), tracking);
        assertTrue(Rows.number(row.get("return_label_cents")) > 0);
        Mail sent = mailWith(tracking);
        String name = "return-label-" + o.number() + ".pdf";
        assertTrue(sent.getAttachmentNames().contains(name), sent.getAttachmentNames().toString());
        byte[] pdf = sent.getAttachment(name);
        assertEquals("%PDF-", new String(pdf, 0, 5, "ISO-8859-1"));
        assertEquals("%PDF-", new String(documents.returnLabel(s.storeId, r.id()), 0, 5,
                "ISO-8859-1"));
        Shop other = shop("rlabel-other", 0);
        expect(404, () -> documents.returnLabel(other.storeId, r.id()));
    }

    @Test
    void anInternationalParcelGetsACustomsForm() throws Exception {
        Shop s = shop("customs", 5);
        db.execute("UPDATE variant SET hs_code = '691200', origin_country = 'PT' WHERE id = ?",
                new Object[] {s.variantId});
        OrderDto o = s.buy(People.customer(accounts, "customs-buyer"), 1);
        List<FulfilLineDto> one = new ArrayList<FulfilLineDto>();
        one.add(new FulfilLineDto(lineOf(o).getId(), null, null, 1));
        FulfillmentDto f = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, null, one));
        expect(409, () -> documents.customsForm(s.storeId, f.id()));
        db.execute("UPDATE store_order SET ship_country = 'GB' WHERE id = ?",
                new Object[] {o.id()});
        byte[] pdf = documents.customsForm(s.storeId, f.id());
        assertEquals("%PDF-", new String(pdf, 0, 5, "ISO-8859-1"));
        Shop other = shop("customs-other", 0);
        expect(404, () -> documents.customsForm(other.storeId, f.id()));

        assertTrue(Documents.cn22(1000, "USD", 350));
        assertFalse(Documents.cn22(40001, "USD", 350), "above 300 SDR");
        assertFalse(Documents.cn22(1000, "USD", 2001), "above 2 kg");
        assertTrue(Documents.cn22(30000, "XYZ", 100));
        assertFalse(Documents.cn22(31001, "XYZ", 100), "an unknown currency takes the lowest");
    }

    // ------------------------------------------------------------ back office

    @Test
    void theBackOfficeEditsProfilesAndRouting() throws Exception {
        Shop s = shop("shipweb", 2);
        Browser b = new Browser(mvc);
        b.signIn(s.owner, People.PASSWORD);
        String base = "/admin/" + s.storeId + "/shipping";
        b.assertPage(b.get(base), "Order routing");
        b.post(base + "/profiles", "name", "Fragile", "p-" + s.productId, "1");
        List<ShippingProfileDto> saved = rates.profiles(s.owner, s.storeId);
        assertEquals(1, saved.size());
        assertEquals("Fragile", saved.get(0).name());
        assertEquals(s.productId, saved.get(0).productIds().get(0));
        b.post(base + "/routing", "serviceLocationId", "", "service", "");
        assertFalse(routing.routing(s.owner, s.storeId).allowSplit(), "the box was unticked");
        b.post(base + "/rules", "kind", Router.PRIORITY, "l-" + s.locationId, "1");
        RoutingDto r = routing.routing(s.owner, s.storeId);
        assertEquals(1, r.rules().size());
        assertEquals(s.locationId, r.rules().get(0).locationIds());
        b.assertPage(b.get(base), "Fragile");
    }
}
