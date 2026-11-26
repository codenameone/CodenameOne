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
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.CustomDomainDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderRiskDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.risk.Fraud;
import com.codenameone.examples.bazaar.risk.RiskProvider;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.taxes.ReverseCharge;
import com.codenameone.examples.bazaar.taxes.TaxExemptions;
import com.codenameone.examples.bazaar.trade.Trade;
import com.codenameone.examples.bazaar.web.DomainVerification;
import com.codenameone.examples.bazaar.web.SiteUrls;
import com.codenameone.examples.bazaar.web.security.FormKeys;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a store needs before taking real orders: domain ownership, shared
/// form keys, the EU reverse charge and an outside risk provider.
@BackendTest
class ProductReadinessTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private Fraud fraud;
    @Autowired
    private DomainVerification domains;
    @Autowired
    private Trade trade;
    @Autowired
    private TaxExemptions exemptions;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// A probe that finds what it was told is published.
    static final class FakeProbe implements DomainVerification.Probe {
        final List<String> files = new ArrayList<String>();
        final List<String> records = new ArrayList<String>();
        String body = "";

        @Override
        public String fetch(String url) {
            return files.contains(url) ? body : null;
        }

        @Override
        public List<String> txt(String name) {
            List<String> out = new ArrayList<String>();
            for (String r : records) {
                if (r.startsWith(name + " ")) {
                    out.add(r.substring(name.length() + 1));
                }
            }
            return out;
        }
    }

    // ============================================================ domains

    @Test
    void aClaimedDomainIsServedOnlyOnceItsTokenIsFound() throws Exception {
        Shop s = shop("pr-dom");
        FakeProbe probe = new FakeProbe();
        domains.useProbe(probe);
        CustomDomainDto claimed = domains.claim(s.owner, s.storeId, "Shop.PR-Dom.example");
        assertEquals("shop.pr-dom.example", claimed.domain());
        assertFalse(claimed.verified());
        assertEquals(32, claimed.token().length());
        assertNull(SiteUrls.served(storeRows.findById(s.storeId)));
        assertFalse(domains.verify(s.owner, s.storeId).verified());
        // The wrong body at the right address proves nothing.
        probe.files.add(claimed.fileUrl());
        probe.body = "something else";
        assertFalse(domains.verify(s.owner, s.storeId).verified());
        probe.body = claimed.token() + "\n";
        CustomDomainDto ok = domains.verify(s.owner, s.storeId);
        assertTrue(ok.verified(), ok.message());
        assertEquals("shop.pr-dom.example", SiteUrls.served(storeRows.findById(s.storeId)));
        // A new domain starts over with a new token.
        CustomDomainDto next = domains.claim(s.owner, s.storeId, "www.pr-dom.example");
        assertFalse(next.verified());
        assertNotEquals(claimed.token(), next.token());
        assertNull(SiteUrls.served(storeRows.findById(s.storeId)));
        probe.records.add(next.txtName() + " " + next.txtValue());
        assertTrue(domains.verify(s.owner, s.storeId).verified());
    }

    @Test
    void anUnprovedClaimCannotSquatAndAProvedOneCannotBeTaken() throws Exception {
        Shop owner = shop("pr-own");
        Shop squatter = shop("pr-squat");
        FakeProbe probe = new FakeProbe();
        domains.useProbe(probe);
        domains.claim(squatter.owner, squatter.storeId, "shop.pr-own.example");
        CustomDomainDto mine = domains.claim(owner.owner, owner.storeId, "shop.pr-own.example");
        assertEquals("", domains.status(squatter.owner, squatter.storeId).domain());
        probe.records.add(mine.txtName() + " " + mine.txtValue());
        assertTrue(domains.verify(owner.owner, owner.storeId).verified());
        try {
            domains.claim(squatter.owner, squatter.storeId, "shop.pr-own.example");
            throw new AssertionError("taken");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
    }

    @Test
    void nothingPublishedIsNotProof() {
        assertNull(DomainVerification.found(new FakeProbe(), "x.example", "abc"));
        assertNull(DomainVerification.found(new FakeProbe(), "x.example", ""));
    }

    // =========================================================== form keys

    @Test
    void formKeysAreSharedFromConfigAndSeparatePerUse() throws Exception {
        String secret = "0123456789abcdef0123456789abcdef";
        FormKeys a = new FormKeys(secret);
        FormKeys b = new FormKeys(secret);
        assertEquals(a.key(FormKeys.BOT_GUARD), b.key(FormKeys.BOT_GUARD));
        assertNotEquals(a.key(FormKeys.BOT_GUARD), a.key(FormKeys.THEME_PREVIEW));
        assertNull(FormKeys.problem(secret, false, "prod"));
        assertTrue(FormKeys.problem("", true, "dev").startsWith("warning:"));
        assertTrue(FormKeys.problem("short", false, "prod").startsWith("error:"));
    }

    // ===================================================== reverse charge

    @Test
    void reverseChargeIsCrossBorderEuBusinessOnly() {
        assertTrue(ReverseCharge.applies("FR", "DE123456789", "DE"));
        assertTrue(ReverseCharge.applies("FR", "EL123456789", "GR"));
        assertFalse(ReverseCharge.applies("FR", "FR40303265045", "FR"));
        assertFalse(ReverseCharge.applies("FR", "DE123456789", "FR"));
        assertFalse(ReverseCharge.applies("US", "DE123456789", "DE"));
        assertFalse(ReverseCharge.applies("FR", "DE123456789", "US"));
        assertFalse(ReverseCharge.applies("", "DE123456789", "DE"));
    }

    @Test
    void aVerifiedForeignCompanyBuysWithoutVat() throws Exception {
        Shop s = shop("pr-rc");
        db.execute("INSERT INTO tax_settings (store_id, eu_vat, home_country, us_provider, "
                + "updated_at) VALUES (?, ?, ?, ?, ?)", new Object[] {s.storeId, 0, "FR", "",
                System.currentTimeMillis()});
        CompanyDto c = trade.writeCompany(s.storeId, null, new CompanyDto(null, "Acme GmbH",
                "DE123456789", 0, ""));
        String buyer = People.customer(accounts, "pr-rc-buyer");
        trade.addBuyer(s.owner, s.storeId, c.id(), new CompanyBuyerDto(null, buyer, ""));
        Instant now = Instant.now();
        // Checked on its format alone, the number does not qualify.
        assertNull(exemptions.certificateFor(s.storeId, buyer, "DE", "", now));
        db.execute("UPDATE company SET vat_status = ? WHERE id = ?",
                new Object[] {"verified", c.id()});
        String cert = exemptions.certificateFor(s.storeId, buyer, "DE", "", now);
        assertEquals(ReverseCharge.PREFIX + "DE123456789", cert);
        assertEquals("DE123456789", ReverseCharge.vatIdOf(cert));
        assertNull(exemptions.certificateFor(s.storeId, buyer, "FR", "", now));
        assertNull(exemptions.certificateFor(s.storeId,
                People.customer(accounts, "pr-rc-other"), "DE", "", now));
        assertNull(ReverseCharge.vatIdOf("CERT-1"));
    }

    // ============================================================== risk

    private OrderDto place(Shop s, String user) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        ShippingOptionDto rate = c.shippingOptions().get(0);
        String card = (String) Rest.addCard(mvc, user, Rest.VISA).get("id");
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                rate.rateId(), card, user + "@example.com", "", null, 0, null),
                null, null, null, null);
    }

    @Test
    void aHighOutsideScoreHoldsTheOrderAndAnOutageDoesNot() throws Exception {
        Shop s = shop("pr-risk");
        try {
            fraud.useProvider(new RiskProvider() {
                @Override
                public String name() {
                    return "fake";
                }

                @Override
                public Signal assess(StoreOrder order, String clientCountry) {
                    return new Signal(70).reason("card seen in chargebacks");
                }
            });
            OrderDto held = place(s, People.customer(accounts, "pr-risk-a"));
            OrderRiskDto r = fraud.detail(s.owner, s.storeId, held.id());
            assertEquals(Fraud.HELD, r.status());
            assertTrue(r.reasons().toString().contains("card seen in chargebacks"),
                    r.reasons().toString());
            fraud.useProvider(new RiskProvider() {
                @Override
                public String name() {
                    return "down";
                }

                @Override
                public Signal assess(StoreOrder order, String clientCountry) throws IOException {
                    throw new IOException("timeout");
                }
            });
            OrderDto clear = place(s, People.customer(accounts, "pr-risk-b"));
            OrderRiskDto r2 = fraud.detail(s.owner, s.storeId, clear.id());
            assertEquals(Fraud.CLEAR, r2.status());
            assertTrue(r2.reasons().toString().contains("not reached"), r2.reasons().toString());
        } finally {
            fraud.useProvider(RiskProvider.NONE);
        }
    }
}
