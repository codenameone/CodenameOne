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

import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The studio as anyone signed in sees it: its sites, its classes and its
/// trainers.
@RestClient
public interface StudioApi {
    @GET("/api/studio/locations")
    void locations(OnComplete<Response<List<LocationDto>>> callback);

    @GET("/api/studio/types")
    void classTypes(OnComplete<Response<List<ClassTypeDto>>> callback);

    @GET("/api/studio/trainers")
    void trainers(OnComplete<Response<List<TrainerDto>>> callback);

    /// The timetable at a site from `from` (epoch millis) for `days` days, at
    /// most 14, cancelled classes included so a member sees what changed.
    @GET("/api/studio/classes")
    void classes(@Query("location") String location, @Query("from") long from,
            @Query("days") int days, OnComplete<Response<List<SessionDto>>> callback);

    @GET("/api/studio/classes/{id}")
    void session(@Path("id") String id, OnComplete<Response<SessionDto>> callback);
}
