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

/// Digital goods and rich product media.
///
/// A variant marked digital needs no shipping; its download (and, when it
/// has one, a pool of licence keys) is set here by an OWNER or MANAGER. A
/// paid order line of a digital variant gets a download grant: a signed,
/// expiring link with a download count, which the merchant may revoke and a
/// refund revokes by itself. A product's gallery takes videos and 3D models
/// beside its pictures.
@RestClient
public interface DigitalApi {
    @GET("/api/merchant/stores/{storeId}/variants/{variantId}/download")
    void download(@Path("storeId") String storeId, @Path("variantId") String variantId,
            OnComplete<Response<DigitalAssetDto>> callback);

    /// Uploads (or replaces) the file and sets the limits.
    @POST("/api/merchant/stores/{storeId}/variants/{variantId}/download")
    void setDownload(@Path("storeId") String storeId, @Path("variantId") String variantId,
            @Body DigitalUploadDto upload, OnComplete<Response<DigitalAssetDto>> callback);

    @POST("/api/merchant/stores/{storeId}/variants/{variantId}/license-keys")
    void addLicenseKeys(@Path("storeId") String storeId, @Path("variantId") String variantId,
            @Body LicenseKeysDto keys, OnComplete<Response<DigitalAssetDto>> callback);

    @GET("/api/merchant/stores/{storeId}/orders/{orderId}/downloads")
    void orderDownloads(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<List<DownloadGrantDto>>> callback);

    /// Stops a grant's link from working; the buyer's keys stay sold.
    @POST("/api/merchant/stores/{storeId}/download-grants/{grantId}/revoke")
    void revokeGrant(@Path("storeId") String storeId, @Path("grantId") String grantId,
            OnComplete<Response<DownloadGrantDto>> callback);

    /// Adds a video or 3D model to the end of the product's gallery.
    @POST("/api/merchant/stores/{storeId}/products/{productId}/media")
    void addMedia(@Path("storeId") String storeId, @Path("productId") String productId,
            @Body MediaUploadDto media, OnComplete<Response<ProductDto>> callback);

    /// Moves a gallery entry one place `up` or `down`.
    @POST("/api/merchant/stores/{storeId}/products/{productId}/photos/{photoId}/move")
    void moveMedia(@Path("storeId") String storeId, @Path("productId") String productId,
            @Path("photoId") String photoId, @Query("direction") String direction,
            OnComplete<Response<ProductDto>> callback);

    /// The signed-in buyer's downloads for one of their orders.
    @GET("/api/orders/{orderId}/downloads")
    void myDownloads(@Path("orderId") String orderId,
            OnComplete<Response<List<DownloadGrantDto>>> callback);
}
