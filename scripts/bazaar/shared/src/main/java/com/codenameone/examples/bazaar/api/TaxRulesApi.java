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

/// Merchant mode: the tax engine -- EU VAT by destination and product
/// category, overrides per product or collection, the US sales-tax provider,
/// and the One-Stop Shop report (`oss-report.csv` for the file).
@RestClient
public interface TaxRulesApi {
    @GET("/api/merchant/stores/{storeId}/tax-settings")
    void settings(@Path("storeId") String storeId, OnComplete<Response<TaxSettingsDto>> callback);

    @POST("/api/merchant/stores/{storeId}/tax-settings")
    void saveSettings(@Path("storeId") String storeId, @Body TaxSettingsDto settings, OnComplete<Response<TaxSettingsDto>> callback);

    @GET("/api/merchant/stores/{storeId}/tax-overrides")
    void overrides(@Path("storeId") String storeId, OnComplete<Response<List<TaxOverrideDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/tax-overrides")
    void saveOverride(@Path("storeId") String storeId, @Body TaxOverrideDto override, OnComplete<Response<TaxOverrideDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/tax-overrides/{overrideId}")
    void removeOverride(@Path("storeId") String storeId, @Path("overrideId") String overrideId, OnComplete<Response<List<TaxOverrideDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/products/{productId}/tax-category")
    void setCategory(@Path("storeId") String storeId, @Path("productId") String productId, @Body TaxCategoryDto category, OnComplete<Response<TaxCategoryDto>> callback);

    /// The EU VAT table the engine uses.
    @GET("/api/merchant/stores/{storeId}/vat-rates")
    void vatRates(@Path("storeId") String storeId, OnComplete<Response<List<VatRateDto>>> callback);

    /// Sales to EU countries other than the home country, per country and
    /// rate, for the days `from` to `to` (ISO dates, inclusive).
    @GET("/api/merchant/stores/{storeId}/oss-report")
    void ossReport(@Path("storeId") String storeId, @Query("from") String from, @Query("to") String to, OnComplete<Response<List<OssRowDto>>> callback);

    /// Administrators: changes one rate of the EU VAT table.
    @POST("/api/admin/vat-rates")
    void saveVatRate(@Body VatRateDto rate, OnComplete<Response<VatRateDto>> callback);
}
