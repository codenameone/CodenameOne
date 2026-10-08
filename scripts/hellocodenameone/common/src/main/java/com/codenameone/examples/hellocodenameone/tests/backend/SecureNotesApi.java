/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The backend's secured API as the app declares it. Nothing here mentions a
/// token: the generated client gets its `Authorization` header from the
/// authorizer registered for the base URL, which is the point of the tests that
/// use it.
@RestClient
public interface SecureNotesApi {
    @GET("/api/secure/notes")
    void notes(OnComplete<Response<List<NoteDto>>> callback);

    @GET("/api/secure/whoami")
    void whoami(OnComplete<Response<String>> callback);

    @GET("/api/secure/summary/{owner}")
    void summary(@Path("owner") String owner, OnComplete<Response<String>> callback);

    @GET("/api/secure/admin")
    void admin(OnComplete<Response<String>> callback);

    static SecureNotesApi of(String baseUrl) {
        return com.codename1.io.rest.RestClients.create(SecureNotesApi.class, baseUrl);
    }
}
