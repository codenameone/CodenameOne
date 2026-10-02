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
package com.codename1.backend.test;

/// The requests [MockMvc] sends, for static import:
///
/// ```java
/// mvc.perform(post("/pets").contentType(MediaType.APPLICATION_JSON)
///         .content("{\"name\":\"Rex\"}"));
/// mvc.perform(get("/pets/{id}", 7));
/// ```
///
/// `{name}` placeholders take the variables in order, percent-encoded.
public final class MockMvcRequestBuilders {
    private MockMvcRequestBuilders() {
    }

    public static MockRequestBuilder get(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("GET", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder post(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("POST", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder put(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("PUT", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder patch(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("PATCH", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder delete(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("DELETE", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder head(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("HEAD", uriTemplate, uriVariables);
    }

    public static MockRequestBuilder options(String uriTemplate, Object... uriVariables) {
        return new MockRequestBuilder("OPTIONS", uriTemplate, uriVariables);
    }

    /// Any method, by name.
    public static MockRequestBuilder request(String method, String uriTemplate,
                                             Object... uriVariables) {
        return new MockRequestBuilder(method, uriTemplate, uriVariables);
    }

    /// A `multipart/form-data` POST, built with `file(...)` and `param(...)`.
    public static MockMultipartRequestBuilder multipart(String uriTemplate, Object... uriVariables) {
        return new MockMultipartRequestBuilder("POST", uriTemplate, uriVariables);
    }
}
