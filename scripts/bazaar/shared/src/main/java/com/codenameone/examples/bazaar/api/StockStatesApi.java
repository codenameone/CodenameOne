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

/// Stock beyond on hand and available: the committed, unavailable and
/// incoming states of every variant at every location, and serial and lot
/// tracking. Reading is for any of the store's staff; changes are for OWNER,
/// MANAGER and FULFILMENT.
@RestClient
public interface StockStatesApi {
    /// Every variant at every location (or only `locationId`) in every state.
    @GET("/api/merchant/stores/{storeId}/inventory/states")
    void states(@Path("storeId") String storeId, @Query("locationId") String locationId,
            OnComplete<Response<List<InventoryStateDto>>> callback);

    /// Moves units between available and an unavailable bucket.
    @POST("/api/merchant/stores/{storeId}/inventory/unavailable")
    void unavailable(@Path("storeId") String storeId, @Body UnavailableDto move,
            OnComplete<Response<InventoryStateDto>> callback);

    /// Every variant whose units are tracked by serial or lot.
    @GET("/api/merchant/stores/{storeId}/tracking")
    void tracking(@Path("storeId") String storeId,
            OnComplete<Response<List<TrackingDto>>> callback);

    /// Sets a variant's tracking mode: NONE, SERIAL or LOT.
    @POST("/api/merchant/stores/{storeId}/tracking")
    void setTracking(@Path("storeId") String storeId, @Body TrackingDto mode,
            OnComplete<Response<TrackingDto>> callback);

    /// Records the serials or the lot of units received.
    @POST("/api/merchant/stores/{storeId}/tracking/receive")
    void receive(@Path("storeId") String storeId, @Body TrackedReceiptDto receipt,
            OnComplete<Response<List<SerialDto>>> callback);

    /// Serials matching `q` (a serial, or the start of one), each with the
    /// order it was sold on.
    @GET("/api/merchant/stores/{storeId}/serials")
    void serials(@Path("storeId") String storeId, @Query("q") String q,
            OnComplete<Response<List<SerialDto>>> callback);

    /// The lots of a variant, soonest expiring first.
    @GET("/api/merchant/stores/{storeId}/lots")
    void lots(@Path("storeId") String storeId, @Query("variantId") String variantId,
            OnComplete<Response<List<LotDto>>> callback);

    /// Assigns scanned serials to a line of the order.
    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/serials")
    void assignSerials(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body SerialAssignDto serials, OnComplete<Response<List<SerialDto>>> callback);
}
