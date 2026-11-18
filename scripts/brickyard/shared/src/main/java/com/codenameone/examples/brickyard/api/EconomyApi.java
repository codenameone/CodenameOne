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
package com.codenameone.examples.brickyard.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Bricks: the wallet and its ledger, store receipts, game passes, developer products, the catalog, Plus and creator payouts.
@RestClient
public interface EconomyApi {
    @GET("/api/me/wallet")
    void wallet(OnComplete<Response<WalletDto>> callback);

    @POST("/api/me/wallet/receipts")
    void redeemReceipt(@Body ReceiptDto receipt, OnComplete<Response<WalletDto>> callback);

    @POST("/api/experiences/{experienceId}/products")
    void createProduct(@Path("experienceId") String experienceId, @Body ProductDto product, OnComplete<Response<ProductDto>> callback);

    @GET("/api/experiences/{experienceId}/products")
    void products(@Path("experienceId") String experienceId, OnComplete<Response<List<ProductDto>>> callback);

    @POST("/api/products/{productId}/buy")
    void buyProduct(@Path("productId") String productId, OnComplete<Response<OwnershipDto>> callback);

    @GET("/api/engine/ownership")
    void owns(@Query("username") String username, @Query("productId") String productId, OnComplete<Response<OwnershipDto>> callback);

    @GET("/api/catalog")
    void catalog(OnComplete<Response<List<CatalogItemDto>>> callback);

    @POST("/api/catalog/{itemId}/buy")
    void buyItem(@Path("itemId") String itemId, OnComplete<Response<OwnershipDto>> callback);

    @GET("/api/me/inventory")
    void inventory(OnComplete<Response<List<OwnershipDto>>> callback);

    @GET("/api/me/plus")
    void plus(OnComplete<Response<PlusDto>> callback);

    @POST("/api/engine/plus-play")
    void reportPlay(@Body PlayReportDto report, OnComplete<Response<PlayReportDto>> callback);

    @POST("/api/me/payouts")
    void requestPayout(@Body PayoutDto payout, OnComplete<Response<PayoutDto>> callback);

    /// Renames or reprices a product of the caller's experience.
    @POST("/api/products/{productId}")
    void updateProduct(@Path("productId") String productId, @Body ProductDto product, OnComplete<Response<ProductDto>> callback);

    /// Takes a product off sale; what players already own stays theirs.
    @POST("/api/products/{productId}/deactivate")
    void deactivateProduct(@Path("productId") String productId, OnComplete<Response<ProductDto>> callback);

    /// The caller's payout requests, newest first, whatever their status.
    @GET("/api/me/payouts")
    void myPayouts(OnComplete<Response<List<PayoutDto>>> callback);
}
