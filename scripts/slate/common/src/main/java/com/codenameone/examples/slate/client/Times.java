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
package com.codenameone.examples.slate.client;

import com.codename1.l10n.SimpleDateFormat;
import com.codenameone.examples.slate.ui.Lang;

import java.util.Date;

/// Times as the booking screens say them. Every time is shown in the phone's
/// own zone; the server keeps millis and the business's zone decides the
/// days a slot belongs to.
public final class Times {
    public static final long MINUTE = 60000L;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;

    private Times() {
    }

    public static String clock(long millis) {
        return new SimpleDateFormat("HH:mm").format(new Date(millis));
    }

    /// `2030-01-07`, the day as the API names it, in the phone's zone.
    public static String iso(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(millis));
    }

    /// `Mon 7`, for a strip of days.
    public static String shortDay(long millis) {
        return new SimpleDateFormat("EEE d").format(new Date(millis));
    }

    /// `Nov 7`, for a range of days, where the year would not fit.
    public static String monthDay(long millis) {
        return new SimpleDateFormat("MMM d").format(new Date(millis));
    }

    /// Midnight starting the day `millis` is in, in the phone's zone.
    public static long dayStart(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(new Date(millis));
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }

    /// The Monday starting the week `millis` is in: a week view starts where a
    /// calendar's week does, not on whatever day it was opened.
    public static long weekStart(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(new Date(dayStart(millis)));
        int back = (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        return addDays(millis, -back);
    }

    /// The day `days` after the one `millis` is in. Noon of the target day
    /// is found first, so an hour gained or lost to the clocks changing on
    /// the way still lands inside the right day.
    public static long addDays(long millis, int days) {
        return dayStart(dayStart(millis) + days * DAY + 12 * HOUR);
    }

    /// `45 min`, `1 h 30`.
    public static String length(int minutes) {
        if (minutes < 60) {
            return Lang.tr("{0} min", String.valueOf(minutes));
        }
        int rest = minutes % 60;
        return rest == 0 ? Lang.tr("{0} h", String.valueOf(minutes / 60))
                : Lang.tr("{0} h {1}", String.valueOf(minutes / 60), two(rest));
    }

    /// `09:30` for 570 minutes after midnight.
    public static String minuteOfDay(int minute) {
        return two(minute / 60) + ":" + two(minute % 60);
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
