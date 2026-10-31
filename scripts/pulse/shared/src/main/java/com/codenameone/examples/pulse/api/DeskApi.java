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
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The front desk and the door kiosk: for accounts with the DESK role.
@RestClient
public interface DeskApi {
    /// Admits a member, or says why not. A refusal is an answer, not an error:
    /// the kiosk shows it, and the desk can still let them in by hand.
    @POST("/api/desk/checkin")
    void checkIn(@Body CheckInRequestDto request, OnComplete<Response<CheckInResultDto>> callback);

    /// Members whose name or e-mail contains `q`, at most 25.
    @GET("/api/desk/members")
    void find(@Query("q") String q, OnComplete<Response<List<MemberSummaryDto>>> callback);

    @POST("/api/desk/walkins")
    void walkIn(@Body WalkInDto walkIn, OnComplete<Response<WalkInDto>> callback);

    @GET("/api/desk/today")
    void today(@Query("location") String location, OnComplete<Response<DeskDayDto>> callback);
}
