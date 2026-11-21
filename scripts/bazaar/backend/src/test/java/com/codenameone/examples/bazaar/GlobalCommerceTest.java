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
import com.codenameone.examples.bazaar.api.DisputeDto;
import com.codenameone.examples.bazaar.api.DisputeEvidenceDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderRiskDto;
import com.codenameone.examples.bazaar.api.TaxExemptionDto;
import com.codenameone.examples.bazaar.api.TaxReportRowDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.dispute.Disputes;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.risk.Fraud;
import com.codenameone.examples.bazaar.risk.RiskHolds;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.taxes.TaxExemptions;
import com.codenameone.examples.bazaar.web.security.BotGuard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Wave B: markets and exchange rates, translations, tax exemptions and the
/// tax report, fraud scoring with held fulfilment, bot protection on the web
/// forms, and disputes from the processor's signed webhook -- each through
/// its service and its back-office page, with another store answering 404, a
/// post without its CSRF token 403 and a role without the right 403.
@BackendTest
class GlobalCommerceTest {
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
    private Markets markets;
    @Autowired
    private Translations translations;
    @Autowired
    private TaxExemptions taxes;
    @Autowired
    private Fraud fraud;
    @Autowired
    private RiskHolds holds;
    @Autowired
    private Disputes disputes;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private BotGuard bots;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 300, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
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

