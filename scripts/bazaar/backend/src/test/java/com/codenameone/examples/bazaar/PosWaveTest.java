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
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The point of sale's second round: tips, commissions, manager approvals,
/// custom items, parked carts, the smart grid, price labels, editing placed
/// orders and layaways.
@BackendTest
class PosWaveTest {
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
    private CustomerRepository customers;
    @Autowired
    private PaymentRepository paymentRows;

    /// A shop with a register taxed as New York (4%), a shift open on it, a
    /// cashier with PIN 1234 and a manager with PIN 4321.
    private final class Till {
        final Shop shop;
        final String store;
        final String base;
        final String registerId;
        final String shiftId;
        final String cashier;
        final String manager;

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
            manager = People.customer(accounts, name + "-manager");
            owner(post(store + "/staff"), "{\"username\":\"" + cashier
                    + "\",\"role\":\"CASHIER\",\"pin\":\"1234\"}");
            owner(post(store + "/staff"), "{\"username\":\"" + manager
                    + "\",\"role\":\"MANAGER\",\"pin\":\"4321\"}");
        }

        Map owner(MockRequestBuilder r, String json) throws Exception {
            return Rest.json(mvc, as(r, json, shop.owner));
        }

        MockRequestBuilder as(MockRequestBuilder r, String json, String who) {
            return People.asMerchant(json == null ? r : Rest.body(r, json), who);
        }

        int status(MockRequestBuilder r, String json, String who) throws Exception {
            return Rest.statusOf(mvc, as(r, json, who));
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

        int available() throws Exception {
            return inventory.available(shop.variantId, shop.locationId);
        }

        String customer() throws Exception {
            String id = Ids.next();
            db.execute("INSERT INTO customer (id, store_id, username, email, phone, first_name, "
                    + "last_name, tags, accepts_marketing, note, orders_count, spent_cents, "
                    + "store_credit_cents, created_at, updated_at) VALUES (?, ?, '', "
                    + "'grace@example.com', '', 'Grace', 'Hopper', '', 0, '', 0, 0, 0, 0, 0)",
                    new Object[] {id, shop.storeId});
            return id;
        }
    }

    private static String tender(String kind, long cents) {
        return "{\"kind\":\"" + kind + "\",\"amountCents\":" + cents + ",\"reference\":\"\"}";
    }

    @Test
    void aTipRidesOnTheCardAndStaysOutOfTheTotal() throws Exception {
        Till t = new Till("pw-tip", 10);
        Map r = t.sync(t.sale("tip-1", t.mug(1), tender("CARD", 1240), "\"tipCents\":200,"));
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(1040, ((Number) r.get("totalCents")).intValue());
        StoreOrder o = orderRows.findById((String) r.get("orderId"));
        assertEquals(200, o.getTipCents());
        assertEquals(40, o.getTaxCents());
        Map cashTip = t.sync(t.sale("tip-2", t.mug(1), tender("CASH", 2000),
                "\"tipCents\":200,"));
        assertEquals("ERROR", cashTip.get("status"));
    }

    @Test
    void commissionsFollowTheSellerNetOfTaxAndTips() throws Exception {
        Till t = new Till("pw-comm", 10);
        t.owner(post(t.base + "/staff-rates"), "{\"username\":\"" + t.cashier
                + "\",\"displayName\":\"\",\"role\":\"\",\"rateBp\":500}");
        Map r = t.sync(t.sale("comm-1", t.mug(2), tender("CARD", 2380),
                "\"staffUsername\":\"" + t.cashier + "\",\"tipCents\":300,"));
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(t.cashier, orderRows.findById((String) r.get("orderId")).getStaffUsername());
        List rows = Rest.list(mvc, t.as(get(t.base + "/commissions"), null, t.shop.owner));
        Map mine = null;
        for (Object o : rows) {
            if (t.cashier.equals(((Map) o).get("username"))) {
                mine = (Map) o;
            }
        }
        assertEquals(2000, ((Number) mine.get("netCents")).intValue());
        assertEquals(300, ((Number) mine.get("tipCents")).intValue());
        assertEquals(100, ((Number) mine.get("commissionCents")).intValue());
        assertEquals(404, t.status(get(t.base + "/commissions"), null,
                People.customer(accounts, "pw-comm-stranger")));
        assertNotEquals(200, t.status(get(t.base + "/commissions"), null, t.cashier));
    }

