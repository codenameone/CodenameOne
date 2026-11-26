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
package com.codenameone.examples.bazaar.ops;

import java.util.Currency;
import java.util.Locale;

/// Money as integers in the minor units of its own currency.
///
/// Every amount in Bazaar -- including the columns and fields still named
/// `...Cents` -- is a whole number of the currency's smallest unit: cents for
/// US dollars, yen for yen (no decimals), fils for Bahraini dinars (three).
/// How many decimals a currency has comes from
/// `Currency.getInstance(code).getDefaultFractionDigits()`, never from a
/// constant 100.
///
/// ```java
/// Amounts.plain(1250, "USD");   // "12.50"
/// Amounts.plain(1250, "JPY");   // "1250"
/// Amounts.format(1250, "BHD");  // "BHD 1.250"
/// ```
///
/// Nothing here touches a `double` or the default locale: the text is the
/// same on the server, under both of its runtimes, and on every device.
public final class Amounts {
    /// Rates are millionths: 1000000 is one to one.
    public static final long MICROS = 1000000L;

    private Amounts() {
    }

    /// The decimals of `code`: 2 for USD, 0 for JPY, 3 for BHD. An unknown or
    /// empty code counts as 2, and a pseudo currency without decimals (gold
    /// answers -1) as 0.
    public static int digits(String code) {
        String upper = upper(code);
        if (upper.length() != 3) {
            return 2;
        }
        Currency c;
        try {
            c = Currency.getInstance(upper);
        } catch (IllegalArgumentException unknown) {
            return 2;
        }
        return Math.max(0, c.getDefaultFractionDigits());
    }

    /// `10^digits`: how many minor units make one major unit.
    public static long unit(int digits) {
        long u = 1;
        for (int i = 0; i < digits; i++) {
            u *= 10;
        }
        return u;
    }

    /// How many minor units of `code` make one major unit: 100, 1 or 1000.
    public static long unitOf(String code) {
        return unit(digits(code));
    }

    /// An amount in major units with the currency's own decimals and no
    /// grouping, as structured data and exports want it: `1250, "USD"` is
    /// `12.50`, `1250, "JPY"` is `1250`, `1250, "BHD"` is `1.250`.
    public static String plain(long minor, String code) {
        return plain(minor, digits(code));
    }

    /// [#plain(long, String)] for a digit count.
    public static String plain(long minor, int digits) {
        boolean negative = minor < 0;
        String abs = Long.toString(negative ? -minor : minor);
        StringBuilder b = new StringBuilder();
        if (negative) {
            b.append('-');
        }
        if (digits <= 0) {
            return b.append(abs).toString();
        }
        StringBuilder padded = new StringBuilder();
        for (int i = abs.length(); i <= digits; i++) {
            padded.append('0');
        }
        padded.append(abs);
        String all = padded.toString();
        int point = all.length() - digits;
        b.append(all.substring(0, point)).append('.').append(all.substring(point));
        return b.toString();
    }

    /// The symbol a page shows in front of an amount, as US English writes
    /// it (`$`, the euro and yen signs, `CA$`), or empty when the currency is
    /// written by its code.
    public static String symbol(String code) {
        String upper = upper(code);
        if (upper.length() != 3) {
            return "";
        }
        try {
            String s = Currency.getInstance(upper).getSymbol(Locale.US);
            return s == null || s.equals(upper) ? "" : s;
        } catch (IllegalArgumentException unknown) {
            return "";
        }
    }

    /// `1250, "USD"` is `$12.50`; a currency without a symbol is spelled out
    /// in front: `BHD 1.250`.
    public static String format(long minor, String code) {
        String symbol = symbol(code);
        String amount = plain(minor < 0 ? -minor : minor, code);
        String sign = minor < 0 ? "-" : "";
        if (symbol.length() > 0) {
            return sign + symbol + amount;
        }
        return sign + upper(code) + " " + amount;
    }

