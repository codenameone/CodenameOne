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
package com.codenameone.examples.bazaar.web.commerce;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.bazaar.Days;

/// Reading the back office's form fields: money typed as `12.50`, dates typed
/// as `yyyy-MM-dd`, whole numbers. By hand, character by character, with no
/// floating point and no regular expressions (the device class library has
/// none), and a 400 the page can show for anything that is not one.
final class AdminForms {
    private AdminForms() {
    }

    /// A form or query field, trimmed; empty when missing or malformed.
    static String field(HttpServer.Request request, String name) {
        String v;
        try {
            v = request.param(name);
        } catch (RuntimeException malformed) {
            v = null;
        }
        return v == null ? "" : v.trim();
    }

    /// `12.50`, `12.5`, `12` or `$1,234.50` as cents. Negative amounts and
    /// more than two decimals are refused.
    static long cents(String text, String what) {
        String t = text == null ? "" : text.trim();
        long whole = 0;
        long frac = 0;
        int fracDigits = -1;
        int digits = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '$' || c == ',' || (c == ' ' && fracDigits < 0)) {
                continue;
            }
            if (c == '.' && fracDigits < 0) {
                fracDigits = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                throw new ResponseStatusException(400, "Write " + what + " as an amount like 12.50");
            }
            if (fracDigits >= 0) {
                if (fracDigits == 2) {
                    throw new ResponseStatusException(400, what + " has at most two decimals");
                }
                frac = frac * 10 + (c - '0');
                fracDigits++;
            } else {
                if (whole > 10000000000L) {
                    throw new ResponseStatusException(400, what + " is too large");
                }
                whole = whole * 10 + (c - '0');
            }
            digits++;
        }
        if (digits == 0) {
            throw new ResponseStatusException(400, "Write " + what + " as an amount like 12.50");
        }
        if (fracDigits == 1) {
            frac *= 10;
        }
        return whole * 100 + frac;
    }

    /// A whole number, possibly negative; `fallback` when empty.
    static int number(String text, int fallback, String what) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "Write " + what + " as a whole number");
        }
    }

    /// The first millisecond (UTC) of a `yyyy-MM-dd` day, or -1 for anything else.
    static long dayStart(String iso) {
        if (!Days.isIso(iso)) {
            return -1;
        }
        long y = digits(iso, 0, 4);
        long m = digits(iso, 5, 7);
        long d = digits(iso, 8, 10);
        // The inverse of Days.iso: a year that starts in March.
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m > 2 ? m - 3 : m + 9) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468;
        return days * Days.MILLIS;
    }

    private static long digits(String s, int from, int to) {
        long out = 0;
        for (int iter = from; iter < to; iter++) {
            out = out * 10 + (s.charAt(iter) - '0');
        }
        return out;
    }
}
