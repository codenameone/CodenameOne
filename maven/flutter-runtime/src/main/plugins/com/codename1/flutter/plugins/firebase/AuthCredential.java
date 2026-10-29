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

/**
 * firebase_auth's {@code AuthCredential}: what {@code signInWithCredential} exchanges
 * for a Firebase session. A federated one ({@link OAuthCredential}) carries the
 * identity provider's tokens; an email one ({@link EmailAuthProvider#credential})
 * the address and password.
 */
public class AuthCredential {
    final String providerId;
    final String signInMethod;
    String email;
    String password;

    AuthCredential(String providerId, String signInMethod) {
        this.providerId = providerId;
        this.signInMethod = signInMethod;
    }

    public String providerId() {
        return providerId;
    }

    public String signInMethod() {
        return signInMethod;
    }

    /** Always null: Firebase keys a credential by its tokens, not a number. */
    public Long token() {
        return null;
    }

    public String accessToken() {
        return null;
    }

    @Override
    public String toString() {
        return "AuthCredential(providerId: " + providerId + ", signInMethod: " + signInMethod + ")";
    }
}
