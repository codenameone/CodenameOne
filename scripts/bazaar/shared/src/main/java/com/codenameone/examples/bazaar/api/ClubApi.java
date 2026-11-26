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

package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Consumer clubs: run by OWNER and MANAGER; shoppers see, join and leave them.
@RestClient
public interface ClubApi {
    @GET("/api/merchant/stores/{storeId}/clubs")
    void list(@Path("storeId") String storeId,
            OnComplete<Response<List<ClubDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/clubs")
    void create(@Path("storeId") String storeId,
            @Body ClubDto club,
            OnComplete<Response<ClubDto>> callback);

    @POST("/api/merchant/stores/{storeId}/clubs/{clubId}")
    void save(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            @Body ClubDto club,
            OnComplete<Response<ClubDto>> callback);

    @GET("/api/merchant/stores/{storeId}/clubs/{clubId}/members")
    void members(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            OnComplete<Response<List<ClubMembershipDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/clubs/{clubId}/items")
    void items(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            OnComplete<Response<List<ClubItemDto>>> callback);

    /// Holds a product or collection back for the club.
    @POST("/api/merchant/stores/{storeId}/clubs/{clubId}/items")
    void addItem(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            @Body ClubItemDto item,
            OnComplete<Response<List<ClubItemDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/clubs/{clubId}/items/{itemId}")
    void removeItem(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            @Path("itemId") String itemId,
            OnComplete<Response<List<ClubItemDto>>> callback);

    /// The store's active clubs, for anyone.
    @GET("/api/shop/stores/{storeId}/clubs")
    void storeClubs(@Path("storeId") String storeId,
            OnComplete<Response<List<ClubDto>>> callback);

    /// The shopper's memberships here.
    @GET("/api/shop/stores/{storeId}/clubs/me")
    void mine(@Path("storeId") String storeId,
            OnComplete<Response<List<ClubMembershipDto>>> callback);

    /// Joins a club, charging its fee to the card when it has one.
    @POST("/api/shop/stores/{storeId}/clubs/{clubId}/join")
    void join(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            @Body JoinClubDto join,
            OnComplete<Response<ClubMembershipDto>> callback);

    /// Stops renewing; the membership lasts to its renewal date.
    @POST("/api/shop/stores/{storeId}/clubs/{clubId}/leave")
    void leave(@Path("storeId") String storeId,
            @Path("clubId") String clubId,
            OnComplete<Response<ClubMembershipDto>> callback);
}
