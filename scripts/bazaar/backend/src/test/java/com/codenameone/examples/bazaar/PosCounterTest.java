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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The counter's links to the rest of the store: a card reader the server
/// drives, the pickup queue, sales shipped to the customer and commissions
/// kept at the rate of the day of the sale.
@BackendTest
class PosCounterTest {
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
    private StoreOrderRepository orderRows;
    @Autowired
    private PaymentRepository paymentRows;

    private final MailSender mail = MailSender.current();

    /// A shop with a register at its location taxed as New York (4%), a
    /// shift open on it and a cashier with PIN 1234.
    private final class Till {
        final Shop shop;
        final String store;
        final String base;
        final String registerId;
        final String shiftId;
        final String cashier;

        Till(String name, int stock) throws Exception {
            shop = new Shop(name, stock, accounts, stores, pricing, db, inventory, carts,
                    checkouts, mvc);
            store = "/api/merchant/stores/" + shop.storeId;
            base = store + "/pos";
            registerId = (String) owner(post(base + "/registers"), "{\"locationId\":\""
                    + shop.locationId + "\",\"name\":\"Front\",\"active\":true,"
                    + "\"taxCountry\":\"US\",\"taxRegion\":\"NY\"}").get("id");
            shiftId = (String) owner(post(base + "/registers/" + registerId + "/shifts"),
                    "{\"openingCents\":10000,\"note\":\"\"}").get("id");
            cashier = People.customer(accounts, name + "-cashier");
            owner(post(store + "/staff"), "{\"username\":\"" + cashier
                    + "\",\"role\":\"CASHIER\",\"pin\":\"1234\"}");
        }

        Map owner(MockRequestBuilder r, String json) throws Exception {
            return as(r, json, shop.owner);
        }

        Map as(MockRequestBuilder r, String json, String who) throws Exception {
            return Rest.json(mvc, People.asMerchant(json == null ? r : Rest.body(r, json), who));
        }

        int status(MockRequestBuilder r, String json, String who) throws Exception {
            return Rest.statusOf(mvc, People.asMerchant(json == null ? r : Rest.body(r, json),
                    who));
        }

        String reg() {
            return base + "/registers/" + registerId;
        }

        Map sync(String sale) throws Exception {
            Map out = owner(post(reg() + "/sales"), "{\"sales\":[" + sale + "]}");
            return (Map) ((List) out.get("results")).get(0);
        }

        String sale(String clientId, String lines, String tenders, String extra) {
            return "{\"clientId\":\"" + clientId + "\",\"shiftId\":\"" + shiftId + "\","
                    + extra + "\"lines\":[" + lines + "],\"tenders\":[" + tenders + "]}";
        }

        String mug(int qty) {
            return "{\"variantId\":\"" + shop.variantId + "\",\"quantity\":" + qty
                    + ",\"unitCents\":0}";
        }

        Map pairReader() throws Exception {
            return owner(post(base + "/readers"), "{\"label\":\"Front reader\",\"registerId\":\""
                    + registerId + "\",\"registrationCode\":\"simulated-front\"}");
        }

        Map startCard(String clientId, long cents, String who) throws Exception {
            return as(post(reg() + "/card-payments"), "{\"clientId\":\"" + clientId
                    + "\",\"amountCents\":" + cents + "}", who);
        }

        Map poll(String paymentId) throws Exception {
            return owner(get(base + "/card-payments/" + paymentId), null);
        }
    }

    private static String tender(String kind, long cents, String reference) {
        return "{\"kind\":\"" + kind + "\",\"amountCents\":" + cents + ",\"reference\":\""
                + reference + "\"}";
    }

    // ------------------------------------------------------------ card reader

