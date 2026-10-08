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
package com.codename1.backend.security.oauth2.server.resource;

import com.codename1.backend.security.oauth2.core.OAuth2Error;

/// An error a request with a bearer token is answered with (RFC 6750 3.1): the
/// code, the status it is sent under, and the scope it would have needed.
public final class BearerTokenError extends OAuth2Error {
    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final String scope;

    public BearerTokenError(String errorCode, int httpStatus, String description, String errorUri) {
        this(errorCode, httpStatus, description, errorUri, null);
    }

    public BearerTokenError(String errorCode, int httpStatus, String description, String errorUri,
                            String scope) {
        super(errorCode, description, errorUri);
        this.httpStatus = httpStatus;
        this.scope = scope;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /// The scope the request needed, or null.
    public String getScope() {
        return scope;
    }
}
