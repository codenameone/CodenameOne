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

import com.codename1.io.oidc.OidcTokens;

import dart.async.Future;
import dart.core.DartMap;

import java.util.Map;

/** google_sign_in's {@code GoogleSignInAccount}, read from the ID token's claims. */
public class GoogleSignInAccount {
    private final GoogleSignIn owner;
    private final String id;
    private final String email;
    private final String displayName;
    private final String photoUrl;
    private final GoogleSignInAuthentication auth;

    GoogleSignInAccount(GoogleSignIn owner, OidcTokens t) {
        this(owner, t.getSubject(), t.getEmail(), claim(t, "name"), claim(t, "picture"),
                new GoogleSignInAuthentication(t.getIdToken(), t.getAccessToken()));
    }

    GoogleSignInAccount(GoogleSignIn owner, String id, String email, String displayName, String photoUrl,
            GoogleSignInAuthentication auth) {
        this.owner = owner;
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.auth = auth;
    }

    public String id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String photoUrl() {
        return photoUrl;
    }

    /** Always null: the PKCE flow exchanges the code on the device itself. */
    public String serverAuthCode() {
        return null;
    }

    public Future<GoogleSignInAuthentication> authentication() {
        return Future.value(auth);
    }

    public Future<Map<String, String>> authHeaders() {
        DartMap<String, String> h = new DartMap<String, String>();
        if (auth.accessToken() != null) {
            h.put("Authorization", "Bearer " + auth.accessToken());
            h.put("X-Goog-AuthUser", "0");
        }
        return Future.<Map<String, String>>value(h);
    }

    public Future<Object> clearAuthCache() {
        return Future.value(null);
    }

    @Override
    public String toString() {
        return "GoogleSignInAccount:{displayName: " + displayName + ", email: " + email + ", id: " + id
                + ", photoUrl: " + photoUrl + "}";
    }

    private static String claim(OidcTokens t, String name) {
        Map<String, Object> c = t.getIdTokenClaims();
        Object v = c == null ? null : c.get(name);
        return v instanceof String ? (String) v : null;
    }
}
