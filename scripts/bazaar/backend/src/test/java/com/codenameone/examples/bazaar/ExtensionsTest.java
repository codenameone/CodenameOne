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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AppSessionDto;
import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.ExtensionActionDto;
import com.codenameone.examples.bazaar.api.ExtensionActionResultDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderFieldDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.extensions.AppProxyPages;
import com.codenameone.examples.bazaar.extensions.AppSessionTokens;
import com.codenameone.examples.bazaar.extensions.Extensions;
import com.codenameone.examples.bazaar.extensions.ProxySignature;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.AppOAuth;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// App UI extensions end to end: an installed app uploads declarative block
/// trees, Bazaar fetches their data and posts their actions signed, opens the
/// app's page with a short-lived token, prints storefront blocks escaped,
/// forwards the app proxy signed and capped, and keeps checkout answers on
/// the order. Uninstalling the app takes all of it away.
@BackendTest
class ExtensionsTest {
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
    private Apps apps;
    @Autowired
    private Extensions extensions;

    private static final String REDIRECT = "https://app.example/callback";

    /// Answers a fixed reply and keeps what it was sent.
    static final class Fake implements Extensions.Transport {
        int status = 200;
        String type = "application/json";
        byte[] reply = "{}".getBytes();
        final List<String> urls = new ArrayList<String>();
        final List<List<String>> headers = new ArrayList<List<String>>();
        final List<byte[]> bodies = new ArrayList<byte[]>();

        @Override
        public Extensions.Reply send(String method, String url, List<String> h, byte[] body,
                int timeoutMillis, int maxBytes) throws IOException {
            urls.add(url);
            headers.add(h);
            bodies.add(body);
            return new Extensions.Reply(status, type, reply);
        }

        String header(String name) {
            List<String> h = headers.get(headers.size() - 1);
            for (int iter = 0; iter < h.size(); iter++) {
                if (h.get(iter).startsWith(name + ": ")) {
                    return h.get(iter).substring(name.length() + 2);
                }
            }
            return null;
        }
    }

    private Fake fake() {
        Fake f = new Fake();
        extensions.useTransport(f);
        extensions.clearCache();
        return f;
    }

