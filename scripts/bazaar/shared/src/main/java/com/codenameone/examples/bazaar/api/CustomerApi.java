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

/// Merchant mode: a store's customers, their history, store credit, notes and
/// tags, segments and an export.
@RestClient
public interface CustomerApi {
    /// Customers, filtered. Every filter is optional (null or blank):
    /// `q` matches name, email or phone; `tag` one tag; `minSpent` cents;
    /// `minOrders` a count; `marketing` "true" for subscribers; `segment` a
    /// [SegmentDto] id. `sort` is "name" (default), "spent", "orders" or "recent".
    @GET("/api/merchant/stores/{storeId}/customers")
    void list(@Path("storeId") String storeId, @Query("q") String q, @Query("tag") String tag,
            @Query("minSpent") String minSpent, @Query("minOrders") String minOrders,
            @Query("marketing") String marketing, @Query("segment") String segment,
            @Query("sort") String sort, OnComplete<Response<List<CustomerDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/customers/{customerId}")
    void detail(@Path("storeId") String storeId, @Path("customerId") String customerId,
            OnComplete<Response<CustomerDetailDto>> callback);

    /// Changes the note, the tags and the marketing consent.
    @POST("/api/merchant/stores/{storeId}/customers/{customerId}")
    void edit(@Path("storeId") String storeId, @Path("customerId") String customerId,
            @Body CustomerEditDto edit, OnComplete<Response<CustomerDto>> callback);

    @POST("/api/merchant/stores/{storeId}/customers/{customerId}/credit")
    void adjustCredit(@Path("storeId") String storeId, @Path("customerId") String customerId,
            @Body CreditAdjustDto adjust, OnComplete<Response<CustomerDto>> callback);

    /// The saved segments (VIP, lapsed, subscribers, new) with their counts.
    @GET("/api/merchant/stores/{storeId}/customer-segments")
    void segments(@Path("storeId") String storeId,
            OnComplete<Response<List<SegmentDto>>> callback);

    /// Every customer of a segment (all of them for a blank one) as CSV rows.
    @GET("/api/merchant/stores/{storeId}/customer-export")
    void export(@Path("storeId") String storeId, @Query("segment") String segment,
            OnComplete<Response<ExportDto>> callback);
}
