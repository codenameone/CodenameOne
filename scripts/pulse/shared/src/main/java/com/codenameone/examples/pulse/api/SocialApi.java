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
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Training with friends. Nothing is shown to anyone who is not a friend, and a
/// workout reaches the feed only when its owner shared it.
@RestClient
public interface SocialApi {
    @GET("/api/social/friends")
    void friends(OnComplete<Response<List<FriendDto>>> callback);

    /// Asks a member, by e-mail, to be friends. Answers the same whether or not
    /// the address has an account, so the call cannot be used to find out.
    @POST("/api/social/friends")
    void request(@Body FriendRequestDto request, OnComplete<Response<FriendDto>> callback);

    @POST("/api/social/friends/{account}/accept")
    void accept(@Path("account") String account, OnComplete<Response<FriendDto>> callback);

    @POST("/api/social/friends/{account}/remove")
    void remove(@Path("account") String account, OnComplete<Response<FriendDto>> callback);

    @GET("/api/social/feed")
    void feed(OnComplete<Response<List<FeedItemDto>>> callback);

    @POST("/api/social/workouts/{id}/kudos")
    void kudos(@Path("id") String id, OnComplete<Response<FeedItemDto>> callback);

    /// Invites a friend to a class; they are sent a push they can book from.
    @POST("/api/social/invite")
    void invite(@Body InviteDto invite, OnComplete<Response<NudgeDto>> callback);
}
