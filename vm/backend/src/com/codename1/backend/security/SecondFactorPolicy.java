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

/// Stands between a user passing their first factor and being signed in.
///
/// Every way of signing in that ends in a session -- the form login, and a
/// sign-in through another identity provider -- hands the authentication it
/// established to the chain's [SessionSignIn] rather than storing it. When the
/// chain has a policy, the policy is asked first, and may take the request
/// over: `http.mfa(...)` installs the one that asks for a one-time code.
///
/// A policy that takes a request over must leave it anonymous. Nothing has
/// been stored for the user at this point -- no security context, no changed
/// session id -- and the policy keeps only what it needs to finish later, by
/// calling [SessionSignIn#complete] once the second factor is in.
public interface SecondFactorPolicy {
    /// Decides whether `authentication`, whose first factor has just been
    /// accepted, must present a second.
    ///
    /// @param rememberMe whether the user asked to be remembered: to be handed
    /// back to [SessionSignIn#complete], since the request that finishes the
    /// sign-in is not the one that asked
    /// @return null to let the sign-in complete now; otherwise the answer to
    /// this request -- a redirect to where the second factor is asked for --
    /// with the sign-in left pending
    HttpServer.Response intercept(HttpServer.Request request, Authentication authentication,
                                  boolean rememberMe) throws Exception;
}
