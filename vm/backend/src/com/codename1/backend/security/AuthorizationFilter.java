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
import java.util.function.Supplier;

/// Puts the request to the chain's authorization rules, and throws
/// [AccessDeniedException] when they refuse it. Last in every chain: what it
/// lets through reaches the application.
public final class AuthorizationFilter implements SecurityFilter {
    private static final Supplier<Authentication> CURRENT = new Supplier<Authentication>() {
        @Override
        public Authentication get() {
            SecurityContext context = SecurityContextHolder.peek();
            return context == null ? null : context.getAuthentication();
        }
    };

    /// The attribute of the request's [SecurityExchange] that holds, as a list
    /// of text, the authorities any one of which the rule that refused the
    /// request would have accepted. Set when that rule was one about
    /// authorities, so that whatever answers the refusal can say what to ask
    /// for.
    public static final String REQUIRED_AUTHORITIES =
            "com.codename1.backend.security.requiredAuthorities";

    private final AuthorizationManager<HttpServer.Request> authorizationManager;

    AuthorizationFilter(AuthorizationManager<HttpServer.Request> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        AuthorizationDecision decision = authorizationManager.check(CURRENT, request);
        if (decision != null && !decision.isGranted()) {
            SecurityExchange exchange = SecurityExchange.of(request);
            if (exchange != null && decision instanceof AuthorityAuthorizationDecision) {
                exchange.setAttribute(REQUIRED_AUTHORITIES,
                        ((AuthorityAuthorizationDecision) decision).getAuthorities());
            }
            throw new AccessDeniedException("Access Denied");
        }
        return chain.doFilter(request);
    }
}
