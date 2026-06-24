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

/// Immutable bundle of ambient information shared with every provider through
/// {@link AnalyticsProvider#init(AnalyticsContext)}. The {@link Analytics}
/// facade assembles it once from {@code Display} properties and the
/// pseudonymous client id so providers do not each have to re-derive it.
public final class AnalyticsContext {
    private final String appName;
    private final String appVersion;
    private final String clientId;
    private final String locale;
    private final String platform;

    AnalyticsContext(String appName, String appVersion, String clientId,
            String locale, String platform) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.clientId = clientId;
        this.locale = locale;
        this.platform = platform;
    }

    /// The user-facing application name.
    ///
    /// #### Returns
    ///
    /// the app name
    public String getAppName() {
        return appName;
    }

    /// The application version string.
    ///
    /// #### Returns
    ///
    /// the app version
    public String getAppVersion() {
        return appVersion;
    }

    /// The pseudonymous, user-resettable client id. This is not derived from
    /// any hardware identifier and may be cleared via
    /// {@link Analytics#resetClientId()} to honour erasure requests.
    ///
    /// #### Returns
    ///
    /// the client id
    public String getClientId() {
        return clientId;
    }

    /// The device locale (for example {@code en_US}).
    ///
    /// #### Returns
    ///
    /// the locale
    public String getLocale() {
        return locale;
    }

    /// The platform name as reported by {@code Display.getPlatformName()}
    /// (for example {@code and}, {@code ios}, {@code mac}).
    ///
    /// #### Returns
    ///
    /// the platform name
    public String getPlatform() {
        return platform;
    }
}
