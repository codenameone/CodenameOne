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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.l10n.L10NManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/// java.util.Currency and the L10NManager currency API on the device. Every
/// assertion holds under any device locale: the device's own currency is only
/// required to be a real ISO 4217 code, and formatted amounts are judged by
/// how many digits they carry, never by symbol placement or separators.
public class CurrencyApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            currencyFacts();
            deviceCurrency();
            formatsAnyCurrency();
            numberFormatTakesACurrency();
        } catch (Throwable t) {
            fail("currency API failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private void currencyFacts() {
        Currency eur = Currency.getInstance("EUR");
        assertEqual("EUR", eur.getCurrencyCode(), "EUR code");
        assertEqual(978, eur.getNumericCode(), "EUR numeric code");
        assertEqual(2, eur.getDefaultFractionDigits(), "EUR digits");
        assertEqual(0, Currency.getInstance("JPY").getDefaultFractionDigits(), "JPY digits");
        assertEqual(3, Currency.getInstance("BHD").getDefaultFractionDigits(), "BHD digits");
        assertEqual("036", Currency.getInstance("AUD").getNumericCodeAsString(), "AUD numeric string");
        assertBool(eur == Currency.getInstance("EUR"), "one instance per code");
        assertBool(Currency.getInstance(new Locale("ja", "JP")) == Currency.getInstance("JPY"), "Japan uses yen");
        assertBool(Currency.getAvailableCurrencies().contains(eur), "EUR is available");
        // A malformed code, not merely an unknown one: Android hands
        // java.util.Currency to ICU, which accepts ANY well-formed three-letter
        // code ("XYZ" returns a currency there) and rejects only what is not
        // three letters. The JDK and Codename One's own tables reject both.
        boolean threw = false;
        try {
            Currency.getInstance("EURO");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertBool(threw, "a malformed code is rejected");
    }

    private void deviceCurrency() {
        L10NManager l10n = L10NManager.getInstance();
        String code = l10n.getCurrencyCode();
        if (code != null) {
            assertNotNull(Currency.getInstance(code), "device currency " + code + " is ISO 4217");
            assertBool(l10n.formatCurrency(12.5).length() > 0, "device currency formats");
        }
        String symbol = l10n.getCurrencySymbol();
        assertBool(symbol != null && symbol.length() > 0, "device currency symbol");
        String euro = l10n.getCurrencySymbol("EUR");
        assertBool(euro != null && euro.length() > 0, "EUR symbol");
    }

    private void formatsAnyCurrency() {
        L10NManager l10n = L10NManager.getInstance();
        assertEqual(6, digits(l10n.formatCurrency(1234.5, "USD")), "USD has two fraction digits: "
                + l10n.formatCurrency(1234.5, "USD"));
        assertEqual(4, digits(l10n.formatCurrency(1234, "JPY")), "JPY has no fraction digits: "
                + l10n.formatCurrency(1234, "JPY"));
        assertEqual(4, digits(l10n.formatCurrency(1.25, "BHD")), "BHD has three fraction digits: "
                + l10n.formatCurrency(1.25, "BHD"));
        boolean threw = false;
        try {
            // Malformed for the reason given in currencyFacts: Android accepts "XYZ".
            l10n.formatCurrency(1, "EURO");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertBool(threw, "formatCurrency rejects a malformed code");
    }

    private void numberFormatTakesACurrency() {
        NumberFormat f = NumberFormat.getCurrencyInstance();
        Currency chf = Currency.getInstance("CHF");
        f.setCurrency(chf);
        assertBool(f.getCurrency() == chf, "NumberFormat keeps the currency it was given");
        assertBool(f.format(3.5).length() > 0, "NumberFormat formats in CHF");
    }

    /// Counts digits in any script: a device locale may not write ASCII ones.
    private static int digits(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                n++;
            }
        }
        return n;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
