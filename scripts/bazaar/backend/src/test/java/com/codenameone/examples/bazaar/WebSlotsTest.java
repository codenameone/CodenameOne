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

/// App slots: what a slot does
/// to a storefront page's policy, cache validators and HTML, the sandboxed
/// widget route, consent gating, and the places nothing optional loads.
@BackendTest
class WebSlotsTest {
    private static final String CDN = "https://cdn.example.com";
    private static final String API = "https://api.example.com";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private com.codenameone.examples.bazaar.web.slots.Slots slots;
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

    /// Adds an app as the store's owner (`b` is unused: the editor is the
    /// app's, so the test saves through [Slots] as its REST API does); answers
    /// its id.
    private String add(Browser b, Shop s, String name, String slot, String script, String html,
            String origins, String category) throws Exception {
        com.codenameone.examples.bazaar.web.slots.AppSlot a =
                new com.codenameone.examples.bazaar.web.slots.AppSlot();
        a.name = name;
        a.slot = slot;
        a.scriptUrl = script;
        a.widgetHtml = html;
        a.origins = origins;
        a.category = category;
        a.enabled = true;
        return slots.save(s.owner, s.storeId, null, a);
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

    /// The back office is the app's: an old back-office address, signed in
    /// or not, is a permanent redirect to `/app/`, never a page.
    @Test
    void oldBackOfficeAddressesGoToTheApp() throws Exception {
        String[] old = {"/admin", "/admin/s1/orders", "/admin/s1/apps/x", "/platform"};
        for (int iter = 0; iter < old.length; iter++) {
            MockResponse r = new Browser(mvc).get(old[iter]);
            assertEquals(301, r.getStatus(), old[iter]);
            assertEquals("/app/", r.getHeader("Location"), old[iter]);
        }
    }

}
