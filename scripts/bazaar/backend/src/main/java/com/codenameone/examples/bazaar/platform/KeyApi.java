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
package com.codenameone.examples.bazaar.platform;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.BulkJobDto;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.StoreRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The key API: what an integration reaches with a store's API key, under
/// `/api/v1/stores/{storeId}`. Every route asks [StoreApiKeys#require] for
/// the key, its store and the route's scope; a staff member's token is not
/// a key and is refused, as a key is refused everywhere else under `/api`.
///
/// | Route | Scope |
/// |---|---|
/// | `GET products`, `GET products/{id}` | `read_products` |
/// | `GET orders`, `GET orders/{id}` | `read_orders` |
/// | `POST bulk-operations` | by kind, see [BulkJobs#scopeOf] |
/// | `GET bulk-operations/{id}`, `GET bulk-operations/{id}/result.jsonl` | the kind's |
/// | `POST graphql` | `read_products` and/or `read_orders`, by what is asked |
///
/// Lists answer the oldest [#PAGE] after `since` (epoch milliseconds of
/// `createdAt`, 0 when absent); the last one's `createdAt` is the next page's
/// `since`.
///
/// The staff download of a bulk result, `result.jsonl` under the merchant
/// API, is here too: a document rather than JSON, like the PDFs.
@RestController
public class KeyApi {
    static final int PAGE = 100;

    private final StoreApiKeys keys;
    private final BulkJobs jobs;
    private final Catalog catalog;
    private final Orders orders;
    private final StoreRepository stores;
    private final DataSource db;

    public KeyApi(StoreApiKeys keys, BulkJobs jobs, Catalog catalog, Orders orders,
            StoreRepository stores, DataSource db) {
        this.keys = keys;
        this.jobs = jobs;
        this.catalog = catalog;
        this.orders = orders;
        this.stores = stores;
        this.db = db;
    }

    private String owner(String storeId) throws IOException {
        Store s = stores.findById(storeId);
        if (s == null) {
            throw new ResponseStatusException(404, "No such store");
        }
        return s.getOwnerUsername();
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }

    // ------------------------------------------------------------ products

