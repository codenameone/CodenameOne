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
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Post-purchase upsell: the merchant's one offered product, the offer on
/// an order just placed, and taking it as a separate add-on order.
@RestClient
public interface UpsellApi {
    @GET("/api/merchant/stores/{storeId}/upsell")
    void upsell(@Path("storeId") String storeId, OnComplete<Response<UpsellDto>> callback);

    @PUT("/api/merchant/stores/{storeId}/upsell")
    void setUpsell(@Path("storeId") String storeId, @Body UpsellDto upsell,
            OnComplete<Response<UpsellDto>> callback);

    @GET("/api/checkout/{storeId}/orders/{orderId}/upsell")
    void offer(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<UpsellDto>> callback);

    @POST("/api/checkout/{storeId}/orders/{orderId}/upsell")
    void accept(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    @POST("/api/checkout/{storeId}/buy-now")
    void buyNow(@Path("storeId") String storeId, @Body BuyNowDto buy,
            OnComplete<Response<OrderDto>> callback);
}
