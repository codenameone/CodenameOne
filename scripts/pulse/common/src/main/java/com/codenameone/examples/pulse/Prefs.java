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
package com.codenameone.examples.pulse;

import com.codename1.io.Preferences;

/// What the user has chosen in Settings.
///
/// Every screen reads and writes a preference through this class and through
/// nothing else, so where the values live is decided in one place. Today that
/// is the device: `Preferences`, which survives a restart and is gone with the
/// app. A server that keeps the same values for the account plugs in at
/// [#onChange]: it is told the name of each value as it changes, and puts the
/// account's values in with the setters when it has fetched them. No screen
/// has to change for that.
public final class Prefs {
    /// Told when a value changes, with the value's name.
    public interface Listener {
        void changed(String name);
    }

    public static final String LANGUAGE = "language";
    public static final String THEME = "theme";
    public static final String UNITS = "units";
    public static final String NOTIFY_CLASSES = "notifyClasses";
    public static final String NOTIFY_RECEIPTS = "notifyReceipts";
    public static final String NOTIFY_PROMOTIONS = "notifyPromotions";
    public static final String PAYMENT_METHOD = "defaultPaymentMethodId";
    public static final String QUIET_FROM = "quietFrom";
    public static final String QUIET_TO = "quietTo";
    /// What the listener is told when the values change hands; see [#ownedBy].
    public static final String OWNER = "owner";
    /// Whose the stored values are: see [#ownedBy].
    private static final String RECENTS_OWNER = "recentsOwner";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String UNITS_KM = "km";
    public static final String UNITS_MILES = "mi";
    /// The one way to pay that needs nothing set up.
    public static final String PAYMENT_CASH = "cash";

    private static final String PREFIX = "pulse.pref.";
    /// The plain values that are a copy of the account's; see [#ownedBy].
    private static final String[] ACCOUNT_OWNED = {NOTIFY_CLASSES, NOTIFY_RECEIPTS,
        NOTIFY_PROMOTIONS, PAYMENT_METHOD, QUIET_FROM, QUIET_TO};

    private static Listener listener;

    private Prefs() {
    }

    /// Names the one listener told of every change. A value put in by the
    /// listener itself is reported to it like any other, so a listener that
    /// stores what it is told must not answer by setting the value again.
    public static void onChange(Listener told) {
        listener = told;
    }

    // -------------------------------------------------------------- appearance

    /// The language chosen, as its two-letter code; empty while the user has
    /// chosen none and the device's language decides.
    public static String language() {
        return text(LANGUAGE, "");
    }

    public static void setLanguage(String code) {
        put(LANGUAGE, code);
    }

    /// [#THEME_SYSTEM], [#THEME_LIGHT] or [#THEME_DARK].
    public static String theme() {
        return text(THEME, THEME_SYSTEM);
    }

    public static void setTheme(String theme) {
        put(THEME, theme);
    }

    /// [#UNITS_KM] or [#UNITS_MILES].
    public static String units() {
        return text(UNITS, UNITS_KM);
    }

    public static void setUnits(String units) {
        put(UNITS, units);
    }

    // ----------------------------------------------------------- notifications

    public static boolean notifyClasses() {
        return flag(NOTIFY_CLASSES, true);
    }

    public static void setNotifyClasses(boolean on) {
        put(NOTIFY_CLASSES, on);
    }

    public static boolean notifyReceipts() {
        return flag(NOTIFY_RECEIPTS, true);
    }

    public static void setNotifyReceipts(boolean on) {
        put(NOTIFY_RECEIPTS, on);
    }

    public static boolean notifyPromotions() {
        return flag(NOTIFY_PROMOTIONS, false);
    }

    public static void setNotifyPromotions(boolean on) {
        put(NOTIFY_PROMOTIONS, on);
    }

    // --------------------------------------------------------------- payment

    /// The id of the way the user pays unless they say otherwise for an invoice.
    public static String paymentMethod() {
        return text(PAYMENT_METHOD, PAYMENT_CASH);
    }

    public static void setPaymentMethod(String id) {
        put(PAYMENT_METHOD, id);
    }

    /// The hour of the day (0-23) from which no class reminder is shown.
    public static int quietFrom() {
        return Preferences.get(PREFIX + QUIET_FROM, 22);
    }

    /// The hour of the day (0-23) at which class reminders may be shown again.
    public static int quietTo() {
        return Preferences.get(PREFIX + QUIET_TO, 7);
    }

    public static void setQuiet(int from, int to) {
        if (from != quietFrom() || to != quietTo()) {
            Preferences.set(PREFIX + QUIET_FROM, from);
            Preferences.set(PREFIX + QUIET_TO, to);
            changed(QUIET_FROM);
        }
    }

    // ------------------------------------------------------------- the owner

    /// The account the stored values now belong to. A different account than
    /// the last one forgets the values that are a copy of the previous
    /// account's, so they are not sent to the server as the new one's.
    ///
    /// The listener is told with [#OWNER]. It sends what it is told of to the
    /// server, and values dropped here were not changed by anyone: reported,
    /// they would be saved over the new account's.
    ///
    /// @param account the account, with its server, as
    ///     [com.codenameone.examples.pulse.net.Session#owner(String)] names
    ///     it, or empty for none
    public static void ownedBy(String account) {
        String owner = account == null ? "" : account;
        if (owner.equals(text(RECENTS_OWNER, ""))) {
            return;
        }
        for (String name : ACCOUNT_OWNED) {
            Preferences.delete(PREFIX + name);
        }
        Preferences.set(PREFIX + RECENTS_OWNER, owner);
        changed(OWNER);
    }

    // ------------------------------------------------------------- the store

    private static String text(String name, String fallback) {
        return Preferences.get(PREFIX + name, fallback);
    }

    private static boolean flag(String name, boolean fallback) {
        return Preferences.get(PREFIX + name, fallback);
    }

    private static void put(String name, String value) {
        String wanted = value == null ? "" : value.trim();
        if (!wanted.equals(text(name, null))) {
            Preferences.set(PREFIX + name, wanted);
            changed(name);
        }
    }

    private static void put(String name, boolean value) {
        // Compared with the opposite as the fallback, so a value never stored
        // is stored the first time whatever it is.
        if (flag(name, !value) != value) {
            Preferences.set(PREFIX + name, value);
            changed(name);
        }
    }

    private static void changed(String name) {
        if (listener != null) {
            listener.changed(name);
        }
    }
}