    @GetMapping("/api/v1/stores/{storeId}/products")
    public HttpServer.Response products(@PathVariable("storeId") String storeId,
            @RequestParam(value = "since", required = false) String since) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_PRODUCTS);
        return json(productList(storeId, since, PAGE));
    }

    @GetMapping("/api/v1/stores/{storeId}/products/{productId}")
    public HttpServer.Response product(@PathVariable("storeId") String storeId,
            @PathVariable("productId") String productId) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_PRODUCTS);
        return json(catalog.merchantProduct(owner(storeId), storeId, productId));
    }

    List<Object> productList(String storeId, String since, int max) throws IOException {
        String owner = owner(storeId);
        List rows = db.query("SELECT id AS id FROM product WHERE store_id = ? AND created_at > ? "
                + "ORDER BY created_at", new Object[] {storeId, Long.valueOf(number(since))});
        List<Object> out = new ArrayList<Object>();
        for (int iter = 0; rows != null && iter < rows.size() && iter < max; iter++) {
            out.add(catalog.merchantProduct(owner, storeId,
                    Rows.text(((Map) rows.get(iter)).get("id"))));
        }
        return out;
    }

    // -------------------------------------------------------------- orders

    @GetMapping("/api/v1/stores/{storeId}/orders")
    public HttpServer.Response orderList(@PathVariable("storeId") String storeId,
            @RequestParam(value = "since", required = false) String since) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_ORDERS);
        return json(orderList(storeId, since, PAGE));
    }

    @GetMapping("/api/v1/stores/{storeId}/orders/{orderId}")
    public HttpServer.Response order(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_ORDERS);
        return json(orders.detail(owner(storeId), storeId, orderId));
    }

    List<Object> orderList(String storeId, String since, int max) throws IOException {
        String owner = owner(storeId);
        List rows = db.query("SELECT id AS id FROM store_order WHERE store_id = ? AND (channel <> ? OR location_id <> '') "
                + "AND created_at > ? ORDER BY created_at", new Object[] {storeId, Orders.DRAFT,
                    Long.valueOf(number(since))});
        List<Object> out = new ArrayList<Object>();
        for (int iter = 0; rows != null && iter < rows.size() && iter < max; iter++) {
            out.add(orders.detail(owner, storeId, Rows.text(((Map) rows.get(iter)).get("id"))));
        }
        return out;
    }

    // ------------------------------------------------------------- bulk

    @PostMapping("/api/v1/stores/{storeId}/bulk-operations")
    public HttpServer.Response startBulk(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        BulkJobDto wanted = Json.read(request.getBody() == null ? "{}" : request.getBody(),
                BulkJobDto.class);
        String kind = wanted == null ? null : wanted.kind();
        StoreApiKeys.Key key = keys.require(storeId, BulkJobs.scopeOf(kind));
        return json(jobs.start(storeId, key.owner(), wanted));
    }

    @GetMapping("/api/v1/stores/{storeId}/bulk-operations/{jobId}")
    public HttpServer.Response bulk(@PathVariable("storeId") String storeId,
            @PathVariable("jobId") String jobId) throws Exception {
        keys.require(storeId, null);
        BulkJobDto job = jobs.jobOf(storeId, jobId);
        keys.require(storeId, BulkJobs.scopeOf(job.kind()));
        return json(job);
    }

    @GetMapping("/api/v1/stores/{storeId}/bulk-operations/{jobId}/result.jsonl")
    public HttpServer.Response bulkResult(@PathVariable("storeId") String storeId,
            @PathVariable("jobId") String jobId) throws Exception {
        keys.require(storeId, null);
        BulkJobDto job = jobs.jobOf(storeId, jobId);
        keys.require(storeId, BulkJobs.scopeOf(job.kind()));
        return jsonl(jobs.output(storeId, jobId), jobId);
    }

    @GetMapping("/api/merchant/stores/{storeId}/bulk-jobs/{jobId}/result.jsonl")
    public HttpServer.Response staffResult(@PathVariable("storeId") String storeId,
            @PathVariable("jobId") String jobId) throws Exception {
        return jsonl(jobs.result(Caller.name(), storeId, jobId), jobId);
    }

    private static HttpServer.Response jsonl(String body, String jobId) throws IOException {
        return new HttpServer.Response(200, "application/x-ndjson; charset=utf-8",
                body.getBytes("UTF-8")).header("Cache-Control", "no-store")
                .header("Content-Disposition", "attachment; filename=\"bulk-" + jobId + ".jsonl\"");
    }

    // ------------------------------------------------------------ graphql

    /// A read-only GraphQL endpoint over products and orders; see [MiniGraphQl]
    /// for the subset of the language it reads.
    @PostMapping("/api/v1/stores/{storeId}/graphql")
    public HttpServer.Response graphql(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        keys.require(storeId, null);
        Map body = Json.parseObject(request.getBody() == null ? "{}" : request.getBody());
        Object query = body == null ? null : body.get("query");
        MiniGraphQl.Field root;
        try {
            root = MiniGraphQl.parse(query instanceof String ? (String) query : "");
        } catch (IllegalArgumentException bad) {
            return HttpServer.Response.json(400, Json.write(MiniGraphQl.error(bad.getMessage())));
        }
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<String, Object>();
        for (int iter = 0; iter < root.children.size(); iter++) {
            MiniGraphQl.Field f = root.children.get(iter);
            int first = Math.max(1, Math.min(PAGE, f.intArg("first", 20)));
            List<Object> found;
            if ("products".equals(f.name)) {
                keys.require(storeId, StoreApiKeys.READ_PRODUCTS);
                found = productList(storeId, null, first);
            } else if ("orders".equals(f.name)) {
                keys.require(storeId, StoreApiKeys.READ_ORDERS);
                found = orderList(storeId, null, first);
            } else {
                return HttpServer.Response.json(400, Json.write(MiniGraphQl.error(
                        "Unknown field " + f.name + "; the root has products and orders")));
            }
            List<Object> projected = new ArrayList<Object>();
            try {
                for (int i = 0; i < found.size(); i++) {
                    projected.add(MiniGraphQl.project(Json.parse(Json.write(found.get(i))), f));
                }
            } catch (IllegalArgumentException bad) {
                return HttpServer.Response.json(400, Json.write(MiniGraphQl.error(bad.getMessage())));
            }
            data.put(f.alias, projected);
        }
        java.util.Map<String, Object> answer = new java.util.LinkedHashMap<String, Object>();
        answer.put("data", data);
        return json(answer);
    }

    /// `since` as a number; -1 (before everything) when absent or not a
    /// number, since a row imported without a date carries the epoch, 0.
    private static long number(String s) {
        if (s == null || s.length() == 0 || s.length() > 18) {
            return -1;
        }
        long n = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }
}
