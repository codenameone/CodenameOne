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

/// Decks, match history, replays, the leaderboard, private challenges and reports.
@RestClient
public interface GameApi {
    @GET("/api/deck")
    void deck(OnComplete<Response<DeckDto>> callback);

    @POST("/api/deck")
    void saveDeck(@Body DeckDto deck, OnComplete<Response<DeckDto>> callback);

    @GET("/api/matches")
    void history(OnComplete<Response<List<MatchSummaryDto>>> callback);

    @GET("/api/matches/{id}")
    void replay(@Path("id") String id, OnComplete<Response<ReplayDto>> callback);

    @GET("/api/leaderboard")
    void leaderboard(@Query("limit") int limit, OnComplete<Response<List<LeaderDto>>> callback);

    @POST("/api/challenges")
    void challenge(OnComplete<Response<ChallengeDto>> callback);

    @POST("/api/reports")
    void report(@Body ReportDto report, OnComplete<Response<FlagDto>> callback);
}
