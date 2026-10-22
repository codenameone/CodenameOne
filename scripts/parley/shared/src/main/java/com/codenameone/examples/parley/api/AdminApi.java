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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Running the service: the dashboard, accounts, reports and bridges.
@RestClient
public interface AdminApi {
    @GET("/api/admin/stats")
    void stats(OnComplete<Response<AdminStatsDto>> callback);

    @GET("/api/admin/stats/series")
    void statsSeries(@Query("days") int days, OnComplete<Response<StatsSeriesDto>> callback);

    @GET("/api/admin/users")
    void users(OnComplete<Response<List<UserDto>>> callback);

    @GET("/api/admin/users/{username}")
    void user(@Path("username") String username, OnComplete<Response<UserDetailDto>> callback);

    @POST("/api/admin/users/{username}/roles")
    void setRoles(@Path("username") String username, @Body RoleChangeDto roles,
            OnComplete<Response<UserDto>> callback);

    @POST("/api/admin/users/{username}/suspend")
    void suspend(@Path("username") String username, @Body SuspendDto suspend,
            OnComplete<Response<UserDto>> callback);

    @POST("/api/admin/users/{username}/flag")
    void flag(@Path("username") String username, @Body FlagDto flag,
            OnComplete<Response<UserDto>> callback);

    @GET("/api/admin/reports")
    void reports(@Query("status") String status, OnComplete<Response<List<ReportViewDto>>> callback);

    @POST("/api/admin/reports/{id}/resolve")
    void resolve(@Path("id") String id, @Body ReasonDto outcome,
            OnComplete<Response<ReportViewDto>> callback);

    @GET("/api/admin/bridges")
    void bridges(OnComplete<Response<List<BridgeLinkDto>>> callback);
}
