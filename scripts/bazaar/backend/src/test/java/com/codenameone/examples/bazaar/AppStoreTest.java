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
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.platform.AppOAuth;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The app directory and app billing: the listing review and what it makes
/// public, installing from a listing, one review per store, and charges
/// approved, declined and cancelled by removal, each store kept to its own.
@BackendTest
class AppStoreTest {
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
    private Webhooks webhooks;

    private static final String REDIRECT = "https://app.example/callback";

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String m(Shop s) {
        return "/api/merchant/stores/" + s.storeId;
    }

    private static MockRequestBuilder bearer(MockRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private static final class Dev {
        String username;
        Map app;
    }

    private Dev register(String name) throws Exception {
        Dev d = new Dev();
        d.username = People.customer(accounts, "store-" + AppStoreText.handle(name) + "-dev");
        d.app = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps"),
                d.username), "{\"name\":\"" + name + "\",\"description\":\"Does things\","
                + "\"appUrl\":\"https://app.example/start\",\"webhookUrl\":"
                + "\"https://app.example/hooks\",\"redirectUris\":[\"" + REDIRECT + "\"],"
                + "\"scopes\":[\"read_products\"],\"status\":\"LISTED\"}"));
        return d;
    }

    private Map submit(Dev d, String name, String tagline) throws Exception {
        return Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps/"
                + d.app.get("id") + "/listing"), d.username), "{\"name\":\"" + name
                + "\",\"tagline\":\"" + tagline + "\",\"description\":\"Long words\","
                + "\"category\":\"marketing\",\"iconUrl\":\"https://app.example/i.png\","
                + "\"screenshots\":[\"https://app.example/s1.png\"],\"pricing\":\"Free\","
                + "\"supportUrl\":\"https://app.example/help\",\"privacyUrl\":\"\"}"));
    }

    private int decide(String listingId, String action, String reason) throws Exception {
        return Rest.statusOf(mvc, Rest.body(People.asAdmin(post("/api/admin/app-listings/"
                + listingId + "/decision"), admin()), "{\"action\":\"" + action
                + "\",\"reason\":\"" + reason + "\"}"));
    }

    private String admin;

    private String admin() throws Exception {
        if (admin == null) {
            admin = People.customer(accounts, "store-admin-" + Long.toHexString(System.nanoTime()));
        }
        return admin;
    }

    private Map approved(String name) throws Exception {
        Dev d = register(name);
        Map l = submit(d, name, "Tag of " + name);
        assertEquals(200, decide((String) l.get("id"), "APPROVE", ""));
        l.put("dev", d);
        return l;
    }

    /// Installs `app` in `s` the way the app would after the directory's link.
    private String install(Shop s, Map app) throws Exception {
        String v = "verifier-" + s.storeId + "-0123456789012345678901234567890123456789";
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/apps/authorize"),
                s.owner), "{\"clientId\":\"" + app.get("clientId") + "\",\"scope\":\"\","
                + "\"redirectUri\":\"" + REDIRECT + "\",\"state\":\"x\",\"codeChallenge\":\""
                + OAuth2Parameters.sha256(v) + "\",\"codeChallengeMethod\":\"S256\","
                + "\"approve\":true}"));
        String location = (String) to.get("location");
        String code = OAuth2Parameters.parse(location.substring(location.indexOf('?') + 1))
                .get("code");
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("grant_type", "authorization_code");
        f.put("client_id", (String) app.get("clientId"));
        f.put("client_secret", (String) app.get("clientSecret"));
        f.put("code", code);
        f.put("code_verifier", v);
        f.put("redirect_uri", REDIRECT);
        MockResponse r = mvc.perform(post(AppOAuth.TOKEN_PATH)
                .contentType("application/x-www-form-urlencoded")
                .content(OAuth2Parameters.format(f))).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return (String) Json.parseObject(r.getContentAsString()).get("access_token");
    }

    private String installationId(Shop s, Map app) throws Exception {
        return Rows.text(db.queryOne("SELECT id FROM app_installation WHERE store_id = ? AND "
                + "app_id = ?", new Object[] {s.storeId, app.get("id")}).get("id"));
    }

    private static List<String[]> to(Webhooks.RecordingTransport t, String topic) {
        List<String[]> all = t.sent();
        List<String[]> out = new java.util.ArrayList<String[]>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter)[1].contains("X-Bazaar-Topic: " + topic + "\n")) {
                out.add(all.get(iter));
            }
        }
        return out;
    }

    // ----------------------------------------------------------------- tests

    @Test
    void onlyApprovedListingsArePublicAndEveryDecisionIsHeld() throws Exception {
        Dev d = register("Review Flow");
        Map l = submit(d, "Review Flow <b>&\\\"", "Finds things");
        String id = (String) l.get("id");
        String handle = (String) l.get("handle");
        assertEquals("PENDING", l.get("status"));
        assertEquals("review-flow-b", handle);
        // Pending: not in the directory, not on its page, but in the admin queue.
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/apps/" + handle)));
        assertEquals(404, mvc.perform(get("/apps/" + handle)).andReturn().getResponse()
                .getStatus());
        assertFalse(contains(Rest.list(mvc, get(dir("finds", ""))), id));
        assertTrue(contains(Rest.list(mvc, People.asAdmin(get("/api/admin/app-listings"),
                admin())), id));
        // Neither a merchant nor its developer decides; a rejection needs a reason.
        assertEquals(403, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(
                "/api/admin/app-listings/" + id + "/decision"), d.username),
                "{\"action\":\"APPROVE\",\"reason\":\"\"}")));
        assertEquals(400, decide(id, "REJECT", ""));
        assertEquals(200, decide(id, "REJECT", "Screenshots missing"));
        Map mine = Rest.json(mvc, People.asMerchant(get("/api/developer/apps/" + d.app.get("id")
                + "/listing"), d.username));
        assertEquals("REJECTED", mine.get("status"));
        assertEquals("Screenshots missing", mine.get("reason"));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/apps/" + handle)));
        // Resubmitted and approved: public, searchable, rendered escaped.
        submit(d, "Review Flow <b>&\\\"", "Finds things");
        assertEquals(200, decide(id, "APPROVE", ""));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/apps/" + handle)));
        assertTrue(contains(Rest.list(mvc, get(dir("FINDS", ""))), id));
        assertTrue(contains(Rest.list(mvc, get(dir("", "marketing"))), id));
        assertFalse(contains(Rest.list(mvc, get(dir("finds", "finance"))),
                id));
        MockResponse page = mvc.perform(get("/apps/" + handle)).andReturn().getResponse();
        assertEquals(200, page.getStatus());
        String html = page.getContentAsString();
        assertTrue(html.contains("<title>Review Flow &lt;b&gt;&amp;&quot; - Bazaar apps</title>"),
                html);
        assertTrue(html.contains("<meta name=\"description\" content=\"Finds things\">"), html);
        assertFalse(html.contains("<b>&\""), html);
        String index = mvc.perform(get("/apps?q=finds")).andReturn().getResponse()
                .getContentAsString();
        assertTrue(index.contains("/apps/" + handle), index);
        // Delisted: gone again, and a delisting needs a reason too.
        assertEquals(400, decide(id, "DELIST", ""));
        assertEquals(200, decide(id, "DELIST", "Broke its terms"));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/apps/" + handle)));
        assertFalse(contains(Rest.list(mvc, get(dir("", ""))), id));
        // Only the developer reads or writes their app's listing.
        String other = People.customer(accounts, "store-not-the-dev");
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(get("/api/developer/apps/"
                + d.app.get("id") + "/listing"), other)));
    }

    @Test
    void installingFromAListingReachesTheAppsConsent() throws Exception {
        Shop s = shop("store-install");
        Map l = approved("Install From Here");
        String link = AppStoreText.installUrl((String) l.get("installUrl"), s.storeId,
                (String) l.get("clientId"));
        assertTrue(link.startsWith("https://app.example/start?store_id=" + s.storeId
                + "&client_id="), link);
        // The app sends the owner on with its PKCE challenge: consent shows it.
        Map consent = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/apps/consent"),
                s.owner), "{\"clientId\":\"" + l.get("clientId") + "\",\"scope\":\"\","
                + "\"redirectUri\":\"" + REDIRECT + "\",\"state\":\"x\",\"codeChallenge\":\""
                + OAuth2Parameters.sha256("v-0123456789012345678901234567890123456789012")
                + "\",\"codeChallengeMethod\":\"S256\",\"approve\":false}"));
        assertEquals("Install From Here", consent.get("appName"));
        assertEquals(l.get("appId"), consent.get("appId"));
        Browser browser = new Browser(mvc);
        browser.signIn(s.owner, People.PASSWORD);
        String web = browser.get("/account/apps/authorize?store="
                + s.storeId + "&client_id=" + l.get("clientId") + "&redirect_uri="
                + OAuth2Parameters.encode(REDIRECT) + "&code_challenge="
                + OAuth2Parameters.sha256("v-0123456789012345678901234567890123456789012")
                + "&code_challenge_method=S256").getContentAsString();
        assertTrue(web.contains("Install Install From Here"), web);
    }

    @Test
    void oneReviewPerStoreByAStoreThatInstalledItAndAdminsHideThem() throws Exception {
        Shop a = shop("store-rev-a");
        Shop b = shop("store-rev-b");
        Shop never = shop("store-rev-never");
        Map l = approved("Reviewed App");
        Map app = ((Dev) l.get("dev")).app;
        install(a, app);
        install(b, app);
        // B removes it, and may still review it.
        Rest.list(mvc, People.asMerchant(delete(m(b) + "/apps/" + installationId(b, app)),
                b.owner));
        String body = "{\"appId\":\"" + app.get("id") + "\",\"rating\":5,\"body\":\"Great\"}";
        assertEquals(403, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(never)
                + "/app-reviews"), never.owner), body)));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(a)
                + "/app-reviews"), a.owner), "{\"appId\":\"" + app.get("id")
                + "\",\"rating\":6,\"body\":\"\"}")));
        Map first = Rest.json(mvc, Rest.body(People.asMerchant(post(m(a) + "/app-reviews"),
                a.owner), body));
        // Writing again replaces the store's one review.
        Map again = Rest.json(mvc, Rest.body(People.asMerchant(post(m(a) + "/app-reviews"),
                a.owner), "{\"appId\":\"" + app.get("id") + "\",\"rating\":3,\"body\":\"OK\"}"));
        assertEquals(first.get("id"), again.get("id"));
        Map rb = Rest.json(mvc, Rest.body(People.asMerchant(post(m(b) + "/app-reviews"),
                b.owner), "{\"appId\":\"" + app.get("id") + "\",\"rating\":4,\"body\":\"<i>\"}"));
        Map shown = Rest.json(mvc, get("/api/shop/apps/" + l.get("handle")));
        assertEquals(2L, Rest.number(shown, "reviews"));
        assertEquals(35L, Rest.number(shown, "ratingTenths"));
        assertEquals(1L, Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM app_review WHERE "
                + "store_id = ?", new Object[] {a.storeId}).get("n")));
        // Hidden: gone from the listing and its average, still in the admin's list.
        String admin = admin();
        assertEquals(403, Rest.statusOf(mvc, People.asMerchant(post("/api/admin/app-reviews/"
                + rb.get("id") + "/hide"), b.owner)));
        assertEquals(200, Rest.statusOf(mvc, People.asAdmin(post("/api/admin/app-reviews/"
                + rb.get("id") + "/hide"), admin)));
        shown = Rest.json(mvc, get("/api/shop/apps/" + l.get("handle")));
        assertEquals(1L, Rest.number(shown, "reviews"));
        assertEquals(30L, Rest.number(shown, "ratingTenths"));
        assertEquals(1, Rest.list(mvc, get("/api/shop/apps/" + l.get("handle") + "/reviews"))
                .size());
        assertEquals(2, Rest.list(mvc, People.asAdmin(get("/api/admin/app-listings/"
                + l.get("id") + "/reviews"), admin)).size());
    }

    @Test
    void chargesAreApprovedDeclinedAndCancelledWithTheAppAndKeptToTheirStore()
            throws Exception {
        Webhooks.RecordingTransport rec = new Webhooks.RecordingTransport();
        webhooks.useTransport(rec);
        Shop a = shop("store-bill-a");
        Shop b = shop("store-bill-b");
        Map app = register("Billing App").app;
        String ta = install(a, app);
        String tb = install(b, app);
        String v1a = "/api/v1/stores/" + a.storeId + "/app-charges";
        String recurring = "{\"name\":\"Pro plan\",\"kind\":\"RECURRING\",\"amount\":999,"
                + "\"currency\":\"USD\",\"trialDays\":14,\"returnUrl\":\"https://app.example/b\"}";
        // Bad requests, a staff member's session and another store's token are refused.
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(v1a), ta),
                "{\"name\":\"x\",\"kind\":\"ONE_TIME\",\"amount\":100,\"currency\":\"USD\","
                + "\"trialDays\":3}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(v1a), ta),
                "{\"name\":\"x\",\"kind\":\"ONE_TIME\",\"amount\":0,\"currency\":\"USD\"}")));
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(post(v1a), tb), recurring)));
        Map plan = Rest.json(mvc, Rest.body(bearer(post(v1a), ta), recurring));
        assertEquals("PENDING", plan.get("status"));
        assertTrue(((String) plan.get("confirmUrl")).startsWith("/account/apps/charges/"));
        Map once = Rest.json(mvc, Rest.body(bearer(post(v1a), ta), "{\"name\":\"Setup\","
                + "\"kind\":\"ONE_TIME\",\"amount\":2500,\"currency\":\"USD\"}"));
        Map later = Rest.json(mvc, Rest.body(bearer(post(v1a), ta), "{\"name\":\"Extra\","
                + "\"kind\":\"ONE_TIME\",\"amount\":100,\"currency\":\"USD\"}"));
        // B sees none of A's charges and cannot answer them.
        assertEquals(0, Rest.list(mvc, People.asMerchant(get(m(b) + "/app-charges"), b.owner))
                .size());
        assertEquals(404, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(b)
                + "/app-charges/" + plan.get("id") + "/decision"), b.owner),
                "{\"approve\":true}")));
        assertEquals(404, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(a)
                + "/app-charges/" + plan.get("id") + "/decision"), b.owner),
                "{\"approve\":true}")));
        assertEquals(3, Rest.list(mvc, People.asMerchant(get(m(a) + "/app-charges"), a.owner))
                .size());
        assertEquals(0, Rest.list(mvc, bearer(get("/api/v1/stores/" + b.storeId
                + "/app-charges"), tb)).size());

        // Approve the plan in the client, decline the one-time charge on the web page.
        Map ok = Rest.json(mvc, Rest.body(People.asMerchant(post(m(a) + "/app-charges/"
                + plan.get("id") + "/decision"), a.owner), "{\"approve\":true}"));
        assertEquals("ACTIVE", ok.get("status"));
        assertTrue(Rest.number(ok, "billingStartsAt") > System.currentTimeMillis()
                + 13L * 24 * 60 * 60 * 1000);
        assertEquals(409, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(a)
                + "/app-charges/" + plan.get("id") + "/decision"), a.owner),
                "{\"approve\":false}")));
        Browser browser = new Browser(mvc);
        browser.signIn(a.owner, People.PASSWORD);
        MockResponse page = browser.get((String) once.get("confirmUrl"));
        assertEquals(200, page.getStatus());
        assertTrue(page.getContentAsString().contains("$25.00 once"),
                page.getContentAsString());
        MockResponse declined = browser.post("/account/apps/charges/" + once.get("id"),
                "store", a.storeId, "decision", "decline");
        assertEquals(303, declined.getStatus(), declined.getContentAsString());
        assertEquals("DECLINED", Rows.text(db.queryOne("SELECT status FROM app_charge WHERE "
                + "id = ?", new Object[] {once.get("id")}).get("status")));
        // The platform owes the developer for the plan alone.
        Map owed = db.queryOne("SELECT COUNT(*) AS n, SUM(amount) AS total FROM "
                + "app_payout_entry WHERE app_id = ?", new Object[] {app.get("id")});
        assertEquals(1L, Rows.number(owed.get("n")));
        assertEquals(999L, Rows.number(owed.get("total")));

        // Removing the app cancels the active plan and the pending charge.
        Rest.list(mvc, People.asMerchant(delete(m(a) + "/apps/" + installationId(a, app)),
                a.owner));
        List charges = Rest.list(mvc, People.asMerchant(get(m(a) + "/app-charges"), a.owner));
        for (int iter = 0; iter < charges.size(); iter++) {
            Map c = (Map) charges.get(iter);
            String expected = c.get("id").equals(once.get("id")) ? "DECLINED" : "CANCELLED";
            assertEquals(expected, c.get("status"), String.valueOf(c.get("name")));
        }
        assertTrue(Rest.list(mvc, bearer(get("/api/v1/stores/" + b.storeId
                + "/app-charges"), tb)).isEmpty());
        webhooks.deliverDue(System.currentTimeMillis());
        // Approved, declined, then the two cancellations.
        List<String[]> updates = to(rec, Webhooks.APP_SUBSCRIPTIONS_UPDATE);
        assertEquals(4, updates.size());
        boolean cancelledSeen = false;
        for (int iter = 0; iter < updates.size(); iter++) {
            Map p = Json.parseObject(updates.get(iter)[2]);
            assertEquals(a.storeId, p.get("store_id"));
            if ("CANCELLED".equals(p.get("status")) && later.get("id").equals(p.get("charge_id"))) {
                cancelledSeen = true;
            }
        }
        assertTrue(cancelledSeen);
        // A removed app's token charges nothing.
        assertEquals(401, Rest.statusOf(mvc, Rest.body(bearer(post(v1a), ta), recurring)));
    }

    private static String dir(String q, String category) {
        return "/api/shop/apps?q=" + q + "&category=" + category + "&page=0";
    }

    private static boolean contains(List listings, Object id) {
        for (int iter = 0; iter < listings.size(); iter++) {
            if (id.equals(((Map) listings.get(iter)).get("id"))) {
                return true;
            }
        }
        return false;
    }
}
