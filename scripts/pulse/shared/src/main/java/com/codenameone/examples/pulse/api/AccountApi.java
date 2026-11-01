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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Opening an account, proving a phone number, asking who is signed in, and the
/// settings an account keeps.
///
/// The app calls these through the generated client. The server implements the
/// generated `AccountApiServer`, and its build routes each path to that
/// implementation; neither side writes the other's half.
@RestClient
public interface AccountApi {
    /// Opens an account. The one call here that needs no sign-in.
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<UserDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<UserDto>> callback);

    /// Sends a verification code to the phone number on the account.
    @POST("/api/account/phone/start")
    void startPhoneVerification(OnComplete<Response<PhoneChallengeDto>> callback);

    @POST("/api/account/phone/verify")
    void verifyPhone(@Body PhoneCodeDto code, OnComplete<Response<UserDto>> callback);

    /// A ticket for the live channel; see [TicketDto].
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

    @POST("/api/account/password")
    void changePassword(@Body PasswordChangeDto change, OnComplete<Response<UserDto>> callback);

    /// Closes the account for good. Refused while an invoice is being charged, and
    /// without the account's password: a token is what a borrowed phone
    /// holds too, and this cannot be taken back.
    @POST("/api/account/delete")
    void deleteAccount(@Body AccountClosureDto closure, OnComplete<Response<UserDto>> callback);

    // tag::pulse-push-contract[]
    /// Records a device the signed-in account is to be sent notifications on.
    /// Registering the same token again moves it to this account: a phone
    /// belongs to whoever signed in on it last.
    @POST("/api/account/push")
    void registerPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);

    /// Forgets a device, on signing out: the next person to use the phone is
    /// not to be told about this account's classes.
    @POST("/api/account/push/remove")
    void unregisterPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);
    // end::pulse-push-contract[]
}
