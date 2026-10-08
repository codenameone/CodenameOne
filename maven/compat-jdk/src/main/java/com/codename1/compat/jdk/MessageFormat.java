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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/// `java.text.MessageFormat` for the Codename One runtime: builds a message
/// from a pattern and a list of arguments.
///
/// #### Pattern syntax
///
/// Text is copied as it is, and each `{...}` element is replaced by an
/// argument:
///
/// - `{0}`: the argument at that index. A `Number` is formatted with
///   `NumberFormat.getInstance`, a `Date` with the short date and time
///   format, a `String` is copied, anything else contributes `toString()`.
/// - `{0,number}`, `{0,number,integer}`, `{0,number,percent}`,
///   `{0,number,currency}`, or `{0,number,#,##0.00}` with a [DecimalFormat]
///   pattern.
/// - `{0,date}` and `{0,time}`, optionally followed by `short`, `medium`,
///   `long` or `full`, or by a `SimpleDateFormat` pattern:
///   `{0,date,yyyy-MM-dd}`. The device's own `java.text.DateFormat` does the
///   formatting, so the named styles produce what the device produces.
/// - `{0,choice,0#none|1#one|1<{0} items}` with a [ChoiceFormat] pattern.
///   If the chosen text has elements of its own, they are formatted against
///   the same arguments.
///
/// A single quote makes the characters up to the next one literal, which is
/// how a `{` is written (`'{'`), and two quotes in a row stand for one. An
/// apostrophe in a message must therefore be doubled: `it''s`.
///
/// An argument that is `null` is written as `null`, and an element whose
/// index is past the end of the arguments is written back as it was, `{2}`.
///
/// #### Differences from the JDK
///
/// - No `parse`, and no `getFormats`/`setFormat` family: those hand out and
///   accept `java.text.Format` objects, which the number formats here are not
///   (see [NumberFormat]).
/// - `toPattern` returns the elements as they were written rather than a
///   canonical rewrite of them.
/// - Date and time elements ignore the locale passed to the constructor: the
///   device formats dates for its own locale only.
public class MessageFormat implements Cloneable {

    private static final int TYPE_NONE = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_DATE = 2;
    private static final int TYPE_TIME = 3;
    private static final int TYPE_CHOICE = 4;

    private Locale locale;

    /// The literal text before each element, and after the last one: always
    /// one longer than the element arrays.
    private String[] literals = new String[] {""};
    private int[] argumentNumbers = new int[0];
    private Object[] formats = new Object[0];
    private String[] elementTexts = new String[0];

    public MessageFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public MessageFormat(String pattern, Locale locale) {
        this.locale = locale;
        parsePattern(pattern);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public void applyPattern(String pattern) {
        parsePattern(pattern);
    }

    private void parsePattern(String pattern) {
        List<String> newLiterals = new ArrayList<String>();
        List<Integer> newNumbers = new ArrayList<Integer>();
        List<Object> newFormats = new ArrayList<Object>();
        List<String> newTexts = new ArrayList<String>();
        StringBuilder raw = new StringBuilder();
        // The parts of the element being read: index, type, style.
        StringBuilder[] parts = new StringBuilder[3];
        int part = -1;
        boolean inQuote = false;
        int braceDepth = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (part < 0) {
                if (ch == '\'') {
                    if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                        raw.append(ch);
                        i++;
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (ch == '{' && !inQuote) {
                    part = 0;
                    parts[0] = new StringBuilder();
                    parts[1] = null;
                    parts[2] = null;
                } else {
                    raw.append(ch);
                }
                continue;
            }
            if (inQuote) {
                // Inside an element the quotes are the sub-pattern's own.
                parts[part].append(ch);
                if (ch == '\'') {
                    inQuote = false;
                }
                continue;
            }
            if (ch == ',') {
                if (part < 2) {
                    part++;
                    parts[part] = new StringBuilder();
                } else {
                    parts[part].append(ch);
                }
            } else if (ch == '{') {
                braceDepth++;
                parts[part].append(ch);
            } else if (ch == '}') {
                if (braceDepth == 0) {
                    newLiterals.add(raw.toString());
                    raw.setLength(0);
                    String type = parts[1] == null ? "" : parts[1].toString();
                    String style = parts[2] == null ? "" : parts[2].toString();
                    int number = parseArgumentNumber(parts[0].toString());
                    newNumbers.add(Integer.valueOf(number));
                    Object made = makeFormat(type, style);
                    newFormats.add(made);
                    newTexts.add(number + describe(typeOf(type), style, made));
                    part = -1;
                } else {
                    braceDepth--;
                    parts[part].append(ch);
                }
            } else if (ch == ' ') {
                // Leading spaces of the type are not part of it.
                if (part != 1 || parts[1].length() > 0) {
                    parts[part].append(ch);
                }
            } else {
                if (ch == '\'') {
                    inQuote = true;
                }
                parts[part].append(ch);
            }
        }
        if (braceDepth == 0 && part >= 0) {
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
        newLiterals.add(raw.toString());
        int n = newNumbers.size();
        literals = newLiterals.toArray(new String[newLiterals.size()]);
        argumentNumbers = new int[n];
        for (int i = 0; i < n; i++) {
            argumentNumbers[i] = newNumbers.get(i).intValue();
        }
        formats = newFormats.toArray(new Object[n]);
        elementTexts = newTexts.toArray(new String[n]);
    }

    private static int parseArgumentNumber(String text) {
        boolean negative = text.startsWith("-");
        int from = negative ? 1 : 0;
        if (text.length() == from || text.length() - from > 9) {
            throw new IllegalArgumentException("can't parse argument number: " + text);
        }
        int value = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("can't parse argument number: " + text);
            }
            value = value * 10 + (c - '0');
        }
        if (negative) {
            throw new IllegalArgumentException("negative argument number: " + text);
        }
        return value;
    }

