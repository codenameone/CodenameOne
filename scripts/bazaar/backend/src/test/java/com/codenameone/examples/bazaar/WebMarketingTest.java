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
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.SaleEventDto;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.sale.SaleEvents;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The back office's fulfilment and marketing pages: each page's main action
/// through the same services the app uses, another store answering 404, a
/// post without its CSRF token refused, a shopper refused, and a fulfilment
/// clerk kept to the board.
@BackendTest
class WebMarketingTest {
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
    private Fulfillments fulfillments;
    @Autowired
    private SaleEvents sales;
    @Autowired
    private RateSettings rates;
    @Autowired
    private Loyalty loyalty;
    @Autowired
    private Clubs clubs;
    @Autowired
    private Audiences audiences;
    @Autowired
    private Campaigns campaigns;
    @Autowired
    private Journeys journeys;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private Browser signedIn(String user) throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        return b;
    }

    private static void redirected(MockResponse r) throws Exception {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303,
                "answered " + r.getStatus() + " " + r.getContentAsString());
    }

    private static String day(int fromToday) {
        return Days.iso(System.currentTimeMillis() + fromToday * Days.MILLIS);
    }

    private static String graph() {
        return "{\"nodes\":[{\"id\":\"e\",\"data\":{\"type\":\"" + JourneyCatalog.ENTRY_TRIGGER
                + "\",\"event\":\"order.paid\"}},{\"id\":\"t\",\"data\":{\"type\":\""
                + JourneyCatalog.TAG_ADD + "\",\"tag\":\"walked\"}},{\"id\":\"x\",\"data\":"
                + "{\"type\":\"" + JourneyCatalog.EXIT + "\"}}],\"edges\":[{\"from\":\"e\","
                + "\"fromPort\":\"out\",\"to\":\"t\"},{\"from\":\"t\",\"fromPort\":\"out\","
                + "\"to\":\"x\"}]}";
    }

    @Test
    void theBoardMovesACardAndTheOrderPagePacksLabelsAndShips() throws Exception {
        Shop s = shop("wmk-fulfil");
        String buyer = People.customer(accounts, "wmk-fulfil-buyer");
        OrderDto boarded = s.buy(buyer, 1);
        OrderDto parcel = s.buy(buyer, 1);
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;

        MockResponse board = b.get(base + "/fulfilment");
        b.assertPage(board, "id=\"board\"");
        b.assertPage(board, "#" + boarded.number());
        b.assertPage(board, "hx-select=\"#board\"");
        redirected(b.post(base + "/fulfilment/move", "order", boarded.id(), "to",
                Fulfillments.COLUMN_PACKED));
        assertEquals(1, fulfillments.ofOrder(s.owner, s.storeId, boarded.id()).size());
        // Two columns at once is the service's 409, shown as a message.
        redirected(b.post(base + "/fulfilment/move", "order", parcel.id(), "to",
                Fulfillments.COLUMN_DELIVERED));
        assertTrue(fulfillments.ofOrder(s.owner, s.storeId, parcel.id()).isEmpty());

        String detail = base + "/orders/" + parcel.id();
        String line = orders.detailOf(parcel.id()).lines().get(0).id();
        redirected(b.post(detail + "/fulfil", "location", s.locationId, "qty_" + line, "1"));
        FulfillmentDto f = fulfillments.ofOrder(s.owner, s.storeId, parcel.id()).get(0);
        MockResponse page = b.get(detail);
        b.assertPage(page, "Parcels");
        b.assertPage(page, "name=\"service\"");
        redirected(b.post(detail + "/parcels/" + f.id() + "/pack", "weight", "450"));
        assertEquals(Fulfillments.PACKED, fulfillments.ofOrder(s.owner, s.storeId, parcel.id())
                .get(0).status());
        List<RateQuoteDto> quotes = fulfillments.rates(s.owner, s.storeId, f.id());
        redirected(b.post(detail + "/parcels/" + f.id() + "/label", "service",
                quotes.get(0).service()));
        b.assertPage(b.get(detail), "Label (PDF)");
        redirected(b.post(detail + "/parcels/" + f.id() + "/ship"));
        assertEquals(Fulfillments.SHIPPED, fulfillments.ofOrder(s.owner, s.storeId, parcel.id())
                .get(0).status());
        MockResponse slip = b.get(detail + "/packing-slip.pdf");
        assertEquals(200, slip.getStatus());
        assertTrue(slip.getContentType().startsWith("application/pdf"), slip.getContentType());
        assertEquals(200, b.get(detail + "/parcels/" + f.id() + "/label.pdf").getStatus());
    }

    @Test
    void salesAreScheduledShownOnTheCalendarAndMoved() throws Exception {
        Shop s = shop("wmk-sales");
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        MockResponse list = b.get(base + "/sales");
        b.assertPage(list, "Schedule a sale");
        b.assertPage(list, "var_" + s.variantId);

        redirected(b.post(base + "/sales", "title", "Spring sale", "kind", SaleEvents.SALE,
                "mode", SaleEvents.PERCENT, "amount", "20", "starts", day(2), "ends", day(5),
                "stockCap", "0", "var_" + s.variantId, "1"));
        List<SaleEventDto> made = sales.list(s.owner, s.storeId);
        assertEquals(1, made.size());
        SaleEventDto sale = made.get(0);
        assertEquals(2000, sale.amount());
        assertEquals(1, sale.items().size());
        b.assertPage(b.get(base + "/sales/" + sale.id()), "Reschedule");

        MockResponse cal = b.get(base + "/calendar?month=" + day(2).substring(0, 7));
        b.assertPage(cal, "Spring sale");
        b.assertPage(cal, "promo sale");
        redirected(b.post(base + "/calendar/move", "kind", SaleEvents.SALE, "id", sale.id(),
                "starts", day(3), "ends", day(9), "month", day(3).substring(0, 7)));
        SaleEventDto moved = sales.get(s.owner, s.storeId, sale.id());
        assertEquals(day(3), Days.iso(moved.startsAt()));
        redirected(b.post(base + "/sales/" + sale.id() + "/reschedule", "starts", day(4), "ends",
                day(9)));
        assertEquals(day(4), Days.iso(sales.get(s.owner, s.storeId, sale.id()).startsAt()));
    }

    @Test
    void discountsAreSavedWithTheirRules() throws Exception {
        Shop s = shop("wmk-disc");
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base + "/new-discount"), "Save discount");
        redirected(b.post(base + "/discounts", "code", "WMK10", "title", "Ten off", "kind",
                "PERCENT", "amount", "10", "active", "1"));
        DiscountDto made = null;
        List<DiscountDto> all = rates.discounts(s.owner, s.storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if ("WMK10".equals(all.get(iter).code())) {
                made = all.get(iter);
            }
        }
        assertTrue(made != null, "no WMK10 among " + all.size());
        assertEquals(1000, made.amount());
        assertEquals(0, made.startsAt());
        String id = made.id();
        b.assertPage(b.get(base + "/discounts"), "WMK10");
        b.assertPage(b.get(base + "/discounts/" + id), "Stacking and rules");
        redirected(b.post(base + "/discounts/" + id + "/rules", "discountClass", "ORDER",
                "combinesShipping", "1", "eligibility", "", "rules", ""));
        assertTrue(sales.rules(s.owner, s.storeId, id).combinesShipping());
    }

    @Test
    void loyaltyAndClubsAreEdited() throws Exception {
        Shop s = shop("wmk-loyal");
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base + "/loyalty"), "Save programme");
        redirected(b.post(base + "/loyalty", "active", "1", "pointsPerUnit", "2", "unitCents",
                "1.00", "rounding", "DOWN", "pointsPerReward", "100", "rewardCents", "5.00",
                "minRedeemPoints", "100", "expiryDays", "0", "tierBasis", Loyalty.SPEND,
                "referrerPoints", "0", "refereePoints", "0", "tiers", "1", "t0_id", "",
                "t0_name", "Gold", "t0_threshold", "10000", "t0_bps", "5", "t0_early", "0"));
        assertEquals(2, loyalty.program(s.owner, s.storeId).pointsPerUnit());
        b.assertPage(b.get(base + "/loyalty"), "Gold");

        redirected(b.post(base + "/clubs", "name", "Insiders", "description", "Early access",
                "fee", "0", "periodDays", "30", "bps", "10", "early", "24", "active", "1"));
        List<ClubDto> all = clubs.list(s.owner, s.storeId);
        assertEquals(1, all.size());
        assertEquals(1000, all.get(0).memberBps());
        b.assertPage(b.get(base + "/clubs/" + all.get(0).id()), "Insiders");
    }

    @Test
    void segmentsAreBuiltRowByRowWithALiveCount() throws Exception {
        Shop s = shop("wmk-seg");
        String buyer = People.customer(accounts, "wmk-seg-buyer");
        s.buy(buyer, 1);
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        MockResponse fresh = b.get(base + "/new-segment");
        b.assertPage(fresh, "Add condition");
        b.assertPage(fresh, "match");
        MockResponse added = b.post(base + "/new-segment", "rows", "1", "name", "Buyers", "op",
                "AND", "f0", "ordersCount", "o0", ">=", "v0", "1", "add", "1");
        b.assertPage(added, "name=\"f1\"");
        b.assertPage(added, "1 customer matches");
        redirected(b.post(base + "/segments", "rows", "1", "name", "Buyers", "op", "AND", "f0",
                "ordersCount", "o0", ">=", "v0", "1"));
        List<AudienceDto> all = audiences.list(s.owner, s.storeId);
        assertEquals(1, all.size());
        assertTrue(all.get(0).rules().contains("\"value\":1"), all.get(0).rules());
        b.assertPage(b.get(base + "/segments"), "Buyers");
        b.assertPage(b.get(base + "/segments/" + all.get(0).id()), "1 customer matches");
    }

    @Test
    void campaignsAreWrittenTestedAndScheduled() throws Exception {
        Shop s = shop("wmk-camp");
        AudienceDto seg = audiences.save(s.owner, s.storeId, null, new AudienceDto(null, "All",
                "{\"op\":\"AND\",\"rules\":[]}", 0));
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base + "/new-campaign"), "Save campaign");
        redirected(b.post(base + "/campaigns", "name", "Launch", "channel", Campaigns.EMAIL,
                "audienceId", seg.id(), "subjectA", "Hello", "subjectB", "", "abPercent", "0",
                "winnerAfterMinutes", "60", "body", "New things", "links",
                "https://example.com/a\nhttps://example.com/b"));
        List<CampaignDto> all = campaigns.list(s.owner, s.storeId);
        assertEquals(1, all.size());
        assertEquals(2, all.get(0).links().size());
        String c = base + "/campaigns/" + all.get(0).id();
        b.assertPage(b.get(c), "Send a test");
        redirected(b.post(c + "/test", "to", "tester@example.com"));
        // The REST twin of the form answers the campaign, unmoved.
        assertEquals(Campaigns.DRAFT, campaigns.sendTest(s.owner, s.storeId, all.get(0).id(),
                "tester@example.com").status());
        redirected(b.post(c + "/schedule", "day", day(3)));
        assertEquals(Campaigns.SCHEDULED, campaigns.list(s.owner, s.storeId).get(0).status());
        b.assertPage(b.get(base + "/campaigns"), "Launch");
    }

    @Test
    void aJourneyIsDrawnAsAFlowchart() throws Exception {
        Shop s = shop("wmk-journey");
        JourneyDto j = journeys.write(s.storeId, null, new JourneyDto(null, "Thanks", graph(),
                Journeys.ACTIVE, 0, null));
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base + "/journeys"), "Thanks");
        MockResponse page = b.get(base + "/journeys/" + j.id());
        b.assertPage(page, "<svg");
        b.assertPage(page, "class=\"edge\"");
        b.assertPage(page, "0 passed, 0 here now");
    }

    @Test
    void anotherStoresMarketingAnswers404() throws Exception {
        Shop mine = shop("wmk-idor-a");
        Shop theirs = shop("wmk-idor-b");
        ClubDto club = clubs.save(theirs.owner, theirs.storeId, null, new ClubDto(null, "Theirs",
                "", 0, 30, 0, 0, "", true, 0));
        AudienceDto seg = audiences.save(theirs.owner, theirs.storeId, null, new AudienceDto(null,
                "Theirs", "{\"op\":\"AND\",\"rules\":[]}", 0));
        Browser b = signedIn(mine.owner);
        String a = "/admin/" + mine.storeId;
        String other = "/admin/" + theirs.storeId;
        String[] gets = {other + "/fulfilment", other + "/sales", other + "/calendar",
            other + "/discounts", other + "/loyalty", other + "/clubs", other + "/segments",
            other + "/campaigns", other + "/journeys", a + "/clubs/" + club.id(),
            a + "/segments/" + seg.id()};
        for (int iter = 0; iter < gets.length; iter++) {
            assertEquals(404, b.get(gets[iter]).getStatus(), gets[iter]);
        }
        b.get(a + "/sales");
        String[] posts = {other + "/sales", other + "/loyalty", other + "/clubs",
            a + "/clubs/" + club.id(), a + "/segments/" + seg.id() + "/delete",
            other + "/fulfilment/move"};
        for (int iter = 0; iter < posts.length; iter++) {
            assertEquals(404, b.post(posts[iter], "name", "Mine now").getStatus(), posts[iter]);
        }
        assertEquals("Theirs", clubs.list(theirs.owner, theirs.storeId).get(0).name());
        assertEquals(1, audiences.list(theirs.owner, theirs.storeId).size());
    }

    @Test
    void postsWithoutTheTokenAndShoppersAreRefused() throws Exception {
        Shop s = shop("wmk-csrf");
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId;
        assertEquals(403, b.postWithoutToken(base + "/clubs", "name", "Sneaky").getStatus());
        assertEquals(403, b.postWithoutToken(base + "/loyalty", "active", "1").getStatus());
        assertTrue(clubs.list(s.owner, s.storeId).isEmpty());

        String shopper = People.customer(accounts, "wmk-csrf-shopper");
        Browser c = signedIn(shopper);
        assertEquals(403, c.get(base + "/campaigns").getStatus());
        assertEquals(403, c.get(base + "/fulfilment").getStatus());
        c.get("/account");
        assertEquals(403, c.post(base + "/clubs", "name", "Mine").getStatus());
        assertTrue(clubs.list(s.owner, s.storeId).isEmpty());
    }

    @Test
    void aFulfilmentClerkMovesTheBoardButNotTheMarketing() throws Exception {
        Shop s = shop("wmk-staff");
        String buyer = People.customer(accounts, "wmk-staff-buyer");
        OrderDto o = s.buy(buyer, 1);
        String packer = People.customer(accounts, "wmk-staff-packer");
        stores.saveStaff(s.owner, s.storeId, new StaffDto(null, packer, null, "FULFILMENT", null,
                null, false));
        Browser b = signedIn(packer);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base + "/fulfilment"), "#" + o.number());
        redirected(b.post(base + "/fulfilment/move", "order", o.id(), "to",
                Fulfillments.COLUMN_PACKED));
        assertFalse(fulfillments.ofOrder(s.owner, s.storeId, o.id()).isEmpty());

        assertEquals(403, b.get(base + "/segments").getStatus());
        assertEquals(403, b.get(base + "/campaigns").getStatus());
        assertEquals(403, b.post(base + "/clubs", "name", "Packers").getStatus());
        assertEquals(403, b.post(base + "/loyalty", "active", "1").getStatus());
        assertTrue(clubs.list(s.owner, s.storeId).isEmpty());
        assertTrue(sales.list(s.owner, s.storeId).isEmpty());
    }
}
