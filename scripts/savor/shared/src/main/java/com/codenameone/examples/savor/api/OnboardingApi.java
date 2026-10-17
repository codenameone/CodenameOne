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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Applying to deliver. Open to anyone signed in, and only ever about the
/// caller's own application: the COURIER role, and `CourierApi` with it, comes
/// once an admin approves.
@RestClient
public interface OnboardingApi {
    /// The caller's application; its status is `none` before one is begun.
    @GET("/api/onboarding/application")
    void application(OnComplete<Response<CourierApplicationDto>> callback);

    /// Saves the form as a draft. Nothing has to be complete yet.
    @PUT("/api/onboarding/application")
    void save(@Body CourierApplicationDto application,
            OnComplete<Response<CourierApplicationDto>> callback);

    /// Stores one document, replacing an earlier one of the same kind.
    @POST("/api/onboarding/application/documents")
    void upload(@Body DocumentUploadDto document,
            OnComplete<Response<CourierApplicationDto>> callback);

    /// One of the caller's own documents, to show back to them.
    @GET("/api/onboarding/application/documents/{kind}")
    void document(@Path("kind") String kind, OnComplete<Response<DocumentContentDto>> callback);

    /// Hands the application to the admins. Refused until every field is filled
    /// and every document is there.
    @POST("/api/onboarding/application/submit")
    void submit(OnComplete<Response<CourierApplicationDto>> callback);
}
