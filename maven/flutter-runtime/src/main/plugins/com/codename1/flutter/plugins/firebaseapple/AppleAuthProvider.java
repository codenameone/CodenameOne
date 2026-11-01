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
package com.codename1.flutter.plugins.firebaseapple;

import com.codename1.flutter.plugins.firebase.FederatedSignIn;
import com.codename1.flutter.plugins.firebase.FirebaseApp;
import com.codename1.flutter.plugins.firebase.FirebaseAuthException;
import com.codename1.flutter.plugins.firebase.OAuthCredential;
import com.codename1.social.AppleSignIn;
import com.codename1.social.AppleSignInCallback;
import com.codename1.social.AppleSignInResult;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartList;

import java.util.Map;

/**
 * firebase_auth's {@code AppleAuthProvider}. Passed to {@code signInWithProvider} it runs
 * {@link AppleSignIn}: the native sheet on iOS, which hands back the raw nonce Firebase
 * checks the ID token's hashed one against, and Apple's web flow elsewhere. The web flow's
 * token carries OIDC's own nonce rather than a hash of one, so there is no raw nonce to
 * send and a Firebase project that enforces it refuses the exchange. A pack of its own,
 * so that only an app that signs in with Apple carries the Sign in with Apple entitlement.
 */
public class AppleAuthProvider implements FederatedSignIn {
    public static final String PROVIDER_ID = "apple.com";
    public static final String APPLE_SIGN_IN_METHOD = "apple.com";

    private final DartList<String> scopes = new DartList<String>();

    public static OAuthCredential credentialWithIDToken(String idToken, String rawNonce, Object fullName) {
        return new OAuthCredential(PROVIDER_ID, APPLE_SIGN_IN_METHOD, idToken, null, rawNonce, null);
    }

    public String providerId() {
        return PROVIDER_ID;
    }

    public DartList<String> scopes() {
        return scopes;
    }

    public AppleAuthProvider addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    public AppleAuthProvider setCustomParameters(Map<Object, Object> parameters) {
        return this;
    }

    @Override
    public Future<OAuthCredential> obtainCredential(FirebaseApp app) {
        final Completer<OAuthCredential> done = new Completer<OAuthCredential>();
        if (!Display.isInitialized()) {
            done.completeError(new FirebaseAuthException("operation-not-allowed",
                    "Sign in with Apple needs a display"));
            return done.future();
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < scopes.size(); i++) {
            if (s.length() > 0) {
                s.append(' ');
            }
            s.append(scopes.get(i));
        }
        AppleSignIn.getInstance().signIn(s.length() == 0 ? null : s.toString(), new AppleSignInCallback() {
            @Override
            public void onSuccess(AppleSignInResult r) {
                done.complete(credentialWithIDToken(r.getIdentityToken(), r.getRawNonce(), null));
            }

            @Override
            public void onError(String error) {
                done.completeError(new FirebaseAuthException("invalid-credential", error));
            }

            @Override
            public void onCancel() {
                done.completeError(new FirebaseAuthException("web-context-cancelled",
                        "Sign in with Apple was cancelled"));
            }
        });
        return done.future();
    }
}
