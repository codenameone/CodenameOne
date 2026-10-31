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
package com.codenameone.examples.dash.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Accounts: registration, the signed-in player, push devices and the game socket's ticket. Signing in is OAuth (see the app's Session).
@RestClient
public interface AccountApi {
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<PlayerDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<PlayerDto>> callback);

    @POST("/api/me/profile")
    void saveProfile(@Body ProfileDto profile, OnComplete<Response<PlayerDto>> callback);

    @POST("/api/game/ticket")
    void gameTicket(OnComplete<Response<TicketDto>> callback);

    @POST("/api/account/push")
    void registerPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);

    @POST("/api/account/push/remove")
    void unregisterPush(@Body PushDeviceDto device, OnComplete<Response<PushDeviceDto>> callback);
}
