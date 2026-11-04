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
import java.util.List;

/// Inviting friends and the home banner. The invite link itself is minted and
/// tracked by `com.codename1.analytics.invite.Invites`; the server only knows
/// whose code a link carried, so it can pay both players once the friend has
/// played.
@RestClient
public interface SocialApi {
    @GET("/api/social/invite")
    void invite(OnComplete<Response<InviteDto>> callback);

    /// Credits an invite to the player who just joined. Accounts older than a
    /// week, a player's own code and a second claim are refused.
    @POST("/api/social/claim")
    void claim(@Body InviteClaimDto claim, OnComplete<Response<RewardDto>> callback);

    @GET("/api/social/friends")
    void friends(OnComplete<Response<List<FriendDto>>> callback);

    @GET("/api/social/spotlight")
    void spotlight(OnComplete<Response<SpotlightDto>> callback);
}
