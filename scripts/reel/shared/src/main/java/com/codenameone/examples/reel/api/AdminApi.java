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
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Admin console: the numbers, the accounts, the switches, the plans, the ads and the sharing alerts.
@RestClient
public interface AdminApi {
    /// What every client reads before sign-in; no sign-in needed.
    @GET("/api/config")
    void config(OnComplete<Response<AppConfigDto>> callback);

    @GET("/api/admin/stats")
    void stats(OnComplete<Response<AdminStatsDto>> callback);

    /// The charts over the last `days` days; 1 to 90.
    @GET("/api/admin/stats/series")
    void statsSeries(@Query("days") int days, OnComplete<Response<StatsSeriesDto>> callback);

    @GET("/api/admin/users")
    void users(@Query("q") String q, OnComplete<Response<List<UserDto>>> callback);

    @GET("/api/admin/users/{username}")
    void user(@Path("username") String username, OnComplete<Response<UserDetailDto>> callback);

    @POST("/api/admin/users/{username}/roles")
    void setRoles(@Path("username") String username, @Body RoleChangeDto roles,
            OnComplete<Response<UserDto>> callback);

    @POST("/api/admin/users/{username}/suspend")
    void suspend(@Path("username") String username, @Body SuspendDto suspend,
            OnComplete<Response<UserDto>> callback);

    @POST("/api/admin/users/{username}/flag")
    void flag(@Path("username") String username, @Body FlagDto flag, OnComplete<Response<UserDto>> callback);

    /// Every stream playing now.
    @GET("/api/admin/sessions")
    void sessions(OnComplete<Response<List<SessionDto>>> callback);

    /// Ends a stream; its next heartbeat is refused.
    @DELETE("/api/admin/sessions/{id}")
    void endSession(@Path("id") String id, OnComplete<Response<String>> callback);

    @GET("/api/admin/flags")
    void flags(OnComplete<Response<List<FeatureFlagDto>>> callback);

    @PUT("/api/admin/flags/{key}")
    void setFlag(@Path("key") String key, @Body FeatureFlagDto flag,
            OnComplete<Response<FeatureFlagDto>> callback);

    @GET("/api/admin/presets")
    void presets(OnComplete<Response<List<PresetDto>>> callback);

    @POST("/api/admin/presets/{id}")
    void applyPreset(@Path("id") String id, OnComplete<Response<List<FeatureFlagDto>>> callback);

    @PUT("/api/admin/branding")
    void setBranding(@Body BrandingDto branding, OnComplete<Response<BrandingDto>> callback);

    @GET("/api/admin/plans")
    void plans(OnComplete<Response<List<PlanDto>>> callback);

    @PUT("/api/admin/plans/{code}")
    void savePlan(@Path("code") String code, @Body PlanDto plan, OnComplete<Response<PlanDto>> callback);

    @GET("/api/admin/coupons")
    void coupons(OnComplete<Response<List<CouponDto>>> callback);

    @PUT("/api/admin/coupons/{code}")
    void saveCoupon(@Path("code") String code, @Body CouponDto coupon,
            OnComplete<Response<CouponDto>> callback);

    @GET("/api/admin/campaigns")
    void campaigns(OnComplete<Response<List<AdCampaignDto>>> callback);

    @POST("/api/admin/campaigns")
    void saveCampaign(@Body AdCampaignDto campaign, OnComplete<Response<AdCampaignDto>> callback);

    @GET("/api/admin/sharing")
    void sharingAlerts(@Query("status") String status, OnComplete<Response<List<SharingAlertDto>>> callback);

    @POST("/api/admin/sharing/{id}")
    void decideSharing(@Path("id") String id, @Body SharingDecisionDto decision,
            OnComplete<Response<SharingAlertDto>> callback);

    @GET("/api/admin/jobs")
    void jobs(@Query("status") String status, OnComplete<Response<List<JobDto>>> callback);

    @POST("/api/admin/jobs/{id}/retry")
    void retryJob(@Path("id") String id, OnComplete<Response<JobDto>> callback);
}
