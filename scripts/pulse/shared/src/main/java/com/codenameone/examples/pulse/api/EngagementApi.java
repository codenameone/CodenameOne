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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The caller's own progress, the times they will be away, and what the studio
/// has sent them.
@RestClient
public interface EngagementApi {
    @GET("/api/engagement/stats")
    void stats(OnComplete<Response<StatsDto>> callback);

    @GET("/api/engagement/away")
    void away(OnComplete<Response<List<AwayDto>>> callback);

    /// Declares time away. No nudge is sent meanwhile, and with
    /// `freezeBilling` the household's memberships are frozen for it.
    @POST("/api/engagement/away")
    void addAway(@Body AwayDto away, OnComplete<Response<AwayDto>> callback);

    @POST("/api/engagement/away/{id}/remove")
    void removeAway(@Path("id") String id, OnComplete<Response<AwayDto>> callback);

    @GET("/api/engagement/nudges")
    void nudges(OnComplete<Response<List<NudgeDto>>> callback);

    /// Marks a nudge read, which is what a campaign's open rate counts.
    @POST("/api/engagement/nudges/{id}/opened")
    void opened(@Path("id") String id, OnComplete<Response<NudgeDto>> callback);

    /// Records a workout from the phone's health store or the watch.
    @POST("/api/engagement/workouts")
    void addWorkout(@Body WorkoutDto workout, OnComplete<Response<WorkoutDto>> callback);

    @GET("/api/engagement/workouts")
    void workouts(OnComplete<Response<List<WorkoutDto>>> callback);

    /// A live heart rate reading during a class, for the trainer's screen.
    /// Sent only while the member has chosen to share it.
    @POST("/api/engagement/heartrate")
    void heartRate(@Body HeartRateDto reading, OnComplete<Response<HeartRateDto>> callback);
}
