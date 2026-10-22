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
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Chats and groups: creating them, their members, roles and invite links.
@RestClient
public interface ConversationApi {
    @GET("/api/conversations")
    void list(OnComplete<Response<List<ConversationDto>>> callback);

    @POST("/api/conversations")
    void create(@Body NewConversationDto conversation,
            OnComplete<Response<ConversationDto>> callback);

    @GET("/api/conversations/{id}")
    void get(@Path("id") String id, OnComplete<Response<ConversationDto>> callback);

    @PUT("/api/conversations/{id}")
    void edit(@Path("id") String id, @Body ConversationEditDto edit,
            OnComplete<Response<ConversationDto>> callback);

    @PUT("/api/conversations/{id}/flags")
    void flags(@Path("id") String id, @Body ConversationFlagsDto flags,
            OnComplete<Response<ConversationDto>> callback);

    @PUT("/api/conversations/{id}/photo")
    void savePhoto(@Path("id") String id, @Body PhotoDto photo,
            OnComplete<Response<ConversationDto>> callback);

    @GET("/api/conversations/{id}/photo")
    void photo(@Path("id") String id, OnComplete<Response<PhotoDto>> callback);

    @POST("/api/conversations/{id}/members")
    void addMember(@Path("id") String id, @Body MemberChangeDto member,
            OnComplete<Response<ConversationDto>> callback);

    @PUT("/api/conversations/{id}/members/{username}")
    void setRole(@Path("id") String id, @Path("username") String username,
            @Body MemberChangeDto member, OnComplete<Response<ConversationDto>> callback);

    @DELETE("/api/conversations/{id}/members/{username}")
    void removeMember(@Path("id") String id, @Path("username") String username,
            OnComplete<Response<ConversationDto>> callback);

    @POST("/api/conversations/{id}/leave")
    void leave(@Path("id") String id, OnComplete<Response<ConversationDto>> callback);

    @POST("/api/conversations/{id}/invite")
    void invite(@Path("id") String id, OnComplete<Response<InviteDto>> callback);

    @GET("/api/invites/{code}")
    void preview(@Path("code") String code, OnComplete<Response<InviteDto>> callback);

    @POST("/api/invites/{code}/join")
    void join(@Path("code") String code, OnComplete<Response<ConversationDto>> callback);

    @GET("/api/conversations/{id}/devices")
    void devices(@Path("id") String id, OnComplete<Response<List<DeviceAddressDto>>> callback);
}
