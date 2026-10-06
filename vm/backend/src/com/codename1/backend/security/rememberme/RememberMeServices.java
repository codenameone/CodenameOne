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
package com.codename1.backend.security.rememberme;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;

/// What remembers a user between sessions: issues the cookie at sign-in, and
/// recognizes it on a request nobody has signed in for.
public interface RememberMeServices {
    /// The attribute of the request's
    /// [com.codename1.backend.security.SecurityExchange] that says the user
    /// asked to be remembered, when the request that completes the sign-in is
    /// not the one that carried the checkbox: set to `Boolean.TRUE` before
    /// [#loginSuccess] by whatever finishes a sign-in in a second step.
    String REQUESTED_ATTRIBUTE = "com.codename1.backend.security.rememberMe.requested";

    /// The attribute of the request's
    /// [com.codename1.backend.security.SecurityExchange] that says the sign-in
    /// being completed passed a second factor: set to `Boolean.TRUE` before
    /// [#loginSuccess]. A cookie issued then may sign a user who has a second
    /// factor in later; one issued without it may not. Services that issue
    /// cookies of their own carry this over, and say so on what [#autoLogin]
    /// returns -- see
    /// [com.codename1.backend.security.RememberMeAuthenticationToken#isAfterSecondFactor].
    String SECOND_FACTOR_ATTRIBUTE = "com.codename1.backend.security.rememberMe.secondFactor";

    /// Recognizes the user from the request's cookie.
    ///
    /// @return who it is, or null when the request carries no cookie this
    /// accepts -- in which case the cookie, if there was one, is withdrawn
    Authentication autoLogin(HttpServer.Request request);

    /// A sign-in was refused: withdraws the cookie.
    void loginFail(HttpServer.Request request);

    /// A user signed in: issues the cookie, if they asked to be remembered.
    /// Public so that a sign-in an application completes itself -- after a
    /// step of its own -- can issue it too.
    void loginSuccess(HttpServer.Request request, Authentication successfulAuthentication);
}
