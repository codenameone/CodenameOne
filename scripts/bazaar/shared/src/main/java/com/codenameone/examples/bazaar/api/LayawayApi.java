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

/// Layaways: goods put aside for a customer on a deposit, paid off in parts at the counter, and
/// handed over when the balance is paid. Their stock is reserved meanwhile; cancelling releases
/// it and refunds what was paid less the layaway's cancellation fee.
@RestClient
public interface LayawayApi {
    /// Starts a layaway; a `clientId` already used answers the layaway it made.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/layaways")
    void create(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body LayawayRequestDto request, OnComplete<Response<LayawayDto>> callback);

    @GET("/api/merchant/stores/{storeId}/layaways")
    void layaways(@Path("storeId") String storeId, OnComplete<Response<List<LayawayDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/layaways/{layawayId}")
    void layaway(@Path("storeId") String storeId, @Path("layawayId") String layawayId, OnComplete<Response<LayawayDto>> callback);

    /// A payment toward the balance; paying it off completes the layaway and hands the goods over.
    @POST("/api/merchant/stores/{storeId}/layaways/{layawayId}/payments")
    void pay(@Path("storeId") String storeId, @Path("layawayId") String layawayId, @Body PosTenderDto payment, OnComplete<Response<LayawayDto>> callback);

    @POST("/api/merchant/stores/{storeId}/layaways/{layawayId}/cancel")
    void cancel(@Path("storeId") String storeId, @Path("layawayId") String layawayId, @Body LayawayCancelDto cancel, OnComplete<Response<LayawayDto>> callback);
}
