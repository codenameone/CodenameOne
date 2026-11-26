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
package com.codenameone.examples.bazaar.merchant;

/// A commission rate typed as a percentage ("5", "7.5", "12.25%") and kept
/// in basis points, and shown back the same way. Plain, for tests.
public final class CommissionRate {
    private CommissionRate() {
    }

    /// Basis points, or -1 when `typed` is not a percentage from 0 to 100
    /// with at most two decimals.
    public static long parse(String typed) {
        if (typed == null) {
            return -1;
        }
        String t = typed.trim();
        if (t.endsWith("%")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        if (t.length() == 0) {
            return -1;
        }
        long whole = 0;
        long fraction = 0;
        int decimals = -1;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '.' || c == ',') {
                if (decimals >= 0) {
                    return -1;
                }
                decimals = 0;
            } else if (c >= '0' && c <= '9') {
                if (decimals < 0) {
                    whole = whole * 10 + (c - '0');
                    if (whole > 100) {
                        return -1;
                    }
                } else {
                    if (++decimals > 2) {
                        return -1;
                    }
                    fraction = fraction * 10 + (c - '0');
                }
            } else {
                return -1;
            }
        }
        if (decimals == 1) {
            fraction *= 10;
        }
        long bp = whole * 100 + fraction;
        return bp > 10000 ? -1 : bp;
    }

    /// `bp` as a percentage, without trailing zeros: 500 is "5%", 750 "7.5%".
    public static String show(long bp) {
        long whole = bp / 100;
        long rest = bp % 100;
        if (rest == 0) {
            return whole + "%";
        }
        return whole + "." + (rest % 10 == 0 ? Long.toString(rest / 10)
                : rest < 10 ? "0" + rest : Long.toString(rest)) + "%";
    }
}
