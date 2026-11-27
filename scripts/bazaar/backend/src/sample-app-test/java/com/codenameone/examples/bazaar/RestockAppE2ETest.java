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
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AppSessionDto;
import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.ExtensionActionDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.extensions.Extensions;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import com.codenameone.examples.restock.RestockAlerts;
import com.codenameone.examples.restock.RestockEndpoints;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The sample third-party app, Restock Alerts (`scripts/bazaar/sample-app`),
/// installed into this server and used the way a store would use it: install,
/// webhook, admin block data and action, proxied signup, checkout function,
/// embedded page, uninstall.
///
/// Both run in this process, joined at the HTTP layer only. Bazaar's calls to
/// `https://restock.example/...` are handed to the app's routes as requests;
/// the app's calls to `https://bazaar.example/...` go through this server's
/// MockMvc. Nothing else of either side is touched by the other.
@BackendTest
class RestockAppE2ETest {
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
    @Autowired
    private Webhooks webhooks;

    private static final String BAZAAR = "https://bazaar.example";
    private static final String APP = "https://restock.example";

    /// The app's side of the wire: Bazaar's outbound calls land here.
    private RestockEndpoints endpoints;
    private final List<String> webhookStatuses = new ArrayList<String>();

    private HttpServer.Response toApp(String method, String url, List<String> headers,
            byte[] body) throws IOException {
        assertTrue(url.startsWith(APP + "/"), url);
        String target = url.substring(APP.length());
        Map<String, String> h = new LinkedHashMap<String, String>();
        for (int iter = 0; iter < headers.size(); iter++) {
            String line = headers.get(iter);
            int colon = line.indexOf(": ");
            h.put(line.substring(0, colon), line.substring(colon + 2));
        }
        HttpServer.Request r = HttpServer.Request.of(method, target, h,
                body == null ? new byte[0] : body);
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        if ("/restock/webhooks".equals(path)) {
            return endpoints.webhook(r);
        }
        if ("/restock/blocks/data".equals(path)) {
            return endpoints.blockData(r);
        }
        if ("/restock/blocks/action".equals(path)) {
            return endpoints.blockAction(r);
        }
        String proxy = "/restock/proxy/";
        if (path.startsWith(proxy)) {
            String rest = path.substring(proxy.length());
            int slash = rest.indexOf('/');
            return slash < 0 ? endpoints.proxyRoot(rest, r)
                    : endpoints.proxyPage(rest.substring(0, slash), rest.substring(slash + 1), r);
        }
        throw new IOException("No app route for " + path);
    }

    private static byte[] bodyOf(HttpServer.Response r) {
        return r.getBody() == null ? new byte[0] : r.getBody();
    }

