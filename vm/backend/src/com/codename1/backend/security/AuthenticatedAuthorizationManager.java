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

import java.util.function.Supplier;

/// Grants access by whether the caller signed in at all.
public final class AuthenticatedAuthorizationManager<T> implements AuthorizationManager<T> {
    private static final int AUTHENTICATED = 0;
    private static final int ANONYMOUS = 1;
    private static final int FULLY = 2;
    private static final int REMEMBERED = 3;
    private final int wanted;

    private AuthenticatedAuthorizationManager(int wanted) {
        this.wanted = wanted;
    }

    /// The caller signed in: any authentication but the anonymous one.
    public static <T> AuthenticatedAuthorizationManager<T> authenticated() {
        return new AuthenticatedAuthorizationManager<T>(AUTHENTICATED);
    }

    /// The caller did not sign in.
    public static <T> AuthenticatedAuthorizationManager<T> anonymous() {
        return new AuthenticatedAuthorizationManager<T>(ANONYMOUS);
    }

    /// The caller signed in during this session, rather than being recognized
    /// by a remember-me cookie.
    public static <T> AuthenticatedAuthorizationManager<T> fullyAuthenticated() {
        return new AuthenticatedAuthorizationManager<T>(FULLY);
    }

    /// The caller was recognized by a remember-me cookie.
    public static <T> AuthenticatedAuthorizationManager<T> rememberMe() {
        return new AuthenticatedAuthorizationManager<T>(REMEMBERED);
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, T object) {
        Authentication current = authentication.get();
        boolean anonymous = current == null || current instanceof AnonymousAuthenticationToken;
        if (wanted == ANONYMOUS) {
            return new AuthorizationDecision(anonymous);
        }
        boolean signedIn = !anonymous && current.isAuthenticated();
        boolean remembered = current instanceof RememberMeAuthenticationToken;
        if (wanted == REMEMBERED) {
            return new AuthorizationDecision(signedIn && remembered);
        }
        return new AuthorizationDecision(signedIn && !(wanted == FULLY && remembered));
    }
}
