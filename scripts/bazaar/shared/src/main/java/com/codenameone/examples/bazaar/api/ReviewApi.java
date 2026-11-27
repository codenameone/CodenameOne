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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Product reviews: anyone reads the published ones; a signed-in shopper
/// writes one per product (verified when they bought it), which waits for
/// the store's OWNER or MANAGER to publish or reject it.
@RestClient
public interface ReviewApi {
    @GET("/api/shop/products/{productId}/reviews")
    void reviews(@Path("productId") String productId, @Query("page") int page,
            @Query("size") int size, OnComplete<Response<ReviewPageDto>> callback);

    @POST("/api/products/{productId}/reviews")
    void submit(@Path("productId") String productId, @Body ReviewDto review,
            OnComplete<Response<ReviewDto>> callback);

    /// The store's reviews, newest first; `status` empty for all.
    @GET("/api/merchant/stores/{storeId}/reviews")
    void storeReviews(@Path("storeId") String storeId, @Query("status") String status,
            OnComplete<Response<List<ReviewDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/reviews/{reviewId}/publish")
    void publish(@Path("storeId") String storeId, @Path("reviewId") String reviewId,
            OnComplete<Response<ReviewDto>> callback);

    @POST("/api/merchant/stores/{storeId}/reviews/{reviewId}/reject")
    void reject(@Path("storeId") String storeId, @Path("reviewId") String reviewId,
            OnComplete<Response<ReviewDto>> callback);
}
