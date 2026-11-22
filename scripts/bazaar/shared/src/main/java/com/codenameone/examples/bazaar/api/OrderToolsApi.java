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

/// Order tools for the store's owner and managers: archiving, bulk actions,
/// the order and return settings, checkout links for drafts, and the
/// wording of the notifications shoppers receive.
@RestClient
public interface OrderToolsApi {
    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/archive")
    void archive(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    @POST("/api/merchant/stores/{storeId}/orders/{orderId}/unarchive")
    void unarchive(@Path("storeId") String storeId, @Path("orderId") String orderId,
            OnComplete<Response<OrderDto>> callback);

    /// The store's archived orders, newest first.
    @GET("/api/merchant/stores/{storeId}/archived-orders")
    void archived(@Path("storeId") String storeId,
            OnComplete<Response<List<OrderDto>>> callback);

    /// One action on many orders, answered order by order.
    @POST("/api/merchant/stores/{storeId}/bulk-orders")
    void bulk(@Path("storeId") String storeId, @Body BulkActionDto action,
            OnComplete<Response<List<BulkActionResultDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/order-settings")
    void settings(@Path("storeId") String storeId,
            OnComplete<Response<OrderSettingsDto>> callback);

    @POST("/api/merchant/stores/{storeId}/order-settings")
    void saveSettings(@Path("storeId") String storeId, @Body OrderSettingsDto settings,
            OnComplete<Response<OrderSettingsDto>> callback);

    /// Sends the draft to its customer as a signed checkout link.
    @POST("/api/merchant/stores/{storeId}/drafts/{orderId}/pay-link")
    void payLink(@Path("storeId") String storeId, @Path("orderId") String orderId,
            @Body PayLinkRequestDto request, OnComplete<Response<PayLinkDto>> callback);

    /// Every shopper notification text in `language` (empty for English).
    @GET("/api/merchant/stores/{storeId}/templates")
    void templates(@Path("storeId") String storeId, @Query("language") String language,
            OnComplete<Response<List<TemplateDto>>> callback);

    /// Saves the store's wording of one text.
    @POST("/api/merchant/stores/{storeId}/templates")
    void saveTemplate(@Path("storeId") String storeId, @Body TemplateDto template,
            OnComplete<Response<TemplateDto>> callback);

    /// Goes back to the built-in wording of one text.
    @POST("/api/merchant/stores/{storeId}/templates/reset")
    void resetTemplate(@Path("storeId") String storeId, @Body TemplateDto template,
            OnComplete<Response<TemplateDto>> callback);

    /// A text with sample values in its placeholders, saved or not.
    @POST("/api/merchant/stores/{storeId}/templates/preview")
    void previewTemplate(@Path("storeId") String storeId, @Body TemplateDto template,
            OnComplete<Response<TemplateDto>> callback);
}
