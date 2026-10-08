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
package com.codename1.compat.jdk;

import com.codename1.util.BigInteger;

/// `java.text.DecimalFormat` for the Codename One runtime: formats and
/// parses decimal numbers according to a pattern.
///
/// #### Pattern syntax
///
/// A pattern is a positive subpattern, optionally followed by `;` and a
/// negative subpattern of which only the prefix and suffix are used. Each
/// subpattern is a prefix, a number and a suffix. In the number:
///
/// - `0` is a digit that is always written, `#` a digit written only when
///   it is significant.
/// - `.` is the decimal separator and `,` the grouping separator; the
///   distance from the last `,` to the end of the integer part is the group
///   size.
/// - `E` followed by one or more `0` selects scientific notation, with at
///   least that many exponent digits. With more integer digit positions than
///   `0`s among them (`##0.###E0`), the exponent is kept a multiple of the
///   number of integer positions.
///
/// In a prefix or suffix, `%` multiplies the number by 100 and writes a
/// percent sign, the per mille sign multiplies by 1000, the currency sign
/// writes the currency symbol (doubled, the international currency code), and
/// `-` writes the minus sign. A single quote makes the characters up to the
/// next one literal, and two in a row stand for one quote.
///
/// The characters actually written -- separators, minus, percent, currency
/// -- come from the format's [DecimalFormatSymbols].
///
/// #### Rounding
///
/// Rounding is half-even, the JDK default, and like the JDK it is decided by
/// the exact binary value of a `double`: `0.125` is a true tie and formats as
/// `0.12` under `0.00`, while `0.15`, which as a `double` lies just below the
/// decimal `0.15`, formats as `0.1` under `0.0`.
///
/// The JDK departs from half-even in two places, and so does this class,
/// because a last digit that differs from the desktop's is worse than either
/// answer: a whole number rounds a tie up in scientific notation (`12345`
/// under `0.###E0` is `1.235E4`), and `5E-4`, `5E-5` and so on round down
/// when the 5 is the first digit dropped (`0.0005` under `0.000` is `0.000`).
///
/// #### Differences from the JDK
///
/// - No `setRoundingMode`, `setCurrency`, `setParseBigDecimal` or
///   `formatToCharacterIterator`: the device has none of the types they name.
/// - No localized patterns (`applyLocalizedPattern`, `toLocalizedPattern`).
/// - It is not a `java.text.Format`; see [NumberFormat].
public class DecimalFormat extends NumberFormat {

    private static final long serialVersionUID = 1L;

    /// The currency sign of a pattern. Not written as a character literal:
    /// the sources are ASCII.
    static final char CURRENCY_SIGN = (char) 0xA4;

    private static final char PER_MILLE_SIGN = (char) 0x2030;
    private static final char QUOTE = '\'';

    /// A `double` has no more integer digits than this, nor fraction digits
    /// than the next; the JDK caps the settings there for a `double` as well.
    private static final int DOUBLE_INTEGER_DIGITS = 309;
    private static final int DOUBLE_FRACTION_DIGITS = 340;

    private DecimalFormatSymbols symbols;

    // The affixes as the pattern gave them, in an internal form where a quote
    // precedes each character to be replaced by a symbol and '' is a quote.
    private String posPrefixPattern = "";
    private String posSuffixPattern = "";
    private String negPrefixPattern = "'-";
    private String negSuffixPattern = "";

    // The same affixes with the symbols filled in.
    private String positivePrefix = "";
    private String positiveSuffix = "";
    private String negativePrefix = "-";
    private String negativeSuffix = "";

    private int multiplier = 1;
    private int groupingSize = 3;
    private boolean decimalSeparatorAlwaysShown;
    private boolean useExponentialNotation;
    private int minExponentDigits;
    private boolean currencyFormat;

    public DecimalFormat() {
        this("#,##0.###", DecimalFormatSymbols.getInstance());
    }

    public DecimalFormat(String pattern) {
        this(pattern, DecimalFormatSymbols.getInstance());
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        if (symbols == null) {
            throw new NullPointerException();
        }
        Object copy = symbols.clone();
        this.symbols = copy instanceof DecimalFormatSymbols ? (DecimalFormatSymbols) copy : symbols;
        parsePattern(pattern);
    }

    // ------------------------------------------------------------------
    // Digits
    // ------------------------------------------------------------------

    /// A non-negative decimal number: `count` significant digits, with the
    /// decimal point `decimalAt` digits from the left (so the value is
    /// 0.d1d2... times ten to the `decimalAt`). No trailing zeros; zero is no
    /// digits at all.
    private static final class Digits {
        /// The digits are the number: a tie goes to the even neighbour.
        static final int TIE_EVEN = 0;
        /// The digits are the shortest text of a `double`: a tie is settled
        /// by where the `double` really lies, and is even only if it lies
        /// exactly there.
        static final int TIE_BY_VALUE = 1;
        static final int TIE_UP = 2;
        static final int TIE_DOWN = 3;

        private char[] d = new char[24];
        private int count;
        private int decimalAt;

        private void append(char c) {
            if (count == d.length) {
                char[] bigger = new char[d.length * 2];
                System.arraycopy(d, 0, bigger, 0, count);
                d = bigger;
            }
            d[count++] = c;
        }

        private void trim() {
            while (count > 0 && d[count - 1] == '0') {
                count--;
            }
            if (count == 0) {
                decimalAt = 0;
            }
        }

