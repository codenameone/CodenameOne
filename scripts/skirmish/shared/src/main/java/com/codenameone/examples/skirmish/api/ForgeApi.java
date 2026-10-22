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
package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The Forge: packs of characters, arenas, missions and campaigns that players
/// build, share and play. A pack's content travels as the JSON
/// `com.codenameone.examples.skirmish.rules.forge.PackCodec` writes, and the
/// server validates it with the same rules module the app runs.
@RestClient
public interface ForgeApi {
    /// `scope` is official, popular, new or mine.
    @GET("/api/forge/packs")
    void packs(@Query("scope") String scope, OnComplete<Response<List<PackSummaryDto>>> callback);

    /// The latest published version; a share link opens this.
    @GET("/api/forge/packs/{id}")
    void pack(@Path("id") String id, OnComplete<Response<PackDto>> callback);

    /// One exact version, as a replay or a mission result names it.
    @GET("/api/forge/packs/{id}/versions/{version}")
    void version(@Path("id") String id, @Path("version") int version, OnComplete<Response<PackDto>> callback);

    /// The caller's working copy of their own pack.
    @GET("/api/forge/drafts/{id}")
    void draft(@Path("id") String id, OnComplete<Response<PackDto>> callback);

    @POST("/api/forge/drafts")
    void saveDraft(@Body PackSaveDto draft, OnComplete<Response<PackSummaryDto>> callback);

    /// Publishes the draft as a new version, or answers 422 with what is wrong.
    @POST("/api/forge/drafts/{id}/publish")
    void publish(@Path("id") String id, OnComplete<Response<PackSummaryDto>> callback);

    @POST("/api/forge/drafts/{id}/delete")
    void delete(@Path("id") String id, OnComplete<Response<PackSummaryDto>> callback);

    /// Remixes a published pack into a draft of the caller's own.
    @POST("/api/forge/packs/{id}/fork")
    void fork(@Path("id") String id, @Body ForkDto fork, OnComplete<Response<PackSummaryDto>> callback);

    @POST("/api/forge/rate")
    void rate(@Body RateDto rating, OnComplete<Response<PackSummaryDto>> callback);

    @POST("/api/forge/report")
    void report(@Body PackReportDto report, OnComplete<Response<PackSummaryDto>> callback);

    @POST("/api/forge/assets")
    void upload(@Body AssetUploadDto file, OnComplete<Response<AssetDto>> callback);

    @GET("/api/forge/assets/{sha256}")
    void asset(@Path("sha256") String sha256, OnComplete<Response<AssetDto>> callback);

    /// Records a finished mission and pays its first-time stars.
    @POST("/api/forge/results")
    void result(@Body MissionResultDto result, OnComplete<Response<MissionOutcomeDto>> callback);

    @GET("/api/forge/packs/{id}/stars")
    void stars(@Path("id") String id, OnComplete<Response<List<MissionStarsDto>>> callback);
}
