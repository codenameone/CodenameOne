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
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Order routing rules and the fulfilment service (3PL), for OWNER and
/// MANAGER.
@RestClient
public interface RoutingApi {
    /// The settings and the rules by position.
    @GET("/api/merchant/stores/{storeId}/routing")
    void routing(@Path("storeId") String storeId, OnComplete<Response<RoutingDto>> callback);

    /// Saves the split switch and the fulfilment service; `rules` is ignored.
    @POST("/api/merchant/stores/{storeId}/routing")
    void saveRouting(@Path("storeId") String storeId, @Body RoutingDto routing,
            OnComplete<Response<RoutingDto>> callback);

    @POST("/api/merchant/stores/{storeId}/routing/rules")
    void saveRule(@Path("storeId") String storeId, @Body RoutingRuleDto rule,
            OnComplete<Response<RoutingRuleDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/routing/rules/{ruleId}")
    void deleteRule(@Path("storeId") String storeId, @Path("ruleId") String ruleId,
            OnComplete<Response<RoutingRuleDto>> callback);

    /// What was handed to the fulfilment service, newest first.
    @GET("/api/merchant/stores/{storeId}/fulfilment-services/orders")
    void serviceOrders(@Path("storeId") String storeId,
            OnComplete<Response<List<FulfilmentServiceOrderDto>>> callback);
}
