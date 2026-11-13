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
package com.codenameone.examples.splat.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The loadout, match history, replays, the leaderboard, party invitations,
/// squads and reports.
@RestClient
public interface GameApi {
    @GET("/api/runner")
    void runner(OnComplete<Response<RunnerDto>> callback);

    @POST("/api/runner")
    void saveRunner(@Body RunnerDto runner, OnComplete<Response<RunnerDto>> callback);

    @GET("/api/races")
    void history(OnComplete<Response<List<RaceSummaryDto>>> callback);

    @GET("/api/races/{id}")
    void replay(@Path("id") String id, OnComplete<Response<ReplayDto>> callback);

    /// The trophy ladder: who wins races.
    @GET("/api/leaderboard")
    void leaderboard(@Query("limit") int limit, OnComplete<Response<List<LeaderDto>>> callback);

    @POST("/api/challenges")
    void challenge(OnComplete<Response<ChallengeDto>> callback);

    @GET("/api/crews")
    void crews(OnComplete<Response<List<CrewDto>>> callback);

    @POST("/api/crews")
    void createCrew(@Body CrewCreateDto crew, OnComplete<Response<CrewDto>> callback);

    @POST("/api/crews/{code}/join")
    void joinCrew(@Path("code") String code, OnComplete<Response<CrewDto>> callback);

    @POST("/api/crews/{code}/leave")
    void leaveCrew(@Path("code") String code, OnComplete<Response<CrewDto>> callback);

    @POST("/api/reports")
    void report(@Body ReportDto report, OnComplete<Response<FlagDto>> callback);
}
