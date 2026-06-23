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
import com.codename1.io.Preferences;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/// The application-facing entry point for analytics. {@code Analytics} holds the
/// set of registered {@link AnalyticsProvider providers}, the user's consent
/// state and the pseudonymous client id, and fans every reporting call out to
/// all providers -- but only once the relevant consent has been satisfied.
///
/// ### Typical setup
///
/// ```java
/// // register one or more providers (no reflection -- explicit instances)
/// Analytics.addProvider(new CodenameOneAnalyticsProvider());
/// Analytics.addProvider(new GoogleAnalyticsProvider("G-XXXX", "api-secret"));
///
/// // GDPR: nothing is sent until the user grants consent (opt-in is the default)
/// Analytics.setConsent(AnalyticsConsent.granted());
///
/// Analytics.screen("Home", null);
/// Analytics.event(AnalyticsEvent.create("purchase").param("value", 9.99).build());
/// ```
///
/// ### Consent and privacy
///
/// The {@link ConsentMode} governs behaviour before an explicit choice is
/// recorded. In the default {@link ConsentMode#OPT_IN} mode reporting calls are
/// silently dropped until {@link #setConsent(AnalyticsConsent)} grants the
/// matching category. The consent choice and the client id are persisted in
/// {@link Preferences} so they survive restarts. The client id is not derived
/// from any hardware identifier and can be cleared with {@link #resetClientId()}
/// to honour an erasure request.
public final class Analytics {
    private static final String PREF_CLIENT_ID = "cn1$analyticsClientId";
    private static final String PREF_CONSENT_SET = "cn1$analyticsConsentSet";
    private static final String PREF_CONSENT_ANALYTICS = "cn1$analyticsConsentAnalytics";
    private static final String PREF_CONSENT_CRASH = "cn1$analyticsConsentCrash";
    private static final String PREF_CONSENT_PERSONALIZATION = "cn1$analyticsConsentPersonalization";
    private static final String PREF_CONSENT_AD = "cn1$analyticsConsentAdStorage";
    private static final String PREF_DIMENSIONS = "cn1$analyticsDimensions";

    private static final Object LOCK = new Object();
    private static final List<AnalyticsProvider> PROVIDERS = new ArrayList<AnalyticsProvider>();
    // App-scoped segmentation dimensions ("plan", "role", ...) that the cloud
    // console segments by. Insertion-ordered, guarded by LOCK, loaded lazily
    // from Preferences and persisted on every mutation so they survive restarts.
    private static final Map<String, String> DIMENSIONS = new LinkedHashMap<String, String>();
    private static boolean dimensionsLoaded;
    private static ConsentMode consentMode = ConsentMode.OPT_IN;
    private static AnalyticsConsent consent;
    private static boolean consentLoaded;
    private static String clientId;

    private Analytics() {
    }

