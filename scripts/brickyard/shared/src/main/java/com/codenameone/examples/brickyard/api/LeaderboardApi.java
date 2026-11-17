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

/// Leaderboards. Scores are written by engine servers only; anyone signed in may read them.
@RestClient
public interface LeaderboardApi {
    @POST("/api/leaderboards/{experienceId}/scores")
    void write(@Path("experienceId") String experienceId, @Body ScoreWriteDto score,
            OnComplete<Response<LeaderDto>> callback);

    @GET("/api/leaderboards/{experienceId}/{stat}")
    void top(@Path("experienceId") String experienceId, @Path("stat") String stat,
            @Query("period") String period, @Query("limit") int limit,
            OnComplete<Response<List<LeaderDto>>> callback);

    @GET("/api/leaderboards/{experienceId}/{stat}/friends")
    void friends(@Path("experienceId") String experienceId, @Path("stat") String stat,
            @Query("period") String period, OnComplete<Response<List<LeaderDto>>> callback);

    /// The stat names the experience has leaderboard entries for.
    @GET("/api/experiences/{id}/stats")
    void stats(@Path("id") String id, OnComplete<Response<List<StatDto>>> callback);
    @POST("/api/experiences/{id}/bounds")
    void setBound(@Path("id") String id, @Body StatBoundDto bound, OnComplete<Response<StatBoundDto>> callback);
}
