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

/// Stock: levels per variant and location, the ledger of every change,
/// transfers between locations, suppliers and purchase orders.
///
/// Every call needs the caller on the store's staff. Reading and adjusting
/// stock is for OWNER, MANAGER and FULFILMENT; suppliers and purchase orders
/// for OWNER and MANAGER; a lookup by barcode or SKU for anyone on the staff,
/// the register included.
@RestClient
public interface InventoryApi {
    /// Levels, optionally of one location or one product; `low` set to `true`
    /// keeps only those at or under their reorder point.
    @GET("/api/merchant/stores/{storeId}/inventory")
    void levels(@Path("storeId") String storeId, @Query("location") String locationId,
            @Query("product") String productId, @Query("low") String low,
            OnComplete<Response<List<LevelDto>>> callback);

    /// Levels at or under their reorder point.
    @GET("/api/merchant/stores/{storeId}/inventory/low")
    void lowStock(@Path("storeId") String storeId, OnComplete<Response<List<LevelDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/inventory/adjust")
    void adjust(@Path("storeId") String storeId, @Body AdjustDto adjust,
            OnComplete<Response<LevelDto>> callback);

    /// Records a stock count: on hand becomes `quantity`, and the ledger gets
    /// a COUNT line with the difference.
    @POST("/api/merchant/stores/{storeId}/inventory/count")
    void count(@Path("storeId") String storeId, @Body AdjustDto count,
            OnComplete<Response<LevelDto>> callback);

    /// Sets the reorder point of a level: `quantity` is the new point.
    @POST("/api/merchant/stores/{storeId}/inventory/reorder-point")
    void reorderPoint(@Path("storeId") String storeId, @Body AdjustDto point,
            OnComplete<Response<LevelDto>> callback);

    /// The ledger, newest first, optionally of one variant or location.
    @GET("/api/merchant/stores/{storeId}/inventory/moves")
    void moves(@Path("storeId") String storeId, @Query("variant") String variantId,
            @Query("location") String locationId, @Query("limit") String limit,
            OnComplete<Response<List<MoveDto>>> callback);

    /// A variant by barcode or SKU, matched exactly (SKU ignoring case).
    @GET("/api/merchant/stores/{storeId}/lookup")
    void lookup(@Path("storeId") String storeId, @Query("code") String code,
            OnComplete<Response<VariantHitDto>> callback);

    @GET("/api/merchant/stores/{storeId}/transfers")
    void transfers(@Path("storeId") String storeId,
            OnComplete<Response<List<TransferDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/transfers")
    void createTransfer(@Path("storeId") String storeId, @Body TransferDto transfer,
            OnComplete<Response<TransferDto>> callback);

    /// Takes the units off hand at the source; the destination counts them as
    /// incoming.
    @POST("/api/merchant/stores/{storeId}/transfers/{transferId}/send")
    void sendTransfer(@Path("storeId") String storeId, @Path("transferId") String transferId,
            OnComplete<Response<TransferDto>> callback);

    @POST("/api/merchant/stores/{storeId}/transfers/{transferId}/receive")
    void receiveTransfer(@Path("storeId") String storeId, @Path("transferId") String transferId,
            @Body ReceiveDto receive, OnComplete<Response<TransferDto>> callback);

    /// Cancels a transfer; what was sent and not received goes back on hand at
    /// the source.
    @POST("/api/merchant/stores/{storeId}/transfers/{transferId}/cancel")
    void cancelTransfer(@Path("storeId") String storeId, @Path("transferId") String transferId,
            OnComplete<Response<TransferDto>> callback);

    @GET("/api/merchant/stores/{storeId}/suppliers")
    void suppliers(@Path("storeId") String storeId,
            OnComplete<Response<List<SupplierDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/suppliers")
    void saveSupplier(@Path("storeId") String storeId, @Body SupplierDto supplier,
            OnComplete<Response<SupplierDto>> callback);

    /// Refused while the supplier has an open purchase order.
    @DELETE("/api/merchant/stores/{storeId}/suppliers/{supplierId}")
    void deleteSupplier(@Path("storeId") String storeId, @Path("supplierId") String supplierId,
            OnComplete<Response<SupplierDto>> callback);

    @GET("/api/merchant/stores/{storeId}/purchase-orders")
    void purchaseOrders(@Path("storeId") String storeId,
            OnComplete<Response<List<PurchaseOrderDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/purchase-orders/{orderId}")
    void purchaseOrder(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<PurchaseOrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/purchase-orders")
    void createPurchaseOrder(@Path("storeId") String storeId, @Body PurchaseOrderDto order,
            OnComplete<Response<PurchaseOrderDto>> callback);

    /// Places a DRAFT order: its units count as incoming at its location.
    @POST("/api/merchant/stores/{storeId}/purchase-orders/{orderId}/order")
    void placePurchaseOrder(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<PurchaseOrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/purchase-orders/{orderId}/receive")
    void receivePurchaseOrder(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body ReceiveDto receive, OnComplete<Response<PurchaseOrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/purchase-orders/{orderId}/cancel")
    void cancelPurchaseOrder(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<PurchaseOrderDto>> callback);
}
