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

import com.codename1.io.Preferences;

/// What the user has chosen in Settings.
///
/// Every screen reads and writes a preference through this class and through
/// nothing else. The values live on the device in `Preferences`, and most are
/// also the account's: [com.codenameone.examples.parley.net.Account] listens
/// at [#onChange] and keeps the server's copy in step, so a second device
/// signs in with the same privacy settings. The server needs them anyway --
/// it is the server that hides last-seen and the profile photo from whoever
/// the user chose, and that decides whom a push wakes.
public final class Prefs {
    /// Told when a value changes, with the value's name.
    public interface Listener {
        void changed(String name);
    }

    public static final String LANGUAGE = "language";
    public static final String THEME = "theme";
    public static final String NOTIFY_MESSAGES = "notifyMessages";
    public static final String NOTIFY_GROUPS = "notifyGroups";
    public static final String NOTIFY_CALLS = "notifyCalls";
    public static final String READ_RECEIPTS = "readReceipts";
    public static final String TYPING = "typingIndicators";
    public static final String LAST_SEEN = "lastSeen";
    public static final String PROFILE_PHOTO = "profilePhoto";
    public static final String DISAPPEARING = "disappearingSeconds";
    public static final String SCREEN_SECURITY = "screenSecurity";
    public static final String APP_LOCK = "appLock";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    /// Who sees last-seen and the profile photo.
    public static final String EVERYONE = "everyone";
    public static final String CONTACTS = "contacts";
    public static final String NOBODY = "nobody";

    private static final String PREFIX = "parley.pref.";
    /// The values that are a copy of the account's; see [#ownedBy].
    private static final String[] ACCOUNT_OWNED = {NOTIFY_MESSAGES, NOTIFY_GROUPS, NOTIFY_CALLS,
        READ_RECEIPTS, TYPING, LAST_SEEN, PROFILE_PHOTO, DISAPPEARING, SCREEN_SECURITY, APP_LOCK};
    private static final String OWNER = "owner";

    private static Listener listener;

    private Prefs() {
    }

    /// Names the one listener told of every change. A value put in by the
    /// listener itself is reported to it like any other, so a listener that
    /// stores what it is told must not answer by setting the value again.
    public static void onChange(Listener told) {
        listener = told;
    }

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

    public static boolean notifyMessages() {
        return flag(NOTIFY_MESSAGES, true);
    }

    public static void setNotifyMessages(boolean on) {
        put(NOTIFY_MESSAGES, on);
    }

    public static boolean notifyGroups() {
        return flag(NOTIFY_GROUPS, true);
    }

    public static void setNotifyGroups(boolean on) {
        put(NOTIFY_GROUPS, on);
    }

    public static boolean notifyCalls() {
        return flag(NOTIFY_CALLS, true);
    }

    public static void setNotifyCalls(boolean on) {
        put(NOTIFY_CALLS, on);
    }

    /// Whether others see that this account read their message. Off means
    /// this account does not see theirs either, as in every messenger.
    public static boolean readReceipts() {
        return flag(READ_RECEIPTS, true);
    }

    public static void setReadReceipts(boolean on) {
        put(READ_RECEIPTS, on);
    }

    public static boolean typingIndicators() {
        return flag(TYPING, true);
    }

    public static void setTypingIndicators(boolean on) {
        put(TYPING, on);
    }

    /// [#EVERYONE], [#CONTACTS] or [#NOBODY].
    public static String lastSeen() {
        return text(LAST_SEEN, EVERYONE);
    }

    public static void setLastSeen(String who) {
        put(LAST_SEEN, who);
    }

    public static String profilePhoto() {
        return text(PROFILE_PHOTO, EVERYONE);
    }

    public static void setProfilePhoto(String who) {
        put(PROFILE_PHOTO, who);
    }

    /// The disappearing timer new chats start with, in seconds; 0 for off.
    public static int disappearingSeconds() {
        return Preferences.get(PREFIX + DISAPPEARING, 0);
    }

    public static void setDisappearingSeconds(int seconds) {
        if (seconds != disappearingSeconds()) {
            Preferences.set(PREFIX + DISAPPEARING, seconds);
            changed(DISAPPEARING);
        }
    }

    /// Keeps the app out of screenshots, screen recordings and the app
    /// switcher's picture.
    public static boolean screenSecurity() {
        return flag(SCREEN_SECURITY, false);
    }

    public static void setScreenSecurity(boolean on) {
        put(SCREEN_SECURITY, on);
    }

    /// Asks for the fingerprint, face or device passcode to open the app.
    public static boolean appLock() {
        return flag(APP_LOCK, false);
    }

    public static void setAppLock(boolean on) {
        put(APP_LOCK, on);
    }

    /// Says whose device this is now, by the name they signed in with; empty
    /// for nobody. When the account changes, the copy of the last account's
    /// settings is dropped rather than shown to the next and sent as theirs.
    /// How the app looks (language, theme) is the device's and stays.
    ///
    /// @param account the account, with its server, as
    ///     [com.codenameone.examples.parley.net.Session#owner(String)] names
    ///     it, or empty for none
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
