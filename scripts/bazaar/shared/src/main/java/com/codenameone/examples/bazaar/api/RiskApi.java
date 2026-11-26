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

/// Fraud scores of orders, and the review of the ones held for it.
@RestClient
public interface RiskApi {
    @GET("/api/merchant/stores/{storeId}/order-risks")
    void orderRisks(@Path("storeId") String storeId,
            @Query("held") String held,
            OnComplete<Response<List<OrderRiskDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/order-risks/{orderId}")
    void orderRisk(@Path("storeId") String storeId,
            @Path("orderId") String orderId,
            OnComplete<Response<OrderRiskDto>> callback);

    @POST("/api/merchant/stores/{storeId}/order-risks/{orderId}/approve")
    void approveOrderRisk(@Path("storeId") String storeId,
            @Path("orderId") String orderId,
            OnComplete<Response<OrderRiskDto>> callback);

    @POST("/api/merchant/stores/{storeId}/order-risks/{orderId}/cancel")
    void cancelOrderRisk(@Path("storeId") String storeId,
            @Path("orderId") String orderId,
            OnComplete<Response<OrderRiskDto>> callback);

    /// The store's fraud filter rules, oldest first.
    @GET("/api/merchant/stores/{storeId}/fraud-rules")
    void fraudRules(@Path("storeId") String storeId,
            OnComplete<Response<List<FraudRuleDto>>> callback);

    /// Adds a rule; answers every rule of the store.
    @POST("/api/merchant/stores/{storeId}/fraud-rules")
    void addFraudRule(@Path("storeId") String storeId, @Body FraudRuleDto rule,
            OnComplete<Response<List<FraudRuleDto>>> callback);

    /// Removes a rule; answers the rules left.
    @DELETE("/api/merchant/stores/{storeId}/fraud-rules/{ruleId}")
    void removeFraudRule(@Path("storeId") String storeId, @Path("ruleId") String ruleId,
            OnComplete<Response<List<FraudRuleDto>>> callback);
}
