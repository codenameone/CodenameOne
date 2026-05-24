/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.social;

/// Successful outcome of an [AppleSignIn#signIn(String, AppleSignInCallback)]
/// call.
///
/// Apple only returns the user's name and email on the **first** authorization
/// for a given app. On subsequent sign-ins those fields are absent in the
/// native callback; [AppleSignIn] backfills them from [com.codename1.io.Preferences]
/// when present, so the application sees a consistent result.
///
/// @since 7.1
public final class AppleSignInResult {

    String identityToken;
    String authorizationCode;
    String userId;
    String email;
    String fullName;

    AppleSignInResult() {}

    /// JWT identity token signed by Apple. Send to your backend, where you
    /// must validate the signature against Apple's JWKS and check the
    /// `aud` / `iss` / `exp` claims before trusting it.
    public String getIdentityToken() {
        return identityToken;
    }

    /// Authorization code suitable for the server-side `client_secret`
    /// token exchange (Apple does not expose refresh tokens to public
    /// clients, so this is the only way to obtain one).
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    /// Stable opaque identifier ("user identifier" in Apple's docs). Treat
    /// this as the user's primary key for your app.
    public String getUserId() {
        return userId;
    }

    /// Email the user shared with the app. May be the real address, may be
    /// a relay address (`@privaterelay.appleid.com`), or may be `null` if
    /// the user has previously signed in and the email was already stored.
    public String getEmail() {
        return email;
    }

    /// Full display name (given + family) on the first authorization;
    /// previously-stored value otherwise; `null` if the user declined to
    /// share it.
    public String getFullName() {
        return fullName;
    }
}