    @Test
    void aPriceOverrideNeedsAManagersPinOnce() throws Exception {
        Till t = new Till("pw-appr", 10);
        t.owner(post(t.base + "/policy"), "{\"discountApprovalBp\":0,"
                + "\"approvePriceOverrides\":true,\"approveRefunds\":true,"
                + "\"approveOrderEdits\":false,\"tip1Bp\":1500,\"tip2Bp\":1800,\"tip3Bp\":2000,"
                + "\"layawayDepositBp\":2000,\"layawayCancelFeeBp\":0}");
        String cheap = "{\"variantId\":\"" + t.shop.variantId
                + "\",\"quantity\":1,\"unitCents\":500,\"overridden\":true}";
        String seller = "\"staffUsername\":\"" + t.cashier + "\",";
        Map refused = t.sync(t.sale("ov-1", cheap, tender("CASH", 1000), seller));
        assertEquals("APPROVAL", refused.get("status"));
        String ask = "{\"managerUsername\":\"" + t.manager + "\",\"pin\":\"%s\","
                + "\"kind\":\"PRICE_OVERRIDE\",\"amountCents\":500,\"reason\":\"dent\"}";
        assertEquals(401, t.status(post(t.reg() + "/approvals"),
                String.format(ask, "0000"), t.cashier));
        String notManager = "{\"managerUsername\":\"" + t.cashier + "\",\"pin\":\"1234\","
                + "\"kind\":\"PRICE_OVERRIDE\",\"amountCents\":500,\"reason\":\"\"}";
        assertEquals(403, t.status(post(t.reg() + "/approvals"), notManager, t.cashier));
        Map ok = Rest.json(mvc, t.as(post(t.reg() + "/approvals"), String.format(ask, "4321"),
                t.cashier));
        String approval = "\"approvalId\":\"" + ok.get("id") + "\",";
        Map sold = t.sync(t.sale("ov-2", cheap, tender("CASH", 1000), seller + approval));
        assertEquals("OK", sold.get("status"), String.valueOf(sold.get("message")));
        assertEquals(520, ((Number) sold.get("totalCents")).intValue());
        Map again = t.sync(t.sale("ov-3", cheap, tender("CASH", 1000), seller + approval));
        assertEquals("APPROVAL", again.get("status"));
        Map byManager = t.sync(t.sale("ov-4", cheap, tender("CASH", 1000),
                "\"staffUsername\":\"" + t.manager + "\","));
        assertEquals("OK", byManager.get("status"));
    }

    /// A percent or an amount off the whole sale reaches the server only as
    /// lower unit prices on lines the register did not flag; they count
    /// towards the discount threshold all the same.
    @Test
    void aSaleWideDiscountCountsTowardsTheApprovalThreshold() throws Exception {
        Till t = new Till("pw-wide", 10);
        t.owner(post(t.base + "/policy"), "{\"discountApprovalBp\":1000,"
                + "\"approvePriceOverrides\":false,\"approveRefunds\":false,"
                + "\"approveOrderEdits\":false,\"tip1Bp\":1500,\"tip2Bp\":1800,\"tip3Bp\":2000,"
                + "\"layawayDepositBp\":2000,\"layawayCancelFeeBp\":0}");
        String seller = "\"staffUsername\":\"" + t.cashier + "\",";
        String fifthOff = "{\"variantId\":\"" + t.shop.variantId
                + "\",\"quantity\":1,\"unitCents\":800,\"overridden\":false}";
        Map refused = t.sync(t.sale("wd-1", fifthOff, tender("CASH", 1000), seller));
        assertEquals("APPROVAL", refused.get("status"), String.valueOf(refused.get("message")));
        String littleOff = "{\"variantId\":\"" + t.shop.variantId
                + "\",\"quantity\":1,\"unitCents\":950,\"overridden\":false}";
        Map under = t.sync(t.sale("wd-2", littleOff, tender("CASH", 1000), seller));
        assertEquals("OK", under.get("status"), String.valueOf(under.get("message")));
        String ask = "{\"managerUsername\":\"" + t.manager + "\",\"pin\":\"4321\","
                + "\"kind\":\"DISCOUNT\",\"amountCents\":200,\"reason\":\"loyal\"}";
        Map ok = Rest.json(mvc, t.as(post(t.reg() + "/approvals"), ask, t.cashier));
        Map sold = t.sync(t.sale("wd-1", fifthOff, tender("CASH", 1000),
                seller + "\"approvalId\":\"" + ok.get("id") + "\","));
        assertEquals("OK", sold.get("status"), String.valueOf(sold.get("message")));
    }

