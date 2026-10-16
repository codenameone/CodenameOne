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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Driver mode: going on line, answering offers, running a ride and being paid
/// for it.
@RestClient
public interface DriverApi {
    /// Goes on or off line, and reports where the car is.
    @POST("/api/driver/status")
    void status(@Body DriverStatusDto status, OnComplete<Response<DriverDto>> callback);

    @GET("/api/driver/me")
    void me(OnComplete<Response<DriverDto>> callback);

    /// The offer waiting for an answer or the ride under way, or one whose state
    /// is NONE.
    @GET("/api/driver/active")
    void active(OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/offers/{id}/accept")
    void accept(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/offers/{id}/decline")
    void decline(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/rides/{id}/arrived")
    void arrived(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/rides/{id}/start")
    void start(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/rides/{id}/complete")
    void complete(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @POST("/api/driver/rides/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<RideDto>> callback);

    @GET("/api/driver/earnings")
    void earnings(OnComplete<Response<EarningsDto>> callback);

    @GET("/api/driver/history")
    void history(OnComplete<Response<List<RideDto>>> callback);

    @GET("/api/driver/earnings/summary")
    void earningsSummary(OnComplete<Response<EarningsSummaryDto>> callback);

    @GET("/api/driver/payouts")
    void payouts(OnComplete<Response<List<PayoutDto>>> callback);

    /// Pays the whole balance out to the payout account. Refused when there is
    /// no balance, or no account to send it to.
    @POST("/api/driver/payouts")
    void cashOut(OnComplete<Response<PayoutDto>> callback);

    @GET("/api/driver/payout-account")
    void payoutAccount(OnComplete<Response<PayoutAccountDto>> callback);

    @PUT("/api/driver/payout-account")
    void savePayoutAccount(@Body PayoutAccountDto account,
            OnComplete<Response<PayoutAccountDto>> callback);
}
