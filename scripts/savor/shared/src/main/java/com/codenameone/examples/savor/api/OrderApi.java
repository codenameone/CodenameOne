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

/// Ordering, following an order, and what a customer keeps: favorites and
/// addresses. Every call is about the signed-in customer's own.
@RestClient
public interface OrderApi {
    /// What a cart would cost now. Nothing is charged.
    @POST("/api/orders/quote")
    void quote(@Body CartDto cart, OnComplete<Response<QuoteDto>> callback);

    /// Places an order: the card is authorized for the total, and charged
    /// when the order is delivered.
    @POST("/api/orders")
    void place(@Body CartDto cart, OnComplete<Response<OrderDto>> callback);

    /// The orders under way.
    @GET("/api/orders/active")
    void active(OnComplete<Response<List<OrderDto>>> callback);

    @GET("/api/orders/history")
    void history(OnComplete<Response<List<OrderDto>>> callback);

    @GET("/api/orders/{id}")
    void order(@Path("id") String id, OnComplete<Response<OrderDto>> callback);

    /// Cancels an order the restaurant has not taken yet.
    @POST("/api/orders/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<OrderDto>> callback);

    @POST("/api/orders/{id}/rate")
    void rate(@Path("id") String id, @Body RatingDto rating, OnComplete<Response<OrderDto>> callback);

    /// Records the Live Activity the phone follows this order in, for the
    /// server to keep up to date by push.
    @POST("/api/orders/{id}/activity")
    void activity(@Path("id") String id, @Body DeliveryActivityDto activity,
            OnComplete<Response<DeliveryActivityDto>> callback);

    @GET("/api/favorites")
    void favorites(@Query("lat") double lat, @Query("lng") double lng,
            OnComplete<Response<List<RestaurantSummaryDto>>> callback);

    @PUT("/api/favorites/{id}")
    void favorite(@Path("id") String restaurantId, OnComplete<Response<RestaurantSummaryDto>> callback);

    @DELETE("/api/favorites/{id}")
    void unfavorite(@Path("id") String restaurantId, OnComplete<Response<RestaurantSummaryDto>> callback);

    @GET("/api/addresses")
    void addresses(OnComplete<Response<List<SavedAddressDto>>> callback);

    @POST("/api/addresses")
    void saveAddress(@Body SavedAddressDto address, OnComplete<Response<SavedAddressDto>> callback);

    @DELETE("/api/addresses/{id}")
    void removeAddress(@Path("id") String id, OnComplete<Response<SavedAddressDto>> callback);
}
