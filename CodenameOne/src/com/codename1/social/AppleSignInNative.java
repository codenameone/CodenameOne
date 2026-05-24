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

/// Service-provider interface for native `Sign in with Apple`. The iOS port
/// supplies a class `com.codename1.social.AppleSignInNativeImpl` that wraps
/// `ASAuthorizationAppleIDProvider`; [AppleSignIn] loads it via
/// `Class.forName` at first use. Cn1libs that want to plug in their own
/// implementation can register one with [AppleSignIn#setNative(AppleSignInNative)]
/// -- this interface does not extend
/// [com.codename1.system.NativeInterface] because [AppleSignIn] is part of
/// the core framework and the iOS impl talks to native code via
/// `IOSImplementation.nativeInstance`, not through `NativeLookup`.
///
/// The native side serialises its result as a single pipe-delimited string
/// to keep the bridge boundary primitive-only:
///
/// `{idToken}|{authorizationCode}|{user}|{givenName}|{familyName}|{email}`
///
/// `null` segments are sent as empty strings. `user` is the stable opaque
/// identifier Apple returns; `givenName` / `familyName` / `email` are only
/// populated on the **first** authorization (Apple does not re-send the
/// profile on subsequent logins). The Java side persists them.
///
/// @since 7.0.245
public interface AppleSignInNative {

    /// `true` if this implementation is usable on the current device / OS
    /// version. iOS 13+ returns `true`; older iOS, non-iOS platforms, or
    /// missing entitlement returns `false` so [AppleSignIn] falls back to
    /// its web OIDC flow.
    boolean isSupported();

    /// Starts the system Sign-in-with-Apple sheet. The call blocks the
    /// calling thread until the user completes or cancels.
    ///
    /// #### Parameters
    ///
    /// - `scopes`: Space-separated scope list (e.g. `"name email"`).
    /// - `nonce`: SHA-256 hash of the per-request nonce, base64url encoded.
    ///   Apple binds this to the returned ID token's `nonce` claim.
    String signIn(String scopes, String nonce);

    /// Returns `true` if the user is currently signed in (i.e. the previously
    /// returned credential is still valid in the Apple keychain).
    boolean isLoggedIn();

    /// Clears the current Apple credential from the app's keychain entry.
    /// Apple does not provide an explicit sign-out -- this only removes the
    /// local credential association so the next [#signIn(String, String)]
    /// will prompt again.
    void signOut();
}
