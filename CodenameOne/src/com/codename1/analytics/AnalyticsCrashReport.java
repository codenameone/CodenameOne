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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// A crash / exception report routed to providers that advertise
/// {@link AnalyticsCapability#CRASH_REPORTING}. This is the analytics
/// abstraction for exception telemetry; it is distinct from the dedicated
/// on-device crash-protection client in {@code com.codename1.crash}.
public final class AnalyticsCrashReport {
    private final Throwable throwable;
    private final String message;
    private final boolean fatal;
    private final Map<String, String> customKeys;

    AnalyticsCrashReport(Throwable throwable, String message, boolean fatal,
            Map<String, String> customKeys) {
        this.throwable = throwable;
        this.message = message;
        this.fatal = fatal;
        this.customKeys = Collections.unmodifiableMap(customKeys);
    }

    /// Creates a crash report.
    ///
    /// #### Parameters
    ///
    /// - `throwable`: the captured exception, may be null
    ///
    /// - `message`: a human readable description, may be null
    ///
    /// - `fatal`: true if the exception terminated the application
    ///
    /// #### Returns
    ///
    /// a new crash report
    public static AnalyticsCrashReport create(Throwable throwable, String message, boolean fatal) {
        return new AnalyticsCrashReport(throwable, message, fatal, new LinkedHashMap<String, String>());
    }

    /// The captured throwable.
    ///
    /// #### Returns
    ///
    /// the throwable or null
    public Throwable getThrowable() {
        return throwable;
    }

    /// The descriptive message.
    ///
    /// #### Returns
    ///
    /// the message or null
    public String getMessage() {
        return message;
    }

    /// Whether the exception was fatal.
    ///
    /// #### Returns
    ///
    /// true if fatal
    public boolean isFatal() {
        return fatal;
    }

    /// Optional custom key/value context attached to the report.
    ///
    /// #### Returns
    ///
    /// an unmodifiable map of custom keys
    public Map<String, String> getCustomKeys() {
        return customKeys;
    }

    /// Returns a builder for adding custom keys to a base report.
    ///
    /// #### Returns
    ///
    /// a builder seeded from this report
    public Builder asBuilder() {
        Builder b = new Builder(throwable, message, fatal);
        b.customKeys.putAll(customKeys);
        return b;
    }

    /// Mutable builder for {@link AnalyticsCrashReport}.
    public static final class Builder {
        private final Throwable throwable;
        private final String message;
        private final boolean fatal;
        private final Map<String, String> customKeys = new LinkedHashMap<String, String>();

        Builder(Throwable throwable, String message, boolean fatal) {
            this.throwable = throwable;
            this.message = message;
            this.fatal = fatal;
        }

        /// Attaches a custom key/value pair to the report.
        ///
        /// #### Parameters
        ///
        /// - `key`: the key
        ///
        /// - `value`: the value
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder customKey(String key, String value) {
            customKeys.put(key, value);
            return this;
        }

        /// Builds the immutable report.
        ///
        /// #### Returns
        ///
        /// a new {@link AnalyticsCrashReport}
        public AnalyticsCrashReport build() {
            return new AnalyticsCrashReport(throwable, message, fatal, customKeys);
        }
    }
}
