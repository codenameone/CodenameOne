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
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for {@link DateFormatSymbols}.
 */
class DateFormatSymbolsTest extends UITestBase {

    /**
     * Regression test for
     * <a href="https://github.com/codenameone/CodenameOne/issues/1243">#1243</a>
     * — getShortWeekdays() must look up resource-bundle keys by the English
     * weekday name (the documented {@code WEEKDAY_SHORTNAME_<DAY>} key), not by
     * the already-localized long name. Otherwise a Dutch user supplying
     * {@code WEEKDAY_LONGNAME_MONDAY=maandag} and
     * {@code WEEKDAY_SHORTNAME_MONDAY=ma} would see "maa" (the first three
     * characters of the long name) instead of "ma".
     */
    @Test
    void getShortWeekdaysUsesEnglishKeyAfterLocalization() {
        Hashtable<String, String> bundle = new Hashtable<String, String>();
        bundle.put("WEEKDAY_LONGNAME_MONDAY", "maandag");
        bundle.put("WEEKDAY_SHORTNAME_MONDAY", "ma");
        bundle.put("WEEKDAY_LONGNAME_TUESDAY", "dinsdag");
        bundle.put("WEEKDAY_SHORTNAME_TUESDAY", "di");

        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setResourceBundle(bundle);

        String[] shortWeekdays = symbols.getShortWeekdays();

        // Weekday array is Sunday-first per the DateFormatSymbols / Calendar
        // convention: index 0 = Sunday, 1 = Monday, ...
        assertEquals("ma", shortWeekdays[Calendar.MONDAY - 1],
                "Localized short weekday must come from WEEKDAY_SHORTNAME_MONDAY, "
                        + "not from a derived prefix of the long name 'maandag'");
        assertEquals("di", shortWeekdays[Calendar.TUESDAY - 1],
                "Localized short weekday must come from WEEKDAY_SHORTNAME_TUESDAY, "
                        + "not from a derived prefix of the long name 'dinsdag'");
    }

    /**
     * Sanity check: the long weekday lookup itself was already correct in the
     * 2015 report — this test guards against a regression in the (unrelated)
     * {@link DateFormatSymbols#getWeekdays()} behaviour while fixing
     * {@link DateFormatSymbols#getShortWeekdays()}.
     */
    @Test
    void getWeekdaysReturnsLocalizedLongNames() {
        Hashtable<String, String> bundle = new Hashtable<String, String>();
        bundle.put("WEEKDAY_LONGNAME_MONDAY", "maandag");

        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setResourceBundle(bundle);

        assertEquals("maandag", symbols.getWeekdays()[Calendar.MONDAY - 1]);
    }

    /**
     * When the resource bundle has only the long name but no short name, the
     * code should fall back to the English-derived prefix (first three
     * characters of the English weekday name), not the prefix of the localized
     * long name. This matches what {@link DateFormatSymbols#getShortMonths()}
     * already does for months.
     */
    @Test
    void getShortWeekdaysFallsBackToEnglishPrefixWhenShortKeyMissing() {
        Hashtable<String, String> bundle = new Hashtable<String, String>();
        bundle.put("WEEKDAY_LONGNAME_MONDAY", "maandag");
        // intentionally no WEEKDAY_SHORTNAME_MONDAY

        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setResourceBundle(bundle);

        // Before the fix this would be "maa" (prefix of the localized name).
        // After the fix it is "Mon" (prefix of the English name) — which is
        // the same behaviour {@link DateFormatSymbols#getShortMonths()} has
        // exhibited all along when the SHORTNAME key is missing.
        assertEquals("Mon", symbols.getShortWeekdays()[Calendar.MONDAY - 1]);
    }

    /// The platform's short month names were written into the long-name
    /// array: getShortMonths() threw when the long names had not been built,
    /// and overwrote them with short names when they had.
    @Test
    void localizedShortMonthsAreThePlatformsAndLeaveLongNamesAlone() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] longBefore = symbols.getMonths().clone();
        String[] shortMonths = symbols.getShortMonths();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        assertEquals(L10NManager.getInstance().getShortMonthName(cal.getTime()), shortMonths[Calendar.MARCH]);
        assertArrayEquals(longBefore, symbols.getMonths());
    }

    @Test
    void shortMonthsWithoutAnyLongNameBuiltDoNotThrow() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        assertEquals(12, symbols.getShortMonths().length);
    }

    /// Without platform localization a short month is the English prefix,
    /// as for weekdays, not the English long name.
    @Test
    void unlocalizedShortMonthsAreTheEnglishPrefix() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setLocalized(false);
        assertEquals("Jan", symbols.getShortMonths()[0]);
        assertEquals("Sep", symbols.getShortMonths()[8]);
    }
}
