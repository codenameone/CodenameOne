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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;

/// Keeps the token in the HTTP session: the default. The token is sent to the
/// page masked and must come back masked, in the `X-CSRF-TOKEN` header or the
/// `_csrf` form field.
public final class HttpSessionCsrfTokenRepository implements CsrfTokenRepository {
    private static final String ATTRIBUTE = "SPRING_SECURITY_CSRF_TOKEN";

    private String headerName = "X-CSRF-TOKEN";
    private String parameterName = "_csrf";

    public void setHeaderName(String headerName) {
        this.headerName = CsrfFilter.requireName(headerName, "headerName");
    }

    public void setParameterName(String parameterName) {
        this.parameterName = CsrfFilter.requireName(parameterName, "parameterName");
    }

    @Override
    public CsrfToken generateToken(HttpServer.Request request) {
        return new DefaultCsrfToken(headerName, parameterName, CsrfFilter.newTokenValue());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServer.Request request) {
        if (token == null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute(ATTRIBUTE) != null) {
                session.removeAttribute(ATTRIBUTE);
            }
            return;
        }
        // The value alone: the session store keeps what JSON can write.
        request.getSession(true).setAttribute(ATTRIBUTE, token.getToken());
    }

    @Override
    public CsrfToken loadToken(HttpServer.Request request) {
        HttpSession session = request.getSession(false);
        Object stored = session == null ? null : session.getAttribute(ATTRIBUTE);
        if (!(stored instanceof String) || ((String) stored).length() == 0) {
            return null;
        }
        return new DefaultCsrfToken(headerName, parameterName, (String) stored);
    }
}
