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
package com.codenameone.examples.bazaar.trade;

import com.codenameone.examples.bazaar.api.VatCheckDto;

/// The format of EU VAT identification numbers: a two-letter country prefix
/// and the national number's shape, per the European Commission's published
/// formats.
///
/// This checks the **format only**. Whether a number is actually issued and
/// belongs to the company is answered by the Commission's VIES service,
/// which needs a network call to an external SOAP/REST endpoint, its own
/// failure handling (member-state services are often down) and records kept
/// of each consultation for audit. That is out of scope for this sample: a
/// production store would call VIES when a company is saved and store the
/// consultation number with it.
///
/// Patterns: `D` is a digit, `A` a letter, `X` either; anything else is
/// itself, and a lower case letter is that letter in upper case, literally.
/// Alternatives are separated by `|`.
public final class VatIds {
    private static final String[][] FORMATS = {
        {"AT", "UDDDDDDDD"},
        {"BE", "0DDDDDDDDD|1DDDDDDDDD"},
        {"BG", "DDDDDDDDD|DDDDDDDDDD"},
        {"CY", "DDDDDDDDA"},
        {"CZ", "DDDDDDDD|DDDDDDDDD|DDDDDDDDDD"},
        {"DE", "DDDDDDDDD"},
        {"DK", "DDDDDDDD"},
        {"EE", "DDDDDDDDD"},
        {"EL", "DDDDDDDDD"},
        {"ES", "XDDDDDDDX"},
        {"FI", "DDDDDDDD"},
        {"FR", "XXDDDDDDDDD"},
        {"HR", "DDDDDDDDDDD"},
        {"HU", "DDDDDDDD"},
        {"IE", "DDDDDDDA|DDDDDDDAA|DXDDDDDA"},
        {"IT", "DDDDDDDDDDD"},
        {"LT", "DDDDDDDDD|DDDDDDDDDDDD"},
        {"LU", "DDDDDDDD"},
        {"LV", "DDDDDDDDDDD"},
        {"MT", "DDDDDDDD"},
        {"NL", "DDDDDDDDDBDD"},
        {"PL", "DDDDDDDDDD"},
        {"PT", "DDDDDDDDD"},
        {"RO", "DD|DDD|DDDD|DDDDD|DDDDDD|DDDDDDD|DDDDDDDD|DDDDDDDDD|DDDDDDDDDD"},
        {"SE", "DDDDDDDDDD01"},
        {"SI", "DDDDDDDD"},
        {"SK", "DDDDDDDDDD"},
        {"XI", "DDDDDDDDD|DDDDDDDDDDDD|gdDDD|haDDD"},
    };

    private VatIds() {
    }

    /// `typed` with spaces, dashes and dots dropped and letters upper case,
    /// folded by hand rather than by the locale.
    public static String normalize(String typed) {
        if (typed == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(typed.length());
        for (int iter = 0; iter < typed.length(); iter++) {
            char c = typed.charAt(iter);
            if (c == ' ' || c == '-' || c == '.' || c == '\t') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            out.append(c);
        }
        return out.toString();
    }

    /// Checks `typed`'s format and says why it fails.
    public static VatCheckDto check(String typed) {
        String id = normalize(typed);
        if (id.length() < 4 || id.length() > 14) {
            return new VatCheckDto(id, false, "", "A VAT id is a country prefix and 2 to 12 more characters");
        }
        String country = id.substring(0, 2);
        if ("GR".equals(country)) {
            return new VatCheckDto(id, false, country, "Greek VAT ids start with EL, not GR");
        }
        String formats = null;
        for (int iter = 0; iter < FORMATS.length; iter++) {
            if (FORMATS[iter][0].equals(country)) {
                formats = FORMATS[iter][1];
            }
        }
        if (formats == null) {
            return new VatCheckDto(id, false, country, country + " is not an EU VAT prefix");
        }
        String body = id.substring(2);
        int start = 0;
        while (start <= formats.length()) {
            int bar = formats.indexOf('|', start);
            String pattern = formats.substring(start, bar < 0 ? formats.length() : bar);
            if (fits(body, pattern)) {
                return new VatCheckDto(id, true, country, "The format is valid");
            }
            if (bar < 0) {
                break;
            }
            start = bar + 1;
        }
        return new VatCheckDto(id, false, country, "That is not the format of a " + country
                + " VAT id");
    }

    /// Whether `typed` has a valid EU VAT id format.
    public static boolean valid(String typed) {
        return check(typed).valid();
    }

    private static boolean fits(String body, String pattern) {
        if (body.length() != pattern.length()) {
            return false;
        }
        for (int iter = 0; iter < body.length(); iter++) {
            char c = body.charAt(iter);
            char p = pattern.charAt(iter);
            boolean digit = c >= '0' && c <= '9';
            boolean letter = c >= 'A' && c <= 'Z';
            if (p == 'D' ? !digit : p == 'A' ? !letter : p == 'X' ? !(digit || letter)
                    : p >= 'a' && p <= 'z' ? c != (char) (p - 'a' + 'A') : c != p) {
                return false;
            }
        }
        return true;
    }
}