    private void connect(RestockAlerts app) {
        extensions.useTransport(new Extensions.Transport() {
            @Override
            public Extensions.Reply send(String method, String url, List<String> headers,
                    byte[] body, int timeoutMillis, int maxBytes) throws IOException {
                HttpServer.Response r = toApp(method, url, headers, body);
                return new Extensions.Reply(r.getStatus(), r.getContentType(), bodyOf(r));
            }
        });
        webhooks.useTransport(new Webhooks.Transport() {
            @Override
            public int post(String url, List<String> headers, byte[] body) throws IOException {
                int status = toApp("POST", url, headers, body).getStatus();
                for (int iter = 0; iter < headers.size(); iter++) {
                    if (headers.get(iter).startsWith("X-Bazaar-Topic: ")) {
                        webhookStatuses.add(headers.get(iter).substring(16) + "=" + status);
                    }
                }
                return status;
            }
        });
        app.useHttp(new RestockAlerts.Http() {
            @Override
            public RestockAlerts.Answer send(String method, String url, List<String> headers,
                    byte[] body) throws IOException {
                assertTrue(url.startsWith(BAZAAR + "/"), url);
                MockRequestBuilder b = request(method, url.substring(BAZAAR.length()));
                for (int iter = 0; iter < headers.size(); iter++) {
                    String line = headers.get(iter);
                    int colon = line.indexOf(": ");
                    String name = line.substring(0, colon);
                    String value = line.substring(colon + 2);
                    if ("Content-Type".equals(name)) {
                        b.contentType(value);
                    } else {
                        b.header(name, value);
                    }
                }
                if (body != null) {
                    b.content(body);
                }
                try {
                    MockResponse r = mvc.perform(b).andReturn().getResponse();
                    return new RestockAlerts.Answer(r.getStatus(), r.getContentAsString());
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        });
    }

    private static Map<String, String> query(String url) {
        return OAuth2Parameters.parse(url.substring(url.indexOf('?') + 1));
    }

    @Test
    void installWebhookBlockProxyFunctionAndUninstall() throws Exception {
        Shop s = new Shop("restock-e2e", 10, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);

        // The developer registers the app; Bazaar shows the id and secret once.
        String developer = People.customer(accounts, "restock-e2e-dev");
        Map registered = Rest.json(mvc, Rest.body(People.asMerchant(post(
                "/api/developer/apps"), developer), "{\"name\":\"Restock Alerts\","
                + "\"description\":\"Tell shoppers when sold-out items return\","
                + "\"appUrl\":\"" + APP + "/restock/admin\",\"webhookUrl\":\"" + APP
                + "/restock/webhooks\",\"redirectUris\":[\"" + APP + "/restock/callback\"],"
                + "\"scopes\":[\"read_inventory\",\"write_extensions\",\"write_functions\"],"
                + "\"status\":\"LISTED\"}"));
        RestockAlerts app = new RestockAlerts(BAZAAR, APP, (String) registered.get("clientId"),
                (String) registered.get("clientSecret"));
        endpoints = new RestockEndpoints(app);
        connect(app);

        // 1. Install: the app's redirect carries PKCE; the owner approves; the
        //    browser comes back to the app's callback with the code.
        // GET /restock/install answers a redirect to this address.
        assertEquals(302, endpoints.install(s.storeId).getStatus());
        String consent = app.installUrl(s.storeId);
        Map<String, String> c = query(consent);
        assertTrue(consent.startsWith(BAZAAR + "/account/apps/authorize?"), consent);
        assertEquals("S256", c.get("code_challenge_method"));
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/merchant/stores/"
                + s.storeId + "/apps/authorize"), s.owner), "{\"clientId\":\""
                + c.get("client_id") + "\",\"scope\":\"" + c.get("scope") + "\",\"redirectUri\":\""
                + c.get("redirect_uri") + "\",\"state\":\"" + c.get("state")
                + "\",\"codeChallenge\":\"" + c.get("code_challenge")
                + "\",\"codeChallengeMethod\":\"S256\",\"approve\":true}"));
        Map<String, String> back = query((String) to.get("location"));
        HttpServer.Response done = endpoints.callback(back.get("code"), back.get("state"));
        assertEquals(200, done.getStatus(), new String(bodyOf(done), "UTF-8"));
        assertTrue(app.installed(s.storeId));
        // A replayed callback is refused: the state was used up.
        assertEquals(400, endpoints.callback(back.get("code"), back.get("state")).getStatus());
        String installation = (String) db.queryOne("SELECT id FROM app_installation WHERE "
                + "store_id = ? AND app_id = ?", new Object[] {s.storeId, registered.get("id")})
                .get("id");

        // 2. inventory/low reaches the app signed, and is believed.
        webhooks.emit(s.storeId, Webhooks.INVENTORY_LOW, "{\"storeId\":\"" + s.storeId
                + "\",\"variantId\":\"" + s.variantId + "\",\"available\":1,\"reorderPoint\":3}");
        webhooks.deliverDue(System.currentTimeMillis() + 1000);
        assertTrue(webhookStatuses.contains("inventory/low=200"), webhookStatuses.toString());
        assertEquals(1, app.lowStock(s.storeId).size());

        // 3. The storefront shows the app's block and its checkout field.
        String page = mvc.perform(get("/s/restock-e2e/products/restock-e2e-mug")).andReturn()
                .getResponse().getContentAsString();
        assertTrue(page.indexOf("Notify me when back in stock") >= 0, "storefront block");
        assertEquals(1, Rest.list(mvc, get("/api/shop/stores/restock-e2e/extensions"
                + "?target=checkout.block")).size());

        // 4. A shopper signs up through the app proxy; both pages are signed.
        MockResponse form = mvc.perform(get("/s/restock-e2e/apps/restock?product="
                + s.productId)).andReturn().getResponse();
        assertEquals(200, form.getStatus(), form.getContentAsString());
        assertTrue(form.getContentAsString().indexOf("/s/restock-e2e/apps/restock/join") >= 0);
        MockResponse joined = mvc.perform(get("/s/restock-e2e/apps/restock/join?product="
                + s.productId + "&email=ada%40shopper.example")).andReturn().getResponse();
        assertTrue(joined.getContentAsString().indexOf("ada@shopper.example") >= 0,
                joined.getContentAsString());
        // Straight to the app, without the store's signature, is refused.
        assertEquals(401, endpoints.proxyPage(s.storeId, "join", HttpServer.Request.of("GET",
                "/restock/proxy/" + s.storeId + "/join?product=x&email=e%40x.example&shop=a"
                + "&timestamp=1&signature=00", new LinkedHashMap<String, String>(),
                new byte[0])).getStatus());

        // 5. The admin product block reads "waiting" from the app, signed.
        extensions.clearCache();
        List<ExtensionBlockDto> blocks = extensions.blocks(s.owner, s.storeId, "product",
                s.productId);
        assertEquals(1, blocks.size());
        assertEquals("", blocks.get(0).error());
        assertEquals("1", valueOf(blocks.get(0).values(), "waiting"));

        // 6. "Notify now" writes to the waiting shopper and clears the count.
        assertEquals("Notified 1 shopper", extensions.action(s.owner, s.storeId,
                blocks.get(0).extensionId(), new ExtensionActionDto("notify", s.productId,
                        new ArrayList<BlockValueDto>())).message());
        assertEquals("[ada@shopper.example]", app.sent(s.storeId).toString());
        assertEquals(0, app.waiting(s.storeId, s.productId));

        // 7. The app's checkout function limits a cart to five of each.
        String buyer = People.customer(accounts, "restock-e2e-buyer");
        carts.add(s.storeId, buyer, null, new CartLineChangeDto(s.variantId, 6));
        CartDto cart = carts.estimate(s.storeId, buyer, null, new EstimateDto(Shop.NY, null));
        assertTrue(cart.problems().contains("At most five of each item while stock is short"),
                "" + cart.problems());

        // 8. The embedded admin page verifies the session Bazaar opens it with.
        AppSessionDto session = extensions.session(s.owner, s.storeId, installation);
        String token = query(session.url()).get("session");
        HttpServer.Response admin = endpoints.admin(token);
        assertEquals(200, admin.getStatus());
        assertTrue(new String(bodyOf(admin), "UTF-8").indexOf(s.owner) >= 0);
        assertEquals(401, endpoints.admin(token.substring(0, token.length() - 2) + "xx")
                .getStatus());

        // 9. Uninstall: app/uninstalled drops the token, shop/redact the rest.
        apps.uninstall(s.owner, s.storeId, installation);
        long now = System.currentTimeMillis();
        webhooks.deliverDue(now + 1000);
        assertTrue(webhookStatuses.contains("app/uninstalled=200"), webhookStatuses.toString());
        assertFalse(app.installed(s.storeId));
        long later = now + 49L * 60 * 60 * 1000;
        apps.sendShopRedacts(later);
        webhooks.deliverDue(later + 1000);
        assertTrue(app.privacyLog().contains("shop/redact " + s.storeId), "" + app.privacyLog());
        // The extensions went with the app.
        page = mvc.perform(get("/s/restock-e2e/products/restock-e2e-mug")).andReturn()
                .getResponse().getContentAsString();
        assertFalse(page.indexOf("Notify me when back in stock") >= 0);
    }

    private static String valueOf(List<BlockValueDto> values, String key) {
        for (BlockValueDto v : values) {
            if (key.equals(v.key())) {
                return v.value();
            }
        }
        return null;
    }
}
