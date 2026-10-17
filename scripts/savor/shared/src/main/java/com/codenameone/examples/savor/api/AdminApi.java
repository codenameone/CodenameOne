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

    @GET("/api/admin/deliveries")
    void deliveries(OnComplete<Response<List<DeliveryDto>>> callback);

    @GET("/api/admin/users")
    void users(OnComplete<Response<List<UserDto>>> callback);

    @GET("/api/admin/users/{username}")
    void user(@Path("username") String username, OnComplete<Response<UserDetailDto>> callback);

    @GET("/api/admin/couriers")
    void couriers(OnComplete<Response<List<CourierDto>>> callback);

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

    /// Onboarding applications, newest first. `status` narrows them to one status;
    /// empty lists them all.
    @GET("/api/admin/applications")
    void applications(@Query("status") String status,
            OnComplete<Response<List<CourierApplicationDto>>> callback);

    @GET("/api/admin/applications/{username}")
    void application(@Path("username") String username,
            OnComplete<Response<CourierApplicationDto>> callback);

    @GET("/api/admin/applications/{username}/documents/{kind}")
    void document(@Path("username") String username, @Path("kind") String kind,
            OnComplete<Response<DocumentContentDto>> callback);

    /// Approves an application, which makes its account a courier.
    @POST("/api/admin/applications/{username}/approve")
    void approve(@Path("username") String username,
            OnComplete<Response<CourierApplicationDto>> callback);

    @POST("/api/admin/applications/{username}/reject")
    void reject(@Path("username") String username, @Body ReasonDto reason,
            OnComplete<Response<CourierApplicationDto>> callback);

    @POST("/api/admin/deliveries/{id}/cancel")
    void cancelDelivery(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/admin/deliveries/{id}/refund")
    void refundDelivery(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<ReceiptDto>> callback);

    /// Every receipt, newest first.
    @GET("/api/admin/payments")
    void payments(OnComplete<Response<List<ReceiptDto>>> callback);

    @GET("/api/admin/pricing")
    void pricing(OnComplete<Response<PricingDto>> callback);

    @PUT("/api/admin/pricing")
    void savePricing(@Body PricingDto pricing, OnComplete<Response<PricingDto>> callback);
}
