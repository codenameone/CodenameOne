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
package com.codenameone.examples.parley;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.AnalyticsProvider;
import com.codename1.analytics.CodenameOneAnalyticsProvider;
import com.codename1.analytics.LoggingAnalyticsProvider;
import com.codename1.ui.CN;
import com.codenameone.examples.parley.ui.Lang;

/// What the app reports about how it is used, and whether it may.
///
/// This is the only class that talks to `com.codename1.analytics`. A screen
/// says what happened by calling one of the methods here, so the whole
/// vocabulary -- every event name and every parameter -- is in this file, and
/// it is here that the one rule is kept: nothing that says who someone is,
/// whom they talk to or what they say ever goes into an event. No name,
/// e-mail address, phone number, conversation id or message text; a count
/// goes as the bracket it falls in. What is known about the user is two
/// things: the mode they are in and the language they read.
///
/// Nothing is reported until the user has said yes. The library starts with
/// consent withheld and drops every call made without it, so a method here is
/// always safe to call. The answer is asked for once, after the first sign-in
/// on a device ([#asked()], `ui.ConsentForm`), can be changed in Settings at
/// any time ([#allow(boolean)]), and is kept by the library on the device, in
/// `Preferences`, with the made-up id that tells one installation from the
/// next. Deleting the account forgets both ([#forget()]).
///
/// On a device the reports go to Codename One's own service, which shows them
/// in the developer console. That service knows which app is reporting from
/// the identity a build stamps into it, so the simulator, which has none, only
/// writes what it would have sent to the log.
public final class Telemetry {
    private static final String ACCOUNT = "account";
    private static final String MESSAGING = "messaging";
    private static final String CALLS = "calls";
    private static final String ADMIN = "admin";
    private static final String SETTINGS = "settings";

    /// The screen reported last, which the next one names as where it came from.
    private static String screen;
    /// The screen that is showing, reported or not.
    private static String showing;
    private static String role;
    /// The role and the language as they were last reported.
    private static String described;
    private static boolean started;

    private Telemetry() {
    }

    /// Says where the reports go. Called once, when the app starts.
    public static void start() {
        if (started) {
            return;
        }
        started = true;
        if (CN.isSimulator()) {
            Analytics.addProvider(new LoggingAnalyticsProvider());
        } else {
            Analytics.addProvider(new CodenameOneAnalyticsProvider());
        }
    }

    /// Sends the reports to `providers` and nowhere else. For a test, which
    /// wants to see what was reported and wants none of it to leave.
    public static void use(AnalyticsProvider... providers) {
        started = true;
        Analytics.clearProviders();
        for (AnalyticsProvider provider : providers) {
            Analytics.addProvider(provider);
        }
        screen = null;
        described = null;
    }

    // --------------------------------------------------------------- consent

    /// Whether the user has answered, either way.
    public static boolean asked() {
        return Analytics.getConsent() != null;
    }

    /// Whether the user said yes.
    public static boolean allowed() {
        AnalyticsConsent consent = Analytics.getConsent();
        return consent != null && consent.isAnalytics();
    }

    /// Records the user's answer. A yes covers how the app is used and the
    /// errors it runs into; nothing here follows a person or serves an
    /// advertisement, and those are never granted.
    public static void allow(boolean yes) {
        boolean was = allowed();
        Analytics.setConsent(AnalyticsConsent.builder()
                .analytics(yes)
                .crashReporting(yes)
                .personalization(false)
                .adStorage(false)
                .build());
        if (yes && !was) {
            // What was dropped while the answer was no: who is using the app,
            // in the two words kept about them, and the screen they are on.
            screen = null;
            described = null;
            describe();
            if (showing != null) {
                screen(showing);
            }
        }
    }

    /// Takes the answer back, so that it is asked for again.
    public static void unask() {
        Analytics.setConsent(null);
    }

    /// The account is gone: the installation gets a new id, which cuts what is
    /// reported from now on from everything reported before, and whoever signs
    /// in next is asked for themselves.
    public static void forget() {
        // What is waiting was gathered under the id that is about to go, and
        // is sent under it.
        Analytics.flush();
        Analytics.resetClientId();
        Analytics.setConsent(null);
        role = null;
        screen = null;
        described = null;
    }

    /// Sends what is waiting. Reports are gathered and sent a batch at a
    /// time; this is for when the app is put away.
    public static void flush() {
        Analytics.flush();
    }

    // ------------------------------------------------------- screens and who

    /// A screen is showing. `name` is the one the screen was built with, which
    /// is not translated and does not change with its title.
    public static void screen(String name) {
        if (name == null || name.length() == 0) {
            return;
        }
        showing = name;
        if (name.equals(screen) || !allowed()) {
            return;
        }
        Analytics.screen(name, screen);
        screen = name;
    }