    private static int typeOf(String type) {
        String t = type.trim();
        if (t.length() == 0) {
            return TYPE_NONE;
        }
        if (t.equalsIgnoreCase("number")) {
            return TYPE_NUMBER;
        }
        if (t.equalsIgnoreCase("date")) {
            return TYPE_DATE;
        }
        if (t.equalsIgnoreCase("time")) {
            return TYPE_TIME;
        }
        if (t.equalsIgnoreCase("choice")) {
            return TYPE_CHOICE;
        }
        throw new IllegalArgumentException("unknown format type: " + type);
    }

    /// The date style a keyword names, or -1 when the text is a pattern.
    private static int dateStyle(String style) {
        String s = style.trim();
        if (s.length() == 0) {
            return DateFormat.DEFAULT;
        }
        if (s.equalsIgnoreCase("short")) {
            return DateFormat.SHORT;
        }
        if (s.equalsIgnoreCase("medium")) {
            return DateFormat.DEFAULT;
        }
        if (s.equalsIgnoreCase("long")) {
            return DateFormat.LONG;
        }
        if (s.equalsIgnoreCase("full")) {
            return DateFormat.FULL;
        }
        return -1;
    }

    private Object makeFormat(String type, String style) {
        int kind = typeOf(type);
        String keyword = style.trim();
        if (kind == TYPE_NUMBER) {
            if (keyword.length() == 0) {
                return NumberFormat.getInstance(locale);
            }
            if (keyword.equalsIgnoreCase("currency")) {
                return NumberFormat.getCurrencyInstance(locale);
            }
            if (keyword.equalsIgnoreCase("percent")) {
                return NumberFormat.getPercentInstance(locale);
            }
            if (keyword.equalsIgnoreCase("integer")) {
                return NumberFormat.getIntegerInstance(locale);
            }
            return new DecimalFormat(style, DecimalFormatSymbols.getInstance(locale));
        }
        if (kind == TYPE_DATE) {
            int dateStyle = dateStyle(style);
            return dateStyle >= 0 ? DateFormat.getDateInstance(dateStyle) : new SimpleDateFormat(style);
        }
        if (kind == TYPE_TIME) {
            int timeStyle = dateStyle(style);
            return timeStyle >= 0 ? DateFormat.getTimeInstance(timeStyle) : new SimpleDateFormat(style);
        }
        if (kind == TYPE_CHOICE) {
            try {
                return new ChoiceFormat(style);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Choice Pattern incorrect: " + style);
            }
        }
        return null;
    }

