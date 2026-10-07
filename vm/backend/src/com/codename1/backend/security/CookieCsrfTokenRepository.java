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

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;

/// Keeps the token in a cookie, `XSRF-TOKEN`, for a page whose script reads the
/// cookie and sends its value back in the `X-XSRF-TOKEN` header -- the
/// convention Angular and axios follow. The token is also kept in the session
/// so a sibling subdomain cannot inject a cookie and submit a matching value.
///
/// ```java
/// http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
/// ```
///
/// A script can only read the cookie when it is not `HttpOnly`, which is what
/// [#withHttpOnlyFalse] is for. Because the script has the token as the cookie
/// holds it, a chain with this repository accepts the token unmasked in the
/// header as well as masked. A cookie is trusted only when it matches the
/// token issued to the current session. Tokens copied from other sessions
/// are rejected.
public final class CookieCsrfTokenRepository implements CsrfTokenRepository {
    private static final String ATTRIBUTE = "CN1_COOKIE_CSRF_TOKEN:";
    private String cookieName = "XSRF-TOKEN";
    private String headerName = "X-XSRF-TOKEN";
    private String parameterName = "_csrf";
    private String cookiePath = "/";
    private boolean cookieHttpOnly = true;

    /// A repository whose cookie a script can read.
    public static CookieCsrfTokenRepository withHttpOnlyFalse() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.cookieHttpOnly = false;
        return repository;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = CsrfFilter.requireName(cookieName, "cookieName");
    }

    public void setHeaderName(String headerName) {
        this.headerName = CsrfFilter.requireName(headerName, "headerName");
    }

    public void setParameterName(String parameterName) {
        this.parameterName = CsrfFilter.requireName(parameterName, "parameterName");
    }

    /// The cookie's path; `/` unless set.
    public void setCookiePath(String cookiePath) {
        if (cookiePath == null || !cookiePath.startsWith("/") || cookiePath.indexOf(';') >= 0) {
            throw new IllegalArgumentException("A cookie path starts with / and holds no ;");
        }
        for (int iter = 0 ; iter < cookiePath.length() ; iter++) {
            char c = cookiePath.charAt(iter);
            if (c <= 0x20 || c > 0x7e) {
                throw new IllegalArgumentException("A cookie path is printable ASCII");
            }
        }
        this.cookiePath = cookiePath;
    }

    public void setCookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    @Override
    public CsrfToken generateToken(HttpServer.Request request) {
        return new DefaultCsrfToken(headerName, parameterName, CsrfFilter.newTokenValue());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServer.Request request) {
        SecurityExchange exchange = SecurityExchange.of(request);
        if (exchange == null) {
            throw new IllegalStateException("The CSRF cookie is written with the response of "
                    + "the request a chain is serving, and this thread serves none");
        }
        HttpSession session = request.getSession(token != null);
        if (session != null) {
            if (token == null) {
                session.removeAttribute(ATTRIBUTE + cookieName);
            } else {
                session.setAttribute(ATTRIBUTE + cookieName, token.getToken());
            }
        }
        StringBuilder cookie = new StringBuilder(cookieName).append('=')
                .append(token == null ? "" : token.getToken()).append("; Path=").append(cookiePath);
        if (token == null) {
            cookie.append("; Max-Age=0");
        }
        if (exchange.isSecure()) {
            cookie.append("; Secure");
        }
        if (cookieHttpOnly) {
            cookie.append("; HttpOnly");
        }
        cookie.append("; SameSite=Lax");
        exchange.addResponseHeader("Set-Cookie", cookie.toString());
    }

    @Override
    public CsrfToken loadToken(HttpServer.Request request) {
        String value = request.getCookie(cookieName);
        if (value == null || value.length() == 0) {
            return null;
        }
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(ATTRIBUTE + cookieName);
        if (!(expected instanceof String) || !Crypto.equalsConstantTime(
                Responses.utf8((String) expected), Responses.utf8(value))) {
            return null;
        }
        return new DefaultCsrfToken(headerName, parameterName, value);
    }
}
