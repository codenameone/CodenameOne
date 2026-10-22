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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Opening an account, verifying its phone, and what its owner edits.
@RestClient
public interface AccountApi {
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<UserDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<UserDto>> callback);

    @POST("/api/account/phone/start")
    void startPhoneVerification(OnComplete<Response<PhoneChallengeDto>> callback);

    @POST("/api/account/phone/verify")
    void verifyPhone(@Body PhoneCodeDto code, OnComplete<Response<UserDto>> callback);

    @POST("/api/live/ticket")
    void liveTicket(OnComplete<Response<TicketDto>> callback);

    @GET("/api/account/preferences")
    void preferences(OnComplete<Response<PreferencesDto>> callback);

    @PUT("/api/account/preferences")
    void savePreferences(@Body PreferencesDto preferences,
            OnComplete<Response<PreferencesDto>> callback);

    @GET("/api/account/profile")
    void profile(OnComplete<Response<ProfileDto>> callback);

    @PUT("/api/account/profile")
    void saveProfile(@Body ProfileDto profile, OnComplete<Response<UserDto>> callback);

    @PUT("/api/account/photo")
    void savePhoto(@Body PhotoDto photo, OnComplete<Response<UserDto>> callback);

    @GET("/api/users/{username}/photo")
    void photo(@Path("username") String username, OnComplete<Response<PhotoDto>> callback);

    @POST("/api/account/password")
    void changePassword(@Body PasswordChangeDto change, OnComplete<Response<UserDto>> callback);

    @POST("/api/account/delete")
    void deleteAccount(@Body AccountClosureDto closure, OnComplete<Response<UserDto>> callback);

    // tag::parley-push-contract[]
    @POST("/api/account/push")
    void registerPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);

    @POST("/api/account/push/remove")
    void unregisterPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);
    // end::parley-push-contract[]
}
