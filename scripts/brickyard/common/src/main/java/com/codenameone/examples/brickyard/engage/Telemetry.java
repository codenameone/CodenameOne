/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.engage;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.AnalyticsProvider;
import com.codename1.analytics.CodenameOneAnalyticsProvider;
import com.codename1.analytics.LoggingAnalyticsProvider;
import com.codename1.ui.CN;

/// Analytics, through `com.codename1.analytics`. Nothing is sent until the
/// player has said yes (Settings), and then only the events named here.
public final class Telemetry {
    private static final String ACCOUNT = "account";
    private static final String PLAY = "play";
    private static final String ECONOMY = "economy";
    private static boolean started;
    private static String screen;

    private Telemetry() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Analytics.addProvider(CN.isSimulator() ? new LoggingAnalyticsProvider()
                : new CodenameOneAnalyticsProvider());
    }

    /// Replaces the providers; for tests.
    public static void use(AnalyticsProvider... providers) {
        started = true;
        Analytics.clearProviders();
        for (AnalyticsProvider p : providers) {
            Analytics.addProvider(p);
        }
        screen = null;
    }

    public static boolean allowed() {
        AnalyticsConsent c = Analytics.getConsent();
        return c != null && c.isAnalytics();
    }

    public static void allow(boolean yes) {
        Analytics.setConsent(AnalyticsConsent.builder()
                .analytics(yes)
                .crashReporting(yes)
                .personalization(false)
                .adStorage(false)
                .build());
    }

    public static void flush() {
        Analytics.flush();
    }

    public static void screen(String name) {
        if (name.equals(screen) || !allowed()) {
            return;
        }
        Analytics.screen(name, screen);
        screen = name;
    }

    public static void registered() {
        send(ACCOUNT, "sign_up", "method", "password");
    }

    public static void signedIn() {
        send(ACCOUNT, "login", "method", "password");
    }

    public static void played(String experienceId) {
        send(PLAY, "experience_play", "experience", experienceId);
    }

    public static void purchase(String sku) {
        send(ECONOMY, "purchase", "sku", sku);
    }

    public static void adWatched(String placement) {
        send(ECONOMY, "ad_reward", "placement", placement);
    }

    public static void inviteSent(String experienceId) {
        send(ACCOUNT, "share", "content", "invite", "experience", experienceId);
    }

    public static void inviteAccepted() {
        send(ACCOUNT, "invite_accepted");
    }

    public static void pushOpened(String campaign) {
        send(ACCOUNT, "push_open", "campaign", campaign);
    }

    private static void send(String category, String name, Object... pairs) {
        if (!allowed()) {
            return;
        }
        AnalyticsEvent.Builder event = AnalyticsEvent.create(name).category(category);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            event.param(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        Analytics.event(event.build());
    }
}
