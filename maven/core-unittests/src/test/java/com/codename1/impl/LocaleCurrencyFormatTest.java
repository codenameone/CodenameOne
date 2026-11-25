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
package com.codename1.impl;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocaleCurrencyFormatTest {
    private static final Locale DE = new Locale("de", "DE");
    private static final Locale US = new Locale("en", "US");
    private static final String EURO = String.valueOf((char) 0x20AC);

    @Test
    void germanConventionsPutTheSymbolAfterTheAmount() {
        LocaleCurrencyFormat f = LocaleCurrencyFormat.parse(",\n.\n" + EURO + "\nEUR \n0\n1");
        assertEquals("EUR", f.getCurrencyCode());
        assertEquals("1.234,50 " + EURO, f.format(1234.5, "EUR", DE));
        assertEquals("1.235 " + yen(DE), f.format(1234.6, "JPY", DE));
        assertEquals("-12,50 " + EURO, f.format(-12.5, "EUR", DE));
    }

    @Test
    void usConventionsPutTheSymbolFirst() {
        LocaleCurrencyFormat f = LocaleCurrencyFormat.parse(".\n,\n$\nUSD\n1\n0");
        assertEquals("$1,234.50", f.format(1234.5, "USD", US));
        String dinar = f.format(1.25, "BHD", US);
        assertEquals("1.250", dinar.substring(dinar.length() - 5));
        assertEquals("$", f.getSymbol("USD", US));
        assertEquals("$0.00", f.format(-0.001, "USD", US));
        assertThrows(IllegalArgumentException.class, () -> f.format(1, "XYZ", US));
    }

    @Test
    void anEmptyGroupingSeparatorDoesNotGroup() {
        LocaleCurrencyFormat f = LocaleCurrencyFormat.parse(".\n\n$\nUSD\n1\n0");
        assertEquals("$1234.50", f.format(1234.5, "USD", US));
    }

    @Test
    void theCLocaleAndMalformedAnswersAreRejected() {
        assertNull(LocaleCurrencyFormat.parse(null));
        assertNull(LocaleCurrencyFormat.parse("\n\n\n\n0\n0"));
        assertNull(LocaleCurrencyFormat.parse(".\n,\n$"));
        assertNull(LocaleCurrencyFormat.parse(".\n,\n$\nQQQ\n1\n0").getCurrencyCode());
    }

    private static String yen(Locale l) {
        return java.util.Currency.getInstance("JPY").getSymbol(l);
    }
}
