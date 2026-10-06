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
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationServer;

/// Answers the endpoints of an authorization server a user must be signed in
/// for: the authorization endpoint and the device verification page.
///
/// It runs once the chain knows who the request is from, and inside the part
/// of the chain that turns "nobody is signed in" into a trip to the login page
/// and back -- so the user signs in whichever way the chain declares, and
/// arrives here again with the request they started with.
public final class OAuth2AuthorizationEndpointFilter implements SecurityFilter {
    private final OAuth2AuthorizationServer server;

    OAuth2AuthorizationEndpointFilter(OAuth2AuthorizationServer server) {
        this.server = server;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        if (!server.isUserEndpoint(path)) {
            return chain.doFilter(request);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            authentication = null;
        }
        if (path.equals(server.getSettings().getAuthorizationEndpoint())) {
            return server.authorize(request, authentication);
        }
        CsrfToken token = CsrfFilter.getToken(request);
        return server.deviceVerification(request, authentication,
                token == null ? null : token.getParameterName(),
                token == null ? null : token.getHeaderName(),
                token == null ? null : token.getToken());
    }
}
