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

/// The app directory and app billing.
///
/// A developer submits a listing for an app; a platform admin approves,
/// rejects or delists it, and only approved listings are public. Stores
/// browse the directory, install from it (the listing's `installUrl`, the
/// app's own link, which starts the OAuth consent of [AppApi]) and review
/// what they installed. An app charges a store through the key API
/// (`POST /api/v1/stores/{storeId}/app-charges` with its token), and the
/// owner approves or declines it here or on the charge's web page.
@RestClient
public interface AppStoreApi {
    /// Approved listings matching `q` (name, tagline, description) and
    /// `category`, both optional; `page` counts from 0. Anyone may browse.
    @GET("/api/shop/apps")
    void directory(@Query("q") String q, @Query("category") String category,
            @Query("page") int page, OnComplete<Response<List<AppListingDto>>> callback);

    /// One approved listing.
    @GET("/api/shop/apps/{handle}")
    void listing(@Path("handle") String handle, OnComplete<Response<AppListingDto>> callback);

    /// An approved listing's visible reviews, newest first.
    @GET("/api/shop/apps/{handle}/reviews")
    void reviews(@Path("handle") String handle,
            OnComplete<Response<List<AppReviewDto>>> callback);

    /// The listing of one of the caller's apps, or 404 before it has one.
    @GET("/api/developer/apps/{appId}/listing")
    void myListing(@Path("appId") String appId, OnComplete<Response<AppListingDto>> callback);

    /// Submits (or resubmits) the listing of one of the caller's apps for review.
    @POST("/api/developer/apps/{appId}/listing")
    void submitListing(@Path("appId") String appId, @Body AppListingDto listing,
            OnComplete<Response<AppListingDto>> callback);

    /// Listings by status (`PENDING` when empty), oldest first. Admins only.
    @GET("/api/admin/app-listings")
    void listings(@Query("status") String status,
            OnComplete<Response<List<AppListingDto>>> callback);

    /// Approves, rejects or delists a listing. Admins only.
    @POST("/api/admin/app-listings/{listingId}/decision")
    void decide(@Path("listingId") String listingId, @Body AppListingDecisionDto decision,
            OnComplete<Response<AppListingDto>> callback);

    /// Every review of a listing, hidden ones included. Admins only.
    @GET("/api/admin/app-listings/{listingId}/reviews")
    void allReviews(@Path("listingId") String listingId,
            OnComplete<Response<List<AppReviewDto>>> callback);

    /// Hides a review from the listing and its average. Admins only.
    @POST("/api/admin/app-reviews/{reviewId}/hide")
    void hideReview(@Path("reviewId") String reviewId,
            OnComplete<Response<AppReviewDto>> callback);

    /// The store's review of an app (`appId` in the body), written or
    /// rewritten: one per store, and only by a store that installed it.
    @POST("/api/merchant/stores/{storeId}/app-reviews")
    void review(@Path("storeId") String storeId, @Body AppReviewDto review,
            OnComplete<Response<AppReviewDto>> callback);

    /// The charges apps asked the store for, newest first. Owners only.
    @GET("/api/merchant/stores/{storeId}/app-charges")
    void charges(@Path("storeId") String storeId,
            OnComplete<Response<List<AppChargeDto>>> callback);

    /// Approves or declines a pending charge. Owners only.
    @POST("/api/merchant/stores/{storeId}/app-charges/{chargeId}/decision")
    void decideCharge(@Path("storeId") String storeId, @Path("chargeId") String chargeId,
            @Body AppChargeDecisionDto decision, OnComplete<Response<AppChargeDto>> callback);
}
