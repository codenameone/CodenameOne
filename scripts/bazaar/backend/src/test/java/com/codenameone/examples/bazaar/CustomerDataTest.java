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

import com.codename1.backend.Base64;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.DataExportDto;
import com.codenameone.examples.bazaar.api.DataRequestDto;
import com.codenameone.examples.bazaar.api.DiscountCodesDto;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.ExportDto;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.customer.CreditMath;
import com.codenameone.examples.bazaar.customer.CustomerData;
import com.codenameone.examples.bazaar.customer.StoreCredit;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.ReviewRequest;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.marketing.MarketingMails;
import com.codenameone.examples.bazaar.marketing.ReviewLinks;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.rates.DiscountCodes;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.ReviewRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Customer data (merge, export, erasure), expiring store credit spent at
/// checkout, bulk single-use codes, and the automatic mails: browse
/// abandonment, the review request and the scheduled gift card.
@BackendTest
class CustomerDataTest {
    private static final long DAY = 86400000L;

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
    private StoreCredit credit;
    @Autowired
    private CustomerData data;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private StoreOrderRepository orders;
    @Autowired
    private RateSettings rates;
    @Autowired
    private DiscountCodes codes;
    @Autowired
    private DiscountRepository discounts;
    @Autowired
    private MarketingMails automations;
    @Autowired
    private ReviewLinks reviewLinks;
    @Autowired
    private ReviewRequestRepository reviewRequests;
    @Autowired
    private ReviewRepository reviews;
    @Autowired
    private GiftCards giftCards;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    private final MailSender mail = MailSender.current();

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    private Customer customer(Shop s, String user) {
        Customer c = customers.findByStoreIdAndUsername(s.storeId, user);
        assertNotNull(c, "the order made " + user + " a customer");
        return c;
    }

    // ------------------------------------------------------------ credit

    @Test
    void creditIsSpentSoonestToLapseFirst() {
        long now = 1000000L;
        long[] remaining = {500, 300, 200};
        long[] expires = {0, now + 2 * DAY, now + DAY};
        long[] created = {1, 2, 3};
        long[] parts = CreditMath.plan(remaining, expires, created, now, 400);
        assertEquals(0, parts[0], "never-expiring credit goes last");
        assertEquals(200, parts[1]);
        assertEquals(200, parts[2], "the grant lapsing first goes first");
        assertNull(CreditMath.plan(remaining, expires, created, now, 1001), "more than held");
        assertEquals(500, CreditMath.available(remaining, expires, now + 3 * DAY),
                "lapsed grants pay nothing");
    }

    @Test
    void expiringCreditLapsesAndRestoresOntoItsGrant() throws Exception {
        Shop s = shop("credit-ledger", 5);
        String user = People.customer(accounts, "credit-ledger-buyer");
        s.buy(user, 1);
        Customer c = customer(s, user);
        long base = c.getStoreCreditCents();
        credit.grant(s.storeId, c.getId(), 500, Instant.now().plusMillis(DAY), "Goodwill");
        credit.grant(s.storeId, c.getId(), 300, null, "Return");
        assertEquals(base + 800, customers.findById(c.getId()).getStoreCreditCents());
        assertTrue(credit.take(c.getId(), 600, "order-x"));
        assertEquals(600, credit.spentOn("order-x"));
        assertEquals(600, credit.restore("order-x", Long.MAX_VALUE));
        assertEquals(base + 800, customers.findById(c.getId()).getStoreCreditCents());
        assertThrows(ResponseStatusException.class, () -> credit.grant(s.storeId, c.getId(), 100,
                Instant.now().minusMillis(DAY), "past"), "an expiry in the past is refused");
        long lapsed = credit.sweep(System.currentTimeMillis() + 2 * DAY);
        assertTrue(lapsed >= 500, "the dated grant lapsed: " + lapsed);
        assertEquals(base + 300, customers.findById(c.getId()).getStoreCreditCents());
        List rows = db.query("SELECT id FROM audit_event WHERE action = 'STORE_CREDIT_EXPIRED' "
                + "AND subject = ?", new Object[] {c.getId()});
        assertEquals(1, rows.size(), "the lapse is audited");
    }

