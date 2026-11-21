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
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// App slots: the owner's editor under `/admin/{store}/apps`, what a slot does
/// to a storefront page's policy, cache validators and HTML, the sandboxed
/// widget route, consent gating, and the places nothing optional loads.
@BackendTest
class WebSlotsTest {
    private static final String CDN = "https://cdn.example.com";
    private static final String API = "https://api.example.com";

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

    /// Adds an app as the owner `b`; answers its id.
    private String add(Browser b, Shop s, String name, String slot, String script, String html,
            String origins, String category) throws Exception {
        b.get("/admin/" + s.storeId + "/new-app");
        MockResponse r = b.post("/admin/" + s.storeId + "/apps", "name", name, "slot", slot,
                "scriptUrl", script, "widgetHtml", html, "origins", origins,
                "category", category, "enabled", "1");
        redirected(r);
        String to = r.getRedirectedUrl();
        String prefix = "/admin/" + s.storeId + "/apps/";
        assertTrue(to.startsWith(prefix), to);
        return to.substring(prefix.length());
    }

    private int apps(Shop s) throws Exception {
        List rows = db.query("SELECT id FROM app_slot WHERE store_id = ?",
                new Object[] {s.storeId});
        return rows.size();
    }

    private static MockRequestBuilder page(String path, String consent) {
        MockRequestBuilder b = com.codename1.backend.test.MockMvcRequestBuilders.get(path);
        if (consent != null) {
            b.cookie("bz_consent", consent);
        }
        return b;
    }

    private static String scriptSrc(String csp) {
        int at = csp.indexOf("script-src");
        return csp.substring(at, csp.indexOf(';', at));
    }

    @Test
    void anAppWidensThePagePolicyByItsHostsAndChangesTheTag() throws Exception {
        Shop s = shop("wsl-csp");
        Browser owner = signedIn(s.owner);
        MockResponse list = owner.get("/admin/" + s.storeId + "/apps");
        owner.assertPage(list, "No apps yet.");
        String id = add(owner, s, "Reviews", "body-end", CDN + "/reviews.js", "", API,
                "necessary");
        owner.assertPage(owner.get("/admin/" + s.storeId + "/apps"), "Reviews");
        Map audit = db.queryOne("SELECT action, username FROM audit_event WHERE store_id = ? "
                + "AND subject = ?", new Object[] {s.storeId, id});
        assertEquals("APP_SLOT_CREATED", audit.get("action"));
        assertEquals(s.owner, audit.get("username"));

        Browser visitor = new Browser(mvc);
        MockResponse p = visitor.get("/s/wsl-csp/products/wsl-csp-mug");
        assertEquals(200, p.getStatus());
        String csp = p.getHeader("Content-Security-Policy");
        assertTrue(scriptSrc(csp).contains(CDN), csp);
        assertTrue(scriptSrc(csp).contains(API), csp);
        assertTrue(csp.contains("connect-src 'self' " + API), csp);
        assertFalse(csp.contains("unsafe-inline"), csp);
        assertTrue(p.getContentAsString().contains("data-src=\"" + CDN + "/reviews.js\""));
        assertEquals("public, max-age=60, stale-while-revalidate=600",
                p.getHeader("Cache-Control"));
        String tag = p.getHeader("ETag");
        assertNotNull(tag);
        assertEquals(304, visitor.send(page("/s/wsl-csp/products/wsl-csp-mug", null)
                .header("If-None-Match", tag)).getStatus());

        // Only the allowed origins change: the HTML is the same, the policy
        // is not, so the old copy must not be revalidated.
        owner.get("/admin/" + s.storeId + "/apps/" + id);
        redirected(owner.post("/admin/" + s.storeId + "/apps/" + id, "name", "Reviews", "slot",
                "body-end", "scriptUrl", CDN + "/reviews.js", "widgetHtml", "", "origins",
                API + " https://api2.example.com", "category", "necessary", "enabled", "1"));
        MockResponse again = visitor.send(page("/s/wsl-csp/products/wsl-csp-mug", null)
                .header("If-None-Match", tag));
        assertEquals(200, again.getStatus());
        assertNotEquals(tag, again.getHeader("ETag"));
        assertTrue(again.getHeader("Content-Security-Policy").contains("https://api2.example.com"));
        assertEquals(1, db.query("SELECT id FROM audit_event WHERE store_id = ? AND subject = ? "
                + "AND action = 'APP_SLOT_UPDATED'", new Object[] {s.storeId, id}).size());

        redirected(owner.post("/admin/" + s.storeId + "/apps/" + id + "/delete"));
        assertEquals(0, apps(s));
        assertEquals(1, db.query("SELECT id FROM audit_event WHERE store_id = ? AND subject = ? "
                + "AND action = 'APP_SLOT_DELETED'", new Object[] {s.storeId, id}).size());
        MockResponse gone = visitor.get("/s/wsl-csp/products/wsl-csp-mug");
        assertFalse(gone.getHeader("Content-Security-Policy").contains(CDN));
        assertFalse(gone.getContentAsString().contains(CDN));
    }

