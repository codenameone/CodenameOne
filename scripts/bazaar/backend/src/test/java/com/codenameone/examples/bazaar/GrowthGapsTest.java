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
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.CampaignStatsDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubItemDto;
import com.codenameone.examples.bazaar.api.FormResultDto;
import com.codenameone.examples.bazaar.api.FormSubmitDto;
import com.codenameone.examples.bazaar.api.LedgerEntryDto;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyRewardDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.SignupFormDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.domain.CampaignSend;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.MarketingConsent;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.marketing.SignupForms;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CampaignSendRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.MarketingConsentRepository;
import com.codenameone.examples.bazaar.repository.SignupEntryRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.DemoStores;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The growth paths [GrowthTest] leaves out: an A/B campaign run end to end by
/// its scheduler, the referral fraud guard, points redeemed into a quote and
/// the expiry job, members-only and early-access products, the guest forms
/// over HTTP, a confirmed signup reaching a campaign, and the demo seed.
@BackendTest
class GrowthGapsTest {
    static final long DAY = 24L * 3600 * 1000;
    static final long MINUTE = 60000L;

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
    private Audiences audiences;
    @Autowired
    private Campaigns campaigns;
    @Autowired
    private SignupForms forms;
    @Autowired
    private Orders orderService;
    @Autowired
    private DemoStores demoStores;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private CampaignSendRepository sends;
    @Autowired
    private SignupEntryRepository entries;
    @Autowired
    private MarketingConsentRepository consents;
    @Autowired
    private StoreOrderRepository orders;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static final String MARKETABLE =
            "{\"op\":\"AND\",\"rules\":[{\"field\":\"acceptsMarketing\",\"op\":\"is\",\"value\":true}]}";

    private void customerRow(String storeId, String email) throws Exception {
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO customer (id, store_id, username, email, phone, first_name, last_name, "
                + "tags, accepts_marketing, note, orders_count, spent_cents, store_credit_cents, "
                + "created_at, updated_at) VALUES (?, ?, '', ?, '', 'Test', 'Shopper', '', 1, '', 0, 0, 0, ?, ?)",
                new Object[] {Ids.next(), storeId, email, Long.valueOf(now), Long.valueOf(now)});
    }

    // ------------------------------------------------------------------ A/B

    @Test
    void anAbTestPicksAWinnerAndSendsTheRestIt() throws Exception {
        Shop s = shop("gg-ab", 5);
        for (int iter = 0; iter < 10; iter++) {
            customerRow(s.storeId, "ab" + iter + "@example.com");
        }
        AudienceDto seg = audiences.write(s.storeId, null, new AudienceDto(null, "Everyone", MARKETABLE, 0));
        assertEquals(10, seg.count());
        CampaignDto c = campaigns.write(s.storeId, null, new CampaignDto(null, "Split", Campaigns.EMAIL,
                seg.id(), "Subject A", "Subject B", "Body", 50, 5, "", 0, "", "#000000",
                new ArrayList<String>()));
        campaigns.schedule(s.owner, s.storeId, c.id(), 0);
        Instant start = Instant.now();

        // The first pass starts the test: half the audience, split A/B, the rest held.
        assertEquals(5, campaigns.tick(start));
        List<CampaignSend> rows = sends.findByCampaignId(c.id());
        assertEquals(10, rows.size());
        int held = 0;
        List<String> bTokens = new ArrayList<String>();
        for (CampaignSend r : rows) {
            if (Campaigns.HELD.equals(r.getStatus())) {
                held++;
            } else {
                assertEquals(Campaigns.SENT, r.getStatus());
                if ("B".equals(r.getVariant())) {
                    bTokens.add(r.getToken());
                }
            }
        }
        assertEquals(5, held);
        assertFalse(bTokens.isEmpty());
        // Everyone who got B opens it, over HTTP as a mail client does.
        for (String t : bTokens) {
            assertEquals(200, mvc.perform(get("/api/shop/m/o/" + t + ".gif")).andReturn().getResponse()
                    .getStatus());
        }

        // Before the window closes nothing more goes out.
        assertEquals(0, campaigns.tick(start.plusMillis(MINUTE)));
        assertEquals(Campaigns.TESTING, campaigns.list(s.owner, s.storeId).get(0).status());

        // After it, B wins and the held half goes out with B's subject.
        assertEquals(5, campaigns.tick(start.plusMillis(6 * MINUTE)));
        campaigns.tick(start.plusMillis(7 * MINUTE));
        CampaignStatsDto st = campaigns.stats(s.owner, s.storeId, c.id());
        assertEquals("B", st.winner());
        assertEquals(10, st.sent());
        assertEquals(Campaigns.SENT, campaigns.list(s.owner, s.storeId).get(0).status());
        int b = 0;
        for (CampaignSend r : sends.findByCampaignId(c.id())) {
            assertEquals(Campaigns.SENT, r.getStatus());
            if ("B".equals(r.getVariant())) {
                b++;
            }
        }
        assertEquals(bTokens.size() + 5, b);
        // A pass more sends nothing twice.
        assertEquals(0, campaigns.tick(start.plusMillis(8 * MINUTE)));
    }