    @Test
    void aCustomItemHasNoStock() throws Exception {
        Till t = new Till("pw-custom", 10);
        String wrap = "{\"variantId\":\"\",\"quantity\":2,\"unitCents\":300,"
                + "\"title\":\"Gift wrap\",\"taxable\":false}";
        Map r = t.sync(t.sale("cu-1", t.mug(1) + "," + wrap, tender("CASH", 2000), ""));
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        assertEquals(1640, ((Number) r.get("totalCents")).intValue());
        assertEquals(9, t.available());
        String untitled = "{\"variantId\":\"\",\"quantity\":1,\"unitCents\":300}";
        assertEquals("ERROR", t.sync(t.sale("cu-2", untitled, tender("CASH", 500), ""))
                .get("status"));
        Map order = t.owner(get(t.store + "/orders/" + r.get("orderId")), null);
        String wrapLine = null;
        for (Object l : (List) order.get("lines")) {
            if ("Gift wrap".equals(((Map) l).get("title"))) {
                wrapLine = (String) ((Map) l).get("id");
            }
        }
        Map back = t.owner(post(t.reg() + "/orders/" + r.get("orderId") + "/return"),
                "{\"shiftId\":\"" + t.shiftId + "\",\"lines\":[{\"orderLineId\":\"" + wrapLine
                + "\",\"quantity\":1}],\"refundTo\":\"CASH\",\"reason\":\"\",\"restock\":true}");
        assertEquals(300, ((Number) back.get("refundCents")).intValue());
        assertEquals(9, t.available());
    }

    @Test
    void aParkedCartSentTwiceIsKeptOnceAndNeverComesBack() throws Exception {
        Till t = new Till("pw-park", 10);
        String park = "{\"id\":\"\",\"clientId\":\"hold-1\",\"label\":\"Blue coat\","
                + "\"customerId\":\"\",\"staffUsername\":\"\",\"note\":\"\",\"lines\":["
                + t.mug(2) + "],\"status\":\"\",\"createdAt\":0}";
        Map first = t.owner(post(t.reg() + "/parked"), park);
        Map second = t.owner(post(t.reg() + "/parked"), park);
        assertEquals(first.get("id"), second.get("id"));
        assertEquals(1, Rest.list(mvc, t.as(get(t.reg() + "/parked"), null, t.cashier)).size());
        String resume = t.reg() + "/parked/" + first.get("id") + "/resume";
        Map resumed = t.owner(post(resume), null);
        assertEquals("RESUMED", resumed.get("status"));
        assertEquals(2, ((Number) ((Map) ((List) resumed.get("lines")).get(0))
                .get("quantity")).intValue());
        assertEquals("RESUMED", t.owner(post(resume), null).get("status"));
        assertEquals(409, t.status(post(t.reg() + "/parked/" + first.get("id") + "/discard"),
                null, t.shop.owner));
        // The hold replayed after it was resumed answers what became of it.
        assertEquals("RESUMED", t.owner(post(t.reg() + "/parked"), park).get("status"));
        assertEquals(0, Rest.list(mvc, t.as(get(t.reg() + "/parked"), null, t.cashier)).size());
        assertEquals(10, t.available());
    }

