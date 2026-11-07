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

/// A provider's own day, week and calendars, for the PROVIDER role.
@RestClient
public interface ProviderApi {
    @GET("/api/provider/me")
    void me(OnComplete<Response<ProviderDto>> callback);

    /// The provider's title, bio, colour and limits. The services they offer
    /// are the owner's to change.
    @PUT("/api/provider/me")
    void saveMe(@Body ProviderDto me, OnComplete<Response<ProviderDto>> callback);

    /// `days` days from `from` (epoch millis), at most 31.
    @GET("/api/provider/schedule")
    void schedule(@Query("from") long from, @Query("days") int days,
            OnComplete<Response<ScheduleDto>> callback);

    @GET("/api/provider/hours")
    void hours(OnComplete<Response<WeekDto>> callback);

    /// Replaces the whole week. Answers 409 when a booking would be left
    /// outside it.
    @PUT("/api/provider/hours")
    void saveHours(@Body WeekDto week,
            OnComplete<Response<WeekDto>> callback);

    @GET("/api/provider/time-off")
    void timeOff(OnComplete<Response<List<TimeOffDto>>> callback);

    /// Answers 409 when the time already has bookings in it.
    @POST("/api/provider/time-off")
    void addTimeOff(@Body TimeOffDto off, OnComplete<Response<TimeOffDto>> callback);

    @DELETE("/api/provider/time-off/{id}")
    void removeTimeOff(@Path("id") String id, OnComplete<Response<TimeOffDto>> callback);

    @GET("/api/provider/calendars")
    void calendars(OnComplete<Response<List<CalendarLinkDto>>> callback);

    @GET("/api/provider/calendars/config")
    void calendarConfig(OnComplete<Response<CalendarConfigDto>> callback);

    @POST("/api/provider/calendars")
    void connect(@Body ConnectCalendarDto connect,
            OnComplete<Response<CalendarLinkDto>> callback);

    @DELETE("/api/provider/calendars/{id}")
    void disconnect(@Path("id") String id, OnComplete<Response<CalendarLinkDto>> callback);

    /// Reads a calendar the server syncs, now rather than on its next round.
    @POST("/api/provider/calendars/{id}/sync")
    void sync(@Path("id") String id, OnComplete<Response<CalendarLinkDto>> callback);

    /// The busy time the phone read from one of its own calendars.
    @POST("/api/provider/busy")
    void uploadBusy(@Body BusyUploadDto busy, OnComplete<Response<CalendarLinkDto>> callback);

    @POST("/api/provider/appointments/{id}/complete")
    void complete(@Path("id") String id, OnComplete<Response<AppointmentDto>> callback);

    /// Takes the no-show fee. Only once the appointment has started.
    @POST("/api/provider/appointments/{id}/no-show")
    void noShow(@Path("id") String id, OnComplete<Response<AppointmentDto>> callback);

    @POST("/api/provider/appointments/{id}/cancel")
    void cancel(@Path("id") String id, @Body CancelDto cancel,
            OnComplete<Response<AppointmentDto>> callback);
}
