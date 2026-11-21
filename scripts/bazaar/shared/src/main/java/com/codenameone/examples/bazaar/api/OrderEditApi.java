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

/// Editing an order after it was placed, for the store's owner and managers (and cashiers, with
/// a manager's approval when the policy wants one): lines added, removed or changed and the
/// discount set; totals and tax are worked out again, stock reservations follow, and the
/// difference is collected or refunded through the payment the order already has.
@RestClient
public interface OrderEditApi {
    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/edit")
    void edit(@Path("storeId") String storeId, @Path("orderId") String orderId, @Body OrderEditDto edit, OnComplete<Response<OrderEditResultDto>> callback);

    /// The order's edits, oldest first.
    @GET("/api/merchant/stores/{storeId}/orders/{orderId}/edits")
    void edits(@Path("storeId") String storeId, @Path("orderId") String orderId, OnComplete<Response<List<OrderEditEntryDto>>> callback);
}
