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
package com.codenameone.examples.savor;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.AnalyticsProvider;
import com.codename1.analytics.CodenameOneAnalyticsProvider;
import com.codename1.analytics.LoggingAnalyticsProvider;
import com.codename1.ui.CN;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.ui.Lang;

/// What the app reports about how it is used, and whether it may.
///
/// This is the only class that talks to `com.codename1.analytics`. A screen
/// says what happened by calling one of the methods here, so the whole
/// vocabulary -- every event name and every parameter -- is in this file, and
/// it is here that the one rule is kept: nothing that says who someone is or
/// where they are ever goes into an event. No name, e-mail address, phone
/// number, address, position, card number or delivery id; an amount of money goes
/// as the bracket it falls in. What is known about the user is two things: the
/// mode they are in and the language they read.
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
    private static final String DELIVERY = "delivery";
    private static final String PAYMENT = "payment";
    private static final String COURIER = "courier";
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

    /// @param onboarding whether they came to drive
    public static void registered(boolean onboarding) {
        send(ACCOUNT, "sign_up", "method", "password", "intent", onboarding ? "courier" : "customer");
    }

    public static void phoneVerified() {
        send(ACCOUNT, "phone_verified");
    }

    // ------------------------------------------------------------------ deliveries

    /// @param lines how many dishes the cart holds
    public static void orderQuoted(int lines) {
        send(DELIVERY, "order_quoted", "lines", Integer.valueOf(lines));
    }

    public static void orderPlaced(boolean cash, boolean promo) {
        send(DELIVERY, "order_placed", "payment", cash ? "cash" : "card", "promo",
                promo ? "yes" : "no");
    }

    /// @param by `customer`, `courier` or `admin`
    /// @param state the state the delivery was in
    public static void deliveryCancelled(String by, DeliveryState state) {
        send(DELIVERY, "delivery_cancelled", "by", by, "state", state == null ? "" : state.name());
    }

    /// @param side whose device saw it end: `customer` or `courier`. A delivery that
    ///     both of them use the app for is reported by both, so a count of
    ///     deliveries takes one side.
    public static void deliveryCompleted(String side, long fareCents, boolean cash) {
        send(DELIVERY, "delivery_completed", "side", side, "fare", fare(fareCents), "payment",
                cash ? "cash" : "card");
    }

    public static void rated(int stars) {
        send(DELIVERY, "rating_given", "stars", Integer.valueOf(stars));
    }

    // --------------------------------------------------------------- payment

    /// @param where `delivery` at the end of the delivery, `receipt` from the receipt
    /// @param percent the share of the fare, or 0 for an amount typed in
    public static void tipped(String where, int percent) {
        send(PAYMENT, "tip_added", "where", where, "share",
                percent > 0 ? percent + "_percent" : "custom");
    }

    public static void cardAdded() {
        send(PAYMENT, "card_added");
    }

    public static void cardRemoved() {
        send(PAYMENT, "card_removed");
    }

    // --------------------------------------------------------------- onboarding

    /// @param again whether this is an application sent back, being put right
    public static void applicationStarted(boolean again) {
        send(COURIER, "courier_application_started", "resubmission", Boolean.valueOf(again));
    }

    public static void applicationSubmitted() {
        send(COURIER, "courier_application_submitted");
    }

    public static void courierOnline(boolean online) {
        send(COURIER, online ? "courier_online" : "courier_offline");
    }

    public static void offerAnswered(boolean accepted) {
        send(COURIER, accepted ? "offer_accepted" : "offer_declined");
    }

    public static void payoutRequested(long cents) {
        send(COURIER, "payout_requested", "amount", fare(cents));
    }

    // ----------------------------------------------------------------- admin

    public static void applicationReviewed(boolean approved) {
        send(ADMIN, approved ? "admin_application_approved" : "admin_application_rejected");
    }

    public static void userFlagged(boolean flagged) {
        send(ADMIN, flagged ? "admin_user_flagged" : "admin_user_unflagged");
    }

    public static void userBlocked(boolean blocked) {
        send(ADMIN, blocked ? "admin_user_blocked" : "admin_user_unblocked");
    }

    /// @param from the screen it was done from
    public static void deliveryRefunded(String from) {
        send(ADMIN, "admin_delivery_refunded", "from", from);
    }

    public static void pricingChanged() {
        send(ADMIN, "admin_pricing_changed");
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

    /// The bracket an amount of money falls in, in whole units of the
    /// currency. The amount itself, beside a time, can be matched to a delivery.
    static String fare(long cents) {
        long units = cents / 100;
        return units < 5 ? "under_5" : units < 10 ? "5_to_10" : units < 20 ? "10_to_20"
                : units < 35 ? "20_to_35" : units < 50 ? "35_to_50" : "50_or_more";
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
