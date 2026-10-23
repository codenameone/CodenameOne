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
package com.codenameone.examples.reel;

import com.codename1.io.Preferences;

/// What the user has chosen in Settings, and who is watching.
///
/// Every screen reads and writes a preference through this class and through
/// nothing else, so where the values live is decided in one place: the
/// device's `Preferences`, which survives a restart and is gone with the app.
public final class Prefs {
    /// Told when a value changes, with the value's name.
    public interface Listener {
        void changed(String name);
    }

    public static final String LANGUAGE = "language";
    public static final String THEME = "theme";
    public static final String AUTOPLAY = "autoplay";
    public static final String SUBTITLES = "subtitles";
    public static final String DATA_SAVER = "dataSaver";
    public static final String NOTIFY_NEW_EPISODES = "notifyNewEpisodes";
    public static final String NOTIFY_ACCOUNT = "notifyAccount";
    public static final String PAYMENT_METHOD = "defaultPaymentMethodId";
    /// The household profile watching on this device.
    public static final String PROFILE = "profile";
    /// Whose the values above are: see [#ownedBy].
    private static final String OWNER = "owner";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    /// The one way to pay that needs nothing set up -- there is none for a
    /// subscription, so it means "no card chosen".
    public static final String PAYMENT_CASH = "cash";

    private static final String PREFIX = "reel.pref.";
    /// The values that belong to the account rather than the device.
    private static final String[] ACCOUNT_OWNED = {PROFILE, PAYMENT_METHOD, SUBTITLES};

    private static Listener listener;

    private Prefs() {
    }

    /// Names the one listener told of every change.
    public static void onChange(Listener told) {
        listener = told;
    }

    /// The language chosen, as its two-letter code; empty while the device decides.
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

    /// Whether the next episode starts by itself when one ends.
    public static boolean autoplay() {
        return flag(AUTOPLAY, true);
    }

    public static void setAutoplay(boolean on) {
        put(AUTOPLAY, on);
    }

    /// The subtitle language, or empty for none.
    public static String subtitles() {
        return text(SUBTITLES, "");
    }

    public static void setSubtitles(String code) {
        put(SUBTITLES, code);
    }

    /// Whether streams are held to standard definition, to spare a data plan.
    public static boolean dataSaver() {
        return flag(DATA_SAVER, false);
    }

    public static void setDataSaver(boolean on) {
        put(DATA_SAVER, on);
    }

    public static boolean notifyNewEpisodes() {
        return flag(NOTIFY_NEW_EPISODES, true);
    }

    /// The choice is the server's to keep (it sends the notifications), so
    /// the device is registered again with it.
    public static void setNotifyNewEpisodes(boolean on) {
        put(NOTIFY_NEW_EPISODES, on);
        com.codenameone.examples.reel.notify.Notifications.signedIn();
    }

    public static boolean notifyAccount() {
        return flag(NOTIFY_ACCOUNT, true);
    }

    public static void setNotifyAccount(boolean on) {
        put(NOTIFY_ACCOUNT, on);
        com.codenameone.examples.reel.notify.Notifications.signedIn();
    }

    /// The card subscriptions and rentals are charged to, or empty.
    public static String paymentMethod() {
        return text(PAYMENT_METHOD, "");
    }

    public static void setPaymentMethod(String id) {
        put(PAYMENT_METHOD, id);
    }

    /// The household profile watching, or empty before one is picked.
    public static String profile() {
        return text(PROFILE, "");
    }

    public static void setProfile(String id) {
        put(PROFILE, id);
    }

    /// Says whose device this is now, by the account and server they signed
    /// in with; empty for nobody. When it changes, the last account's profile,
    /// card and subtitle choice are dropped -- the next person must not start
    /// out as someone else's kids profile. How the app looks stays.
    public static void ownedBy(String account) {
        String owner = account == null ? "" : account;
        if (owner.equals(text(OWNER, ""))) {
            return;
        }
        for (String name : ACCOUNT_OWNED) {
            Preferences.delete(PREFIX + name);
        }
        Preferences.set(PREFIX + OWNER, owner);
    }

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
