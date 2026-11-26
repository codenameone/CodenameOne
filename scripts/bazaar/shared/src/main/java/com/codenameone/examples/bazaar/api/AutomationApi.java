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

/// Merchant mode: automations, graphs of trigger, condition and action nodes
/// drawn in the node editor and run by the server on store events.
@RestClient
public interface AutomationApi {
    /// Every node type with its ports and parameters, for the palette.
    @GET("/api/merchant/stores/{storeId}/automation-catalog")
    void catalog(@Path("storeId") String storeId, OnComplete<Response<List<NodeTypeDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/automations")
    void list(@Path("storeId") String storeId, OnComplete<Response<List<AutomationDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/automations/{automationId}")
    void get(@Path("storeId") String storeId, @Path("automationId") String automationId,
            OnComplete<Response<AutomationDto>> callback);

    /// Saves a new automation. An active one must validate.
    @POST("/api/merchant/stores/{storeId}/automations")
    void create(@Path("storeId") String storeId, @Body AutomationDto automation,
            OnComplete<Response<AutomationDto>> callback);

    @POST("/api/merchant/stores/{storeId}/automations/{automationId}")
    void save(@Path("storeId") String storeId, @Path("automationId") String automationId,
            @Body AutomationDto automation, OnComplete<Response<AutomationDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/automations/{automationId}")
    void remove(@Path("storeId") String storeId, @Path("automationId") String automationId,
            OnComplete<Response<List<AutomationDto>>> callback);

    /// Checks a graph without saving it: a trigger, known node types, no
    /// cycles, no edge to a missing node or port.
    @POST("/api/merchant/stores/{storeId}/automations-validate")
    void validate(@Path("storeId") String storeId, @Body AutomationDto automation,
            OnComplete<Response<ValidationDto>> callback);

    /// What an automation's actions did, newest first.
    @GET("/api/merchant/stores/{storeId}/automations/{automationId}/activity")
    void activity(@Path("storeId") String storeId, @Path("automationId") String automationId,
            OnComplete<Response<List<AuditDto>>> callback);
}
