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
import java.util.ArrayList;
import java.util.List;

/// How a chain answers a request that must sign in, and one that may not have
/// what it asked for.
///
/// ```java
/// http.exceptionHandling(handling -> handling
///         .authenticationEntryPoint(new HttpStatusEntryPoint(401))
///         .accessDeniedHandler((request, denied) ->
///                 HttpServer.Response.json(403, "{\"error\":\"forbidden\"}")));
/// ```
///
/// Without an entry point set here, form login's is the answer -- a redirect to
/// the login page -- and HTTP Basic's challenge when that is the only way in,
/// or when a script made the request. A chain with neither answers 403.
public final class ExceptionHandlingConfigurer extends SecurityConfigurer {
    private AuthenticationEntryPoint authenticationEntryPoint;
    private AccessDeniedHandler accessDeniedHandler;
    /// {RequestMatcher, AuthenticationEntryPoint}, as the sign-in parts add them.
    private final List<Object[]> defaults = new ArrayList<Object[]>();
    /// {RequestMatcher, AccessDeniedHandler}, likewise.
    private final List<Object[]> deniedDefaults = new ArrayList<Object[]>();

    ExceptionHandlingConfigurer() {
    }

    /// The entry point for every request under the chain.
    public ExceptionHandlingConfigurer authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        this.authenticationEntryPoint = entryPoint;
        return this;
    }

    /// What answers a signed-in caller that is denied; a 403 unless set.
    public ExceptionHandlingConfigurer accessDeniedHandler(AccessDeniedHandler handler) {
        this.accessDeniedHandler = handler;
        return this;
    }

    /// An entry point for the requests `preferredMatcher` matches, used when no
    /// single entry point is set. The first one added also answers the requests
    /// none of them matches.
    public ExceptionHandlingConfigurer defaultAuthenticationEntryPointFor(
            AuthenticationEntryPoint entryPoint, RequestMatcher preferredMatcher) {
        if (entryPoint == null || preferredMatcher == null) {
            throw new IllegalArgumentException("An entry point and a matcher are required");
        }
        defaults.add(new Object[] {preferredMatcher, entryPoint});
        return this;
    }

    /// A handler for the denied requests `preferredMatcher` matches, used when
    /// no single handler is set. A denied request none of them matches is
    /// answered 403.
    public ExceptionHandlingConfigurer defaultAccessDeniedHandlerFor(
            AccessDeniedHandler handler, RequestMatcher preferredMatcher) {
        if (handler == null || preferredMatcher == null) {
            throw new IllegalArgumentException("A handler and a matcher are required");
        }
        deniedDefaults.add(new Object[] {preferredMatcher, handler});
        return this;
    }

    AccessDeniedHandler resolveAccessDeniedHandler() {
        if (accessDeniedHandler != null) {
            return accessDeniedHandler;
        }
        if (deniedDefaults.isEmpty()) {
            return new AccessDeniedHandlerImpl();
        }
        RequestMatcher[] matchers = new RequestMatcher[deniedDefaults.size()];
        AccessDeniedHandler[] handlers = new AccessDeniedHandler[deniedDefaults.size()];
        for (int iter = 0 ; iter < matchers.length ; iter++) {
            matchers[iter] = (RequestMatcher) deniedDefaults.get(iter)[0];
            handlers[iter] = (AccessDeniedHandler) deniedDefaults.get(iter)[1];
        }
        return new DelegatingDenied(matchers, handlers);
    }

    /// The handler of the first matcher that matches the request, or else 403.
    private static final class DelegatingDenied implements AccessDeniedHandler {
        private final RequestMatcher[] matchers;
        private final AccessDeniedHandler[] handlers;
        private final AccessDeniedHandler otherwise = new AccessDeniedHandlerImpl();

        DelegatingDenied(RequestMatcher[] matchers, AccessDeniedHandler[] handlers) {
            this.matchers = matchers;
            this.handlers = handlers;
        }

        @Override
        public HttpServer.Response handle(HttpServer.Request request,
                                          AccessDeniedException denied) throws Exception {
            // A request refused for want of a CSRF token is not one whose
            // bearer token granted too little, whatever it carries.
            if (!(denied instanceof CsrfException)) {
                for (int iter = 0 ; iter < matchers.length ; iter++) {
                    if (matchers[iter].matches(request)) {
                        return handlers[iter].handle(request, denied);
                    }
                }
            }
            return otherwise.handle(request, denied);
        }
    }

    AuthenticationEntryPoint resolveEntryPoint() {
        if (authenticationEntryPoint != null) {
            return authenticationEntryPoint;
        }
        if (defaults.isEmpty()) {
            return new Http403ForbiddenEntryPoint();
        }
        if (defaults.size() == 1) {
            return (AuthenticationEntryPoint) defaults.get(0)[1];
        }
        RequestMatcher[] matchers = new RequestMatcher[defaults.size()];
        AuthenticationEntryPoint[] entryPoints = new AuthenticationEntryPoint[defaults.size()];
        for (int iter = 0 ; iter < matchers.length ; iter++) {
            matchers[iter] = (RequestMatcher) defaults.get(iter)[0];
            entryPoints[iter] = (AuthenticationEntryPoint) defaults.get(iter)[1];
        }
        return new Delegating(matchers, entryPoints);
    }

    /// The entry point of the first matcher that matches the request, or else
    /// the first one added.
    private static final class Delegating implements AuthenticationEntryPoint {
        private final RequestMatcher[] matchers;
        private final AuthenticationEntryPoint[] entryPoints;

        Delegating(RequestMatcher[] matchers, AuthenticationEntryPoint[] entryPoints) {
            this.matchers = matchers;
            this.entryPoints = entryPoints;
        }

        @Override
        public HttpServer.Response commence(HttpServer.Request request,
                                            AuthenticationException authException) throws Exception {
            for (int iter = 0 ; iter < matchers.length ; iter++) {
                if (matchers[iter].matches(request)) {
                    return entryPoints[iter].commence(request, authException);
                }
            }
            return entryPoints[0].commence(request, authException);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilter(new ExceptionTranslationFilter(resolveEntryPoint(),
                resolveAccessDeniedHandler(), http.resolveRequestCache()),
                HttpSecurity.ORDER_EXCEPTION_TRANSLATION);
    }
}
