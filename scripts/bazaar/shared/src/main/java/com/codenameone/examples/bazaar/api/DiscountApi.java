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
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// A store's discounts, for OWNER and MANAGER.
@RestClient
public interface DiscountApi {
    @GET("/api/merchant/stores/{storeId}/discounts")
    void list(@Path("storeId") String storeId, OnComplete<Response<List<DiscountDto>>> callback);

    /// Creates a discount (no `id`) or replaces one.
    @POST("/api/merchant/stores/{storeId}/discounts")
    void save(@Path("storeId") String storeId, @Body DiscountDto discount,
            OnComplete<Response<DiscountDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/discounts/{discountId}")
    void delete(@Path("storeId") String storeId, @Path("discountId") String discountId,
            OnComplete<Response<DiscountDto>> callback);

    /// Makes `count` unique single-use codes from a discount; answers them as CSV rows.
    @POST("/api/merchant/stores/{storeId}/discounts/{discountId}/codes")
    void generateCodes(@Path("storeId") String storeId, @Path("discountId") String discountId,
            @Body DiscountCodesDto codes, OnComplete<Response<ExportDto>> callback);

    /// Every code generated from a discount, with whether it was used, as CSV rows.
    @GET("/api/merchant/stores/{storeId}/discounts/{discountId}/codes")
    void codes(@Path("storeId") String storeId, @Path("discountId") String discountId,
            OnComplete<Response<ExportDto>> callback);
}
