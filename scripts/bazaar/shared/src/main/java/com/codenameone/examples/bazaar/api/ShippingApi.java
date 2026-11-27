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

/// A store's shipping zones and rates, for OWNER and MANAGER.
@RestClient
public interface ShippingApi {
    /// The zones by position, each with its rates.
    @GET("/api/merchant/stores/{storeId}/shipping")
    void zones(@Path("storeId") String storeId,
            OnComplete<Response<List<ShippingZoneDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/shipping/zones")
    void saveZone(@Path("storeId") String storeId, @Body ShippingZoneDto zone,
            OnComplete<Response<ShippingZoneDto>> callback);

    /// Deletes a zone and its rates.
    @DELETE("/api/merchant/stores/{storeId}/shipping/zones/{zoneId}")
    void deleteZone(@Path("storeId") String storeId, @Path("zoneId") String zoneId,
            OnComplete<Response<ShippingZoneDto>> callback);

    @POST("/api/merchant/stores/{storeId}/shipping/rates")
    void saveRate(@Path("storeId") String storeId, @Body ShippingRateDto rate,
            OnComplete<Response<ShippingRateDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/shipping/rates/{rateId}")
    void deleteRate(@Path("storeId") String storeId, @Path("rateId") String rateId,
            OnComplete<Response<ShippingRateDto>> callback);

    /// The shipping profiles by position, each with its products.
    @GET("/api/merchant/stores/{storeId}/shipping/profiles")
    void profiles(@Path("storeId") String storeId,
            OnComplete<Response<List<ShippingProfileDto>>> callback);

    /// Saves a profile; its `productIds` become exactly the products in it,
    /// taken out of any other profile.
    @POST("/api/merchant/stores/{storeId}/shipping/profiles")
    void saveProfile(@Path("storeId") String storeId, @Body ShippingProfileDto profile,
            OnComplete<Response<ShippingProfileDto>> callback);

    /// Deletes a profile with its zones and rates; its products go back to the
    /// general profile.
    @DELETE("/api/merchant/stores/{storeId}/shipping/profiles/{profileId}")
    void deleteProfile(@Path("storeId") String storeId, @Path("profileId") String profileId,
            OnComplete<Response<ShippingProfileDto>> callback);
}
