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

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.AuthenticationEntryPoint;
import com.codename1.backend.security.AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;

/// Answers a request that needs a bearer token and has none, or has a bad one,
/// as RFC 6750 3 says to:
///
/// ```java
/// 401   WWW-Authenticate: Bearer
/// 401   WWW-Authenticate: Bearer error="invalid_token", error_description="Jwt expired at ...",
///                         error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
/// 400   WWW-Authenticate: Bearer error="invalid_request", error_description="Found multiple ..."
/// ```
///
/// A request that sent no token is told only that one is wanted. One that sent
/// a token is told what was wrong with it.
public final class BearerTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private String realmName;

    /// A realm to name in the challenge; none unless set.
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public HttpServer.Response commence(HttpServer.Request request,
                                        AuthenticationException authException) {
        int status = 401;
        StringBuilder challenge = new StringBuilder("Bearer");
        boolean first = true;
        if (realmName != null) {
            first = parameter(challenge, first, "realm", realmName);
        }
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) authException).getError();
            first = parameter(challenge, first, "error", error.getErrorCode());
            if (error.getDescription() != null && error.getDescription().length() > 0) {
                first = parameter(challenge, first, "error_description", error.getDescription());
            }
            if (error.getUri() != null && error.getUri().length() > 0) {
                first = parameter(challenge, first, "error_uri", error.getUri());
            }
            if (error instanceof BearerTokenError) {
                BearerTokenError bearer = (BearerTokenError) error;
                if (bearer.getScope() != null && bearer.getScope().length() > 0) {
                    parameter(challenge, first, "scope", bearer.getScope());
                }
                status = bearer.getHttpStatus();
            }
        }
        return HttpServer.Response.text(status, "").header("WWW-Authenticate", challenge.toString());
    }

    /// Appends `name="value"`; answers false, for "no longer the first".
    static boolean parameter(StringBuilder challenge, boolean first, String name, String value) {
        challenge.append(first ? " " : ", ").append(name).append("=\"");
        // RFC 6750 3: %x20-21 / %x23-5B / %x5D-7E. Part of a description came
        // out of the token, and a header is no place for what its author chose.
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            challenge.append(c >= 0x20 && c <= 0x7e && c != '"' && c != '\\' ? c : '?');
        }
        challenge.append('"');
        return false;
    }
}
