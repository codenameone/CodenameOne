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
package com.codenameone.examples.bazaar.channel;

/// Decimal money strings (`"12.3"`, `"12.34"`) to cents and back, without
/// floating point: marketplaces answer prices as decimal strings.
public final class Cents {
    private Cents() {
    }

    /// Cents of a decimal string; more than two decimals are cut. 0 when it is
    /// not a number or would not fit.
    static long parse(Object value) {
        String s = value == null ? "" : String.valueOf(value).trim();
        boolean negative = s.startsWith("-");
        int at = negative ? 1 : 0;
        long whole = 0;
        long frac = 0;
        int fracDigits = 0;
        boolean dot = false;
        if (at >= s.length() || s.length() > 18) {
            return 0;
        }
        for (; at < s.length(); at++) {
            char c = s.charAt(at);
            if (c == '.' && !dot) {
                dot = true;
            } else if (c >= '0' && c <= '9') {
                if (!dot) {
                    whole = whole * 10 + (c - '0');
                } else if (fracDigits < 2) {
                    frac = frac * 10 + (c - '0');
                    fracDigits++;
                }
            } else {
                return 0;
            }
        }
        if (fracDigits == 1) {
            frac *= 10;
        }
        long cents = whole * 100 + frac;
        return negative ? -cents : cents;
    }

    /// `1234` as `"12.34"`.
    public static String format(long cents) {
        long abs = Math.abs(cents);
        long f = abs % 100;
        return (cents < 0 ? "-" : "") + abs / 100 + "." + (f < 10 ? "0" : "") + f;
    }
}
