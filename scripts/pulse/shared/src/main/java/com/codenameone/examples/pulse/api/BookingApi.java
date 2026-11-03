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

/// Places in classes for the caller's household.
///
/// A class with room gives a place at once; a full one puts the member on its
/// waitlist, and a place given up is handed to the first in the queue, who is
/// told by push. Cancelling inside the class's cut-off counts as a late
/// cancellation and, on a class pack, spends the credit.
@RestClient
public interface BookingApi {
    /// The household's places and queue entries from now on, soonest first.
    @GET("/api/bookings")
    void upcoming(OnComplete<Response<List<BookingDto>>> callback);

    /// The household's classes that have happened, newest first.
    @GET("/api/bookings/history")
    void history(OnComplete<Response<List<BookingDto>>> callback);

    /// Answers 403 for a member who is not the caller's, 409 when the member
    /// is outside the class's age band or already holds a place, and 402 when
    /// no membership covers the class.
    @POST("/api/bookings")
    void book(@Body BookRequestDto request, OnComplete<Response<BookingDto>> callback);

    @POST("/api/bookings/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<BookingDto>> callback);
}
