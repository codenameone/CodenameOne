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

/// Plans, the household's memberships, and what they were billed.
///
/// Billing is the server's: a membership renews on the account's default card
/// with nobody present, and a charge the card refuses is tried again on a
/// ladder of days before the membership falls PAST_DUE. Memberships are a
/// service delivered in the studio, not digital content, so nothing here goes
/// through an app store.
@RestClient
public interface MembershipApi {
    /// The plans on sale. Each one's age band is checked against the member it
    /// is bought for.
    @GET("/api/membership/plans")
    void plans(OnComplete<Response<List<PlanDto>>> callback);

    @GET("/api/membership")
    void memberships(OnComplete<Response<List<MembershipDto>>> callback);

    /// Starts a plan and charges its first period now. Answers 402 with the
    /// card's reason when the charge is refused; nothing is started then.
    @POST("/api/membership")
    void join(@Body JoinDto join, OnComplete<Response<MembershipDto>> callback);

    @POST("/api/membership/{id}/freeze")
    void freeze(@Path("id") String id, @Body FreezeDto freeze,
            OnComplete<Response<MembershipDto>> callback);

    @POST("/api/membership/{id}/unfreeze")
    void unfreeze(@Path("id") String id, OnComplete<Response<MembershipDto>> callback);

    /// Ends the membership when the period paid for runs out. Nothing is
    /// refunded and nothing more is charged.
    @POST("/api/membership/{id}/cancel")
    void cancel(@Path("id") String id, OnComplete<Response<MembershipDto>> callback);

    @GET("/api/membership/invoices")
    void invoices(OnComplete<Response<List<InvoiceDto>>> callback);

    /// Pays an open or failed invoice now, which brings a PAST_DUE membership
    /// back to ACTIVE.
    @POST("/api/membership/invoices/{id}/pay")
    void pay(@Path("id") String id, @Body PayInvoiceDto pay,
            OnComplete<Response<InvoiceDto>> callback);
}
