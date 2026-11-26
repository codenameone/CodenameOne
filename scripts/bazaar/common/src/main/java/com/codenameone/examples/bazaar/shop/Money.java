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

import com.codenameone.examples.bazaar.ops.Amounts;

/// Money as the shopper reads it. Pure: no display, no bundle, so the rules
/// are tested on their own.
///
/// Amounts are whole minor units of their currency -- cents, yen, fils --
/// with as many decimals as [Amounts#digits(String)] gives it. Dollars are written `$12.40`; any
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
        int digits = Amounts.digits(code);
        long unit = Amounts.unit(digits);
        String amount = grouped(abs / unit) + fraction(abs % unit, digits);
        String sign = negative ? "-" : "";
        if (code == null || code.length() == 0 || "USD".equalsIgnoreCase(code)) {
            return sign + "$" + amount;
        }
        return sign + amount + " " + code;
    }

    /// `cents` in whole dollars, rounded down, for a figure that has to fit
    /// in a ring: `$877`, `$1,250`.
    public static String whole(long cents) {
        return (cents < 0 ? "-$" : "$") + grouped(Math.abs(cents) / 100);
    }

    /// `part` minor units as the decimals of an amount, `.05` for two
    /// digits, `.005` for three, nothing for a currency without decimals.
    private static String fraction(long part, int digits) {
        if (digits <= 0) {
            return "";
        }
        String text = String.valueOf(part);
        StringBuilder out = new StringBuilder(".");
        for (int iter = text.length(); iter < digits; iter++) {
            out.append('0');
        }
        return out.append(text).toString();
    }

    /// `whole` with a comma between each group of three digits: `4,952`.
    static String grouped(long whole) {
        String digits = String.valueOf(whole);
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < digits.length(); iter++) {
            if (iter > 0 && (digits.length() - iter) % 3 == 0) {
                out.append(',');
            }
            out.append(digits.charAt(iter));
        }
        return out.toString();
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
        return typed(cents, "USD");
    }

    /// [#typed(long)] in the minor units of `currency`: `1250` yen is `1250`,
    /// `1250` fils is `1.250`.
    public static String typed(long cents, String currency) {
        // Sign first, then the magnitude: -1250 is "-12.50", never "-12.-50".
        String sign = cents < 0 ? "-" : "";
        long a = Math.abs(cents);
        int digits = Amounts.digits(currency);
        long unit = Amounts.unit(digits);
        long whole = a / unit;
        long part = a % unit;
        if (part == 0) {
            return sign + whole;
        }
        return sign + whole + fraction(part, digits);
    }

    /// Dollars as typed into a price filter (`12`, `12.5`, `12.50`) to cents;
    /// -1 for anything that is not an amount.
    public static long parse(String text) {
        return parse(text, "USD");
    }

    /// [#parse(String)] in `currency`: as many decimals as it has, so `12.5`
    /// is 1250 cents, `1250` is 1250 yen and `1.25` is 1250 fils. More
    /// decimals than that (`12.345` dollars) is -1, not a cut-down amount.
    public static long parse(String text, String currency) {
        int digits = Amounts.digits(currency);
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
                    if (partDigits < digits) {
                        part = part * 10 + (c - '0');
                        partDigits++;
                    } else if (c != '0') {
                        // More decimals than the currency has: not an amount,
                        // never a cut-down one.
                        return -1;
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
        for (; partDigits < digits; partDigits++) {
            part *= 10;
        }
        return whole * Amounts.unit(digits) + part;
    }
}
