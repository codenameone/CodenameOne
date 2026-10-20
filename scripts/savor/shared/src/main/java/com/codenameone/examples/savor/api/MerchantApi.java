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

/// Running a restaurant: its orders, its menu and its details. Every call is
/// about the restaurant whose staff the signed-in account is on.
@RestClient
public interface MerchantApi {
    @GET("/api/merchant/restaurant")
    void restaurant(OnComplete<Response<RestaurantDto>> callback);

    @PUT("/api/merchant/restaurant")
    void save(@Body RestaurantEditDto details, OnComplete<Response<RestaurantDto>> callback);

    /// Stops taking orders for now, or starts again.
    @POST("/api/merchant/paused")
    void pause(@Query("paused") boolean paused, OnComplete<Response<RestaurantDto>> callback);

    @GET("/api/merchant/summary")
    void summary(OnComplete<Response<MerchantSummaryDto>> callback);

    /// The orders under way, oldest first.
    @GET("/api/merchant/orders")
    void orders(OnComplete<Response<List<OrderDto>>> callback);

    @GET("/api/merchant/orders/history")
    void history(OnComplete<Response<List<OrderDto>>> callback);

    @POST("/api/merchant/orders/{id}/accept")
    void accept(@Path("id") String id, @Body AcceptDto accept, OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/orders/{id}/reject")
    void reject(@Path("id") String id, @Body ReasonDto reason, OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/orders/{id}/ready")
    void ready(@Path("id") String id, OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/sections")
    void addSection(@Body MenuSectionDto section, OnComplete<Response<MenuSectionDto>> callback);

    @PUT("/api/merchant/sections/{id}")
    void saveSection(@Path("id") String id, @Body MenuSectionDto section,
            OnComplete<Response<MenuSectionDto>> callback);

    @DELETE("/api/merchant/sections/{id}")
    void removeSection(@Path("id") String id, OnComplete<Response<MenuSectionDto>> callback);

    @POST("/api/merchant/dishes")
    void addDish(@Body DishDto dish, OnComplete<Response<DishDto>> callback);

    @PUT("/api/merchant/dishes/{id}")
    void saveDish(@Path("id") String id, @Body DishDto dish, OnComplete<Response<DishDto>> callback);

    @DELETE("/api/merchant/dishes/{id}")
    void removeDish(@Path("id") String id, OnComplete<Response<DishDto>> callback);

    /// Marks a dish sold out, or back on.
    @POST("/api/merchant/dishes/{id}/available")
    void available(@Path("id") String id, @Query("available") boolean available,
            OnComplete<Response<DishDto>> callback);

    @POST("/api/merchant/photos")
    void upload(@Body PhotoDto photo, OnComplete<Response<PhotoDto>> callback);
}
