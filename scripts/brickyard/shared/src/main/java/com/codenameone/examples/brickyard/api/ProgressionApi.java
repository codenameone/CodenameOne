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

/// XP and levels, the daily login streak, quests and the season pass.
@RestClient
public interface ProgressionApi {
    @GET("/api/me/progress")
    void progress(OnComplete<Response<ProgressDto>> callback);

    @POST("/api/me/login")
    void dailyLogin(OnComplete<Response<ProgressDto>> callback);

    @GET("/api/me/quests")
    void quests(OnComplete<Response<List<QuestDto>>> callback);

    @POST("/api/me/quests/{questId}/claim")
    void claimQuest(@Path("questId") String questId, OnComplete<Response<QuestDto>> callback);

    @POST("/api/engine/progress")
    void reportProgress(@Body ProgressEventDto event, OnComplete<Response<ProgressDto>> callback);

    @GET("/api/season")
    void season(OnComplete<Response<List<SeasonTierDto>>> callback);

    @POST("/api/me/season/premium")
    void buyPremium(OnComplete<Response<ProgressDto>> callback);

    @POST("/api/me/season/claim")
    void claimTier(@Query("tier") int tier, @Query("track") String track, OnComplete<Response<SeasonTierDto>> callback);

    /// The season in progress: its id, name and when it started and ends.
    @GET("/api/season/current")
    void currentSeason(OnComplete<Response<SeasonDto>> callback);
}
