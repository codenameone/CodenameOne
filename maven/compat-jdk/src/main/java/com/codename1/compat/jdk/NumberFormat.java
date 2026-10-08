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

import java.text.ParseException;
import java.util.Locale;

/// `java.text.NumberFormat` for the Codename One runtime: the base of the
/// number formats, and the factory for the usual ones.
///
/// The factory methods answer a [DecimalFormat] with these patterns:
///
/// - `getInstance` / `getNumberInstance`: `#,##0.###`
/// - `getIntegerInstance`: `#,##0`, rounding to a whole number and parsing
///   only the integer part
/// - `getPercentInstance`: `#,##0%`
/// - `getCurrencyInstance`: the currency symbol followed by `#,##0.00`, and a
///   minus sign before the symbol for a negative amount
///
/// The separators and the currency symbol are those of
/// [DecimalFormatSymbols]; read there how a locale is honoured.
///
/// #### Differences from the JDK
///
/// - It does not extend `java.text.Format`. The device's `Format` declares
///   `format(Object)` abstract where the JDK declares it final, so no class
///   can extend both. Everything `Format` offers is declared here directly --
///   `format(Object)`, `parseObject(String)` -- but a number format cannot be
///   assigned to a variable of type `Format`.
/// - Rounding is always half-even, the JDK's default. There is no
///   `setRoundingMode`, as the device has no `java.math.RoundingMode`.
/// - There is no `getCurrency`/`setCurrency`, as the device has no
///   `java.util.Currency`.
public abstract class NumberFormat implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /// Names the integer part of a formatted number to a [FieldPosition].
    public static final int INTEGER_FIELD = 0;

    /// Names the fraction part of a formatted number to a [FieldPosition].
    public static final int FRACTION_FIELD = 1;

    private boolean groupingUsed = true;
    private int maximumIntegerDigits = 40;
    private int minimumIntegerDigits = 1;
    private int maximumFractionDigits = 3;
    private int minimumFractionDigits;
    private boolean parseIntegerOnly;

    protected NumberFormat() {
    }

    public static final NumberFormat getInstance() {
        return getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getInstance(Locale inLocale) {
        return getNumberInstance(inLocale);
    }

    public static final NumberFormat getNumberInstance() {
        return getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getNumberInstance(Locale inLocale) {
        return new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(inLocale));
    }

    public static final NumberFormat getIntegerInstance() {
        return getIntegerInstance(Locale.getDefault());
    }

    public static NumberFormat getIntegerInstance(Locale inLocale) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(inLocale));
        format.setParseIntegerOnly(true);
        return format;
    }

    public static final NumberFormat getPercentInstance() {
        return getPercentInstance(Locale.getDefault());
    }

    public static NumberFormat getPercentInstance(Locale inLocale) {
        return new DecimalFormat("#,##0%", DecimalFormatSymbols.getInstance(inLocale));
    }

    public static final NumberFormat getCurrencyInstance() {
        return getCurrencyInstance(Locale.getDefault());
    }

    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        String pattern = DecimalFormat.CURRENCY_SIGN + "#,##0.00";
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(inLocale));
    }

    /// Formats a `Number`: an integral type as a `long`, anything else as a
    /// `double`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `number` is not a `Number`
    public final String format(Object number) {
        return format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof Long || number instanceof Integer || number instanceof Short
                || number instanceof Byte) {
            return format(((Number) number).longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            return format(((Number) number).doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public final String format(double number) {
        return format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long number) {
        return format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public abstract StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos);

    /// Parses a number starting at `parsePosition`, which is advanced past
    /// what was read. On failure the position is left alone, its error index
    /// is set, and `null` is returned. The result is a `Long` when the text is
    /// a whole number that fits one, and a `Double` otherwise.
    public abstract Number parse(String source, ParsePosition parsePosition);

    public Number parse(String source) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + source + "\"", parsePosition.getErrorIndex());
        }
        return result;
    }

    public final Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    public Object parseObject(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object result = parseObject(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Format.parseObject(String) failed", pos.getErrorIndex());
        }
        return result;
    }

    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    public void setGroupingUsed(boolean newValue) {
        groupingUsed = newValue;
    }

    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    public void setMaximumIntegerDigits(int newValue) {
        maximumIntegerDigits = Math.max(0, newValue);
        if (minimumIntegerDigits > maximumIntegerDigits) {
            minimumIntegerDigits = maximumIntegerDigits;
        }
    }

    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    public void setMinimumIntegerDigits(int newValue) {
        minimumIntegerDigits = Math.max(0, newValue);
        if (minimumIntegerDigits > maximumIntegerDigits) {
            maximumIntegerDigits = minimumIntegerDigits;
        }
    }

    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int newValue) {
        maximumFractionDigits = Math.max(0, newValue);
        if (maximumFractionDigits < minimumFractionDigits) {
            minimumFractionDigits = maximumFractionDigits;
        }
    }

    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    public void setMinimumFractionDigits(int newValue) {
        minimumFractionDigits = Math.max(0, newValue);
        if (maximumFractionDigits < minimumFractionDigits) {
            maximumFractionDigits = minimumFractionDigits;
        }
    }

    /// Copies the settings this class holds into `target`, for the `clone`
    /// of a subclass: `Object.clone()` answers `null` on the device.
    final void copyNumberFormatTo(NumberFormat target) {
        target.groupingUsed = groupingUsed;
        target.maximumIntegerDigits = maximumIntegerDigits;
        target.minimumIntegerDigits = minimumIntegerDigits;
        target.maximumFractionDigits = maximumFractionDigits;
        target.minimumFractionDigits = minimumFractionDigits;
        target.parseIntegerOnly = parseIntegerOnly;
    }

    /// A copy of this format. Every subclass in this package implements it
    /// by copying its fields.
    @Override
    public abstract Object clone();

    @Override
    public int hashCode() {
        return maximumIntegerDigits * 37 + maximumFractionDigits;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass() || !(obj instanceof NumberFormat)) {
            return false;
        }
        NumberFormat other = (NumberFormat) obj;
        return maximumIntegerDigits == other.maximumIntegerDigits
                && minimumIntegerDigits == other.minimumIntegerDigits
                && maximumFractionDigits == other.maximumFractionDigits
                && minimumFractionDigits == other.minimumFractionDigits
                && groupingUsed == other.groupingUsed
                && parseIntegerOnly == other.parseIntegerOnly;
    }
}
