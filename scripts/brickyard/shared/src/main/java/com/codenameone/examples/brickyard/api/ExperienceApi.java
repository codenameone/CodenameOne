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
package com.codenameone.examples.brickyard.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Experiences: creating one, storing and publishing its place versions, and what players do to it.
@RestClient
public interface ExperienceApi {
    @POST("/api/experiences")
    void create(@Body ExperienceSaveDto experience, OnComplete<Response<ExperienceDto>> callback);

    @GET("/api/experiences")
    void browse(@Query("genre") String genre, OnComplete<Response<List<ExperienceDto>>> callback);

    @GET("/api/experiences/{id}")
    void experience(@Path("id") String id, OnComplete<Response<ExperienceDto>> callback);

    @POST("/api/experiences/{id}/drafts")
    void saveDraft(@Path("id") String id, @Body PlaceUploadDto place, OnComplete<Response<VersionDto>> callback);

    @POST("/api/experiences/{id}/publish")
    void publish(@Path("id") String id, @Body PlaceUploadDto place, OnComplete<Response<VersionDto>> callback);

    @POST("/api/experiences/{id}/rollback")
    void rollback(@Path("id") String id, @Body RollbackDto target, OnComplete<Response<VersionDto>> callback);

    @GET("/api/experiences/{id}/versions")
    void versions(@Path("id") String id, OnComplete<Response<List<VersionDto>>> callback);

    @GET("/api/experiences/{id}/place")
    void place(@Path("id") String id, OnComplete<Response<PlaceDto>> callback);

    /// The tile picture; `data` is empty when the creator has not set one.
    @GET("/api/experiences/{id}/thumbnail")
    void thumbnail(@Path("id") String id, OnComplete<Response<ThumbnailDto>> callback);

    /// Sets the tile picture: a JPEG or PNG frame of the experience, owner only.
    @POST("/api/experiences/{id}/thumbnail")
    void setThumbnail(@Path("id") String id, @Body ThumbnailDto image, OnComplete<Response<ThumbnailDto>> callback);

    @POST("/api/experiences/{id}/visit")
    void visit(@Path("id") String id, OnComplete<Response<ExperienceDto>> callback);

    @POST("/api/experiences/{id}/favourite")
    void favourite(@Path("id") String id, OnComplete<Response<ExperienceDto>> callback);

    @POST("/api/experiences/{id}/vote")
    void vote(@Path("id") String id, @Body VoteDto vote, OnComplete<Response<ExperienceDto>> callback);

    /// Players in each experience right now, for the "N playing" on a tile;
    /// an experience nobody is in is left out.
    @GET("/api/playing")
    void playing(OnComplete<Response<List<PlayingDto>>> callback);

    /// The caller's own experiences, drafts included.
    @GET("/api/me/experiences")
    void mine(OnComplete<Response<List<ExperienceDto>>> callback);
    /// Seats the caller on a server of the experience and answers a socket ticket.
    @POST("/api/experiences/{id}/join")
    void join(@Path("id") String id, @Body JoinDto join, OnComplete<Response<JoinTicketDto>> callback);
}
