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

/**
 * Apple does not ship a native Sign-in-with-Apple SDK for Android; the
 * supported flow on Android is Apple's web-based authorization endpoint
 * (the same one the Services ID is configured for). This implementation
 * reports {@link #isSupported()} = {@code false} so that the Java-side
 * {@link AppleSignIn} class falls back to its {@code OidcClient} +
 * {@code SystemBrowser} path -- which on Android resolves to a Custom Tab
 * over the Apple `https://appleid.apple.com/auth/authorize` endpoint.
 *
 * <p>The class exists chiefly to make the {@code NativeLookup} probe in
 * {@link AppleSignIn#lookupNative()} non-null so we explicitly answer the
 * "is this platform native?" question instead of falling through to a
 * {@code ClassNotFoundException} swallowed deep inside {@code NativeLookup}.
 */
public class AppleSignInNativeImpl implements AppleSignInNative {

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public String signIn(String scopes, String nonce) {
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public void signOut() {
        // No-op: the OidcClient-backed AppleSignIn fallback drives its own
        // token cache.
    }
}
