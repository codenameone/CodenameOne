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
import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.AccessDeniedHandler;

/// Answers a request whose token is good and does not grant what the request
/// needs, as RFC 6750 3.1 says to:
///
/// ```java
/// 403   WWW-Authenticate: Bearer error="insufficient_scope",
///           error_description="The request requires higher privileges than provided by the
///           access token.", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
/// ```
public final class BearerTokenAccessDeniedHandler implements AccessDeniedHandler {
    private String realmName;

    /// A realm to name in the challenge; none unless set.
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request,
                                      AccessDeniedException accessDeniedException) {
        StringBuilder challenge = new StringBuilder("Bearer");
        boolean first = true;
        if (realmName != null) {
            first = BearerTokenAuthenticationEntryPoint.parameter(challenge, first, "realm",
                    realmName);
        }
        first = BearerTokenAuthenticationEntryPoint.parameter(challenge, first, "error",
                "insufficient_scope");
        first = BearerTokenAuthenticationEntryPoint.parameter(challenge, first, "error_description",
                "The request requires higher privileges than provided by the access token.");
        BearerTokenAuthenticationEntryPoint.parameter(challenge, first, "error_uri",
                "https://tools.ietf.org/html/rfc6750#section-3.1");
        return HttpServer.Response.text(403, "").header("WWW-Authenticate", challenge.toString());
    }
}