    private static int status(ThrowingRunnable r) throws Exception {
        try {
            r.run();
            return 200;
        } catch (ResponseStatusException e) {
            return e.getStatus();
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void addStaff(Shop s, String who, String role) throws Exception {
        Rest.json(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/" + s.storeId
                + "/staff"), "{\"username\":\"" + who + "\",\"role\":\"" + role
                + "\",\"pin\":\"\"}"), s.owner));
    }

    private MarketDto euro(Shop s) throws Exception {
        markets.putRate(s.owner, s.storeId, new FxRateDto("EUR", 920000));
        return markets.save(s.owner, s.storeId, null, new MarketDto(null, "eu", "Europe", "EUR",
                Arrays.asList("DE", "FR"), "de", Markets.NONE, 0, true));
    }

    // =============================================================== markets

    @Test
    void conversionIsIntegerHalfUpWithRoundingAndOverflowGuard() {
        assertEquals(920, Markets.convert(1000, 920000, 0, Markets.NONE));
        assertEquals(966, Markets.convert(1000, 920000, 500, Markets.NONE));
        assertEquals(1000, Markets.convert(1000, 920000, 0, Markets.WHOLE));
        assertEquals(999, Markets.convert(1000, 920000, 0, Markets.NINETY_NINE));
        assertEquals(1, Markets.convert(1, 500000, 0, Markets.NONE));
        assertThrows(ResponseStatusException.class,
                () -> Markets.convert(Long.MAX_VALUE / 2, 3000000, 0, Markets.NONE));
    }

    @Test
    void aMarketPricesTheProductAndIsAnAlternateOfTheDefault() throws Exception {
        Shop s = shop("gc-market");
        euro(s);
        String page = "/s/gc-market/products/gc-market-mug";
        MockResponse plain = mvc.perform(get(page).header("Host", SignIn.HOST)).andReturn()
                .getResponse();
        assertEquals(200, plain.getStatus());
        assertTrue(plain.getContentAsString().contains("Europe (EUR)"), "the picker");
        String link = plain.getHeader("Link");
        assertTrue(link.contains("hreflang=\"de\""), link);
        assertTrue(link.contains("market=eu"), link);
        MockResponse eu = mvc.perform(get(page + "?market=eu").header("Host", SignIn.HOST))
                .andReturn().getResponse();
        assertTrue(eu.getContentAsString().contains("9.20"), "converted price");
        assertTrue(eu.getContentAsString().contains("checkout charges EUR"));
        assertTrue(eu.getHeader("Link").contains("/products/gc-market-mug?market=eu>; "
                + "rel=\"canonical\""), eu.getHeader("Link"));
        assertTrue(eu.getHeader("Cache-Control").contains("public"), eu.getHeader("Cache-Control"));
        // The same market from a cookie is this visitor's alone.
        MockResponse cookie = mvc.perform(get(page).header("Host", SignIn.HOST)
                .cookie("market", "eu")).andReturn().getResponse();
        assertTrue(cookie.getContentAsString().contains("9.20"));
        assertTrue(cookie.getHeader("Cache-Control").contains("private"),
                cookie.getHeader("Cache-Control"));
        // A fixed price wins over the conversion.
        String mid = markets.list(s.owner, s.storeId).get(0).id();
        markets.putPrice(s.owner, s.storeId, mid,
                new com.codenameone.examples.bazaar.api.MarketPriceDto(null, s.variantId, 777));
        assertTrue(mvc.perform(get(page + "?market=eu").header("Host", SignIn.HOST)).andReturn()
                .getResponse().getContentAsString().contains("7.77"));
    }

    @Test
    void marketsAreStoreScopedAndManagersOnly() throws Exception {
        Shop a = shop("gc-mk-a");
        Shop b = shop("gc-mk-b");
        MarketDto m = euro(b);
        assertEquals(404, status(() -> markets.detail(a.owner, b.storeId, m.id())));
        assertEquals(404, status(() -> markets.detail(a.owner, a.storeId, m.id())));
        String packer = People.customer(accounts, "gc-mk-packer");
        addStaff(b, packer, "FULFILMENT");
        assertEquals(403, status(() -> markets.save(packer, b.storeId, null, new MarketDto(null,
                "uk", "UK", "USD", Arrays.asList("GB"), "", Markets.NONE, 0, true))));
        // A currency without a rate is refused.
        assertEquals(400, status(() -> markets.save(b.owner, b.storeId, null, new MarketDto(null,
                "jp", "Japan", "GBP", Arrays.asList("JP"), "", Markets.NONE, 0, true))));
        // Duties are estimated by the longest HS prefix.
        markets.addDuty(b.owner, b.storeId, new com.codenameone.examples.bazaar.api.DutyRateDto(
                null, "DE", "", 500));
        markets.addDuty(b.owner, b.storeId, new com.codenameone.examples.bazaar.api.DutyRateDto(
                null, "DE", "6912", 1200));
        markets.putCustomsCode(b.owner, b.storeId,
                new com.codenameone.examples.bazaar.api.CustomsCodeDto(b.productId, "6912.00"));
        List<String[]> lines = new ArrayList<String[]>();
        lines.add(new String[] {b.productId, "10000"});
        assertEquals(1200, markets.dutyEstimate(b.storeId, "DE", lines));
        assertEquals(0, markets.dutyEstimate(b.storeId, "US", lines));
    }

    @Test
    void marketsPagePostsNeedTheTokenAndAnotherStoreIs404() throws Exception {
        Shop a = shop("gc-mkweb-a");
        Shop b = shop("gc-mkweb-b");
        Browser owner = signedIn(b.owner);
        owner.assertPage(owner.get("/admin/" + b.storeId + "/markets"), "Add a market");
        redirected(owner.post("/admin/" + b.storeId + "/fx-rates", "currency", "EUR",
                "rate", "0.9200"));
        assertEquals(920000L, markets.rates(b.owner, b.storeId).get(0).rateMicros());
        redirected(owner.post("/admin/" + b.storeId + "/markets", "code", "eu", "name", "Europe",
                "countries", "DE, FR", "currency", "EUR", "locale", "de", "rounding", "WHOLE",
                "adjustBps", "0", "active", "true"));
        assertEquals(1, markets.list(b.owner, b.storeId).size());
        assertEquals(403, owner.postWithoutToken("/admin/" + b.storeId + "/fx-rates",
                "currency", "GBP", "rate", "0.8").getStatus());
        Browser other = signedIn(a.owner);
        assertEquals(404, other.get("/admin/" + b.storeId + "/markets").getStatus());
        other.get("/admin/" + a.storeId + "/markets");
        assertEquals(404, other.post("/admin/" + b.storeId + "/fx-rates", "currency", "GBP",
                "rate", "0.8").getStatus());
    }

    // ========================================================== translations

    @Test
    void translationsFallBackAndFollowTheMarketOrTheBrowser() throws Exception {
        Shop s = shop("gc-tr");
        translations.put(s.owner, s.storeId, new TranslationDto(Translations.PRODUCT,
                s.productId, "de", "title", "Becher"));
        assertEquals("Becher", translations.fields(s.storeId, Translations.PRODUCT, s.productId,
                "de-AT").get("title"));
        assertNull(translations.fields(s.storeId, Translations.PRODUCT, s.productId, "fr")
                .get("title"));
        String page = "/s/gc-tr/products/gc-tr-mug";
        MockResponse de = mvc.perform(get(page).header("Host", SignIn.HOST)
                .header("Accept-Language", "fr;q=0.5, de-CH")).andReturn().getResponse();
        assertTrue(de.getContentAsString().contains("Becher"));
        assertTrue(de.getHeader("Vary").contains("Accept-Language"), de.getHeader("Vary"));
        MockResponse en = mvc.perform(get(page).header("Host", SignIn.HOST)).andReturn()
                .getResponse();
        assertTrue(!en.getContentAsString().contains("Becher"));
        // Another store's product cannot be translated.
        Shop o = shop("gc-tr-other");
        assertEquals(404, status(() -> translations.put(o.owner, o.storeId, new TranslationDto(
                Translations.PRODUCT, s.productId, "de", "title", "X"))));
        // The editor shows the original beside the field, and saves.
        Browser owner = signedIn(s.owner);
        owner.assertPage(owner.get("/admin/" + s.storeId + "/translations?kind=PRODUCT&refId="
                + s.productId + "&locale=de"), "Original: Mug");
        redirected(owner.post("/admin/" + s.storeId + "/translations", "kind", "PRODUCT",
                "refId", s.productId, "locale", "de", "title", "Tasse", "description", "",
                "seoTitle", "", "seoDescription", ""));
        assertEquals("Tasse", translations.fields(s.storeId, Translations.PRODUCT, s.productId,
                "de").get("title"));
        assertEquals(403, owner.postWithoutToken("/admin/" + s.storeId + "/translations",
                "kind", "PRODUCT", "refId", s.productId, "locale", "de", "title", "Y")
                .getStatus());
    }

    // ================================================================= taxes

    @Test
    void anExemptionTakesTheTaxOffAndTheReportCountsIt() throws Exception {
        Shop s = shop("gc-tax");
        String exempt = People.customer(accounts, "gc-tax-exempt");
        String plain = People.customer(accounts, "gc-tax-plain");
        taxes.add(s.owner, s.storeId, new TaxExemptionDto(null, exempt, "CERT-NY-1",
                Arrays.asList("US-NY"), 0));
        OrderDto taxed = s.buy(plain, 1);
        OrderDto free = s.buy(exempt, 1);
        assertTrue(taxed.taxCents() > 0, "the demo store taxes New York");
        assertEquals(0, free.taxCents());
        assertEquals("CERT-NY-1", taxes.certificateOf(free.id()));
        List<TaxReportRowDto> rows = taxes.report(s.owner, s.storeId, Days.iso(System.currentTimeMillis() - Days.MILLIS), Days.iso(System.currentTimeMillis() + Days.MILLIS));
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).orders());
        assertEquals(1, rows.get(0).exemptOrders());
        assertEquals(taxed.taxCents(), rows.get(0).taxCents());
        // Another region is not covered.
        taxes.add(s.owner, s.storeId, new TaxExemptionDto(null, plain, "CERT-CA", Arrays.asList(
                "US-CA"), 0));
        assertNull(taxes.certificateFor(s.storeId, plain, "US", "NY", java.time.Instant.now()));
        // The page and its CSV; another store's owner sees neither.
        Browser owner = signedIn(s.owner);
        owner.assertPage(owner.get("/admin/" + s.storeId + "/tax"), "CERT-NY-1");
        MockResponse csv = owner.get("/admin/" + s.storeId + "/tax/report.csv");
        assertEquals(200, csv.getStatus());
        assertTrue(csv.getContentAsString().startsWith("country,region,orders"));
        assertTrue(csv.getContentAsString().contains("US,NY,2,1,"), csv.getContentAsString());
        Shop o = shop("gc-tax-other");
        Browser other = signedIn(o.owner);
        assertEquals(404, other.get("/admin/" + s.storeId + "/tax").getStatus());
        assertEquals(404, other.get("/admin/" + s.storeId + "/tax/report.csv").getStatus());
        String packer = People.customer(accounts, "gc-tax-packer");
        addStaff(s, packer, "FULFILMENT");
        assertEquals(403, status(() -> taxes.report(packer, s.storeId, "2026-01-01", "2026-01-02")));
    }

    // ================================================================== risk

    @Test
    void scoringRulesAreAdditive() throws Exception {
        com.codenameone.examples.bazaar.domain.StoreOrder o =
                new com.codenameone.examples.bazaar.domain.StoreOrder();
        o.setId("o1");
        o.setTotalCents(250000);
        o.setShipCountry("US");
        Fraud.Score s = Fraud.score(o, new ArrayList<com.codenameone.examples.bazaar.domain
                .StoreOrder>(), 3, "RU", System.currentTimeMillis());
        assertEquals(40 + 30 + 25, s.points);
        assertEquals(Fraud.HIGH, s.level());
        o.setTotalCents(1000);
        assertEquals(Fraud.LOW, Fraud.score(o, new ArrayList<com.codenameone.examples.bazaar
                .domain.StoreOrder>(), 0, null, 0).level());
    }

    @Test
    void aHighRiskOrderIsHeldFromFulfilmentUntilApproved() throws Exception {
        Shop s = shop("gc-risk");
        String buyer = People.customer(accounts, "gc-risk-buyer");
        OrderDto normal = s.buy(buyer, 1);
        OrderRiskDto low = fraud.detail(s.owner, s.storeId, normal.id());
        assertEquals(Fraud.CLEAR, low.status());
        holds.recordFailure(s.storeId, buyer);
        holds.recordFailure(s.storeId, buyer);
        holds.recordFailure(s.storeId, buyer);
        OrderDto big = s.buy(buyer, 200);
        OrderRiskDto held = fraud.detail(s.owner, s.storeId, big.id());
        assertEquals(Fraud.HELD, held.status(), held.reasons().toString());
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(big.lines().get(0).id(), null, null, 200));
        assertEquals(409, status(() -> fulfillments.create(s.owner, s.storeId, big.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls))));
        // Another store's owner cannot see or release it; a packer cannot approve.
        Shop o = shop("gc-risk-other");
        assertEquals(404, status(() -> fraud.approve(o.owner, o.storeId, big.id())));
        assertEquals(404, status(() -> fraud.approve(o.owner, s.storeId, big.id())));
        String packer = People.customer(accounts, "gc-risk-packer");
        addStaff(s, packer, "FULFILMENT");
        assertEquals(403, status(() -> fraud.approve(packer, s.storeId, big.id())));
        // The review page, its CSRF, and approval through it.
        Browser owner = signedIn(s.owner);
        owner.assertPage(owner.get("/admin/" + s.storeId + "/risk"), "Held for review");
        assertEquals(403, owner.postWithoutToken("/admin/" + s.storeId + "/risk/" + big.id()
                + "/approve").getStatus());
        redirected(owner.post("/admin/" + s.storeId + "/risk/" + big.id() + "/approve"));
        assertEquals(Fraud.APPROVED, fraud.detail(s.owner, s.storeId, big.id()).status());
        assertNotNull(fulfillments.create(s.owner, s.storeId, big.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls)).id());
    }

    // ========================================================== bot defence

    @Test
    void aBotLikeSignInIsRefused() throws Exception {
        String user = People.customer(accounts, "gc-bot-user");
        Browser b = new Browser(mvc);
        b.get("/auth/signin");
        assertNotNull(b.fill, "the page carries a timing token");
        assertEquals(400, b.post("/auth/signin", "username", user, "password", People.PASSWORD,
                "website", "http://spam.example").getStatus());
        String token = b.fill;
        b.fill = "";
        assertEquals(400, b.post("/auth/signin", "username", user, "password", People.PASSWORD)
                .getStatus());
        b.fill = "1." + token.substring(token.indexOf('.') + 1);
        assertEquals(400, b.post("/auth/signin", "username", user, "password", People.PASSWORD)
                .getStatus());
        bots.setMinFillMillis(60000);
        try {
            b.get("/auth/signin");
            assertEquals(400, b.post("/auth/signin", "username", user, "password",
                    People.PASSWORD).getStatus());
        } finally {
            bots.setMinFillMillis(0);
        }
        b.get("/auth/signin");
        redirected(b.post("/auth/signin", "username", user, "password", People.PASSWORD));
    }

    @Test
    void theCheckoutFormCarriesTheGuard() throws Exception {
        Shop s = shop("gc-botco");
        String user = "gc-botco-guest";
        Browser b = new Browser(mvc);
        b.send(Browser.form("/s/gc-botco/cart/add", "variantId", s.variantId, "quantity", "1"));
        MockResponse page = b.get("/s/gc-botco/checkout");
        b.assertPage(page, "name=\"_ft\"");
        b.assertPage(page, "class=\"hp\"");
        b.fill = "";
        assertEquals(400, b.post("/s/gc-botco/checkout/place", "email", user + "@example.com")
                .getStatus());
    }

    // ============================================================== disputes

    private MockResponse webhook(String body, String secret) throws Exception {
        return mvc.perform(Rest.body(post("/api/payments/webhooks/disputes"), body)
                .header("X-Bazaar-Signature", "sha256=" + Tokens.sign(secret, body)))
                .andReturn().getResponse();
    }

    @Test
    void aSignedDisputeEventOpensOneOnceAndHoldsTheFunds() throws Exception {
        Shop s = shop("gc-dsp");
        OrderDto o = s.buy(People.customer(accounts, "gc-dsp-buyer"), 2);
        String event = "{\"id\":\"evt-gc-1\",\"type\":\"dispute.created\",\"orderId\":\""
                + o.id() + "\",\"amountCents\":1500,\"reason\":\"fraudulent\"}";
        assertEquals(401, webhook(event, "wrong-secret").getStatus());
        assertEquals(200, webhook(event, "test-dispute-secret").getStatus());
        assertEquals(200, webhook(event, "test-dispute-secret").getStatus());
        List<DisputeDto> all = disputes.list(s.owner, s.storeId);
        assertEquals(1, all.size());
        DisputeDto d = all.get(0);
        assertEquals(Disputes.NEEDS_RESPONSE, d.status());
        assertEquals(1500, disputes.heldCents(s.storeId));
        assertTrue(d.dueAt() > System.currentTimeMillis());
        // Submitting needs evidence; the order record is written by the server.
        assertEquals(400, status(() -> disputes.submit(s.owner, s.storeId, d.id())));
        List<DisputeEvidenceDto> ev = disputes.addEvidence(s.owner, s.storeId, d.id(),
                new DisputeEvidenceDto(null, Disputes.ORDER, "ignored", null, 0));
        assertTrue(ev.get(0).body().contains("Order #" + o.number()), ev.get(0).body());
        // Scoping and roles.
        Shop other = shop("gc-dsp-other");
        assertEquals(404, status(() -> disputes.detail(other.owner, s.storeId, d.id())));
        assertEquals(404, status(() -> disputes.detail(other.owner, other.storeId, d.id())));
        String packer = People.customer(accounts, "gc-dsp-packer");
        addStaff(s, packer, "FULFILMENT");
        assertEquals(403, status(() -> disputes.submit(packer, s.storeId, d.id())));
        // The page: funds held, CSRF, submit.
        Browser owner = signedIn(s.owner);
        owner.assertPage(owner.get("/admin/" + s.storeId + "/disputes"), "Funds held");
        owner.assertPage(owner.get("/admin/" + s.storeId + "/disputes/" + d.id()), "Evidence");
        assertEquals(403, owner.postWithoutToken("/admin/" + s.storeId + "/disputes/" + d.id()
                + "/submit").getStatus());
        redirected(owner.post("/admin/" + s.storeId + "/disputes/" + d.id() + "/submit"));
        assertEquals(Disputes.UNDER_REVIEW, disputes.detail(s.owner, s.storeId, d.id()).status());
        assertEquals(1500, disputes.heldCents(s.storeId));
        String closed = "{\"id\":\"evt-gc-2\",\"type\":\"dispute.closed\",\"disputeEventId\":"
                + "\"evt-gc-1\",\"outcome\":\"won\"}";
        assertEquals(200, webhook(closed, "test-dispute-secret").getStatus());
        assertEquals(Disputes.WON, disputes.detail(s.owner, s.storeId, d.id()).status());
        assertEquals(0, disputes.heldCents(s.storeId));
        assertEquals(404, signedIn(other.owner).get("/admin/" + s.storeId + "/disputes")
                .getStatus());
    }

    @Test
    void evidenceIsRefusedPastTheDeadline() throws Exception {
        Shop s = shop("gc-dsp-late");
        OrderDto o = s.buy(People.customer(accounts, "gc-dsp-late-buyer"), 1);
        assertEquals(200, webhook("{\"id\":\"evt-late\",\"type\":\"dispute.created\","
                + "\"orderId\":\"" + o.id() + "\"}", "test-dispute-secret").getStatus());
        DisputeDto d = disputes.list(s.owner, s.storeId).get(0);
        disputes.addEvidence(s.owner, s.storeId, d.id(), new DisputeEvidenceDto(null,
                Disputes.TEXT, "Delivered and signed for.", null, 0));
        db.execute("UPDATE dispute SET due_at = 1 WHERE id = ?", new Object[] {d.id()});
        assertEquals(409, status(() -> disputes.submit(s.owner, s.storeId, d.id())));
    }
}
