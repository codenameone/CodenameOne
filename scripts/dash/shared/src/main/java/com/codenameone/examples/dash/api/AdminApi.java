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
package com.codenameone.examples.dash.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// God mode: everything the admin console reads and does. Every route requires the ADMIN role, and every action is written to the audit log.
@RestClient
public interface AdminApi {
    @GET("/api/admin/stats")
    void stats(OnComplete<Response<AdminStatsDto>> callback);

    @GET("/api/admin/players")
    void players(@Query("q") String query, OnComplete<Response<List<PlayerDto>>> callback);

    @GET("/api/admin/players/{username}")
    void player(@Path("username") String username, OnComplete<Response<PlayerDetailDto>> callback);

    @POST("/api/admin/players/{username}/action")
    void act(@Path("username") String username, @Body AdminActionDto action,
            OnComplete<Response<PlayerDto>> callback);

    @GET("/api/admin/flags")
    void flags(@Query("open") boolean open, OnComplete<Response<List<FlagDto>>> callback);

    @POST("/api/admin/flags/{id}/resolve")
    void resolve(@Path("id") String id, OnComplete<Response<FlagDto>> callback);

    @GET("/api/admin/races")
    void liveRaces(OnComplete<Response<List<LiveRaceDto>>> callback);

    @GET("/api/admin/audit")
    void audit(OnComplete<Response<List<AuditDto>>> callback);

    /// Packs and files waiting for a moderator: reported or hidden packs, and
    /// uploaded files nobody has looked at yet.
    @GET("/api/admin/forge/packs")
    void forgeQueue(OnComplete<Response<List<PackSummaryDto>>> callback);

    @GET("/api/admin/forge/assets")
    void forgeAssets(OnComplete<Response<List<AssetDto>>> callback);

    /// `hide`, `restore`, `official` or `unofficial` a pack.
    @POST("/api/admin/forge/packs/action")
    void forgePackAction(@Body ForgeActionDto action, OnComplete<Response<PackSummaryDto>> callback);

    /// `approve` or `reject` an uploaded file.
    @POST("/api/admin/forge/assets/action")
    void forgeAssetAction(@Body ForgeActionDto action, OnComplete<Response<AssetDto>> callback);

    /// The Promote tab: players, credited invites, top inviters, pushes sent
    /// and the home banner.
    @GET("/api/admin/promote")
    void promote(OnComplete<Response<PromoteDto>> callback);

    /// How many players a broadcast would reach, without sending it.
    @POST("/api/admin/promote/audience")
    void audience(@Body BroadcastDto broadcast, OnComplete<Response<BroadcastDto>> callback);

    /// Sends a push to a segment, through Codename One push.
    @POST("/api/admin/promote/broadcast")
    void broadcast(@Body BroadcastDto broadcast, OnComplete<Response<BroadcastDto>> callback);

    @POST("/api/admin/promote/spotlight")
    void spotlight(@Body SpotlightDto spotlight, OnComplete<Response<SpotlightDto>> callback);

    /// Daily players, new accounts, races and sales over the last `days`,
    /// with the funnel, retention and trophy bands.
    @GET("/api/admin/reports")
    void reports(@Query("days") int days, OnComplete<Response<InsightsDto>> callback);

    /// The players matching every condition, counted, with the first of them.
    @POST("/api/admin/query")
    void query(@Body QueryDto query, OnComplete<Response<QueryResultDto>> callback);

    @GET("/api/admin/pipelines")
    void pipelines(OnComplete<Response<List<PipelineDto>>> callback);

    /// Creates a pipeline (no id) or replaces one.
    @POST("/api/admin/pipelines")
    void savePipeline(@Body PipelineDto pipeline, OnComplete<Response<PipelineDto>> callback);

    @POST("/api/admin/pipelines/{id}/delete")
    void deletePipeline(@Path("id") String id, OnComplete<Response<PipelineDto>> callback);

    /// Runs a pipeline now instead of waiting for the hour.
    @POST("/api/admin/pipelines/{id}/run")
    void runPipeline(@Path("id") String id, OnComplete<Response<PipelineDto>> callback);
}