    /// The user is in `mode`: customer, courier or admin.
    public static void role(String mode) {
        role = mode;
        describe();
    }

    /// Reports the role and the language, when either is news: the home
    /// screen is built again for every change of look, and says its mode each
    /// time.
    private static void describe() {
        if (role == null || !allowed()) {
            return;
        }
        String language = Lang.current();
        String now = role + "/" + language;
        if (!now.equals(described)) {
            described = now;
            Analytics.setUserProperty("role", role);
            Analytics.setUserProperty("language", language);
        }
    }

    // --------------------------------------------------------------- account

    public static void signedIn() {
        send(ACCOUNT, "login", "method", "password");
    }

    /// @param reason `user` when they chose to, `expired` when the session
    ///     ended under them, `deleted` when the account was closed
    public static void signedOut(String reason) {
        send(ACCOUNT, "logout", "reason", reason);
        role = null;
    }

    public static void registered() {
        send(ACCOUNT, "sign_up", "method", "password");
    }

    public static void phoneVerified() {
        send(ACCOUNT, "phone_verified");
    }

    // ------------------------------------------------------------- messaging

    /// @param kind what was sent: text, image, video, voice or file
    /// @param group whether it went to a group rather than one person
    public static void messageSent(String kind, boolean group) {
        send(MESSAGING, "message_sent", "kind", kind, "to", group ? "group" : "direct");
    }

    /// @param members how many people the group started with
    public static void groupCreated(int members) {
        send(MESSAGING, "group_created", "size", bracket(members));
    }

    public static void inviteShared() {
        send(MESSAGING, "invite_shared");
    }

    /// @param seconds the disappearing timer chosen, 0 for off
    public static void timerChanged(int seconds) {
        send(MESSAGING, "disappearing_changed", "timer", seconds == 0 ? "off"
                : seconds < 3600 ? "minutes" : seconds < 86400 ? "hours"
                : seconds < 604800 ? "days" : "weeks");
    }

    public static void safetyNumberViewed() {
        send(MESSAGING, "safety_number_viewed");
    }

    public static void reported() {
        send(MESSAGING, "report_filed");
    }

    public static void blocked(boolean on) {
        send(MESSAGING, on ? "contact_blocked" : "contact_unblocked");
    }

    /// @param network the bridge: telegram or whatsapp
    public static void bridgeLinked(String network) {
        send(MESSAGING, "bridge_linked", "network", network);
    }

    // ----------------------------------------------------------------- calls

    public static void callStarted(boolean video) {
        send(CALLS, "call_started", "media", video ? "video" : "audio");
    }

    /// @param answered whether it was picked up
    /// @param seconds how long it lasted once it was
    public static void callEnded(boolean answered, long seconds) {
        send(CALLS, "call_ended", "answered", answered ? "yes" : "no", "length",
                !answered ? "none" : seconds < 60 ? "under_1_min" : seconds < 600
                ? "1_to_10_min" : "over_10_min");
    }

    // ----------------------------------------------------------------- admin

    /// @param outcome how the report was closed: dismissed, warned or suspended
    public static void reportResolved(String outcome) {
        send(ADMIN, "admin_report_resolved", "outcome", outcome);
    }

    public static void userFlagged(boolean flagged) {
        send(ADMIN, flagged ? "admin_user_flagged" : "admin_user_unflagged");
    }

    public static void userBlocked(boolean blocked) {
        send(ADMIN, blocked ? "admin_user_blocked" : "admin_user_unblocked");
    }

    // -------------------------------------------------------------- settings

    public static void languageChanged(String code) {
        send(SETTINGS, "language_changed", "language", code);
        describe();
    }

    public static void themeChanged(String theme) {
        send(SETTINGS, "theme_changed", "theme", theme);
    }

    // ---------------------------------------------------------------- errors

    /// Something failed that the app caught and carried on from. Only what
    /// kind of failure it was is reported: a message can quote what the user
    /// typed, or the file it was reading.
    public static void error(Throwable failed) {
        if (failed != null) {
            Analytics.crash(failed, failed.getClass().getName(), false);
        }
    }

    // ------------------------------------------------------------- the store

    /// The bracket a count falls in. The count itself, beside a time, can
    /// pick out a group.
    static String bracket(int count) {
        return count < 3 ? "2" : count < 6 ? "3_to_5" : count < 11 ? "6_to_10"
                : count < 51 ? "11_to_50" : "over_50";
    }

    /// Reports `name` with `pairs`: a parameter's name, then its value.
    private static void send(String category, String name, Object... pairs) {
        AnalyticsEvent.Builder event = AnalyticsEvent.create(name).category(category);
        for (int iter = 0; iter + 1 < pairs.length; iter += 2) {
            event.param((String) pairs[iter], pairs[iter + 1]);
        }
        Analytics.event(event.build());
    }
}
