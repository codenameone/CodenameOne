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

/// The people an account books for: the account holder, and the children
/// they add. A child has no sign-in of their own; everything about them is
/// done by the account that added them.
@RestClient
public interface HouseholdApi {
    @GET("/api/household")
    void members(OnComplete<Response<List<MemberDto>>> callback);

    @POST("/api/household")
    void addChild(@Body NewMemberDto child, OnComplete<Response<MemberDto>> callback);

    /// Records that the account holder accepted the studio's waiver for this
    /// member. No class can be booked for a member without one.
    @POST("/api/household/{id}/waiver")
    void signWaiver(@Path("id") String id, OnComplete<Response<MemberDto>> callback);

    /// What the member's door pass is made from. Asked once and kept on the
    /// device, so the pass works with no signal at the door.
    @GET("/api/household/{id}/pass")
    void pass(@Path("id") String id, OnComplete<Response<PassDto>> callback);

    /// Replaces the pass secret, for a phone that was lost: every code made from
    /// the old one stops opening the door.
    @POST("/api/household/{id}/pass/rotate")
    void rotatePass(@Path("id") String id, OnComplete<Response<PassDto>> callback);
}
