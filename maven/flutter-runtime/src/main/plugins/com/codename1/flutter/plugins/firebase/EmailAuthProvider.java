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
 * firebase_auth's {@code EmailAuthProvider}, and firebase_ui_auth's of the same name: an
 * instance in a {@code SignInScreen}'s providers asks for the email form.
 */
public final class EmailAuthProvider {
    public static final String PROVIDER_ID = "password";
    public static final String EMAIL_PASSWORD_SIGN_IN_METHOD = "password";

    public EmailAuthProvider() {
    }

    public static AuthCredential credential(String email, String password) {
        AuthCredential c = new AuthCredential(PROVIDER_ID, EMAIL_PASSWORD_SIGN_IN_METHOD);
        c.email = email;
        c.password = password;
        return c;
    }
}
