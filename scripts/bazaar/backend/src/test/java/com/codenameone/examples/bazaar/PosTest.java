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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.domain.OrderEvent;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.OrderEventRepository;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The point of sale: offline sales uploaded (and uploaded again), tenders,
/// shifts and their cash, PINs, the catalog snapshot and counter returns.
@BackendTest
class PosTest {
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
    private GiftCards giftCards;
    @Autowired
    private GiftCardRepository giftCardRows;
    @Autowired
    private PaymentRepository paymentRows;
    @Autowired
    private AuditEventRepository audits;
    @Autowired
    private OrderEventRepository orderEvents;

    /// A shop with a register at its location, taxed as New York (4%), and a
    /// shift open on it with a $100 float.
    private final class Till {
        final Shop shop;
        final String base;
        final String registerId;
        String shiftId;

        Till(String name, int stock) throws Exception {
            shop = new Shop(name, stock, accounts, stores, pricing, db, inventory, carts,
                    checkouts, mvc);
            base = "/api/merchant/stores/" + shop.storeId + "/pos";
            Map reg = owner(post(base + "/registers"), "{\"locationId\":\"" + shop.locationId
                    + "\",\"name\":\"Front\",\"active\":true,\"taxCountry\":\"US\","
                    + "\"taxRegion\":\"NY\"}");
            registerId = (String) reg.get("id");
            shiftId = (String) owner(post(base + "/registers/" + registerId + "/shifts"),
                    "{\"openingCents\":10000,\"note\":\"\"}").get("id");
        }

        Map owner(com.codename1.backend.test.MockRequestBuilder r, String json) throws Exception {
            return Rest.json(mvc, People.asMerchant(json == null ? r : Rest.body(r, json),
                    shop.owner));
        }

        String sale(String clientId, int qty, String tenders) {
            return sale(clientId, qty, tenders, "");
        }

        String sale(String clientId, int qty, String tenders, String extra) {
            return "{\"clientId\":\"" + clientId + "\",\"shiftId\":\"" + shiftId + "\","
                    + extra + "\"lines\":[{\"variantId\":\"" + shop.variantId
                    + "\",\"quantity\":" + qty + ",\"unitCents\":0}],\"tenders\":[" + tenders
                    + "]}";
        }

        List sync(String... sales) throws Exception {
            StringBuilder b = new StringBuilder("{\"sales\":[");
            for (int iter = 0; iter < sales.length; iter++) {
                b.append(iter == 0 ? "" : ",").append(sales[iter]);
            }
            Map out = owner(post(base + "/registers/" + registerId + "/sales"),
                    b.append("]}").toString());
            return (List) out.get("results");
        }

        Map shift() throws Exception {
            return owner(get(base + "/shifts/" + shiftId), null);
        }

        int available() throws Exception {
            return inventory.available(shop.variantId, shop.locationId);
        }
    }

    private static String cash(long cents) {
        return "{\"kind\":\"CASH\",\"amountCents\":" + cents + ",\"reference\":\"\"}";
    }

    @Test
    void anOfflineBatchSentTwiceIsRecordedOnce() throws Exception {
        Till t = new Till("pos-replay", 10);
        String a = t.sale("sale-a", 2, cash(5000));
        String b = t.sale("sale-b", 1, cash(5000));
        List first = t.sync(a, b, a);
        assertEquals("OK", ((Map) first.get(0)).get("status"));
        assertEquals("OK", ((Map) first.get(1)).get("status"));
        assertEquals("DUPLICATE", ((Map) first.get(2)).get("status"));
        assertEquals(((Map) first.get(0)).get("orderId"), ((Map) first.get(2)).get("orderId"));
        assertEquals(7, t.available());
        List again = t.sync(a, b);
        assertEquals("DUPLICATE", ((Map) again.get(0)).get("status"));
        assertEquals("DUPLICATE", ((Map) again.get(1)).get("status"));
        assertEquals(Rest.number((Map) first.get(1), "number"),
                Rest.number((Map) again.get(1), "number"));
        assertEquals(7, t.available());
        assertEquals(2, Rest.number(t.shift(), "salesCount"));
    }

