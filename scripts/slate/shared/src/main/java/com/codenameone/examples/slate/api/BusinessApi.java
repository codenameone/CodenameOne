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

/// What can be booked, with whom, and when: the catalogue every signed-in
/// account reads, and the slot search over it.
@RestClient
public interface BusinessApi {
    @GET("/api/business")
    void business(OnComplete<Response<BusinessDto>> callback);

    @GET("/api/business/policy")
    void policy(OnComplete<Response<PolicyDto>> callback);

    @GET("/api/locations")
    void locations(OnComplete<Response<List<LocationDto>>> callback);

    /// The services on offer, in the owner's order. Inactive ones are left out.
    @GET("/api/services")
    void services(OnComplete<Response<List<ServiceDto>>> callback);

    @GET("/api/providers")
    void providers(OnComplete<Response<List<ProviderDto>>> callback);

    /// The starts `day` (`yyyy-MM-dd`, in the business's zone) offers for a
    /// service at a location. `providers` is empty for anyone, or account
    /// names separated by commas, every one of whom must be free.
    @GET("/api/slots")
    void slots(@Query("service") String service, @Query("location") String location,
            @Query("day") String day, @Query("providers") String providers,
            OnComplete<Response<DaySlotsDto>> callback);

    /// The first times that suit every provider named, over several days.
    @POST("/api/slots/find")
    void findTime(@Body FindTimeRequestDto request, OnComplete<Response<FindTimeDto>> callback);
}
