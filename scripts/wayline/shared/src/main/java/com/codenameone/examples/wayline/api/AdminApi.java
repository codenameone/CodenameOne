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
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Admin mode: the numbers, the fleet, who may drive, and who may do what.
@RestClient
public interface AdminApi {
    @GET("/api/admin/stats")
    void stats(OnComplete<Response<AdminStatsDto>> callback);

    /// The charts, over the last `days` days; 1 to 90.
    @GET("/api/admin/stats/series")
    void statsSeries(@Query("days") int days, OnComplete<Response<StatsSeriesDto>> callback);

    @GET("/api/admin/rides")
    void rides(OnComplete<Response<List<RideDto>>> callback);

    @GET("/api/admin/users")
    void users(OnComplete<Response<List<UserDto>>> callback);

    @GET("/api/admin/users/{username}")
    void user(@Path("username") String username, OnComplete<Response<UserDetailDto>> callback);

    @GET("/api/admin/drivers")
    void drivers(OnComplete<Response<List<DriverDto>>> callback);

    @POST("/api/admin/users/{username}/roles")
    void setRoles(@Path("username") String username, @Body RoleChangeDto roles,
            OnComplete<Response<UserDto>> callback);

    /// Blocks an account, or lifts the block.
    @POST("/api/admin/users/{username}/suspend")
    void suspend(@Path("username") String username, @Body SuspendDto suspend,
            OnComplete<Response<UserDto>> callback);

    @POST("/api/admin/users/{username}/flag")
    void flag(@Path("username") String username, @Body FlagDto flag,
            OnComplete<Response<UserDto>> callback);

    /// Driving applications, newest first. `status` narrows them to one status;
    /// empty lists them all.
    @GET("/api/admin/applications")
    void applications(@Query("status") String status,
            OnComplete<Response<List<DriverApplicationDto>>> callback);

    @GET("/api/admin/applications/{username}")
    void application(@Path("username") String username,
            OnComplete<Response<DriverApplicationDto>> callback);

    @GET("/api/admin/applications/{username}/documents/{kind}")
    void document(@Path("username") String username, @Path("kind") String kind,
            OnComplete<Response<DocumentContentDto>> callback);

    /// Approves an application, which makes its account a driver.
    @POST("/api/admin/applications/{username}/approve")
    void approve(@Path("username") String username,
            OnComplete<Response<DriverApplicationDto>> callback);

    @POST("/api/admin/applications/{username}/reject")
    void reject(@Path("username") String username, @Body ReasonDto reason,
            OnComplete<Response<DriverApplicationDto>> callback);

    @POST("/api/admin/rides/{id}/cancel")
    void cancelRide(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<RideDto>> callback);

    @POST("/api/admin/rides/{id}/refund")
    void refundRide(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<ReceiptDto>> callback);

    /// Every receipt, newest first.
    @GET("/api/admin/payments")
    void payments(OnComplete<Response<List<ReceiptDto>>> callback);

    @GET("/api/admin/pricing")
    void pricing(OnComplete<Response<PricingDto>> callback);

    @PUT("/api/admin/pricing")
    void savePricing(@Body PricingDto pricing, OnComplete<Response<PricingDto>> callback);
}
