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
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.WebhookDto;
import com.codenameone.examples.bazaar.content.Metafields;
import com.codenameone.examples.bazaar.webhook.Webhooks;

/// What only an app's token reaches on the key API, beside what a key does:
///
/// | Route | Scope |
/// |---|---|
/// | `GET webhooks`, `DELETE webhooks/{id}` | none beyond the app's |
/// | `POST webhooks` | the topic's read scope ([#scopeOfTopic]) |
/// | `GET metafields?ownerKind=&ownerId=` | `read_metafields` (a key too) |
/// | `POST metafield-definitions`, `PUT metafields` | `write_metafields`, own namespace only |
///
/// An app's subscriptions are its own: they are signed with its key, and
/// neither the store's staff nor another app sees them.
@RestController
public class AppKeyApi {
    private static final String BASE = "/api/v1/stores/{storeId}";

    private final StoreApiKeys keys;
    private final Webhooks webhooks;
    private final Metafields metafields;
    private final Apps apps;

    public AppKeyApi(StoreApiKeys keys, Webhooks webhooks, Metafields metafields, Apps apps) {
        this.keys = keys;
        this.webhooks = webhooks;
        this.metafields = metafields;
        this.apps = apps;
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }

    /// The scope an app needs to follow `topic`.
    static String scopeOfTopic(String topic) {
        if (Webhooks.PRODUCTS_UPDATE.equals(topic)) {
            return StoreApiKeys.READ_PRODUCTS;
        }
        if (Webhooks.INVENTORY_LOW.equals(topic)) {
            return StoreApiKeys.READ_INVENTORY;
        }
        if (Webhooks.CUSTOMERS_CREATE.equals(topic)) {
            return StoreApiKeys.READ_CUSTOMERS;
        }
        if (topic != null && (topic.startsWith("orders/") || topic.startsWith("refunds/"))) {
            return StoreApiKeys.READ_ORDERS;
        }
        throw new ResponseStatusException(400, "The topic is one of " + Webhooks.TOPICS);
    }

    // -------------------------------------------------------------- webhooks

    @GetMapping(BASE + "/webhooks")
    public HttpServer.Response hooks(@PathVariable("storeId") String storeId) throws Exception {
        StoreApiKeys.Key k = keys.requireApp(storeId, null);
        return json(webhooks.appList(k.id));
    }

    @PostMapping(BASE + "/webhooks")
    public HttpServer.Response subscribe(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        WebhookDto wanted = Json.read(request.getBody() == null ? "{}" : request.getBody(),
                WebhookDto.class);
        keys.requireApp(storeId, null);
        StoreApiKeys.Key k = keys.requireApp(storeId,
                scopeOfTopic(wanted == null ? null : wanted.topic()));
        WebhookDto made = webhooks.appCreate(storeId, k.id, apps.signingKeyOf(k.appId), wanted);
        apps.audit(storeId, k.owner(), "APP_WEBHOOK_SUBSCRIBED", k.appId, made.topic() + " "
                + made.url());
        return json(made);
    }

    @DeleteMapping(BASE + "/webhooks/{webhookId}")
    public HttpServer.Response unsubscribe(@PathVariable("storeId") String storeId,
            @PathVariable("webhookId") String webhookId) throws Exception {
        StoreApiKeys.Key k = keys.requireApp(storeId, null);
        Object left = webhooks.appRemove(k.id, webhookId);
        apps.audit(storeId, k.owner(), "APP_WEBHOOK_REMOVED", k.appId, webhookId);
        return json(left);
    }

    // ------------------------------------------------------------ metafields

    @GetMapping(BASE + "/metafields")
    public HttpServer.Response values(@PathVariable("storeId") String storeId,
            @RequestParam(value = "ownerKind", required = false) String ownerKind,
            @RequestParam(value = "ownerId", required = false) String ownerId) throws Exception {
        keys.require(storeId, StoreApiKeys.READ_METAFIELDS);
        return json(metafields.valuesForApi(storeId, ownerKind, ownerId));
    }

    @PostMapping(BASE + "/metafield-definitions")
    public HttpServer.Response define(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = keys.requireApp(storeId, StoreApiKeys.WRITE_METAFIELDS);
        MetafieldDefDto made = metafields.defineForApp(storeId, k.appId, Json.read(
                request.getBody() == null ? "{}" : request.getBody(), MetafieldDefDto.class));
        apps.audit(storeId, k.owner(), "APP_METAFIELD_DEFINED", k.appId, made.key());
        return json(made);
    }

    @PutMapping(BASE + "/metafields")
    public HttpServer.Response put(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = keys.requireApp(storeId, StoreApiKeys.WRITE_METAFIELDS);
        MetafieldValueDto in = Json.read(request.getBody() == null ? "{}" : request.getBody(),
                MetafieldValueDto.class);
        Object out = metafields.putForApp(storeId, k.appId, in);
        apps.audit(storeId, k.owner(), "APP_METAFIELD_SET", k.appId, in == null ? ""
                : in.defId() + " on " + in.ownerId());
        return json(out);
    }
}
