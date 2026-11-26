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
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codename1.pdf.PdfReader;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.BulkOrderResultDto;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.RenewalDto;
import com.codenameone.examples.bazaar.api.SellingPlanDto;
import com.codenameone.examples.bazaar.api.SubscribeDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Schedules;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import com.codenameone.examples.bazaar.trade.VatIds;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Subscriptions and B2B against the database: renewals and dunning driven
/// through the job's tick, month arithmetic, price lists and quantity rules
/// in quotes, the quote pipeline, net terms with their invoice, CSV orders,
/// VAT formats, and who may reach what.
@BackendTest
class CommerceDepthTest {
    static final long DAY = 24L * 3600 * 1000;

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
    private Orders orders;
    @Autowired
    private Subscriptions subscriptions;
    @Autowired
    private Trade trade;
    @Autowired
    private Schedules schedules;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static int status(Exception e) {
        return e instanceof ResponseStatusException ? ((ResponseStatusException) e).getStatus() : -1;
    }

    private int statusOf(MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private String pdf(MockRequestBuilder request) throws Exception {
        MockResponse r = mvc.perform(request).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        byte[] bytes = r.getContentAsByteArray();
        assertEquals("%PDF-", new String(bytes, 0, 5, "ISO-8859-1"));
        return PdfReader.open(bytes).extractText();
    }

    private SubscriptionDto only(String user, String storeId) {
        List<SubscriptionDto> mine = subscriptions.mine(user, storeId);
        assertEquals(1, mine.size());
        return mine.get(0);
    }

    // -------------------------------------------------------- subscriptions

    @Test
    void aSubscriptionRenewsAndDunsUntilItPauses() throws Exception {
        Shop s = shop("cd-sub", 20);
        SellingPlanDto plan = subscriptions.writePlan(s.storeId, null, new SellingPlanDto(null,
                s.productId, "Weekly", "weeks", 1, 1000, true));
        assertEquals("WEEK", plan.unit());
        assertEquals(1, subscriptions.productPlans(s.storeId, s.productId).size());
        String user = People.customer(accounts, "cd-sub-member");
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        SubscriptionDto sub = subscriptions.subscribe(user, s.storeId, new SubscribeDto(plan.id(),
                s.variantId, 2, (String) card.get("id"), Shop.NY));
        assertEquals("ACTIVE", sub.status());
        OrderDto first = orders.detailOf(sub.lastOrderId());
        assertEquals("PAID", first.financial());
        assertEquals(1800, first.totalCents());
        long due = sub.dueAt();
        assertTrue(due > System.currentTimeMillis() + 6 * DAY);

        // The calendar shows this renewal and the ones after it.
        List<RenewalDto> cal = subscriptions.calendar(s.owner, s.storeId,
                Long.toString(System.currentTimeMillis()),
                Long.toString(System.currentTimeMillis() + 30 * DAY));
        assertTrue(cal.size() >= 4, cal.toString());
        assertEquals(due, cal.get(0).dueAt());
        assertEquals(1800, cal.get(0).amountCents());

        assertEquals(1, subscriptions.tick(Instant.ofEpochMilli(due + 1000)));
        SubscriptionDto renewed = only(user, s.storeId);
        assertEquals(due + 7 * DAY, renewed.dueAt());
        assertFalse(first.id().equals(renewed.lastOrderId()));
        assertEquals("PAID", orders.detailOf(renewed.lastOrderId()).financial());
        // A second pass at the same moment charges nothing more.
        assertEquals(0, subscriptions.tick(Instant.ofEpochMilli(due + 2000)));

        // The card now fails: retried after a day, then three, then paused.
        Map failing = Rest.addCard(mvc, user, Rest.FAILS_WHEN_CHARGED);
        db.execute("UPDATE subscription SET payment_method_id = ? WHERE id = ?",
                new Object[] {failing.get("id"), sub.id()});
        long next = renewed.dueAt();
        assertEquals(0, subscriptions.tick(Instant.ofEpochMilli(next + 1000)));
        assertEquals(1, only(user, s.storeId).failures());
        assertEquals(0, subscriptions.tick(Instant.ofEpochMilli(next + 2000)));
        assertEquals(1, only(user, s.storeId).failures());
        subscriptions.tick(Instant.ofEpochMilli(next + 1000 + DAY + 1000));
        assertEquals(2, only(user, s.storeId).failures());
        subscriptions.tick(Instant.ofEpochMilli(next + 1000 + 5 * DAY));
        SubscriptionDto paused = only(user, s.storeId);
        assertEquals(3, paused.failures());
        assertEquals("PAUSED", paused.status());
        assertEquals(renewed.lastOrderId(), paused.lastOrderId());

        // Resume with a good card, then skip and cancel.
        db.execute("UPDATE subscription SET payment_method_id = ? WHERE id = ?",
                new Object[] {card.get("id"), sub.id()});
        SubscriptionDto resumed = subscriptions.changeMine(user, s.storeId, sub.id(), "resume");
        assertEquals("ACTIVE", resumed.status());
        assertEquals(0, resumed.failures());
        SubscriptionDto skipped = subscriptions.change(s.owner, s.storeId, sub.id(), "skip");
        assertEquals(resumed.dueAt() + 7 * DAY, skipped.dueAt());
        assertEquals("CANCELLED", subscriptions.changeMine(user, s.storeId, sub.id(), "cancel")
                .status());
        try {
            subscriptions.changeMine(user, s.storeId, sub.id(), "cancel");
            fail("cancelled twice");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        String other = People.customer(accounts, "cd-sub-other");
        try {
            subscriptions.changeMine(other, s.storeId, sub.id(), "pause");
            fail("another shopper paused it");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test
    void monthlyRenewalsKeepTheirDayOfTheMonth() {
        long jan31 = Instant.parse("2026-01-31T10:00:00Z").toEpochMilli();
        assertEquals(Instant.parse("2026-02-28T10:00:00Z").toEpochMilli(),
                Subscriptions.advance(jan31, 1, Subscriptions.MONTH, 1));
        assertEquals(Instant.parse("2026-03-31T10:00:00Z").toEpochMilli(),
                Subscriptions.advance(jan31, 2, Subscriptions.MONTH, 1));
        assertEquals(Instant.parse("2028-02-29T10:00:00Z").toEpochMilli(),
                Subscriptions.advance(jan31, 1, Subscriptions.MONTH, 25));
        assertEquals(jan31 + 14 * DAY, Subscriptions.advance(jan31, 2, Subscriptions.WEEK, 1));
    }

    // ------------------------------------------------------------------ B2B

    @Test
    void aPriceListPricesTheQuoteAndItsRulesRejectQuantities() throws Exception {
        Shop s = shop("cd-price", 50);
        PriceListDto list = trade.writePriceList(s.storeId, null, new PriceListDto(null, "Gold",
                1000));
        trade.writeEntry(s.storeId, list.id(), new PriceListEntryDto(null, s.variantId, 700, 2, 10,
                2));
        CompanyDto c = trade.writeCompany(s.storeId, null, new CompanyDto(null, "Acme", "", 0,
                list.id()));
        String buyer = People.customer(accounts, "cd-price-buyer");
        trade.addBuyer(s.owner, s.storeId, c.id(), new CompanyBuyerDto(null, buyer, ""));
        TradeQuoteDto q = trade.requestQuote(buyer, s.storeId, CommerceSeed.request(s.variantId, 4));
        assertEquals("DRAFT", q.status());
        assertEquals(700, q.lines().get(0).unitCents());
        assertEquals(4000, q.subtotalCents());
        assertEquals(2800, q.totalCents());
        assertEquals(1200, q.discountCents());

        int[] refused = {3, 1, 12};
        for (int iter = 0; iter < refused.length; iter++) {
            try {
                trade.requestQuote(buyer, s.storeId, CommerceSeed.request(s.variantId,
                        refused[iter]));
                fail(refused[iter] + " was accepted");
            } catch (ResponseStatusException e) {
                assertEquals(400, e.getStatus(), e.getMessage());
                assertTrue(e.getMessage().contains("Line 1:"), e.getMessage());
            }
        }

        // Without an entry the list's percentage applies.
        PriceListDto plain = trade.writePriceList(s.storeId, null, new PriceListDto(null, "Silver",
                1500));
        CompanyDto c2 = trade.writeCompany(s.storeId, null, new CompanyDto(null, "Beta", "", 0,
                plain.id()));
        String buyer2 = People.customer(accounts, "cd-price-buyer2");
        trade.addBuyer(s.owner, s.storeId, c2.id(), new CompanyBuyerDto(null, buyer2, ""));
        TradeQuoteDto q2 = trade.requestQuote(buyer2, s.storeId, CommerceSeed.request(s.variantId,
                3));
        assertEquals(850, q2.lines().get(0).unitCents());
        assertEquals(2550, q2.totalCents());
    }

    @Test
    void aProductOutsideItsWindowIsNotOnTheTradeSheetNorQuoted() throws Exception {
        Shop s = shop("cd-sched", 50);
        PriceListDto list = trade.writePriceList(s.storeId, null, new PriceListDto(null, "Gold",
                1000));
        trade.writeEntry(s.storeId, list.id(), new PriceListEntryDto(null, s.variantId, 700, 0, 0,
                0));
        CompanyDto c = trade.writeCompany(s.storeId, null, new CompanyDto(null, "Acme", "", 0,
                list.id()));
        String buyer = People.customer(accounts, "cd-sched-buyer");
        trade.addBuyer(s.owner, s.storeId, c.id(), new CompanyBuyerDto(null, buyer, ""));
        assertEquals(1, trade.myTerms(buyer, s.storeId).prices.size());
        long now = System.currentTimeMillis();
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now + 3600000L, 0, now);
        assertTrue(trade.myTerms(buyer, s.storeId).prices.isEmpty());
        try {
            trade.requestQuote(buyer, s.storeId, CommerceSeed.request(s.variantId, 2));
            fail("a product before its launch was quoted");
        } catch (ResponseStatusException e) {
            assertTrue(e.getMessage().contains("not sold"), e.getMessage());
        }
    }

    @Test
    void aNetTermsOrderIsUnpaidUntilMarkedAndPrintsItsDueDate() throws Exception {
        Shop s = shop("cd-net", 20);
        CommerceSeed seed = new CommerceSeed(s, "cd-net", accounts, mvc, subscriptions, trade);
        TradeQuoteDto q = seed.ordered;
        assertEquals("ORDERED", q.status());
        assertTrue(q.orderId().length() > 0);
        OrderDto o = orders.detailOf(q.orderId());
        assertEquals("PENDING", o.financial());
        assertEquals(900, o.totalCents());
        List<NetTermDto> terms = trade.netTerms(s.owner, s.storeId);
        assertEquals(1, terms.size());
        NetTermDto t = terms.get(0);
        assertEquals("OPEN", t.status());
        assertEquals(30, t.termsDays());
        assertTrue(Math.abs(t.dueAt() - (System.currentTimeMillis() + 30 * DAY)) < DAY);
        assertEquals(1, trade.myNetTerms(seed.buyer, s.storeId).size());

        String due = Days.iso(t.dueAt());
        String text = pdf(People.asMerchant(get("/api/merchant/stores/" + s.storeId
                + "/net-terms/" + o.id() + "/invoice.pdf"), s.owner));
        assertTrue(text.contains("Net 30, due " + due), text);
        assertTrue(text.contains("#" + o.number()), text);
        String mine = pdf(People.asCustomer(get("/api/shop/stores/" + s.storeId
                + "/b2b/net-terms/" + o.id() + "/invoice.pdf"), seed.buyer));
        assertTrue(mine.contains("Net 30"), mine);

        NetTermDto paid = trade.markPaid(s.owner, s.storeId, o.id());
        assertEquals("PAID", paid.status());
        assertEquals("PAID", orders.detailOf(o.id()).financial());
        try {
            trade.markPaid(s.owner, s.storeId, o.id());
            fail("paid twice");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
    }

    @Test
    void theQuotePipelineRefusesIllegalMoves() throws Exception {
        Shop s = shop("cd-pipe", 20);
        CommerceSeed seed = new CommerceSeed(s, "cd-pipe", accounts, mvc, subscriptions, trade);
        TradeQuoteDto q = trade.requestQuote(seed.buyer, s.storeId,
                CommerceSeed.request(s.variantId, 2));
        String move = "/api/merchant/stores/" + s.storeId + "/quotes/" + q.id() + "/move";
        assertEquals(409, statusOf(People.asMerchant(Rest.body(post(move),
                "{\"status\":\"ORDERED\"}"), s.owner)));
        assertEquals(400, statusOf(People.asMerchant(Rest.body(post(move),
                "{\"status\":\"SHIPPED\"}"), s.owner)));
        try {
            trade.moveMyQuote(seed.buyer, s.storeId, q.id(), "ACCEPTED");
            fail("a draft was accepted");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        Map sent = Rest.json(mvc, People.asMerchant(Rest.body(post(move), "{\"status\":\"SENT\"}"),
                s.owner));
        assertEquals("SENT", sent.get("status"));
        try {
            trade.moveQuote(s.owner, s.storeId, q.id(), "ACCEPTED");
            fail("the merchant accepted for the buyer");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        assertEquals("DECLINED", trade.moveMyQuote(seed.buyer, s.storeId, q.id(), "declined")
                .status());
        assertEquals(409, statusOf(People.asMerchant(Rest.body(post(move),
                "{\"status\":\"SENT\"}"), s.owner)));
        List<TradeQuoteDto> board = trade.quotes(s.owner, s.storeId);
        assertEquals(2, board.size());
    }

    @Test
    void aCsvOrderReportsEveryBadRow() throws Exception {
        Shop s = shop("cd-csv", 50);
        CommerceSeed seed = new CommerceSeed(s, "cd-csv", accounts, mvc, subscriptions, trade);
        trade.writeEntry(s.storeId, seed.list.id(), new PriceListEntryDto(null, s.variantId, 0, 0,
                0, 6));
        String csv = "SKU,Quantity\r\nNOPE,1\r\n" + s.sku + ",abc\r\n" + s.sku + ",4\r\n" + s.sku
                + "\r\n\r\n\"" + s.sku + "\",12\r\n,3\r\n";
        BulkOrderResultDto r = trade.bulkOrder(seed.buyer, s.storeId, csv);
        assertNull(r.quote());
        assertEquals(1, r.accepted());
        assertEquals(5, r.errors().size(), r.errors().toString());
        assertEquals(2, r.errors().get(0).line());
        assertTrue(r.errors().get(0).message().startsWith("Unknown SKU"));
        assertEquals(3, r.errors().get(1).line());
        assertEquals(4, r.errors().get(2).line());
        assertTrue(r.errors().get(2).message().indexOf("multiples of 6") > 0,
                r.errors().get(2).message());
        assertEquals(5, r.errors().get(3).line());
        assertEquals(8, r.errors().get(4).line());

        BulkOrderResultDto ok = trade.bulkOrder(seed.buyer, s.storeId, s.sku + ",12\n" + s.sku
                + ",6");
        assertTrue(ok.errors().isEmpty(), ok.errors().toString());
        assertEquals("DRAFT", ok.quote().status());
        assertEquals(18 * 900, ok.quote().totalCents());
        assertEquals(1, trade.bulkOrder(seed.buyer, s.storeId, "").errors().size());
    }

    @Test
    void vatIdsAreCheckedForTheirEuFormat() throws Exception {
        String[] good = {"DE123456789", "de 123-456.789", "FR40303265045", "NL123456789B01",
            "ATU12345678", "ESX1234567X", "SE123456789001", "XIGD123", "EL123456789"};
        for (int iter = 0; iter < good.length; iter++) {
            assertTrue(VatIds.valid(good[iter]), good[iter]);
        }
        String[] bad = {"GR123456789", "DE12345678", "US123456789", "NL123456789X01", "", "DE",
            "ATX12345678"};
        for (int iter = 0; iter < bad.length; iter++) {
            assertFalse(VatIds.valid(bad[iter]), bad[iter]);
        }
        assertEquals("EL", VatIds.check("GR123456789").country().length() == 2 ? "EL" : "");
        Shop s = shop("cd-vat", 1);
        try {
            trade.writeCompany(s.storeId, null, new CompanyDto(null, "Bad", "DE12", 0, ""));
            fail("a bad VAT id was saved");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        Map check = Rest.json(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/"
                + s.storeId + "/vat-check"), "{\"vatId\":\"it 12345678901\"}"), s.owner));
        assertEquals(Boolean.TRUE, check.get("valid"));
        assertEquals("IT", check.get("country"));
    }

    @Test
    void storesAndCompaniesSeeOnlyTheirOwn() throws Exception {
        Shop a = shop("cd-iso-a", 10);
        Shop b = shop("cd-iso-b", 10);
        CommerceSeed seedA = new CommerceSeed(a, "cd-iso-a", accounts, mvc, subscriptions, trade);
        CommerceSeed seedB = new CommerceSeed(b, "cd-iso-b", accounts, mvc, subscriptions, trade);
        String bQuote = "/api/merchant/stores/" + b.storeId + "/quotes/" + seedB.ordered.id();
        assertEquals(404, statusOf(People.asMerchant(get(bQuote), a.owner)));
        assertEquals(404, statusOf(People.asMerchant(get("/api/merchant/stores/" + a.storeId
                + "/quotes/" + seedB.ordered.id()), a.owner)));
        assertEquals(404, statusOf(People.asMerchant(get("/api/merchant/stores/" + b.storeId
                + "/net-terms/" + seedB.ordered.orderId() + "/invoice.pdf"), a.owner)));
        assertEquals(404, statusOf(People.asMerchant(get("/api/merchant/stores/" + a.storeId
                + "/net-terms/" + seedB.ordered.orderId() + "/invoice.pdf"), a.owner)));
        assertEquals(200, statusOf(People.asMerchant(get(bQuote), b.owner)));

        // A second company in store A: its buyer cannot reach the first's.
        CompanyDto other = trade.writeCompany(a.storeId, null, new CompanyDto(null, "Other", "",
                15, ""));
        String rival = People.customer(accounts, "cd-iso-rival");
        trade.addBuyer(a.owner, a.storeId, other.id(), new CompanyBuyerDto(null, rival, ""));
        try {
            trade.moveMyQuote(rival, a.storeId, seedA.ordered.id(), "DECLINED");
            fail("a rival moved the quote");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
        assertEquals(404, statusOf(People.asCustomer(get("/api/shop/stores/" + a.storeId
                + "/b2b/net-terms/" + seedA.ordered.orderId() + "/invoice.pdf"), rival)));
        assertTrue(trade.myQuotes(rival, a.storeId).isEmpty());
        String stranger = People.customer(accounts, "cd-iso-stranger");
        assertEquals(403, statusOf(People.asCustomer(get("/api/shop/stores/" + a.storeId
                + "/b2b/quotes"), stranger)));
    }

    @Test
    void staffWithoutTheRoleAreRefused() throws Exception {
        Shop s = shop("cd-role", 10);
        CommerceSeed seed = new CommerceSeed(s, "cd-role", accounts, mvc, subscriptions, trade);
        String cashier = People.customer(accounts, "cd-role-cashier");
        Rest.json(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/" + s.storeId
                + "/staff"), "{\"username\":\"" + cashier + "\",\"role\":\"CASHIER\",\"pin\":\"4321\"}"),
                s.owner));
        String base = "/api/merchant/stores/" + s.storeId;
        assertEquals(200, statusOf(People.asMerchant(get(base + "/quotes"), cashier)));
        assertEquals(403, statusOf(People.asMerchant(Rest.body(post(base + "/quotes"),
                "{\"companyId\":\"" + seed.company.id() + "\",\"lines\":[]}"), cashier)));
        assertEquals(403, statusOf(People.asMerchant(Rest.body(post(base + "/net-terms/"
                + seed.ordered.orderId() + "/paid"), "{}"), cashier)));
        String subId = subscriptions.list(s.owner, s.storeId).get(0).id();
        assertEquals(403, statusOf(People.asMerchant(Rest.body(post(base + "/subscriptions/"
                + subId + "/pause"), "{}"), cashier)));
        assertEquals(403, statusOf(People.asMerchant(Rest.body(post(base + "/selling-plans"),
                "{}"), cashier)));
        try {
            trade.createQuote(cashier, s.storeId, CommerceSeed.request(s.variantId, 1));
            fail("a cashier wrote a quote");
        } catch (ResponseStatusException e) {
            assertEquals(403, status(e));
        }
    }
}
