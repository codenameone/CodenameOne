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
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.AppOAuth;
import com.codenameone.examples.bazaar.platform.KeyApiOnly;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The app API's breadth: GraphQL mutations and reads, their REST twins,
/// scope enforcement with no partial writes, the API version header and
/// store isolation.
@BackendTest
class AppApiBreadthTest {
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

    private static final String PRODUCT = "{\"title\":\"Api Mug\",\"status\":\"ACTIVE\","
            + "\"productType\":\"Mug\",\"vendor\":\"Api\",\"tags\":[],\"options\":[{\"name\":"
            + "\"Kind\",\"values\":[\"V0\"]}],\"variants\":[{\"option1\":\"V0\",\"sku\":"
            + "\"API-MUG-0\",\"barcode\":\"\",\"priceCents\":1500,\"taxable\":true,"
            + "\"trackInventory\":true}]}";

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String v1(Shop s) {
        return "/api/v1/stores/" + s.storeId;
    }

    private static MockRequestBuilder bearer(MockRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    /// Registers an app asking for `scopes` and installs it on `s` with all of them.
    private String install(Shop s, String name, String... scopes) throws Exception {
        String developer = People.customer(accounts, "brd-" + name + "-dev");
        StringBuilder list = new StringBuilder();
        StringBuilder spaced = new StringBuilder();
        for (int iter = 0; iter < scopes.length; iter++) {
            list.append(iter == 0 ? "" : ",").append('"').append(scopes[iter]).append('"');
            spaced.append(iter == 0 ? "" : " ").append(scopes[iter]);
        }
        Map app = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps"),
                developer), "{\"name\":\"" + name + "\",\"description\":\"Does things\","
                + "\"appUrl\":\"https://app.example\",\"webhookUrl\":\"https://app.example/h\","
                + "\"redirectUris\":[\"" + REDIRECT + "\"],\"scopes\":[" + list + "],"
                + "\"status\":\"LISTED\"}"));
        StringBuilder v = new StringBuilder("v" + s.storeId + name);
        while (v.length() < 50) {
            v.append("-verifier");
        }
        String verifier = v.toString();
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/merchant/stores/"
                + s.storeId + "/apps/authorize"), s.owner), "{\"clientId\":\""
                + app.get("clientId") + "\",\"scope\":\"" + spaced + "\",\"redirectUri\":\""
                + REDIRECT + "\",\"state\":\"st\",\"codeChallenge\":\""
                + OAuth2Parameters.sha256(verifier) + "\",\"codeChallengeMethod\":\"S256\","
                + "\"approve\":true}"));
        String location = (String) to.get("location");
        String code = OAuth2Parameters.parse(location.substring(location.indexOf('?') + 1))
                .get("code");
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("grant_type", "authorization_code");
        f.put("client_id", (String) app.get("clientId"));
        f.put("client_secret", (String) app.get("clientSecret"));
        f.put("code", code);
        f.put("code_verifier", verifier);
        f.put("redirect_uri", REDIRECT);
        MockResponse r = mvc.perform(post(AppOAuth.TOKEN_PATH)
                .contentType("application/x-www-form-urlencoded")
                .content(OAuth2Parameters.format(f))).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return (String) Json.parseObject(r.getContentAsString()).get("access_token");
    }

    private MockResponse gql(Shop s, String token, String query, String variables)
            throws Exception {
        String body = "{\"query\":" + Json.write(query)
                + (variables == null ? "" : ",\"variables\":" + variables) + "}";
        return mvc.perform(Rest.body(bearer(post(v1(s) + "/graphql"), token), body))
                .andReturn().getResponse();
    }

    private static Map data(MockResponse r) throws Exception {
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return (Map) Json.parseObject(r.getContentAsString()).get("data");
    }

    private static String code(MockResponse r) throws Exception {
        Map e = (Map) ((List) Json.parseObject(r.getContentAsString()).get("errors")).get(0);
        return (String) ((Map) e.get("extensions")).get("code");
    }

    private long count(String sql, String arg) throws Exception {
        List rows = db.query(sql, new Object[] {arg});
        return Rows.number(((Map) rows.get(0)).get("n"));
    }

    @Test
    void everyMutationNeedsItsScopeAndADeniedOneWritesNothing() throws Exception {
        Shop s = shop("brd-scope");
        String t = install(s, "scopeapp", "write_products", "read_inventory");
        long before = count("SELECT COUNT(*) AS n FROM product WHERE store_id = ?", s.storeId);
        String[][] denied = {
            {"inventoryAdjust(input: {variantId: \"x\", delta: 1})", "write_inventory"},
            {"discountCreate(input: {code: \"X\"})", "write_discounts"},
            {"fulfillmentCreate(orderId: \"o\", input: {})", "write_fulfillments"},
            {"metafieldsSet(metafields: [])", "write_metafields"},
            {"customerUpdate(id: \"c\", input: {})", "write_customers"},
        };
        for (int iter = 0; iter < denied.length; iter++) {
            // Each beside an allowed productCreate: neither runs.
            MockResponse r = gql(s, t, "mutation($p: ProductInput!) { a: productCreate(input: $p)"
                    + " { id } b: " + denied[iter][0] + " { id } }", "{\"p\":" + PRODUCT + "}");
            assertEquals(403, r.getStatus(), r.getContentAsString());
            assertEquals("ACCESS_DENIED", code(r));
            assertTrue(r.getContentAsString().contains(denied[iter][1]), r.getContentAsString());
        }
        assertEquals(before, count("SELECT COUNT(*) AS n FROM product WHERE store_id = ?",
                s.storeId), "no partial writes");
        // Reads are held to their scopes too.
        assertEquals("ACCESS_DENIED", code(gql(s, t, "{ customers { id } }", null)));
        assertEquals(200, gql(s, t, "{ inventoryLevels { sku available } }", null).getStatus());
        // And the REST twins.
        assertEquals(403, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s) + "/discounts"), t),
                "{\"code\":\"X\"}")));
        assertEquals(403, Rest.statusOf(mvc, bearer(get(v1(s) + "/customers"), t)));
        assertEquals(403, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s)
                + "/inventory/adjustments"), t), "{\"variantId\":\"" + s.variantId
                + "\",\"delta\":1}")));
    }

    @Test
    void writesRoundTripThroughGraphQlAndRest() throws Exception {
        Webhooks.RecordingTransport rec = new Webhooks.RecordingTransport();
        webhooks.useTransport(rec);
        Shop s = shop("brd-write");
        OrderDto order = s.buy(People.customer(accounts, "brd-buyer"), 1);
        String t = install(s, "writeapp", "write_products", "write_inventory",
                "write_discounts", "write_fulfillments", "write_customers", "read_orders");
        Rest.json(mvc, Rest.body(bearer(post(v1(s) + "/webhooks"), t),
                "{\"topic\":\"products/update\",\"url\":\"https://app.example/p\"}"));

        Map made = (Map) data(gql(s, t, "mutation($p: ProductInput!) { p: productCreate("
                + "input: $p) { id title } }", "{\"p\":" + PRODUCT + "}")).get("p");
        String id = (String) made.get("id");
        assertEquals("Api Mug", made.get("title"));
        Map upd = (Map) data(gql(s, t, "mutation { productUpdate(id: \"" + id
                + "\", input: {title: \"Renamed Mug\"}) { title vendor } }", null))
                .get("productUpdate");
        assertEquals("Renamed Mug", upd.get("title"));
        assertEquals("Api", upd.get("vendor"), "members not given are kept");
        webhooks.deliver();
        boolean announced = false;
        for (String[] sent : rec.sent()) {
            announced |= sent[1].contains("X-Bazaar-Topic: products/update\n")
                    && sent[2].contains("Renamed Mug");
        }
        assertTrue(announced, "products/update went out");
        Map rest = Rest.json(mvc, Rest.body(bearer(put(v1(s) + "/products/" + id), t),
                "{\"vendor\":\"Rest\"}"));
        assertEquals("Rest", rest.get("vendor"));
        assertEquals("Renamed Mug", rest.get("title"));

        Map level = (Map) data(gql(s, t, "mutation { inventoryAdjust(input: {variantId: \""
                + s.variantId + "\", locationId: \"" + s.locationId + "\", delta: 5}) "
                + "{ available } }", null)).get("inventoryAdjust");
        long after = Rest.number(level, "available");
        Map level2 = Rest.json(mvc, Rest.body(bearer(post(v1(s) + "/inventory/adjustments"), t),
                "{\"variantId\":\"" + s.variantId + "\",\"locationId\":\"" + s.locationId
                + "\",\"delta\":-2}"));
        assertEquals(after - 2, Rest.number(level2, "available"));

        Map disc = (Map) data(gql(s, t, "mutation { discountCreate(input: {code: \"APP10\", "
                + "title: \"Ten\", kind: \"PERCENT\", amount: 10, active: true}) { id code } }",
                null)).get("discountCreate");
        assertEquals("APP10", disc.get("code"));
        List discounts = Rest.list(mvc, bearer(get(v1(s) + "/discounts"), t));
        assertTrue(String.valueOf(discounts).contains("APP10"), String.valueOf(discounts));
        List viaGql = (List) data(gql(s, t, "{ discounts { code } }", null)).get("discounts");
        assertEquals(discounts.size(), viaGql.size());

        Map detail = Rest.json(mvc, bearer(get(v1(s) + "/orders/" + order.id()), t));
        String lineId = (String) ((Map) ((List) detail.get("lines")).get(0)).get("id");
        Map ful = Rest.json(mvc, Rest.body(bearer(post(v1(s) + "/orders/" + order.id()
                + "/fulfillments"), t), "{\"locationId\":\"" + s.locationId + "\",\"method\":"
                + "\"SHIP\",\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}"));
        assertTrue(ful.get("id") != null);
        List fs = (List) data(gql(s, t, "{ fulfillments(orderId: \"" + order.id()
                + "\") { id } }", null)).get("fulfillments");
        assertEquals(1, fs.size());

        List cs = Rest.list(mvc, bearer(get(v1(s) + "/customers"), t));
        String customerId = (String) ((Map) cs.get(0)).get("id");
        Map cu = (Map) data(gql(s, t, "mutation { customerUpdate(id: \"" + customerId
                + "\", input: {note: \"From the app\", tags: [\"api\"]}) { note tags } }", null))
                .get("customerUpdate");
        assertEquals("From the app", cu.get("note"));
        Map back = Rest.json(mvc, Rest.body(bearer(put(v1(s) + "/customers/" + customerId), t),
                "{\"note\":\"Again\"}"));
        assertEquals("Again", back.get("note"));
        assertEquals("[api]", String.valueOf(back.get("tags")), "tags kept");
    }

    @Test
    void theApiVersionHeaderIsCheckedAndEchoed() throws Exception {
        Shop s = shop("brd-version");
        String t = install(s, "verapp", "read_products");
        MockResponse plain = mvc.perform(bearer(get(v1(s) + "/products"), t)).andReturn()
                .getResponse();
        assertEquals(200, plain.getStatus());
        assertEquals(KeyApiOnly.LATEST, plain.getHeader(KeyApiOnly.VERSION_HEADER));
        MockResponse older = mvc.perform(bearer(get(v1(s) + "/products"), t)
                .header(KeyApiOnly.VERSION_HEADER, "2026-07")).andReturn().getResponse();
        assertEquals(200, older.getStatus());
        assertEquals("2026-07", older.getHeader(KeyApiOnly.VERSION_HEADER));
        MockResponse unknown = mvc.perform(bearer(get(v1(s) + "/products"), t)
                .header(KeyApiOnly.VERSION_HEADER, "1999-01")).andReturn().getResponse();
        assertEquals(400, unknown.getStatus());
        MockResponse refused = mvc.perform(bearer(get(v1(s) + "/customers"), t)).andReturn()
                .getResponse();
        assertEquals(403, refused.getStatus());
        assertEquals(KeyApiOnly.LATEST, refused.getHeader(KeyApiOnly.VERSION_HEADER));
    }

    @Test
    void anAppCannotTouchAnotherStore() throws Exception {
        Shop a = shop("brd-iso-a");
        Shop b = shop("brd-iso-b");
        b.buy(People.customer(accounts, "brd-iso-buyer"), 1);
        String ta = install(a, "isoapp", "write_products", "write_inventory", "write_customers",
                "write_discounts", "write_fulfillments");
        String tb = install(b, "isoappb", "read_customers");
        // B's store through A's token.
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(post(v1(b) + "/products"), ta),
                PRODUCT)));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/inventory"), ta)));
        assertEquals(404, Rest.statusOf(mvc, bearer(get(v1(b) + "/discounts"), ta)));
        assertEquals(404, gql(b, ta, "mutation { discountCreate(input: {code: \"Z\"}) { id } }",
                null).getStatus());
        // B's records through A's own store.
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(put(v1(a) + "/products/"
                + b.productId), ta), "{\"title\":\"Stolen\"}")));
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(post(v1(a)
                + "/inventory/adjustments"), ta), "{\"variantId\":\"" + b.variantId
                + "\",\"delta\":5}")));
        String customerB = (String) ((Map) Rest.list(mvc, bearer(get(v1(b) + "/customers"),
                tb)).get(0)).get("id");
        assertEquals(404, Rest.statusOf(mvc, Rest.body(bearer(put(v1(a) + "/customers/"
                + customerB), ta), "{\"note\":\"x\"}")));
        assertEquals(404, gql(a, ta, "mutation { productUpdate(id: \"" + b.productId
                + "\", input: {title: \"Stolen\"}) { id } }", null).getStatus());
        assertEquals(0, count("SELECT COUNT(*) AS n FROM product WHERE title = ?", "Stolen"));
    }
}
