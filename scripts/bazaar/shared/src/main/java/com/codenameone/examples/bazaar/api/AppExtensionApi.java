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

/// App UI extensions: the declarative blocks installed apps add to the
/// merchant's product, order and customer pages, to the storefront and to
/// the checkout. No app code runs in Bazaar; a block is a tree of a fixed
/// vocabulary (`ops.Blocks`) the app renders natively.
@RestClient
public interface AppExtensionApi {
    /// The blocks and actions of installed apps for one admin page: `area`
    /// is `product`, `order` or `customer`, `resourceId` the one shown. Each
    /// block's data is fetched by the server from its app, signed.
    @GET("/api/merchant/stores/{storeId}/extensions")
    void blocks(@Path("storeId") String storeId, @Query("area") String area,
            @Query("resourceId") String resourceId,
            OnComplete<Response<List<ExtensionBlockDto>>> callback);

    /// Presses an action button: the server posts it to the app, signed.
    @POST("/api/merchant/stores/{storeId}/extensions/{extensionId}/action")
    void action(@Path("storeId") String storeId, @Path("extensionId") String extensionId,
            @Body ExtensionActionDto action,
            OnComplete<Response<ExtensionActionResultDto>> callback);

    /// An installed app's own page with a five-minute session token.
    @POST("/api/merchant/stores/{storeId}/apps/{installationId}/session")
    void session(@Path("storeId") String storeId, @Path("installationId") String installationId,
            OnComplete<Response<AppSessionDto>> callback);

    /// The storefront or checkout blocks of a store, for shoppers: `target`
    /// is `storefront.block` or `checkout.block`. These carry no app data.
    @GET("/api/shop/stores/{handle}/extensions")
    void shopBlocks(@Path("handle") String handle, @Query("target") String target,
            OnComplete<Response<List<ExtensionBlockDto>>> callback);
}
