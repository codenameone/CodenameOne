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
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Stores: the public face of each, and what its staff configure -- settings,
/// locations and who works there.
///
/// `/api/shop` needs no sign-in and shows only active stores. `/api/merchant`
/// needs the store's staff list to name the caller; settings and staff need an
/// OWNER or MANAGER.
@RestClient
public interface StoreApi {
    /// Every active store, by name.
    @GET("/api/shop/stores")
    void stores(OnComplete<Response<List<StoreDto>>> callback);

    @GET("/api/shop/stores/{handle}")
    void store(@Path("handle") String handle, OnComplete<Response<StoreDto>> callback);

    /// The active locations of a store that shoppers may pick up from.
    @GET("/api/shop/stores/{handle}/locations")
    void pickupLocations(@Path("handle") String handle,
            OnComplete<Response<List<LocationDto>>> callback);

    /// The stores the caller works for, with the role at each.
    @GET("/api/merchant/stores")
    void myStores(OnComplete<Response<List<StoreRoleDto>>> callback);

    @GET("/api/merchant/stores/{storeId}")
    void storeSettings(@Path("storeId") String storeId, OnComplete<Response<StoreDto>> callback);

    @PUT("/api/merchant/stores/{storeId}")
    void saveSettings(@Path("storeId") String storeId, @Body StoreDto settings,
            OnComplete<Response<StoreDto>> callback);

    @GET("/api/merchant/stores/{storeId}/locations")
    void locations(@Path("storeId") String storeId,
            OnComplete<Response<List<LocationDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/locations")
    void addLocation(@Path("storeId") String storeId, @Body LocationDto location,
            OnComplete<Response<LocationDto>> callback);

    @PUT("/api/merchant/stores/{storeId}/locations/{locationId}")
    void saveLocation(@Path("storeId") String storeId, @Path("locationId") String locationId,
            @Body LocationDto location, OnComplete<Response<LocationDto>> callback);

    /// Deletes a location that never held stock, and deactivates one that did.
    @DELETE("/api/merchant/stores/{storeId}/locations/{locationId}")
    void removeLocation(@Path("storeId") String storeId, @Path("locationId") String locationId,
            OnComplete<Response<LocationDto>> callback);

    @GET("/api/merchant/stores/{storeId}/staff")
    void staff(@Path("storeId") String storeId, OnComplete<Response<List<StaffDto>>> callback);

    /// Adds an existing account to the staff, or changes the role, location
    /// or PIN of one already on it. The account is given the MERCHANT role.
    @POST("/api/merchant/stores/{storeId}/staff")
    void saveStaff(@Path("storeId") String storeId, @Body StaffDto staff,
            OnComplete<Response<StaffDto>> callback);

    /// Takes someone off the staff. The store's owner cannot be removed.
    @DELETE("/api/merchant/stores/{storeId}/staff/{staffId}")
    void removeStaff(@Path("storeId") String storeId, @Path("staffId") String staffId,
            OnComplete<Response<StaffDto>> callback);
}
