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
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Playback: a leased session per stream, its heartbeats and its ads.
@RestClient
public interface PlaybackApi {
    /// Starts a session. Answers 402 when the account is not entitled, 409 when it is at its stream limit.
    @POST("/api/playback/start")
    void start(@Body PlaybackRequestDto request, OnComplete<Response<PlaybackDto>> callback);

    @POST("/api/playback/heartbeat")
    void heartbeat(@Body HeartbeatDto heartbeat, OnComplete<Response<HeartbeatReplyDto>> callback);

    @POST("/api/playback/stop")
    void stop(@Body StopDto stop, OnComplete<Response<String>> callback);

    /// The creatives of an ad break, already parsed from VAST for players that cannot.
    @GET("/api/ads/{breakId}")
    void ads(@Path("breakId") String breakId, @Query("session") String session,
            OnComplete<Response<List<AdDto>>> callback);

    @POST("/api/ads/event")
    void adEvent(@Body AdEventDto event, OnComplete<Response<String>> callback);
}
