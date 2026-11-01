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

import dart.async.Future;

/**
 * A provider {@code signInWithProvider} can run interactively: it shows the identity
 * provider's own sign-in (Google's, Apple's sheet) and answers the credential to
 * exchange. The providers that do this live in the firebaseoauth pack, so an app that
 * signs in with a password never ships a reference to Google or Apple sign-in, and the
 * builders never add their SDKs and entitlements to it.
 */
public interface FederatedSignIn {
    /** Runs the provider's sign-in for {@code app}; a cancelled one fails the future. */
    Future<OAuthCredential> obtainCredential(FirebaseApp app);
}
