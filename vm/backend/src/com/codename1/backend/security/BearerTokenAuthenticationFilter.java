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
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenResolver;

/// Authenticates a request from its bearer token.
///
/// The token is checked on every request that carries one and nothing is kept:
/// no session is started and the authentication is not stored. A request
/// without a token passes through untouched, for the rules further on to
/// judge. One whose token is refused is answered at once -- 401 with the reason
/// in `WWW-Authenticate` -- and never reaches the application.
public final class BearerTokenAuthenticationFilter implements SecurityFilter {
    /// The exchange attribute a filter ahead of this one sets to say the
    /// request's bearer value was its own to judge -- an API key -- and is not
    /// a token for this filter.
    static final String CLAIMED = "com.codename1.backend.security.bearer.claimed";

    private final AuthenticationManagerResolver managers;
    private final BearerTokenResolver resolver;
    private final AuthenticationEntryPoint entryPoint;

    BearerTokenAuthenticationFilter(AuthenticationManagerResolver managers,
                                    BearerTokenResolver resolver,
                                    AuthenticationEntryPoint entryPoint) {
        this.managers = managers;
        this.resolver = resolver;
        this.entryPoint = entryPoint;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityExchange exchange = SecurityExchange.of(request);
        if (exchange != null && exchange.getAttribute(CLAIMED) != null) {
            return chain.doFilter(request);
        }
        String token;
        try {
            token = resolver.resolve(request);
        } catch (OAuth2AuthenticationException invalid) {
            return entryPoint.commence(request, invalid);
        }
        if (token == null) {
            return chain.doFilter(request);
        }
        try {
            AuthenticationManager manager = managers.resolve(request);
            Authentication result = manager == null ? null
                    : manager.authenticate(new BearerTokenAuthenticationToken(token));
            if (result == null) {
                throw new AuthenticationServiceException("Nothing authenticated the bearer token");
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(result);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationServiceException broken) {
            // The token could not be judged: this server's failure, or its
            // issuer's. Not a 401 -- the caller's token may be perfectly good,
            // and a client told otherwise throws it away.
            SecurityContextHolder.clearContext();
            return Responses.status(500, "Internal Server Error");
        } catch (AuthenticationException refused) {
            SecurityContextHolder.clearContext();
            return entryPoint.commence(request, refused);
        }
        return chain.doFilter(request);
    }
}
