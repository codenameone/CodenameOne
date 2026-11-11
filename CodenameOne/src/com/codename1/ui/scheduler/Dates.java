/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui.scheduler;

import com.codename1.l10n.DateFormatSymbols;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.plaf.UIManager;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;

/// Names and labels for dates that are held as epoch days.
///
/// The views work in a zone of their own choosing, which need not be the
/// device's, so nothing here formats a `java.util.Date`: the day, month and
/// minute are worked out first and only named here.
final class Dates {
    // The names are asked for with every header painted, so one set is kept
    // for as long as the application's bundle stays the one it was made from.
    private static final SymbolCache CACHE = new SymbolCache();

    private Dates() {
    }

    private static DateFormatSymbols symbols() {
        return CACHE.get(UIManager.getInstance().getBundle());
    }

    /// `isoDay` is 1 for Monday to 7 for Sunday.
    static String weekdayShort(int isoDay) {
        return symbols().getShortWeekdays()[isoDay % 7];
    }

    static String weekdayLong(int isoDay) {
        return symbols().getWeekdays()[isoDay % 7];
    }

    /// `month` is 1 for January.
    static String monthShort(int month) {
        return symbols().getShortMonths()[month - 1];
    }

    static String monthLong(int month) {
        return symbols().getMonths()[month - 1];
    }

    static int year(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getYear();
    }

    static int month(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getMonthValue();
    }

    static int dayOfMonth(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getDayOfMonth();
    }

    /// "Mon, Jan 5".
    static String dayTitle(long epochDay, int isoDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        return weekdayShort(isoDay) + ", " + monthShort(d.getMonthValue()) + " " + d.getDayOfMonth();
    }

    /// "Jan 5".
    static String monthDay(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        return monthShort(d.getMonthValue()) + " " + d.getDayOfMonth();
    }

    /// "January 2026".
    static String monthTitle(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        return monthLong(d.getMonthValue()) + " " + d.getYear();
    }

    /// A time of day: "09:30" on a 24 hour clock, otherwise "9:30 AM", and
    /// "9 AM" on the hour when `brief`.
    static String time(int minuteOfDay, boolean h24, boolean brief) {
        int m = minuteOfDay % 1440;
        int h = m / 60;
        int mm = m % 60;
        StringBuilder sb = new StringBuilder();
        if (h24) {
            if (h < 10) {
                sb.append('0');
            }
            sb.append(h).append(':');
            if (mm < 10) {
                sb.append('0');
            }
            sb.append(mm);
            return sb.toString();
        }
        int h12 = h % 12 == 0 ? 12 : h % 12;
        sb.append(h12);
        if (mm != 0 || !brief) {
            sb.append(':');
            if (mm < 10) {
                sb.append('0');
            }
            sb.append(mm);
        }
        String[] ampm = symbols().getAmPmStrings();
        sb.append(' ').append(ampm[h < 12 ? 0 : 1]);
        return sb.toString();
    }

    /// Whether the device's locale writes one in the afternoon as 13.
    static boolean deviceUses24Hours() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 13);
        c.set(Calendar.MINUTE, 5);
        return L10NManager.getInstance().formatTime(c.getTime()).indexOf("13") >= 0;
    }

    private static final class SymbolCache {
        private DateFormatSymbols symbols;
        private Map<String, String> bundle;

        DateFormatSymbols get(Map<String, String> current) {
            if (symbols == null || current != bundle) { //NOPMD CompareObjectsWithEquals - the bundle's identity is the point
                DateFormatSymbols s = new DateFormatSymbols();
                if (current instanceof Hashtable) {
                    s.setResourceBundle((Hashtable<String, String>) current);
                }
                symbols = s;
                bundle = current;
            }
            return symbols;
        }
    }
}
