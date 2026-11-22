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

/// Merchant mode: how the store's staff sign in -- a required authenticator
/// app, and single sign-on through the store's OpenID Connect providers.
/// Owners only.
@RestClient
public interface StaffSecurityApi {
    @GET("/api/merchant/stores/{storeId}/staff-security")
    void settings(@Path("storeId") String storeId, OnComplete<Response<StaffSecurityDto>> callback);

    /// Requiring an authenticator app is refused until the owner has one.
    @POST("/api/merchant/stores/{storeId}/staff-security")
    void save(@Path("storeId") String storeId, @Body StaffSecurityDto settings, OnComplete<Response<StaffSecurityDto>> callback);

    @GET("/api/merchant/stores/{storeId}/sso-providers")
    void providers(@Path("storeId") String storeId, OnComplete<Response<List<SsoProviderDto>>> callback);

    /// Adds a provider, or changes the one `provider.id` names. The issuer's
    /// discovery document is read to find its endpoints.
    @POST("/api/merchant/stores/{storeId}/sso-providers")
    void saveProvider(@Path("storeId") String storeId, @Body SsoProviderDto provider, OnComplete<Response<SsoProviderDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/sso-providers/{providerId}")
    void removeProvider(@Path("storeId") String storeId, @Path("providerId") String providerId, OnComplete<Response<List<SsoProviderDto>>> callback);
}