    private Shop shop(String name) throws Exception {
        return new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String v1(Shop s) {
        return "/api/v1/stores/" + s.storeId + "/extensions";
    }

    private static MockRequestBuilder bearer(MockRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private Map register(String name) throws Exception {
        String developer = People.customer(accounts, "ext-" + name + "-dev");
        return Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps"),
                developer), "{\"name\":\"" + name + "\",\"description\":\"Extensions\","
                + "\"appUrl\":\"https://app.example/admin\",\"webhookUrl\":\"https://app.example/h\","
                + "\"redirectUris\":[\"" + REDIRECT + "\"],\"scopes\":[\"write_extensions\"],"
                + "\"status\":\"LISTED\"}"));
    }

    private String install(Shop s, Map app) throws Exception {
        StringBuilder v = new StringBuilder("verifier-" + s.storeId);
        while (v.length() < 50) {
            v.append("-v");
        }
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/merchant/stores/"
                + s.storeId + "/apps/authorize"), s.owner), "{\"clientId\":\"" + app.get("clientId")
                + "\",\"scope\":\"\",\"redirectUri\":\"" + REDIRECT + "\",\"state\":\"s\","
                + "\"codeChallenge\":\"" + OAuth2Parameters.sha256(v.toString())
                + "\",\"codeChallengeMethod\":\"S256\",\"approve\":true}"));
        String location = (String) to.get("location");
        String code = OAuth2Parameters.parse(location.substring(location.indexOf('?') + 1))
                .get("code");
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("grant_type", "authorization_code");
        f.put("client_id", (String) app.get("clientId"));
        f.put("client_secret", (String) app.get("clientSecret"));
        f.put("code", code);
        f.put("code_verifier", v.toString());
        f.put("redirect_uri", REDIRECT);
        MockResponse r = mvc.perform(post(AppOAuth.TOKEN_PATH)
                .contentType("application/x-www-form-urlencoded")
                .content(OAuth2Parameters.format(f))).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return (String) Json.parseObject(r.getContentAsString()).get("access_token");
    }

    private Map upload(Shop s, String token, String body) throws Exception {
        return Rest.json(mvc, Rest.body(bearer(post(v1(s)), token), body));
    }

    private int uploadStatus(Shop s, String token, String body) throws Exception {
        return Rest.statusOf(mvc, Rest.body(bearer(post(v1(s)), token), body));
    }

    private String installationOf(Shop s, Map app) throws IOException {
        Map r = db.queryOne("SELECT id FROM app_installation WHERE store_id = ? AND app_id = ?",
                new Object[] {s.storeId, app.get("id")});
        return (String) r.get("id");
    }

    private OrderDto place(Shop s, String user, List<FieldAnswerDto> fields) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        String rate = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null))
                .shippingOptions().get(0).rateId();
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY, rate,
                (String) card.get("id"), user + "@example.com", "", null, 0, fields));
    }

    private static String value(List<BlockValueDto> values, String key) {
        for (BlockValueDto v : values) {
            if (key.equals(v.key())) {
                return v.value();
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- tests

    @Test
    void badTreesAreRefusedAndOnlyTheAppsTokenUploads() throws Exception {
        Shop s = shop("ext-validate");
        Map app = register("validator");
        String t = install(s, app);
        String ok = "{\"type\":\"text\",\"text\":\"Hi\"}";
        String deep = ok;
        for (int iter = 0; iter < 8; iter++) {
            deep = "{\"type\":\"stack\",\"children\":[" + deep + "]}";
        }
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.block\",\"tree\":"
                + "{\"type\":\"script\",\"text\":\"x\"}}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.block\",\"tree\":"
                + "{\"type\":\"text\",\"text\":\"x\",\"onclick\":\"y\"}}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.block\",\"tree\":"
                + "{\"type\":\"image\",\"src\":\"http://cdn.example/a.png\"}}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.block\",\"tree\":"
                + deep + "}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.nowhere\",\"tree\":" + ok
                + "}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"storefront.block\",\"tree\":" + ok
                + "}"));
        // An action needs somewhere to post it.
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.action\",\"tree\":"
                + "{\"type\":\"button\",\"label\":\"Go\",\"action\":\"go\"}}"));
        assertEquals(400, uploadStatus(s, t, "{\"target\":\"admin.order.block\",\"tree\":" + ok
                + ",\"dataUrl\":\"https://127.0.0.1/data\"}"));
        upload(s, t, "{\"target\":\"admin.order.block\",\"tree\":" + ok + "}");
        assertEquals(1, Rest.list(mvc, bearer(get(v1(s)), t)).size());
        // A merchant's session is not an app token.
        assertEquals(401, Rest.statusOf(mvc, People.asMerchant(get(v1(s)), s.owner)));
    }

    @Test
    void anAdminBlockFetchesSignedDataOnceAndBindsIt() throws Exception {
        Shop s = shop("ext-data");
        Map app = register("points");
        String t = install(s, app);
        upload(s, t, "{\"target\":\"admin.order.block\",\"title\":\"Points\",\"tree\":"
                + "{\"type\":\"text\",\"text\":\"Points: {{points}}\"},"
                + "\"dataUrl\":\"https://app.example/data\"}");
        OrderDto o = place(s, People.customer(accounts, "ext-data-buyer"), null);
        Fake f = fake();
        f.reply = "{\"values\":{\"points\":\"42\"}}".getBytes("UTF-8");
        List<ExtensionBlockDto> b = extensions.blocks(s.owner, s.storeId, "order", o.id());
        assertEquals(1, b.size());
        assertEquals("", b.get(0).error());
        assertEquals("42", value(b.get(0).values(), "points"));
        assertEquals(Webhooks.signature(apps.signingKeyOf((String) app.get("id")),
                f.bodies.get(0)), f.header(Webhooks.SIGNATURE_HEADER));
        Map sent = Json.parseObject(new String(f.bodies.get(0), "UTF-8"));
        assertEquals(o.id(), sent.get("resourceId"));
        extensions.blocks(s.owner, s.storeId, "order", o.id());
        assertEquals(1, f.urls.size(), "the second look is cached");
        // An app that fails shows its block with an error, not the page's.
        extensions.clearCache();
        f.status = 500;
        b = extensions.blocks(s.owner, s.storeId, "order", o.id());
        assertTrue(b.get(0).error().length() > 0);
        // Another store's order is not this store's resource.
        assertThrows(ResponseStatusException.class,
                () -> extensions.blocks(s.owner, s.storeId, "order", "nope"));
    }

    @Test
    void anActionIsPostedSignedWithItsCheckedValues() throws Exception {
        Shop s = shop("ext-action");
        Map app = register("tagger");
        String t = install(s, app);
        Map e = upload(s, t, "{\"target\":\"admin.order.block\",\"title\":\"Tag\",\"tree\":"
                + "{\"type\":\"stack\",\"children\":[{\"type\":\"field\",\"key\":\"qty\","
                + "\"label\":\"Quantity\",\"kind\":\"number\",\"required\":true},"
                + "{\"type\":\"button\",\"label\":\"Tag it\",\"action\":\"tag\"}]},"
                + "\"actionUrl\":\"https://app.example/act\"}");
        OrderDto o = place(s, People.customer(accounts, "ext-action-buyer"), null);
        Fake f = fake();
        f.reply = "{\"message\":\"Tagged\"}".getBytes("UTF-8");
        List<BlockValueDto> vals = new ArrayList<BlockValueDto>();
        vals.add(new BlockValueDto("qty", "abc"));
        String id = (String) e.get("id");
        assertThrows(ResponseStatusException.class, () -> extensions.action(s.owner, s.storeId,
                id, new ExtensionActionDto("tag", o.id(), vals)));
        assertThrows(ResponseStatusException.class, () -> extensions.action(s.owner, s.storeId,
                id, new ExtensionActionDto("other", o.id(), new ArrayList<BlockValueDto>())));
        assertEquals(0, f.urls.size());
        vals.clear();
        vals.add(new BlockValueDto("qty", "3"));
        ExtensionActionResultDto r = extensions.action(s.owner, s.storeId, id,
                new ExtensionActionDto("tag", o.id(), vals));
        assertTrue(r.ok());
        assertEquals("Tagged", r.message());
        assertEquals("https://app.example/act", f.urls.get(0));
        String body = new String(f.bodies.get(0), "UTF-8");
        assertTrue(body.indexOf("\"action\":\"tag\"") >= 0, body);
        assertEquals(Webhooks.signature(apps.signingKeyOf((String) app.get("id")),
                f.bodies.get(0)), f.header(Webhooks.SIGNATURE_HEADER));
    }

    @Test
    void theAppPageTokenVerifiesWithTheClientSecretForThatAppOnly() throws Exception {
        Shop s = shop("ext-session");
        Map app = register("embedded");
        install(s, app);
        AppSessionDto d = extensions.session(s.owner, s.storeId, installationOf(s, app));
        assertTrue(d.url().startsWith("https://app.example/admin?shop=" + s.storeId
                + "&session="), d.url());
        String token = d.url().substring(d.url().indexOf("&session=") + 9);
        String key = Apps.signingKey((String) app.get("clientSecret"));
        Map claims = AppSessionTokens.verify(token, key, (String) app.get("clientId"));
        assertEquals(s.storeId, claims.get("dest"));
        assertEquals(s.owner, claims.get("sub"));
        assertThrows(IOException.class, () -> AppSessionTokens.verify(token, key, "another"));
        assertThrows(IOException.class, () -> AppSessionTokens.verify(token, key + "0",
                (String) app.get("clientId")));
        assertThrows(IOException.class, () -> AppSessionTokens.verify(token.substring(0,
                token.length() - 2) + "xx", key, (String) app.get("clientId")));
    }

    @Test
    void theProxyForwardsSignedAndRefusesWhatItDoesNotServe() throws Exception {
        Shop s = shop("ext-proxy");
        Map app = register("proxied");
        String t = install(s, app);
        String proxy = "/api/v1/stores/" + s.storeId + "/app-proxy";
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(proxy), t),
                "{\"subpath\":\"Bad Path\",\"url\":\"https://app.example/p\"}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(proxy), t),
                "{\"subpath\":\"rewards\",\"url\":\"http://app.example/p\"}")));
        Rest.json(mvc, Rest.body(bearer(post(proxy), t),
                "{\"subpath\":\"rewards\",\"url\":\"https://app.example/p\"}"));
        Fake f = fake();
        f.type = "text/html; charset=utf-8";
        f.reply = "<p>Your points</p>".getBytes("UTF-8");
        MockResponse r = mvc.perform(get("/s/ext-proxy/apps/rewards/me?ref=a%20b")
                .header("Cookie", "bz_session=secret").header("Authorization", "Bearer x"))
                .andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        assertEquals("<p>Your points</p>", r.getContentAsString());
        String csp = r.getHeader("Content-Security-Policy");
        assertTrue(csp != null && csp.startsWith("sandbox"), String.valueOf(csp));
        String url = f.urls.get(0);
        assertTrue(url.startsWith("https://app.example/p/me?"), url);
        Map<String, String> q = queryOf(url);
        assertEquals("a b", q.get("ref"));
        assertEquals("ext-proxy", q.get("shop"));
        assertEquals("/apps/rewards", q.get("path_prefix"));
        String key = apps.signingKeyOf((String) app.get("id"));
        long now = System.currentTimeMillis() / 1000L;
        assertTrue(ProxySignature.verify(q, key, now));
        q.put("ref", "tampered");
        assertFalse(ProxySignature.verify(q, key, now));
        assertFalse(ProxySignature.verify(queryOf(url), key, now + 3600));
        for (String h : f.headers.get(0)) {
            assertFalse(h.startsWith("Cookie") || h.startsWith("Authorization"), h);
        }
        // Types it does not serve, and answers past the cap, are refused.
        f.type = "image/svg+xml";
        assertEquals(502, Rest.statusOf(mvc, get("/s/ext-proxy/apps/rewards")));
        f.type = "application/json";
        f.reply = new byte[AppProxyPages.MAX_BYTES + 1];
        assertEquals(502, Rest.statusOf(mvc, get("/s/ext-proxy/apps/rewards")));
        assertEquals(404, Rest.statusOf(mvc, get("/s/ext-proxy/apps/unknown")));
        Rest.json(mvc, bearer(delete(proxy), t));
        assertEquals(404, Rest.statusOf(mvc, get("/s/ext-proxy/apps/rewards")));
    }

    private static Map<String, String> queryOf(String url) throws IOException {
        Map<String, String> q = new LinkedHashMap<String, String>();
        String s = url.substring(url.indexOf('?') + 1);
        for (Map.Entry<String, String> e : OAuth2Parameters.parse(s).entrySet()) {
            q.put(e.getKey(), e.getValue());
        }
        return q;
    }

    @Test
    void checkoutFieldsAreCheckedAndReachTheOrder() throws Exception {
        Shop s = shop("ext-checkout");
        Map app = register("gifting");
        String t = install(s, app);
        Map e = upload(s, t, "{\"target\":\"checkout.block\",\"title\":\"Gift\",\"tree\":"
                + "{\"type\":\"stack\",\"children\":[{\"type\":\"field\",\"key\":\"note\","
                + "\"label\":\"Gift note\",\"kind\":\"text\"},{\"type\":\"field\",\"key\":"
                + "\"wrap\",\"label\":\"Wrap\",\"kind\":\"select\",\"options\":[\"Red\","
                + "\"Blue\"]}]}}");
        String prefix = "app:" + e.get("id") + ":";
        // The server-rendered checkout draws the same fields, under the ids
        // the checkout reads back.
        List<com.codenameone.examples.bazaar.extensions.Extensions.FormField> form =
                extensions.checkoutFields(s.storeId);
        assertEquals(2, form.size());
        assertEquals(prefix + "note", form.get(0).id);
        assertEquals("gifting: Gift note", form.get(0).label);
        assertEquals(java.util.Arrays.asList("Red", "Blue"), form.get(1).options);
        List<FieldAnswerDto> bad = new ArrayList<FieldAnswerDto>();
        bad.add(new FieldAnswerDto(prefix + "wrap", "Green"));
        assertThrows(ResponseStatusException.class,
                () -> place(s, People.customer(accounts, "ext-checkout-bad"), bad));
        List<FieldAnswerDto> good = new ArrayList<FieldAnswerDto>();
        good.add(new FieldAnswerDto(prefix + "note", "Happy birthday"));
        good.add(new FieldAnswerDto(prefix + "wrap", "Blue"));
        OrderDto o = place(s, People.customer(accounts, "ext-checkout-buyer"), good);
        List<String> seen = new ArrayList<String>();
        for (OrderFieldDto f : o.fields()) {
            seen.add(f.label() + "=" + f.value());
        }
        assertTrue(seen.contains("gifting: Gift note=Happy birthday"), seen.toString());
        assertTrue(seen.contains("gifting: Wrap=Blue"), seen.toString());
        // The shopper's storefront sees the block and nothing of the app's data.
        assertEquals(1, Rest.list(mvc, get("/api/shop/stores/ext-checkout/extensions"
                + "?target=checkout.block")).size());
    }

    @Test
    void storefrontBlocksPrintEscapedAndUninstallRemovesEverything() throws Exception {
        Shop s = shop("ext-store");
        Map app = register("banner");
        String t = install(s, app);
        upload(s, t, "{\"target\":\"storefront.block\",\"place\":\"product-below-price\","
                + "\"title\":\"Promo\",\"tree\":{\"type\":\"stack\",\"children\":["
                + "{\"type\":\"heading\",\"text\":\"<script>alert(1)</script>\"},"
                + "{\"type\":\"badge\",\"text\":\"Free gift\",\"tone\":\"success\"}]}}");
        Rest.json(mvc, Rest.body(bearer(post("/api/v1/stores/" + s.storeId + "/app-proxy"), t),
                "{\"subpath\":\"promo\",\"url\":\"https://app.example/p\"}"));
        String page = "/s/ext-store/products/ext-store-mug";
        MockResponse r = mvc.perform(get(page)).andReturn().getResponse();
        assertEquals(200, r.getStatus());
        String html = r.getContentAsString();
        assertTrue(html.indexOf("&lt;script&gt;alert(1)&lt;/script&gt;") >= 0, "escaped");
        assertFalse(html.indexOf("<script>alert(1)") >= 0);
        assertTrue(html.indexOf("Free gift") >= 0);
        apps.uninstall(s.owner, s.storeId, installationOf(s, app));
        assertEquals(0, db.query("SELECT id FROM app_extension WHERE store_id = ?",
                new Object[] {s.storeId}).size());
        assertEquals(0, db.query("SELECT id FROM app_proxy WHERE store_id = ?",
                new Object[] {s.storeId}).size());
        html = mvc.perform(get(page)).andReturn().getResponse().getContentAsString();
        assertFalse(html.indexOf("Free gift") >= 0);
        // A reinstall starts with nothing.
        String again = install(s, app);
        assertEquals(0, Rest.list(mvc, bearer(get(v1(s)), again)).size());
    }
}
