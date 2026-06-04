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
package com.codename1.io.graphql;

import java.util.Collections;
import java.util.List;

/// Result of a GraphQL query or mutation. Mirrors the shape of
/// [com.codename1.io.rest.Response] and
/// [com.codename1.io.grpc.GrpcResponse] so call sites feel familiar,
/// but reflects a GraphQL-specific reality: a single response can
/// carry **both** mapped `data` and a non-empty `errors` array (a
/// partial result). Always check [#hasErrors()] in addition to
/// [#getData()].
///
/// [#getResponseCode()] returns the underlying HTTP status (usually
/// `200`, since GraphQL surfaces logical failures in the body rather
/// than via HTTP status). `0` is used for transport-level failures
/// that never reached an HTTP response.
public final class GraphQLResponse<T> {

    private final int httpCode;
    private final T data;
    private final List<GraphQLError> errors;
    private final String responseErrorMessage;

    public GraphQLResponse(int httpCode, T data, List<GraphQLError> errors,
                           String responseErrorMessage) {
        this.httpCode = httpCode;
        this.data = data;
        this.errors = errors == null ? Collections.<GraphQLError>emptyList() : errors;
        this.responseErrorMessage = responseErrorMessage;
    }

    /// The mapped `data` payload, or null when the server returned no
    /// data (a fatal error, or an empty/transport-failed response).
    public T getData() {
        return data;
    }

    /// The `errors` array, never null (empty when the call succeeded
    /// cleanly).
    public List<GraphQLError> getErrors() {
        return errors;
    }

    /// `true` when the response carried at least one GraphQL error.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /// The underlying HTTP status code. `0` signals a transport-level
    /// failure (network error, unparseable body) that never produced
    /// an HTTP response.
    public int getResponseCode() {
        return httpCode;
    }

    /// `true` iff the response carried no GraphQL errors. Note this is
    /// independent of [#getData()] being non-null -- a valid response
    /// to a nullable root field can be error-free with null data.
    public boolean isOk() {
        return errors.isEmpty();
    }

    /// The first error message (a transport-failure description when
    /// the call never reached the server), or null on clean success.
    public String getResponseErrorMessage() {
        return responseErrorMessage;
    }
}
