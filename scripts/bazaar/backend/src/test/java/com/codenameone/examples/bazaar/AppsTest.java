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
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.DataRequestDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.customer.CustomerData;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.AppCallLimits;
import com.codenameone.examples.bazaar.platform.AppConsentPage;
import com.codenameone.examples.bazaar.platform.AppOAuth;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.Throttle;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Apps: registration, the OAuth install with PKCE, scoped tokens on the key
/// API, scope upgrades, removal, the call limit, app webhooks with the
/// privacy topics, and app-owned metafield namespaces.
@BackendTest
class AppsTest {
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
    @Autowired
    private Apps apps;
    @Autowired
    private AppCallLimits limits;
    @Autowired
    private CustomerData customerData;

    private static final String REDIRECT = "https://app.example/callback";
    private static final String HOOKS = "https://app.example/hooks";

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String m(Shop s) {
        return "/api/merchant/stores/" + s.storeId;
    }

    private static String v1(Shop s) {
        return "/api/v1/stores/" + s.storeId;
    }

    private static MockRequestBuilder bearer(MockRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    /// Registers a listed app asking for `scopes`.
    private Map register(String name, String... scopes) throws Exception {
        String developer = People.customer(accounts, "apps-" + name.toLowerCase().replace(' ', '-') + "-dev");
        StringBuilder list = new StringBuilder();
        for (int iter = 0; iter < scopes.length; iter++) {
            list.append(iter == 0 ? "" : ",").append('"').append(scopes[iter]).append('"');
        }
        Map app = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps"),
                developer), "{\"name\":\"" + name + "\",\"description\":\"Does things\","
                + "\"appUrl\":\"https://app.example\",\"webhookUrl\":\"" + HOOKS + "\","
                + "\"redirectUris\":[\"" + REDIRECT + "\"],\"scopes\":[" + list + "],"
                + "\"status\":\"LISTED\"}"));
        assertTrue(((String) app.get("clientSecret")).startsWith("bzs_"));
        return app;
    }

    private static String verifier(String seed) {
        StringBuilder b = new StringBuilder(seed);
        while (b.length() < 50) {
            b.append("-verifier");
        }
        return b.toString();
    }

    private static String authorizeJson(Map app, String scope, String challenge, boolean ok) {
        return "{\"clientId\":\"" + app.get("clientId") + "\",\"scope\":\"" + scope + "\","
                + "\"redirectUri\":\"" + REDIRECT + "\",\"state\":\"st8\",\"codeChallenge\":\""
                + challenge + "\",\"codeChallengeMethod\":\"S256\",\"approve\":" + ok + "}";
    }

    /// The owner approves through the merchant API; answers the code.
    private String approve(Shop s, Map app, String scope, String verifier) throws Exception {
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/apps/authorize"),
                s.owner), authorizeJson(app, scope, OAuth2Parameters.sha256(verifier), true)));
        String location = (String) to.get("location");
        assertTrue(location.startsWith(REDIRECT + "?"), location);
        assertTrue(location.contains("state=st8"), location);
        return query(location, "code");
    }

    private static String query(String url, String name) {
        Map<String, String> q = OAuth2Parameters.parse(url.substring(url.indexOf('?') + 1));
        return q.get(name);
    }

    private MockResponse token(Map app, String code, String verifier) throws Exception {
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("grant_type", "authorization_code");
        f.put("client_id", (String) app.get("clientId"));
        f.put("client_secret", (String) app.get("clientSecret"));
        f.put("code", code);
        f.put("code_verifier", verifier);
        f.put("redirect_uri", REDIRECT);
        return mvc.perform(post(AppOAuth.TOKEN_PATH)
                .contentType("application/x-www-form-urlencoded")
                .content(OAuth2Parameters.format(f))).andReturn().getResponse();
    }

    private String install(Shop s, Map app, String scope) throws Exception {
        String v = verifier("v" + s.storeId);
        MockResponse r = token(app, approve(s, app, scope, v), v);
        assertEquals(200, r.getStatus(), r.getContentAsString());
        String t = (String) com.codename1.backend.Json.parseObject(r.getContentAsString())
                .get("access_token");
        assertTrue(t.startsWith("bzk_app_"), t);
        return t;
    }

    private Map<String, Object> installationOf(Shop s, Map app) throws Exception {
        return db.queryOne("SELECT id AS id, status AS status, token_hash AS h, scopes AS sc "
                + "FROM app_installation WHERE store_id = ? AND app_id = ?",
                new Object[] {s.storeId, app.get("id")});
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
    void theWholeInstallThroughTheConsentPageAndPkceIsChecked() throws Exception {
        Shop s = shop("apps-flow");
        Map app = register("Flow app", "read_products", "write_metafields");
        String v = verifier("flow");
        String challenge = OAuth2Parameters.sha256(v);
        String link = AppConsentPage.PATH + "?store=" + s.storeId + "&client_id="
                + app.get("clientId") + "&scope=read_products&redirect_uri="
                + OAuth2Parameters.encode(REDIRECT) + "&state=abc&code_challenge=" + challenge
                + "&code_challenge_method=S256";

        Browser b = new Browser(mvc);
        b.signIn(s.owner, People.PASSWORD);
        MockResponse page = b.get(link);
        b.assertPage(page, "Flow app");
        assertTrue(page.getContentAsString().contains("read_products"));
        assertFalse(page.getContentAsString().contains("write_metafields"),
                "only what was asked is shown");
        // A redirect address the app did not register is an error here, never a redirect.
        MockResponse bad = b.get(link.replace(OAuth2Parameters.encode(REDIRECT),
                OAuth2Parameters.encode("https://evil.example/cb")));
        assertEquals(400, bad.getStatus());
        b.get(link);
        MockResponse decided = b.post(AppConsentPage.PATH, "store", s.storeId, "client_id",
                (String) app.get("clientId"), "scope", "read_products", "redirect_uri", REDIRECT,
                "state", "abc", "code_challenge", challenge, "code_challenge_method", "S256",
                "decision", "approve");
        assertEquals(303, decided.getStatus(), decided.getContentAsString());
        String location = decided.getHeader("Location");
        assertTrue(location.startsWith(REDIRECT + "?") && location.contains("state=abc"),
                location);
        String code = query(location, "code");

        // The wrong verifier is refused, and spends the code: the right one
        // after it is refused too.
        assertEquals(400, token(app, code, verifier("wrong")).getStatus());
        assertEquals(400, token(app, code, v).getStatus());
        assertEquals(null, installationOf(s, app));

        // A fresh code with the right verifier, and the wrong secret first.
        String code2 = approve(s, app, "read_products", v);
        Map<String, Object> wrongSecret = new LinkedHashMap<String, Object>(app);
        wrongSecret.put("clientSecret", "bzs_nope");
        assertEquals(401, token(wrongSecret, code2, v).getStatus());
        String code3 = approve(s, app, "read_products", v);
        MockResponse ok = token(app, code3, v);
        assertEquals(200, ok.getStatus(), ok.getContentAsString());
        Map answer = com.codename1.backend.Json.parseObject(ok.getContentAsString());
        String t = (String) answer.get("access_token");
        assertEquals("read_products", answer.get("scope"));
        assertEquals(s.storeId, answer.get("store_id"));
        // Kept as a hash only.
        assertFalse(String.valueOf(installationOf(s, app).get("h")).contains(t));

        MockResponse products = mvc.perform(bearer(get(v1(s) + "/products"), t)).andReturn()
                .getResponse();
        assertEquals(200, products.getStatus(), products.getContentAsString());
        assertTrue(products.getContentAsString().contains(s.productId));
        assertNotNull(products.getHeader(AppCallLimits.HEADER));
        // The token is an app's and reaches nothing outside the key API.
        assertEquals(403, Rest.statusOf(mvc, bearer(get(m(s) + "/products"), t)));

        List listed = Rest.list(mvc, People.asMerchant(get(m(s) + "/apps"), s.owner));
        assertEquals(1, listed.size());
        assertEquals(Apps.INSTALLED, ((Map) listed.get(0)).get("status"));
        Map audited = db.queryOne("SELECT COUNT(*) AS n FROM audit_event WHERE store_id = ? "
                + "AND action = 'APP_INSTALLED'", new Object[] {s.storeId});
        assertEquals(1L, Rows.number(audited.get("n")));
    }

    @Test
    void aScopeNotGrantedIsDeniedAndAnUpgradeAsksOnlyForWhatIsAdded() throws Exception {
        Shop s = shop("apps-scope");
        Map app = register("Scope app", "read_products", "read_orders");
        String t = install(s, app, "read_products");
        assertEquals(403, Rest.statusOf(mvc, bearer(get(v1(s) + "/orders"), t)));
        // A scope the app never registered cannot be asked for.
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/apps/consent"), s.owner), authorizeJson(app, "write_orders",
                OAuth2Parameters.sha256(verifier("x")), true))));

        Map consent = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/apps/consent"),
                s.owner), authorizeJson(app, "read_products read_orders",
                OAuth2Parameters.sha256(verifier("x")), true)));
        assertEquals(Boolean.TRUE, consent.get("upgrade"));
        assertEquals("[read_orders]", String.valueOf(consent.get("added")));
        assertEquals("[read_products]", String.valueOf(consent.get("granted")));

        String t2 = install(s, app, "read_products read_orders");
        assertEquals(200, Rest.statusOf(mvc, bearer(get(v1(s) + "/orders"), t2)));
        // The upgrade issued a new token; the old one is retired.
        assertEquals(401, Rest.statusOf(mvc, bearer(get(v1(s) + "/products"), t)));
        // A refusal sends the owner back with an error and changes nothing.
        Map denied = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/apps/authorize"),
                s.owner), authorizeJson(app, "", OAuth2Parameters.sha256(verifier("x")),
                false)));
        assertTrue(((String) denied.get("location")).contains("error=access_denied"));
        assertEquals(200, Rest.statusOf(mvc, bearer(get(v1(s) + "/orders"), t2)));
    }

    @Test
    void uninstallRevokesTheTokenAndSendsTheMandatoryWebhooks() throws Exception {
        Webhooks.RecordingTransport rec = new Webhooks.RecordingTransport();
        webhooks.useTransport(rec);
        Shop s = shop("apps-gone");
        Map app = register("Gone app", "read_orders", "read_customers");
        String t = install(s, app, "");
        String installationId = (String) installationOf(s, app).get("id");

        // An app subscribes; the hook is signed with the key derived from its secret.
        Map hook = Rest.json(mvc, Rest.body(bearer(post(v1(s) + "/webhooks"), t),
                "{\"topic\":\"orders/create\",\"url\":\"https://app.example/orders\"}"));
        assertEquals("", hook.get("secret"));
        // Staff do not see an app's hooks, and an app cannot follow a privacy topic.
        assertEquals(0, Rest.list(mvc, People.asMerchant(get(m(s) + "/webhooks"), s.owner))
                .size());
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s) + "/webhooks"), t),
                "{\"topic\":\"customers/redact\",\"url\":\"https://app.example/x\"}")));
        s.buy(People.customer(accounts, "apps-gone-buyer"), 1);
        webhooks.deliverDue(System.currentTimeMillis());
        List<String[]> orders = to(rec, Webhooks.ORDERS_CREATE);
        assertEquals(1, orders.size());
        String key = Apps.signingKey((String) app.get("clientSecret"));
        assertTrue(Webhooks.verify(key, orders.get(0)[2].getBytes("UTF-8"),
                header(orders.get(0)[1], Webhooks.SIGNATURE_HEADER)));

        // The privacy topics reach the installed app at the right moments.
        String buyer = People.customer(accounts, "apps-gone-buyer2");
        s.buy(buyer, 1);
        DataRequestDto asked = customerData.request(buyer, s.storeId, CustomerData.EXPORT);
        customerData.erase(s.owner, s.storeId, asked.customerId());
        webhooks.deliverDue(System.currentTimeMillis());
        assertEquals(1, to(rec, Webhooks.CUSTOMERS_DATA_REQUEST).size());
        assertEquals(1, to(rec, Webhooks.CUSTOMERS_REDACT).size());
        assertTrue(to(rec, Webhooks.CUSTOMERS_REDACT).get(0)[0].equals(HOOKS));

        Rest.list(mvc, People.asMerchant(delete(m(s) + "/apps/" + installationId), s.owner));
        assertEquals(401, Rest.statusOf(mvc, bearer(get(v1(s) + "/orders"), t)));
        assertEquals("", installationOf(s, app).get("h"));
        webhooks.deliverDue(System.currentTimeMillis());
        List<String[]> gone = to(rec, Webhooks.APP_UNINSTALLED);
        assertEquals(1, gone.size());
        assertTrue(Webhooks.verify(key, gone.get(0)[2].getBytes("UTF-8"),
                header(gone.get(0)[1], Webhooks.SIGNATURE_HEADER)));
        // Its own subscriptions are off now.
        s.buy(People.customer(accounts, "apps-gone-buyer3"), 1);
        webhooks.deliverDue(System.currentTimeMillis());
        // Two orders while installed, none since.
        assertEquals(2, to(rec, Webhooks.ORDERS_CREATE).size());

        // shop/redact 48 hours later, once.
        long later = System.currentTimeMillis() + Apps.SHOP_REDACT_MILLIS + 1000;
        assertEquals(0, apps.sendShopRedacts(System.currentTimeMillis()));
        assertTrue(apps.sendShopRedacts(later) >= 1);
        assertEquals(0, apps.sendShopRedacts(later));
        webhooks.deliverDue(later);
        assertEquals(1, to(rec, Webhooks.SHOP_REDACT).size());
        Map audited = db.queryOne("SELECT COUNT(*) AS n FROM audit_event WHERE store_id = ? "
                + "AND action = 'APP_UNINSTALLED'", new Object[] {s.storeId});
        assertEquals(1L, Rows.number(audited.get("n")));
    }

    @Test
    void theCallLimitIsALeakyBucketPerInstallation() throws Exception {
        Shop s = shop("apps-limit");
        Map app = register("Busy app", "read_products");
        String t = install(s, app, "");
        final long[] now = {System.currentTimeMillis()};
        limits.useClock(new Throttle.Clock() {
            @Override
            public long now() {
                return now[0];
            }
        });
        try {
            MockResponse last = null;
            int calls = 0;
            while (calls < limits.capacity()) {
                last = mvc.perform(bearer(get(v1(s) + "/products"), t)).andReturn()
                        .getResponse();
                assertEquals(200, last.getStatus(), last.getContentAsString());
                calls++;
            }
            assertEquals(limits.capacity() + "/" + limits.capacity(),
                    last.getHeader(AppCallLimits.HEADER));
            MockResponse refused = mvc.perform(bearer(get(v1(s) + "/products"), t)).andReturn()
                    .getResponse();
            assertEquals(429, refused.getStatus());
            assertEquals("1", refused.getHeader("Retry-After"));
            // A second's leak makes room again.
            now[0] += 1000;
            assertEquals(200, Rest.statusOf(mvc, bearer(get(v1(s) + "/products"), t)));
        } finally {
            limits.useClock(new Throttle.Clock());
        }
    }

    @Test
    void anAppWritesOnlyItsOwnMetafieldNamespace() throws Exception {
        Shop s = shop("apps-meta");
        Map one = register("Meta one", "write_metafields");
        Map two = register("Meta two", "write_metafields");
        String t1 = install(s, one, "");
        String t2 = install(s, two, "");
        Map def = Rest.json(mvc, Rest.body(bearer(post(v1(s) + "/metafield-definitions"), t1),
                "{\"ownerKind\":\"PRODUCT\",\"key\":\"rating\",\"name\":\"Rating\","
                + "\"type\":\"text\",\"storefront\":false}"));
        assertEquals("app--" + one.get("id") + "--rating", def.get("key"));
        String value = "{\"defId\":\"" + def.get("id") + "\",\"ownerId\":\"" + s.productId
                + "\",\"value\":\"five\"}";
        assertEquals(200, Rest.statusOf(mvc, Rest.body(bearer(put(v1(s) + "/metafields"), t1),
                value)));
        // Another app may not write it, and neither may the store's staff.
        assertEquals(403, Rest.statusOf(mvc, Rest.body(bearer(put(v1(s) + "/metafields"), t2),
                value)));
        assertEquals(403, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/metafield-values"), s.owner), value)));
        // Reading is a read_metafields matter: write covers it.
        MockResponse read = mvc.perform(bearer(get(v1(s) + "/metafields?ownerKind=PRODUCT"
                + "&ownerId=" + s.productId), t2)).andReturn().getResponse();
        assertEquals(200, read.getStatus(), read.getContentAsString());
        assertTrue(read.getContentAsString().contains("five"));
    }

    @Test
    void anAppTokenForOneStoreReachesNoOtherStore() throws Exception {
        Shop a = shop("apps-idor-a");
        Shop b = shop("apps-idor-b");
        Map app = register("Idor app", "read_products", "read_orders", "write_metafields");
        String ta = install(a, app, "");
        String tb = install(b, app, "");
        assertEquals(200, Rest.statusOf(mvc, bearer(get(v1(a) + "/products"), ta)));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/products"), ta)));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/products/" + b.productId),
                ta)));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/orders"), ta)));
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(post(v1(b) + "/graphql"), ta),
                "{\"query\":\"{ products { id } }\"}")));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/webhooks"), ta)));
        // One store's subscription is not another's to remove.
        Map hook = Rest.json(mvc, Rest.body(bearer(post(v1(b) + "/webhooks"), tb),
                "{\"topic\":\"orders/paid\",\"url\":\"https://app.example/paid\"}"));
        assertEquals(404, Rest.statusOf(mvc, bearer(delete(v1(a) + "/webhooks/"
                + hook.get("id")), ta)));
        // Nor is one store's installation another owner's to remove.
        String instB = (String) installationOf(b, app).get("id");
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(delete(m(a) + "/apps/" + instB),
                a.owner)));
        assertEquals(200, Rest.statusOf(mvc, bearer(get(v1(b) + "/products"), tb)));
    }

    private static String header(String headers, String name) {
        int at = headers.indexOf(name + ": ");
        if (at < 0) {
            return null;
        }
        int end = headers.indexOf('\n', at);
        return headers.substring(at + name.length() + 2, end < 0 ? headers.length() : end);
    }
}