        /// Takes plain decimal digits, an integer.
        void setInteger(String digits) {
            count = 0;
            int i = 0;
            while (i < digits.length() && digits.charAt(i) == '0') {
                i++;
            }
            decimalAt = digits.length() - i;
            for (; i < digits.length(); i++) {
                append(digits.charAt(i));
            }
            trim();
        }

        /// Takes the shortest decimal text that identifies a finite,
        /// non-negative `double`, as `Double.toString` writes it: digits with
        /// a decimal point, and an optional exponent.
        void setDouble(double value) {
            String s = Double.toString(value);
            int exponent = 0;
            int e = s.indexOf('E');
            if (e < 0) {
                e = s.indexOf('e');
            }
            int end = s.length();
            if (e >= 0) {
                exponent = parseExponent(s, e + 1);
                end = e;
            }
            count = 0;
            int pointAt = -1;
            int seen = 0;
            boolean leading = true;
            int leadingZerosAfterPoint = 0;
            for (int i = 0; i < end; i++) {
                char c = s.charAt(i);
                if (c == '.' || c == ',') {
                    // Some ports write the device locale's decimal comma.
                    pointAt = seen;
                    continue;
                }
                if (c < '0' || c > '9') {
                    continue;
                }
                if (leading && c == '0') {
                    if (pointAt >= 0) {
                        leadingZerosAfterPoint++;
                    }
                    continue;
                }
                leading = false;
                append(c);
                seen++;
            }
            if (pointAt < 0) {
                pointAt = seen;
            }
            decimalAt = pointAt - leadingZerosAfterPoint + exponent;
            trim();
        }