    @Test
    void theGridIsTheOwnersAndEveryoneReadsIt() throws Exception {
        Till t = new Till("pw-grid", 10);
        String url = t.base + "/locations/" + t.shop.locationId + "/grid";
        String grid = "{\"locationId\":\"" + t.shop.locationId + "\",\"tiles\":["
                + "{\"position\":0,\"kind\":\"PRODUCT\",\"ref\":\"" + t.shop.variantId
                + "\",\"label\":\"Mug\",\"color\":255},"
                + "{\"position\":1,\"kind\":\"ACTION\",\"ref\":\"PARK\",\"label\":\"Hold\","
                + "\"color\":0}]}";
        t.owner(post(url), grid);
        Map read = Rest.json(mvc, t.as(get(url), null, t.cashier));
        assertEquals(2, ((List) read.get("tiles")).size());
        assertNotEquals(200, t.status(post(url), grid, t.cashier));
        assertEquals(400, t.status(post(url), grid.replace("PARK", "LAUNCH"), t.shop.owner));
    }

    @Test
    void priceLabelsAreAPdf() throws Exception {
        Till t = new Till("pw-label", 10);
        String url = t.base + "/labels.pdf?variants=" + t.shop.variantId + "&copies=3";
        assertEquals(200, t.status(get(url), null, t.cashier));
        assertEquals(400, t.status(get(t.base + "/labels.pdf?variants=&copies=1"), null,
                t.cashier));
        assertEquals(404, t.status(get(url), null, People.customer(accounts, "pw-label-x")));
    }

    @Test
    void editingAnOrderMovesStockAndMoney() throws Exception {
        Till t = new Till("pw-edit", 10);
        String user = People.customer(accounts, "pw-edit-buyer");
        OrderDto o = t.shop.buy(user, 3);
        assertEquals(7, t.available());
        String line = o.lines().get(0).id();
        String url = t.store + "/orders/" + o.id() + "/edit";
        // Fewer: the hold is taken, the difference goes back to the card.
        Map less = t.owner(post(url), "{\"change\":[{\"lineId\":\"" + line
                + "\",\"quantity\":1}],\"discountCents\":-1,\"note\":\"two fewer\"}");
        assertEquals("ORIGINAL", less.get("settlement"));
        long down = -((Number) less.get("deltaCents")).longValue();
        assertTrue(down >= 2000, "two mugs cost " + down);
        assertEquals(9, t.available());
        assertEquals(down, paymentRows.findById(o.id()).getRefundedCents());
        assertEquals(409, t.status(post(url), "{\"change\":[{\"lineId\":\"" + line
                + "\",\"quantity\":50}],\"discountCents\":-1,\"collect\":"
                + tender("CARD", 0) + "}", t.shop.owner));
        assertEquals(9, t.available());
        assertEquals(400, t.status(post(url), "{\"change\":[{\"lineId\":\"" + line
                + "\",\"quantity\":2}],\"discountCents\":-1}", t.shop.owner));
        // More: collected at the terminal, one more mug reserved.
        Map more = t.owner(post(url), "{\"change\":[{\"lineId\":\"" + line
                + "\",\"quantity\":2}],\"discountCents\":-1,\"collect\":"
                + tender("CARD", 0) + "}");
        assertEquals("CARD", more.get("settlement"));
        assertTrue(((Number) more.get("deltaCents")).longValue() >= 1000);
        assertEquals(8, t.available());
        List history = Rest.list(mvc, t.as(get(t.store + "/orders/" + o.id() + "/edits"), null,
                t.shop.owner));
        assertEquals(2, history.size());
        assertEquals(400, t.status(post(url), "{\"discountCents\":-1}", t.shop.owner));
    }