    /// Registers a provider and immediately supplies it with the current
    /// {@link AnalyticsContext} and consent state.
    ///
    /// #### Parameters
    ///
    /// - `provider`: the provider to add, ignored if null
    public static void addProvider(AnalyticsProvider provider) {
        if (provider == null) {
            return;
        }
        AnalyticsContext ctx = context();
        synchronized (LOCK) {
            PROVIDERS.add(provider);
        }
        AnalyticsConsent c = currentConsent();
        try {
            provider.init(ctx);
            provider.onConsentChanged(c == null ? AnalyticsConsent.denied() : c);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Removes a previously registered provider.
    ///
    /// #### Parameters
    ///
    /// - `provider`: the provider to remove
    public static void removeProvider(AnalyticsProvider provider) {
        synchronized (LOCK) {
            PROVIDERS.remove(provider);
        }
    }

    /// Removes every registered provider.
    public static void clearProviders() {
        synchronized (LOCK) {
            PROVIDERS.clear();
        }
    }

    /// The currently registered providers.
    ///
    /// #### Returns
    ///
    /// an immutable copy of the provider list
    public static List<AnalyticsProvider> getProviders() {
        synchronized (LOCK) {
            return new ArrayList<AnalyticsProvider>(PROVIDERS);
        }
    }

    /// Sets the consent mode that governs behaviour before an explicit consent
    /// choice is recorded. Defaults to {@link ConsentMode#OPT_IN}.
    ///
    /// #### Parameters
    ///
    /// - `mode`: the consent mode, ignored if null
    public static void setConsentMode(ConsentMode mode) {
        if (mode == null) {
            return;
        }
        synchronized (LOCK) {
            consentMode = mode;
        }
    }

    /// The active consent mode.
    ///
    /// #### Returns
    ///
    /// the consent mode
    public static ConsentMode getConsentMode() {
        synchronized (LOCK) {
            return consentMode;
        }
    }

    /// Records the user's consent choice, persists it and notifies every
    /// provider. Passing null clears the stored choice (reverting to the
    /// implicit behaviour of the current {@link ConsentMode}).
    ///
    /// #### Parameters
    ///
    /// - `newConsent`: the consent state, or null to clear
    public static void setConsent(AnalyticsConsent newConsent) {
        List<AnalyticsProvider> snapshot;
        synchronized (LOCK) {
            consent = newConsent;
            consentLoaded = true;
            if (newConsent == null) {
                Preferences.set(PREF_CONSENT_SET, false);
            } else {
                Preferences.set(PREF_CONSENT_SET, true);
                Preferences.set(PREF_CONSENT_ANALYTICS, newConsent.isAnalytics());
                Preferences.set(PREF_CONSENT_CRASH, newConsent.isCrashReporting());
                Preferences.set(PREF_CONSENT_PERSONALIZATION, newConsent.isPersonalization());
                Preferences.set(PREF_CONSENT_AD, newConsent.isAdStorage());
            }
            snapshot = new ArrayList<AnalyticsProvider>(PROVIDERS);
        }
        AnalyticsConsent effective = newConsent == null ? AnalyticsConsent.denied() : newConsent;
        for (AnalyticsProvider p : snapshot) {
            try {
                p.onConsentChanged(effective);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// The currently recorded consent, loading it from {@link Preferences} on
    /// first access. Returns null if the user has not made an explicit choice.
    ///
    /// #### Returns
    ///
    /// the consent state or null
    public static AnalyticsConsent getConsent() {
        return currentConsent();
    }

    /// Records a screen / page view across all providers. No-op unless the
    /// analytics consent category is satisfied.
    ///
    /// #### Parameters
    ///
    /// - `name`: the screen name
    ///
    /// - `referrer`: the previous screen, may be null
    public static void screen(String name, String referrer) {
        if (!analyticsAllowed()) {
            return;
        }
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.trackScreen(name, referrer);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Records a named event across all providers. No-op unless the analytics
    /// consent category is satisfied.
    ///
    /// #### Parameters
    ///
    /// - `event`: the event
    public static void event(AnalyticsEvent event) {
        if (event == null || !analyticsAllowed()) {
            return;
        }
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.trackEvent(event);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Internal hook used by the framework to auto-instrument built-in APIs
    /// (purchases, sharing, ...). It builds an {@link AnalyticsEvent} and routes
    /// it through the normal consent-gated {@link #event(AnalyticsEvent)} path,
    /// so it is automatically a no-op when consent has not been granted. As a
    /// fast path it does nothing at all when no provider is registered, which
    /// keeps the cost of the framework call sites negligible when analytics is
    /// not in use. Never throws into the caller.
    ///
    /// #### Parameters
    ///
    /// - `name`: the event name
    ///
    /// - `category`: the event category, may be null
    ///
    /// - `params`: optional event parameters, may be null
    ///
    /// #### Deprecated
    ///
    /// Internal framework auto-instrumentation hook; not intended for app use.
    public static void autoEvent(String name, String category, Map<String, Object> params) {
        try {
            synchronized (LOCK) {
                if (PROVIDERS.isEmpty()) {
                    return;
                }
            }
            AnalyticsEvent.Builder builder = AnalyticsEvent.create(name);
            if (category != null) {
                builder.category(category);
            }
            if (params != null) {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    builder.param(e.getKey(), e.getValue());
                }
            }
            event(builder.build());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Associates subsequent activity with a user id. No-op unless the
    /// personalization consent category is satisfied.
    ///
    /// #### Parameters
    ///
    /// - `id`: the user id, or null to clear
    public static void setUserId(String id) {
        if (!personalizationAllowed()) {
            return;
        }
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.setUserId(id);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Sets a user property / custom dimension. No-op unless the analytics
    /// consent category is satisfied.
    ///
    /// #### Parameters
    ///
    /// - `key`: the property name
    ///
    /// - `value`: the property value
    public static void setUserProperty(String key, String value) {
        if (!analyticsAllowed()) {
            return;
        }
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.setUserProperty(key, value);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Sets an app-scoped custom dimension used by the cloud console to segment
    /// reports (for example {@code "plan"} or {@code "role"}). Dimensions are
    /// persisted in {@link Preferences} so they survive restarts and are sent
    /// with every first-party batch. Passing a null value removes the key.
    /// Null or empty keys are ignored.
    ///
    /// #### Parameters
    ///
    /// - `key`: the dimension key
    ///
    /// - `value`: the dimension value, or null to remove the key
    public static void setDimension(String key, String value) {
        if (key == null || key.length() == 0) {
            return;
        }
        synchronized (LOCK) {
            loadDimensions();
            if (value == null) {
                DIMENSIONS.remove(key);
            } else {
                DIMENSIONS.put(key, value);
            }
            persistDimensions();
        }
    }

    /// Removes a single custom dimension.
    ///
    /// #### Parameters
    ///
    /// - `key`: the dimension key
    public static void clearDimension(String key) {
        setDimension(key, null);
    }

    /// Removes every custom dimension.
    public static void clearDimensions() {
        synchronized (LOCK) {
            loadDimensions();
            DIMENSIONS.clear();
            persistDimensions();
        }
    }

    /// The current custom dimensions.
    ///
    /// #### Returns
    ///
    /// a defensive, insertion-ordered copy of the dimension map
    public static Map<String, String> getDimensions() {
        synchronized (LOCK) {
            loadDimensions();
            return new LinkedHashMap<String, String>(DIMENSIONS);
        }
    }

    /// Reports a crash / exception to the registered analytics providers as an
    /// exception signal -- for example GA4's {@code app_exception} or the
    /// Firebase equivalent -- so exception counts surface alongside your usage
    /// metrics. No-op unless the crash reporting consent category is satisfied.
    ///
    /// This is deliberately separate from Codename One Crash Protection
    /// ({@link com.codename1.crash.CrashProtection}). Crash Protection is a
    /// dedicated crash-capture pipeline that records full, symbolicated stack
    /// traces to the build cloud crash console for debugging. This method only
    /// emits a lightweight exception event to whichever analytics backends you
    /// have registered. The two subsystems are independent: use Crash
    /// Protection to diagnose crashes, use {@code Analytics.crash} when you also
    /// want exception events in your analytics; you can enable either or both.
    ///
    /// #### Parameters
    ///
    /// - `throwable`: the captured exception, may be null
    ///
    /// - `message`: a human readable description, may be null
    ///
    /// - `fatal`: whether the exception terminated the application
    public static void crash(Throwable throwable, String message, boolean fatal) {
        crash(AnalyticsCrashReport.create(throwable, message, fatal));
    }

    /// Reports a crash / exception across all providers. No-op unless the crash
    /// reporting consent category is satisfied.
    ///
    /// #### Parameters
    ///
    /// - `report`: the crash report
    public static void crash(AnalyticsCrashReport report) {
        if (report == null || !crashAllowed()) {
            return;
        }
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.reportCrash(report);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Flushes any buffered events in every provider.
    public static void flush() {
        for (AnalyticsProvider p : snapshot()) {
            try {
                p.flush();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// The pseudonymous client id, generating and persisting one on first use.
    /// Not derived from any hardware identifier.
    ///
    /// #### Returns
    ///
    /// the client id
    public static String clientId() {
        synchronized (LOCK) {
            if (clientId == null) {
                String stored = Preferences.get(PREF_CLIENT_ID, "");
                if (stored == null || stored.length() == 0) {
                    clientId = newClientId();
                    Preferences.set(PREF_CLIENT_ID, clientId);
                } else {
                    clientId = stored;
                }
            }
            return clientId;
        }
    }

    /// Generates a fresh pseudonymous client id, persists it and re-initialises
    /// every provider with the new identity. Use this to honour a "right to be
    /// forgotten" / erasure request from the user.
    ///
    /// #### Returns
    ///
    /// the new client id
    public static String resetClientId() {
        List<AnalyticsProvider> snapshot;
        synchronized (LOCK) {
            clientId = newClientId();
            Preferences.set(PREF_CLIENT_ID, clientId);
            snapshot = new ArrayList<AnalyticsProvider>(PROVIDERS);
        }
        AnalyticsContext ctx = context();
        for (AnalyticsProvider p : snapshot) {
            try {
                p.init(ctx);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        return clientId;
    }

    // Must be called while holding LOCK. Lazily loads the persisted dimensions
    // from a tab/newline delimited string: rows are newline separated, key and
    // value within a row are tab separated. Values had tabs/newlines replaced
    // with spaces on the way in, so the parse is unambiguous.
    private static void loadDimensions() {
        if (dimensionsLoaded) {
            return;
        }
        dimensionsLoaded = true;
        String stored = Preferences.get(PREF_DIMENSIONS, "");
        if (stored == null || stored.length() == 0) {
            return;
        }
        String[] rows = split(stored, '\n');
        for (String row : rows) {
            if (row.length() == 0) {
                continue;
            }
            int tab = row.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String key = row.substring(0, tab);
            String value = row.substring(tab + 1);
            if (key.length() > 0) {
                DIMENSIONS.put(key, value);
            }
        }
    }

    // Must be called while holding LOCK.
    private static void persistDimensions() {
        if (DIMENSIONS.isEmpty()) {
            Preferences.set(PREF_DIMENSIONS, "");
            return;
        }
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : DIMENSIONS.entrySet()) {
            if (!first) {
                b.append('\n');
            }
            b.append(sanitize(e.getKey())).append('\t').append(sanitize(e.getValue()));
            first = false;
        }
        Preferences.set(PREF_DIMENSIONS, b.toString());
    }

    // Replaces the delimiter characters so the persisted form parses back
    // unambiguously. Dimension keys/values are simple labels, so this is safe.
    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    // CLDC-safe split (String.split is not available); never returns null.
    private static String[] split(String value, char delim) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        int idx = value.indexOf(delim);
        while (idx >= 0) {
            out.add(value.substring(start, idx));
            start = idx + 1;
            idx = value.indexOf(delim, start);
        }
        out.add(value.substring(start));
        return out.toArray(new String[out.size()]);
    }

    private static List<AnalyticsProvider> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<AnalyticsProvider>(PROVIDERS);
        }
    }

    private static AnalyticsConsent currentConsent() {
        synchronized (LOCK) {
            if (!consentLoaded) {
                consentLoaded = true;
                if (Preferences.get(PREF_CONSENT_SET, false)) {
                    consent = new AnalyticsConsent(
                            Preferences.get(PREF_CONSENT_ANALYTICS, false),
                            Preferences.get(PREF_CONSENT_CRASH, false),
                            Preferences.get(PREF_CONSENT_PERSONALIZATION, false),
                            Preferences.get(PREF_CONSENT_AD, false));
                }
            }
            return consent;
        }
    }

    private static boolean analyticsAllowed() {
        AnalyticsConsent c = currentConsent();
        if (getConsentMode() == ConsentMode.OPT_OUT) {
            return c == null || c.isAnalytics();
        }
        return c != null && c.isAnalytics();
    }

    private static boolean crashAllowed() {
        AnalyticsConsent c = currentConsent();
        if (getConsentMode() == ConsentMode.OPT_OUT) {
            return c == null || c.isCrashReporting();
        }
        return c != null && c.isCrashReporting();
    }

    private static boolean personalizationAllowed() {
        AnalyticsConsent c = currentConsent();
        if (getConsentMode() == ConsentMode.OPT_OUT) {
            return c == null || c.isPersonalization();
        }
        return c != null && c.isPersonalization();
    }

    private static AnalyticsContext context() {
        Display d = Display.getInstance();
        String appName = "";
        String appVersion = "1.0";
        String platform = "";
        if (d != null) {
            appName = d.getProperty("AppName", "");
            appVersion = d.getProperty("AppVersion", "1.0");
            platform = d.getPlatformName();
        }
        Locale loc = Locale.getDefault();
        String locale = loc == null ? "" : loc.toString();
        return new AnalyticsContext(appName, appVersion, clientId(), locale, platform);
    }

    private static String newClientId() {
        Random r = new Random();
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder b = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            b.append(hex[r.nextInt(16)]);
        }
        return b.toString();
    }
}
