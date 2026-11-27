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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The currency defaults a port inherits when it formats nothing natively.
class L10NManagerCurrencyTest extends UITestBase {

    private static L10NManager plain(String language, String country) {
        return new L10NManager(language, country) {
        };
    }

    @Test
    void currencyCodeComesFromTheCountry() {
        assertEquals("USD", plain("en", "US").getCurrencyCode());
        assertEquals("EUR", plain("de", "DE").getCurrencyCode());
        assertEquals("JPY", plain("ja", "JP").getCurrencyCode());
        assertEquals("ILS", plain("iw", "il").getCurrencyCode());
    }

    @Test
    void noCountryMeansNoCurrency() {
        assertNull(plain("en", "").getCurrencyCode());
        assertNull(plain("en", null).getCurrencyCode());
        assertEquals("" + 1.5, plain("en", "").formatCurrency(1.5));
        assertEquals("$", plain("en", "").getCurrencySymbol());
    }

    @Test
    void anyCurrencyIsFormattedWithItsOwnDigits() {
        L10NManager us = plain("en", "US");
        assertEquals("$1,234.50", us.formatCurrency(1234.5, "USD"));
        assertEquals("$1,234.50", us.formatCurrency(1234.5));
        assertEquals(1, count(us.formatCurrency(1234.5, "JPY"), ','));
        assertEquals(-1, us.formatCurrency(1234.5, "JPY").indexOf('.'));
        assertEquals("1.235", digitsAfterSymbol(us.formatCurrency(1.2346, "BHD")));
    }

    @Test
    void symbolsFollowTheLocale() {
        assertEquals("$", plain("en", "US").getCurrencySymbol());
        assertEquals("$", plain("en", "US").getCurrencySymbol("USD"));
    }

    @Test
    void unknownCodesAreRejected() {
        final L10NManager us = plain("en", "US");
        assertThrows(IllegalArgumentException.class, () -> us.formatCurrency(1, "XYZ"));
        assertThrows(IllegalArgumentException.class, () -> us.getCurrencySymbol("XYZ"));
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    private static String digitsAfterSymbol(String s) {
        int i = 0;
        while (i < s.length() && !Character.isDigit(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }
}
