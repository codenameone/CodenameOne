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

/// `java.text.ChoiceFormat` for the Codename One runtime: picks a string by
/// the range a number falls in. It is what the `choice` element of a
/// [MessageFormat] pattern uses to write plurals.
///
/// A pattern is a list of `limit#text` pairs separated by `|`, the limits
/// ascending: `0#no files|1#one file|1<{0} files`. A number selects the last
/// pair whose limit it reaches; `#` includes the limit itself and `<` starts
/// just above it. A number below the first limit selects the first pair. A
/// single quote makes the characters up to the next one literal, and two in a
/// row stand for one quote.
public class ChoiceFormat extends NumberFormat {

    private static final long serialVersionUID = 1L;

    private static final char LESS_OR_EQUAL = (char) 0x2264;
    private static final String INFINITY = String.valueOf((char) 0x221E);

    private static final long SIGN_BIT = 0x8000000000000000L;
    private static final long POSITIVE_INFINITY_BITS = 0x7FF0000000000000L;

    private double[] choiceLimits = new double[0];
    private String[] choiceFormats = new String[0];

    public ChoiceFormat(String newPattern) {
        parsePattern(newPattern);
    }

    public ChoiceFormat(double[] limits, String[] formats) {
        store(limits, formats);
    }

    public void applyPattern(String newPattern) {
        parsePattern(newPattern);
    }

    private void parsePattern(String newPattern) {
        StringBuilder limitText = new StringBuilder();
        StringBuilder formatText = new StringBuilder();
        double[] limits = new double[8];
        String[] formats = new String[8];
        int count = 0;
        boolean inFormat = false;
        boolean inQuote = false;
        double startValue = 0;
        double oldStartValue = Double.NaN;
        for (int i = 0; i < newPattern.length(); i++) {
            char ch = newPattern.charAt(i);
            StringBuilder current = inFormat ? formatText : limitText;
            if (ch == '\'') {
                if (i + 1 < newPattern.length() && newPattern.charAt(i + 1) == ch) {
                    current.append(ch);
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (inQuote) {
                current.append(ch);
            } else if (ch == '<' || ch == '#' || ch == LESS_OR_EQUAL) {
                if (limitText.length() == 0) {
                    throw new IllegalArgumentException("A choice has no limit: " + newPattern);
                }
                startValue = parseLimit(limitText.toString().trim(), newPattern);
                if (ch == '<' && !Double.isInfinite(startValue)) {
                    startValue = nextDouble(startValue);
                }
                if (startValue <= oldStartValue) {
                    throw new IllegalArgumentException("Choice limits are not ascending: " + newPattern);
                }
                limitText.setLength(0);
                inFormat = true;
            } else if (ch == '|') {
                if (count == limits.length) {
                    double[] moreLimits = new double[count * 2];
                    String[] moreFormats = new String[count * 2];
                    System.arraycopy(limits, 0, moreLimits, 0, count);
                    System.arraycopy(formats, 0, moreFormats, 0, count);
                    limits = moreLimits;
                    formats = moreFormats;
                }
                limits[count] = startValue;
                formats[count] = formatText.toString();
                count++;
                oldStartValue = startValue;
                formatText.setLength(0);
                inFormat = false;
            } else {
                current.append(ch);
            }
        }
        if (inFormat) {
            if (count == limits.length) {
                double[] moreLimits = new double[count + 1];
                String[] moreFormats = new String[count + 1];
                System.arraycopy(limits, 0, moreLimits, 0, count);
                System.arraycopy(formats, 0, moreFormats, 0, count);
                limits = moreLimits;
                formats = moreFormats;
            }
            limits[count] = startValue;
            formats[count] = formatText.toString();
            count++;
        }
        choiceLimits = new double[count];
        System.arraycopy(limits, 0, choiceLimits, 0, count);
        choiceFormats = new String[count];
        System.arraycopy(formats, 0, choiceFormats, 0, count);
    }

    private static double parseLimit(String text, String pattern) {
        if (text.equals(INFINITY)) {
            return Double.POSITIVE_INFINITY;
        }
        if (text.equals("-" + INFINITY)) {
            return Double.NEGATIVE_INFINITY;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A choice limit is not a number: " + pattern);
        }
    }

    private static double distanceToInteger(double value) {
        return Math.abs(value - Math.floor(value + 0.5));
    }

    public String toPattern() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < choiceLimits.length; i++) {
            if (i != 0) {
                result.append('|');
            }
            // Either "limit#" or "justBelow<" says the same thing; the one
            // that is closer to a whole number is the one that was written.
            double limit = choiceLimits[i];
            double less = previousDouble(limit);
            if (distanceToInteger(limit) < distanceToInteger(less)) {
                appendLimit(result, limit);
                result.append('#');
            } else {
                appendLimit(result, less);
                result.append('<');
            }
            String text = choiceFormats[i];
            boolean needQuote = text.indexOf('<') >= 0 || text.indexOf('#') >= 0
                    || text.indexOf(LESS_OR_EQUAL) >= 0 || text.indexOf('|') >= 0;
            if (needQuote) {
                result.append('\'');
            }
            for (int k = 0; k < text.length(); k++) {
                char c = text.charAt(k);
                result.append(c);
                if (c == '\'') {
                    result.append(c);
                }
            }
            if (needQuote) {
                result.append('\'');
            }
        }
        return result.toString();
    }

