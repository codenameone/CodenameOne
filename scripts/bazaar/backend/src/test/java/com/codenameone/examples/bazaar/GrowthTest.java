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
import com.codenameone.examples.bazaar.alerts.StockAlerts;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.CampaignStatsDto;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.api.FormResultDto;
import com.codenameone.examples.bazaar.api.FormSubmitDto;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.SaleEventDto;
import com.codenameone.examples.bazaar.api.SaleItemDto;
import com.codenameone.examples.bazaar.api.SignupFormDto;
import com.codenameone.examples.bazaar.api.StockAlertDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.domain.CampaignSend;
import com.codenameone.examples.bazaar.domain.SignupEntry;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.marketing.DemoMarketing;
import com.codenameone.examples.bazaar.marketing.SignupForms;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CampaignSendRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.SignupEntryRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.sale.SaleEvents;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The growth features against the database: points earned once and
/// reversed once, sale prices applied and reverted once, the flash cap, a
/// club renewal, a back-in-stock alert, a campaign sent, tracked and
/// unsubscribed, a signup, a journey run and the demo seed.
@BackendTest
class GrowthTest {
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
    private Loyalty loyalty;
    @Autowired
    private Clubs clubs;
    @Autowired
    private SaleEvents sales;
    @Autowired
    private StockAlerts alerts;
    @Autowired
    private Audiences audiences;
    @Autowired
    private Campaigns campaigns;
    @Autowired
    private SignupForms forms;
    @Autowired
    private Journeys journeys;
    @Autowired
    private DemoMarketing demo;
    @Autowired
    private com.codenameone.examples.bazaar.order.Orders orderService;
    @Autowired
    private VariantRepository variants;
    @Autowired
    private StoreOrderRepository orders;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private CampaignSendRepository sends;
    @Autowired
    private SignupEntryRepository entries;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static int status(Exception e) {
        return e instanceof ResponseStatusException ? ((ResponseStatusException) e).getStatus() : -1;
    }

    @Test
    void pointsAreEarnedOnceAndReversedOnce() throws Exception {
        Shop s = shop("gr-points", 50);
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        tiers.add(new LoyaltyTierDto(null, "Base", 0, 0, false, 0, "#999999", 0));
        loyalty.save(s.storeId, new LoyaltyProgramDto(true, 1, 100, LoyaltyMath.DOWN, 100, 500, 100, 365,
                true, "SPEND", 0, 0, tiers));
        String user = People.customer(accounts, "gr-points-buyer");
        OrderDto o = s.buy(user, 2);
        assertEquals(0, loyalty.me(user, s.storeId).balance());
        orderService.capturePayment(o.id(), s.owner);
        long balance = loyalty.me(user, s.storeId).balance();
        assertTrue(balance > 0, "earned " + balance);
        assertEquals(0, loyalty.earn(o.id()));
        assertEquals(balance, loyalty.me(user, s.storeId).balance());

        loyalty.reverse(o.id(), true);
        assertEquals(0, loyalty.me(user, s.storeId).balance());
        loyalty.reverse(o.id(), true);
        assertEquals(0, loyalty.me(user, s.storeId).balance());
    }

    @Test
    void saleTickAppliesOnceAndRevertsOnce() throws Exception {
        Shop s = shop("gr-sale", 50);
        long price = variants.findById(s.variantId).getPriceCents();
        Instant now = Instant.now();
        sales.insert(s.storeId, new SaleEventDto(null, "Half off", SaleEvents.SALE, SaleEvents.PERCENT, 5000,
                now.toEpochMilli() - 60000, now.toEpochMilli() + DAY, "", 0, 0, "#000000",
                Arrays.asList(new SaleItemDto(s.variantId, 0, 0, ""))), now);
        try {
            sales.insert(s.storeId, new SaleEventDto(null, "Clash", SaleEvents.SALE, SaleEvents.PERCENT, 1000,
                    now.toEpochMilli(), now.toEpochMilli() + DAY, "", 0, 0, "#000000",
                    Arrays.asList(new SaleItemDto(s.variantId, 0, 0, ""))), now);
            fail("overlapping sale accepted");
        } catch (ResponseStatusException expected) {
            assertEquals(409, expected.getStatus());
        }
        sales.tick(now);
        assertEquals(SaleEvents.salePrice(price, SaleEvents.PERCENT, 5000),
                variants.findById(s.variantId).getPriceCents());
        assertEquals(price, variants.findById(s.variantId).getCompareAtCents());
        sales.tick(now.plusMillis(1000));
        assertEquals(SaleEvents.salePrice(price, SaleEvents.PERCENT, 5000),
                variants.findById(s.variantId).getPriceCents());

        Instant after = now.plusMillis(2 * DAY);
        sales.tick(after);
        assertEquals(price, variants.findById(s.variantId).getPriceCents());
        sales.tick(after.plusMillis(1000));
        assertEquals(price, variants.findById(s.variantId).getPriceCents());
    }

