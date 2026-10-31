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
package com.codenameone.examples.dash.engage;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.AnalyticsProvider;
import com.codename1.analytics.CodenameOneAnalyticsProvider;
import com.codename1.analytics.LoggingAnalyticsProvider;
import com.codename1.ui.CN;

/// What the game reports about how it is played, through
/// `com.codename1.analytics`.
///
/// The events are a funnel a game designer reads top to bottom: installed,
/// signed up, first run, first race win, level published, first purchase.
/// Runs also report the level, how far the runner got and how it ended,
/// which is what tuning a level's difficulty needs. Nothing is sent until the player says yes on the
/// consent screen, and nothing that names them is sent at all.
///
/// The simulator logs every event to the console instead of sending it; swap
/// in your own provider (Firebase, Amplitude, your own server) with [#use].
// tag::dash-analytics[]
public final class Telemetry {
    private static final String ACCOUNT = "account";
    private static final String RUN = "run";
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

    /// Replaces the providers; the tests use it to read what was sent.
    public static void use(AnalyticsProvider... providers) {
        started = true;
        Analytics.clearProviders();
        for (AnalyticsProvider p : providers) {
            Analytics.addProvider(p);
        }
        screen = null;
    }

    public static boolean asked() {
        return Analytics.getConsent() != null;
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

    /// `mode` is solo, daily, ghost, race or tutorial.
    public static void runStarted(String mode, String level) {
        send(RUN, "run_start", "mode", mode, "level", level);
    }

    /// `place` counts from 0; a solo run is place 0 of 1.
    public static void runEnded(String mode, String level, int place, int metres, boolean crashed) {
        send(RUN, "run_end", "mode", mode, "level", level, "place", Integer.valueOf(place),
                "metres", Integer.valueOf(metres), "crashed", crashed ? "yes" : "no");
    }

    public static void levelPublished(String pack) {
        send(RUN, "level_published", "pack", pack);
    }

    public static void runnerChosen(String runner) {
        send(ECONOMY, "runner_chosen", "runner", runner);
    }

    public static void itemBought(String item, String currency) {
        send(ECONOMY, "spend_virtual_currency", "item", item, "currency", currency);
    }

    public static void purchase(String sku) {
        send(ECONOMY, "purchase", "sku", sku);
    }

    public static void adWatched(String placement) {
        send(ECONOMY, "ad_reward", "placement", placement);
    }

    public static void challengeShared() {
        send(ACCOUNT, "share", "content", "challenge");
    }

    public static void inviteSent(String campaign) {
        send(ACCOUNT, "share", "content", "invite", "campaign", campaign);
    }

    public static void inviteAccepted() {
        send(ACCOUNT, "invite_accepted");
    }

    /// A tap on a push notification. The broadcast's campaign rides in its
    /// data, so the console reports what each broadcast brought back.
    public static void pushOpened(String campaign) {
        send(ACCOUNT, "push_open", "campaign", campaign);
    }

    /// Who the player is, as segments the console slices every report by:
    /// how far they got. No names, no ids.
    public static void describe(int level, int trophies) {
        if (!allowed()) {
            return;
        }
        Analytics.setUserProperty("level_band", level < 5 ? "1-4" : level < 10 ? "5-9" : "10+");
        Analytics.setUserProperty("trophy_band", trophies < 100 ? "0-99" : trophies < 500 ? "100-499" : "500+");
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
// end::dash-analytics[]
