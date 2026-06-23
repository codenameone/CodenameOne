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

/// An immutable snapshot of the consent the user has granted, broken down by
/// category so that an application can honour granular choices (for example
/// allowing crash reporting while declining behavioural analytics). Instances
/// are created through {@link #all()}, {@link #none()} or {@link #builder()}
/// and handed to {@link Analytics#setConsent(AnalyticsConsent)}. An application
/// that has already collected consent elsewhere can unblock everything in one
/// line: {@code Analytics.setConsent(AnalyticsConsent.all())}.
public final class AnalyticsConsent {
    private final boolean analytics;
    private final boolean crashReporting;
    private final boolean personalization;
    private final boolean adStorage;

    AnalyticsConsent(boolean analytics, boolean crashReporting,
            boolean personalization, boolean adStorage) {
        this.analytics = analytics;
        this.crashReporting = crashReporting;
        this.personalization = personalization;
        this.adStorage = adStorage;
    }

    /// Consent for screen views, events and user properties.
    ///
    /// #### Returns
    ///
    /// true if behavioural analytics collection is permitted
    public boolean isAnalytics() {
        return analytics;
    }

    /// Consent for crash and exception reporting.
    ///
    /// #### Returns
    ///
    /// true if crash reporting is permitted
    public boolean isCrashReporting() {
        return crashReporting;
    }

    /// Consent for personalization (e.g. user-level identification).
    ///
    /// #### Returns
    ///
    /// true if personalization is permitted
    public boolean isPersonalization() {
        return personalization;
    }

    /// Consent for advertising / ad storage.
    ///
    /// #### Returns
    ///
    /// true if ad storage is permitted
    public boolean isAdStorage() {
        return adStorage;
    }

    /// A consent object granting every category. Use this when the application
    /// has already obtained consent through its own mechanism (a custom prompt,
    /// a third-party consent-management platform, an enterprise MDM policy, or
    /// a jurisdiction where the integrator has determined consent is not
    /// required) and simply wants reporting to flow:
    ///
    /// ```java
    /// // app handled consent elsewhere -- unblock all categories at startup
    /// Analytics.setConsent(AnalyticsConsent.all());
    /// ```
    ///
    /// For apps that would rather not gate at all, see
    /// {@link Analytics#setConsentMode(ConsentMode)} with
    /// {@link ConsentMode#OPT_OUT}.
    ///
    /// #### Returns
    ///
    /// consent with all categories granted
    public static AnalyticsConsent all() {
        return new AnalyticsConsent(true, true, true, true);
    }

    /// A consent object denying every category. Equivalent to the implicit
    /// state before consent is recorded in {@link ConsentMode#OPT_IN}.
    ///
    /// #### Returns
    ///
    /// consent with all categories denied
    public static AnalyticsConsent none() {
        return new AnalyticsConsent(false, false, false, false);
    }

    /// Alias for {@link #all()}; granting every category.
    ///
    /// #### Returns
    ///
    /// consent with all categories granted
    public static AnalyticsConsent granted() {
        return all();
    }

    /// Alias for {@link #none()}; denying every category.
    ///
    /// #### Returns
    ///
    /// consent with all categories denied
    public static AnalyticsConsent denied() {
        return none();
    }

    /// Creates a builder seeded with all categories denied.
    ///
    /// #### Returns
    ///
    /// a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// Returns a builder seeded with this object's current values so a single
    /// category can be toggled without rebuilding from scratch.
    ///
    /// #### Returns
    ///
    /// a builder pre-populated from this instance
    public Builder asBuilder() {
        return new Builder()
                .analytics(analytics)
                .crashReporting(crashReporting)
                .personalization(personalization)
                .adStorage(adStorage);
    }

    /// Mutable builder for {@link AnalyticsConsent}.
    public static final class Builder {
        private boolean analytics;
        private boolean crashReporting;
        private boolean personalization;
        private boolean adStorage;

        /// Sets the analytics category.
        ///
        /// #### Parameters
        ///
        /// - `value`: true to grant behavioural analytics
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder analytics(boolean value) {
            this.analytics = value;
            return this;
        }

        /// Sets the crash reporting category.
        ///
        /// #### Parameters
        ///
        /// - `value`: true to grant crash reporting
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder crashReporting(boolean value) {
            this.crashReporting = value;
            return this;
        }

        /// Sets the personalization category.
        ///
        /// #### Parameters
        ///
        /// - `value`: true to grant personalization
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder personalization(boolean value) {
            this.personalization = value;
            return this;
        }

        /// Sets the ad storage category.
        ///
        /// #### Parameters
        ///
        /// - `value`: true to grant ad storage
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder adStorage(boolean value) {
            this.adStorage = value;
            return this;
        }

        /// Grants every category. Handy as a base before selectively
        /// revoking one, e.g. {@code builder().all().adStorage(false)}.
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder all() {
            this.analytics = true;
            this.crashReporting = true;
            this.personalization = true;
            this.adStorage = true;
            return this;
        }

        /// Denies every category.
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder none() {
            this.analytics = false;
            this.crashReporting = false;
            this.personalization = false;
            this.adStorage = false;
            return this;
        }

        /// Builds the immutable consent object.
        ///
        /// #### Returns
        ///
        /// a new {@link AnalyticsConsent}
        public AnalyticsConsent build() {
            return new AnalyticsConsent(analytics, crashReporting, personalization, adStorage);
        }
    }
}
