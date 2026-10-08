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
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;

/// Finds the token in the `Authorization: Bearer` header.
///
/// A token in the address -- `?access_token=...` -- is read only when
/// [#setAllowUriQueryParameter] says so, and then on a GET alone. An address is
/// what ends up in access logs, browser history and the `Referer` of the next
/// page, which is no place for a credential; the switch exists for the client
/// that cannot set a header, an `EventSource` or a download link.
///
/// A request with a token in both places is refused rather than guessed at.
public final class DefaultBearerTokenResolver implements BearerTokenResolver {
    private boolean allowUriQueryParameter;
    private String bearerTokenHeaderName = "Authorization";

    /// Whether `access_token` in the query of a GET is read; not, unless set.
    public void setAllowUriQueryParameter(boolean allowUriQueryParameter) {
        this.allowUriQueryParameter = allowUriQueryParameter;
    }

    /// The header the token is read from; `Authorization` unless set. For a
    /// server behind a proxy that keeps `Authorization` for itself.
    public void setBearerTokenHeaderName(String bearerTokenHeaderName) {
        if (bearerTokenHeaderName == null || bearerTokenHeaderName.length() == 0) {
            throw new IllegalArgumentException("A header name is required");
        }
        this.bearerTokenHeaderName = bearerTokenHeaderName;
    }

    @Override
    public String resolve(HttpServer.Request request) {
        String fromHeader = fromHeader(request);
        String fromQuery = allowUriQueryParameter && "GET".equals(request.getMethod())
                ? fromQuery(request) : null;
        if (fromHeader != null && fromQuery != null) {
            throw new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest(
                    "Found multiple bearer tokens in the request"));
        }
        return fromHeader != null ? fromHeader : fromQuery;
    }

    private String fromHeader(HttpServer.Request request) {
        String header = request.getHeader(bearerTokenHeaderName);
        if (header == null) {
            return null;
        }
        header = header.trim();
        // Compared character by character: folding the scheme with the locale
        // would stop "BEARER" matching under a Turkish one.
        if (!header.regionMatches(true, 0, "Bearer", 0, 6)
                || (header.length() > 6 && header.charAt(6) != ' ')) {
            return null;
        }
        String token = header.length() <= 7 ? "" : header.substring(7).trim();
        if (!wellFormed(token)) {
            throw new OAuth2AuthenticationException(BearerTokenErrors.invalidToken(
                    "Bearer token is malformed"));
        }
        return token;
    }

    private static String fromQuery(HttpServer.Request request) {
        String token;
        try {
            token = request.queryParam("access_token");
        } catch (RuntimeException malformed) {
            return null;
        }
        if (token == null) {
            return null;
        }
        if (!wellFormed(token)) {
            throw new OAuth2AuthenticationException(BearerTokenErrors.invalidToken(
                    "Bearer token is malformed"));
        }
        return token;
    }

    /// RFC 6750 2.1: `1*( ALPHA / DIGIT / "-" / "." / "_" / "~" / "+" / "/" ) *"="`.
    private static boolean wellFormed(String token) {
        int length = token.length();
        int end = length;
        while (end > 0 && token.charAt(end - 1) == '=') {
            end--;
        }
        if (end == 0) {
            return false;
        }
        for (int iter = 0 ; iter < end ; iter++) {
            char c = token.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~' || c == '+' || c == '/';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
