/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The console: everything it reads and does. Every route requires the ADMIN role, and every
/// action is written to the audit log.
@RestClient
public interface AdminApi {
    @GET("/api/admin/stats")
    void stats(OnComplete<Response<AdminStatsDto>> callback);

    @GET("/api/admin/players")
    void players(@Query("q") String query, OnComplete<Response<List<PlayerDto>>> callback);

    @POST("/api/admin/players/{username}/action")
    void act(@Path("username") String username, @Body AdminActionDto action, OnComplete<Response<PlayerDto>> callback);

    @GET("/api/admin/flags")
    void flags(@Query("open") boolean open, OnComplete<Response<List<FlagDto>>> callback);

    @POST("/api/admin/flags/{id}/resolve")
    void resolve(@Path("id") String id, @Body ResolveDto verdict, OnComplete<Response<FlagDto>> callback);

    @POST("/api/admin/daily")
    void setDaily(@Body DailySeedDto seed, OnComplete<Response<DailyDto>> callback);

    @POST("/api/admin/broadcast")
    void broadcast(@Body BroadcastDto broadcast, OnComplete<Response<BroadcastDto>> callback);

    @GET("/api/admin/audit")
    void audit(OnComplete<Response<List<AuditDto>>> callback);
}