    @Test
    void aWidgetIsFramedSandboxedAndServedWithItsOwnPolicyAndNoCookie() throws Exception {
        Shop s = shop("wsl-widget");
        Browser owner = signedIn(s.owner);
        String id = add(owner, s, "Size chart", "product-below-price", "",
                "<p>Fits true to size</p><script>document.title='x'</script>", API, "necessary");
        MockResponse p = new Browser(mvc).get("/s/wsl-widget/products/wsl-widget-mug");
        String html = p.getContentAsString();
        assertTrue(html.contains("sandbox=\"allow-scripts\""), html);
        assertTrue(html.contains("src=\"/w/" + id + "\""), html);
        assertFalse(html.contains("Fits true to size"));
        assertFalse(p.getHeader("Content-Security-Policy").contains("unsafe-inline"));

        MockResponse w = new Browser(mvc).get("/w/" + id);
        assertEquals(200, w.getStatus());
        assertTrue(w.getContentAsString().contains("Fits true to size"));
        String csp = w.getHeader("Content-Security-Policy");
        assertTrue(csp.startsWith("sandbox allow-scripts;"), csp);
        assertTrue(csp.contains("default-src 'none'"), csp);
        assertTrue(csp.contains("frame-ancestors 'self'"), csp);
        assertTrue(csp.contains("form-action 'none'"), csp);
        assertTrue(csp.contains("connect-src " + API), csp);
        assertNull(w.getHeader("Set-Cookie"));
        assertNull(w.getCookie("CN1SESSION"));

        assertEquals(404, new Browser(mvc).get("/w/nope").getStatus());
        owner.get("/admin/" + s.storeId + "/apps/" + id);
        redirected(owner.post("/admin/" + s.storeId + "/apps/" + id, "name", "Size chart",
                "slot", "product-below-price", "scriptUrl", "", "widgetHtml", "<p>x</p>",
                "origins", "", "category", "necessary"));
        assertEquals(404, new Browser(mvc).get("/w/" + id).getStatus());
    }

