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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Courier mode: going on line, answering offers, running a delivery and being paid
/// for it.
@RestClient
public interface CourierApi {
    /// Goes on or off line, and reports where the car is.
    @POST("/api/courier/status")
    void status(@Body CourierStatusDto status, OnComplete<Response<CourierDto>> callback);

    @GET("/api/courier/me")
    void me(OnComplete<Response<CourierDto>> callback);

    /// The offer waiting for an answer or the delivery under way, or one whose state
    /// is NONE.
    @GET("/api/courier/active")
    void active(OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/offers/{id}/accept")
    void accept(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/offers/{id}/decline")
    void decline(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/deliveries/{id}/arrived")
    void arrived(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/deliveries/{id}/start")
    void start(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/deliveries/{id}/complete")
    void complete(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/courier/deliveries/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @GET("/api/courier/earnings")
    void earnings(OnComplete<Response<EarningsDto>> callback);

    @GET("/api/courier/history")
    void history(OnComplete<Response<List<DeliveryDto>>> callback);

    @GET("/api/courier/earnings/summary")
    void earningsSummary(OnComplete<Response<EarningsSummaryDto>> callback);

    @GET("/api/courier/payouts")
    void payouts(OnComplete<Response<List<PayoutDto>>> callback);

    /// Pays the whole balance out to the payout account. Refused when there is
    /// no balance, or no account to send it to.
    @POST("/api/courier/payouts")
    void cashOut(OnComplete<Response<PayoutDto>> callback);

    @GET("/api/courier/payout-account")
    void payoutAccount(OnComplete<Response<PayoutAccountDto>> callback);

    @PUT("/api/courier/payout-account")
    void savePayoutAccount(@Body PayoutAccountDto account,
            OnComplete<Response<PayoutAccountDto>> callback);
}
