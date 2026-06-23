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

import java.util.Map;

/// A provider that forwards analytics to the native Firebase Analytics SDK.
/// On Android and iOS, with Firebase configured in the build, events flow to
/// the Firebase console. Where no native implementation is wired (the
/// simulator, the desktop build, or a build without Firebase set up) the
/// provider degrades silently to a no-op, so it is always safe to register:
///
/// ```java
/// Analytics.addProvider(new FirebaseAnalyticsProvider());
/// ```
///
/// ### How the native call is wired
///
/// Unlike a {@code NativeInterface} (which exists for the build server to
/// generate per-app native peers), this provider talks to the platform through
/// a small {@link Bridge}. The Codename One build supplies the concrete bridge
/// for the target and registers it via {@link #registerBridge(Bridge)} before
/// the app starts:
///
/// - **Android**: a bridge that calls
///   `FirebaseAnalytics.getInstance(context).logEvent(...)` /
///   `setUserId` / `setUserProperty` directly. Requires
///   `google-services.json` and the Firebase Gradle plugin in the build.
/// - **iOS**: a bridge whose methods are declared `native` and implemented in
///   Objective-C (`FIRAnalytics`). Requires `GoogleService-Info.plist` and the
///   Firebase pods.
/// - **Everything else** (simulator, desktop): no bridge is registered, so the
///   provider is a no-op.
///
/// Firebase is enabled with the {@code codename1.arg.android.firebaseAnalytics}
/// / {@code codename1.arg.ios.firebaseAnalytics} build hints, which is what
/// makes the build register the bridge.
public class FirebaseAnalyticsProvider extends AbstractAnalyticsProvider {

    /// The platform's connection to the native Firebase SDK. The Codename One
    /// build implements this for Android (direct SDK calls) and iOS (native
    /// methods) and hands it to {@link #registerBridge(Bridge)}. Application
    /// code never implements this directly.
    public interface Bridge {
        /// Whether the native Firebase SDK is present and initialised.
        ///
        /// #### Returns
        ///
        /// true when events can be delivered
        boolean isSupported();

        /// Logs a named event with a JSON object of parameters.
        ///
        /// #### Parameters
        ///
        /// - `name`: the event name
        ///
        /// - `paramsJson`: a JSON object of parameters, may be empty
        void logEvent(String name, String paramsJson);

        /// Logs a screen view.
        ///
        /// #### Parameters
        ///
        /// - `screenName`: the screen name
        void logScreen(String screenName);

        /// Sets the Firebase user id.
        ///
        /// #### Parameters
        ///
        /// - `id`: the user id, or null to clear
        void setUserId(String id);

        /// Sets a Firebase user property.
        ///
        /// #### Parameters
        ///
        /// - `key`: the property name
        ///
        /// - `value`: the property value
        void setUserProperty(String key, String value);
    }

    private static Bridge registeredBridge;

    /// Registers the platform bridge. Called by the Codename One build for a
    /// Firebase-enabled Android / iOS target; never called by application code.
    /// Passing null clears the registration (the provider becomes a no-op).
    ///
    /// #### Parameters
    ///
    /// - `bridge`: the platform bridge, or null
    public static void registerBridge(Bridge bridge) {
        registeredBridge = bridge;
    }

    private Bridge bridge;
    private boolean available;

    @Override
    public String getName() {
        return "firebase";
    }

    @Override
    public void init(AnalyticsContext context) {
        super.init(context);
        bridge = registeredBridge;
        try {
            available = bridge != null && bridge.isSupported();
        } catch (Throwable t) {
            available = false;
        }
    }

    @Override
    public void trackScreen(String name, String referrer) {
        if (available) {
            bridge.logScreen(name);
        }
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        if (available) {
            bridge.logEvent(event.getName(), paramsJson(event.getParameters()));
        }
    }

    @Override
    public void setUserId(String id) {
        if (available) {
            bridge.setUserId(id);
        }
    }

    @Override
    public void setUserProperty(String key, String value) {
        if (available) {
            bridge.setUserProperty(key, value);
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
        bridge.logEvent("app_exception", b.toString());
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