    private static void appendLimit(StringBuilder out, double limit) {
        if (limit == Double.POSITIVE_INFINITY) {
            out.append(INFINITY);
        } else if (limit == Double.NEGATIVE_INFINITY) {
            out.append('-').append(INFINITY);
        } else {
            out.append(Double.toString(limit));
        }
    }

    public void setChoices(double[] limits, String[] formats) {
        store(limits, formats);
    }

    private void store(double[] limits, String[] formats) {
        if (limits.length != formats.length) {
            throw new IllegalArgumentException("Array and limit arrays must be of the same length.");
        }
        choiceLimits = limits.clone();
        choiceFormats = formats.clone();
    }

    public double[] getLimits() {
        return choiceLimits.clone();
    }

    public Object[] getFormats() {
        return choiceFormats.clone();
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition status) {
        return format((double) number, toAppendTo, status);
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition status) {
        if (choiceFormats.length == 0) {
            return toAppendTo;
        }
        int i = 0;
        // Written so that not-a-number, which is below nothing, selects the
        // first choice.
        while (i < choiceLimits.length && number >= choiceLimits[i]) {
            i++;
        }
        i--;
        if (i < 0) {
            i = 0;
        }
        return toAppendTo.append(choiceFormats[i]);
    }

    /// Finds the choice whose text starts at the position, the longest if
    /// several do, and answers its limit as a `Double`. When none does, the
    /// error index is set and the answer is not-a-number.
    @Override
    public Number parse(String text, ParsePosition status) {
        int start = status.getIndex();
        int furthest = start;
        double bestNumber = Double.NaN;
        for (int i = 0; i < choiceFormats.length; i++) {
            String candidate = choiceFormats[i];
            if (text.regionMatches(start, candidate, 0, candidate.length())) {
                int end = start + candidate.length();
                if (end > furthest) {
                    furthest = end;
                    bestNumber = choiceLimits[i];
                    if (furthest == text.length()) {
                        break;
                    }
                }
            }
        }
        status.setIndex(furthest);
        if (furthest == start) {
            status.setErrorIndex(furthest);
        }
        return Double.valueOf(bestNumber);
    }

    public static final double nextDouble(double d) {
        return nextDouble(d, true);
    }

    public static final double previousDouble(double d) {
        return nextDouble(d, false);
    }

    /// The `double` next to `d`: the next greater when `positive`, else the
    /// next smaller.
    public static double nextDouble(double d, boolean positive) {
        if (Double.isNaN(d)) {
            return d;
        }
        if (d == 0.0) {
            double smallest = Double.longBitsToDouble(1L);
            return positive ? smallest : -smallest;
        }
        long bits = Double.doubleToLongBits(d);
        long magnitude = bits & ~SIGN_BIT;
        if ((bits > 0) == positive) {
            if (magnitude != POSITIVE_INFINITY_BITS) {
                magnitude += 1;
            }
        } else {
            magnitude -= 1;
        }
        return Double.longBitsToDouble(magnitude | (bits & SIGN_BIT));
    }

    /// A copy of this format, built from copies of its arrays:
    /// `Object.clone()` answers `null` on the device for anything but an
    /// array.
    @Override
    public Object clone() {
        ChoiceFormat copy = new ChoiceFormat(choiceLimits, choiceFormats);
        copyNumberFormatTo(copy);
        return copy;
    }

    @Override
    public int hashCode() {
        int result = choiceLimits.length;
        if (choiceFormats.length > 0) {
            result ^= choiceFormats[choiceFormats.length - 1].hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChoiceFormat) || getClass() != obj.getClass()) {
            return false;
        }
        ChoiceFormat other = (ChoiceFormat) obj;
        if (choiceLimits.length != other.choiceLimits.length) {
            return false;
        }
        for (int i = 0; i < choiceLimits.length; i++) {
            if (Double.doubleToLongBits(choiceLimits[i]) != Double.doubleToLongBits(other.choiceLimits[i])
                    || !choiceFormats[i].equals(other.choiceFormats[i])) {
                return false;
            }
        }
        return true;
    }
}
