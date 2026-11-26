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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Merchant mode: webhooks. Each delivery is a JSON POST with the headers
/// `X-Bazaar-Topic` and `X-Bazaar-Signature: sha256=<hex HMAC-SHA256 of the
/// body keyed with the webhook's secret>`. A delivery that is not answered 2xx
/// is tried again after 1, 2, 4, 8 and 16 minutes, six attempts in all.
@RestClient
public interface WebhookApi {
    /// The topics a webhook may subscribe to.
    @GET("/api/merchant/stores/{storeId}/webhook-topics")
    void topics(@Path("storeId") String storeId, OnComplete<Response<List<String>>> callback);

    @GET("/api/merchant/stores/{storeId}/webhooks")
    void list(@Path("storeId") String storeId, OnComplete<Response<List<WebhookDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/webhooks")
    void create(@Path("storeId") String storeId, @Body WebhookDto webhook,
            OnComplete<Response<WebhookDto>> callback);

    @POST("/api/merchant/stores/{storeId}/webhooks/{webhookId}")
    void save(@Path("storeId") String storeId, @Path("webhookId") String webhookId,
            @Body WebhookDto webhook, OnComplete<Response<WebhookDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/webhooks/{webhookId}")
    void remove(@Path("storeId") String storeId, @Path("webhookId") String webhookId,
            OnComplete<Response<List<WebhookDto>>> callback);

    /// The newest deliveries first.
    @GET("/api/merchant/stores/{storeId}/webhooks/{webhookId}/deliveries")
    void deliveries(@Path("storeId") String storeId, @Path("webhookId") String webhookId,
            OnComplete<Response<List<WebhookDeliveryDto>>> callback);

    /// Sends a sample payload now and answers how it went.
    @POST("/api/merchant/stores/{storeId}/webhooks/{webhookId}/test")
    void test(@Path("storeId") String storeId, @Path("webhookId") String webhookId,
            OnComplete<Response<WebhookDeliveryDto>> callback);

    /// Sends a delivery again now, whatever became of it.
    @POST("/api/merchant/stores/{storeId}/webhook-deliveries/{deliveryId}/redeliver")
    void redeliver(@Path("storeId") String storeId, @Path("deliveryId") String deliveryId,
            OnComplete<Response<WebhookDeliveryDto>> callback);
}