    @Test
    void theReaderTakesTheCardAndTheSaleSpendsItOnce() throws Exception {
        Till t = new Till("pc-reader", 10);
        Map reader = t.pairReader();
        assertEquals("ACTIVE", reader.get("status"));
        assertTrue(((String) reader.get("serial")).startsWith("SIM-"));
        assertEquals(1, Rest.list(mvc, People.asMerchant(get(t.base + "/readers"),
                t.shop.owner)).size());
        // A cashier cannot pair a reader, but takes payments on it.
        assertTrue(t.status(post(t.base + "/readers"), "{\"registerId\":\""
                + t.registerId + "\",\"registrationCode\":\"simulated-x\"}", t.cashier) >= 400);

        Map p = t.startCard("cp-1", 1040, t.cashier);
        String id = (String) p.get("id");
        assertEquals("PENDING", p.get("status"));
        // Asked again with the same client id, the same payment answers.
        assertEquals(id, t.startCard("cp-1", 1040, t.cashier).get("id"));
        // The reader is still waiting for the card, then the card is tapped.
        assertEquals("PENDING", t.poll(id).get("status"));
        Map done = t.poll(id);
        assertEquals("SUCCEEDED", done.get("status"), String.valueOf(done.get("message")));
        assertEquals("Visa 4242", done.get("cardLabel"));

        // A tender that is not what the reader took is refused.
        Map wrong = t.sync(t.sale("cp-sale-0", t.mug(1), tender("CARD", 2080, id), ""));
        assertEquals("ERROR", wrong.get("status"));
        Map r = t.sync(t.sale("cp-sale-1", t.mug(1), tender("CARD", 1040, id), ""));
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        String orderId = (String) r.get("orderId");
        com.codenameone.examples.bazaar.domain.Payment pay = paymentRows.findById(orderId);
        assertTrue(pay.getMethodLabel().contains("Visa 4242"), pay.getMethodLabel());
        assertTrue(pay.getProviderRef().startsWith("sim_tpi_"), pay.getProviderRef());
        // The same payment cannot pay a second sale.
        Map twice = t.sync(t.sale("cp-sale-2", t.mug(1), tender("CARD", 1040, id), ""));
        assertEquals("ERROR", twice.get("status"));
        // A standalone terminal's card tender still sells as it did.
        Map plain = t.sync(t.sale("cp-sale-3", t.mug(1), tender("CARD", 1040, "1234"), ""));
        assertEquals("OK", plain.get("status"), String.valueOf(plain.get("message")));
    }

    @Test
    void aDeclinedOrCancelledPaymentCanBeTriedAgain() throws Exception {
        Till t = new Till("pc-decline", 10);
        // Without a reader at the register there is nothing to push to.
        assertEquals(409, t.status(post(t.reg() + "/card-payments"),
                "{\"clientId\":\"cd-0\",\"amountCents\":1040}", t.shop.owner));
        t.pairReader();
        // The simulated reader declines amounts ending in 01.
        String id = (String) t.startCard("cd-1", 1001, t.shop.owner).get("id");
        t.poll(id);
        Map declined = t.poll(id);
        assertEquals("FAILED", declined.get("status"));
        assertTrue(((String) declined.get("message")).contains("declined"));
        Map refused = t.sync(t.sale("cd-sale", t.mug(1), tender("CARD", 1001, id), ""));
        assertEquals("ERROR", refused.get("status"));
        // Retried under the same client id, a new attempt starts.
        Map again = t.startCard("cd-1", 1040, t.shop.owner);
        assertEquals("PENDING", again.get("status"));
        Map cancelled = t.owner(post(t.base + "/card-payments/" + again.get("id") + "/cancel"),
                null);
        assertEquals("CANCELED", cancelled.get("status"));
        // Unpaired, the register has no reader again.
        String readerId = (String) ((Map) Rest.list(mvc, People.asMerchant(
                get(t.base + "/readers"), t.shop.owner)).get(0)).get("id");
        assertEquals("REMOVED", t.owner(post(t.base + "/readers/" + readerId + "/remove"), null)
                .get("status"));
        assertEquals(409, t.status(post(t.reg() + "/card-payments"),
                "{\"clientId\":\"cd-2\",\"amountCents\":1040}", t.shop.owner));
    }

    // ----------------------------------------------------------------- pickup

    private CartDto estimate(Shop s, String user) throws Exception {
        return carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
    }

