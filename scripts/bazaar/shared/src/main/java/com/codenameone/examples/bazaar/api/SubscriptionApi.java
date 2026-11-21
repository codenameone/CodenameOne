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
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Subscriptions: selling plans per product, the merchant's view of every
/// subscription and its renewal calendar, and the shopper's own.
@RestClient
public interface SubscriptionApi {
    @GET("/api/merchant/stores/{storeId}/selling-plans")
    void plans(@Path("storeId") String storeId,
            OnComplete<Response<List<SellingPlanDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/selling-plans")
    void createPlan(@Path("storeId") String storeId,
            @Body SellingPlanDto body,
            OnComplete<Response<SellingPlanDto>> callback);

    @POST("/api/merchant/stores/{storeId}/selling-plans/{planId}")
    void savePlan(@Path("storeId") String storeId,
            @Path("planId") String planId,
            @Body SellingPlanDto body,
            OnComplete<Response<SellingPlanDto>> callback);

    @GET("/api/merchant/stores/{storeId}/subscriptions")
    void list(@Path("storeId") String storeId,
            OnComplete<Response<List<SubscriptionDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/subscriptions/{subscriptionId}")
    void detail(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/merchant/stores/{storeId}/subscriptions/{subscriptionId}/pause")
    void pause(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/merchant/stores/{storeId}/subscriptions/{subscriptionId}/resume")
    void resume(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/merchant/stores/{storeId}/subscriptions/{subscriptionId}/skip")
    void skip(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/merchant/stores/{storeId}/subscriptions/{subscriptionId}/cancel")
    void cancel(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    /// Renewals due from `from` (inclusive) to `to` (exclusive), epoch millis; at most 92 days.
    @GET("/api/merchant/stores/{storeId}/subscription-calendar")
    void calendar(@Path("storeId") String storeId,
            @Query("from") String from,
            @Query("to") String to,
            OnComplete<Response<List<RenewalDto>>> callback);

    @GET("/api/shop/stores/{storeId}/products/{productId}/selling-plans")
    void productPlans(@Path("storeId") String storeId,
            @Path("productId") String productId,
            OnComplete<Response<List<SellingPlanDto>>> callback);

    /// Subscribes: the first delivery is ordered and charged now.
    @POST("/api/shop/stores/{storeId}/subscriptions")
    void subscribe(@Path("storeId") String storeId,
            @Body SubscribeDto body,
            OnComplete<Response<SubscriptionDto>> callback);

    @GET("/api/shop/stores/{storeId}/subscriptions/me")
    void mine(@Path("storeId") String storeId,
            OnComplete<Response<List<SubscriptionDto>>> callback);

    @POST("/api/shop/stores/{storeId}/subscriptions/{subscriptionId}/pause")
    void pauseMine(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/shop/stores/{storeId}/subscriptions/{subscriptionId}/resume")
    void resumeMine(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/shop/stores/{storeId}/subscriptions/{subscriptionId}/skip")
    void skipMine(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/shop/stores/{storeId}/subscriptions/{subscriptionId}/cancel")
    void cancelMine(@Path("storeId") String storeId,
            @Path("subscriptionId") String subscriptionId,
            OnComplete<Response<SubscriptionDto>> callback);
}
