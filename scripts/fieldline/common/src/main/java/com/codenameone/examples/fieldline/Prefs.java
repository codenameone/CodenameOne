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
package com.codenameone.examples.fieldline;

import com.codename1.io.Preferences;

/// What the user has chosen in Settings.
///
/// Every screen reads and writes a preference through this class and through
/// nothing else, so where the values live is decided in one place: the
/// device's `Preferences`, which survives a restart. The account keeps the
/// same values on the server; `Account` listens at [#onChange] and sends each
/// one as it changes, and puts the account's values in with the setters when
/// it has fetched them.
public final class Prefs {
    /// Told when a value changes, with the value's name.
    public interface Listener {
        void changed(String name);
    }

    public static final String LANGUAGE = "language";
    public static final String THEME = "theme";
    public static final String NOTIFY_ASSIGNMENTS = "notifyAssignments";
    public static final String NOTIFY_SCHEDULE = "notifyScheduleChanges";
    public static final String CALENDAR_SYNC = "calendarSync";
    public static final String AI_ASSIST = "aiAssist";
    /// The ids of the events this device put in the calendar, by job.
    public static final String CALENDAR_EVENTS = "calendarEvents";
    private static final String OWNER = "owner";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String PREFIX = "fieldline.pref.";
    /// The plain values that are a copy of the account's; see [#ownedBy].
    private static final String[] ACCOUNT_OWNED = {NOTIFY_ASSIGNMENTS, NOTIFY_SCHEDULE,
        CALENDAR_SYNC, AI_ASSIST, CALENDAR_EVENTS};

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

    // ------------------------------------------------------------------- work

    /// Told when a job is assigned to them.
    public static boolean notifyAssignments() {
        return flag(NOTIFY_ASSIGNMENTS, true);
    }

    public static void setNotifyAssignments(boolean on) {
        put(NOTIFY_ASSIGNMENTS, on);
    }

    /// Told when the scheduler moves their work.
    public static boolean notifyScheduleChanges() {
        return flag(NOTIFY_SCHEDULE, true);
    }

    public static void setNotifyScheduleChanges(boolean on) {
        put(NOTIFY_SCHEDULE, on);
    }

    /// Whether the day's jobs are copied into the device's calendar.
    public static boolean calendarSync() {
        return flag(CALENDAR_SYNC, false);
    }

    public static void setCalendarSync(boolean on) {
        put(CALENDAR_SYNC, on);
    }

    /// Whether photos are diagnosed by the assistant as they are taken.
    public static boolean aiAssist() {
        return flag(AI_ASSIST, true);
    }

    public static void setAiAssist(boolean on) {
        put(AI_ASSIST, on);
    }

    /// The calendar events this device made, as `jobId=eventId` lines.
    /// Kept on the device alone: another phone has its own calendar.
    public static String calendarEvents() {
        return text(CALENDAR_EVENTS, "");
    }

    public static void setCalendarEvents(String lines) {
        Preferences.set(PREFIX + CALENDAR_EVENTS, lines == null ? "" : lines);
    }

    /// Says whose device this is now; empty for nobody. When the account
    /// changes, the copy of the last account's settings is dropped, so the
    /// next person never sends it as their own. How the app looks stays.
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