    // ------------------------------------------------------------- referrals

    private LoyaltyProgramDto program(int referrer, int referee) {
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        tiers.add(new LoyaltyTierDto(null, "Base", 0, 0, false, 0, "#999999", 0));
        return new LoyaltyProgramDto(true, 1, 100, LoyaltyMath.DOWN, 100, 500, 100, 365, true, "SPEND",
                referrer, referee, tiers);
    }

    private static boolean has(List<LedgerEntryDto> ledger, String kind, String noteStart) {
        for (LedgerEntryDto e : ledger) {
            if (kind.equals(e.kind()) && e.note().startsWith(noteStart)) {
                return true;
            }
        }
        return false;
    }

    /// Referee `referee` orders after naming `referrer`'s code; answers the
    /// referrer's balance once the order is paid.
    private long refer(Shop s, String referrer, String referee, String orderEmail, String orderPhone)
            throws Exception {
        LoyaltyMemberDto r = loyalty.me(referrer, s.storeId);
        loyalty.applyReferral(referee, s.storeId, r.referralCode());
        OrderDto o = s.buy(referee, 1, orderEmail);
        if (orderPhone != null) {
            db.execute("UPDATE store_order SET ship_phone = ? WHERE id = ?", new Object[] {orderPhone, o.id()});
        }
        orderService.capturePayment(o.id(), s.owner);
        return loyalty.me(referrer, s.storeId).balance();
    }

    @Test
    void aReferralBetweenTwoPeopleIsRewardedAndOnePersonIsNot() throws Exception {
        Shop s = shop("gg-ref", 50);
        loyalty.save(s.storeId, program(300, 200));

        // Two different people: both rewarded.
        String alice = People.customer(accounts, "gg-ref-alice");
        String bob = People.customer(accounts, "gg-ref-bob");
        assertEquals(300, refer(s, alice, bob, "bob.real@example.com", null));
        assertTrue(has(loyalty.myLedger(bob, s.storeId), Loyalty.REFERRAL, "Welcome"));

        // The same mailbox (plus-addressing folded away).
        String carol = People.customer(accounts, "gg-ref-carol");
        String carol2 = People.customer(accounts, "gg-ref-carol2");
        Customer cc = customers.findById(loyalty.me(carol, s.storeId).customerId());
        db.execute("UPDATE customer SET email = ? WHERE id = ?", new Object[] {"carol@example.com", cc.getId()});
        assertEquals(0, refer(s, carol, carol2, "carol+again@example.com", null));
        assertTrue(has(loyalty.myLedger(carol2, s.storeId), Loyalty.REFERRAL, "Referral not rewarded: same e-mail"));

        // The same phone.
        String dan = People.customer(accounts, "gg-ref-dan");
        String dan2 = People.customer(accounts, "gg-ref-dan2");
        Customer dc = customers.findById(loyalty.me(dan, s.storeId).customerId());
        db.execute("UPDATE customer SET phone = ? WHERE id = ?", new Object[] {"+1 (555) 019-9001", dc.getId()});
        assertEquals(0, refer(s, dan, dan2, "dan.two@example.com", "15550199001"));
        assertTrue(has(loyalty.myLedger(dan2, s.storeId), Loyalty.REFERRAL, "Referral not rewarded: same phone"));

        // The same delivery address: the referrer has had orders shipped there.
        String erin = People.customer(accounts, "gg-ref-erin");
        LoyaltyMemberDto er = loyalty.me(erin, s.storeId);
        s.buy(erin, 1, "erin@example.com");
        String erin2 = People.customer(accounts, "gg-ref-erin2");
        loyalty.applyReferral(erin2, s.storeId, er.referralCode());
        long before = loyalty.me(erin, s.storeId).balance();
        OrderDto o = s.buy(erin2, 1, "erin.two@example.com");
        orderService.capturePayment(o.id(), s.owner);
        assertEquals(before, loyalty.me(erin, s.storeId).balance());
        assertTrue(has(loyalty.myLedger(erin2, s.storeId), Loyalty.REFERRAL, "Referral not rewarded: same address"));
    }

