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
package com.codenameone.examples.bastion.api;

import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The numbers behind a customer's charts, all of their own accounts, and the
/// market candles anyone signed in may see.
@RestClient
public interface InsightsApi {
    /// The account's closing balance for each of the last `days` UTC days
    /// (default 30, at most 366), today last.
    @GET("/api/accounts/{id}/balance-history")
    void balanceHistory(@Path("id") String id, @Query("days") String days,
            OnComplete<Response<List<BalancePointDto>>> callback);

    /// Money in and out of the customer's accounts in `asset` (default USD)
    /// per UTC day over the last `days` (default 30, at most 366).
    @GET("/api/insights/cashflow")
    void cashflow(@Query("asset") String asset, @Query("days") String days,
            OnComplete<Response<List<CashflowDayDto>>> callback);

    /// What the customer holds, per asset, valued in US cents.
    @GET("/api/insights/allocation")
    void allocation(OnComplete<Response<List<AllocationDto>>> callback);

    /// OHLC candles of `interval` milliseconds (default an hour; one minute
    /// to one day), the newest `limit` (default 168, at most 500).
    @GET("/api/exchange/markets/{id}/candles")
    void candles(@Path("id") String id, @Query("interval") String interval,
            @Query("limit") String limit, OnComplete<Response<List<CandleDto>>> callback);
}
