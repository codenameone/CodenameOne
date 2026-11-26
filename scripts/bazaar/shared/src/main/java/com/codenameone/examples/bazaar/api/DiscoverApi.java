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

import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Shopping across stores: the discovery feed and search over every active
/// store, which need no sign-in, and the stores a shopper follows, which do.
///
/// A feed page mixes the stores, a product of each in turn, so one large
/// catalog does not fill it. `sort` is `newest` (each store's newest first) or
/// `popular` (rating, then number of ratings); `q` narrows it to products whose
/// title, vendor, type or tags match; `size` is 1 to 60, 24 when absent.
@RestClient
public interface DiscoverApi {
    @GET("/api/shop/discover")
    void feed(@Query("q") String q, @Query("sort") String sort, @Query("size") String size,
            OnComplete<Response<ProductPageDto>> callback);

    /// The stores the caller follows, newest follow first.
    @GET("/api/follows")
    void following(OnComplete<Response<List<StoreDto>>> callback);

    /// Follows an active store; following it again changes nothing.
    @PUT("/api/follows/{storeId}")
    void follow(@Path("storeId") String storeId, OnComplete<Response<List<StoreDto>>> callback);

    @DELETE("/api/follows/{storeId}")
    void unfollow(@Path("storeId") String storeId,
            OnComplete<Response<List<StoreDto>>> callback);

    /// New products of the followed stores, mixed as the public feed is.
    @GET("/api/follows/feed")
    void followingFeed(@Query("size") String size, OnComplete<Response<ProductPageDto>> callback);
}
