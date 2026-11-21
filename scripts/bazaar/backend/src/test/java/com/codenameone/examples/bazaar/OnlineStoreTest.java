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
import com.codenameone.examples.bazaar.api.BlogPostDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.ContentPageDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.RedirectDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.api.TaxExemptionDto;
import com.codenameone.examples.bazaar.api.ThemeDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.content.Navigation;
import com.codenameone.examples.bazaar.content.Pages;
import com.codenameone.examples.bazaar.content.RichText;
import com.codenameone.examples.bazaar.content.Themes;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.taxes.TaxExemptions;
import com.codenameone.examples.bazaar.web.security.BotGuard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The online store: themes and their signed preview, sanitized pages and the
/// blog with its feeds, menus, redirects, metafields, product feeds per
/// market, checkout in the shopper's currency with refunds in it, the web
/// sign-up behind the bot guard and tax exemptions in a tax-inclusive store
/// -- each with another store answering 404, a post without its CSRF token
/// 403 and a role without the right 403.
@BackendTest
class OnlineStoreTest {
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
    private Themes themes;
    @Autowired
    private Pages pages;
    @Autowired
    private Navigation navigation;
    @Autowired
    private Orders orders;
    @Autowired
    private TaxExemptions taxes;
    @Autowired
    private StoreRepository storeRows;
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

    private MockResponse visit(String path) throws Exception {
        return mvc.perform(get(path).header("Host", SignIn.HOST)).andReturn().getResponse();
    }

    private static void redirected(MockResponse r) throws Exception {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303,
                "answered " + r.getStatus() + " " + r.getContentAsString());
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static int status(ThrowingRunnable r) throws Exception {
        try {
            r.run();
            return 200;
        } catch (ResponseStatusException e) {
            return e.getStatus();
        }
    }

    private String packer(Shop s, String name) throws Exception {
        String who = People.customer(accounts, name);
        Rest.json(mvc, People.asMerchant(Rest.body(com.codename1.backend.test
                .MockMvcRequestBuilders.post("/api/merchant/stores/" + s.storeId + "/staff"),
                "{\"username\":\"" + who + "\",\"role\":\"FULFILMENT\",\"pin\":\"\"}"), s.owner));
        return who;
    }

    private void euro(Shop s) throws Exception {
        markets.putRate(s.owner, s.storeId, new FxRateDto("EUR", 920000, 0, "", 0));
        markets.save(s.owner, s.storeId, null, new MarketDto(null, "eu", "Europe", "EUR",
                Arrays.asList("DE", "FR"), "de", Markets.NONE, 0, true, Markets.MANUAL));
    }

    // ============================================================ rich text

    @Test
    void theSanitizerDropsScriptsHandlersAndJavascriptUrls() {
        String out = RichText.sanitize("<p onclick=\"steal()\">Hi <b>there</b>"
                + "<script>alert(1)</script><a href=\"javascript:alert(2)\">x</a>"
                + "<a href=\" JaVaScRiPt:alert(3)\">y</a><a href=\"/s/ok\">ok</a></p>"
                + "<img src=x onerror=alert(4)><style>p{}</style><iframe src=//evil></iframe>");
        String lower = out.toLowerCase(java.util.Locale.ROOT);
        assertFalse(lower.contains("script"), out);
        assertFalse(lower.contains("onclick"), out);
        assertFalse(lower.contains("onerror"), out);
        assertFalse(lower.contains("javascript:"), out);
        assertFalse(lower.contains("iframe"), out);
        assertFalse(lower.contains("alert(1)"), out);
        assertTrue(out.contains("<strong>there</strong>"), out);
        assertTrue(out.contains("href=\"/s/ok\""), out);
        assertEquals(null, RichText.safeHref("//evil.example"));
        assertEquals(null, RichText.safeHref("javascript:x"));
    }

    // ===================================================== pages and themes

