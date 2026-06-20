/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.analytics;

import com.codename1.system.NativeLookup;

import java.util.Map;

/// A provider that forwards analytics to the native Firebase Analytics
/// SDK through the {@link NativeFirebaseAnalytics} native interface. On
/// Android and iOS, with Firebase configured in the build, events flow
/// to the Firebase console. Where no native peer exists (the simulator,
/// or a build without Firebase set up) the provider degrades silently to
/// a no-op, so it is always safe to register.
///
/// ```java
/// Analytics.addProvider(new FirebaseAnalyticsProvider());
/// ```
///
/// Firebase requires the usual platform configuration:
/// `google-services.json` (Android) / `GoogleService-Info.plist` (iOS)
/// plus the Firebase dependencies in the generated native project.
public class FirebaseAnalyticsProvider extends AbstractAnalyticsProvider {
    private NativeFirebaseAnalytics peer;
    private boolean available;

    @Override
    public String getName() {
        return "firebase";
    }

    @Override
    public void init(AnalyticsContext context) {
        super.init(context);
        try {
            peer = NativeLookup.create(NativeFirebaseAnalytics.class);
            available = peer != null && peer.isSupported();
        } catch (Throwable t) {
            available = false;
        }
    }

    @Override
    public void trackScreen(String name, String referrer) {
        if (available) {
            peer.logScreen(name);
        }
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        if (available) {
            peer.logEvent(event.getName(), paramsJson(event.getParameters()));
        }
    }

    @Override
    public void setUserId(String id) {
        if (available) {
            peer.setUserId(id);
        }
    }

    @Override
    public void setUserProperty(String key, String value) {
        if (available) {
            peer.setUserProperty(key, value);
        }
    }

    @Override
    public void reportCrash(AnalyticsCrashReport report) {
        if (!available) {
            return;
        }
        String description = report.getMessage();
        if ((description == null || description.length() == 0) && report.getThrowable() != null) {
            description = report.getThrowable().getClass().getName();
        }
        StringBuilder b = new StringBuilder();
        b.append('{');
        AnalyticsJson.appendString(b, "description", description == null ? "" : description, true);
        AnalyticsJson.appendBoolean(b, "fatal", report.isFatal(), false);
        b.append('}');
        peer.logEvent("app_exception", b.toString());
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return capability == AnalyticsCapability.SCREEN_VIEWS
                || capability == AnalyticsCapability.EVENTS
                || capability == AnalyticsCapability.USER_PROPERTIES
                || capability == AnalyticsCapability.CRASH_REPORTING;
    }

    private static String paramsJson(Map<String, Object> params) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        if (params != null) {
            boolean first = true;
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (!first) {
                    b.append(',');
                }
                b.append('"');
                AnalyticsJson.escape(b, e.getKey());
                b.append("\":");
                AnalyticsJson.appendValue(b, e.getValue());
                first = false;
            }
        }
        b.append('}');
        return b.toString();
    }
}
