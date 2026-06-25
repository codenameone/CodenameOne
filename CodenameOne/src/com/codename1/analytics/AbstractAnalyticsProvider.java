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

/// Convenience base class for {@link AnalyticsProvider} implementations. Every
/// SPI method has an empty / no-op body so a concrete provider only overrides
/// the calls it actually supports. The {@link AnalyticsContext} handed to
/// {@link #init(AnalyticsContext)} is retained and exposed via
/// {@link #getContext()}; {@link #supports(AnalyticsCapability)} returns
/// {@code false} for every capability and should be overridden.
public abstract class AbstractAnalyticsProvider implements AnalyticsProvider {
    private AnalyticsContext context;

    @Override
    public abstract String getName();

    @Override
    public void init(AnalyticsContext context) {
        this.context = context;
    }

    /// The context supplied at {@link #init(AnalyticsContext)} time.
    ///
    /// #### Returns
    ///
    /// the context, or null if not yet initialised
    protected AnalyticsContext getContext() {
        return context;
    }

    @Override
    public void trackScreen(String name, String referrer) {
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
    }

    @Override
    public void setUserId(String id) {
    }

    @Override
    public void setUserProperty(String key, String value) {
    }

    @Override
    public void reportCrash(AnalyticsCrashReport report) {
    }

    @Override
    public void onConsentChanged(AnalyticsConsent consent) {
    }

    @Override
    public void flush() {
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return false;
    }
}
