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

/// Creator studio: titles, videos, resumable uploads, chapters, interactions and analytics.
@RestClient
public interface StudioApi {
    @GET("/api/studio/titles")
    void titles(OnComplete<Response<List<StudioTitleDto>>> callback);

    @GET("/api/studio/titles/{id}")
    void title(@Path("id") String id, OnComplete<Response<StudioTitleDto>> callback);

    @POST("/api/studio/titles")
    void createTitle(@Body StudioTitleEditDto title, OnComplete<Response<StudioTitleDto>> callback);

    @PUT("/api/studio/titles/{id}")
    void updateTitle(@Path("id") String id, @Body StudioTitleEditDto title,
            OnComplete<Response<StudioTitleDto>> callback);

    /// Sends a title to moderation; it is published when a moderator approves it.
    @POST("/api/studio/titles/{id}/submit")
    void submit(@Path("id") String id, OnComplete<Response<StudioTitleDto>> callback);

    @POST("/api/studio/titles/{id}/unpublish")
    void unpublish(@Path("id") String id, OnComplete<Response<StudioTitleDto>> callback);

    @POST("/api/studio/titles/{id}/appeal")
    void appeal(@Path("id") String id, @Body AppealDto appeal, OnComplete<Response<StudioTitleDto>> callback);

    @POST("/api/studio/titles/{id}/videos")
    void addVideo(@Path("id") String id, @Body StudioVideoEditDto video,
            OnComplete<Response<StudioVideoDto>> callback);

    @PUT("/api/studio/videos/{id}")
    void updateVideo(@Path("id") String id, @Body StudioVideoEditDto video,
            OnComplete<Response<StudioVideoDto>> callback);

    @DELETE("/api/studio/videos/{id}")
    void deleteVideo(@Path("id") String id, OnComplete<Response<String>> callback);

    @PUT("/api/studio/videos/{id}/chapters")
    void setChapters(@Path("id") String id, @Body ChaptersDto chapters,
            OnComplete<Response<StudioVideoDto>> callback);

    @PUT("/api/studio/videos/{id}/interactions")
    void setInteractions(@Path("id") String id, @Body InteractionsDto interactions,
            OnComplete<Response<StudioVideoDto>> callback);

    /// Adds or replaces a subtitle track: `label` and the WebVTT text, in
    /// the language `lang` names (`en`, `pt-BR`).
    @PUT("/api/studio/videos/{id}/subtitles/{lang}")
    void setSubtitle(@Path("id") String id, @Path("lang") String lang, @Body SubtitleUploadDto subtitle,
            OnComplete<Response<StudioVideoDto>> callback);

    @DELETE("/api/studio/videos/{id}/subtitles/{lang}")
    void deleteSubtitle(@Path("id") String id, @Path("lang") String lang,
            OnComplete<Response<StudioVideoDto>> callback);

    /// Opens a resumable upload. The chunks go to `PUT /api/studio/uploads/{id}?offset=n` as raw bytes.
    @POST("/api/studio/uploads")
    void startUpload(@Body UploadStartDto upload, OnComplete<Response<UploadDto>> callback);

    /// Where to resume an interrupted upload.
    @GET("/api/studio/uploads/{id}")
    void upload(@Path("id") String id, OnComplete<Response<UploadDto>> callback);

    @GET("/api/studio/jobs")
    void jobs(OnComplete<Response<List<JobDto>>> callback);

    @GET("/api/studio/titles/{id}/analytics")
    void analytics(@Path("id") String id, @Query("days") int days,
            OnComplete<Response<TitleAnalyticsDto>> callback);
}