    @Test
    void optionalAppsWaitForConsentAndTheBannerWorksWithoutScript() throws Exception {
        Shop s = shop("wsl-consent");
        Browser owner = signedIn(s.owner);
        add(owner, s, "Stats", "head", CDN + "/stats.js", "", "", "analytics");
        add(owner, s, "Ads", "body-end", "https://ads.example.com/px.js", "", "", "marketing");
        String home = "/s/wsl-consent";

        Browser visitor = new Browser(mvc);
        MockResponse before = visitor.get(home);
        String html = before.getContentAsString();
        assertFalse(html.contains(CDN + "/stats.js"), html);
        assertFalse(html.contains("ads.example.com"));
        assertFalse(before.getHeader("Content-Security-Policy").contains(CDN));
        assertTrue(html.contains("action=\"/consent\""), html);
        assertTrue(html.contains("/s/wsl-consent/privacy"));
        assertTrue(before.getHeader("Vary").contains("Cookie"), before.getHeader("Vary"));
        assertTrue(before.getHeader("Cache-Control").startsWith("public"));

        MockResponse accepted = visitor.send(Browser.form("/consent", "choice", "custom",
                "analytics", "1", "back", home));
        assertEquals(303, accepted.getStatus());
        assertEquals(home, accepted.getRedirectedUrl());
        String cookie = accepted.getHeader("Set-Cookie");
        assertTrue(cookie.startsWith("bz_consent=analytics;"), cookie);
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"), cookie);

        MockResponse after = visitor.send(page(home, "analytics"));
        html = after.getContentAsString();
        assertTrue(html.contains("data-src=\"" + CDN + "/stats.js\""), html);
        assertTrue(html.contains("data-head=\"1\""), html);
        assertFalse(html.contains("ads.example.com"));
        assertFalse(html.contains("action=\"/consent\""));
        assertTrue(scriptSrc(after.getHeader("Content-Security-Policy")).contains(CDN));
        assertEquals("private, no-cache", after.getHeader("Cache-Control"));
        assertNotEquals(before.getHeader("ETag"), after.getHeader("ETag"));

        MockResponse all = visitor.send(page(home, "analytics.marketing"));
        assertTrue(all.getContentAsString().contains("https://ads.example.com/px.js"));
        MockResponse declined = visitor.send(page(home, "none"));
        assertFalse(declined.getContentAsString().contains(CDN));
        assertFalse(declined.getContentAsString().contains("action=\"/consent\""));

        // Declining needs no script either, and the form is no open redirect.
        MockResponse no = visitor.send(Browser.form("/consent", "choice", "none", "back",
                "//evil.example.com/x"));
        assertEquals("/", no.getRedirectedUrl());
        assertTrue(no.getHeader("Set-Cookie").startsWith("bz_consent=none;"));
        int crossSite = mvc.perform(Browser.form("/consent", "choice", "all", "back", "/")
                .header("Host", SignIn.HOST).header("Sec-Fetch-Site", "cross-site"))
                .andReturn().getResponse().getStatus();
        assertEquals(403, crossSite);

        MockResponse privacy = visitor.send(page(home + "/privacy", "analytics"));
        assertTrue(privacy.getContentAsString().contains("Stats"));
        assertTrue(privacy.getContentAsString().contains("You accepted: analytics"));
        assertEquals("private, no-cache", privacy.getHeader("Cache-Control"));
    }

    @Test
    void checkoutAndAccountNeverLoadOptionalApps() throws Exception {
        Shop s = shop("wsl-checkout");
        Browser owner = signedIn(s.owner);
        add(owner, s, "Stats", "body-end", CDN + "/stats.js", "", "", "analytics");
        add(owner, s, "Ads", "checkout-thank-you", "https://ads.example.com/px.js", "", "",
                "marketing");
        add(owner, s, "Fraud", "body-end", "https://risk.example.com/r.js", "", "",
                "necessary");

        Browser guest = new Browser(mvc);
        redirected(guest.send(Browser.form("/s/wsl-checkout/cart/add", "variantId", s.variantId,
                "quantity", "1")));
        MockResponse cart = guest.send(page("/s/wsl-checkout/cart", "analytics.marketing"));
        assertTrue(cart.getContentAsString().contains(CDN + "/stats.js"));
        MockResponse checkout = guest.send(page("/s/wsl-checkout/checkout",
                "analytics.marketing"));
        guest.assertPage(checkout, "Place order");
        String html = checkout.getContentAsString();
        assertFalse(html.contains(CDN), "an analytics app at checkout");
        assertFalse(html.contains("ads.example.com"), "a marketing app at checkout");
        assertFalse(html.contains("action=\"/consent\""));
        assertTrue(html.contains("https://risk.example.com/r.js"), "necessary apps still load");
        String csp = checkout.getHeader("Content-Security-Policy");
        assertFalse(csp.contains(CDN), csp);
        assertFalse(csp.contains("unsafe-inline"), csp);

        Browser shopper = signedIn(People.customer(accounts, "wsl-checkout-shopper"));
        MockResponse account = shopper.send(page("/account", "analytics.marketing"));
        assertEquals(200, account.getStatus());
        assertFalse(account.getContentAsString().contains(CDN));
        assertFalse(account.getContentAsString().contains("risk.example.com"));
        assertFalse(account.getHeader("Content-Security-Policy").contains(CDN));
    }

