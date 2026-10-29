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
package com.codename1.flutter.plugins.firebaseuigoogle;

import com.codename1.flutter.plugins.firebase.FirebaseApp;
import com.codename1.flutter.plugins.firebase.OAuthCredential;
import com.codename1.flutter.plugins.firebasegoogle.GoogleAuthProvider;
import com.codename1.flutter.plugins.firebaseui.UiOAuthProvider;

import dart.async.Future;
import dart.core.DartList;

/**
 * firebase_ui_oauth_google's {@code GoogleProvider}: signs in through
 * {@link GoogleAuthProvider} with this provider's {@code clientId}. {@code redirectUri} is
 * the Display property {@code google.redirectUri}'s job here; {@code iOSPreferPlist} is
 * accepted and not applied.
 */
public class GoogleProvider implements UiOAuthProvider {
    private String clientId;
    private DartList<String> scopes;

    public void clientId(String v) {
        clientId = v;
    }

    public void redirectUri(String v) {
    }

    public void scopes(DartList<String> v) {
        scopes = v;
    }

    public void iOSPreferPlist(Boolean v) {
    }

    public String providerId() {
        return GoogleAuthProvider.PROVIDER_ID;
    }

    @Override
    public String label() {
        return "Sign in with Google";
    }

    @Override
    public Future<OAuthCredential> obtainCredential(FirebaseApp app) {
        GoogleAuthProvider p = new GoogleAuthProvider().clientId(clientId);
        if (scopes != null) {
            for (int i = 0; i < scopes.size(); i++) {
                p.addScope(scopes.get(i));
            }
        }
        return p.obtainCredential(app);
    }
}
