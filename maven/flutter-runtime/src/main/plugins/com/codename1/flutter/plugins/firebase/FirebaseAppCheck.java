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

import com.codename1.io.Log;
import com.codename1.ui.Display;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;

/**
 * firebase_app_check. Codename One has no device attestation (Play Integrity,
 * DeviceCheck, App Attest, reCAPTCHA), so {@code activate} accepts every provider and
 * applies none of them; the debug provider is what yields a token. {@code getToken}
 * answers the Display property {@code firebase.appCheck.debugToken} -- a debug token
 * registered for the app in the Firebase console -- and null, with a logged warning,
 * when it is unset (and always headless). {@code onTokenChange} reports the token when
 * {@code activate} first finds one and when a forced refresh reads a different one.
 * {@code setTokenAutoRefreshEnabled} is recorded; there is no expiring token to refresh.
 */
public class FirebaseAppCheck {
    /** {@code FirebaseAppCheck.instance}. */
    public static final FirebaseAppCheck instance = new FirebaseAppCheck();

    /** The Display property the debug token is read from. */
    public static final String DEBUG_TOKEN_PROPERTY = "firebase.appCheck.debugToken";

    private final StreamController<String> tokens = StreamController.broadcast();
    private boolean activated;
    private boolean autoRefresh = true;
    private boolean warned;
    private String last;

    FirebaseAppCheck() {
    }

    public static FirebaseAppCheck instanceFor(Object app) {
        return instance;
    }

    public Future<Object> activate(Object webProvider, Object providerWeb, AndroidProvider androidProvider,
            AppleProvider appleProvider, Object providerAndroid, Object providerApple) {
        activated = true;
        String t = debugToken();
        if (t != null && !t.equals(last)) {
            last = t;
            tokens.add(t);
        }
        return Future.value(null);
    }

    /** Whether activate has run. */
    public boolean isActivated() {
        return activated;
    }

    public Future<String> getToken(Boolean forceRefresh) {
        String t = debugToken();
        if (t == null) {
            // Log needs a running implementation, so headless the warning is skipped.
            if (!warned && Display.isInitialized()) {
                warned = true;
                Log.p("firebase_app_check: no attestation provider on this platform; set the Display property "
                        + DEBUG_TOKEN_PROPERTY + " to a registered debug token");
            }
            return Future.value(null);
        }
        if (Boolean.TRUE.equals(forceRefresh) && !t.equals(last)) {
            last = t;
            tokens.add(t);
        }
        return Future.value(t);
    }

    public Future<String> getLimitedUseToken() {
        String t = debugToken();
        if (t == null) {
            return Future.error(new FirebaseException("app-check", "no-token",
                    "No App Check token: set " + DEBUG_TOKEN_PROPERTY));
        }
        return Future.value(t);
    }

    public Future<Object> setTokenAutoRefreshEnabled(boolean enabled) {
        autoRefresh = enabled;
        return Future.value(null);
    }

    public boolean isTokenAutoRefreshEnabled() {
        return autoRefresh;
    }

    public Stream<String> onTokenChange() {
        return tokens.stream();
    }

    private static String debugToken() {
        if (!Display.isInitialized()) {
            return null;
        }
        String t = Display.getInstance().getProperty(DEBUG_TOKEN_PROPERTY, null);
        return t == null || t.length() == 0 ? null : t;
    }
}
