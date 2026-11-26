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

/// The point of sale. Registers are managed by OWNER and MANAGER; everything
/// else is open to any of the store's staff, cashiers included. A register
/// works offline from [#catalog] and uploads what it sold with [#sync], which
/// never refuses a sale for want of stock.
@RestClient
public interface PosApi {
    @GET("/api/merchant/stores/{storeId}/pos/registers")
    void registers(@Path("storeId") String storeId,
            OnComplete<Response<List<PosRegisterDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/registers")
    void addRegister(@Path("storeId") String storeId, @Body PosRegisterDto register,
            OnComplete<Response<PosRegisterDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}")
    void saveRegister(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Body PosRegisterDto register, OnComplete<Response<PosRegisterDto>> callback);

    /// Takes a register out of use (it is kept: its shifts and sales name it).
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/remove")
    void removeRegister(@Path("storeId") String storeId, @Path("registerId") String registerId,
            OnComplete<Response<PosRegisterDto>> callback);

    /// Checks a staff member's PIN at a register. Five wrong PINs in a row
    /// lock the PIN for fifteen minutes (423).
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/sign-in")
    void signIn(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Body PinSignInDto pin, OnComplete<Response<PosSessionDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/shifts")
    void openShift(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Body OpenShiftDto open, OnComplete<Response<ShiftDto>> callback);

    @GET("/api/merchant/stores/{storeId}/pos/shifts/{shiftId}")
    void shift(@Path("storeId") String storeId, @Path("shiftId") String shiftId,
            OnComplete<Response<ShiftDto>> callback);

    @GET("/api/merchant/stores/{storeId}/pos/shifts/{shiftId}/cash")
    void cashMoves(@Path("storeId") String storeId, @Path("shiftId") String shiftId,
            OnComplete<Response<List<CashMoveDto>>> callback);

    /// Cash in (positive) or out (negative), with a reason.
    @POST("/api/merchant/stores/{storeId}/pos/shifts/{shiftId}/cash")
    void moveCash(@Path("storeId") String storeId, @Path("shiftId") String shiftId,
            @Body CashMoveDto move, OnComplete<Response<ShiftDto>> callback);

    /// Closes the shift with the cash counted; the Z report is then at
    /// `/api/merchant/stores/{storeId}/shifts/{shiftId}/report.pdf`.
    @POST("/api/merchant/stores/{storeId}/pos/shifts/{shiftId}/close")
    void closeShift(@Path("storeId") String storeId, @Path("shiftId") String shiftId,
            @Body CloseShiftDto close, OnComplete<Response<ShiftDto>> callback);

    /// The offline catalog; pass the `version` already held (or empty) and an
    /// unchanged catalog is answered with `unchanged` and empty lists.
    @GET("/api/merchant/stores/{storeId}/pos/registers/{registerId}/catalog")
    void catalog(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Query("version") String version, OnComplete<Response<PosCatalogDto>> callback);

    /// The store's customers matching an e-mail, a phone number or a name.
    @GET("/api/merchant/stores/{storeId}/pos/customers")
    void customers(@Path("storeId") String storeId, @Query("q") String q,
            OnComplete<Response<List<PosCustomerDto>>> callback);

    /// Uploads sales; each is OK, DUPLICATE (already uploaded) or ERROR.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/sales")
    void sync(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Body PosSyncDto sales, OnComplete<Response<PosSyncResultDto>> callback);

    /// A return at the counter, refunded in cash or to the original tenders.
    /// An exchange is a return followed by a new sale.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/orders/{orderId}/return")
    void counterReturn(@Path("storeId") String storeId, @Path("registerId") String registerId,
            @Path("orderId") String orderId, @Body PosReturnDto request,
            OnComplete<Response<PosReturnResultDto>> callback);

    /// The receipt as text; its PDF is at `.../orders/{orderId}/receipt.pdf`.
    @GET("/api/merchant/stores/{storeId}/pos/orders/{orderId}/receipt")
    void receipt(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<PosReceiptDto>> callback);

    /// Sends the receipt by text message and/or records it as e-mailed.
    @POST("/api/merchant/stores/{storeId}/pos/orders/{orderId}/receipt")
    void sendReceipt(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body ReceiptSendDto to, OnComplete<Response<PosReceiptDto>> callback);
}