    /// Minor units of a decimal string in `code` (`"12.3"` USD is 1230,
    /// `"1250"` JPY is 1250). 0 when it is not a number, has more decimals
    /// than the currency (`"12.345"` USD; trailing zeros past them are fine,
    /// `"1250.0"` JPY is 1250), or would not fit in a long. Nothing is ever
    /// cut or rounded: an amount it answers is exactly the one written.
    public static long parse(String value, String code) {
        return parse(value, digits(code));
    }

    /// [#parse(String, String)] for a digit count.
    public static long parse(String value, int digits) {
        long minor = read(value, digits);
        return minor == NOT_AN_AMOUNT ? 0 : minor;
    }

    /// What [#read] answers for text that is not an exact amount.
    public static final long NOT_AN_AMOUNT = Long.MIN_VALUE;

    /// [#parse(String, int)], but [#NOT_AN_AMOUNT] rather than 0 when the
    /// text is not a number, has more (non-zero) decimals than `digits`, or
    /// does not fit: for a field that must tell a typo from a zero.
    public static long read(String value, int digits) {
        String s = value == null ? "" : value.trim();
        boolean negative = s.startsWith("-");
        int at = negative ? 1 : 0;
        if (at >= s.length() || s.length() > 40) {
            return NOT_AN_AMOUNT;
        }
        long whole = 0;
        long frac = 0;
        int fracDigits = 0;
        boolean dot = false;
        boolean any = false;
        try {
            for (; at < s.length(); at++) {
                char c = s.charAt(at);
                if (c == '.' && !dot) {
                    dot = true;
                } else if (c >= '0' && c <= '9') {
                    any = true;
                    if (!dot) {
                        whole = Math.addExact(Math.multiplyExact(whole, 10L), (long) (c - '0'));
                    } else if (fracDigits < digits) {
                        frac = frac * 10 + (c - '0');
                        fracDigits++;
                    } else if (c != '0') {
                        // A decimal the currency cannot hold: refused, never cut.
                        return NOT_AN_AMOUNT;
                    }
                } else {
                    return NOT_AN_AMOUNT;
                }
            }
            if (!any) {
                return NOT_AN_AMOUNT;
            }
            for (; fracDigits < digits; fracDigits++) {
                frac *= 10;
            }
            long minor = Math.addExact(Math.multiplyExact(whole, unit(digits)), frac);
            return negative ? -minor : minor;
        } catch (ArithmeticException tooLarge) {
            return NOT_AN_AMOUNT;
        }
    }

    /// `numerator / denominator` rounded half to even; 0 for a denominator
    /// that is not positive (the device VMs do not throw on division by zero).
    public static long divideHalfEven(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        boolean negative = numerator < 0;
        long n = negative ? -numerator : numerator;
        long q = n / denominator;
        long r = n - q * denominator;
        long twice = r * 2;
        if (twice > denominator || (twice == denominator && (q & 1) == 1)) {
            q++;
        }
        return negative ? -q : q;
    }

    /// `minor` with `fromDigits` decimals rewritten with `toDigits`, rounded
    /// half to even: 1250 at 2 digits is 12 at 0 and 12500 at 3.
    public static long rescale(long minor, int fromDigits, int toDigits) {
        if (toDigits >= fromDigits) {
            return Math.multiplyExact(minor, unit(toDigits - fromDigits));
        }
        return divideHalfEven(minor, unit(fromDigits - toDigits));
    }

    /// `minor` units of `from` in minor units of `to`, at `rateMicros` units of
    /// `to` per unit of `from` (millionths), rounded half to even once at the
    /// end. Throws `ArithmeticException` when the product overflows.
    public static long convert(long minor, String from, String to, long rateMicros) {
        int df = digits(from);
        int dt = digits(to);
        long scaled = Math.multiplyExact(minor, rateMicros);
        if (dt >= df) {
            return divideHalfEven(Math.multiplyExact(scaled, unit(dt - df)), MICROS);
        }
        return divideHalfEven(scaled, Math.multiplyExact(MICROS, unit(df - dt)));
    }

    /// An ASCII upper case of `code`, trimmed; never the locale's folding,
    /// which turns `i` into a dotted capital in Turkish.
    private static String upper(String code) {
        if (code == null) {
            return "";
        }
        String t = code.trim();
        StringBuilder b = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }
}
