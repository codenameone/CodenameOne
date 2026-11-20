/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Friends: the people whose scores the FRIENDS board shows.
@RestClient
public interface FriendsApi {
    @GET("/api/friends")
    void friends(OnComplete<Response<List<FriendDto>>> callback);

    @POST("/api/friends")
    void add(@Body FriendAddDto friend, OnComplete<Response<List<FriendDto>>> callback);

    @POST("/api/friends/remove")
    void remove(@Body FriendAddDto friend, OnComplete<Response<List<FriendDto>>> callback);
}
