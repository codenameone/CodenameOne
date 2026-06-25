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

import com.codename1.io.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A provider that simply logs every call (and records them in memory) instead
/// of sending data anywhere. It is the recommended default in the simulator and
/// is used as the backing provider in unit tests, where {@link #getLog()} lets a
/// test assert exactly which calls were made.
public class LoggingAnalyticsProvider extends AbstractAnalyticsProvider {
    private final List<String> log = new ArrayList<String>();

    @Override
    public String getName() {
        return "logging";
    }

    /// The in-memory record of calls received by this provider, in order. Each
    /// entry is a short description such as {@code "screen:Home"} or
    /// {@code "event:purchase"}.
    ///
    /// #### Returns
    ///
    /// the recorded call log
    public List<String> getLog() {
        return log;
    }

    /// Clears the recorded call log.
    public void clearLog() {
        log.clear();
    }

    @Override
    public void trackScreen(String name, String referrer) {
        record("screen:" + name + (referrer == null ? "" : " <- " + referrer));
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        StringBuilder b = new StringBuilder("event:");
        b.append(event.getName());
        Map<String, Object> params = event.getParameters();
        if (!params.isEmpty()) {
            b.append(' ').append(params.toString());
        }
        record(b.toString());
    }

    @Override
    public void setUserId(String id) {
        record("userId:" + id);
    }

    @Override
    public void setUserProperty(String key, String value) {
        record("userProperty:" + key + "=" + value);
    }

    @Override
    public void reportCrash(AnalyticsCrashReport report) {
        record("crash:" + report.getMessage() + " fatal=" + report.isFatal());
    }

    @Override
    public void onConsentChanged(AnalyticsConsent consent) {
        record("consent:analytics=" + consent.isAnalytics()
                + " crash=" + consent.isCrashReporting());
    }

    @Override
    public void flush() {
        record("flush");
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return true;
    }

    private void record(String entry) {
        log.add(entry);
        Log.p("[analytics] " + entry);
    }
}
