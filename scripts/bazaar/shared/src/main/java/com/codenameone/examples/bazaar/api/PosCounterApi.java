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
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The counter's connected work: card readers (paired by OWNER and MANAGER) and the card
/// payments pushed to them, the queue of online orders collected at the register's location,
/// and sales shipped to the customer. All of it needs the network.
@RestClient
public interface PosCounterApi {
    @GET("/api/merchant/stores/{storeId}/pos/readers")
    void readers(@Path("storeId") String storeId, OnComplete<Response<List<CardReaderDto>>> callback);

    /// Pairs a reader by the code it shows and places it at `registerId`.
    @POST("/api/merchant/stores/{storeId}/pos/readers")
    void addReader(@Path("storeId") String storeId, @Body CardReaderDto reader, OnComplete<Response<CardReaderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/readers/{readerId}/remove")
    void removeReader(@Path("storeId") String storeId, @Path("readerId") String readerId, OnComplete<Response<CardReaderDto>> callback);

    /// Pushes a payment to the register's reader; a `clientId` already used answers its payment.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/card-payments")
    void startCardPayment(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body CardPaymentDto payment, OnComplete<Response<CardPaymentDto>> callback);

    /// The payment as the reader has it now; asked until it is no longer PENDING.
    @GET("/api/merchant/stores/{storeId}/pos/card-payments/{paymentId}")
    void cardPayment(@Path("storeId") String storeId, @Path("paymentId") String paymentId, OnComplete<Response<CardPaymentDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/card-payments/{paymentId}/cancel")
    void cancelCardPayment(@Path("storeId") String storeId, @Path("paymentId") String paymentId, OnComplete<Response<CardPaymentDto>> callback);

    /// Pickups at the register's location not yet collected, oldest first.
    @GET("/api/merchant/stores/{storeId}/pos/registers/{registerId}/pickups")
    void pickups(@Path("storeId") String storeId, @Path("registerId") String registerId, OnComplete<Response<List<PosPickupDto>>> callback);

    /// Ready to collect: the order's open lines become a pickup, and the shopper is told by
    /// e-mail and notification.
    @POST("/api/merchant/stores/{storeId}/pos/pickups/{orderId}/ready")
    void pickupReady(@Path("storeId") String storeId, @Path("orderId") String orderId, OnComplete<Response<PosPickupDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pos/pickups/{orderId}/picked-up")
    void pickedUp(@Path("storeId") String storeId, @Path("orderId") String orderId, OnComplete<Response<PosPickupDto>> callback);

    /// The delivery options for shipping these lines to an address, each with the sale's total
    /// that way.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/ship-quote")
    void shipQuote(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body PosShipQuoteDto quote, OnComplete<Response<List<PosShipOptionDto>>> callback);

    /// Records a sale shipped to the customer; answered as an upload of one sale is.
    @POST("/api/merchant/stores/{storeId}/pos/registers/{registerId}/ship-sales")
    void shipSale(@Path("storeId") String storeId, @Path("registerId") String registerId, @Body PosShipSaleDto sale, OnComplete<Response<PosSaleResultDto>> callback);
}
