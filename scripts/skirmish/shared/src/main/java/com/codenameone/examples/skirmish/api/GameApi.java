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
