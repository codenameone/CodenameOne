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

/// Turns the security exceptions thrown by what follows it -- the authorization
/// rules, a controller, a service the controller calls -- into answers.
///
/// - An [AuthenticationException] goes to the [AuthenticationEntryPoint]: a 401
///   with a challenge, or a redirect to the login page.
/// - An [AccessDeniedException] for an anonymous caller goes there too, after
///   the address is remembered, because the remedy is to sign in.
/// - An [AccessDeniedException] for a signed-in caller goes to the
///   [AccessDeniedHandler]: 403.
///
/// Anything else is not this filter's and is thrown on.
public final class ExceptionTranslationFilter implements SecurityFilter {
    private final AuthenticationEntryPoint entryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final RequestCache requestCache;

    ExceptionTranslationFilter(AuthenticationEntryPoint entryPoint,
                               AccessDeniedHandler accessDeniedHandler, RequestCache requestCache) {
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.requestCache = requestCache;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        try {
            return chain.doFilter(request);
        } catch (Exception err) {
            RuntimeException security = securityCause(err);
            if (security == null) {
                throw err;
            }
            return translate(request, security);
        }
    }

    /// The security exception `err` is or was caused by, or null. The cause is
    /// searched because generated code may wrap what a bean throws.
    static RuntimeException securityCause(Throwable err) {
        Throwable at = err;
        for (int depth = 0 ; at != null && depth < 8 ; depth++) {
            if (at instanceof AuthenticationException || at instanceof AccessDeniedException) {
                return (RuntimeException) at;
            }
            Throwable cause = at.getCause();
            if (cause == at) { //NOPMD CompareObjectsWithEquals - a self-caused throwable
                break;
            }
            at = cause;
        }
        return null;
    }

    /// The answer to a security exception.
    HttpServer.Response translate(HttpServer.Request request, RuntimeException err)
            throws Exception {
        if (err instanceof AuthenticationException) {
            return startAuthentication(request, (AuthenticationException) err);
        }
        if (!(err instanceof AccessDeniedException)) {
            throw err;
        }
        AccessDeniedException denied = (AccessDeniedException) err;
        if (denied instanceof CsrfException) {
            return accessDeniedHandler.handle(request, denied);
        }
        SecurityContext context = SecurityContextHolder.peek();
        Authentication authentication = context == null ? null : context.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return startAuthentication(request, new InsufficientAuthenticationException(
                    "Full authentication is required to access this resource", denied));
        }
        return accessDeniedHandler.handle(request, denied);
    }

    private HttpServer.Response startAuthentication(HttpServer.Request request,
                                                    AuthenticationException reason) throws Exception {
        // Whoever the context named was not good enough for this request.
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        requestCache.saveRequest(request);
        return entryPoint.commence(request, reason);
    }
}
