/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Applying to drive. Open to anyone signed in, and only ever about the
/// caller's own application: the DRIVER role, and `DriverApi` with it, comes
/// once an admin approves.
@RestClient
public interface DrivingApi {
    /// The caller's application; its status is `none` before one is begun.
    @GET("/api/driving/application")
    void application(OnComplete<Response<DriverApplicationDto>> callback);

    /// Saves the form as a draft. Nothing has to be complete yet.
    @PUT("/api/driving/application")
    void save(@Body DriverApplicationDto application,
            OnComplete<Response<DriverApplicationDto>> callback);

    /// Stores one document, replacing an earlier one of the same kind.
    @POST("/api/driving/application/documents")
    void upload(@Body DocumentUploadDto document,
            OnComplete<Response<DriverApplicationDto>> callback);

    /// One of the caller's own documents, to show back to them.
    @GET("/api/driving/application/documents/{kind}")
    void document(@Path("kind") String kind, OnComplete<Response<DocumentContentDto>> callback);

    /// Hands the application to the admins. Refused until every field is filled
    /// and every document is there.
    @POST("/api/driving/application/submit")
    void submit(OnComplete<Response<DriverApplicationDto>> callback);
}
