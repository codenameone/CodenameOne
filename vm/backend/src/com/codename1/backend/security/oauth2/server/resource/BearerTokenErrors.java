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

import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;

/// The three errors of RFC 6750 3.1.
public final class BearerTokenErrors {
    private static final String SPEC = "https://tools.ietf.org/html/rfc6750#section-3.1";

    private BearerTokenErrors() {
    }

    /// 400: the request is malformed -- two tokens, or a header that is not one.
    public static BearerTokenError invalidRequest(String message) {
        return new BearerTokenError(OAuth2ErrorCodes.INVALID_REQUEST, 400, message, SPEC);
    }

    /// 401: the token is expired, malformed, or does not verify.
    public static BearerTokenError invalidToken(String message) {
        return new BearerTokenError(OAuth2ErrorCodes.INVALID_TOKEN, 401, message, SPEC);
    }

    /// 403: the token is good and does not grant what the request needs.
    public static BearerTokenError insufficientScope(String message, String scope) {
        return new BearerTokenError(OAuth2ErrorCodes.INSUFFICIENT_SCOPE, 403, message, SPEC, scope);
    }
}