    @Test
    void storeCreditPaysAtCheckout() throws Exception {
        Shop s = shop("credit-checkout", 5);
        String user = People.customer(accounts, "credit-checkout-buyer");
        s.buy(user, 1);
        Customer c = customer(s, user);
        long before = c.getStoreCreditCents();
        credit.grant(s.storeId, c.getId(), 400, Instant.now().plusMillis(30 * DAY), "Thanks");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto off = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        assertEquals(before + 400, off.creditAvailableCents());
        assertEquals(0, off.storeCreditCents(), "not spent until asked");
        carts.useStoreCredit(s.storeId, user, null, true);
        CartDto on = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        assertTrue(on.useStoreCredit());
        long spend = Math.min(before + 400, on.totalCents() - on.giftCardCents());
        assertEquals(spend, on.storeCreditCents());
        assertEquals(on.totalCents() - on.giftCardCents() - spend, on.dueCents());
        String rate = null;
        for (ShippingOptionDto o : on.shippingOptions()) {
            rate = rate == null ? o.rateId() : rate;
        }
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        OrderDto o = checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                on.shippingRateId() == null ? rate : on.shippingRateId(), (String) card.get("id"),
                user + "@example.com", "", null, 0, null));
        assertEquals(before + 400 - spend, customers.findById(c.getId()).getStoreCreditCents());
        assertEquals(spend, credit.spentOn(o.id()), "the order recorded what it took");
    }

    // ------------------------------------------------------------- merge

    @Test
    void mergeMovesEverythingAndAudits() throws Exception {
        Shop s = shop("merge", 10);
        String a = People.customer(accounts, "merge-a");
        String b = People.customer(accounts, "merge-b");
        s.buy(a, 1);
        s.buy(b, 2);
        Customer ca = customer(s, a);
        Customer cb = customer(s, b);
        credit.grant(s.storeId, cb.getId(), 250, null, "Old record");
        long aCredit = customers.findById(ca.getId()).getStoreCreditCents();
        long bCredit = customers.findById(cb.getId()).getStoreCreditCents();
        CustomerDto merged = data.merge(s.owner, s.storeId, ca.getId(), cb.getId());
        assertNull(customers.findById(cb.getId()), "the duplicate is gone");
        assertEquals(2, orders.findByCustomerIdOrderByCreatedAtDesc(ca.getId()).size(),
                "both orders are the survivor's");
        assertEquals(aCredit + bCredit, merged.storeCreditCents());
        assertEquals(1, db.query("SELECT id FROM audit_event WHERE action = 'CUSTOMER_MERGED' "
                + "AND subject = ?", new Object[] {ca.getId()}).size());
        assertThrows(ResponseStatusException.class,
                () -> data.merge(s.owner, s.storeId, ca.getId(), ca.getId()));
        Shop other = shop("merge-other", 1);
        assertThrows(ResponseStatusException.class,
                () -> data.merge(other.owner, other.storeId, ca.getId(), "nobody"),
                "another store's customer is not found");
        assertEquals("vip,wholesale", CustomerData.joinTags("vip", "wholesale, vip"));
    }

    // ---------------------------------------------------- export, erasure

    @Test
    void exportZipAndErasureKeepFinancials() throws Exception {
        Shop s = shop("gdpr", 5);
        String user = People.customer(accounts, "gdpr-buyer");
        OrderDto o = s.buy(user, 1);
        Customer c = customer(s, user);

        DataExportDto mine = data.exportMine(user);
        String json = unzip(mine.zipBase64(), "customers.json");
        assertTrue(json.contains(c.getId()), json);
        assertNotNull(unzip(mine.zipBase64(), "account.json"));
        DataExportDto theirs = data.exportFor(s.owner, s.storeId, c.getId());
        assertTrue(unzip(theirs.zipBase64(), "customers.json").contains("\"number\""));

        DataRequestDto asked = data.request(user, s.storeId, CustomerData.ERASURE);
        assertEquals(CustomerData.OPEN, asked.status());
        assertEquals(asked.id(), data.request(user, s.storeId, CustomerData.ERASURE).id(),
                "a second request of the kind is the first");
        assertThrows(ResponseStatusException.class,
                () -> data.complete(s.owner, s.storeId, asked.id()), "erasure is done by erasing");
        assertThrows(ResponseStatusException.class,
                () -> data.erase(People.customer(accounts, "gdpr-stranger"), s.storeId, c.getId()));

        CustomerDto erased = data.erase(s.owner, s.storeId, c.getId());
        assertEquals("", erased.email());
        Customer row = customers.findById(c.getId());
        assertEquals("", row.getUsername());
        assertEquals(CustomerData.ERASED, row.getFirstName());
        com.codenameone.examples.bazaar.domain.StoreOrder kept = orders.findById(o.id());
        assertEquals("", kept.getEmail());
        assertEquals("", kept.getShipName());
        assertEquals(o.totalCents(), kept.getTotalCents(), "the amounts stay");
        List<DataRequestDto> all = data.forStore(s.owner, s.storeId);
        assertEquals(CustomerData.DONE, all.get(0).status());
    }

    private static String unzip(String base64, String name) throws Exception {
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(Base64.decode(base64)));
        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            if (name.equals(e.getName())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), "UTF-8");
            }
        }
        return null;
    }

    // -------------------------------------------------------- bulk codes

    @Test
    void bulkCodesAreUniqueSingleUseAndHidden() throws Exception {
        Shop s = shop("bulk-codes", 5);
        DiscountDto d = rates.saveDiscount(s.owner, s.storeId, new DiscountDto(null, "BULKBASE",
                "Ten off", DiscountEngine.PERCENT, 1000, 0, 0, 0, DiscountEngine.ALL, "", false, 0,
                0, false, false, 0, 0, true));
        int listed0 = rates.discounts(s.owner, s.storeId).size();
        ExportDto made = codes.generate(s.owner, s.storeId, d.id(), new DiscountCodesDto(25, "vip-"));
        assertEquals(25, made.rows().size());
        Set<String> seen = new HashSet<String>();
        for (int iter = 0; iter < made.rows().size(); iter++) {
            String code = made.rows().get(iter).cells().get(0);
            assertTrue(code.startsWith("VIP"), code);
            assertTrue(seen.add(code), "unique");
        }
        assertEquals(listed0, rates.discounts(s.owner, s.storeId).size(),
                "the generated codes are not listed as discounts");
        String first = made.rows().get(0).cells().get(0);
        String id = discounts.findByStoreIdAndCode(s.storeId, first).get(0).getId();
        assertEquals(1, discounts.useIf(id));
        assertEquals(0, discounts.useIf(id), "a generated code is good once");
        ExportDto listed = codes.codes(s.owner, s.storeId, d.id());
        assertEquals(25, listed.rows().size());
        assertThrows(ResponseStatusException.class, () -> codes.generate(s.owner, s.storeId,
                d.id(), new DiscountCodesDto(DiscountCodes.MAX_BATCH + 1, "")));
        assertThrows(ResponseStatusException.class, () -> codes.generate(s.owner, s.storeId,
                d.id(), new DiscountCodesDto(1, "bad prefix!")));
        rates.deleteDiscount(s.owner, s.storeId, d.id());
        assertTrue(discounts.findByStoreIdAndCode(s.storeId, first).isEmpty(),
                "deleting the discount deletes its codes");
    }

    // ---------------------------------------------------------- browse

    @Test
    void browseTimingRule() {
        long now = 100 * DAY;
        assertFalse(MarketingMails.shouldMail(now - 10 * 60000L, 0, now), "too soon");
        assertTrue(MarketingMails.shouldMail(now - 2 * 3600000L, 0, now));
        assertFalse(MarketingMails.shouldMail(now - 2 * DAY, 0, now), "too old");
        assertFalse(MarketingMails.shouldMail(now - 2 * 3600000L, now - DAY, now), "throttled");
        assertTrue(MarketingMails.shouldMail(now - 2 * 3600000L, now - 8 * DAY, now));
    }

    @Test
    void browseAbandonmentMailsConsentedShoppersOnce() throws Exception {
        Shop s = shop("browse", 5);
        String user = People.customer(accounts, "browse-buyer");
        String quiet = People.customer(accounts, "browse-quiet");
        s.buy(user, 1, user);
        s.buy(quiet, 1, quiet);
        // The orders came before the view, so the view was not followed by one.
        db.execute("UPDATE store_order SET created_at = ? WHERE store_id = ?",
                new Object[] {System.currentTimeMillis() - 3 * 3600000L, s.storeId});
        Customer c = customers.findById(customer(s, user).getId());
        c.setAcceptsMarketing(true);
        customers.save(c);
        Customer q = customers.findById(customer(s, quiet).getId());
        q.setAcceptsMarketing(false);
        customers.save(q);
        long now = System.currentTimeMillis();
        automations.record(s.storeId, user, s.productId, Instant.ofEpochMilli(now - 2 * 3600000L));
        automations.record(s.storeId, quiet, s.productId, Instant.ofEpochMilli(now - 2 * 3600000L));
        mail.clearSent();
        automations.sweepBrowse(now);
        Mail sent = null;
        for (Mail m : mail.sent()) {
            assertFalse(m.getTo().contains(q.getEmail()), "no mail without consent");
            if (m.getTo().contains(c.getEmail())) {
                sent = m;
            }
        }
        assertNotNull(sent, "the consented shopper is mailed");
        assertTrue(sent.getText().contains("/api/shop/m/b/"), "an unsubscribe link");
        mail.clearSent();
        automations.sweepBrowse(now + 60000L);
        for (Mail m : mail.sent()) {
            assertFalse(m.getTo().contains(c.getEmail()), "throttled");
        }
        String link = sent.getText().substring(sent.getText().indexOf("/api/shop/m/b/"));
        link = link.substring(0, link.indexOf('\n')).trim();
        MockResponse r = mvc.perform(get(link)).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        assertFalse(customers.findById(c.getId()).isAcceptsMarketing(), "unsubscribed");
        assertTrue(mvc.perform(get("/api/shop/m/b/" + Ids.next() + "?s=bad")).andReturn()
                .getResponse().getStatus() >= 400, "a forged link changes nothing");
    }

    // ------------------------------------------------------ review link

    @Test
    void reviewLinkWritesAVerifiedReview() throws Exception {
        Shop s = shop("review-link", 5);
        String user = People.customer(accounts, "review-link-buyer");
        OrderDto o = s.buy(user, 1, user);
        ReviewRequest r = new ReviewRequest();
        r.setId(Ids.next());
        r.setStoreId(s.storeId);
        r.setOrderId(o.id());
        r.setUsername(user);
        r.setEmail(user);
        r.setDueAt(Instant.ofEpochMilli(System.currentTimeMillis() - 1000));
        r.setCreatedAt(Instant.now());
        reviewRequests.insert(r);
        mail.clearSent();
        assertTrue(automations.sweepReviews(System.currentTimeMillis()) >= 1);
        String link = automations.reviewLink(r);
        String path = link.substring(link.indexOf("/review/"));
        MockResponse page = mvc.perform(get(path)).andReturn().getResponse();
        assertEquals(200, page.getStatus());
        assertTrue(page.getContentAsString().contains("Send review"), page.getContentAsString());
        assertEquals(404, mvc.perform(get("/review/" + r.getId() + "?s=forged")).andReturn()
                .getResponse().getStatus());
        assertTrue(reviewLinks.save(r, s.productId, "5", "Great", "Holds tea").startsWith("Thank"));
        com.codenameone.examples.bazaar.domain.Review w =
                reviews.findByProductIdAndUsername(s.productId, user);
        assertNotNull(w);
        assertTrue(w.isVerified());
        assertEquals("PENDING", w.getStatus());
        assertTrue(reviewLinks.save(r, s.productId, "4", "", "").startsWith("You already"));
        assertTrue(reviewLinks.save(r, "not-in-order", "4", "", "").startsWith("That product"));
    }

    // --------------------------------------------------- scheduled gifts

    @Test
    void giftCardIsDeliveredOnItsDate() throws Exception {
        Shop s = shop("gift-later", 1);
        String to = "gift-recipient@example.com";
        long when = System.currentTimeMillis() + 2 * DAY;
        mail.clearSent();
        GiftCardDto card = giftCards.issue(s.owner, s.storeId, new GiftCardDto(null, null, null,
                2500, 0, true, "", 0, "", 0, to, "Grace", "Happy birthday", when, 0));
        assertEquals(when, card.deliverAt());
        for (Mail m : mail.sent()) {
            assertFalse(m.getTo().contains(to), "not before its date");
        }
        assertEquals(0, giftCards.deliverDue(System.currentTimeMillis()));
        assertTrue(giftCards.deliverDue(when + 1000) >= 1);
        Mail gift = null;
        for (Mail m : mail.sent()) {
            if (m.getTo().contains(to)) {
                gift = m;
            }
        }
        assertNotNull(gift);
        assertTrue(gift.getText().contains("Happy birthday"), gift.getText());
        assertTrue(gift.getText().contains("Grace"), gift.getText());
        assertEquals(0, giftCards.deliverDue(when + 2000), "delivered once");
        assertThrows(ResponseStatusException.class, () -> giftCards.issue(s.owner, s.storeId,
                new GiftCardDto(null, null, null, 100, 0, true, "", 0, "", 0, "not-an-address",
                    "", "", 0, 0)));
    }
}
