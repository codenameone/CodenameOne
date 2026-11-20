/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Accounts: registration, the signed-in player, the profile and push devices. Signing in is
/// OAuth (see the app's Session).
@RestClient
public interface AccountApi {
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<PlayerDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<PlayerDto>> callback);

    @POST("/api/me/profile")
    void saveProfile(@Body ProfileDto profile, OnComplete<Response<PlayerDto>> callback);

    @POST("/api/account/push")
    void registerPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);

    @POST("/api/account/push/remove")
    void unregisterPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);
}
