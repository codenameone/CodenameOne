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
package com.codename1.l10n;

import com.codename1.junit.UITestBase;
import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The default month names, read out of the long date style.
class L10NManagerMonthNameTest extends UITestBase {

    private static Date march3() {
        Calendar c = Calendar.getInstance();
        c.set(2025, Calendar.MARCH, 3, 12, 0, 0);
        return c.getTime();
    }

    private static L10NManager longStyle(final String text) {
        return new L10NManager("en", "US") {
            @Override
            public String formatDateLongStyle(Date d) {
                return text;
            }
        };
    }

    @Test
    void weekdayOfDateToStringIsSkipped() {
        assertEquals("Mar", longStyle("Mon Mar 03 12:00:00 GMT 2025").getLongMonthName(march3()));
    }

    @Test
    void punctuatedWeekdayIsSkipped() {
        assertEquals("March", longStyle("Monday, March 3, 2025").getLongMonthName(march3()));
        assertEquals("Mar", longStyle("Monday, March 3, 2025").getShortMonthName(march3()));
    }

    @Test
    void anAbbreviationKeepsItsPeriodAndOnlyALeadingWeekdayIsSkipped() {
        assertEquals("Mar.", longStyle("3 Mar. 2025").getLongMonthName(march3()));
        // a weekday-looking word after the first is the month, never skipped
        assertEquals("Mon", longStyle("3 Mon 2025").getLongMonthName(march3()));
    }

    @Test
    void unparseableStyleFallsBackToTheDatesOwnMonth() {
        // the month table itself is read through the long style, so only the date asked about
        // is unparseable; every other month formats normally
        final String[] names = {"January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};
        L10NManager m = new L10NManager("en", "US") {
            @Override
            public String formatDateLongStyle(Date d) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                if (c.get(Calendar.DAY_OF_MONTH) == 3 && c.get(Calendar.MONTH) == Calendar.MARCH) {
                    return "2025-03-03";
                }
                return names[c.get(Calendar.MONTH)] + " 1 2025";
            }
        };
        // DateFormatSymbols reads the month table from the installed manager
        L10NManager previous = implementation.getLocalizationManager();
        implementation.setLocalizationManager(m);
        try {
            assertEquals("March", m.getLongMonthName(march3()));
        } finally {
            implementation.setLocalizationManager(previous);
        }
    }
}
