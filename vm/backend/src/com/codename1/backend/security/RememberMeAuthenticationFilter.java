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
import com.codename1.backend.security.rememberme.RememberMeServices;

/// Recognizes a returning user by their remember-me cookie, on a request
/// nobody has signed in for.
///
/// A request that already has an authentication -- from its session, or from a
/// credential it carried -- is left alone. Otherwise the chain's
/// [RememberMeServices] are asked; when they recognize the user, the
/// authentication becomes the request's and is saved, so the session that
/// starts here carries it and the cookie is not consulted again until that
/// session ends.
///
/// On a chain with a [SecondFactorPolicy], a user the policy requires a second
/// factor of is recognized only by a cookie issued after one. Any other cookie
/// of theirs is withdrawn and the request stays anonymous: a cookie is not a
/// way around the code.
public final class RememberMeAuthenticationFilter implements SecurityFilter {
    private final RememberMeServices services;
    private final SecurityContextRepository repository;
    private final SessionAuthentication sessionAuthentication;
    private final SecondFactorPolicy secondFactor;

    RememberMeAuthenticationFilter(RememberMeServices services,
                                   SecurityContextRepository repository,
                                   SessionAuthentication sessionAuthentication,
                                   SecondFactorPolicy secondFactor) {
        this.services = services;
        this.repository = repository;
        this.sessionAuthentication = sessionAuthentication;
        this.secondFactor = secondFactor;
    }

    /// Whether the chain's second factor stands between this cookie and the
    /// user it names.
    private boolean held(Authentication remembered) {
        if (secondFactor == null || !secondFactor.requires(remembered)) {
            return false;
        }
        return !(remembered instanceof RememberMeAuthenticationToken)
                || !((RememberMeAuthenticationToken) remembered).isAfterSecondFactor();
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityContext current = SecurityContextHolder.peek();
        Authentication existing = current == null ? null : current.getAuthentication();
        if (existing == null || existing instanceof AnonymousAuthenticationToken) {
            Authentication remembered = services.autoLogin(request);
            if (remembered != null && held(remembered)) {
                // The cookie stood for a password and nothing else.
                services.loginFail(request);
                remembered = null;
            }
            if (remembered != null) {
                // A new session id for the signed-in state, as for any sign-in.
                sessionAuthentication.onAuthentication(request);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(remembered);
                SecurityContextHolder.setContext(context);
                repository.saveContext(context, request);
            }
        }
        return chain.doFilter(request);
    }
}