    // --------------------------------------------------- redeem and expiry

    @Test
    void pointsRedeemIntoTheQuoteAndExpireOnSchedule() throws Exception {
        Shop s = shop("gg-pts", 50);
        loyalty.save(s.storeId, program(0, 0));
        String user = People.customer(accounts, "gg-pts-buyer");
        LoyaltyMemberDto m = loyalty.me(user, s.storeId);
        loyalty.adjust(s.owner, s.storeId, m.id(), 300, "goodwill");

        LoyaltyRewardDto reward = loyalty.redeem(user, s.storeId, 100);
        assertEquals(500, reward.cents());
        assertEquals(200, loyalty.me(user, s.storeId).balance());
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 2));
        CartDto quoted = carts.applyDiscount(s.storeId, user, null, reward.code());
        assertTrue(quoted.problems().isEmpty(), quoted.problems().toString());
        assertEquals(reward.code(), quoted.discountCode());
        assertTrue(quoted.discountCents() >= 500, "discount " + quoted.discountCents());
        try {
            loyalty.redeem(user, s.storeId, 1000);
            fail("redeemed more than the balance");
        } catch (ResponseStatusException expected) {
            assertEquals(409, expected.getStatus());
        }

        // Rewards left unused past their 90 days give their points back, once:
        // the one in the cart was quoted, never ordered.
        LoyaltyRewardDto unused = loyalty.redeem(user, s.storeId, 100);
        assertEquals(100, loyalty.me(user, s.storeId).balance());
        Instant later = Instant.now().plusMillis(91 * DAY);
        assertTrue(loyalty.tick(later) >= 2);
        assertEquals(300, loyalty.me(user, s.storeId).balance());
        boolean expired = false;
        for (LoyaltyRewardDto r : loyalty.myRewards(user, s.storeId)) {
            if (r.id().equals(unused.id())) {
                expired = Loyalty.EXPIRED.equals(r.status());
            }
        }
        assertTrue(expired);
        assertEquals(0, loyalty.tick(later.plusMillis(1000)));
        assertEquals(300, loyalty.me(user, s.storeId).balance());

        // A member idle past the programme's 365 days loses the balance, once.
        Instant idle = Instant.now().plusMillis(400 * DAY);
        // Every other test's idle members lapse too when the tests share one database.
        assertTrue(loyalty.tick(idle) >= 1);
        assertEquals(0, loyalty.me(user, s.storeId).balance());
        assertTrue(has(loyalty.myLedger(user, s.storeId), Loyalty.EXPIRE, "Points expired"));
        assertEquals(0, loyalty.tick(idle.plusMillis(1000)));
    }

    // ------------------------------------------------- members-only products

    private boolean listed(Shop s, String user) throws Exception {
        Store store = storeRows.findById(s.storeId);
        com.codename1.backend.test.MockRequestBuilder b = get("/api/shop/stores/" + store.getHandle() + "/products");
        Map page = Rest.json(mvc, user == null ? b : People.asCustomer(b, user));
        for (Object o : (List) page.get("items")) {
            if (s.productId.equals(((Map) o).get("id"))) {
                return true;
            }
        }
        return false;
    }

    private int productStatus(Shop s, String user) throws Exception {
        com.codename1.backend.test.MockRequestBuilder b = get("/api/shop/products/" + s.productId);
        return Rest.statusOf(mvc, user == null ? b : People.asCustomer(b, user));
    }

    private ClubDto clubWithMember(Shop s, String member) throws Exception {
        ClubDto club = clubs.write(s.storeId, null, new ClubDto(null, "Inner circle", "", 500, 30, 0, 48,
                "#000000", true, 0));
        Map card = Rest.addCard(mvc, member, Rest.VISA);
        clubs.join(member, s.storeId, club.id(), (String) card.get("id"));
        return club;
    }

    @Test
    void aMembersOnlyProductIsHiddenAndRefusedForEveryoneElse() throws Exception {
        Shop s = shop("gg-club", 20);
        String member = People.customer(accounts, "gg-club-member");
        String outsider = People.customer(accounts, "gg-club-outsider");
        ClubDto club = clubWithMember(s, member);
        assertTrue(listed(s, null));
        clubs.add(s.storeId, club.id(), new ClubItemDto(null, s.productId, "", true, 0, 0));

        assertFalse(listed(s, null));
        assertFalse(listed(s, outsider));
        assertEquals(404, productStatus(s, null));
        assertEquals(404, productStatus(s, outsider));
        assertTrue(listed(s, member));
        assertEquals(200, productStatus(s, member));

        CartDto refused = carts.add(s.storeId, outsider, null, new CartLineChangeDto(s.variantId, 1));
        assertFalse(refused.problems().isEmpty());
        assertTrue(refused.problems().get(0).indexOf("club members") > 0, refused.problems().toString());
        try {
            s.buy(outsider, 1);
            fail("an outsider bought a members-only product");
        } catch (ResponseStatusException expected) {
            assertEquals(409, expected.getStatus());
        }
        assertNotNull(s.buy(member, 1).id());
    }

    @Test
    void earlyAccessOpensToMembersBeforeThePublicLaunch() throws Exception {
        Shop s = shop("gg-early", 20);
        String member = People.customer(accounts, "gg-early-member");
        String outsider = People.customer(accounts, "gg-early-outsider");
        ClubDto club = clubWithMember(s, member);
        long now = System.currentTimeMillis();
        clubs.add(s.storeId, club.id(), new ClubItemDto(null, s.productId, "", false, now - MINUTE,
                now + 3 * DAY));

        // Visible to everyone, as a launch to look forward to...
        assertTrue(listed(s, null));
        assertEquals(200, productStatus(s, outsider));
        // ...but only members may buy it before the launch.
        CartDto refused = carts.add(s.storeId, outsider, null, new CartLineChangeDto(s.variantId, 1));
        assertTrue(refused.problems().toString().indexOf("not on sale yet") > 0, refused.problems().toString());
        CartDto opened = carts.add(s.storeId, member, null, new CartLineChangeDto(s.variantId, 1));
        assertTrue(opened.problems().isEmpty(), opened.problems().toString());
        try {
            clubs.add(s.storeId, club.id(), new ClubItemDto(null, s.productId, "", false, now + 4 * DAY,
                    now + 3 * DAY));
            fail("members after the public");
        } catch (ResponseStatusException expected) {
            assertEquals(400, expected.getStatus());
        }
    }

    // ------------------------------------------------------- guest forms

    @Test
    void guestsMaySubmitAFormAndAskForAnAlertOverHttp() throws Exception {
        Shop s = shop("gg-guest", 0);
        SignupFormDto f = forms.write(s.storeId, null, new SignupFormDto(null, "News", "Join", Arrays.asList(
                "email"), 0, false, true, "", 0));
        String submit = "/api/shop/stores/" + s.storeId + "/signup-forms/" + f.id() + "/submit";
        assertEquals(200, Rest.statusOf(mvc, Rest.body(post(submit),
                "{\"email\":\"guest.fan@example.com\",\"phone\":\"\",\"name\":\"\",\"trap\":\"\"}")));
        assertEquals(1, entries.findByFormIdAndEmail(f.id(), "guest.fan@example.com").size());
        // A filled honeypot is answered alike and dropped.
        assertEquals(200, Rest.statusOf(mvc, Rest.body(post(submit),
                "{\"email\":\"bot@example.com\",\"phone\":\"\",\"name\":\"\",\"trap\":\"http://spam\"}")));
        assertTrue(entries.findByFormIdAndEmail(f.id(), "bot@example.com").isEmpty());
        assertEquals(400, Rest.statusOf(mvc, Rest.body(post(submit),
                "{\"email\":\"not-an-address\",\"phone\":\"\",\"name\":\"\",\"trap\":\"\"}")));

        String alert = "/api/shop/stores/" + s.storeId + "/stock-alerts";
        Map a = Rest.json(mvc, Rest.body(post(alert), "{\"variantId\":\"" + s.variantId
                + "\",\"email\":\"guest.wait@example.com\",\"channel\":\"EMAIL\",\"status\":\"\","
                + "\"createdAt\":0,\"trap\":\"\"}"));
        assertEquals("PENDING", a.get("status"));
        Map trapped = Rest.json(mvc, Rest.body(post(alert), "{\"variantId\":\"" + s.variantId
                + "\",\"email\":\"bot@example.com\",\"channel\":\"EMAIL\",\"status\":\"\","
                + "\"createdAt\":0,\"trap\":\"x\"}"));
        assertEquals("", trapped.get("id"));
        assertEquals(1, db.query("SELECT id FROM stock_alert WHERE variant_id = ?",
                new Object[] {s.variantId}).size());
        assertEquals(400, Rest.statusOf(mvc, Rest.body(post(alert), "{\"variantId\":\"" + s.variantId
                + "\",\"email\":\"\",\"channel\":\"EMAIL\",\"status\":\"\",\"createdAt\":0,\"trap\":\"\"}")));

        // Opening those two did not open the rest of the shop's writes.
        assertEquals(401, Rest.statusOf(mvc, Rest.body(post("/api/shop/stores/" + s.storeId
                + "/loyalty/me/redeem"), "{\"points\":100}")));
        assertEquals(401, Rest.statusOf(mvc, Rest.body(post("/api/shop/stores/" + s.storeId
                + "/clubs/x/join"), "{}")));
    }

    // ------------------------------------------- confirmed signup -> customer

    @Test
    void aConfirmedSignupIsACustomerWhoAcceptsMarketingUntilTheyUnsubscribe() throws Exception {
        Shop s = shop("gg-optin", 0);
        SignupFormDto f = forms.write(s.storeId, null, new SignupFormDto(null, "News", "Join", Arrays.asList(
                "email"), 0, true, true, "", 0));
        FormResultDto r = forms.submit(s.storeId, f.id(), new FormSubmitDto("Opt.In@Example.com", "", "", ""),
                Instant.now());
        assertEquals(SignupForms.PENDING, r.status());
        // Not before the address is confirmed.
        assertEquals(null, customers.findByStoreIdAndEmail(s.storeId, "opt.in@example.com"));
        forms.confirm(entries.findByFormIdAndEmail(f.id(), "opt.in@example.com").get(0).getConfirmToken(),
                Instant.now());
        Customer c = customers.findByStoreIdAndEmail(s.storeId, "opt.in@example.com");
        assertNotNull(c);
        assertTrue(c.isAcceptsMarketing());
        MarketingConsent consent = consents.findByStoreIdAndAddressAndChannel(s.storeId, "opt.in@example.com",
                Campaigns.EMAIL);
        assertEquals(Campaigns.SUBSCRIBED, consent.getStatus());
        assertEquals("form:" + f.id(), consent.getSource());
        assertTrue(consent.getCreatedAt().toEpochMilli() > 0);

        // A campaign to everyone who accepts marketing reaches them.
        AudienceDto seg = audiences.write(s.storeId, null, new AudienceDto(null, "Subscribers", MARKETABLE, 0));
        assertEquals(1, seg.count());
        CampaignDto camp = campaigns.write(s.storeId, null, new CampaignDto(null, "Hello", Campaigns.EMAIL,
                seg.id(), "Hi", "", "Welcome", 0, 0, "", 0, "", "#000000", new ArrayList<String>()));
        campaigns.schedule(s.owner, s.storeId, camp.id(), 0);
        assertEquals(1, campaigns.tick(Instant.now()));
        CampaignSend sent = sends.findByCampaignId(camp.id()).get(0);
        assertEquals(c.getId(), sent.getCustomerId());

        // Unsubscribing turns the customer's flag off as well as the consent.
        assertTrue(campaigns.unsubscribe(sent.getToken(),
                Tokens.sign(campaigns.secret(s.storeId), "u:" + sent.getToken())));
        assertFalse(customers.findById(c.getId()).isAcceptsMarketing());
        assertEquals(0, audiences.write(s.storeId, seg.id(), new AudienceDto(seg.id(), "Subscribers",
                MARKETABLE, 0)).count());
    }

    // ------------------------------------------------------------ demo seed

    private int count(String table, String storeId) throws Exception {
        return db.query("SELECT id FROM " + table + " WHERE store_id = ?", new Object[] {storeId}).size();
    }

    @Test
    void theDemoStoresCarryTheGrowthRowsOnceAfterTwoMoreSeeds() throws Exception {
        demoStores.seed(DemoAccounts.MERCHANT, DemoAccounts.CASHIER);
        demoStores.seed(DemoAccounts.MERCHANT, DemoAccounts.CASHIER);
        Store store = storeRows.findByHandle(DemoStores.NORTHWIND);
        String id = store.getId();
        assertEquals(2, count("sale_event", id));
        assertEquals(1, count("club", id));
        assertEquals(2, count("club_item", id));
        assertEquals(1, count("journey", id));
        assertEquals(2, count("campaign", id));
        assertEquals(3, count("audience", id));
        assertEquals(1, count("signup_form", id));
        assertEquals(1, count("loyalty_program", id));
        assertEquals(3, count("loyalty_tier", id));
    }
}
