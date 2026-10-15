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
package com.codename1.jdkparity;

import com.codename1.impl.JdkHost;
import jdkparity.java.text.DecimalFormat;
import jdkparity.java.text.DecimalFormatSymbols;
import jdkparity.java.text.NumberFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// JavaAPI's `DecimalFormatSymbols` takes the device locale's separators and
/// currency from the installed `JdkHost`. A class of its own because the host
/// is process-wide state the other number tests must not see.
class DecimalFormatSymbolsHostTest {

    /// A device that writes "1.234.567,5" and prices in euros.
    private static final class GermanDevice extends JdkHost {
        @Override
        public String[] localeNumberSymbols() {
            return new String[] {",", ".", "EUR"};
        }
    }

    /// A device that writes a decimal comma and does not group.
    private static final class UngroupedDevice extends JdkHost {
        @Override
        public String[] localeNumberSymbols() {
            return new String[] {",", null, null};
        }
    }

    /// A platform whose localization fails outright.
    private static final class BrokenDevice extends JdkHost {
        @Override
        public String[] localeNumberSymbols() {
            throw new IllegalStateException("no localization");
        }
    }

    @AfterEach
    void restoreDefaults() {
        JdkHost.install(null);
    }

    @Test
    void deviceSymbolsComeFromTheHost() {
        JdkHost.install(new GermanDevice());
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("de", "DE"));
        assertEquals(',', symbols.getDecimalSeparator());
        assertEquals(',', symbols.getMonetaryDecimalSeparator());
        assertEquals('.', symbols.getGroupingSeparator());
        assertEquals("EUR", symbols.getCurrencySymbol());
        assertEquals("1.234.567,5", new DecimalFormat("#,##0.0", symbols).format(1234567.5));
        assertEquals("1.234,567", NumberFormat.getNumberInstance(new Locale("de", "DE")).format(1234.567));
    }

    @Test
    void usEnglishIsNeverAskedOfTheHost() {
        JdkHost.install(new GermanDevice());
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "US"));
        assertEquals('.', symbols.getDecimalSeparator());
        assertEquals(',', symbols.getGroupingSeparator());
        assertEquals("$", symbols.getCurrencySymbol());
    }

    @Test
    void aDecimalCommaNeverGroupsWithAComma() {
        JdkHost.install(new UngroupedDevice());
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
        assertEquals(',', symbols.getDecimalSeparator());
        assertEquals('.', symbols.getGroupingSeparator());
        assertEquals("$", symbols.getCurrencySymbol());
    }

    @Test
    void aFailingHostLeavesUsEnglish() {
        JdkHost.install(new BrokenDevice());
        assertEquals(new DecimalFormatSymbols(new Locale("en", "US")),
                new DecimalFormatSymbols(new Locale("fr", "FR")));
    }
}
