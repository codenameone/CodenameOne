/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
