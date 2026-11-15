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

/// Creator analytics: engine event ingest and the dashboard queries.
@RestClient
public interface AnalyticsApi {
    @POST("/api/engine/events")
    void ingest(@Body EventDto event, OnComplete<Response<EventDto>> callback);

    @GET("/api/experiences/{experienceId}/analytics/visits")
    void visits(@Path("experienceId") String experienceId, @Query("days") int days, OnComplete<Response<List<DayCountDto>>> callback);

    @GET("/api/experiences/{experienceId}/analytics/retention")
    void retention(@Path("experienceId") String experienceId, OnComplete<Response<RetentionDto>> callback);

    @GET("/api/experiences/{experienceId}/analytics/revenue")
    void revenue(@Path("experienceId") String experienceId, OnComplete<Response<List<RevenueDto>>> callback);

    @GET("/api/experiences/{experienceId}/analytics/errors")
    void errors(@Path("experienceId") String experienceId, OnComplete<Response<List<ErrorGroupDto>>> callback);
}
