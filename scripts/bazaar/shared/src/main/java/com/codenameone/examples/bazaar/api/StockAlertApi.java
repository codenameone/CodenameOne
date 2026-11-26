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
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Back-in-stock alerts and pre-orders.
@RestClient
public interface StockAlertApi {
    /// Asks to hear when a variant is back; signed in.
    @POST("/api/shop/stores/{storeId}/stock-alerts")
    void subscribe(@Path("storeId") String storeId,
            @Body StockAlertDto alert,
            OnComplete<Response<StockAlertDto>> callback);

    /// A variant's pre-order terms, for the product page.
    @GET("/api/shop/stores/{storeId}/variants/{variantId}/preorder")
    void preorderOf(@Path("storeId") String storeId,
            @Path("variantId") String variantId,
            OnComplete<Response<PreorderDto>> callback);

    /// Everyone waiting for a variant, newest first.
    @GET("/api/merchant/stores/{storeId}/stock-alerts")
    void list(@Path("storeId") String storeId,
            OnComplete<Response<List<StockAlertDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/preorders")
    void preorders(@Path("storeId") String storeId,
            OnComplete<Response<List<PreorderDto>>> callback);

    /// Turns pre-orders on or off for a variant.
    @POST("/api/merchant/stores/{storeId}/variants/{variantId}/preorder")
    void savePreorder(@Path("storeId") String storeId,
            @Path("variantId") String variantId,
            @Body PreorderDto preorder,
            OnComplete<Response<PreorderDto>> callback);
}
