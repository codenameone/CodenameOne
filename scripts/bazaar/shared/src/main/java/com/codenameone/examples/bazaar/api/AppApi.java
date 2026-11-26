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

/// Apps: what a developer registers, and what a store's owner installs.
///
/// The install is OAuth's authorization-code flow with PKCE. The app sends
/// the owner to `/account/apps/authorize?store=...&client_id=...` (the web
/// consent page) or the client shows [#consent] and posts [#authorize]; the
/// owner's decision redirects to the app with a code, which the app trades
/// at `POST /api/apps/oauth/token` for an access token (`bzk_app_...`) used
/// as a bearer token on the key API under `/api/v1/stores/{storeId}`.
@RestClient
public interface AppApi {
    /// The apps the caller registered.
    @GET("/api/developer/apps")
    void mine(OnComplete<Response<List<AppDto>>> callback);

    /// Registers an app; the answer is the only one carrying `clientSecret`.
    @POST("/api/developer/apps")
    void register(@Body AppDto app, OnComplete<Response<AppDto>> callback);

    /// Changes one of the caller's apps: its listing, addresses, scopes and status.
    @PUT("/api/developer/apps/{appId}")
    void update(@Path("appId") String appId, @Body AppDto app, OnComplete<Response<AppDto>> callback);

    /// The apps installed in a store, removed ones included. Owners only.
    @GET("/api/merchant/stores/{storeId}/apps")
    void installed(@Path("storeId") String storeId, OnComplete<Response<List<AppInstallationDto>>> callback);

    /// What the consent screen shows for an authorization request.
    @POST("/api/merchant/stores/{storeId}/apps/consent")
    void consent(@Path("storeId") String storeId, @Body AppAuthorizeDto request, OnComplete<Response<AppConsentDto>> callback);

    /// The owner's decision; answers where to send the browser.
    @POST("/api/merchant/stores/{storeId}/apps/authorize")
    void authorize(@Path("storeId") String storeId, @Body AppAuthorizeDto request, OnComplete<Response<AppRedirectDto>> callback);

    /// Removes an app: its token stops working at once and it is sent `app/uninstalled`.
    @DELETE("/api/merchant/stores/{storeId}/apps/{installationId}")
    void uninstall(@Path("storeId") String storeId, @Path("installationId") String installationId, OnComplete<Response<List<AppInstallationDto>>> callback);
}
