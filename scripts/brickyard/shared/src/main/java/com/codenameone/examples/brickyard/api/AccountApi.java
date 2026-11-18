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
package com.codenameone.examples.brickyard.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Accounts: registration, the signed-in account, becoming a creator, and friends. Signing in is OAuth.
@RestClient
public interface AccountApi {
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<AccountDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<AccountDto>> callback);

    @POST("/api/me/profile")
    void saveProfile(@Body ProfileDto profile, OnComplete<Response<AccountDto>> callback);

    @POST("/api/me/creator")
    void becomeCreator(OnComplete<Response<AccountDto>> callback);

    @POST("/api/me/friends")
    void addFriend(@Body FriendDto friend, OnComplete<Response<FriendDto>> callback);

    @GET("/api/me/friends")
    void friends(OnComplete<Response<List<FriendDto>>> callback);
    /// The caller's profile, avatar colours included.
    @GET("/api/me/profile")
    void profile(OnComplete<Response<ProfileDto>> callback);
    /// Registers this device for push notifications.
    @POST("/api/me/devices")
    void registerDevice(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);
    @GET("/api/me/limits")
    void limits(OnComplete<Response<LimitsDto>> callback);
    /// Sets the parental limits; a player under 13 cannot turn them off themselves.
    @POST("/api/me/limits")
    void saveLimits(@Body LimitsDto limits, OnComplete<Response<LimitsDto>> callback);
}