    /// The type and style of an element as `toPattern` writes them: not the
    /// text that was read but, as in the JDK, a description of the format it
    /// produced -- `{0, number, #.##}` comes back as `{0,number,#0.##}`.
    private String describe(int kind, String style, Object made) {
        if (kind == TYPE_NUMBER) {
            if (made.equals(NumberFormat.getInstance(locale))) {
                return ",number";
            }
            if (made.equals(NumberFormat.getCurrencyInstance(locale))) {
                return ",number,currency";
            }
            if (made.equals(NumberFormat.getPercentInstance(locale))) {
                return ",number,percent";
            }
            if (made.equals(NumberFormat.getIntegerInstance(locale))) {
                return ",number,integer";
            }
            return made instanceof DecimalFormat ? ",number," + ((DecimalFormat) made).toPattern() : ",number";
        }
        if (kind == TYPE_CHOICE) {
            return made instanceof ChoiceFormat ? ",choice," + ((ChoiceFormat) made).toPattern() : ",choice";
        }
        if (kind == TYPE_DATE || kind == TYPE_TIME) {
            String name = kind == TYPE_DATE ? ",date" : ",time";
            int dateStyle = dateStyle(style);
            if (dateStyle == DateFormat.SHORT) {
                return name + ",short";
            }
            if (dateStyle == DateFormat.LONG) {
                return name + ",long";
            }
            if (dateStyle == DateFormat.FULL) {
                return name + ",full";
            }
            // A pattern of either type is a date pattern to the JDK.
            return dateStyle >= 0 ? name : ",date," + style;
        }
        return "";
    }

    private static void appendLiteral(StringBuilder out, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                out.append("''");
            } else if (c == '{') {
                out.append("'{'");
            } else {
                out.append(c);
            }
        }
    }

    public String toPattern() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < argumentNumbers.length; i++) {
            appendLiteral(out, literals[i]);
            out.append('{').append(elementTexts[i]).append('}');
        }
        appendLiteral(out, literals[argumentNumbers.length]);
        return out.toString();
    }

    public static String format(String pattern, Object... arguments) {
        return new MessageFormat(pattern).format(arguments);
    }

    /// Formats an array of arguments.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `arguments` is not an `Object[]`, or
    ///   an argument is not of the type its element formats
    public final String format(Object arguments) {
        return format(arguments, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        if (arguments == null || arguments instanceof Object[]) {
            return format((Object[]) arguments, result, pos);
        }
        throw new IllegalArgumentException("The arguments of a message are an Object[]");
    }

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        for (int i = 0; i < argumentNumbers.length; i++) {
            result.append(literals[i]);
            int number = argumentNumbers[i];
            if (arguments == null || number >= arguments.length) {
                result.append('{').append(number).append('}');
                continue;
            }
            Object argument = arguments[number];
            Object format = formats[i];
            if (argument == null) {
                result.append("null");
            } else if (format instanceof ChoiceFormat) {
                String chosen = ((ChoiceFormat) format).format(argument);
                if (chosen.indexOf('{') >= 0) {
                    new MessageFormat(chosen, locale).format(arguments, result, pos);
                } else {
                    result.append(chosen);
                }
            } else if (format instanceof NumberFormat) {
                result.append(((NumberFormat) format).format(argument));
            } else if (format instanceof DateFormat) {
                result.append(formatDate((DateFormat) format, argument));
            } else if (argument instanceof Number) {
                result.append(NumberFormat.getInstance(locale).format(argument));
            } else if (argument instanceof Date) {
                DateFormat dateTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                result.append(dateTime.format((Date) argument));
            } else if (argument instanceof String) {
                result.append((String) argument);
            } else {
                result.append(String.valueOf(argument.toString()));
            }
        }
        result.append(literals[argumentNumbers.length]);
        return result;
    }

    private static String formatDate(DateFormat format, Object argument) {
        if (argument instanceof Date) {
            return format.format((Date) argument);
        }
        if (argument instanceof Number) {
            return format.format(new Date(((Number) argument).longValue()));
        }
        throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    /// A copy of this format, built by applying its pattern again:
    /// `Object.clone()` answers `null` on the device for anything but an
    /// array.
    @Override
    public Object clone() {
        return new MessageFormat(toPattern(), locale);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() || !(obj instanceof MessageFormat)) {
            return false;
        }
        MessageFormat other = (MessageFormat) obj;
        if (locale == null ? other.locale != null : !locale.equals(other.locale)) {
            return false;
        }
        return toPattern().equals(other.toPattern());
    }

    @Override
    public int hashCode() {
        return toPattern().hashCode();
    }
}
