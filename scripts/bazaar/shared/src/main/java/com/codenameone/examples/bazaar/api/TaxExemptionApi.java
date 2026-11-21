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

/// Customer tax exemptions and the tax report by region and period.
@RestClient
public interface TaxExemptionApi {
    @GET("/api/merchant/stores/{storeId}/tax-exemptions")
    void taxExemptions(@Path("storeId") String storeId,
            OnComplete<Response<List<TaxExemptionDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/tax-exemptions")
    void addTaxExemption(@Path("storeId") String storeId,
            @Body TaxExemptionDto body,
            OnComplete<Response<TaxExemptionDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/tax-exemptions/{exemptionId}")
    void removeTaxExemption(@Path("storeId") String storeId,
            @Path("exemptionId") String exemptionId,
            OnComplete<Response<List<TaxExemptionDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/tax-report")
    void taxReport(@Path("storeId") String storeId,
            @Query("from") String from,
            @Query("to") String to,
            OnComplete<Response<List<TaxReportRowDto>>> callback);
}