        private static int parseExponent(String s, int from) {
            boolean negative = false;
            int i = from;
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                negative = s.charAt(i) == '-';
                i++;
            }
            int value = 0;
            for (; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    value = value * 10 + (c - '0');
                }
            }
            return negative ? -value : value;
        }

        /// Multiplies by a positive integer.
        void multiply(int by) {
            if (count == 0) {
                return;
            }
            char[] out = new char[count + 11];
            int at = out.length;
            long carry = 0;
            for (int i = count - 1; i >= 0; i--) {
                long v = (long) (d[i] - '0') * by + carry;
                out[--at] = (char) ('0' + (int) (v % 10));
                carry = v / 10;
            }
            while (carry > 0) {
                out[--at] = (char) ('0' + (int) (carry % 10));
                carry = carry / 10;
            }
            int produced = out.length - at;
            decimalAt += produced - count;
            count = 0;
            for (int i = at; i < out.length; i++) {
                append(out[i]);
            }
            trim();
        }

        /// Keeps the first `keep` digits, rounding half-even. `tie` says how
        /// digits that end in exactly a half are settled; `value` is the
        /// `double` they were taken from, for [#TIE_BY_VALUE].
        void round(int keep, int tie, double value) {
            if (keep >= count) {
                return;
            }
            if (keep < 0) {
                count = 0;
                decimalAt = 0;
                return;
            }
            if (roundsUp(keep, tie, value)) {
                int i = keep - 1;
                while (i >= 0 && d[i] == '9') {
                    i--;
                }
                if (i < 0) {
                    d[0] = '1';
                    count = 1;
                    decimalAt++;
                } else {
                    d[i]++;
                    count = i + 1;
                }
            } else {
                count = keep;
            }
            trim();
        }

        private boolean roundsUp(int keep, int tie, double value) {
            char next = d[keep];
            if (next != '5') {
                return next > '5';
            }
            if (keep + 1 < count) {
                // Past the 5 there is something, and it is not zero.
                return true;
            }
            // The digits end in exactly a half.
            if (tie == TIE_UP) {
                return true;
            }
            if (tie == TIE_DOWN) {
                return false;
            }
            // A long is those digits. A double is only the nearest double to
            // them, so the true value is compared against the half it
            // appears to be.
            int side = tie == TIE_EVEN ? 0 : compareToDigits(value);
            if (side != 0) {
                return side > 0;
            }
            return keep > 0 && ((d[keep - 1] - '0') & 1) == 1;
        }

        /// The sign of `value` minus the number these digits spell.
        private int compareToDigits(double value) {
            long bits = Double.doubleToLongBits(value);
            int rawExponent = (int) ((bits >> 52) & 0x7ffL);
            long mantissa = bits & 0xfffffffffffffL;
            int binaryExponent;
            if (rawExponent == 0) {
                binaryExponent = -1074;
            } else {
                mantissa |= 1L << 52;
                binaryExponent = rawExponent - 1075;
            }
            // value = mantissa * 2^binaryExponent, digits = n * 10^decimalExponent.
            // Negative powers move to the other side, leaving integers.
            BigInteger left = BigInteger.valueOf(mantissa);
            BigInteger right = new BigInteger(new String(d, 0, count));
            int decimalExponent = decimalAt - count;
            if (binaryExponent > 0) {
                left = left.shiftLeft(binaryExponent);
            } else if (binaryExponent < 0) {
                right = right.shiftLeft(-binaryExponent);
            }
            BigInteger ten = BigInteger.valueOf(10);
            if (decimalExponent > 0) {
                right = right.multiply(ten.pow(decimalExponent));
            } else if (decimalExponent < 0) {
                left = left.multiply(ten.pow(-decimalExponent));
            }
            return left.compareTo(right);
        }
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        if (Double.isNaN(number)) {
            int begin = result.length();
            result.append(symbols.getNaN());
            if (fieldPosition.getField() == INTEGER_FIELD) {
                fieldPosition.setBeginIndex(begin);
                fieldPosition.setEndIndex(result.length());
            }
            return result;
        }
        // The sign bit, so that negative zero is negative.
        boolean negative = (Double.doubleToLongBits(number) < 0) ^ (multiplier < 0);
        double value = number;
        if (multiplier != 1) {
            value = value * multiplier;
        }
        if (Double.isInfinite(value)) {
            result.append(negative ? negativePrefix : positivePrefix);
            int begin = result.length();
            result.append(symbols.getInfinity());
            if (fieldPosition.getField() == INTEGER_FIELD) {
                fieldPosition.setBeginIndex(begin);
                fieldPosition.setEndIndex(result.length());
            }
            result.append(negative ? negativeSuffix : positiveSuffix);
            return result;
        }
        value = Math.abs(value);
        int maxInt = Math.min(getMaximumIntegerDigits(), DOUBLE_INTEGER_DIGITS);
        int minInt = Math.min(getMinimumIntegerDigits(), DOUBLE_INTEGER_DIGITS);
        int maxFrac = Math.min(getMaximumFractionDigits(), DOUBLE_FRACTION_DIGITS);
        int minFrac = Math.min(getMinimumFractionDigits(), DOUBLE_FRACTION_DIGITS);
        Digits digits = new Digits();
        digits.setDouble(value);
        // Two places where the JDK does not round half-even, kept because
        // output that differs from the desktop's in the last digit is worse
        // than either answer. A whole number below two to the 63rd rounds a
        // tie up (12345 under 0.###E0 is 1.235E4), and 5E-4, 5E-5, ... round
        // down when the 5 is the first digit dropped (0.0005 under 0.000 is
        // 0.000, though 0.005 under 0.00 is 0.01).
        int tie = Digits.TIE_BY_VALUE;
        if (value < 9.2E18 && (double) (long) value == value) {
            tie = Digits.TIE_UP;
        }
        if (useExponentialNotation) {
            digits.round(maxInt + maxFrac, tie, value);
        } else {
            int keep = digits.decimalAt + maxFrac;
            if (keep == 0 && value < 1.0E-3 && !isCommonShape(maxInt, minInt, maxFrac, minFrac)) {
                tie = Digits.TIE_DOWN;
            }
            digits.round(keep, tie, value);
        }
        return subformat(result, fieldPosition, digits, negative, false, maxInt, minInt, maxFrac, minFrac);
    }

    /// The shapes the JDK formats by a separate, exact route -- the default
    /// number and currency patterns -- where neither exception applies.
    private boolean isCommonShape(int maxInt, int minInt, int maxFrac, int minFrac) {
        if (!isGroupingUsed() || groupingSize != 3 || multiplier != 1 || decimalSeparatorAlwaysShown
                || minInt != 1 || maxInt < 10) {
            return false;
        }
        return currencyFormat ? minFrac == 2 && maxFrac == 2 : minFrac == 0 && maxFrac == 3;
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        boolean negative = number < 0;
        String text = Long.toString(number);
        Digits digits = new Digits();
        digits.setInteger(negative ? text.substring(1) : text);
        if (multiplier != 1) {
            if (multiplier == 0) {
                digits.setInteger("0");
            } else if (multiplier < 0) {
                negative = !negative;
                if (multiplier == Integer.MIN_VALUE) {
                    digits.multiply(1 << 30);
                    digits.multiply(2);
                } else {
                    digits.multiply(-multiplier);
                }
            } else {
                digits.multiply(multiplier);
            }
        }
        int maxInt = getMaximumIntegerDigits();
        int minInt = getMinimumIntegerDigits();
        int maxFrac = getMaximumFractionDigits();
        int minFrac = getMinimumFractionDigits();
        if (useExponentialNotation) {
            int significant = maxInt + maxFrac;
            if (significant > 0) {
                digits.round(significant, Digits.TIE_EVEN, 0);
            }
        }
        if (digits.count == 0) {
            // An integer zero has no sign.
            negative = false;
        }
        return subformat(result, fieldPosition, digits, negative, true, maxInt, minInt, maxFrac, minFrac);
    }

    private char digitChar(char asciiDigit) {
        return (char) (asciiDigit - '0' + symbols.getZeroDigit());
    }

    private StringBuffer subformat(StringBuffer result, FieldPosition fieldPosition, Digits digits,
                                   boolean negative, boolean isInteger,
                                   int maxInt, int minInt, int maxFrac, int minFrac) {
        char zero = symbols.getZeroDigit();
        char decimal = currencyFormat ? symbols.getMonetaryDecimalSeparator() : symbols.getDecimalSeparator();
        result.append(negative ? negativePrefix : positivePrefix);
        int integerBegin = result.length();
        int integerEnd = -1;
        int fractionBegin = -1;
        if (useExponentialNotation) {
            // The exponent is what leaves the wanted number of integer
            // digits; with more integer positions than required digits it is
            // also kept a multiple of the number of positions.
            int exponent = digits.decimalAt;
            int repeat = maxInt;
            int minimumIntegerDigits = minInt;
            if (repeat > 1 && repeat > minInt) {
                if (exponent >= 1) {
                    exponent = ((exponent - 1) / repeat) * repeat;
                } else {
                    exponent = ((exponent - repeat) / repeat) * repeat;
                }
                minimumIntegerDigits = 1;
            } else {
                exponent -= minimumIntegerDigits;
            }
            int minimumDigits = minInt + minFrac;
            if (minimumDigits < 0) {
                minimumDigits = Integer.MAX_VALUE;
            }
            int integerDigits = digits.count == 0 ? minimumIntegerDigits : digits.decimalAt - exponent;
            if (minimumDigits < integerDigits) {
                minimumDigits = integerDigits;
            }
            int totalDigits = Math.max(digits.count, minimumDigits);
            boolean wroteDecimal = false;
            for (int i = 0; i < totalDigits; i++) {
                if (i == integerDigits) {
                    integerEnd = result.length();
                    result.append(decimal);
                    wroteDecimal = true;
                    fractionBegin = result.length();
                }
                result.append(i < digits.count ? digitChar(digits.d[i]) : zero);
            }
            if (decimalSeparatorAlwaysShown && totalDigits == integerDigits) {
                integerEnd = result.length();
                result.append(decimal);
                wroteDecimal = true;
                fractionBegin = result.length();
            }
            if (integerEnd < 0) {
                integerEnd = result.length();
            }
            if (!wroteDecimal) {
                fractionBegin = result.length();
            }
            int fractionEnd = result.length();
            result.append(symbols.getExponentSeparator());
            if (digits.count == 0) {
                exponent = 0;
            }
            if (exponent < 0) {
                exponent = -exponent;
                result.append(symbols.getMinusSign());
            }
            String exponentText = Integer.toString(exponent);
            for (int i = exponentText.length(); i < minExponentDigits; i++) {
                result.append(zero);
            }
            for (int i = 0; i < exponentText.length(); i++) {
                result.append(digitChar(exponentText.charAt(i)));
            }
            setField(fieldPosition, integerBegin, integerEnd, fractionBegin, fractionEnd);
        } else {
            int count = minInt;
            int digitIndex = 0;
            if (digits.decimalAt > 0 && count < digits.decimalAt) {
                count = digits.decimalAt;
            }
            // With fewer integer digits allowed than the number has, the
            // least significant ones are the ones written: 1997 under a
            // maximum of two is "97".
            if (count > maxInt) {
                count = maxInt;
                digitIndex = digits.decimalAt - count;
            }
            boolean grouping = isGroupingUsed() && groupingSize > 0;
            for (int i = count - 1; i >= 0; i--) {
                if (i < digits.decimalAt && digitIndex < digits.count) {
                    result.append(digitChar(digits.d[digitIndex++]));
                } else {
                    result.append(zero);
                }
                // groupingSize is checked positive just above.
                if (grouping && i > 0 && i % groupingSize == 0) {
                    result.append(symbols.getGroupingSeparator());
                }
            }
            boolean fractionPresent = minFrac > 0 || (!isInteger && digitIndex < digits.count);
            if (!fractionPresent && result.length() == integerBegin) {
                result.append(zero);
            }
            integerEnd = result.length();
            if (decimalSeparatorAlwaysShown || fractionPresent) {
                result.append(decimal);
            }
            fractionBegin = result.length();
            for (int i = 0; i < maxFrac; i++) {
                if (i >= minFrac && (isInteger || digitIndex >= digits.count)) {
                    break;
                }
                // Zeros between the decimal point and the first digit.
                if (-1 - i > digits.decimalAt - 1) {
                    result.append(zero);
                    continue;
                }
                if (!isInteger && digitIndex < digits.count) {
                    result.append(digitChar(digits.d[digitIndex++]));
                } else {
                    result.append(zero);
                }
            }
            setField(fieldPosition, integerBegin, integerEnd, fractionBegin, result.length());
        }
        result.append(negative ? negativeSuffix : positiveSuffix);
        return result;
    }

    private static void setField(FieldPosition fieldPosition, int integerBegin, int integerEnd,
                                 int fractionBegin, int fractionEnd) {
        if (fieldPosition.getField() == INTEGER_FIELD) {
            fieldPosition.setBeginIndex(integerBegin);
            fieldPosition.setEndIndex(integerEnd);
        } else if (fieldPosition.getField() == FRACTION_FIELD) {
            fieldPosition.setBeginIndex(fractionBegin);
            fieldPosition.setEndIndex(fractionEnd);
        }
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private int digitValue(char c) {
        int v = c - symbols.getZeroDigit();
        if (v >= 0 && v <= 9) {
            return v;
        }
        v = c - '0';
        return v >= 0 && v <= 9 ? v : -1;
    }

    private static boolean matches(String text, int at, String part) {
        return text.regionMatches(at, part, 0, part.length());
    }

    @Override
    public Number parse(String text, ParsePosition pos) {
        int start = pos.getIndex();
        int position = start;
        String nan = symbols.getNaN();
        if (nan.length() > 0 && matches(text, position, nan)) {
            pos.setIndex(position + nan.length());
            return Double.valueOf(Double.NaN);
        }
        boolean gotPositive = matches(text, position, positivePrefix);
        boolean gotNegative = matches(text, position, negativePrefix);
        if (gotPositive && gotNegative) {
            if (positivePrefix.length() > negativePrefix.length()) {
                gotNegative = false;
            } else if (positivePrefix.length() < negativePrefix.length()) {
                gotPositive = false;
            }
        }
        if (gotPositive) {
            position += positivePrefix.length();
        } else if (gotNegative) {
            position += negativePrefix.length();
        } else {
            pos.setErrorIndex(position);
            return null;
        }

        boolean infinite = false;
        StringBuilder digits = new StringBuilder();
        int decimalAt = 0;
        String infinity = symbols.getInfinity();
        if (infinity.length() > 0 && matches(text, position, infinity)) {
            position += infinity.length();
            infinite = true;
        } else {
            char decimal = currencyFormat ? symbols.getMonetaryDecimalSeparator() : symbols.getDecimalSeparator();
            char grouping = symbols.getGroupingSeparator();
            String exponentText = symbols.getExponentSeparator();
            boolean sawDecimal = false;
            boolean sawDigit = false;
            int exponent = 0;
            int digitCount = 0;
            int backup = -1;
            for (; position < text.length(); position++) {
                char ch = text.charAt(position);
                int digit = digitValue(ch);
                if (digit >= 0) {
                    backup = -1;
                    sawDigit = true;
                    if (digit == 0 && digits.length() == 0) {
                        // Leading zeros carry no digits, but after the
                        // decimal point they shift it.
                        if (sawDecimal) {
                            decimalAt--;
                        }
                    } else {
                        digitCount++;
                        digits.append((char) ('0' + digit));
                    }
                } else if (ch == decimal) {
                    if (isParseIntegerOnly() || sawDecimal) {
                        break;
                    }
                    decimalAt = digitCount;
                    sawDecimal = true;
                } else if (ch == grouping && isGroupingUsed()) {
                    if (sawDecimal) {
                        break;
                    }
                    // A grouping separator must be followed by a digit; if
                    // it is not, the number ended before it.
                    backup = position;
                } else if (exponentText.length() > 0 && matches(text, position, exponentText)) {
                    int at = position + exponentText.length();
                    boolean negativeExponent = false;
                    if (at < text.length() && text.charAt(at) == symbols.getMinusSign()) {
                        negativeExponent = true;
                        at++;
                    }
                    int exponentStart = at;
                    long parsed = 0;
                    while (at < text.length() && digitValue(text.charAt(at)) >= 0) {
                        if (parsed < 100000000L) {
                            parsed = parsed * 10 + digitValue(text.charAt(at));
                        }
                        at++;
                    }
                    if (at > exponentStart) {
                        exponent = (int) (negativeExponent ? -parsed : parsed);
                        position = at;
                    }
                    break;
                } else {
                    break;
                }
            }
            if (backup != -1) {
                position = backup;
            }
            if (!sawDecimal) {
                decimalAt = digitCount;
            }
            decimalAt += exponent;
            if (!sawDigit) {
                // As in the JDK, a prefix with no number after it is reported
                // at the start of the prefix.
                pos.setErrorIndex(start);
                return null;
            }
        }

        if (gotPositive) {
            gotPositive = matches(text, position, positiveSuffix);
        }
        if (gotNegative) {
            gotNegative = matches(text, position, negativeSuffix);
        }
        if (gotPositive && gotNegative) {
            if (positiveSuffix.length() > negativeSuffix.length()) {
                gotNegative = false;
            } else if (positiveSuffix.length() < negativeSuffix.length()) {
                gotPositive = false;
            }
        }
        if (gotPositive == gotNegative) {
            pos.setErrorIndex(position);
            return null;
        }
        position += gotPositive ? positiveSuffix.length() : negativeSuffix.length();
        pos.setIndex(position);

        if (infinite) {
            boolean positiveResult = gotPositive == (multiplier >= 0);
            return Double.valueOf(positiveResult ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        return toNumber(digits, decimalAt, gotPositive);
    }

    private static final String LONG_MIN_DIGITS = "9223372036854775808";

    /// Whether these digits, an integer with `decimalAt` digits before the
    /// point, fit a `long` of the given sign.
    private static boolean fitsLong(StringBuilder digits, int decimalAt, boolean positive) {
        int count = digits.length();
        if (decimalAt < count || decimalAt > LONG_MIN_DIGITS.length()) {
            return false;
        }
        if (decimalAt < LONG_MIN_DIGITS.length()) {
            return true;
        }
        for (int i = 0; i < count; i++) {
            char mine = digits.charAt(i);
            char limit = LONG_MIN_DIGITS.charAt(i);
            if (mine > limit) {
                return false;
            }
            if (mine < limit) {
                return true;
            }
        }
        if (count < decimalAt) {
            return true;
        }
        // Exactly the magnitude of Long.MIN_VALUE.
        return !positive;
    }

    private Number toNumber(StringBuilder digits, int decimalAt, boolean positive) {
        while (digits.length() > 0 && digits.charAt(digits.length() - 1) == '0') {
            digits.setLength(digits.length() - 1);
        }
        boolean gotDouble;
        long longResult = 0;
        double doubleResult = 0.0;
        if (digits.length() == 0) {
            if (positive || isParseIntegerOnly()) {
                gotDouble = false;
            } else {
                gotDouble = true;
                doubleResult = -0.0;
            }
        } else if (fitsLong(digits, decimalAt, positive)) {
            gotDouble = false;
            StringBuilder sb = new StringBuilder();
            if (!positive) {
                sb.append('-');
            }
            sb.append(digits.toString());
            for (int i = digits.length(); i < decimalAt; i++) {
                sb.append('0');
            }
            longResult = Long.parseLong(sb.toString());
        } else {
            gotDouble = true;
            StringBuilder sb = new StringBuilder();
            if (!positive) {
                sb.append('-');
            }
            sb.append(digits.charAt(0)).append('.');
            if (digits.length() > 1) {
                sb.append(digits.toString().substring(1));
            } else {
                sb.append('0');
            }
            sb.append('E').append(decimalAt - 1);
            doubleResult = Double.parseDouble(sb.toString());
        }
        // A multiplier of zero formats everything as zero; there is nothing
        // to divide back by.
        if (multiplier != 1 && multiplier != 0) {
            if (gotDouble) {
                doubleResult = doubleResult / multiplier;
            } else if (longResult % multiplier == 0) {
                longResult = longResult / multiplier;
            } else {
                doubleResult = (double) longResult / multiplier;
                gotDouble = true;
            }
            if (gotDouble) {
                long whole = (long) doubleResult;
                boolean negativeZero = doubleResult == 0.0 && Double.doubleToLongBits(doubleResult) < 0;
                if (doubleResult == (double) whole && !negativeZero) {
                    gotDouble = false;
                    longResult = whole;
                } else if (isParseIntegerOnly()) {
                    gotDouble = false;
                    longResult = whole;
                }
            }
        }
        if (gotDouble) {
            return Double.valueOf(doubleResult);
        }
        return Long.valueOf(longResult);
    }

    // ------------------------------------------------------------------
    // Pattern
    // ------------------------------------------------------------------

    public void applyPattern(String pattern) {
        parsePattern(pattern);
    }

    private static IllegalArgumentException malformed(String why, String pattern) {
        return new IllegalArgumentException(why + " in pattern \"" + pattern + '"');
    }

    private static boolean isNumberChar(char ch) {
        return ch == '#' || ch == '0' || ch == ',' || ch == '.';
    }

    private void parsePattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        int len = pattern.length();
        boolean gotNegative = false;
        boolean exponential = false;
        boolean currency = false;
        int exponentDigits = 0;
        int newMultiplier = 1;
        String newPosPrefix = "";
        String newPosSuffix = "";
        String newNegPrefix = "";
        String newNegSuffix = "";
        int digitLeftCount = 0;
        int zeroDigitCount = 0;
        int digitRightCount = 0;
        int decimalPos = -1;
        int groupingCount = -1;
        int numberLength = 0;

        int start = 0;
        for (int part = 0; part < 2 && start < len; part++) {
            boolean positive = part == 0;
            StringBuilder prefix = new StringBuilder();
            StringBuilder suffix = new StringBuilder();
            StringBuilder affix = prefix;
            int phase = 0;
            boolean inQuote = false;
            boolean sawSeparator = false;
            int partMultiplier = 1;
            int toSkip = numberLength;
            int pos = start;
            while (pos < len) {
                char ch = pattern.charAt(pos);
                if (phase == 1) {
                    if (!positive) {
                        // Only the affixes of the negative subpattern count;
                        // its number is skipped, as long as the positive one.
                        if (toSkip > 0) {
                            toSkip--;
                            pos++;
                            continue;
                        }
                        phase = 2;
                        affix = suffix;
                        continue;
                    }
                    if (ch == '#') {
                        if (zeroDigitCount > 0) {
                            digitRightCount++;
                        } else {
                            digitLeftCount++;
                        }
                        if (groupingCount >= 0 && decimalPos < 0) {
                            groupingCount++;
                        }
                    } else if (ch == '0') {
                        if (digitRightCount > 0) {
                            throw malformed("Unexpected '0'", pattern);
                        }
                        zeroDigitCount++;
                        if (groupingCount >= 0 && decimalPos < 0) {
                            groupingCount++;
                        }
                    } else if (ch == ',') {
                        groupingCount = 0;
                    } else if (ch == '.') {
                        if (decimalPos >= 0) {
                            throw malformed("Multiple decimal separators", pattern);
                        }
                        decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                    } else if (ch == 'E') {
                        if (exponential) {
                            throw malformed("Multiple exponential symbols", pattern);
                        }
                        exponential = true;
                        exponentDigits = 0;
                        while (pos + 1 < len && pattern.charAt(pos + 1) == '0') {
                            exponentDigits++;
                            numberLength++;
                            pos++;
                        }
                        if (digitLeftCount + zeroDigitCount < 1 || exponentDigits < 1) {
                            throw malformed("Malformed exponential pattern", pattern);
                        }
                        numberLength++;
                        pos++;
                        phase = 2;
                        affix = suffix;
                        continue;
                    } else {
                        phase = 2;
                        affix = suffix;
                        continue;
                    }
                    numberLength++;
                    pos++;
                    continue;
                }
                // A prefix or a suffix.
                if (inQuote) {
                    if (ch == QUOTE) {
                        if (pos + 1 < len && pattern.charAt(pos + 1) == QUOTE) {
                            pos++;
                            affix.append("''");
                        } else {
                            inQuote = false;
                        }
                        pos++;
                        continue;
                    }
                } else if (isNumberChar(ch)) {
                    // In a suffix too: the JDK goes back to reading the
                    // number, so "0.0x0" is "0.00x", and this does the same
                    // rather than reject a pattern the desktop accepts.
                    if (phase == 2 && !positive) {
                        pos = len;
                        continue;
                    }
                    phase = 1;
                    continue;
                } else if (ch == CURRENCY_SIGN) {
                    boolean doubled = pos + 1 < len && pattern.charAt(pos + 1) == CURRENCY_SIGN;
                    if (doubled) {
                        pos++;
                    }
                    currency = true;
                    affix.append(QUOTE).append(CURRENCY_SIGN);
                    if (doubled) {
                        affix.append(CURRENCY_SIGN);
                    }
                    pos++;
                    continue;
                } else if (ch == QUOTE) {
                    if (pos + 1 < len && pattern.charAt(pos + 1) == QUOTE) {
                        pos++;
                        affix.append("''");
                    } else {
                        inQuote = true;
                    }
                    pos++;
                    continue;
                } else if (ch == ';') {
                    if (phase == 0 || !positive) {
                        throw malformed("Unquoted special character ';'", pattern);
                    }
                    sawSeparator = true;
                    pos++;
                    break;
                } else if (ch == '%' || ch == PER_MILLE_SIGN) {
                    if (partMultiplier != 1) {
                        throw malformed("Too many percent/per mille characters", pattern);
                    }
                    partMultiplier = ch == '%' ? 100 : 1000;
                    if (positive) {
                        newMultiplier = partMultiplier;
                    }
                    affix.append(QUOTE).append(ch);
                    pos++;
                    continue;
                } else if (ch == '-') {
                    affix.append(QUOTE).append('-');
                    pos++;
                    continue;
                }
                affix.append(ch);
                pos++;
            }
            if (inQuote) {
                throw malformed("Unterminated quote", pattern);
            }
            if (positive) {
                newPosPrefix = prefix.toString();
                newPosSuffix = suffix.toString();
            } else {
                newNegPrefix = prefix.toString();
                newNegSuffix = suffix.toString();
                gotNegative = true;
            }
            // Without a separator the whole pattern is read again as the
            // negative one. Its affixes then equal the positive ones, which
            // is what selects the default minus sign below.
            start = sawSeparator ? pos : 0;
        }

        if (len == 0) {
            useExponentialNotation = false;
            minExponentDigits = 0;
            currencyFormat = false;
            decimalSeparatorAlwaysShown = false;
            setMinimumIntegerDigits(0);
            setMaximumIntegerDigits(Integer.MAX_VALUE);
            setMinimumFractionDigits(0);
            setMaximumFractionDigits(Integer.MAX_VALUE);
        } else {
            if (zeroDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
                // "#.##", "#." and ".#" have one digit that is always
                // written; it is the one next to the decimal point.
                int n = decimalPos;
                if (n == 0) {
                    n++;
                }
                digitRightCount = digitLeftCount - n;
                digitLeftCount = n - 1;
                zeroDigitCount = 1;
            }
            if ((decimalPos < 0 && digitRightCount > 0)
                    || (decimalPos >= 0
                            && (decimalPos < digitLeftCount || decimalPos > digitLeftCount + zeroDigitCount))
                    || groupingCount == 0) {
                throw malformed("Malformed pattern", pattern);
            }
            int digitTotalCount = digitLeftCount + zeroDigitCount + digitRightCount;
            if (decimalPos > digitTotalCount) {
                decimalPos = digitTotalCount;
            }
            int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : digitTotalCount;
            useExponentialNotation = exponential;
            minExponentDigits = exponentDigits;
            // Reset first, so that neither setter is clamped by a previous
            // pattern's limits.
            setMinimumIntegerDigits(0);
            setMinimumFractionDigits(0);
            int minInt = effectiveDecimalPos - digitLeftCount;
            setMaximumIntegerDigits(exponential ? digitLeftCount + minInt : Integer.MAX_VALUE);
            setMinimumIntegerDigits(minInt);
            setMaximumFractionDigits(decimalPos >= 0 ? digitTotalCount - decimalPos : 0);
            setMinimumFractionDigits(decimalPos >= 0 ? digitLeftCount + zeroDigitCount - decimalPos : 0);
            setGroupingUsed(groupingCount > 0);
            groupingSize = groupingCount > 0 ? groupingCount : 0;
            multiplier = newMultiplier;
            currencyFormat = currency;
            decimalSeparatorAlwaysShown = decimalPos == 0 || decimalPos == digitTotalCount;
        }
        posPrefixPattern = newPosPrefix;
        posSuffixPattern = newPosSuffix;
        // No negative subpattern, or one that says nothing new, means a
        // minus sign in front of the positive one.
        if (!gotNegative || (newNegPrefix.equals(newPosPrefix) && newNegSuffix.equals(newPosSuffix))) {
            negPrefixPattern = "'-" + newPosPrefix;
            negSuffixPattern = newPosSuffix;
        } else {
            negPrefixPattern = newNegPrefix;
            negSuffixPattern = newNegSuffix;
        }
        expandAffixes();
    }

    private void expandAffixes() {
        positivePrefix = expandAffix(posPrefixPattern);
        positiveSuffix = expandAffix(posSuffixPattern);
        negativePrefix = expandAffix(negPrefixPattern);
        negativeSuffix = expandAffix(negSuffixPattern);
    }

    private String expandAffix(String internal) {
        if (internal.indexOf(QUOTE) < 0) {
            return internal;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < internal.length()) {
            char c = internal.charAt(i++);
            if (c != QUOTE || i >= internal.length()) {
                sb.append(c);
                continue;
            }
            c = internal.charAt(i++);
            if (c == CURRENCY_SIGN) {
                if (i < internal.length() && internal.charAt(i) == CURRENCY_SIGN) {
                    i++;
                    sb.append(symbols.getInternationalCurrencySymbol());
                } else {
                    sb.append(symbols.getCurrencySymbol());
                }
            } else if (c == '%') {
                sb.append(symbols.getPercent());
            } else if (c == PER_MILLE_SIGN) {
                sb.append(symbols.getPerMill());
            } else if (c == '-') {
                sb.append(symbols.getMinusSign());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String literalAffix(String literal) {
        if (literal.indexOf(QUOTE) < 0) {
            return literal;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            sb.append(c);
            if (c == QUOTE) {
                sb.append(QUOTE);
            }
        }
        return sb.toString();
    }

    private static boolean needsQuoting(char c) {
        return isNumberChar(c) || c == ';' || c == '%' || c == PER_MILLE_SIGN || c == CURRENCY_SIGN
                || c == '-';
    }

    private static void appendAffixPattern(StringBuilder out, String internal) {
        int i = 0;
        while (i < internal.length()) {
            char c = internal.charAt(i++);
            if (c == QUOTE && i < internal.length()) {
                c = internal.charAt(i++);
                if (c == QUOTE) {
                    out.append("''");
                } else {
                    out.append(c);
                    if (c == CURRENCY_SIGN && i < internal.length() && internal.charAt(i) == CURRENCY_SIGN) {
                        out.append(CURRENCY_SIGN);
                        i++;
                    }
                }
            } else if (needsQuoting(c)) {
                out.append(QUOTE).append(c).append(QUOTE);
            } else {
                out.append(c);
            }
        }
    }

    private void appendNumberPattern(StringBuilder out) {
        int minInt = getMinimumIntegerDigits();
        // Capped: an empty pattern allows any number of fraction digits, and
        // spelling that out exhausts memory, as it does in the JDK.
        int maxFrac = Math.min(getMaximumFractionDigits(), DOUBLE_FRACTION_DIGITS);
        int minFrac = Math.min(getMinimumFractionDigits(), DOUBLE_FRACTION_DIGITS);
        boolean grouping = isGroupingUsed() && groupingSize > 0;
        int digitCount = useExponentialNotation
                ? getMaximumIntegerDigits()
                : Math.max(groupingSize, minInt) + 1;
        for (int i = digitCount; i > 0; i--) {
            // groupingSize is checked positive just above.
            if (i != digitCount && grouping && i % groupingSize == 0) {
                out.append(',');
            }
            out.append(i <= minInt ? '0' : '#');
        }
        if (maxFrac > 0 || decimalSeparatorAlwaysShown) {
            out.append('.');
        }
        for (int i = 0; i < maxFrac; i++) {
            out.append(i < minFrac ? '0' : '#');
        }
        if (useExponentialNotation) {
            out.append('E');
            for (int i = 0; i < minExponentDigits; i++) {
                out.append('0');
            }
        }
    }

    /// A pattern that produces this format. It is equivalent to the pattern
    /// that was applied, not necessarily the same text: `0.00` comes back as
    /// `#0.00`.
    public String toPattern() {
        StringBuilder out = new StringBuilder();
        appendAffixPattern(out, posPrefixPattern);
        appendNumberPattern(out);
        appendAffixPattern(out, posSuffixPattern);
        boolean defaultNegative = negSuffixPattern.equals(posSuffixPattern)
                && negPrefixPattern.equals("'-" + posPrefixPattern);
        if (!defaultNegative) {
            out.append(';');
            appendAffixPattern(out, negPrefixPattern);
            appendNumberPattern(out);
            appendAffixPattern(out, negSuffixPattern);
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    /// A copy of the symbols in use; changing it changes nothing until it
    /// is passed to [#setDecimalFormatSymbols(DecimalFormatSymbols)].
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        Object copy = symbols.clone();
        return copy instanceof DecimalFormatSymbols ? (DecimalFormatSymbols) copy : symbols;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        Object copy = newSymbols.clone();
        if (copy instanceof DecimalFormatSymbols) {
            symbols = (DecimalFormatSymbols) copy;
            expandAffixes();
        }
    }

    public String getPositivePrefix() {
        return positivePrefix;
    }

    public void setPositivePrefix(String newValue) {
        positivePrefix = newValue;
        posPrefixPattern = literalAffix(newValue);
    }

    public String getNegativePrefix() {
        return negativePrefix;
    }

    public void setNegativePrefix(String newValue) {
        negativePrefix = newValue;
        negPrefixPattern = literalAffix(newValue);
    }

    public String getPositiveSuffix() {
        return positiveSuffix;
    }

    public void setPositiveSuffix(String newValue) {
        positiveSuffix = newValue;
        posSuffixPattern = literalAffix(newValue);
    }

    public String getNegativeSuffix() {
        return negativeSuffix;
    }

    public void setNegativeSuffix(String newValue) {
        negativeSuffix = newValue;
        negSuffixPattern = literalAffix(newValue);
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int newValue) {
        multiplier = newValue;
    }

    public int getGroupingSize() {
        return groupingSize;
    }

    public void setGroupingSize(int newValue) {
        groupingSize = Math.max(0, newValue);
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return decimalSeparatorAlwaysShown;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        decimalSeparatorAlwaysShown = newValue;
    }

    /// A copy of this format. Written out field by field: `Object.clone()`
    /// answers `null` on the device for anything but an array.
    @Override
    public Object clone() {
        DecimalFormat copy = new DecimalFormat("", symbols);
        copyNumberFormatTo(copy);
        copy.posPrefixPattern = posPrefixPattern;
        copy.posSuffixPattern = posSuffixPattern;
        copy.negPrefixPattern = negPrefixPattern;
        copy.negSuffixPattern = negSuffixPattern;
        copy.positivePrefix = positivePrefix;
        copy.positiveSuffix = positiveSuffix;
        copy.negativePrefix = negativePrefix;
        copy.negativeSuffix = negativeSuffix;
        copy.multiplier = multiplier;
        copy.groupingSize = groupingSize;
        copy.decimalSeparatorAlwaysShown = decimalSeparatorAlwaysShown;
        copy.useExponentialNotation = useExponentialNotation;
        copy.minExponentDigits = minExponentDigits;
        copy.currencyFormat = currencyFormat;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj) || !(obj instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) obj;
        return positivePrefix.equals(other.positivePrefix)
                && positiveSuffix.equals(other.positiveSuffix)
                && negativePrefix.equals(other.negativePrefix)
                && negativeSuffix.equals(other.negativeSuffix)
                && multiplier == other.multiplier
                && groupingSize == other.groupingSize
                && decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
                && useExponentialNotation == other.useExponentialNotation
                && minExponentDigits == other.minExponentDigits
                && symbols.equals(other.symbols);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 37 + positivePrefix.hashCode();
    }
}