    @Test
    void onlyTheOwnerOfTheStoreEditsItsApps() throws Exception {
        Shop mine = shop("wsl-idor-a");
        Shop theirs = shop("wsl-idor-b");
        String theirApp = add(signedIn(theirs.owner), theirs, "Theirs", "body-end",
                CDN + "/t.js", "", "", "necessary");
        Browser b = signedIn(mine.owner);
        String a = "/admin/" + mine.storeId + "/apps";
        String other = "/admin/" + theirs.storeId + "/apps";
        String[] gets = {other, other + "/" + theirApp, "/admin/" + theirs.storeId + "/new-app",
            a + "/" + theirApp};
        for (int iter = 0; iter < gets.length; iter++) {
            assertEquals(404, b.get(gets[iter]).getStatus(), gets[iter]);
        }
        b.get(a);
        String[] posts = {other, other + "/" + theirApp, other + "/" + theirApp + "/delete",
            a + "/" + theirApp, a + "/" + theirApp + "/delete"};
        for (int iter = 0; iter < posts.length; iter++) {
            assertEquals(404, b.post(posts[iter], "name", "Mine now", "slot", "body-end",
                    "scriptUrl", "https://evil.example.com/x.js", "category", "necessary",
                    "enabled", "1").getStatus(), posts[iter]);
        }
        assertEquals(CDN + "/t.js", db.queryOne("SELECT script_url FROM app_slot WHERE id = ?",
                new Object[] {theirApp}).get("script_url"));
        assertEquals(1, apps(theirs));
        assertEquals(0, apps(mine));

        // A manager works at the store but may not put code on its pages.
        String manager = People.customer(accounts, "wsl-idor-manager");
        stores.saveStaff(mine.owner, mine.storeId, new StaffDto(null, manager, null, "MANAGER",
                null, null, false));
        Browser m = signedIn(manager);
        assertEquals(403, m.get(a).getStatus());
        m.get("/admin/" + mine.storeId);
        assertEquals(403, m.post(a, "name", "x", "slot", "body-end", "scriptUrl",
                CDN + "/x.js", "category", "necessary").getStatus());

        // A shopper is refused the whole back office.
        Browser shopper = signedIn(People.customer(accounts, "wsl-idor-shopper"));
        assertEquals(403, shopper.get(a).getStatus());
        shopper.get("/account");
        assertEquals(403, shopper.post(a, "name", "x", "slot", "body-end", "scriptUrl",
                CDN + "/x.js", "category", "necessary").getStatus());
        assertEquals(0, apps(mine));
    }

    @Test
    void aPostWithoutItsTokenOrWithABadAddressSavesNothing() throws Exception {
        Shop s = shop("wsl-bad");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId + "/apps";
        b.get(a);
        assertEquals(403, b.postWithoutToken(a, "name", "Forged", "slot", "body-end",
                "scriptUrl", CDN + "/x.js", "category", "necessary").getStatus());
        String[][] bad = {
            {"scriptUrl", "http://cdn.example.com/x.js", "origins", ""},
            {"scriptUrl", "javascript:alert(1)", "origins", ""},
            {"scriptUrl", "https://user@cdn.example.com/x.js", "origins", ""},
            {"scriptUrl", "https://cdn.example.com/x.js\" onload=\"x", "origins", ""},
            {"scriptUrl", CDN + "/x.js", "origins", "http://api.example.com"},
            {"scriptUrl", CDN + "/x.js", "origins", "https://*.example.com"},
            {"scriptUrl", CDN + "/x.js", "origins", "'unsafe-inline'"},
            {"scriptUrl", "", "origins", ""},
        };
        for (int iter = 0; iter < bad.length; iter++) {
            b.get(a);
            MockResponse r = b.post(a, "name", "Bad", "slot", "body-end", bad[iter][0],
                    bad[iter][1], bad[iter][2], bad[iter][3], "category", "necessary",
                    "enabled", "1");
            redirected(r);
            assertEquals("/admin/" + s.storeId + "/new-app", r.getRedirectedUrl(), bad[iter][1]);
        }
        b.get(a);
        redirected(b.post(a, "name", "Bad place", "slot", "checkout-payment", "scriptUrl",
                CDN + "/x.js", "category", "necessary"));
        assertEquals(0, apps(s));
    }
}