    @Test
    void aPublishedPageIsCachedWithTheStrictPolicyAndThePreviewIsNot() throws Exception {
        Shop s = shop("os-page");
        pages.save(s.owner, s.storeId, null, new ContentPageDto(null, Pages.PAGE, "about",
                "About us", "<p>We <i>make</i> mugs.<script>x()</script></p>", true, 0));
        MockResponse page = visit("/s/os-page/pages/about");
        assertEquals(200, page.getStatus());
        assertTrue(page.getContentAsString().contains("About us"));
        assertFalse(page.getContentAsString().contains("x()"));
        assertTrue(page.getHeader("Cache-Control").contains("public"),
                page.getHeader("Cache-Control"));
        String csp = page.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        // Cached for everyone, so no per-response nonce; nothing inline either.
        assertFalse(csp.contains("nonce-"), csp);
        assertFalse(csp.contains("unsafe-inline"), csp);
        assertTrue(csp.contains("script-src"), csp);
        assertEquals(404, visit("/s/os-page/pages/nope").getStatus());
        // A policy page.
        pages.save(s.owner, s.storeId, null, new ContentPageDto(null, Pages.POLICY, "refund",
                "Refunds", "<p>30 days.</p>", true, 0));
        assertEquals(200, visit("/s/os-page/policies/refund").getStatus());
        // The theme: a draft is not live; the signed preview shows it, never cached.
        themes.saveDraft(s.owner, s.storeId, new ThemeDto(Themes.DRAFT, "#aa3300", "dark",
                "serif", "round", "", Arrays.asList("featured", "hero")));
        assertFalse(visit("/s/os-page").getContentAsString().contains("th-dark"));
        String token = themes.previewToken(s.owner, s.storeId);
        MockResponse preview = visit("/s/os-page?preview=" + token);
        assertTrue(preview.getContentAsString().contains("th-dark"));
        assertTrue(preview.getHeader("Cache-Control").contains("no-store"),
                preview.getHeader("Cache-Control"));
        assertFalse(visit("/s/os-page?preview=" + token.substring(0, token.length() - 2) + "xx")
                .getContentAsString().contains("th-dark"), "a forged token is ignored");
        themes.publish(s.owner, s.storeId);
        MockResponse live = visit("/s/os-page");
        assertTrue(live.getContentAsString().contains("th-dark font-serif radius-round"));
        assertFalse(live.getContentAsString().contains("style="), "no inline styles");
        // Roles and stores.
        String packer = packer(s, "os-page-packer");
        assertEquals(403, status(() -> themes.saveDraft(packer, s.storeId, new ThemeDto(
                Themes.DRAFT, "#000000", "light", "system", "soft", "", null))));
        Shop o = shop("os-page-other");
        assertEquals(404, status(() -> pages.list(o.owner, s.storeId)));
    }

    @Test
    void theAdminPagesNeedTheTokenAndAnotherStoreIs404() throws Exception {
        Shop a = shop("os-admin-a");
        Shop b = shop("os-admin-b");
        Browser owner = signedIn(b.owner);
        owner.assertPage(owner.get("/admin/" + b.storeId + "/online-store"), "Draft theme");
        redirected(owner.post("/admin/" + b.storeId + "/online-store/pages", "kind", "PAGE",
                "handle", "faq", "title", "FAQ", "body", "<p>Ask</p>", "published", "true"));
        assertEquals(1, pages.list(b.owner, b.storeId).size());
        redirected(owner.post("/admin/" + b.storeId + "/online-store/menus", "menu", "main",
                "parentId", "", "title", "Home", "url", "/s/os-admin-b"));
        redirected(owner.post("/admin/" + b.storeId + "/online-store/menus", "menu", "main",
                "parentId", "", "title", "FAQ", "url", "/s/os-admin-b/pages/faq"));
        String second = navigation.links(b.owner, b.storeId).get(1).id();
        redirected(owner.post("/admin/" + b.storeId + "/online-store/menus/" + second + "/up"));
        assertEquals("FAQ", navigation.links(b.owner, b.storeId).get(0).title());
        String[] subs = {"", "/pages", "/blog", "/menus", "/redirects", "/metafields",
            "/metaobjects"};
        for (int iter = 0; iter < subs.length; iter++) {
            assertEquals(200, owner.get("/admin/" + b.storeId + "/online-store" + subs[iter])
                    .getStatus(), subs[iter]);
        }
        assertEquals(403, owner.postWithoutToken("/admin/" + b.storeId
                + "/online-store/redirects", "fromPath", "/s/os-admin-b/a", "toPath", "/")
                .getStatus());
        Browser other = signedIn(a.owner);
        assertEquals(404, other.get("/admin/" + b.storeId + "/online-store").getStatus());
        other.get("/admin/" + a.storeId + "/online-store");
        assertEquals(404, other.post("/admin/" + b.storeId + "/online-store/theme/publish")
                .getStatus());
        String packer = packer(b, "os-admin-packer");
        Browser p = signedIn(packer);
        p.get("/admin/" + b.storeId + "/online-store");
        assertEquals(403, p.post("/admin/" + b.storeId + "/online-store/theme/publish")
                .getStatus());
    }