    private static ShippingOptionDto pickup(CartDto c) {
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("PICKUP".equals(o.kind())) {
                return o;
            }
        }
        return null;
    }

    @Test
    void checkoutOffersPickupOnlyWhereTheLocationHasStock() throws Exception {
        Till t = new Till("pc-stock", 5);
        String user = People.customer(accounts, "pc-stock-buyer");
        carts.add(t.shop.storeId, user, null, new CartLineChangeDto(t.shop.variantId, 2));
        ShippingOptionDto here = pickup(estimate(t.shop, user));
        assertEquals("Pick up at Back room", here == null ? null : here.name());
        // Move the pickup to a location with nothing on its shelf.
        String kiosk = stores.addLocation(t.shop.owner, t.shop.storeId,
                new com.codenameone.examples.bazaar.api.LocationDto(null, "Kiosk", "RETAIL",
                    "1 Mall Rd", true, true, 0)).id();
        db.execute("UPDATE shipping_rate SET location_id = ? WHERE store_id = ? AND kind = ?",
                new Object[] {kiosk, t.shop.storeId, "PICKUP"});
        assertEquals(null, pickup(estimate(t.shop, user)));
        inventory.restock(t.shop.storeId, t.shop.variantId, kiosk, 1, "kiosk", "test",
                Inventory.RESTOCK);
        assertEquals(null, pickup(estimate(t.shop, user)), "one on the shelf, two wanted");
        inventory.restock(t.shop.storeId, t.shop.variantId, kiosk, 1, "kiosk", "test",
                Inventory.RESTOCK);
        assertEquals("Pick up at Kiosk", pickup(estimate(t.shop, user)).name());
    }

    @Test
    void thePickupQueueMakesOrdersReadyMailsTheShopperAndHandsThemOver() throws Exception {
        Till t = new Till("pc-queue", 5);
        String user = People.customer(accounts, "pc-queue-buyer");
        carts.add(t.shop.storeId, user, null, new CartLineChangeDto(t.shop.variantId, 1));
        ShippingOptionDto rate = pickup(estimate(t.shop, user));
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        String email = "pc-queue-buyer@example.com";
        OrderDto o = checkouts.place(user, t.shop.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                rate.rateId(), (String) card.get("id"), email, "", null, 0, null));
        // An order shipped the usual way is not in the queue.
        t.shop.buy(People.customer(accounts, "pc-queue-other"), 1);

        List queue = Rest.list(mvc, People.asMerchant(get(t.reg() + "/pickups"), t.cashier));
        assertEquals(1, queue.size());
        Map first = (Map) queue.get(0);
        assertEquals(o.id(), first.get("orderId"));
        assertEquals("PENDING", first.get("status"));
        assertEquals(409, t.status(post(t.base + "/pickups/" + o.id() + "/picked-up"), null,
                t.cashier));

        int before = mail.sent().size();
        Map ready = t.as(post(t.base + "/pickups/" + o.id() + "/ready"), null, t.cashier);
        assertEquals("PACKED", ready.get("status"));
        boolean mailed = false;
        List<Mail> sent = mail.sent();
        for (int iter = before; iter < sent.size(); iter++) {
            Mail m = sent.get(iter);
            mailed |= m.getTo().contains(email) && m.getSubject().contains("is ready at Back room");
        }
        assertTrue(mailed, "the shopper is told the order is ready");

        Map handed = t.as(post(t.base + "/pickups/" + o.id() + "/picked-up"), null, t.cashier);
        assertEquals("PICKED_UP", handed.get("status"));
        assertEquals(Orders.FULFILLED, orderRows.findById(o.id()).getFulfilment());
        assertEquals(0, Rest.list(mvc, People.asMerchant(get(t.reg() + "/pickups"), t.cashier))
                .size());
        // Not a pickup: refused.
        StoreOrder shipped = orderRows.findByStoreIdOrderByCreatedAtDesc(t.shop.storeId).get(0);
        String other = shipped.getId().equals(o.id())
                ? orderRows.findByStoreIdOrderByCreatedAtDesc(t.shop.storeId).get(1).getId()
                : shipped.getId();
        assertEquals(409, t.status(post(t.base + "/pickups/" + other + "/ready"), null,
                t.cashier));
    }

    // ------------------------------------------------------- ship to customer

    private static final String CALIFORNIA = "{\"name\":\"Grace Hopper\",\"line1\":\"2 Bay St\","
            + "\"line2\":\"\",\"city\":\"San Jose\",\"region\":\"CA\",\"postal\":\"95112\","
            + "\"country\":\"US\",\"phone\":\"\",\"lat\":0,\"lng\":0}";

    @Test
    void aSaleShippedToTheCustomerIsPaidHeldAndLeftToShip() throws Exception {
        Till t = new Till("pc-ship", 5);
        List options = Rest.list(mvc, People.asMerchant(Rest.body(post(t.reg() + "/ship-quote"),
                "{\"lines\":[" + t.mug(1) + "],\"address\":" + CALIFORNIA + "}"), t.cashier));
        String express = null;
        long expressTotal = 0;
        for (Object o : options) {
            Map m = (Map) o;
            assertFalse(String.valueOf(m.get("name")).startsWith("Pick up"),
                    "no pickup when shipping");
            if ("Express".equals(m.get("name"))) {
                express = (String) m.get("rateId");
                assertEquals(1999, ((Number) m.get("shippingCents")).intValue());
                expressTotal = ((Number) m.get("totalCents")).longValue();
            }
        }
        assertTrue(express != null, String.valueOf(options));
        int onHand = level(t).getOnHand();
        Map quoteOnly = t.as(post(t.reg() + "/ship-sales"), "{\"clientId\":\"ps-0\",\"shiftId\":\""
                + t.shiftId + "\",\"lines\":[" + t.mug(1) + "],\"tenders\":["
                + tender("CASH", 5000, "") + "],\"address\":" + CALIFORNIA
                + ",\"shippingRateId\":\"nope\"}", t.cashier);
        assertEquals("ERROR", quoteOnly.get("status"));
        // California's 7.25% on the mug and the shipping, as the store taxes them.
        Map r = t.as(post(t.reg() + "/ship-sales"), "{\"clientId\":\"ps-1\",\"shiftId\":\""
                + t.shiftId + "\",\"email\":\"ps@example.com\",\"lines\":[" + t.mug(1)
                + "],\"tenders\":[" + tender("CASH", 5000, "") + "],\"address\":" + CALIFORNIA
                + ",\"shippingRateId\":\"" + express + "\"}", t.cashier);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        StoreOrder o = orderRows.findById((String) r.get("orderId"));
        assertEquals(Orders.OPEN, o.getStatus());
        assertEquals(Orders.PAID, o.getFinancial());
        assertEquals(Orders.UNFULFILLED, o.getFulfilment());
        assertEquals(1999, o.getShippingCents());
        assertEquals("San Jose", o.getShipCity());
        assertEquals(o.getTotalCents(), ((Number) r.get("totalCents")).longValue());
        assertEquals(expressTotal, o.getTotalCents(), "the quote's total is the sale's");
        assertTrue(o.getTaxCents() >= 72, "taxed where it goes: " + o.getTaxCents());
        // Held for packing, still on the shelf until it ships.
        InventoryLevel l = level(t);
        assertEquals(onHand, l.getOnHand());
        assertEquals(1, l.getCommitted());
        // It goes out like an online order: a fulfilment can be made for it.
        assertEquals(200, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(t.store
                + "/orders/" + o.getId() + "/fulfillments"), "{\"locationId\":\""
                + t.shop.locationId + "\",\"method\":\"SHIP\",\"lines\":[{\"orderLineId\":\""
                + lineOf(o.getId()) + "\",\"title\":\"\",\"sku\":\"\",\"quantity\":1}]}"),
                t.shop.owner)));
    }

    private InventoryLevel level(Till t) throws Exception {
        return inventory.level(t.shop.storeId, t.shop.variantId, t.shop.locationId);
    }

    private String lineOf(String orderId) throws Exception {
        List rows = db.query("SELECT id FROM order_line WHERE order_id = ?",
                new Object[] {orderId});
        return ((Map) rows.get(0)).get("id").toString();
    }

    // ------------------------------------------------------------ commissions

    @Test
    void aSaleKeepsTheCommissionRateOfTheDayItWasMade() throws Exception {
        Till t = new Till("pc-comm", 10);
        t.owner(post(t.base + "/staff-rates"), "{\"username\":\"" + t.cashier
                + "\",\"displayName\":\"\",\"role\":\"\",\"rateBp\":500}");
        Map first = t.sync(t.sale("pcc-1", t.mug(1), tender("CASH", 2000, ""),
                "\"staffUsername\":\"" + t.cashier + "\","));
        assertEquals("OK", first.get("status"), String.valueOf(first.get("message")));
        assertEquals(500, orderRows.findById((String) first.get("orderId")).getCommissionBp());
        t.owner(post(t.base + "/staff-rates"), "{\"username\":\"" + t.cashier
                + "\",\"displayName\":\"\",\"role\":\"\",\"rateBp\":1000}");
        Map second = t.sync(t.sale("pcc-2", t.mug(1), tender("CASH", 2000, ""),
                "\"staffUsername\":\"" + t.cashier + "\","));
        assertEquals("OK", second.get("status"), String.valueOf(second.get("message")));
        Map mine = null;
        for (Object o : Rest.list(mvc, People.asMerchant(get(t.base + "/commissions"),
                t.shop.owner))) {
            if (t.cashier.equals(((Map) o).get("username"))) {
                mine = (Map) o;
            }
        }
        // 5% of the first mug and 10% of the second, not 10% of both.
        assertEquals(2000, ((Number) mine.get("netCents")).intValue());
        assertEquals(150, ((Number) mine.get("commissionCents")).intValue());
    }
}
