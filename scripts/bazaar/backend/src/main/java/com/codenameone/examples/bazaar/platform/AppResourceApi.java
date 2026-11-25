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
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/// The REST twins of the GraphQL mutations and reads, under
/// `/api/v1/stores/{storeId}`, with the same scopes ([AppResources]):
///
/// | Route | Scope |
/// |---|---|
/// | `POST products`, `PUT products/{id}` (members given replace) | `write_products` |
/// | `GET inventory?locationId=&productId=` | `read_inventory` |
/// | `POST inventory/adjustments` | `write_inventory` |
/// | `GET discounts` | `read_discounts` |
/// | `POST discounts` | `write_discounts` |
/// | `GET orders/{id}/fulfillments` | `read_fulfillments` |
/// | `POST orders/{id}/fulfillments` | `write_fulfillments` |
/// | `GET customers`, `GET customers/{id}` | `read_customers` |
/// | `PUT customers/{id}` (members given replace) | `write_customers` |
@RestController
public class AppResourceApi {
    private static final String BASE = "/api/v1/stores/{storeId}";

    private final StoreApiKeys keys;
    private final AppResources resources;

    public AppResourceApi(StoreApiKeys keys, AppResources resources) {
        this.keys = keys;
        this.resources = resources;
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }

    private static Object body(HttpServer.Request request) throws java.io.IOException {
        return Json.parse(request.getBody() == null || request.getBody().length() == 0 ? "{}"
                : request.getBody());
    }

    private HttpServer.Response write(String storeId, String mutation, Map<String, Object> args)
            throws Exception {
        StoreApiKeys.Key k = keys.require(storeId, AppResources.scopeOfMutation(mutation));
        AppResources.Writes w = new AppResources.Writes();
        Object out = resources.writeOne(k, mutation, args, w);
        resources.afterWrites(storeId, w);
        return json(out);
    }

    private static Map<String, Object> args(Object input, String idName, String id) {
        Map<String, Object> a = new LinkedHashMap<String, Object>();
        a.put("input", input);
        if (idName != null) {
            a.put(idName, id);
        }
        return a;
    }

    // ------------------------------------------------------------ products

    @PostMapping(BASE + "/products")
    public HttpServer.Response createProduct(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return write(storeId, "productCreate", args(body(request), null, null));
    }

    @PutMapping(BASE + "/products/{productId}")
    public HttpServer.Response updateProduct(@PathVariable("storeId") String storeId,
            @PathVariable("productId") String productId, HttpServer.Request request)
            throws Exception {
        return write(storeId, "productUpdate", args(body(request), "id", productId));
    }

    // ----------------------------------------------------------- inventory

    @GetMapping(BASE + "/inventory")
    public HttpServer.Response levels(@PathVariable("storeId") String storeId,
            @RequestParam(value = "locationId", required = false) String locationId,
            @RequestParam(value = "productId", required = false) String productId)
            throws Exception {
        keys.require(storeId, StoreApiKeys.READ_INVENTORY);
        return json(resources.levels(storeId, locationId, productId));
    }

    @PostMapping(BASE + "/inventory/adjustments")
    public HttpServer.Response adjust(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return write(storeId, "inventoryAdjust", args(body(request), null, null));
    }

    // ----------------------------------------------------------- discounts

    @GetMapping(BASE + "/discounts")
    public HttpServer.Response discounts(@PathVariable("storeId") String storeId)
            throws Exception {
        keys.require(storeId, StoreApiKeys.READ_DISCOUNTS);
        return json(resources.discounts(storeId));
    }

    @PostMapping(BASE + "/discounts")
    public HttpServer.Response createDiscount(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return write(storeId, "discountCreate", args(body(request), null, null));
    }

    // -------------------------------------------------------- fulfillments

    @GetMapping(BASE + "/orders/{orderId}/fulfillments")
    public HttpServer.Response fulfillments(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_FULFILLMENTS);
        return json(resources.fulfillmentsOf(storeId, orderId));
    }

    @PostMapping(BASE + "/orders/{orderId}/fulfillments")
    public HttpServer.Response fulfil(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        return write(storeId, "fulfillmentCreate", args(body(request), "orderId", orderId));
    }

    // ----------------------------------------------------------- customers

    @GetMapping(BASE + "/customers")
    public HttpServer.Response customers(@PathVariable("storeId") String storeId)
            throws Exception {
        keys.require(storeId, StoreApiKeys.READ_CUSTOMERS);
        return json(resources.customers(storeId, KeyApi.PAGE));
    }

    @GetMapping(BASE + "/customers/{customerId}")
    public HttpServer.Response customer(@PathVariable("storeId") String storeId,
            @PathVariable("customerId") String customerId) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_CUSTOMERS);
        return json(resources.customer(storeId, customerId));
    }

    @PutMapping(BASE + "/customers/{customerId}")
    public HttpServer.Response updateCustomer(@PathVariable("storeId") String storeId,
            @PathVariable("customerId") String customerId, HttpServer.Request request)
            throws Exception {
        return write(storeId, "customerUpdate", args(body(request), "id", customerId));
    }
}
