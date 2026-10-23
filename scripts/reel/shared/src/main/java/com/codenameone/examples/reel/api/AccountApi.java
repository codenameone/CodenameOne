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
package com.codenameone.examples.reel.api;

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

/// The signed-in account, its devices and its household profiles.
@RestClient
public interface AccountApi {
    @POST("/api/signup")
    void signup(@Body SignupDto signup, OnComplete<Response<AccountDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<AccountDto>> callback);

    @PUT("/api/me")
    void updateProfile(@Body ProfileDto profile, OnComplete<Response<AccountDto>> callback);

    @POST("/api/me/password")
    void changePassword(@Body PasswordChangeDto change, OnComplete<Response<String>> callback);

    /// Closes the account; the password confirms it.
    @POST("/api/me/close")
    void close(@Body AccountClosureDto closure, OnComplete<Response<String>> callback);

    @GET("/api/me/devices")
    void devices(OnComplete<Response<List<DeviceDto>>> callback);

    /// Signs a device out; its tokens stop working.
    @DELETE("/api/me/devices/{id}")
    void signOutDevice(@Path("id") String id, OnComplete<Response<String>> callback);

    /// Streams playing on the account right now.
    @GET("/api/me/sessions")
    void sessions(OnComplete<Response<List<SessionDto>>> callback);

    @POST("/api/me/push")
    void registerPush(@Body PushDeviceDto device, OnComplete<Response<String>> callback);

    @GET("/api/profiles")
    void profiles(OnComplete<Response<List<HouseholdProfileDto>>> callback);

    @POST("/api/profiles")
    void createProfile(@Body ProfileEditDto profile, OnComplete<Response<HouseholdProfileDto>> callback);

    @PUT("/api/profiles/{id}")
    void updateHouseholdProfile(@Path("id") String id, @Body ProfileEditDto profile,
            OnComplete<Response<HouseholdProfileDto>> callback);

    @DELETE("/api/profiles/{id}")
    void deleteProfile(@Path("id") String id, OnComplete<Response<String>> callback);

    /// Checks a protected profile's PIN; a wrong one answers 403.
    @POST("/api/profiles/{id}/unlock")
    void unlockProfile(@Path("id") String id, @Body PinDto pin,
            OnComplete<Response<HouseholdProfileDto>> callback);
}
