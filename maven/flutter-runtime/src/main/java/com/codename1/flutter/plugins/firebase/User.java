/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import dart.async.Future;

/** firebase_auth's {@code User}, from the Identity Toolkit's account record. */
public class User {
    final String uid;
    String email;
    String displayName;
    String photoURL;
    final boolean anonymous;
    boolean emailVerified;
    String idToken;
    String refreshToken;
    long expiresAt;

    User(String uid, String email, boolean anonymous) {
        this.uid = uid;
        this.email = email;
        this.anonymous = anonymous;
    }

    public String uid() {
        return uid;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String photoURL() {
        return photoURL;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public String phoneNumber() {
        return null;
    }

    public String refreshToken() {
        return refreshToken;
    }

    /** The ID token, refreshed first when it has expired or {@code forceRefresh} asks. */
    public Future<String> getIdToken(Boolean forceRefresh) {
        return FirebaseAuth.instance.idToken(this, forceRefresh != null && forceRefresh.booleanValue());
    }

    public Future<Object> updateDisplayName(String name) {
        return FirebaseAuth.instance.updateProfile(this, name, photoURL);
    }

    public Future<Object> updatePhotoURL(String url) {
        return FirebaseAuth.instance.updateProfile(this, displayName, url);
    }

    /** Re-reads the account (accounts:lookup), so a verified email shows as verified. */
    public Future<Object> reload() {
        return FirebaseAuth.instance.reloadUser(this);
    }

    public Future<Object> delete() {
        return FirebaseAuth.instance.deleteUser(this);
    }

    public Future<Object> sendEmailVerification(Object actionCodeSettings) {
        return FirebaseAuth.instance.sendOob(this, "VERIFY_EMAIL", null);
    }

    @Override
    public String toString() {
        return "User(uid: " + uid + ", email: " + email + ")";
    }
}
