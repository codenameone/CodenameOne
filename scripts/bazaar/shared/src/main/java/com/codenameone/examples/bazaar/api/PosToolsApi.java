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

/// The point of sale's tools beyond the sale itself: the store's policy (owner and manager),
/// commission rates and the commission report (owner and manager), manager approvals by PIN,
/// parked carts and the sell screen's smart grid (any staff reads it; owner and manager lay it
/// out). Barcode and price labels for variants are a PDF at
/// `/api/merchant/stores/{storeId}/pos/labels.pdf?variants=<id>,<id>&copies=<n>`.
@RestClient
public interface PosToolsApi {
    @GET("/api/merchant/stores/{storeId}/pos/policy")
    void policy(@Path("storeId") String storeId, OnComplete<Response<PosPolicyDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/policy")
    void savePolicy(@Path("storeId") String storeId, @Body PosPolicyDto policy, OnComplete<Response<PosPolicyDto>> callback);

    /// Every staff member with their commission rate.
    @GET("/api/merchant/stores/{storeId}/pos/staff-rates")
    void staffRates(@Path("storeId") String storeId, OnComplete<Response<List<StaffRateDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/staff-rates")
    void saveStaffRate(@Path("storeId") String storeId, @Body StaffRateDto rate, OnComplete<Response<StaffRateDto>> callback);

    /// Counter sales per staff member from `from` (inclusive) to `to` (exclusive), both epoch
    /// milliseconds; empty means the last 30 days.
    @GET("/api/merchant/stores/{storeId}/pos/commissions")
    void commissions(@Path("storeId") String storeId, @Query("from") String from, @Query("to") String to, OnComplete<Response<List<CommissionDto>>> callback);

    /// A manager's PIN approving a discount, price override, refund or order edit. 401 for a wrong
    /// PIN, 403 when the member is not an owner or manager, 423 while their PIN is locked.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/approvals")
    void approve(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body ApprovalRequestDto request, OnComplete<Response<ApprovalDto>> callback);

    /// The carts parked at a register and not yet resumed.
    @GET("/api/merchant/stores/{storeId}/pos/registers/{registerId}/parked")
    void parked(@Path("storeId") String storeId, @Path("registerId") String registerId, OnComplete<Response<List<ParkedCartDto>>> callback);

    /// Parks a cart; a `clientId` already parked answers the cart kept the first time.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/parked")
    void park(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body ParkedCartDto cart, OnComplete<Response<ParkedCartDto>> callback);

    /// Takes a parked cart back (by its id or the register's `clientId`); asking again answers it
    /// again, so a register replaying its offline queue is safe.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/parked/{parkedId}/resume")
    void resume(@Path("storeId") String storeId, @Path("registerId") String registerId, @Path("parkedId") String parkedId, OnComplete<Response<ParkedCartDto>> callback);

    /// Drops a parked cart; asking again is safe.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/parked/{parkedId}/discard")
    void discard(@Path("storeId") String storeId, @Path("registerId") String registerId, @Path("parkedId") String parkedId, OnComplete<Response<ParkedCartDto>> callback);

    @GET("/api/merchant/stores/{storeId}/pos/locations/{locationId}/grid")
    void grid(@Path("storeId") String storeId, @Path("locationId") String locationId, OnComplete<Response<PosGridDto>> callback);

    /// Replaces a location's grid.
    @POST("/api/merchant/stores/{storeId}/pos/locations/{locationId}/grid")
    void saveGrid(@Path("storeId") String storeId, @Path("locationId") String locationId, @Body PosGridDto grid, OnComplete<Response<PosGridDto>> callback);
}
