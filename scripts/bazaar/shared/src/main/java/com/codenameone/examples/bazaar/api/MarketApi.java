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

/// Markets (countries, currency, price adjustment and rounding, fixed prices), the merchant's exchange rates, duties by HS code, and translations of the catalog.
@RestClient
public interface MarketApi {
    @GET("/api/merchant/stores/{storeId}/markets")
    void markets(@Path("storeId") String storeId,
            OnComplete<Response<List<MarketDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/markets")
    void createMarket(@Path("storeId") String storeId,
            @Body MarketDto body,
            OnComplete<Response<MarketDto>> callback);

    @POST("/api/merchant/stores/{storeId}/markets/{marketId}")
    void saveMarket(@Path("storeId") String storeId,
            @Path("marketId") String marketId,
            @Body MarketDto body,
            OnComplete<Response<MarketDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/markets/{marketId}")
    void removeMarket(@Path("storeId") String storeId,
            @Path("marketId") String marketId,
            OnComplete<Response<List<MarketDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/markets/{marketId}/prices")
    void marketPrices(@Path("storeId") String storeId,
            @Path("marketId") String marketId,
            OnComplete<Response<List<MarketPriceDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/markets/{marketId}/prices")
    void putMarketPrice(@Path("storeId") String storeId,
            @Path("marketId") String marketId,
            @Body MarketPriceDto body,
            OnComplete<Response<List<MarketPriceDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/fx-rates")
    void fxRates(@Path("storeId") String storeId,
            OnComplete<Response<List<FxRateDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/fx-rates")
    void putFxRate(@Path("storeId") String storeId,
            @Body FxRateDto body,
            OnComplete<Response<List<FxRateDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/duty-rates")
    void dutyRates(@Path("storeId") String storeId,
            OnComplete<Response<List<DutyRateDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/duty-rates")
    void addDutyRate(@Path("storeId") String storeId,
            @Body DutyRateDto body,
            OnComplete<Response<List<DutyRateDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/duty-rates/{dutyId}")
    void removeDutyRate(@Path("storeId") String storeId,
            @Path("dutyId") String dutyId,
            OnComplete<Response<List<DutyRateDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/customs-codes")
    void customsCodes(@Path("storeId") String storeId,
            OnComplete<Response<List<CustomsCodeDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/customs-codes")
    void putCustomsCode(@Path("storeId") String storeId,
            @Body CustomsCodeDto body,
            OnComplete<Response<List<CustomsCodeDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/translations")
    void translations(@Path("storeId") String storeId,
            @Query("kind") String kind,
            @Query("refId") String refId,
            OnComplete<Response<List<TranslationDto>>> callback);

    /// Every translatable field of one thing beside its translation into
    /// `locale` and its status; for a product, its options, pictures and text
    /// metafields too.
    @GET("/api/merchant/stores/{storeId}/translations/sheet")
    void translationSheet(@Path("storeId") String storeId,
            @Query("kind") String kind,
            @Query("refId") String refId,
            @Query("locale") String locale,
            OnComplete<Response<List<TranslationFieldDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/translations")
    void putTranslation(@Path("storeId") String storeId,
            @Body TranslationDto body,
            OnComplete<Response<List<TranslationDto>>> callback);
}