    // ============================================================ redirects

    @Test
    void anOldAddressAnswers301AndALiveOneIsNeverRedirected() throws Exception {
        Shop s = shop("os-redir");
        navigation.addRedirect(s.owner, s.storeId, new RedirectDto(null, "/s/os-redir/old-mug",
                "/s/os-redir/products/os-redir-mug", 0));
        MockResponse r = visit("/s/os-redir/old-mug");
        assertEquals(301, r.getStatus());
        assertEquals("/s/os-redir/products/os-redir-mug", r.getHeader("Location"));
        assertEquals(1, navigation.redirects(s.owner, s.storeId).get(0).hits());
        // A redirect from an address that exists never takes it over.
        navigation.addRedirect(s.owner, s.storeId, new RedirectDto(null,
                "/s/os-redir/blog", "/s/os-redir", 0));
        assertEquals(200, visit("/s/os-redir/blog").getStatus());
        // A chain is refused: nothing may redirect to an address that redirects.
        assertEquals(400, status(() -> navigation.addRedirect(s.owner, s.storeId,
                new RedirectDto(null, "/s/os-redir/older", "/s/os-redir/old-mug", 0))));
        // Another store's paths and loops are refused.
        assertEquals(400, status(() -> navigation.addRedirect(s.owner, s.storeId,
                new RedirectDto(null, "/s/elsewhere/x", "/", 0))));
        assertEquals(400, status(() -> navigation.addRedirect(s.owner, s.storeId,
                new RedirectDto(null, "/s/os-redir/loop", "/s/os-redir/loop", 0))));
        String packer = packer(s, "os-redir-packer");
        assertEquals(403, status(() -> navigation.addRedirect(packer, s.storeId,
                new RedirectDto(null, "/s/os-redir/p", "/", 0))));
    }

    // ================================================================ feeds

    @Test
    void theProductFeedIsInTheMarketCurrencyWithAnEtag() throws Exception {
        Shop s = shop("os-feed");
        euro(s);
        MockResponse usd = visit("/s/os-feed/feeds/default/products.xml");
        assertEquals(200, usd.getStatus());
        assertTrue(usd.getContentAsString().contains("USD"), usd.getContentAsString());
        MockResponse eur = visit("/s/os-feed/feeds/eu/products.xml");
        assertEquals(200, eur.getStatus());
        String xml = eur.getContentAsString();
        assertTrue(xml.contains(" EUR"), xml);
        assertFalse(xml.contains(" USD"), xml);
        assertTrue(xml.contains("os-feed-mug"), xml);
        String etag = eur.getHeader("ETag");
        assertNotNull(etag);
        assertEquals(304, mvc.perform(get("/s/os-feed/feeds/eu/products.xml")
                .header("Host", SignIn.HOST).header("If-None-Match", etag)).andReturn()
                .getResponse().getStatus());
        MockResponse csv = visit("/s/os-feed/feeds/eu/products.csv");
        assertEquals(200, csv.getStatus());
        assertTrue(csv.getContentAsString().startsWith("id,"), csv.getContentAsString());
        assertTrue(csv.getContentAsString().contains("EUR"));
        assertEquals(404, visit("/s/os-feed/feeds/nowhere/products.xml").getStatus());
        // The blog feeds carry published posts only.
        pages.savePost(s.owner, s.storeId, null, new BlogPostDto(null, "hello", "Hello world",
                "Ada", Arrays.asList("news"), "First", "<p>Body</p>", true, 0));
        pages.savePost(s.owner, s.storeId, null, new BlogPostDto(null, "draft", "Secret draft",
                "Ada", null, "", "<p>x</p>", false, 0));
        String atom = visit("/s/os-feed/feeds/blog.atom").getContentAsString();
        assertTrue(atom.contains("Hello world"), atom);
        assertFalse(atom.contains("Secret draft"), atom);
        assertTrue(visit("/s/os-feed/feeds/blog.rss").getContentAsString().contains("Hello world"));
        assertTrue(visit("/s/os-feed/blog?tag=news").getContentAsString().contains("Hello world"));
        String sitemap = visit("/sitemap.xml").getContentAsString();
        assertTrue(sitemap.contains("/s/os-feed/blog/hello"), "the sitemap lists the post");
        assertTrue(sitemap.contains("hreflang=\"de\""), "with the market alternates");
    }

    // ========================================================== presentment

