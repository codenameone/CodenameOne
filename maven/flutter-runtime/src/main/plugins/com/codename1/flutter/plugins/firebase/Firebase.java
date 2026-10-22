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
import dart.core.DartList;

/**
 * firebase_core's {@code Firebase}. Initialization holds the options: Firebase Auth talks
 * to Google's REST API with their {@code apiKey}, the other services need nothing more.
 */
public final class Firebase {
    public static final String DEFAULT_APP_NAME = "[DEFAULT]";

    /** {@code Firebase.apps}. */
    public static final DartList<FirebaseApp> apps = new DartList<FirebaseApp>();

    private Firebase() {
    }

    public static Future<FirebaseApp> initializeApp(String name, FirebaseOptions options) {
        String n = name == null ? DEFAULT_APP_NAME : name;
        FirebaseApp existing = find(n);
        if (existing != null) {
            return Future.value(existing);
        }
        FirebaseApp app = new FirebaseApp(n, options);
        apps.add(app);
        return Future.value(app);
    }

    public static FirebaseApp app(String name) {
        String n = name == null ? DEFAULT_APP_NAME : name;
        FirebaseApp a = find(n);
        if (a == null) {
            throw new FirebaseException("core", "no-app",
                    "No Firebase App '" + n + "' has been created - call Firebase.initializeApp()");
        }
        return a;
    }

    static FirebaseApp find(String name) {
        for (FirebaseApp a : apps) {
            if (a.name().equals(name)) {
                return a;
            }
        }
        return null;
    }
}
