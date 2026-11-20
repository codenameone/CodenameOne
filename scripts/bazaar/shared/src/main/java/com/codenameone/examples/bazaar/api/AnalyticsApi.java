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

import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Merchant mode: a store's analytics.
@RestClient
public interface AnalyticsApi {
    /// The dashboard for `from` (inclusive) to `to` (exclusive), both in
    /// milliseconds; blank means the last 30 days up to now. Compared with the
    /// period of the same length just before.
    @GET("/api/merchant/stores/{storeId}/analytics")
    void dashboard(@Path("storeId") String storeId, @Query("from") String from,
            @Query("to") String to, OnComplete<Response<AnalyticsDto>> callback);

    /// Carts, checkouts, orders and fulfilled orders of the period, for a chart.
    @GET("/api/merchant/stores/{storeId}/analytics/funnel")
    void funnel(@Path("storeId") String storeId, @Query("from") String from,
            @Query("to") String to, OnComplete<Response<List<FunnelStepDto>>> callback);
}
