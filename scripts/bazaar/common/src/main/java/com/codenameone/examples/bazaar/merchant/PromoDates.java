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
package com.codenameone.examples.bazaar.merchant;

import java.util.Calendar;
import java.util.TimeZone;

/// The date arithmetic of the promotions calendar: what a drag onto another
/// day means for an event's window, and whether the server will take it.
///
/// A month view moves whole days. The event keeps its time of day and its
/// length -- a 09:00 to 21:00 flash sale dropped two days later runs 09:00
/// to 21:00 two days later -- worked out in the merchant's zone, so a move
/// across a daylight-saving change still starts at 09:00 local time.
final class PromoDates {
    static final String SALE = "SALE";
    static final String DISCOUNT = "DISCOUNT";
    static final String CAMPAIGN = "CAMPAIGN";
    static final long DAY = 24L * 60 * 60 * 1000;
    /// A campaign shows on the calendar as an hour from its send time.
    static final long CAMPAIGN_LENGTH = 60L * 60 * 1000;

    private PromoDates() {
    }

    /// Local midnight of the day `millis` falls on.
    static long dayStart(long millis, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTime(new java.util.Date(millis));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }

    /// Calendar days from the day of `from` to the day of `to` (negative
    /// when `to` is earlier), counted on the wall calendar, not in 24-hour
    /// blocks.
    static int daysBetween(long from, long to, TimeZone zone) {
        long a = dayStart(from, zone);
        long b = dayStart(to, zone);
        // Rounded: a day across a daylight-saving change is 23 or 25 hours.
        long diff = b - a;
        return (int) (diff >= 0 ? (diff + DAY / 2) / DAY : -((-diff + DAY / 2) / DAY));
    }

    /// `millis` moved by `days` calendar days at the same local time.
    static long addDays(long millis, int days, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTime(new java.util.Date(millis));
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        int ms = c.get(Calendar.MILLISECOND);
        long day = dayStart(millis, zone) + days * DAY + DAY / 2;
        c.setTime(new java.util.Date(day));
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, s);
        c.set(Calendar.MILLISECOND, ms);
        return c.getTime().getTime();
    }

    /// The window `{start, end}` an event of `[start, end)` gets when its
    /// first day is dropped on the day of `target`: same time of day, same
    /// number of calendar days, the end at its own local time.
    static long[] moveToDay(long start, long end, long target, TimeZone zone) {
        int days = daysBetween(start, target, zone);
        long s = addDays(start, days, zone);
        long e = end > start ? addDays(end, days, zone) : s;
        return new long[] {s, e};
    }

    /// Why the server would refuse to move this event, or null when it may
    /// move to `[start, end)`: a sale only while scheduled and not yet over,
    /// a campaign only before it goes out and into the future, and anything
    /// only with its start before its end.
    static String refusal(String kind, String status, long start, long end, long now) {
        if (CAMPAIGN.equals(kind)) {
            if (!"SCHEDULED".equals(status) && !"DRAFT".equals(status)) {
                return "That campaign has already gone out";
            }
            return start <= now ? "Schedule it in the future" : null;
        }
        if (start <= 0 || end <= start) {
            return "A start before the end";
        }
        if (SALE.equals(kind)) {
            if (!"SCHEDULED".equals(status)) {
                return "Only a sale that has not started can move";
            }
            if (end - start > 366L * DAY) {
                return "A sale needs a start before its end, within a year";
            }
            if (end <= now) {
                return "That sale would already be over";
            }
        }
        return null;
    }

    /// The query window for the month around `anyDay`: from a week before
    /// its first day to a week after its last, so the weeks of the adjoining
    /// months the grid shows are filled too.
    static long[] monthWindow(long anyDay, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTime(new java.util.Date(dayStart(anyDay, zone)));
        c.set(Calendar.DAY_OF_MONTH, 1);
        long first = c.getTime().getTime();
        int month = c.get(Calendar.MONTH);
        long next = first;
        int guard = 0;
        while (guard++ < 40) {
            next = addDays(next, 1, zone);
            c.setTime(new java.util.Date(next));
            if (c.get(Calendar.MONTH) != month) {
                break;
            }
        }
        return new long[] {addDays(first, -7, zone), addDays(next, 7, zone)};
    }

    /// A `#rrggbb` colour as an int, or `fallback` for anything else.
    static int colour(String hex, int fallback) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return fallback;
        }
        int out = 0;
        for (int iter = 1; iter < 7; iter++) {
            int d = Character.digit(hex.charAt(iter), 16);
            if (d < 0) {
                return fallback;
            }
            out = out * 16 + d;
        }
        return out;
    }
}