    @Test
    void aFlashSaleSellsOnlyItsCap() throws Exception {
        Shop s = shop("gr-flash", 50);
        Instant now = Instant.now();
        sales.insert(s.storeId, new SaleEventDto(null, "Flash", SaleEvents.FLASH, SaleEvents.PERCENT, 3000,
                now.toEpochMilli() - 60000, now.toEpochMilli() + DAY, "", 2, 0, "#000000",
                Arrays.asList(new SaleItemDto(s.variantId, 0, 0, ""))), now);
        sales.tick(now);
        s.buy(People.customer(accounts, "gr-flash-a"), 2);
        try {
            s.buy(People.customer(accounts, "gr-flash-b"), 1);
            fail("sold past the cap");
        } catch (ResponseStatusException expected) {
            assertEquals(409, expected.getStatus());
        }
    }

    @Test
    void aClubChargesOnJoinAndRenews() throws Exception {
        Shop s = shop("gr-club", 10);
        ClubDto club = clubs.write(s.storeId, null, new ClubDto(null, "Insiders", "", 900, 30, 1000, 0,
                "#000000", true, 0));
        String user = People.customer(accounts, "gr-club-member");
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        ClubMembershipDto m = clubs.join(user, s.storeId, club.id(), (String) card.get("id"));
        assertEquals("ACTIVE", m.status());
        long first = m.renewsAt();
        assertTrue(first > System.currentTimeMillis() + 29 * DAY);

        clubs.tick(Instant.ofEpochMilli(first + 1000));
        long second = clubs.mine(user, s.storeId).get(0).renewsAt();
        assertTrue(second >= first + 29 * DAY, first + " -> " + second);
        clubs.tick(Instant.ofEpochMilli(first + 2000));
        assertEquals(second, clubs.mine(user, s.storeId).get(0).renewsAt());
    }

    @Test
    void aBackInStockAlertGoesOutOnce() throws Exception {
        Shop s = shop("gr-alert", 1);
        db.execute("UPDATE inventory_level SET on_hand = 0 WHERE variant_id = ?", new Object[] {s.variantId});
        StockAlertDto a = alerts.subscribe("gr-alert-shopper", s.storeId,
                new StockAlertDto(null, s.variantId, "alert@example.com", "EMAIL", "", 0, ""));
        assertEquals("PENDING", a.status());
        assertEquals(0, alerts.tick());
        db.execute("UPDATE inventory_level SET on_hand = 5 WHERE variant_id = ?", new Object[] {s.variantId});
        assertEquals(1, alerts.tick());
        assertEquals(0, alerts.tick());
    }

