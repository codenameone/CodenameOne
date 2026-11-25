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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

/// The user's locale conventions for writing money, as a platform's C API
/// reports them, and the formatting of any currency with them. Shared by the
/// native Windows and Linux ports, whose platforms describe the conventions
/// (separators, symbol, placement) rather than format arbitrary currencies.
///
/// A platform answers one string of six lines: the monetary decimal
/// separator, the monetary grouping separator, the local currency symbol, the
/// ISO 4217 code of the local currency, `1` when the symbol precedes the
/// amount, and `1` when a space separates them. [#parse(String)] answers null
/// for anything else, and the caller then keeps the table-driven default.
///
/// Not reproduced: the locale's own negative pattern (parentheses, a trailing
/// sign; a negative amount gets a leading minus), digit grouping other than
/// threes (India's 1,00,000), and separators longer than one character, of
/// which only the first is used.
public final class LocaleCurrencyFormat {
    private final String decimal;
    private final String grouping;
    private final String symbol;
    private final String code;
    private final boolean precedes;
    private final boolean space;

    private LocaleCurrencyFormat(String decimal, String grouping, String symbol, String code,
            boolean precedes, boolean space) {
        this.decimal = decimal;
        this.grouping = grouping;
        this.symbol = symbol;
        this.code = code;
        this.precedes = precedes;
        this.space = space;
    }

    /// Reads a platform's six-line answer; null when it is missing or does not
    /// have six lines, or when its separators are empty (a C locale, with no
    /// monetary conventions at all).
    ///
    /// #### Parameters
    ///
    /// - `packed`: the platform's answer, possibly null
    ///
    /// #### Returns
    ///
    /// the conventions, or null
    public static LocaleCurrencyFormat parse(String packed) {
        if (packed == null) {
            return null;
        }
        String[] parts = new String[6];
        int start = 0;
        for (int i = 0; i < 5; i++) {
            int nl = packed.indexOf('\n', start);
            if (nl < 0) {
                return null;
            }
            parts[i] = packed.substring(start, nl);
            start = nl + 1;
        }
        parts[5] = packed.substring(start);
        if (parts[0].length() == 0) {
            return null;
        }
        String iso = parts[3].trim();
        if (!isKnownCode(iso)) {
            iso = null;
        }
        return new LocaleCurrencyFormat(parts[0], parts[1], parts[2], iso,
                "1".equals(parts[4]), "1".equals(parts[5].trim()));
    }

    private static boolean isZero(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '1' && c <= '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownCode(String iso) {
        if (iso.length() != 3) {
            return false;
        }
        try {
            Currency.getInstance(iso);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /// The ISO 4217 code of the locale's own currency, or null when the
    /// platform named none that `java.util.Currency` knows.
    ///
    /// #### Returns
    ///
    /// the code, or null
    public String getCurrencyCode() {
        return code;
    }

    /// The symbol for a currency: the platform's own for the locale's currency,
    /// otherwise the one `java.util.Currency` gives for the locale.
    ///
    /// #### Parameters
    ///
    /// - `currencyCode`: an ISO 4217 code
    ///
    /// - `locale`: the user's locale
    ///
    /// #### Returns
    ///
    /// the symbol
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for an unknown code
    public String getSymbol(String currencyCode, Locale locale) {
        Currency c = Currency.getInstance(currencyCode);
        if (c.getCurrencyCode().equals(code) && symbol.length() > 0) {
            return symbol;
        }
        return c.getSymbol(locale);
    }

    /// Writes an amount of a currency with the locale's separators and
    /// placement, and the currency's own number of fraction digits.
    ///
    /// #### Parameters
    ///
    /// - `amount`: the amount
    ///
    /// - `currencyCode`: an ISO 4217 code
    ///
    /// - `locale`: the user's locale
    ///
    /// #### Returns
    ///
    /// the formatted amount
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for an unknown code
    public String format(double amount, String currencyCode, Locale locale) {
        String sym = getSymbol(currencyCode, locale);
        int digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        if (digits < 0) {
            digits = 2;
        }
        // US English symbols are built without asking the host, which may be
        // this very localization manager.
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "US"));
        symbols.setDecimalSeparator(decimal.charAt(0));
        boolean grouped = grouping.length() > 0 && grouping.charAt(0) != decimal.charAt(0);
        if (grouped) {
            symbols.setGroupingSeparator(grouping.charAt(0));
        }
        DecimalFormat number = new DecimalFormat("#,##0", symbols);
        number.setGroupingUsed(grouped);
        number.setMinimumFractionDigits(digits);
        number.setMaximumFractionDigits(digits);
        boolean negative = amount < 0;
        String digitsText = number.format(negative ? -amount : amount);
        if (negative && isZero(digitsText)) {
            // -0.001 dollars is written as no money at all, not "-$0.00".
            negative = false;
        }
        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        if (precedes) {
            sb.append(sym);
            if (space) {
                sb.append(' ');
            }
            sb.append(digitsText);
        } else {
            sb.append(digitsText);
            if (space) {
                sb.append(' ');
            }
            sb.append(sym);
        }
        return sb.toString();
    }
}
