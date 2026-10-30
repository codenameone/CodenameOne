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

/// A trainer's classes, rosters and shifts: for accounts with the TRAINER role.
@RestClient
public interface TrainerApi {
    /// The trainer's classes for the next 14 days.
    @GET("/api/trainer/classes")
    void classes(OnComplete<Response<List<SessionDto>>> callback);

    /// Who is coming, with the live heart rate of anyone sharing one.
    @GET("/api/trainer/classes/{id}/roster")
    void roster(@Path("id") String id, OnComplete<Response<List<RosterEntryDto>>> callback);

    @POST("/api/trainer/classes/{id}/attendance")
    void attendance(@Path("id") String id, @Body AttendanceDto mark,
            OnComplete<Response<RosterEntryDto>> callback);

    /// The trainer's own shifts, and open ones at their site anyone may take.
    @GET("/api/trainer/shifts")
    void shifts(OnComplete<Response<List<ShiftDto>>> callback);

    /// Offers one of the trainer's shifts to the others.
    @POST("/api/trainer/shifts/{id}/swap")
    void offer(@Path("id") String id, OnComplete<Response<ShiftDto>> callback);

    /// Takes a shift another trainer offered.
    @POST("/api/trainer/shifts/{id}/take")
    void take(@Path("id") String id, OnComplete<Response<ShiftDto>> callback);
}
