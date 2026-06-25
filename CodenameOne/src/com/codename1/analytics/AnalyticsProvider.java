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

/// The analytics service provider interface (SPI). Implement this to plug an
/// analytics backend into the {@link Analytics} facade. Several providers can
/// be registered at once; the facade fans every call out to all of them after
/// the relevant consent has been satisfied.
///
/// Providers are registered by explicit instantiation
/// (`Analytics.addProvider(new MyProvider())`) -- no reflection or service
/// lookup is used, which keeps the mechanism safe under the obfuscation applied
/// to shipped builds. Most implementations should extend
/// {@link AbstractAnalyticsProvider} and override only the calls they support.
public interface AnalyticsProvider {
    /// A short, stable, human readable name for this provider (used in logs and
    /// diagnostics).
    ///
    /// #### Returns
    ///
    /// the provider name
    String getName();

    /// Called once when the provider is registered with the facade, supplying
    /// ambient application context.
    ///
    /// #### Parameters
    ///
    /// - `context`: the analytics context
    void init(AnalyticsContext context);

    /// Records a screen / page view.
    ///
    /// #### Parameters
    ///
    /// - `name`: the screen name
    ///
    /// - `referrer`: the previous screen name, may be null
    void trackScreen(String name, String referrer);

    /// Records a named event.
    ///
    /// #### Parameters
    ///
    /// - `event`: the event
    void trackEvent(AnalyticsEvent event);

    /// Associates subsequent activity with a user identifier.
    ///
    /// #### Parameters
    ///
    /// - `id`: the user id, or null to clear
    void setUserId(String id);

    /// Sets a user-level property / custom dimension.
    ///
    /// #### Parameters
    ///
    /// - `key`: the property name
    ///
    /// - `value`: the property value
    void setUserProperty(String key, String value);

    /// Reports a crash or handled exception.
    ///
    /// #### Parameters
    ///
    /// - `report`: the crash report
    void reportCrash(AnalyticsCrashReport report);

    /// Notifies the provider that the user's consent has changed so it can
    /// enable, disable or reconfigure collection accordingly.
    ///
    /// #### Parameters
    ///
    /// - `consent`: the new consent state
    void onConsentChanged(AnalyticsConsent consent);

    /// Flushes any buffered events to the backend.
    void flush();

    /// Indicates whether the provider supports a given capability.
    ///
    /// #### Parameters
    ///
    /// - `capability`: the capability to query
    ///
    /// #### Returns
    ///
    /// true if supported
    boolean supports(AnalyticsCapability capability);
}