    @Test
    void aCampaignGoesToTheSegmentIsTrackedAndHonoursUnsubscribe() throws Exception {
        Shop s = shop("gr-camp", 50);
        String user = People.customer(accounts, "gr-camp-buyer");
        s.buy(user, 1, "camp.buyer@example.com");
        db.execute("UPDATE customer SET accepts_marketing = ? WHERE store_id = ?",
                new Object[] {Boolean.TRUE, s.storeId});
        AudienceDto seg = audiences.write(s.storeId, null, new AudienceDto(null, "Buyers",
                "{\"op\":\"AND\",\"rules\":[{\"field\":\"ordersCount\",\"op\":\">=\",\"value\":1}]}", 0));
        assertEquals(1, seg.count());
        assertEquals(1, audiences.preview(s.owner, s.storeId, seg).sample().size());

        CampaignDto c = campaigns.write(s.storeId, null, new CampaignDto(null, "Hello", Campaigns.EMAIL,
                seg.id(), "Hi there", "", "Something new", 0, 0, "", 0, "", "#000000",
                Arrays.asList("https://example.com/new")));
        campaigns.schedule(s.owner, s.storeId, c.id(), 0);
        campaigns.tick(Instant.now());
        List<CampaignSend> rows = sends.findByCampaignId(c.id());
        assertEquals(1, rows.size());
        assertEquals(Campaigns.SENT, rows.get(0).getStatus());
        campaigns.tick(Instant.now());
        assertEquals(1, sends.findByCampaignId(c.id()).size());

        String token = rows.get(0).getToken();
        // The links a mail client follows, anonymously over HTTP.
        MockResponse pixel = mvc.perform(get("/api/shop/m/o/" + token + ".gif")).andReturn().getResponse();
        assertEquals(200, pixel.getStatus());
        assertEquals(200, mvc.perform(get("/api/shop/m/o/forged.gif")).andReturn().getResponse().getStatus());
        MockResponse click = mvc.perform(get("/api/shop/m/c/" + token + "/0")).andReturn().getResponse();
        assertEquals(302, click.getStatus());
        assertEquals("https://example.com/new", click.getHeader("Location"));
        assertEquals(404, mvc.perform(get("/api/shop/m/c/" + token + "/4")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/shop/m/u/" + token + "?s=bad")).andReturn().getResponse()
                .getStatus());
        assertEquals(400, mvc.perform(get("/api/shop/m/confirm/nothing")).andReturn().getResponse().getStatus());
        assertEquals("https://example.com/new", campaigns.clicked(token, 0));
        assertNull(campaigns.clicked(token, 3));
        assertNull(campaigns.clicked("forged", 0));

        // A later order is credited to the message that was opened.
        s.buy(user, 1, "camp.buyer@example.com");
        CampaignStatsDto st = campaigns.stats(s.owner, s.storeId, c.id());
        assertEquals(1, st.sent());
        assertEquals(1, st.opened());
        assertEquals(1, st.clicked());
        assertEquals(1, st.orders());
        assertTrue(st.revenueCents() > 0);

        assertFalse(campaigns.unsubscribe(token, "deadbeef"));
        assertTrue(campaigns.consented(s.storeId, rows.get(0).getAddress(), Campaigns.EMAIL));
        assertTrue(campaigns.unsubscribe(token, Tokens.sign(campaigns.secret(s.storeId), "u:" + token)));
        assertFalse(campaigns.consented(s.storeId, rows.get(0).getAddress(), Campaigns.EMAIL));

        // A second campaign skips the address that opted out.
        CampaignDto again = campaigns.write(s.storeId, null, new CampaignDto(null, "Again", Campaigns.EMAIL,
                seg.id(), "Hi again", "", "More", 0, 0, "", 0, "", "#000000", new ArrayList<String>()));
        campaigns.schedule(s.owner, s.storeId, again.id(), 0);
        campaigns.tick(Instant.now());
        assertEquals(0, sends.findByCampaignId(again.id()).size());
    }

    @Test
    void signupsConfirmOnceAndIgnoreTheHoneypot() throws Exception {
        Shop s = shop("gr-form", 1);
        SignupFormDto f = forms.write(s.storeId, null, new SignupFormDto(null, "News", "Join", Arrays.asList(
                "email", "name"), 1000, true, true, "", 0));
        FormResultDto bot = forms.submit(s.storeId, f.id(), new FormSubmitDto("bot@example.com", "", "",
                "http://spam"), Instant.now());
        assertEquals(SignupForms.PENDING, bot.status());
        assertTrue(entries.findByFormIdAndEmail(f.id(), "bot@example.com").isEmpty());

        FormResultDto r = forms.submit(s.storeId, f.id(), new FormSubmitDto("Fan@Example.com", "", "Fan", ""),
                Instant.now());
        assertEquals(SignupForms.PENDING, r.status());
        SignupEntry e = entries.findByFormIdAndEmail(f.id(), "fan@example.com").get(0);
        String code = forms.confirm(e.getConfirmToken(), Instant.now());
        assertNotNull(code);
        assertTrue(code.startsWith("HELLO"), code);
        assertNull(forms.confirm(e.getConfirmToken(), Instant.now()));
        assertEquals(SignupForms.CONFIRMED, forms.submit(s.storeId, f.id(),
                new FormSubmitDto("fan@example.com", "", "", ""), Instant.now()).status());
        try {
            forms.submit(s.storeId, f.id(), new FormSubmitDto("not an address", "", "", ""), Instant.now());
            fail("bad address accepted");
        } catch (ResponseStatusException expected) {
            assertEquals(400, expected.getStatus());
        }
    }

    @Test
    void aPaidOrderWalksTheJourney() throws Exception {
        Shop s = shop("gr-journey", 20);
        String graph = "{\"nodes\":[{\"id\":\"e\",\"data\":{\"type\":\"" + JourneyCatalog.ENTRY_TRIGGER
                + "\",\"event\":\"order.paid\"}},{\"id\":\"t\",\"data\":{\"type\":\"" + JourneyCatalog.TAG_ADD
                + "\",\"tag\":\"journeyed\"}},{\"id\":\"w\",\"data\":{\"type\":\"" + JourneyCatalog.WAIT
                + "\",\"minutes\":60}},{\"id\":\"x\",\"data\":{\"type\":\"" + JourneyCatalog.EXIT + "\"}}],"
                + "\"edges\":[{\"from\":\"e\",\"fromPort\":\"out\",\"to\":\"t\"},"
                + "{\"from\":\"t\",\"fromPort\":\"out\",\"to\":\"w\"},{\"from\":\"w\",\"fromPort\":\"out\",\"to\":\"x\"}]}";
        try {
            journeys.write(s.storeId, null, new JourneyDto(null, "Broken", "{\"nodes\":[]}", Journeys.ACTIVE, 0,
                    null));
            fail("an empty journey went active");
        } catch (ResponseStatusException expected) {
            assertEquals(400, expected.getStatus());
        }
        JourneyDto j = journeys.write(s.storeId, null, new JourneyDto(null, "Thanks", graph, Journeys.ACTIVE, 0,
                null));
        assertTrue(j.problems().isEmpty(), j.problems().toString());
        OrderDto o = s.buy(People.customer(accounts, "gr-journey-buyer"), 1);
        orderService.capturePayment(o.id(), s.owner);
        Instant now = Instant.now();
        // The tick is global: demo stores other tests seeded run their own
        // journeys over their (90-day) history in the same pass, so this
        // journey's two steps are a floor, not the count.
        final int stepped = journeys.tick(now);
        assertTrue(stepped >= 2, () -> "steps: " + stepped);
        StoreOrder so = orders.findById(o.id());
        assertTrue(customers.findById(so.getCustomerId()).getTags().indexOf("journeyed") >= 0);
        assertEquals(0, journeys.tick(now.plusMillis(1000)));
        assertEquals(stat(s, j, "w").current(), 1);
        journeys.tick(now.plusMillis(61 * 60000L));
        assertEquals(0, stat(s, j, "w").current());
        assertEquals(1, stat(s, j, "x").passed());
        assertEquals(1, stat(s, j, "t").passed());
        assertEquals(11, journeys.catalog(s.owner, s.storeId).size());
    }

    private JourneyNodeStatDto stat(Shop s, JourneyDto j, String node) throws Exception {
        List<JourneyNodeStatDto> all = journeys.stats(s.owner, s.storeId, j.id());
        for (int iter = 0; iter < all.size(); iter++) {
            if (node.equals(all.get(iter).nodeId())) {
                return all.get(iter);
            }
        }
        throw new AssertionError("no node " + node);
    }

    @Test
    void theDemoSeedsOnce() throws Exception {
        Shop s = shop("gr-demo", 30);
        demo.seed(s.storeId);
        demo.seed(s.storeId);
        assertEquals(3, audiences.list(s.owner, s.storeId).size());
        assertEquals(2, campaigns.list(s.owner, s.storeId).size());
        assertEquals(1, journeys.list(s.owner, s.storeId).size());
        assertTrue(journeys.list(s.owner, s.storeId).get(0).problems().isEmpty(),
                journeys.list(s.owner, s.storeId).get(0).problems().toString());
        assertEquals(3, loyalty.program(s.owner, s.storeId).tiers().size());
        assertEquals(1, clubs.list(s.owner, s.storeId).size());
        assertEquals(1, forms.list(s.owner, s.storeId).size());
        assertEquals(0, status(new RuntimeException()) + 1);
    }
}
