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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The key API's GraphQL endpoint, `POST /api/v1/stores/{storeId}/graphql`,
/// over the subset [MiniGraphQl] reads. `{"query": ..., "variables": {...}}`.
///
/// | Query field | Scope | Arguments |
/// |---|---|---|
/// | `products`, `orders` | `read_products`, `read_orders` | `first` |
/// | `customers` | `read_customers` | `first` |
/// | `inventoryLevels` | `read_inventory` | `first`, `locationId`, `productId` |
/// | `discounts` | `read_discounts` | `first` |
/// | `fulfillments` | `read_fulfillments` | `orderId` |
///
/// The mutations and their scopes are [AppResources]'s. Every field of the
/// operation is checked before any runs: one the token may not use answers
/// 403 with an `ACCESS_DENIED` error and nothing is read or written. The
/// fields of a mutation then run in one transaction, so a failing one rolls
/// back the rest, and the webhooks go out only after it committed.
@RestController
public class AppGraphQl {
    /// The `extensions.code` of a field the token's scopes do not cover.
    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    private final StoreApiKeys keys;
    private final KeyApi keyApi;
    private final AppResources resources;

    public AppGraphQl(StoreApiKeys keys, KeyApi keyApi, AppResources resources) {
        this.keys = keys;
        this.keyApi = keyApi;
        this.resources = resources;
    }

    /// The scope a query field needs, or null for one there is not.
    static String scopeOfQuery(String name) {
        if ("products".equals(name)) {
            return StoreApiKeys.READ_PRODUCTS;
        }
        if ("orders".equals(name)) {
            return StoreApiKeys.READ_ORDERS;
        }
        if ("customers".equals(name)) {
            return StoreApiKeys.READ_CUSTOMERS;
        }
        if ("inventoryLevels".equals(name)) {
            return StoreApiKeys.READ_INVENTORY;
        }
        if ("discounts".equals(name)) {
            return StoreApiKeys.READ_DISCOUNTS;
        }
        if ("fulfillments".equals(name)) {
            return StoreApiKeys.READ_FULFILLMENTS;
        }
        return null;
    }

    private static HttpServer.Response answer(int status, Object body) {
        return HttpServer.Response.json(status, Json.write(body)).header("Cache-Control",
                "no-store");
    }

    @PostMapping("/api/v1/stores/{storeId}/graphql")
    public HttpServer.Response graphql(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = keys.require(storeId, null);
        Map body = Json.parseObject(request.getBody() == null ? "{}" : request.getBody());
        Object query = body == null ? null : body.get("query");
        Object variables = body == null ? null : body.get("variables");
        MiniGraphQl.Operation op;
        try {
            op = MiniGraphQl.parseOperation(query instanceof String ? (String) query : "",
                    variables instanceof Map ? (Map) variables : null);
        } catch (IllegalArgumentException bad) {
            return answer(400, MiniGraphQl.error(bad.getMessage()));
        }
        List<MiniGraphQl.Field> fields = op.root.children;
        for (int iter = 0; iter < fields.size(); iter++) {
            MiniGraphQl.Field f = fields.get(iter);
            String scope = op.mutation ? AppResources.scopeOfMutation(f.name)
                    : scopeOfQuery(f.name);
            if (scope == null) {
                return answer(400, MiniGraphQl.error("Unknown field " + f.name + (op.mutation
                        ? "; the mutations are productCreate, productUpdate, inventoryAdjust, "
                        + "discountCreate, fulfillmentCreate, metafieldsSet and customerUpdate"
                        : "; the root has products, orders, customers, inventoryLevels, "
                        + "discounts and fulfillments")));
            }
            boolean appOnly = "metafieldsSet".equals(f.name) && k.appId == null;
            if (appOnly || !StoreApiKeys.has(k.scopes, scope)) {
                return answer(403, MiniGraphQl.error(appOnly ? f.name
                        + " takes an app's access token" : "The token lacks the scope "
                        + scope + " for " + f.name, ACCESS_DENIED));
            }
        }
        Map<String, Object> data;
        try {
            if (op.mutation) {
                AppResources.Writes w = new AppResources.Writes();
                Map<String, Object> raw = resources.writeAll(k, fields, w);
                resources.afterWrites(storeId, w);
                data = new LinkedHashMap<String, Object>();
                for (int iter = 0; iter < fields.size(); iter++) {
                    MiniGraphQl.Field f = fields.get(iter);
                    data.put(f.alias, MiniGraphQl.project(Json.parse(Json.write(
                            raw.get(f.alias))), f));
                }
            } else {
                data = new LinkedHashMap<String, Object>();
                for (int iter = 0; iter < fields.size(); iter++) {
                    MiniGraphQl.Field f = fields.get(iter);
                    data.put(f.alias, MiniGraphQl.project(Json.parse(Json.write(read(storeId,
                            f))), f));
                }
            }
        } catch (IllegalArgumentException bad) {
            return answer(400, MiniGraphQl.error(bad.getMessage()));
        } catch (ResponseStatusException refused) {
            return answer(refused.getStatus(), MiniGraphQl.error(refused.getReason(),
                    refused.getStatus() == 404 ? "NOT_FOUND" : "BAD_USER_INPUT"));
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("data", data);
        return answer(200, out);
    }

    private Object read(String storeId, MiniGraphQl.Field f) throws Exception {
        int first = Math.max(1, Math.min(KeyApi.PAGE, f.intArg("first", 20)));
        if ("products".equals(f.name)) {
            return keyApi.productList(storeId, null, first);
        }
        if ("orders".equals(f.name)) {
            return keyApi.orderList(storeId, null, first);
        }
        if ("customers".equals(f.name)) {
            return resources.customers(storeId, first);
        }
        if ("inventoryLevels".equals(f.name)) {
            return firstOf(resources.levels(storeId, f.stringArg("locationId"),
                    f.stringArg("productId")), first);
        }
        if ("discounts".equals(f.name)) {
            return firstOf(resources.discounts(storeId), first);
        }
        return resources.fulfillmentsOf(storeId, f.stringArg("orderId"));
    }

    private static List<Object> firstOf(List all, int max) {
        List<Object> out = new ArrayList<Object>();
        for (int iter = 0; iter < all.size() && iter < max; iter++) {
            out.add(all.get(iter));
        }
        return out;
    }
}
