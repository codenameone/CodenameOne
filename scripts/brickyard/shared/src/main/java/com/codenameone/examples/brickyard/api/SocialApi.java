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
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Friend requests, follows, presence, blocks, parties, chat, direct messages and invites.
@RestClient
public interface SocialApi {
    @GET("/api/me/friends/requests")
    void friendRequests(OnComplete<Response<List<FriendRequestDto>>> callback);

    @POST("/api/me/friends/requests/{from}/accept")
    void acceptFriend(@Path("from") String from, OnComplete<Response<FriendDto>> callback);

    @POST("/api/me/friends/requests/{from}/decline")
    void declineFriend(@Path("from") String from, OnComplete<Response<FriendRequestDto>> callback);

    @POST("/api/creators/{creator}/follow")
    void follow(@Path("creator") String creator, OnComplete<Response<FollowDto>> callback);

    @POST("/api/creators/{creator}/unfollow")
    void unfollow(@Path("creator") String creator, OnComplete<Response<FollowDto>> callback);

    @GET("/api/me/following")
    void following(OnComplete<Response<List<FollowDto>>> callback);

    @POST("/api/me/presence")
    void setPresence(@Body PresenceDto presence, OnComplete<Response<PresenceDto>> callback);

    @GET("/api/me/friends/presence")
    void friendPresence(OnComplete<Response<List<PresenceDto>>> callback);

    @GET("/api/me/friends/{friend}/join")
    void joinFriend(@Path("friend") String friend, OnComplete<Response<PresenceDto>> callback);

    @POST("/api/me/blocks/{username}")
    void block(@Path("username") String username, OnComplete<Response<FriendDto>> callback);

    @GET("/api/me/blocks")
    void blocks(OnComplete<Response<List<FriendDto>>> callback);

    @POST("/api/parties")
    void createParty(OnComplete<Response<PartyDto>> callback);

    @POST("/api/parties/{partyId}/join")
    void joinParty(@Path("partyId") String partyId, OnComplete<Response<PartyDto>> callback);

    @POST("/api/parties/{partyId}/leave")
    void leaveParty(@Path("partyId") String partyId, OnComplete<Response<PartyDto>> callback);

    @GET("/api/me/party")
    void party(OnComplete<Response<PartyDto>> callback);

    @POST("/api/chat/{channel}")
    void send(@Path("channel") String channel, @Body ChatDto message, OnComplete<Response<ChatDto>> callback);

    @GET("/api/chat/{channel}")
    void chat(@Path("channel") String channel, OnComplete<Response<List<ChatDto>>> callback);

    @POST("/api/me/dm/{username}")
    void sendDm(@Path("username") String username, @Body ChatDto message, OnComplete<Response<ChatDto>> callback);

    @GET("/api/me/dm/{username}")
    void dms(@Path("username") String username, OnComplete<Response<List<ChatDto>>> callback);

    @POST("/api/me/invites")
    void createInvite(@Body InviteDto invite, OnComplete<Response<InviteDto>> callback);

    @POST("/api/me/referral")
    void redeemInvite(@Body InviteDto invite, OnComplete<Response<WalletDto>> callback);
}
