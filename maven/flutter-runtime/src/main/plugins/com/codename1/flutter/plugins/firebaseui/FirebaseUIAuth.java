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
package com.codename1.flutter.plugins.firebaseui;

import com.codename1.flutter.plugins.firebase.FirebaseApp;

import dart.core.DartList;

/** firebase_ui_auth's {@code FirebaseUIAuth}: the providers a screen offers when it names none. */
public final class FirebaseUIAuth {
    private static DartList<Object> providers = new DartList<Object>();

    private FirebaseUIAuth() {
    }

    public static void configureProviders(DartList<Object> list, FirebaseApp app) {
        providers = list == null ? new DartList<Object>() : list;
    }

    public static DartList<Object> providersFor(FirebaseApp app) {
        return providers;
    }

    static DartList<Object> orConfigured(DartList<Object> list) {
        return list == null || list.isEmpty() ? providers : list;
    }
}
