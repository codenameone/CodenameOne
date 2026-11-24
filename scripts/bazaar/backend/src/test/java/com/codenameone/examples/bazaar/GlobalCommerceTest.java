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
        markets.putRate(s.owner, s.storeId, new FxRateDto("EUR", 920000, 0, "", 0));
        return markets.save(s.owner, s.storeId, null, new MarketDto(null, "eu", "Europe", "EUR",
                Arrays.asList("DE", "FR"), "de", Markets.NONE, 0, true, Markets.MANUAL));
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
    void choosingAMarketKeepsItThroughTheCookie() throws Exception {
        Shop s = shop("gc-pick");
        euro(s);
        String page = "/s/gc-pick/products/gc-pick-mug";
        Browser b = new Browser(mvc);
        String shown = body(b.get(page));
        assertTrue(shown.contains("action=\"/market\"") && shown.contains("value=\"eu\""),
                "the picker is a form that works without script");
        // Choosing: back to the page, without a query market that would outrank it.
        MockResponse chose = b.send(Browser.form("/market", "market", "eu", "back",
                page + "?market=xx&variant=1"));
        assertEquals(303, chose.getStatus());
        assertEquals(page + "?variant=1", chose.getHeader("Location"));
        assertTrue(chose.getHeader("Set-Cookie").startsWith("market=eu;"), chose.getHeader("Set-Cookie"));
        assertEquals("/", b.send(Browser.form("/market", "market", "eu", "back", "//evil.example"))
                .getHeader("Location"), "never an open redirect");
        MockResponse foreign = b.send(Browser.form("/market", "market", "eu", "back", page)
                .header("Sec-Fetch-Site", "cross-site"));
        assertEquals(403, foreign.getStatus(), "refused from another site");
        // The cookie prices every later page; that copy is the shopper's alone.
        b.market = "eu";
        MockResponse kept = b.get(page);
        assertTrue(body(kept).contains("9.20"), "converted without a query");
        assertTrue(kept.getHeader("Cache-Control").startsWith("private"), kept.getHeader("Cache-Control"));
        // The store's own prices forget the choice; the shared copy varies by Cookie.
        MockResponse cleared = b.send(Browser.form("/market", "market", "", "back", page));
        assertTrue(cleared.getHeader("Set-Cookie").startsWith("market=;")
                && cleared.getHeader("Set-Cookie").contains("Max-Age=0"), cleared.getHeader("Set-Cookie"));
        b.market = null;
        MockResponse plain = b.get(page);
        assertTrue(plain.getHeader("Cache-Control").startsWith("public"), plain.getHeader("Cache-Control"));
        assertTrue(plain.getHeader("Vary").contains("Cookie"), plain.getHeader("Vary"));
        assertNull(plain.getHeader("Set-Cookie"), "a shared page sets no cookie");
    }

    private static String body(MockResponse r) throws Exception {
        return r.getContentAsString();
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
                "uk", "UK", "USD", Arrays.asList("GB"), "", Markets.NONE, 0, true, Markets.MANUAL))));
        // A currency without a rate is refused.
        assertEquals(400, status(() -> markets.save(b.owner, b.storeId, null, new MarketDto(null,
                "jp", "Japan", "GBP", Arrays.asList("JP"), "", Markets.NONE, 0, true, Markets.MANUAL))));
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

    // ========================================================== translations

    // ================================================================= taxes

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
