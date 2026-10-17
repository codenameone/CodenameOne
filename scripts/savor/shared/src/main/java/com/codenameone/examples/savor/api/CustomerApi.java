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

// tag::savor-contract-api[]
/// Asking for a delivery and following it.
@RestClient
public interface CustomerApi {
    @POST("/api/deliveries/quote")
    void quote(@Body DeliveryRequestDto request, OnComplete<Response<FareQuoteDto>> callback);

    @POST("/api/deliveries")
    void request(@Body DeliveryRequestDto request, OnComplete<Response<DeliveryDto>> callback);

    /// The delivery this customer has under way, or one whose state is NONE.
    @GET("/api/deliveries/active")
    void active(OnComplete<Response<DeliveryDto>> callback);

    @GET("/api/deliveries/history")
    void history(OnComplete<Response<List<DeliveryDto>>> callback);

    @GET("/api/deliveries/{id}")
    void delivery(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/deliveries/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<DeliveryDto>> callback);

    @POST("/api/deliveries/{id}/rate")
    void rate(@Path("id") String id, @Body RatingDto rating, OnComplete<Response<DeliveryDto>> callback);

    /// The free couriers around a point, for the cars on the map.
    @GET("/api/deliveries/nearby")
    void nearby(@Query("lat") double lat, @Query("lng") double lng,
            OnComplete<Response<List<NearbyCourierDto>>> callback);

    /// Tells the server which Live Activity this phone shows the delivery in, so
    /// that it can keep it up to date while the app is in the background.
    @POST("/api/deliveries/{id}/activity")
    void activity(@Path("id") String id, @Body DeliveryActivityDto activity,
            OnComplete<Response<DeliveryActivityDto>> callback);
}
// end::savor-contract-api[]
