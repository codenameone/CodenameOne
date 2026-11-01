/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebase;

import dart.core.DartList;

import java.util.Map;

/**
 * firebase_auth's {@code OAuthProvider}: builds the credential of any provider Firebase
 * federates ({@code apple.com}, {@code microsoft.com}, ...) from tokens the app obtained
 * itself. It cannot run a provider's sign-in, so {@code signInWithProvider} refuses it;
 * Google's and Apple's interactive sign-in are {@code GoogleAuthProvider} and
 * {@code AppleAuthProvider}, in packs of their own.
 */
public class OAuthProvider {
    private final String providerId;
    private final DartList<String> scopes = new DartList<String>();

    public OAuthProvider(String providerId) {
        this.providerId = providerId;
    }

    public String providerId() {
        return providerId;
    }

    public DartList<String> scopes() {
        return scopes;
    }

    public OAuthProvider addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    public OAuthProvider setCustomParameters(Map<Object, Object> parameters) {
        return this;
    }

    public OAuthCredential credential(String accessToken, String secret, String idToken, String rawNonce,
            String signInMethod) {
        return new OAuthCredential(providerId, signInMethod, idToken, accessToken, rawNonce, secret);
    }

    @Override
    public String toString() {
        return "OAuthProvider(" + providerId + ")";
    }
}
