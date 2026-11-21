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

/// Sale events and flash sales (OWNER, MANAGER), the promotions calendar, and the storefront's countdown.
@RestClient
public interface SaleApi {
    @GET("/api/merchant/stores/{storeId}/sales")
    void list(@Path("storeId") String storeId,
            OnComplete<Response<List<SaleEventDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/sales/{saleId}")
    void get(@Path("storeId") String storeId,
            @Path("saleId") String saleId,
            OnComplete<Response<SaleEventDto>> callback);

    /// Schedules a sale; a variant may be in one sale at a time.
    @POST("/api/merchant/stores/{storeId}/sales")
    void create(@Path("storeId") String storeId,
            @Body SaleEventDto sale,
            OnComplete<Response<SaleEventDto>> callback);

    /// Cancels a sale; a running one puts its prices back first.
    @POST("/api/merchant/stores/{storeId}/sales/{saleId}/cancel")
    void cancel(@Path("storeId") String storeId,
            @Path("saleId") String saleId,
            OnComplete<Response<SaleEventDto>> callback);

    /// Sales, discounts, campaigns and club drops between `from` and `to` (epoch milliseconds).
    @GET("/api/merchant/stores/{storeId}/promotions/calendar")
    void calendar(@Path("storeId") String storeId,
            @Query("from") String from,
            @Query("to") String to,
            OnComplete<Response<List<PromoEventDto>>> callback);

    /// Moves a calendar entry to new dates.
    @POST("/api/merchant/stores/{storeId}/promotions/calendar/reschedule")
    void reschedule(@Path("storeId") String storeId,
            @Body RescheduleDto move,
            OnComplete<Response<PromoEventDto>> callback);

    /// The extra rules of a discount: class, combinations, eligibility, tiers.
    @GET("/api/merchant/stores/{storeId}/discounts/{discountId}/rules")
    void rules(@Path("storeId") String storeId,
            @Path("discountId") String discountId,
            OnComplete<Response<DiscountRulesDto>> callback);

    @POST("/api/merchant/stores/{storeId}/discounts/{discountId}/rules")
    void saveRules(@Path("storeId") String storeId,
            @Path("discountId") String discountId,
            @Body DiscountRulesDto rules,
            OnComplete<Response<DiscountRulesDto>> callback);

    /// The sales running now, for the storefront's countdown.
    @GET("/api/shop/stores/{storeId}/sales/live")
    void live(@Path("storeId") String storeId,
            OnComplete<Response<List<LiveSaleDto>>> callback);
}
