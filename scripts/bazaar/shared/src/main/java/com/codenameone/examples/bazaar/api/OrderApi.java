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
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Orders: a shopper's own, and a store's for its staff.
///
/// The merchant calls need the caller on the store's staff: reading for
/// anyone, changes for OWNER and MANAGER (FULFILMENT may add notes).
@RestClient
public interface OrderApi {
    /// The signed-in shopper's orders, newest first.
    @GET("/api/orders")
    void mine(OnComplete<Response<List<OrderDto>>> callback);

    @GET("/api/orders/{orderId}")
    void mineDetail(@Path("orderId") String orderId, OnComplete<Response<OrderDto>> callback);

    /// Cancels an order nothing of which has shipped: the stock goes back and
    /// the card hold is let go.
    @POST("/api/orders/{orderId}/cancel")
    void cancelMine(@Path("orderId") String orderId, OnComplete<Response<OrderDto>> callback);

    /// A store's orders, newest first. Every filter is optional; `from` and
    /// `to` are epoch millis, `q` matches the number, e-mail or name.
    @GET("/api/merchant/stores/{storeId}/orders")
    void list(@Path("storeId") String storeId, @Query("status") String status,
            @Query("financial") String financial, @Query("fulfilment") String fulfilment,
            @Query("channel") String channel, @Query("from") String from,
            @Query("to") String to, @Query("q") String q,
            OnComplete<Response<List<OrderDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/orders/{orderId}")
    void detail(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/note")
    void note(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body OrderNoteDto note, OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/cancel")
    void cancel(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    /// Records a manual payment (cash, bank transfer) of a pending order.
    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/mark-paid")
    void markPaid(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/refunds")
    void refund(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body RefundRequestDto refund, OnComplete<Response<OrderDto>> callback);

    @GET("/api/merchant/stores/{storeId}/orders/{orderId}/refunds")
    void refunds(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<List<RefundDto>>> callback);

    /// Writes a DRAFT order; nothing is reserved or charged yet.
    @POST("/api/merchant/stores/{storeId}/drafts")
    void createDraft(@Path("storeId") String storeId, @Body DraftOrderDto draft,
            OnComplete<Response<OrderDto>> callback);

    /// Records that the draft's invoice went to the customer.
    @POST("/api/merchant/stores/{storeId}/drafts/{orderId}/invoice")
    void sendInvoice(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    /// Turns a draft into an open order awaiting payment, reserving its stock.
    @POST("/api/merchant/stores/{storeId}/drafts/{orderId}/convert")
    void convertDraft(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);
}