    @Test
    void aMarketCheckoutChargesItsCurrencyAndRefundsInIt() throws Exception {
        Shop s = shop("os-fx");
        euro(s);
        Store store = storeRows.findById(s.storeId);
        Markets.Pricer pricer = markets.pricer(store, "eu");
        assertNotNull(pricer);
        String user = People.customer(accounts, "os-fx-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 2));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        String rate = null;
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("Express".equals(o.name())) {
                rate = o.rateId();
            }
        }
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        OrderDto o = checkouts.place(user, s.storeId, "os-fx-key", new PlaceOrderDto(Shop.NY,
                rate, (String) card.get("id"), user + "@example.com", "", null), null, null,
                pricer);
        Map row = db.queryOne("SELECT presentment_currency, presentment_total_cents, "
                + "fx_rate_micros, total_cents FROM store_order WHERE id = ?",
                new Object[] {o.id()});
        long total = ((Number) row.get("total_cents")).longValue();
        long eurTotal = ((Number) row.get("presentment_total_cents")).longValue();
        assertEquals("EUR", row.get("presentment_currency"));
        assertEquals(920000L, ((Number) row.get("fx_rate_micros")).longValue());
        assertEquals((total * 920000L + 500000L) / 1000000L, eurTotal);
        Map pay = db.queryOne("SELECT amount_cents, currency FROM payment WHERE order_id = ?",
                new Object[] {o.id()});
        assertEquals("EUR", pay.get("currency"));
        assertEquals(eurTotal, ((Number) pay.get("amount_cents")).longValue());
        // Refund one of the two: the same share of the EUR charge goes back.
        Map line = db.queryOne("SELECT id FROM order_line WHERE order_id = ?",
                new Object[] {o.id()});
        List<RefundLineDto> back = new ArrayList<RefundLineDto>();
        back.add(new RefundLineDto((String) line.get("id"), 1));
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(back, 0, false, "One"));
        Map refund = db.queryOne("SELECT amount_cents, presentment_cents FROM refund "
                + "WHERE order_id = ?", new Object[] {o.id()});
        long settled = ((Number) refund.get("amount_cents")).longValue();
        long presented = ((Number) refund.get("presentment_cents")).longValue();
        assertEquals((settled * eurTotal + total / 2) / total, presented);
        Map after = db.queryOne("SELECT refunded_cents FROM payment WHERE order_id = ?",
                new Object[] {o.id()});
        assertEquals(presented, ((Number) after.get("refunded_cents")).longValue());
        // Refunding the rest empties the card exactly, rounding included.
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(back, o.shippingCents(), false, "Rest"));
        Map end = db.queryOne("SELECT refunded_cents, amount_cents FROM payment WHERE order_id = ?",
                new Object[] {o.id()});
        // What is left on the card is what is left of the order, at its rate:
        // the store-currency refunds did not cover all of it.
        Map settledRow = db.queryOne("SELECT SUM(amount_cents) AS a FROM refund WHERE order_id = ?",
                new Object[] {o.id()});
        long settledLeft = total - ((Number) settledRow.get("a")).longValue();
        long cardLeft = ((Number) end.get("amount_cents")).longValue()
                - ((Number) end.get("refunded_cents")).longValue();
        long expectLeft = (settledLeft * eurTotal + total / 2) / total;
        assertTrue(Math.abs(cardLeft - expectLeft) <= 1, cardLeft + " left on the card, "
                + settledLeft + " of the order's " + total + " unrefunded");
        if (settledLeft == 0) {
            assertEquals(0, cardLeft, "a full refund empties the card exactly");
        }
        Map sum = db.queryOne("SELECT SUM(presentment_cents) AS p FROM refund WHERE order_id = ?",
                new Object[] {o.id()});
        long refundedEur = ((Number) end.get("refunded_cents")).longValue();
        assertEquals(((Number) sum.get("p")).longValue(), refundedEur, "each refund went back in EUR");
        assertTrue(refundedEur <= ((Number) end.get("amount_cents")).longValue());
        // The math guards a zero total and overflow.
        assertEquals(0, Checkouts.atRate(0, 920000));
        assertEquals(400, status(() -> Checkouts.atRate(Long.MAX_VALUE / 2, 920000)));
    }

    // =============================================================== signup

    @Test
    void theSignUpFormRefusesBotsAndOpensAnAccount() throws Exception {
        Browser b = new Browser(mvc);
        b.get("/auth/signup");
        assertNotNull(b.fill, "the page carries a timing token");
        assertEquals(400, b.post("/auth/signup", "email", "os-bot@example.com", "password",
                "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477", "website", "http://spam.example")
                .getStatus());
        b.fill = "";
        assertEquals(400, b.post("/auth/signup", "email", "os-bot@example.com", "password",
                "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477").getStatus());
        bots.setMinFillMillis(60000);
        try {
            b.get("/auth/signup");
            assertEquals(400, b.post("/auth/signup", "email", "os-bot@example.com", "password",
                    "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477").getStatus());
        } finally {
            bots.setMinFillMillis(0);
        }
        b.get("/auth/signup");
        assertEquals(403, b.postWithoutToken("/auth/signup", "email", "os-human@example.com",
                "password", "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477").getStatus());
        b.get("/auth/signup");
        redirected(b.post("/auth/signup", "email", "os-human@example.com", "displayName", "Hu",
                "password", "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477"));
        Browser again = new Browser(mvc);
        again.signIn("os-human@example.com", "longenough1");
        // A taken e-mail is a message on the form, not an account.
        b.get("/auth/signup");
        MockResponse taken = b.post("/auth/signup", "email", "os-human@example.com",
                "password", "longenough1", "confirm", "longenough1", "phone", "+1 555 010 4477");
        assertEquals(200, taken.getStatus());
        assertTrue(taken.getContentAsString().contains("already has an account"));
    }

    // ===================================================== inclusive exempt

    @Test
    void anExemptionTakesTheIncludedTaxOffInATaxInclusiveStore() throws Exception {
        Shop s = shop("os-incl");
        db.execute("UPDATE store SET tax_inclusive = 1 WHERE id = ?", new Object[] {s.storeId});
        String exempt = People.customer(accounts, "os-incl-exempt");
        String plain = People.customer(accounts, "os-incl-plain");
        taxes.add(s.owner, s.storeId, new TaxExemptionDto(null, exempt, "CERT-INCL",
                Arrays.asList("US-NY"), 0));
        OrderDto taxed = s.buy(plain, 1);
        OrderDto free = s.buy(exempt, 1);
        assertTrue(taxed.taxCents() > 0, "prices include New York's tax");
        assertEquals(0, free.taxCents());
        assertEquals(taxed.totalCents() - taxed.taxCents(), free.totalCents());
        assertEquals(taxed.discountCents() + taxed.taxCents(), free.discountCents());
        assertEquals("CERT-INCL", taxes.certificateOf(free.id()));
        // Refunding everything gives back what the exempt buyer paid, no more.
        Map line = db.queryOne("SELECT id FROM order_line WHERE order_id = ?",
                new Object[] {free.id()});
        List<RefundLineDto> back = new ArrayList<RefundLineDto>();
        back.add(new RefundLineDto((String) line.get("id"), 1));
        orders.refund(s.owner, s.storeId, free.id(), new RefundRequestDto(back,
                free.shippingCents(), false, "All"));
        Map refunded = db.queryOne("SELECT SUM(amount_cents) AS a FROM refund WHERE order_id = ?",
                new Object[] {free.id()});
        assertTrue(((Number) refunded.get("a")).longValue() <= free.totalCents(),
                refunded.get("a") + " refunded of " + free.totalCents());
    }

    @Test
    void theWebCheckoutChargesTheMarketCurrencyFromTheCookie() throws Exception {
        Shop s = shop("os-webfx");
        euro(s);
        Browser b = new Browser(mvc);
        b.market = "eu";
        b.send(Browser.form("/s/os-webfx/cart/add", "variantId", s.variantId, "quantity", "1"));
        MockResponse page = b.get("/s/os-webfx/checkout");
        b.assertPage(page, "This order is charged in EUR");
        String html = page.getContentAsString();
        String marker = "name=\"idempotencyKey\" value=\"";
        int at = html.indexOf(marker);
        String key = html.substring(at + marker.length(), html.indexOf('"', at + marker.length()));
        MockResponse placed = b.post("/s/os-webfx/checkout/place", "email", "ada@example.com",
                "name", "Ada Lovelace", "line1", "1 Main St", "city", "Albany", "region", "NY",
                "postal", "12207", "country", "US", "cardNumber", "4242 4242 4242 4242",
                "cardExpiry", "12/39", "cardCvc", "123", "idempotencyKey", key);
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        Map row = db.queryOne("SELECT o.presentment_currency AS pc, p.currency AS cur FROM "
                + "store_order o JOIN payment p ON p.order_id = o.id WHERE o.store_id = ?",
                new Object[] {s.storeId});
        assertEquals("EUR", row.get("pc"));
        assertEquals("EUR", row.get("cur"));
    }
}
