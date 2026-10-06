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

/// Signs a user in from the login form: a POST to the login processing URL
/// carrying `username` and `password`.
///
/// On success the session id changes, the authentication is saved for later
/// requests, and the chain's [AuthenticationSuccessHandler] answers -- by
/// default with a redirect to where the user was going. On failure nothing is
/// kept and the [AuthenticationFailureHandler] answers.
public final class UsernamePasswordAuthenticationFilter implements SecurityFilter {
    private final RequestMatcher loginRequest;
    private final String usernameParameter;
    private final String passwordParameter;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final SecurityContextRepository repository;
    private final SessionAuthentication sessionAuthentication;

    UsernamePasswordAuthenticationFilter(RequestMatcher loginRequest, String usernameParameter,
            String passwordParameter, AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler, SecurityContextRepository repository,
            SessionAuthentication sessionAuthentication) {
        this.loginRequest = loginRequest;
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
        this.authenticationManager = authenticationManager;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.repository = repository;
        this.sessionAuthentication = sessionAuthentication;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!loginRequest.matches(request)) {
            return chain.doFilter(request);
        }
        String username = Responses.param(request, usernameParameter);
        String password = Responses.param(request, passwordParameter);
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
        Authentication result;
        try {
            result = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException refused) {
            SecurityContextHolder.clearContext();
            return failureHandler.onAuthenticationFailure(request, refused);
        }
        if (result == null) {
            SecurityContextHolder.clearContext();
            return failureHandler.onAuthenticationFailure(request,
                    new AuthenticationServiceException("The AuthenticationManager returned no "
                            + "authentication"));
        }
        sessionAuthentication.onAuthentication(request);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request);
        return successHandler.onAuthenticationSuccess(request, result);
    }
}
