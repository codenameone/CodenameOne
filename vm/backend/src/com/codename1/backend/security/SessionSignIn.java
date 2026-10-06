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
import java.util.List;

/// How a chain signs a user in to a session, once some mechanism has
/// established who they are. One per chain, shared by every mechanism of it,
/// so that each ends the same way:
///
/// 1. the chain's [SecondFactorPolicy], if it has one, may hold the sign-in
///    back and answer the request itself;
/// 2. the session id changes and the CSRF token is replaced;
/// 3. the authentication becomes the request's and is saved for later ones;
/// 4. what follows a sign-in is told -- remember-me issues its cookie;
/// 5. the mechanism's [AuthenticationSuccessHandler] answers.
///
/// A sign-in filter calls [#success] with what its [AuthenticationManager]
/// returned, and [#failure] when it was refused. What finishes a held-back
/// sign-in -- the filter that takes the one-time code -- calls [#complete].
public final class SessionSignIn {
    /// What is told of a sign-in besides the chain itself.
    interface Listener {
        /// Whether this request asks for what the listener does: to be
        /// remembered. Asked at the first factor, answered back at the end.
        boolean requested(HttpServer.Request request);

        void success(HttpServer.Request request, Authentication authentication, boolean requested);

        void failure(HttpServer.Request request);
    }

    private final SecurityContextRepository repository;
    private final SessionAuthentication sessionAuthentication;
    private final List<Listener> listeners;
    private final SecondFactorPolicy secondFactor;

    SessionSignIn(SecurityContextRepository repository, SessionAuthentication sessionAuthentication,
                  List<Listener> listeners, SecondFactorPolicy secondFactor) {
        this.repository = repository;
        this.sessionAuthentication = sessionAuthentication;
        this.listeners = listeners;
        this.secondFactor = secondFactor;
    }

    /// A mechanism accepted `authentication`: signs the user in, unless the
    /// chain asks for a second factor first.
    ///
    /// @return the answer to the request
    public HttpServer.Response success(HttpServer.Request request, Authentication authentication,
                                       AuthenticationSuccessHandler handler) throws Exception {
        boolean remember = false;
        for (Listener listener : listeners) {
            remember |= listener.requested(request);
        }
        if (secondFactor != null) {
            HttpServer.Response held = secondFactor.intercept(request, authentication, remember);
            if (held != null) {
                // Not signed in yet, and the request must not look as if it were.
                SecurityContextHolder.clearContext();
                return held;
            }
        }
        return complete(request, authentication, remember, handler);
    }

    /// Signs the user in, with no second factor asked: for the first factor of
    /// a chain without one, and for whatever finishes a sign-in that was held
    /// back.
    ///
    /// @param rememberMe what [SecondFactorPolicy#intercept] was told
    public HttpServer.Response complete(HttpServer.Request request, Authentication authentication,
                                        boolean rememberMe, AuthenticationSuccessHandler handler)
            throws Exception {
        sessionAuthentication.onAuthentication(request);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request);
        for (Listener listener : listeners) {
            listener.success(request, authentication, rememberMe);
        }
        return handler.onAuthenticationSuccess(request, authentication);
    }

    /// A mechanism refused the credentials it was given.
    public void failure(HttpServer.Request request) {
        SecurityContextHolder.clearContext();
        for (Listener listener : listeners) {
            listener.failure(request);
        }
    }
}
