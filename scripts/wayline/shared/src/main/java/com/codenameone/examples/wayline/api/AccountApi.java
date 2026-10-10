/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.api;

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

    /// Closes the account for good. Refused while a ride is under way.
    @POST("/api/account/delete")
    void deleteAccount(OnComplete<Response<UserDto>> callback);
}
