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

/// Running the business, for the OWNER role: what is offered, by whom, where,
/// under which rules, and how it is going.
@RestClient
public interface OwnerApi {
    @PUT("/api/owner/business")
    void saveBusiness(@Body BusinessDto business, OnComplete<Response<BusinessDto>> callback);

    @PUT("/api/owner/policy")
    void savePolicy(@Body PolicyDto policy, OnComplete<Response<PolicyDto>> callback);

    /// Every service, inactive ones included.
    @GET("/api/owner/services")
    void services(OnComplete<Response<List<ServiceDto>>> callback);

    /// Creates the service when its id is empty.
    @PUT("/api/owner/services")
    void saveService(@Body ServiceDto service, OnComplete<Response<ServiceDto>> callback);

    @GET("/api/owner/providers")
    void providers(OnComplete<Response<List<ProviderDto>>> callback);

    @PUT("/api/owner/providers")
    void saveProvider(@Body ProviderDto provider, OnComplete<Response<ProviderDto>> callback);

    @GET("/api/owner/providers/{username}/hours")
    void hours(@Path("username") String username,
            OnComplete<Response<WeekDto>> callback);

    @PUT("/api/owner/providers/{username}/hours")
    void saveHours(@Path("username") String username, @Body WeekDto week,
            OnComplete<Response<WeekDto>> callback);

    @GET("/api/owner/locations")
    void locations(OnComplete<Response<List<LocationDto>>> callback);

    @PUT("/api/owner/locations")
    void saveLocation(@Body LocationDto location, OnComplete<Response<LocationDto>> callback);

    @GET("/api/owner/resources")
    void resources(OnComplete<Response<List<ResourceDto>>> callback);

    @PUT("/api/owner/resources")
    void saveResource(@Body ResourceDto resource, OnComplete<Response<ResourceDto>> callback);

    /// Every provider's day side by side, for `day` (`yyyy-MM-dd`).
    @GET("/api/owner/schedule")
    void schedule(@Query("day") String day, OnComplete<Response<List<ScheduleDto>>> callback);

    /// What a day would offer under rules that are not saved yet.
    @POST("/api/owner/preview")
    void preview(@Body PreviewRequestDto request, OnComplete<Response<PreviewDto>> callback);

    @GET("/api/owner/dashboard")
    void dashboard(OnComplete<Response<DashboardDto>> callback);
}
