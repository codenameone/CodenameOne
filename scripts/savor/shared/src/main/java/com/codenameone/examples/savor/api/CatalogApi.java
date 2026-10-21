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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Finding something to eat: the restaurants near a place, a restaurant's menu,
/// their pictures, and applying to sell on the service.
@RestClient
public interface CatalogApi {
    /// The approved restaurants that deliver to a point, nearest and open first.
    ///
    /// @param cuisine one of the cuisines, or empty for all
    /// @param text words to look for in names, cuisines and dishes; empty for none
    /// @param sort `relevance`, `rating`, `eta` or `fee`
    @GET("/api/restaurants")
    void restaurants(@Query("lat") double lat, @Query("lng") double lng,
            @Query("cuisine") String cuisine, @Query("q") String text, @Query("sort") String sort,
            OnComplete<Response<List<RestaurantSummaryDto>>> callback);

    @GET("/api/restaurants/{id}")
    void restaurant(@Path("id") String id, @Query("lat") double lat, @Query("lng") double lng,
            OnComplete<Response<RestaurantDto>> callback);

    @GET("/api/photos/{id}")
    void photo(@Path("id") String id, OnComplete<Response<PhotoDto>> callback);

    /// Applies to list a restaurant. An admin approves it, and the applicant
    /// becomes its first member of staff.
    @POST("/api/restaurants/application")
    void apply(@Body RestaurantEditDto application, OnComplete<Response<RestaurantDto>> callback);

    /// The restaurant this account applied for, or one whose status is empty.
    @GET("/api/restaurants/application")
    void application(OnComplete<Response<RestaurantDto>> callback);

    /// A picture for an application, before there is a restaurant to keep it.
    @POST("/api/restaurants/application/photo")
    void applicationPhoto(@Body PhotoDto photo, OnComplete<Response<PhotoDto>> callback);
}
