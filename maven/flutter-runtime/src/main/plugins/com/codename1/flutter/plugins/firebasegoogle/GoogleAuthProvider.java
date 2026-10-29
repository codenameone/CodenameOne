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
package com.codename1.flutter.plugins.firebasegoogle;

import com.codename1.flutter.plugins.firebase.FederatedSignIn;
import com.codename1.flutter.plugins.firebase.FirebaseApp;
import com.codename1.flutter.plugins.firebase.FirebaseAuthException;
import com.codename1.flutter.plugins.firebase.FirebaseOptions;
import com.codename1.flutter.plugins.firebase.OAuthCredential;
import com.codename1.flutter.plugins.googlesignin.GoogleSignIn;
import com.codename1.flutter.plugins.googlesignin.GoogleSignInAccount;
import com.codename1.flutter.plugins.googlesignin.GoogleSignInAuthentication;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartList;
import dart.runtime.Funcs;

import java.util.Map;

/**
 * firebase_auth's {@code GoogleAuthProvider}. {@code credential} wraps tokens from
 * google_sign_in; passed to {@code signInWithProvider} it runs the Google sign-in of the
 * googlesignin pack itself, with the client id of the Firebase app's options for this
 * platform ({@code iosClientId} on iOS, {@code androidClientId} elsewhere) unless the
 * {@code google.clientId} Display property names one. A pack of its own, so that only an
 * app that signs in with Google ships Google sign-in.
 */
public class GoogleAuthProvider implements FederatedSignIn {
    public static final String PROVIDER_ID = "google.com";
    public static final String GOOGLE_SIGN_IN_METHOD = "google.com";

    private final DartList<String> scopes = new DartList<String>();

    public static OAuthCredential credential(String idToken, String accessToken) {
        return new OAuthCredential(PROVIDER_ID, GOOGLE_SIGN_IN_METHOD, idToken, accessToken, null, null);
    }

    public String providerId() {
        return PROVIDER_ID;
    }

    public DartList<String> scopes() {
        return scopes;
    }

    public GoogleAuthProvider addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    /** Accepted and not applied: the OIDC request carries no extra parameters. */
    public GoogleAuthProvider setCustomParameters(Map<Object, Object> parameters) {
        return this;
    }

    @Override
    public Future<OAuthCredential> obtainCredential(FirebaseApp app) {
        final Completer<OAuthCredential> done = new Completer<OAuthCredential>();
        GoogleSignIn g = new GoogleSignIn();
        g.scopes(scopes);
        g.clientId(clientId(app));
        g.signIn().then(new Funcs.VoidFunc1<GoogleSignInAccount>() {
            @Override
            public void call(GoogleSignInAccount a) {
                if (a == null) {
                    done.completeError(new FirebaseAuthException("web-context-cancelled",
                            "The Google sign-in was cancelled"));
                    return;
                }
                a.authentication().then(new Funcs.VoidFunc1<GoogleSignInAuthentication>() {
                    @Override
                    public void call(GoogleSignInAuthentication t) {
                        done.complete(credential(t.idToken(), t.accessToken()));
                    }
                });
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object err) {
                done.completeError(err);
            }
        });
        return done.future();
    }

    private static String clientId(FirebaseApp app) {
        if (Display.isInitialized()) {
            String p = Display.getInstance().getProperty("google.clientId", null);
            if (p != null) {
                return p;
            }
        }
        FirebaseOptions o = app == null ? null : app.options();
        if (o == null) {
            return null;
        }
        boolean ios = Display.isInitialized() && "ios".equals(Display.getInstance().getPlatformName());
        String id = ios ? o.iosClientId() : o.androidClientId();
        return id != null ? id : (ios ? o.androidClientId() : o.iosClientId());
    }
}
