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

import com.codename1.system.NativeInterface;

/// Native bridge for {@link FirebaseAnalyticsProvider}. Each platform
/// supplies an implementation that forwards to the native Firebase
/// Analytics SDK:
///
/// - **Android**: `FirebaseAnalytics.getInstance(context).logEvent(...)`
///   / `setUserId` / `setUserProperty` (requires `google-services.json`
///   and the Firebase Gradle plugin in the build).
/// - **iOS**: `FIRAnalytics logEventWithName:parameters:` /
///   `setUserID:` / `setUserProperty:forName:` (requires
///   `GoogleService-Info.plist` and the Firebase pods).
///
/// When no native peer is present (for example in the simulator, or in a
/// build without Firebase configured), {@code NativeLookup.create}
/// returns {@code null} / {@link #isSupported()} returns false and the
/// provider degrades to a no-op. Parameters are passed as a JSON object
/// string so the native side can map them to the SDK's bundle / NSDictionary.
public interface NativeFirebaseAnalytics extends NativeInterface {
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
