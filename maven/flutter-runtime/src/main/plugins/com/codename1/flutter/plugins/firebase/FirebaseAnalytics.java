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

import java.util.Map;

import com.codename1.analytics.AnalyticsService;
import com.codename1.io.Log;

import dart.async.Future;

/**
 * firebase_analytics. Screen views go to Codename One's {@link AnalyticsService}
 * (when the app has initialized one); every event is also written to the log, so an
 * app that only logs analytics keeps running unchanged.
 */
public class FirebaseAnalytics {
    /** {@code FirebaseAnalytics.instance}. */
    public static final FirebaseAnalytics instance = new FirebaseAnalytics();

    private boolean enabled = true;
    private String userId;

    FirebaseAnalytics() {
    }

    public Future<Object> logEvent(String name, Map<String, Object> parameters, Object callOptions) {
        if (enabled) {
            Log.p("[analytics] " + name + (parameters == null ? "" : " " + parameters));
        }
        return Future.value(null);
    }

    public Future<Object> logScreenView(String screenClass, String screenName, Map<String, Object> parameters,
            Object callOptions) {
        String page = screenName != null ? screenName : screenClass;
        if (enabled && page != null && AnalyticsService.isEnabled()) {
            AnalyticsService.visit(page, null);
        }
        return logEvent("screen_view", parameters, null);
    }

    public Future<Object> logLogin(String loginMethod, Map<String, Object> parameters, Object callOptions) {
        return logEvent("login", parameters, null);
    }

    public Future<Object> logSignUp(String signUpMethod, Map<String, Object> parameters, Object callOptions) {
        return logEvent("sign_up", parameters, null);
    }

    public Future<Object> logAppOpen(Map<String, Object> parameters, Object callOptions) {
        return logEvent("app_open", parameters, null);
    }

    public Future<Object> setUserId(String id, Object callOptions) {
        userId = id;
        return Future.value(null);
    }

    public String userId() {
        return userId;
    }

    public Future<Object> setUserProperty(String name, String value, Object callOptions) {
        return Future.value(null);
    }

    public Future<Object> setAnalyticsCollectionEnabled(boolean on) {
        enabled = on;
        return Future.value(null);
    }

    public Future<Object> setCurrentScreen(String screenName, String screenClassOverride, Object callOptions) {
        return logScreenView(screenClassOverride, screenName, null, null);
    }
}
