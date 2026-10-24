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
package com.codenameone.examples.reel.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Plans, the subscription, coupons, rentals and invoices.
@RestClient
public interface BillingApi {
    @GET("/api/plans")
    void plans(OnComplete<Response<List<PlanDto>>> callback);

    @GET("/api/billing/subscription")
    void subscription(OnComplete<Response<SubscriptionDto>> callback);

    /// Subscribes, or changes plan: an upgrade applies now and is prorated, a downgrade at the period end.
    @POST("/api/billing/subscribe")
    void subscribe(@Body SubscribeDto subscribe, OnComplete<Response<SubscriptionDto>> callback);

    /// Cancels at the end of the paid period.
    @POST("/api/billing/cancel")
    void cancel(OnComplete<Response<SubscriptionDto>> callback);

    @POST("/api/billing/resume")
    void resume(OnComplete<Response<SubscriptionDto>> callback);

    @GET("/api/billing/coupons/{code}")
    void checkCoupon(@Path("code") String code, @Query("plan") String plan,
            OnComplete<Response<CouponCheckDto>> callback);

    @GET("/api/billing/invoices")
    void invoices(OnComplete<Response<List<InvoiceDto>>> callback);

    @GET("/api/billing/rentals")
    void rentals(OnComplete<Response<List<RentalDto>>> callback);

    @POST("/api/billing/rent")
    void rent(@Body RentDto rent, OnComplete<Response<RentalDto>> callback);

    /// Turns an app-store purchase into a subscription.
    @POST("/api/billing/receipt")
    void verifyReceipt(@Body PurchaseReceiptDto receipt, OnComplete<Response<SubscriptionDto>> callback);
}