    @Test
    void anOrderEditCanNeedAnApproval() throws Exception {
        Till t = new Till("pw-edit-ok", 10);
        t.owner(post(t.base + "/policy"), "{\"discountApprovalBp\":0,"
                + "\"approvePriceOverrides\":false,\"approveRefunds\":false,"
                + "\"approveOrderEdits\":true,\"tip1Bp\":0,\"tip2Bp\":0,\"tip3Bp\":0,"
                + "\"layawayDepositBp\":2000,\"layawayCancelFeeBp\":0}");
        OrderDto o = t.shop.buy(People.customer(accounts, "pw-edit-ok-buyer"), 2);
        String body = "{\"change\":[{\"lineId\":\"" + o.lines().get(0).id()
                + "\",\"quantity\":1}],\"discountCents\":-1%s}";
        String url = t.store + "/orders/" + o.id() + "/edit";
        assertEquals(403, t.status(post(url), String.format(body, ""), t.cashier));
        assertEquals(8, t.available());
        Map ok = Rest.json(mvc, t.as(post(t.reg() + "/approvals"), "{\"managerUsername\":\""
                + t.manager + "\",\"pin\":\"4321\",\"kind\":\"ORDER_EDIT\",\"amountCents\":0,"
                + "\"reason\":\"\"}", t.cashier));
        assertEquals(200, t.status(post(url),
                String.format(body, ",\"approvalId\":\"" + ok.get("id") + "\""), t.cashier));
        assertEquals(9, t.available());
    }

    @Test
    void aLayawayHoldsStockUntilPaid() throws Exception {
        Till t = new Till("pw-lay", 10);
        String customer = t.customer();
        String start = "{\"clientId\":\"lay-1\",\"shiftId\":\"" + t.shiftId
                + "\",\"customerId\":\"" + customer + "\",\"lines\":[" + t.mug(2)
                + "],\"deposit\":%s,\"cancelFeeBp\":-1}";
        String url = t.reg() + "/layaways";
        assertEquals(400, t.status(post(url), String.format(start, tender("CASH", 100)),
                t.cashier));
        Map l = Rest.json(mvc, t.as(post(url), String.format(start, tender("CASH", 500)),
                t.cashier));
        Map replay = Rest.json(mvc, t.as(post(url), String.format(start, tender("CASH", 500)),
                t.cashier));
        assertEquals(l.get("id"), replay.get("id"));
        assertEquals(2080, ((Number) l.get("totalCents")).intValue());
        assertEquals(1580, ((Number) l.get("balanceCents")).intValue());
        assertEquals(8, t.available());
        String pay = t.store + "/layaways/" + l.get("id") + "/payments";
        t.owner(post(pay), tender("CARD", 1000));
        assertEquals(400, t.status(post(pay), tender("CASH", 5000), t.shop.owner));
        Map done = t.owner(post(pay), tender("CASH", 580));
        assertEquals("COMPLETED", done.get("status"));
        assertEquals(3, ((List) done.get("payments")).size());
        assertEquals(8, t.available());
        StoreOrder order = orderRows.findById((String) l.get("orderId"));
        assertEquals("PAID", order.getFinancial());
        assertEquals("FULFILLED", order.getFulfilment());
        assertEquals(1, Rest.list(mvc, t.as(get(t.store + "/layaways"), null, t.cashier))
                .size());
    }

    @Test
    void cancellingALayawayReleasesStockAndKeepsTheFee() throws Exception {
        Till t = new Till("pw-lay-x", 10);
        String customer = t.customer();
        Map l = t.owner(post(t.reg() + "/layaways"), "{\"clientId\":\"lay-x\",\"shiftId\":\"\","
                + "\"customerId\":\"" + customer + "\",\"lines\":[" + t.mug(1)
                + "],\"deposit\":" + tender("CASH", 500) + ",\"cancelFeeBp\":1000}");
        assertEquals(9, t.available());
        String cancel = t.store + "/layaways/" + l.get("id") + "/cancel";
        Map gone = t.owner(post(cancel), "{\"shiftId\":\"\",\"refundTo\":\"STORE_CREDIT\","
                + "\"reason\":\"changed mind\"}");
        assertEquals("CANCELLED", gone.get("status"));
        assertEquals(396, ((Number) gone.get("refundedCents")).intValue());
        assertEquals(396, customers.findById(customer).getStoreCreditCents());
        assertEquals(10, t.available());
        assertEquals(409, t.status(post(cancel), "{\"refundTo\":\"CASH\"}", t.shop.owner));
        assertEquals(409, t.status(post(t.store + "/layaways/" + l.get("id") + "/payments"),
                tender("CASH", 100), t.shop.owner));
    }
}
