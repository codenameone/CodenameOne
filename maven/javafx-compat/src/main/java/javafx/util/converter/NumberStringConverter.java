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
package javafx.util.converter;

import javafx.util.StringConverter;

/// Converts between `Number` and its text.
///
/// Numbers are written the way the default number format of an English
/// locale writes them, on every device: digits grouped in threes with a
/// comma, a dot before the fraction, at most three fraction digits rounded
/// half to even, and no trailing zeros. Parsing accepts the same form and
/// returns a `Long` for a whole number and a `Double` otherwise.
public class NumberStringConverter extends StringConverter<Number> {

    /// Creates the converter.
    public NumberStringConverter() {
    }

    /// Parses a number. Surrounding blanks are ignored and empty text is
    /// `null`.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: when the text is not a number
    @Override
    public Number fromString(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.length() < 1) {
            return null;
        }
        StringBuilder plain = new StringBuilder(text.length());
        boolean fraction = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ',' && !fraction) {
                continue;
            }
            if (c == '.' || c == 'e' || c == 'E') {
                fraction = true;
            }
            plain.append(c);
        }
        try {
            if (!fraction) {
                return Long.valueOf(Long.parseLong(plain.toString()));
            }
            double parsed = Double.parseDouble(plain.toString());
            if (parsed == Math.floor(parsed) && Math.abs(parsed) < 9.0e18) {
                return Long.valueOf((long) parsed);
            }
            return Double.valueOf(parsed);
        } catch (NumberFormatException malformed) {
            throw new RuntimeException("Unparseable number: \"" + value + "\"", malformed);
        }
    }

    @Override
    public String toString(Number value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            long whole = value.longValue();
            if (whole == Long.MIN_VALUE) {
                return "-9,223,372,036,854,775,808";
            }
            return (whole < 0 ? "-" : "") + group(Long.toString(Math.abs(whole)));
        }
        double number = value.doubleValue();
        if (Double.isNaN(number)) {
            return "NaN";
        }
        if (Double.isInfinite(number)) {
            return number > 0 ? "Infinity" : "-Infinity";
        }
        double magnitude = Math.abs(number);
        if (magnitude >= 9.0e15) {
            // Beyond this a double has no fraction digits left to print.
            return (number < 0 ? "-" : "") + group(plainDigits(magnitude));
        }
        long whole = (long) Math.floor(magnitude);
        long thousandths = roundHalfEven((magnitude - whole) * 1000.0);
        if (thousandths == 1000) {
            whole++;
            thousandths = 0;
        }
        StringBuilder result = new StringBuilder();
        if (number < 0 && (whole != 0 || thousandths != 0)) {
            result.append('-');
        }
        result.append(group(Long.toString(whole)));
        if (thousandths != 0) {
            String digits = Long.toString(thousandths);
            while (digits.length() < 3) {
                digits = "0" + digits;
            }
            int end = digits.length();
            while (digits.charAt(end - 1) == '0') {
                end--;
            }
            result.append('.').append(digits.substring(0, end));
        }
        return result.toString();
    }

    private static long roundHalfEven(double value) {
        double floor = Math.floor(value);
        double rest = value - floor;
        long result = (long) floor;
        if (rest > 0.5 || (rest == 0.5 && (result & 1) == 1)) {
            result++;
        }
        return result;
    }

    /// The digits of a whole number too large for a `long`.
    private static String plainDigits(double magnitude) {
        if (magnitude < 9.0e18) {
            return Long.toString((long) magnitude);
        }
        StringBuilder digits = new StringBuilder();
        double rest = magnitude;
        while (rest >= 1.0) {
            double tenth = Math.floor(rest / 10.0);
            digits.insert(0, (char) ('0' + (int) (rest - tenth * 10.0)));
            rest = tenth;
        }
        return digits.toString();
    }

    private static String group(String digits) {
        StringBuilder result = new StringBuilder(digits.length() + digits.length() / 3);
        int lead = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - lead) % 3 == 0) {
                result.append(',');
            }
            result.append(digits.charAt(i));
        }
        return result.toString();
    }
}
