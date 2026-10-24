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
package com.codenameone.examples.reel.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Browsing: the home screen, a title's page, genres and search. Every call is per profile.
@RestClient
public interface CatalogApi {
    @GET("/api/home")
    void home(@Query("profile") String profile, OnComplete<Response<HomeDto>> callback);

    @GET("/api/titles/{id}")
    void title(@Path("id") String id, @Query("profile") String profile,
            OnComplete<Response<TitleDto>> callback);

    @GET("/api/genres")
    void genres(OnComplete<Response<List<GenreDto>>> callback);

    @GET("/api/genres/{id}")
    void genre(@Path("id") String id, @Query("profile") String profile,
            OnComplete<Response<List<TitleCardDto>>> callback);

    @GET("/api/search")
    void search(@Query("q") String q, @Query("profile") String profile,
            OnComplete<Response<SearchResultDto>> callback);

    /// Reports a title to the moderators.
    @POST("/api/titles/{id}/report")
    void report(@Path("id") String id, @Body ReportDto report, OnComplete<Response<String>> callback);
}
