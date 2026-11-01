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
package com.codename1.flutter.plugins.googlesignin;

import com.codename1.flutter.platform.PlatformException;
import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.social.GoogleConnect;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.DartList;

/**
 * google_sign_in's {@code GoogleSignIn} (the version 6 API), over
 * {@link GoogleConnect#signIn(String, String, String...)}: Google's OIDC endpoints with
 * PKCE in the system browser, on every platform.
 *
 * <p>The client id is the {@code clientId} argument, else the Display property
 * {@code google.clientId}. The redirect is the Display property {@code google.redirectUri},
 * else the reversed client id scheme Google registers for an iOS or Android client
 * ({@code com.googleusercontent.apps.<id>:/oauth2redirect}).
 *
 * <p>Google issues no refresh token to a public client, so {@code signInSilently} answers
 * the account of this run's last sign-in, and null after a restart; it never shows a
 * browser. {@code hostedDomain}, {@code signInOption} and {@code forceCodeForRefreshToken}
 * are accepted and not applied. A cancelled sign-in answers null, as the plugin does.
 */
public class GoogleSignIn {
    private static final StreamController<GoogleSignInAccount> CHANGES = StreamController.broadcast();
    private static GoogleSignInAccount current;

    private final java.util.LinkedHashSet<String> scopes = new java.util.LinkedHashSet<String>();
    private String clientId;
    private String serverClientId;

    public GoogleSignIn() {
        scopes.add("openid");
        scopes.add("email");
        scopes.add("profile");
    }

    public void scopes(DartList<String> v) {
        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                scopes.add(v.get(i));
            }
        }
    }

    public void clientId(String v) {
        clientId = v;
    }

    public void serverClientId(String v) {
        serverClientId = v;
    }

    public void signInOption(Object v) {
    }

    public void hostedDomain(String v) {
    }

    public void forceCodeForRefreshToken(Boolean v) {
    }

    /** The scopes a sign-in asks for, {@code openid email profile} first. */
    public DartList<String> scopes() {
        DartList<String> l = new DartList<String>();
        for (String s : scopes) {
            l.add(s);
        }
        return l;
    }

    public GoogleSignInAccount currentUser() {
        return current;
    }

    public Stream<GoogleSignInAccount> onCurrentUserChanged() {
        return CHANGES.stream();
    }

    public Future<GoogleSignInAccount> signIn() {
        final Completer<GoogleSignInAccount> done = new Completer<GoogleSignInAccount>();
        final String id = clientId();
        if (id == null) {
            done.completeError(new PlatformException("sign_in_failed",
                    "No Google client id: pass clientId or set the google.clientId Display property", null));
            return done.future();
        }
        if (!Display.isInitialized()) {
            done.completeError(new PlatformException("sign_in_failed", "Google sign-in needs a display", null));
            return done.future();
        }
        GoogleConnect.getInstance().signIn(id, redirectUri(id), scopes.toArray(new String[scopes.size()])).ready(new SuccessCallback<OidcTokens>() {
            @Override
            public void onSucess(OidcTokens t) {
                done.complete(changeTo(new GoogleSignInAccount(GoogleSignIn.this, t)));
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                if (err instanceof OidcException
                        && OidcException.USER_CANCELLED.equals(((OidcException) err).getError())) {
                    done.complete(null);
                } else {
                    done.completeError(new PlatformException("sign_in_failed", err.getMessage(), null));
                }
            }
        });
        return done.future();
    }

    public Future<GoogleSignInAccount> signInSilently(Boolean suppressErrors, Boolean reAuthenticate) {
        return Future.value(current);
    }

    public Future<Boolean> isSignedIn() {
        return Future.value(Boolean.valueOf(current != null));
    }

    public Future<GoogleSignInAccount> signOut() {
        changeTo(null);
        return Future.value(null);
    }

    /** Google's token revocation would need the access token sent back; it is dropped instead. */
    public Future<GoogleSignInAccount> disconnect() {
        return signOut();
    }

    public Future<Boolean> requestScopes(DartList<String> scopes) {
        return Future.value(Boolean.valueOf(current != null));
    }

    public Future<Boolean> canAccessScopes(DartList<String> scopes, String accessToken) {
        return Future.value(Boolean.valueOf(current != null));
    }

    String serverClientId() {
        return serverClientId;
    }

    private String clientId() {
        if (clientId != null) {
            return clientId;
        }
        return Display.isInitialized() ? Display.getInstance().getProperty("google.clientId", null) : null;
    }

    private static String redirectUri(String id) {
        String r = Display.getInstance().getProperty("google.redirectUri", null);
        if (r != null) {
            return r;
        }
        String suffix = ".apps.googleusercontent.com";
        String prefix = id.endsWith(suffix) ? id.substring(0, id.length() - suffix.length()) : id;
        return "com.googleusercontent.apps." + prefix + ":/oauth2redirect";
    }

    private static GoogleSignInAccount changeTo(GoogleSignInAccount a) {
        current = a;
        if (CHANGES.hasListener()) {
            CHANGES.add(a);
        }
        return a;
    }
}
