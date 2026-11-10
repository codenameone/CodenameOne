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
package com.codenameone.examples.slate.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The caller's own bookings, from holding a slot to cancelling it.
///
/// Booking is two calls. [#hold] keeps the slot for the business's hold time
/// -- nobody else is offered it meanwhile -- and [#confirm] makes it a booking,
/// authorizing the deposit when the service takes one. A hold that is not
/// confirmed in time lapses by itself.
@RestClient
public interface AppointmentApi {
    /// Bookings from now on, soonest first, held ones included.
    @GET("/api/appointments")
    void upcoming(OnComplete<Response<List<AppointmentDto>>> callback);

    /// Bookings that are over or were cancelled, newest first.
    @GET("/api/appointments/history")
    void history(OnComplete<Response<List<AppointmentDto>>> callback);

    @GET("/api/appointments/{id}")
    void appointment(@Path("id") String id, OnComplete<Response<AppointmentDto>> callback);

    /// Answers 409, naming the rule, when the slot was taken or no longer
    /// meets the rules.
    @POST("/api/appointments/hold")
    void hold(@Body HoldRequestDto request, OnComplete<Response<AppointmentDto>> callback);

    /// Answers 410 when the hold lapsed and 402 when the deposit was declined.
    @POST("/api/appointments/{id}/confirm")
    void confirm(@Path("id") String id, @Body ConfirmDto confirm,
            OnComplete<Response<AppointmentDto>> callback);

    /// Free until the business's cancellation window; after it the deposit is
    /// kept.
    @POST("/api/appointments/{id}/cancel")
    void cancel(@Path("id") String id, @Body CancelDto cancel,
            OnComplete<Response<AppointmentDto>> callback);

    /// Moves a confirmed booking. The new time is taken before the old one is
    /// given up, so a failed move leaves the booking where it was.
    @POST("/api/appointments/{id}/reschedule")
    void reschedule(@Path("id") String id, @Body RescheduleDto move,
            OnComplete<Response<AppointmentDto>> callback);

    @GET("/api/waitlist")
    void waitlist(OnComplete<Response<List<WaitlistDto>>> callback);

    @POST("/api/waitlist")
    void join(@Body WaitlistRequestDto request, OnComplete<Response<WaitlistDto>> callback);

    @POST("/api/waitlist/{id}/cancel")
    void leave(@Path("id") String id, OnComplete<Response<WaitlistDto>> callback);
}
