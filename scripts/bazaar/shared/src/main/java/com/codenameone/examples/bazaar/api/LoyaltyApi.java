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
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Loyalty: the store's points programme and tiers (OWNER, MANAGER), members and member cards (any staff, for the register), and the shopper's own card, points and rewards.
@RestClient
public interface LoyaltyApi {
    /// The programme and its tiers; a store without one answers an inactive default.
    @GET("/api/merchant/stores/{storeId}/loyalty/program")
    void program(@Path("storeId") String storeId,
            OnComplete<Response<LoyaltyProgramDto>> callback);

    /// Saves the programme and replaces its tiers (a tier without an id is new).
    @POST("/api/merchant/stores/{storeId}/loyalty/program")
    void saveProgram(@Path("storeId") String storeId,
            @Body LoyaltyProgramDto program,
            OnComplete<Response<LoyaltyProgramDto>> callback);

    /// Members, best first; `q` filters by e-mail or name.
    @GET("/api/merchant/stores/{storeId}/loyalty/members")
    void members(@Path("storeId") String storeId,
            @Query("q") String q,
            OnComplete<Response<List<LoyaltyMemberDto>>> callback);

    /// The member a scanned card belongs to.
    @GET("/api/merchant/stores/{storeId}/loyalty/cards/{token}")
    void card(@Path("storeId") String storeId,
            @Path("token") String token,
            OnComplete<Response<LoyaltyMemberDto>> callback);

    /// A member's points history.
    @GET("/api/merchant/stores/{storeId}/loyalty/members/{memberId}/ledger")
    void ledger(@Path("storeId") String storeId,
            @Path("memberId") String memberId,
            OnComplete<Response<List<LedgerEntryDto>>> callback);

    /// Adds (or with a negative number takes) points by hand.
    @POST("/api/merchant/stores/{storeId}/loyalty/members/{memberId}/adjust")
    void adjust(@Path("storeId") String storeId,
            @Path("memberId") String memberId,
            @Body PointsAdjustDto change,
            OnComplete<Response<LoyaltyMemberDto>> callback);

    /// Redeems a member's points at the register; the answer's code goes on the sale.
    @POST("/api/merchant/stores/{storeId}/loyalty/members/{memberId}/redeem")
    void redeemFor(@Path("storeId") String storeId,
            @Path("memberId") String memberId,
            @Body RedeemDto redeem,
            OnComplete<Response<LoyaltyRewardDto>> callback);

    /// The signed-in shopper's card in this store, joining the programme on first use.
    @GET("/api/shop/stores/{storeId}/loyalty/me")
    void me(@Path("storeId") String storeId,
            OnComplete<Response<LoyaltyMemberDto>> callback);

    /// The shopper's points history.
    @GET("/api/shop/stores/{storeId}/loyalty/me/ledger")
    void myLedger(@Path("storeId") String storeId,
            OnComplete<Response<List<LedgerEntryDto>>> callback);

    /// The shopper's rewards, newest first.
    @GET("/api/shop/stores/{storeId}/loyalty/me/rewards")
    void myRewards(@Path("storeId") String storeId,
            OnComplete<Response<List<LoyaltyRewardDto>>> callback);

    /// Turns points into a single-use discount code.
    @POST("/api/shop/stores/{storeId}/loyalty/me/redeem")
    void redeem(@Path("storeId") String storeId,
            @Body RedeemDto redeem,
            OnComplete<Response<LoyaltyRewardDto>> callback);

    /// Records who referred the shopper; both are rewarded on the shopper's first paid order.
    @POST("/api/shop/stores/{storeId}/loyalty/me/referral")
    void referral(@Path("storeId") String storeId,
            @Body ReferralDto referral,
            OnComplete<Response<LoyaltyMemberDto>> callback);
}