    @Test
    void aSaleOfMoreThanOnHandIsTakenAndFlagged() throws Exception {
        Till t = new Till("pos-oversold", 1);
        Map r = (Map) t.sync(t.sale("big", 3, cash(10000))).get(0);
        assertEquals("OK", r.get("status"));
        assertEquals(Boolean.TRUE, r.get("oversold"));
        assertEquals(-2, t.available());
        boolean audited = false;
        for (AuditEvent e : audits.findByStoreIdOrderByCreatedAtDesc(t.shop.storeId)) {
            audited |= "POS_OVERSOLD".equals(e.getAction()) && r.get("orderId").equals(e.getSubject());
        }
        assertTrue(audited);
    }

    @Test
    void cashAndCardSplitGivesChangeAndCapturesTheCard() throws Exception {
        Till t = new Till("pos-split", 10);
        // One $10 mug, 4% New York tax: $10.40.
        Map r = (Map) t.sync(t.sale("split", 1, "{\"kind\":\"CARD\",\"amountCents\":500,"
                + "\"reference\":\"A1\"}," + cash(1000))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(1040, Rest.number(r, "totalCents"));
        assertEquals(460, Rest.number(r, "changeCents"));
        String orderId = (String) r.get("orderId");
        assertEquals(Payments.PAID, paymentRows.findById(orderId).getStatus());
        assertTrue(paymentRows.findById(orderId).getProviderRef().startsWith(Payments.TERMINAL));
        Map shift = t.shift();
        assertEquals(540, Rest.number(shift, "cashSalesCents"));
        assertEquals(500, Rest.number(shift, "cardSalesCents"));
        assertEquals(10540, Rest.number(shift, "expectedCents"));
        Map receipt = t.owner(get("/api/merchant/stores/" + t.shop.storeId + "/pos/orders/"
                + orderId + "/receipt"), null);
        assertTrue(((String) receipt.get("text")).contains("Change"));
        assertEquals(200, Rest.statusOf(mvc, People.asMerchant(get((String) receipt.get("pdfPath")),
                t.shop.owner)));
    }

    @Test
    void aGiftCardPaysPartOfASale() throws Exception {
        Till t = new Till("pos-gift", 10);
        GiftCardDto card = giftCards.issue(t.shop.owner, t.shop.storeId,
                new GiftCardDto(null, null, null, 2000, 0, true, "", 0, "", 0, "", "", "", 0, 0));
        Map r = (Map) t.sync(t.sale("gift", 1, "{\"kind\":\"GIFT_CARD\",\"amountCents\":600,"
                + "\"reference\":\"" + card.code() + "\"}," + cash(500))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(60, Rest.number(r, "changeCents"));
        assertEquals(1400, giftCardRows.findById(card.id()).getBalanceCents());
        Map tooMuch = (Map) t.sync(t.sale("gift2", 1, "{\"kind\":\"GIFT_CARD\","
                + "\"amountCents\":1500,\"reference\":\"" + card.code() + "\"}")).get(0);
        assertEquals("ERROR", tooMuch.get("status"));
        assertEquals(1400, giftCardRows.findById(card.id()).getBalanceCents());
        assertEquals(9, t.available());
    }

    @Test
    void storeCreditPaysFromTheCustomersBalance() throws Exception {
        Till t = new Till("pos-credit", 10);
        String customerId = Ids.next();
        db.execute("INSERT INTO customer (id, store_id, username, email, phone, first_name, "
                + "last_name, tags, accepts_marketing, note, orders_count, spent_cents, "
                + "store_credit_cents, created_at, updated_at) VALUES (?, ?, '', "
                + "'grace@example.com', '+1 555 010 9999', 'Grace', 'Hopper', '', 0, '', 0, 0, "
                + "700, 0, 0)", new Object[] {customerId, t.shop.storeId});
        List found = Rest.list(mvc, People.asMerchant(get(t.base + "/customers?q=hopp"),
                t.shop.owner));
        assertEquals(1, found.size());
        assertEquals(1, Rest.list(mvc, People.asMerchant(get(t.base + "/customers?q=0109999"),
                t.shop.owner)).size());
        Map r = (Map) t.sync(t.sale("credit", 1, "{\"kind\":\"STORE_CREDIT\",\"amountCents\":"
                + "700,\"reference\":\"\"}," + cash(340),
                "\"customerId\":\"" + customerId + "\",")).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        Map after = (Map) Rest.list(mvc, People.asMerchant(get(t.base + "/customers?q=grace"),
                t.shop.owner)).get(0);
        assertEquals(0, Rest.number(after, "storeCreditCents"));
        assertEquals(1, Rest.number(after, "ordersCount"));
    }

    @Test
    void closingAShiftReportsTheVariance() throws Exception {
        Till t = new Till("pos-close", 10);
        t.sync(t.sale("c1", 1, cash(2000)));
        t.owner(post(t.base + "/shifts/" + t.shiftId + "/cash"),
                "{\"deltaCents\":-3000,\"reason\":\"Bank drop\"}");
        // 10000 float + 1040 sale - 3000 drop = 8040 expected; 8000 counted.
        Map closed = t.owner(post(t.base + "/shifts/" + t.shiftId + "/close"),
                "{\"countedCents\":8000,\"note\":\"\"}");
        assertEquals("CLOSED", closed.get("status"));
        assertEquals(8040, Rest.number(closed, "expectedCents"));
        assertEquals(-40, Rest.number(closed, "varianceCents"));
        assertEquals(-3000, Rest.number(closed, "cashMovesCents"));
        assertEquals(1, Rest.list(mvc, People.asMerchant(get(t.base + "/shifts/" + t.shiftId
                + "/cash"), t.shop.owner)).size());
    }

    @Test
    void fiveWrongPinsLockThePin() throws Exception {
        Till t = new Till("pos-pin", 1);
        String cashier = People.customer(accounts, "pos-pin-cashier");
        t.owner(post("/api/merchant/stores/" + t.shop.storeId + "/staff"),
                "{\"username\":\"" + cashier + "\",\"role\":\"CASHIER\",\"pin\":\"1234\"}");
        String url = t.base + "/registers/" + t.registerId + "/sign-in";
        Map ok = Rest.json(mvc, People.asMerchant(Rest.body(post(url),
                "{\"username\":\"\",\"pin\":\"1234\"}"), cashier));
        assertEquals("CASHIER", ok.get("role"));
        assertEquals(t.shiftId, ((Map) ok.get("shift")).get("id"));
        for (int iter = 0; iter < 4; iter++) {
            assertEquals(401, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(url),
                    "{\"username\":\"\",\"pin\":\"9999\"}"), cashier)));
        }
        assertEquals(423, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(url),
                "{\"username\":\"\",\"pin\":\"9999\"}"), cashier)));
        assertEquals(423, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(url),
                "{\"username\":\"\",\"pin\":\"1234\"}"), cashier)));
        Map me = Rest.json(mvc, People.asCustomer(get("/api/me"), cashier));
        assertEquals(Boolean.TRUE, me.get("cashier"));
        assertEquals(cashier, me.get("email"));
        assertEquals(1, ((List) me.get("staffStores")).size());
    }

    @Test
    void anUnchangedCatalogSaysSo() throws Exception {
        Till t = new Till("pos-catalog", 4);
        String url = t.base + "/registers/" + t.registerId + "/catalog";
        Map full = t.owner(get(url), null);
        assertFalse((Boolean) full.get("unchanged"));
        List items = (List) full.get("items");
        assertEquals(1, items.size());
        assertEquals(4, Rest.number((Map) items.get(0), "available"));
        Map same = t.owner(get(url + "?version=" + full.get("version")), null);
        assertTrue((Boolean) same.get("unchanged"));
        assertTrue(((List) same.get("items")).isEmpty());
        t.sync(t.sale("cat", 1, cash(2000)));
        Map moved = t.owner(get(url + "?version=" + full.get("version")), null);
        assertFalse((Boolean) moved.get("unchanged"));
    }

    @Test
    void aCounterReturnRefundsCashAndRestocks() throws Exception {
        Till t = new Till("pos-return", 10);
        Map r = (Map) t.sync(t.sale("ret", 2, cash(3000))).get(0);
        assertEquals(8, t.available());
        String orderId = (String) r.get("orderId");
        Map order = t.owner(get("/api/merchant/stores/" + t.shop.storeId + "/orders/" + orderId),
                null);
        String lineId = (String) ((Map) ((List) order.get("lines")).get(0)).get("id");
        Map back = t.owner(post(t.base + "/registers/" + t.registerId + "/orders/" + orderId
                + "/return"), "{\"shiftId\":\"" + t.shiftId + "\",\"lines\":[{\"orderLineId\":\""
                + lineId + "\",\"quantity\":1}],\"refundTo\":\"CASH\",\"reason\":\"Chipped\","
                + "\"restock\":true}");
        assertEquals(9, t.available());
        assertEquals(1040, Rest.number(back, "refundCents"));
        assertEquals(1040, Rest.number(back, "cashCents"));
        // 10000 + 2080 sale - 1040 refund.
        assertEquals(11040, Rest.number(t.shift(), "expectedCents"));
    }

    private static String card(long cents) {
        return "{\"kind\":\"CARD\",\"amountCents\":" + cents + ",\"reference\":\"T1\"}";
    }

    private String eventKinds(Map result) {
        StringBuilder out = new StringBuilder();
        for (OrderEvent e : orderEvents.findByOrderIdOrderByCreatedAtAsc((String) result.get("orderId"))) {
            out.append(e.getKind()).append(' ');
        }
        return out.toString();
    }

    @Test
    void theToleranceIsAUnitOrOnePercent() {
        assertEquals(100, com.codenameone.examples.bazaar.pos.PosSales.tolerance(1040));
        assertEquals(100, com.codenameone.examples.bazaar.pos.PosSales.tolerance(0));
        assertEquals(250, com.codenameone.examples.bazaar.pos.PosSales.tolerance(25000));
    }

    @Test
    void aCardChargedAStaleTotalIsAcceptedAndNoted() throws Exception {
        Till t = new Till("pos-stale-tax", 10);
        // The register still had 5% tax: it charged $10.50 for a $10.40 sale.
        Map r = (Map) t.sync(t.sale("stale", 1, card(1050))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(1040, Rest.number(r, "totalCents"));
        assertTrue(eventKinds(r).contains("TOTAL_ADJUSTED"), eventKinds(r));
        assertTrue(String.valueOf(r.get("message")).contains("10 cents"));
        assertEquals(1050, Rest.number(t.shift(), "cardSalesCents"));
    }

    @Test
    void aCardFarPastTheTotalIsAcceptedAsAnOverTender() throws Exception {
        Till t = new Till("pos-over", 10);
        Map r = (Map) t.sync(t.sale("over", 1, card(5000))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(1040, Rest.number(r, "totalCents"));
        assertTrue(eventKinds(r).contains("OVER_TENDERED"), eventKinds(r));
    }

    @Test
    void aShortfallWithinRoundingIsAcceptedButAnUnpaidSaleIsNot() throws Exception {
        Till t = new Till("pos-short", 10);
        // The register had no tax: $10.00 for a $10.40 sale, 40 cents short.
        Map r = (Map) t.sync(t.sale("short", 1, card(1000))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertTrue(eventKinds(r).contains("TOTAL_ADJUSTED"), eventKinds(r));
        Map refused = (Map) t.sync(t.sale("unpaid", 2, card(1000))).get(0);
        assertEquals("ERROR", refused.get("status"));
        assertFalse(String.valueOf(refused.get("message")).isEmpty());
    }

    @Test
    void cashAfterAnOverpayingCardIsAllChange() throws Exception {
        Till t = new Till("pos-over-cash", 10);
        Map r = (Map) t.sync(t.sale("oc", 1, card(1100) + "," + cash(500))).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(500, Rest.number(r, "changeCents"));
    }
}
