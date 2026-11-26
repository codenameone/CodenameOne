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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.BulkJobs;
import com.codenameone.examples.bazaar.platform.MiniGraphQl;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The developer platform: scoped API keys, the key API under `/api/v1`,
/// bulk operations and the read-only GraphQL endpoint.
@BackendTest
class PlatformApiTest {
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
    private BulkJobs bulk;
    @Autowired
    private com.codenameone.examples.bazaar.order.Orders orderService;
    @Autowired
    private com.codenameone.examples.bazaar.ops.Reports reports;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String m(Shop s) {
        return "/api/merchant/stores/" + s.storeId;
    }

    private Map key(Shop s, String... scopes) throws Exception {
        StringBuilder list = new StringBuilder();
        for (int iter = 0; iter < scopes.length; iter++) {
            list.append(iter == 0 ? "" : ",").append('"').append(scopes[iter]).append('"');
        }
        return Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/api-keys"), s.owner),
                "{\"name\":\"Feed\",\"scopes\":[" + list + "]}"));
    }

    private static MockRequestBuilder withKey(MockRequestBuilder b, String key) {
        return b.header("Authorization", "Bearer " + key);
    }

    @Test
    void keysAreScopedHashedAndConfinedToTheKeyApi() throws Exception {
        Shop a = shop("pk-a");
        Shop b = shop("pk-b");
        Map made = key(a, "read_products");
        String secret = (String) made.get("secret");
        assertTrue(secret.startsWith("bzk_"), secret);
        // Kept as a hash only, and never answered again.
        Map row = db.queryOne("SELECT key_hash AS h FROM store_api_key WHERE id = ?",
                new Object[] {made.get("id")});
        assertFalse(Rows.text(row.get("h")).contains(secret));
        List listed = Rest.list(mvc, People.asMerchant(get(m(a) + "/api-keys"), a.owner));
        assertEquals("", ((Map) listed.get(0)).get("secret"));
        assertTrue(((String) ((Map) listed.get(0)).get("display")).startsWith("bzk_"));

        // The scope it has, at its own store.
        MockResponse ok = mvc.perform(withKey(get("/api/v1/stores/" + a.storeId + "/products"),
                secret)).andReturn().getResponse();
        assertEquals(200, ok.getStatus(), ok.getContentAsString());
        assertTrue(ok.getContentAsString().contains(a.productId));
        Map used = db.queryOne("SELECT last_used_at AS t FROM store_api_key WHERE id = ?",
                new Object[] {made.get("id")});
        assertTrue(Rows.number(used.get("t")) > 0);
        // Not a scope it lacks, not another store, not outside /api/v1.
        assertEquals(403, Rest.statusOf(mvc, withKey(get("/api/v1/stores/" + a.storeId
                + "/orders"), secret)));
        assertEquals(404, Rest.statusOf(mvc, withKey(get("/api/v1/stores/" + b.storeId
                + "/products"), secret)));
        assertEquals(403, Rest.statusOf(mvc, withKey(get(m(a) + "/products"), secret)));
        assertEquals(403, Rest.statusOf(mvc, withKey(get("/api/account/two-factor"), secret)));
        assertEquals(401, Rest.statusOf(mvc, withKey(get("/api/v1/stores/" + a.storeId
                + "/products"), "bzk_notakey")));
        // A staff token is not a key.
        assertEquals(401, Rest.statusOf(mvc, People.asMerchant(get("/api/v1/stores/" + a.storeId
                + "/products"), a.owner)));

        // Revoked at once.
        Rest.list(mvc, People.asMerchant(delete(m(a) + "/api-keys/" + made.get("id")), a.owner));
        assertEquals(401, Rest.statusOf(mvc, withKey(get("/api/v1/stores/" + a.storeId
                + "/products"), secret)));
    }

    @Test
    void onlyTheOwnerManagesKeysAndScopesAreChecked() throws Exception {
        Shop a = shop("pk-own");
        String manager = People.merchant(accounts, "pk-own-mgr");
        Rest.json(mvc, People.asMerchant(Rest.body(post(m(a) + "/staff"), "{\"username\":\""
                + manager + "\",\"role\":\"MANAGER\",\"pin\":\"\"}"), a.owner));
        assertEquals(403, Rest.statusOf(mvc, People.asMerchant(get(m(a) + "/api-keys"), manager)));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(a)
                + "/api-keys"), a.owner), "{\"name\":\"x\",\"scopes\":[\"write_everything\"]}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(a)
                + "/api-keys"), a.owner), "{\"name\":\"x\",\"scopes\":[]}")));
    }

    @Test
    void bulkExportsAndImportsProductsAsJsonLines() throws Exception {
        Shop a = shop("pk-bulk");
        Shop b = shop("pk-bulk-b");
        String secret = (String) key(a, "read_products", "write_products").get("secret");
        String base = "/api/v1/stores/" + a.storeId + "/bulk-operations";

        // An export, run by the worker, downloaded as JSON Lines.
        Map job = Rest.json(mvc, Rest.body(withKey(post(base), secret),
                "{\"kind\":\"EXPORT_PRODUCTS\"}"));
        assertEquals("QUEUED", job.get("status"));
        assertEquals(409, Rest.statusOf(mvc, withKey(get(base + "/" + job.get("id")
                + "/result.jsonl"), secret)));
        assertTrue(bulk.runQueued() >= 1);
        Map done = Rest.json(mvc, withKey(get(base + "/" + job.get("id")), secret));
        assertEquals("DONE", done.get("status"));
        assertEquals(1L, Rest.number(done, "lineCount"));
        MockResponse file = mvc.perform(withKey(get(base + "/" + job.get("id") + "/result.jsonl"),
                secret)).andReturn().getResponse();
        assertEquals(200, file.getStatus());
        assertTrue(file.getContentType().startsWith("application/x-ndjson"));
        Map exported = Json.parseObject(file.getContentAsString().trim());
        assertEquals(a.productId, exported.get("id"));

        // An import: one update, then a product with no variant, one line
        // naming another store's product and one that is not JSON -- each
        // reported alone, and the good line still applied.
        String update = Json.write(exported).replace("\"Mug\"", "\"Big mug\"");
        String foreign = Json.write(exported).replace(a.productId, b.productId);
        String input = update + "\n" + "{\"title\":\"Plate\",\"status\":\"DRAFT\",\"variants\":[]}"
                + "\n" + foreign + "\nnot json\n";
        Map imp = Rest.json(mvc, Rest.body(withKey(post(base), secret),
                Json.write(java.util.Collections.singletonMap("kind", "IMPORT_PRODUCTS"))
                        .replace("}", ",\"input\":" + Json.write(input) + "}")));
        bulk.runQueued();
        Map result = Rest.json(mvc, withKey(get(base + "/" + imp.get("id")), secret));
        assertEquals("DONE", result.get("status"), String.valueOf(result));
        assertEquals(3L, Rest.number(result, "errorCount"), String.valueOf(result));
        List errors = (List) result.get("errors");
        assertTrue(((String) errors.get(0)).startsWith("line 2:"), String.valueOf(errors));
        assertTrue(((String) errors.get(1)).startsWith("line 3:"), String.valueOf(errors));
        assertTrue(((String) errors.get(2)).startsWith("line 4:"), String.valueOf(errors));
        Map renamed = db.queryOne("SELECT title AS t FROM product WHERE id = ?",
                new Object[] {a.productId});
        assertEquals("Big mug", Rows.text(renamed.get("t")));
        Map untouched = db.queryOne("SELECT title AS t, store_id AS s FROM product WHERE id = ?",
                new Object[] {b.productId});
        assertEquals("Mug", Rows.text(untouched.get("t")));
        assertEquals(b.storeId, Rows.text(untouched.get("s")));

        // The kind's scope decides: a read-only key cannot import, and cannot
        // read an import's job either.
        String reader = (String) key(a, "read_products").get("secret");
        assertEquals(403, Rest.statusOf(mvc, Rest.body(withKey(post(base), reader),
                "{\"kind\":\"IMPORT_PRODUCTS\",\"input\":\"{}\"}")));
        assertEquals(403, Rest.statusOf(mvc, withKey(get(base + "/" + imp.get("id")), reader)));
        assertEquals(403, Rest.statusOf(mvc, Rest.body(withKey(post(base), reader),
                "{\"kind\":\"EXPORT_ORDERS\"}")));

        // Staff see the jobs, and download through the merchant API.
        List jobs = Rest.list(mvc, People.asMerchant(get(m(a) + "/bulk-jobs"), a.owner));
        assertEquals(2, jobs.size());
        MockResponse staff = mvc.perform(People.asMerchant(get(m(a) + "/bulk-jobs/"
                + job.get("id") + "/result.jsonl"), a.owner)).andReturn().getResponse();
        assertEquals(200, staff.getStatus());
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(get(m(b) + "/bulk-jobs/"
                + job.get("id")), b.owner)));
    }

    @Test
    void ordersExportAndTheKeyApiReadOrders() throws Exception {
        Shop a = shop("pk-ord");
        String buyer = People.customer(accounts, "pk-ord-buyer");
        a.buy(buyer, 1);
        // A draft not yet turned into an order is OPEN on the DRAFT channel:
        // no export, key API or report counts it.
        orderService.createDraft(a.owner, a.storeId, new com.codenameone.examples.bazaar.api
                .DraftOrderDto("draft@example.com", java.util.Collections.singletonList(
                    new com.codenameone.examples.bazaar.api.CartLineChangeDto(a.variantId, 1)),
                    null, null, "phone sale"));
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        String tomorrow = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).toString();
        assertEquals(1, reports.run(a.owner, a.storeId,
                new com.codenameone.examples.bazaar.api.ReportSpecDto("Orders", "channel", "",
                    "orders", "", "", today, tomorrow)).total());
        String secret = (String) key(a, "read_orders").get("secret");
        List orders = (List) Json.parse(mvc.perform(withKey(get("/api/v1/stores/" + a.storeId
                + "/orders"), secret)).andReturn().getResponse().getContentAsString());
        assertEquals(1, orders.size());
        Map job = Rest.json(mvc, Rest.body(withKey(post("/api/v1/stores/" + a.storeId
                + "/bulk-operations"), secret), "{\"kind\":\"EXPORT_ORDERS\"}"));
        bulk.runQueued();
        Map done = Rest.json(mvc, withKey(get("/api/v1/stores/" + a.storeId + "/bulk-operations/"
                + job.get("id")), secret));
        assertEquals("DONE", done.get("status"));
        assertEquals(1L, Rest.number(done, "lineCount"));
    }

    @Test
    void graphqlReadsWhatTheKeyMayRead() throws Exception {
        Shop a = shop("pk-gql");
        String secret = (String) key(a, "read_products").get("secret");
        String url = "/api/v1/stores/" + a.storeId + "/graphql";
        Map answer = Rest.json(mvc, Rest.body(withKey(post(url), secret),
                "{\"query\":\"{ items: products(first: 5) { id title } }\"}"));
        List items = (List) ((Map) answer.get("data")).get("items");
        assertEquals(1, items.size());
        Map item = (Map) items.get(0);
        assertEquals(a.productId, item.get("id"));
        assertEquals(2, item.size(), "only the fields asked for");
        assertEquals(403, Rest.statusOf(mvc, Rest.body(withKey(post(url), secret),
                "{\"query\":\"{ orders { id } }\"}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(withKey(post(url), secret),
                "{\"query\":\"mutation { deleteProduct(id: 1) { id } }\"}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(withKey(post(url), secret),
                "{\"query\":\"{ products { nope } }\"}")));
    }

    @Test
    void theGraphQlSubsetParses() {
        MiniGraphQl.Field root = MiniGraphQl.parse(
                "query Recent { latest: orders(first: 2) { number lines { title } } }");
        MiniGraphQl.Field f = root.children.get(0);
        assertEquals("orders", f.name);
        assertEquals("latest", f.alias);
        assertEquals(2, f.intArg("first", 0));
        assertEquals("title", f.children.get(1).children.get(0).name);
        assertThrows(IllegalArgumentException.class, () -> MiniGraphQl.parse(
                "{ products { ...Parts } }"));
        assertThrows(IllegalArgumentException.class, () -> MiniGraphQl.parse(
                "mutation { productCreate(input: {}) { id } }"));
        assertThrows(IllegalArgumentException.class, () -> MiniGraphQl.parse(
                "{ products(first: 1.5) { id } }"));
        java.util.Map<String, Object> vars = new java.util.HashMap<String, Object>();
        vars.put("n", Integer.valueOf(3));
        MiniGraphQl.Operation op = MiniGraphQl.parseOperation(
                "mutation Go($n: Int!) { a: x(first: $n, s: \"q\\\"\", o: {k: [1, true, null, RED]}) }",
                vars);
        assertTrue(op.mutation);
        MiniGraphQl.Field x = op.root.children.get(0);
        assertEquals(3, x.intArg("first", 0));
        assertEquals("q\"", x.stringArg("s"));
        assertEquals("[1, true, null, RED]", String.valueOf(((java.util.Map) x.args.get("o")).get("k")));
        assertThrows(IllegalArgumentException.class, () -> MiniGraphQl.parse("{ products { id }"));
        StringBuilder deep = new StringBuilder();
        for (int iter = 0; iter < 20; iter++) {
            deep.append("{ a ");
        }
        assertThrows(IllegalArgumentException.class, () -> MiniGraphQl.parse(deep.toString()));
        assertNull(MiniGraphQl.project("x", root.children.get(0)));
        assertNotNull(MiniGraphQl.error("e").get("errors"));
    }
}
