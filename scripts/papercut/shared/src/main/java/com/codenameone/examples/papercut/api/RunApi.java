/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Runs and the boards: a ticket before a run, the result after it, the leaderboards and the
/// daily sheet. `period` is ALL, WEEK, TODAY or FRIENDS.
@RestClient
public interface RunApi {
    @POST("/api/runs/start")
    void start(@Body StartRunDto request, OnComplete<Response<RunTicketDto>> callback);

    @POST("/api/runs")
    void submit(@Body RunDto run, OnComplete<Response<RunResultDto>> callback);

    @GET("/api/leaderboard")
    void leaderboard(@Query("mode") String mode, @Query("period") String period, OnComplete<Response<List<LeaderDto>>> callback);

    @GET("/api/daily")
    void daily(OnComplete<Response<DailyDto>> callback);
}
