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

/// Sending orders out: the merchant's queue and board, shipments from a
/// location (split as the merchant likes), carrier rates and labels, local
/// pickup and delivery, and tracking for the shopper and for anyone holding a
/// tracking number.
///
/// Printable documents are plain `GET`s answered with `application/pdf`:
/// `/api/merchant/stores/{storeId}/orders/{orderId}/invoice.pdf`,
/// `.../packing-slip.pdf`, `/api/merchant/stores/{storeId}/fulfillments/{id}/label.pdf`,
/// `/api/merchant/stores/{storeId}/purchase-orders/{id}/po.pdf`,
/// `/api/merchant/stores/{storeId}/refunds/{id}/credit-note.pdf`,
/// `/api/merchant/stores/{storeId}/shifts/{id}/report.pdf` and, for the
/// shopper, `/api/orders/{orderId}/invoice.pdf`.
@RestClient
public interface FulfillmentApi {
    /// Orders with units still to send, oldest first; only lines held at
    /// `locationId` when it is given.
    @GET("/api/merchant/stores/{storeId}/fulfilment/queue")
    void queue(@Path("storeId") String storeId, @Query("locationId") String locationId,
            OnComplete<Response<List<UnfulfilledDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/orders/{orderId}/fulfillments")
    void ofOrder(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<List<FulfillmentDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/fulfillments")
    void create(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body NewFulfillmentDto fulfillment, OnComplete<Response<FulfillmentDto>> callback);

    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/pack")
    void pack(@Path("storeId") String storeId, @Path("id") String id, @Body PackDto pack,
            OnComplete<Response<FulfillmentDto>> callback);

    @GET("/api/merchant/stores/{storeId}/fulfillments/{id}/rates")
    void rates(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<List<RateQuoteDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/label")
    void buyLabel(@Path("storeId") String storeId, @Path("id") String id,
            @Body LabelRequestDto label, OnComplete<Response<FulfillmentDto>> callback);

    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/ship")
    void ship(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<FulfillmentDto>> callback);

    /// A pickup is packed and waiting at the counter.
    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/pickup-ready")
    void pickupReady(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<FulfillmentDto>> callback);

    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/picked-up")
    void pickedUp(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<FulfillmentDto>> callback);

    /// A local delivery reached the door.
    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/delivered")
    void delivered(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<FulfillmentDto>> callback);

    @POST("/api/merchant/stores/{storeId}/fulfillments/{id}/cancel")
    void cancel(@Path("storeId") String storeId, @Path("id") String id,
            OnComplete<Response<FulfillmentDto>> callback);

    /// The board: To pack, Packed, Shipped, Delivered.
    @GET("/api/merchant/stores/{storeId}/fulfilment/board")
    void board(@Path("storeId") String storeId,
            OnComplete<Response<List<BoardColumnDto>>> callback);

    /// Moves a card one column on, doing what that column means; answers the
    /// board as it now is.
    @POST("/api/merchant/stores/{storeId}/fulfilment/board/move")
    void move(@Path("storeId") String storeId, @Body BoardMoveDto move,
            OnComplete<Response<List<BoardColumnDto>>> callback);

    /// The shopper's own order's shipments and their scans.
    @GET("/api/orders/{orderId}/tracking")
    void tracking(@Path("orderId") String orderId,
            OnComplete<Response<List<FulfillmentDto>>> callback);

    /// Public: what a tracking number shows, without names or addresses.
    @GET("/api/shop/tracking/{trackingNumber}")
    void track(@Path("trackingNumber") String trackingNumber,
            OnComplete<Response<FulfillmentDto>> callback);
}
