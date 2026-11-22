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
package com.codenameone.examples.bazaar.shop;

/// Money as the shopper reads it. Pure: no display, no bundle, so the rules
/// are tested on their own.
///
/// Amounts are whole cents (minor units). Dollars are written `$12.40`; any
/// other currency as the amount followed by its code, `12.40 EUR`. A negative
/// amount -- a discount, a gift card -- keeps its sign even under one unit,
/// which plain integer division would lose (`-50 / 100` is `0`).
public final class Money {
    private Money() {
    }

    /// `cents` in `currency`, or in `fallback` when the amount carries none.
    public static String format(long cents, String currency, String fallback) {
        String code = currency == null || currency.length() == 0 ? fallback : currency;
        boolean negative = cents < 0;
        long abs = negative ? -cents : cents;
        long whole = abs / 100;
        long part = abs % 100;
        String amount = whole + "." + (part < 10 ? "0" : "") + part;
        String sign = negative ? "-" : "";
        if (code == null || code.length() == 0 || "USD".equalsIgnoreCase(code)) {
            return sign + "$" + amount;
        }
        return sign + amount + " " + code;
    }

    /// A price that depends on the variant: `$10.00 - $14.00`, or one price
    /// when every variant costs the same.
    public static String range(long min, long max, String currency, String fallback) {
        if (max <= min) {
            return format(min, currency, fallback);
        }
        return format(min, currency, fallback) + " - " + format(max, currency, fallback);
    }

    /// How much cheaper than `compareAt` the price is, in whole percent; 0
    /// when it is not a sale. Guarded: a zero compare-at price divides
    /// nothing.
    public static int percentOff(long price, long compareAt) {
        if (compareAt <= 0 || price >= compareAt || price < 0) {
            return 0;
        }
        return (int) ((compareAt - price) * 100 / compareAt);
    }

    /// `cents` as a price filter shows it back for editing: `12`, or `12.50`
    /// when there are cents, so what `parse` reads is what was typed.
    public static String typed(long cents) {
        long whole = cents / 100;
        long part = cents % 100;
        if (part == 0) {
            return String.valueOf(whole);
        }
        return whole + "." + (part < 10 ? "0" : "") + part;
    }

    /// Dollars as typed into a price filter (`12`, `12.5`, `12.50`) to cents;
    /// -1 for anything that is not an amount.
    public static long parse(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim();
        if (t.startsWith("$")) {
            t = t.substring(1).trim();
        }
        if (t.length() == 0) {
            return -1;
        }
        long whole = 0;
        long part = 0;
        int partDigits = 0;
        boolean dot = false;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '.' && !dot) {
                dot = true;
            } else if (c >= '0' && c <= '9') {
                if (dot) {
                    if (partDigits < 2) {
                        part = part * 10 + (c - '0');
                        partDigits++;
                    }
                } else {
                    whole = whole * 10 + (c - '0');
                    if (whole > 100000000L) {
                        return -1;
                    }
                }
            } else {
                return -1;
            }
        }
        if (partDigits == 1) {
            part *= 10;
        }
        return whole * 100 + part;
    }
}
