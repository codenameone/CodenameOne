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
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.StockAlert;
import com.codenameone.examples.bazaar.domain.Wishlist;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StockAlertRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The web account area: each page's main action, and every resource of
/// another shopper answering 404.
@BackendTest
class WebAccountTest {
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
    private Returns returns;
    @Autowired
    private CustomerAddressRepository addresses;
    @Autowired
    private WishlistRepository wishlist;
    @Autowired
    private ReviewRepository reviews;
    @Autowired
    private StockAlertRepository alerts;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private Campaigns campaigns;
    @Autowired
    private Loyalty loyalty;
    @Autowired
    private TotpService totp;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private Browser signedIn(String user) throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        return b;
    }

    private static void redirected(MockResponse r) {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303, "answered " + r.getStatus());
    }

    @Test
    void ordersPageListsDetailCancelsAndHidesOthers() throws Exception {
        Shop s = shop("wa-orders");
        String user = People.customer(accounts, "wa-orders-buyer");
        String other = People.customer(accounts, "wa-orders-other");
        OrderDto o = s.buy(user, 1);
        Browser b = signedIn(user);

        MockResponse list = b.get("/account/orders");
        b.assertPage(list, "Order #" + o.number());
        assertEquals("private, no-store", list.getHeader("Cache-Control"));
        assertTrue(list.getHeader("Content-Security-Policy").contains("nonce-"));

        MockResponse detail = b.get("/account/orders/" + o.id());
        b.assertPage(detail, "Cancel order");
        b.assertPage(detail, "Timeline");
        MockResponse pdf = b.get("/account/orders/" + o.id() + "/invoice.pdf");
        assertEquals(200, pdf.getStatus());
        assertEquals("application/pdf", pdf.getContentType());

        Browser stranger = signedIn(other);
        assertEquals(404, stranger.get("/account/orders/" + o.id()).getStatus());
        assertEquals(404, stranger.get("/account/orders/" + o.id() + "/invoice.pdf").getStatus());
        stranger.get("/account/orders");
        assertEquals(404, stranger.post("/account/orders/" + o.id() + "/cancel").getStatus());
        assertEquals(Orders.OPEN, orders.detailOf(o.id()).status());

        redirected(b.post("/account/orders/" + o.id() + "/cancel"));
        assertEquals(Orders.CANCELLED, orders.detailOf(o.id()).status());
    }

    @Test
    void aShippedLineCanBeReturned() throws Exception {
        Shop s = shop("wa-return");
        String user = People.customer(accounts, "wa-return-buyer");
        String other = People.customer(accounts, "wa-return-other");
        OrderDto o = s.buy(user, 2);
        String line = o.lines().get(0).id();
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(line, null, null, 2));
        String fid = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls)).id();
        fulfillments.ship(s.owner, s.storeId, fid);
        fulfillments.delivered(s.owner, s.storeId, fid);
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/orders/" + o.id()), "Request a return");

        Browser stranger = signedIn(other);
        stranger.get("/account");
        assertEquals(404, stranger.post("/account/orders/" + o.id() + "/return",
                "qty-" + line, "1", "reason", "DAMAGED").getStatus());

        redirected(b.post("/account/orders/" + o.id() + "/return", "qty-" + line, "1",
                "reason", "DAMAGED", "note", "Chipped"));
        assertEquals(1, returns.mine(user, o.id()).size());
        b.assertPage(b.get("/account/orders/" + o.id()), "Returns");
    }

    @Test
    void addressesAreAddedEditedMadeDefaultAndRemoved() throws Exception {
        String user = People.customer(accounts, "wa-addr");
        String other = People.customer(accounts, "wa-addr-other");
        Browser b = signedIn(user);
        b.get("/account/addresses");
        redirected(b.post("/account/addresses", "name", "Ada", "line1", "1 Main St",
                "city", "Albany", "country", "us"));
        redirected(b.post("/account/addresses", "name", "Ada", "line1", "2 Side St",
                "city", "Troy", "country", "US"));
        assertEquals(2, addresses.findByUsername(user).size());
        String second = null;
        for (int i = 0; i < 2; i++) {
            if ("2 Side St".equals(addresses.findByUsername(user).get(i).getLine1())) {
                second = addresses.findByUsername(user).get(i).getId();
            }
        }
        assertNotNull(second);
        b.assertPage(b.get("/account/addresses"), "2 Side St");

        Browser stranger = signedIn(other);
        stranger.get("/account/addresses");
        assertEquals(404, stranger.post("/account/addresses/" + second, "name", "Mallory",
                "line1", "x", "city", "x", "country", "US").getStatus());
        assertEquals(404, stranger.post("/account/addresses/" + second + "/delete").getStatus());

        redirected(b.post("/account/addresses/" + second, "name", "Ada L", "line1", "3 Edit Rd",
                "city", "Troy", "country", "US"));
        assertEquals("3 Edit Rd", addresses.findById(second).getLine1());
        redirected(b.post("/account/addresses/" + second + "/default"));
        assertTrue(addresses.findById(second).isIsDefault());
        redirected(b.post("/account/addresses/" + second + "/delete"));
        assertEquals(1, addresses.findByUsername(user).size());
    }

    @Test
    void wishlistRemovesAndOffersTheCart() throws Exception {
        Shop s = shop("wa-wish");
        String user = People.customer(accounts, "wa-wish-buyer");
        Wishlist w = new Wishlist();
        w.setId(Ids.next());
        w.setUsername(user);
        w.setProductId(s.productId);
        w.setStoreId(s.storeId);
        w.setCreatedAt(Instant.now());
        wishlist.insert(w);
        Browser b = signedIn(user);
        MockResponse page = b.get("/account/wishlist");
        b.assertPage(page, "Add to cart");
        assertTrue(page.getContentAsString().contains(s.variantId));
        redirected(b.post("/account/wishlist/" + s.productId + "/remove"));
        assertTrue(wishlist.findByUsernameOrderByCreatedAtDesc(user).isEmpty());
    }

    private Review review(Shop s, String user, String status) throws Exception {
        Review r = new Review();
        r.setId(Ids.next());
        r.setStoreId(s.storeId);
        r.setProductId(s.productId);
        r.setUsername(user);
        r.setAuthorName("Ada");
        r.setRating(3);
        r.setTitle("Fine");
        r.setBody("A mug.");
        r.setStatus(status);
        r.setCreatedAt(Instant.now());
        reviews.insert(r);
        return r;
    }

    @Test
    void pendingReviewsAreEditedAndDeleted() throws Exception {
        Shop s = shop("wa-review");
        String user = People.customer(accounts, "wa-review-buyer");
        String other = People.customer(accounts, "wa-review-other");
        Review r = review(s, user, "PENDING");
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/reviews"), "A mug.");

        Browser stranger = signedIn(other);
        stranger.get("/account/reviews");
        assertEquals(404, stranger.post("/account/reviews/" + r.getId(), "rating", "1",
                "title", "Bad", "body", "x").getStatus());
        assertEquals(404, stranger.post("/account/reviews/" + r.getId() + "/delete").getStatus());

        redirected(b.post("/account/reviews/" + r.getId(), "rating", "5", "title", "Great",
                "body", "Lovely mug."));
        assertEquals(5, reviews.findById(r.getId()).getRating());
        assertEquals("Lovely mug.", reviews.findById(r.getId()).getBody());
        redirected(b.post("/account/reviews/" + r.getId() + "/delete"));
        assertEquals(null, reviews.findById(r.getId()));

        Review published = review(s, user, "PUBLISHED");
        redirected(b.post("/account/reviews/" + published.getId(), "rating", "1", "title", "x",
                "body", "x"));
        assertEquals(3, reviews.findById(published.getId()).getRating());
    }

    @Test
    void loyaltyShowsBalanceTierReferralAndCard() throws Exception {
        Shop s = shop("wa-loyal");
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        tiers.add(new LoyaltyTierDto(null, "Base", 0, 0, false, 0, "#999999", 0));
        tiers.add(new LoyaltyTierDto(null, "Gold", 100000, 0, false, 0, "#cc9900", 1));
        loyalty.save(s.storeId, new LoyaltyProgramDto(true, 1, 100, LoyaltyMath.DOWN, 100, 500, 100,
                365, true, "SPEND", 0, 0, tiers));
        String user = People.customer(accounts, "wa-loyal-buyer");
        s.buy(user, 1);
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/loyalty"), "Join the programme");
        redirected(b.post("/account/loyalty/" + s.storeId + "/join"));
        MockResponse page = b.get("/account/loyalty");
        b.assertPage(page, "Refer a friend");
        String html = page.getContentAsString();
        assertTrue(html.contains("<progress"), "tier progress bar");
        assertTrue(html.contains("<svg") && html.contains(" d=\"M"), "inline QR code");
        assertTrue(html.contains("?ref="), "share link");

        String other = People.customer(accounts, "wa-loyal-other");
        Browser stranger = signedIn(other);
        stranger.get("/account/loyalty");
        assertEquals(404, stranger.post("/account/loyalty/" + s.storeId + "/join").getStatus());
    }

    @Test
    void clubsAndStockAlertsArePerShopper() throws Exception {
        Shop s = shop("wa-alerts");
        String user = People.customer(accounts, "wa-alert-buyer");
        String other = People.customer(accounts, "wa-alert-other");
        StockAlert a = new StockAlert();
        a.setId(Ids.next());
        a.setStoreId(s.storeId);
        a.setVariantId(s.variantId);
        a.setEmail(user);
        a.setUsername(user);
        a.setChannel("EMAIL");
        a.setStatus("PENDING");
        a.setCreatedAt(Instant.now());
        a.setSentAt(Instant.EPOCH);
        alerts.insert(a);
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/alerts"), "Stop waiting");
        b.assertPage(b.get("/account/clubs"), "Clubs");

        Browser stranger = signedIn(other);
        assertFalse(stranger.get("/account/alerts").getContentAsString().contains("Stop waiting"));
        assertEquals(404, stranger.post("/account/alerts/" + a.getId() + "/delete").getStatus());
        redirected(b.post("/account/alerts/" + a.getId() + "/delete"));
        assertEquals(null, alerts.findById(a.getId()));
    }

    @Test
    void marketingTogglesWriteWhatSendingReads() throws Exception {
        Shop s = shop("wa-mkt");
        String user = People.customer(accounts, "wa-mkt-buyer");
        s.buy(user, 1);
        Customer c = customers.findByStoreIdAndUsername(s.storeId, user);
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/marketing"), "Marketing preferences");
        redirected(b.post("/account/marketing/" + s.storeId, "email", "on"));
        c = customers.findByStoreIdAndUsername(s.storeId, user);
        String email = c.getEmail().length() == 0 ? user : c.getEmail();
        assertTrue(c.isAcceptsMarketing());
        assertTrue(campaigns.consented(s.storeId, email, Campaigns.EMAIL));
        redirected(b.post("/account/marketing/" + s.storeId));
        assertFalse(customers.findByStoreIdAndUsername(s.storeId, user).isAcceptsMarketing());
        assertFalse(campaigns.consented(s.storeId, email, Campaigns.EMAIL));

        Shop elsewhere = shop("wa-mkt-other");
        assertEquals(404, b.post("/account/marketing/" + elsewhere.storeId, "email", "on").getStatus());
    }

    @Test
    void totpEnrolmentShowsRecoveryCodesOnceAndDisablingNeedsThePassword() throws Exception {
        String user = People.customer(accounts, "wa-totp");
        Browser b = signedIn(user);
        b.assertPage(b.get("/account/security"), "Set up an authenticator app");
        redirected(b.post("/account/security/totp/start"));
        MockResponse enrol = b.get("/account/security");
        b.assertPage(enrol, "Key:");
        assertTrue(enrol.getContentAsString().contains(" d=\"M"), "inline QR code");
        assertFalse(totp.isEnabled(user));

        redirected(b.post("/account/security/totp/confirm", "code", "000000x"));
        assertFalse(totp.isEnabled(user));
        MockResponse on = b.post("/account/security/totp/confirm", "code", totp.currentCode(user));
        b.assertPage(on, "Your recovery codes");
        assertTrue(totp.isEnabled(user));
        List<String> codes = codes(on.getContentAsString());
        assertEquals(10, codes.size());
        assertFalse(b.get("/account/security").getContentAsString().contains(codes.get(0)),
                "shown once");
        List<String> stored = new JdbcRecoveryCodeRepository(db).findHashes(user);
        assertEquals(10, stored.size());
        for (int i = 0; i < stored.size(); i++) {
            for (int c = 0; c < codes.size(); c++) {
                assertFalse(stored.get(i).contains(codes.get(c)), "stored hashed");
            }
        }
        redirected(b.post("/account/security/totp/start"));
        assertTrue(totp.isEnabled(user), "a new enrolment cannot replace a working factor");

        redirected(b.post("/account/security/totp/disable", "password", "wrong"));
        assertTrue(totp.isEnabled(user));
        redirected(b.post("/account/security/totp/disable", "password", People.PASSWORD));
        assertFalse(totp.isEnabled(user));
        assertEquals(0, new JdbcRecoveryCodeRepository(db).count(user));
    }

    private static List<String> codes(String html) {
        List<String> out = new ArrayList<String>();
        int at = html.indexOf("class=\"codes\"");
        int end = html.indexOf("</ul>", at);
        int from = at;
        while (true) {
            int c = html.indexOf("<code>", from);
            if (c < 0 || c > end) {
                break;
            }
            int close = html.indexOf("</code>", c);
            out.add(html.substring(c + 6, close));
            from = close;
        }
        return out;
    }

    @Test
    void signingOutOtherSessionsEndsThem() throws Exception {
        String user = People.customer(accounts, "wa-sessions");
        Browser here = signedIn(user);
        Browser there = signedIn(user);
        here.assertPage(here.get("/account"), "Hello");
        there.assertPage(there.get("/account"), "Hello");
        MockResponse list = here.get("/account/security");
        here.assertPage(list, "This browser");

        redirected(here.post("/account/security/sessions/end-others"));
        assertNotEquals(200, there.get("/account").getStatus());
        assertNotEquals(200, there.get("/account").getStatus(), "and stays signed out");
        here.assertPage(here.get("/account"), "Hello");
    }

    @Test
    void accountPostsNeedTheCsrfToken() throws Exception {
        String user = People.customer(accounts, "wa-csrf");
        Browser b = signedIn(user);
        b.get("/account/addresses");
        assertEquals(403, b.postWithoutToken("/account/addresses", "name", "Ada", "line1", "1",
                "city", "x", "country", "US").getStatus());
        assertTrue(addresses.findByUsername(user).isEmpty());
    }
}
