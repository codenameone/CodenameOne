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
package java.text;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/// Builds a message from a pattern and a list of arguments, and reads the
/// arguments back out of a message.
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
/// - `toPattern` returns the elements as they were written rather than a
///   canonical rewrite of them.
/// - Date and time elements ignore the locale passed to the constructor: the
///   device formats dates for its own locale only.
public class MessageFormat extends Format {

    private static final long serialVersionUID = 1L;

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
    private Format[] formats = new Format[0];
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
        List<Format> newFormats = new ArrayList<Format>();
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
                    Format made = makeFormat(type, style);
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
        // Only an element left open at depth zero is refused. A pattern that
        // ends inside a NESTED brace ("{0,choice,0#{") is accepted by every
        // JDK from 8 to 25 -- java.text.MessageFormat tests braceStack == 0
        // at this point too -- so refusing it would reject what the desktop
        // accepts. MessageFormatDifferentialTest.rejectsWhatTheJdkRejects
        // holds both sides to it.
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
        formats = newFormats.toArray(new Format[n]);
        elementTexts = newTexts.toArray(new String[n]);
    }

    // No ceiling on the value, as JDK 8 has none: {999999999} is accepted
    // and the arrays sized by it fail when used. The 9999 limit is a later
    // JDK's, and the conformance host is JDK 8.
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

    private Format makeFormat(String type, String style) {
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
    private String describe(int kind, String style, Format made) {
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

    /// Formats an array of arguments, which `arguments` must be.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `arguments` is not an `Object[]`, or
    ///   an argument is not of the type its element formats
    @Override
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
            Format format = formats[i];
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
            } else if (format != null) {
                result.append(format.format(argument));
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

    /// The formats of the elements, in the order the elements appear in the
    /// pattern; `null` for an element that has none.
    public Format[] getFormats() {
        Format[] copy = new Format[formats.length];
        System.arraycopy(formats, 0, copy, 0, formats.length);
        return copy;
    }

    /// The formats of the arguments, indexed by argument number; `null` for
    /// an argument no element formats. Where several elements name the same
    /// argument, the last one's format is the one returned.
    public Format[] getFormatsByArgumentIndex() {
        Format[] result = new Format[maximumArgumentNumber() + 1];
        for (int i = 0; i < argumentNumbers.length; i++) {
            result[argumentNumbers[i]] = formats[i];
        }
        return result;
    }

    private int maximumArgumentNumber() {
        int max = -1;
        for (int i = 0; i < argumentNumbers.length; i++) {
            max = Math.max(max, argumentNumbers[i]);
        }
        return max;
    }

    /// Sets the format of the element at `formatElementIndex`, counted in
    /// the order the elements appear in the pattern.
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if there is no such element
    public void setFormat(int formatElementIndex, Format newFormat) {
        if (formatElementIndex < 0 || formatElementIndex >= formats.length) {
            throw new ArrayIndexOutOfBoundsException(formatElementIndex);
        }
        formats[formatElementIndex] = newFormat;
        elementTexts[formatElementIndex] = argumentNumbers[formatElementIndex] + describeFormat(newFormat);
    }

    /// Sets the formats of the elements in pattern order; extra formats are
    /// ignored and elements past the end of `newFormats` are left alone.
    public void setFormats(Format[] newFormats) {
        int n = Math.min(newFormats.length, formats.length);
        for (int i = 0; i < n; i++) {
            setFormat(i, newFormats[i]);
        }
    }

    /// Sets the format of every element that formats argument
    /// `argumentIndex`.
    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int i = 0; i < argumentNumbers.length; i++) {
            if (argumentNumbers[i] == argumentIndex) {
                setFormat(i, newFormat);
            }
        }
    }

    /// Sets the formats of the arguments, indexed by argument number.
    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i < argumentNumbers.length; i++) {
            if (argumentNumbers[i] < newFormats.length) {
                setFormat(i, newFormats[argumentNumbers[i]]);
            }
        }
    }

    /// How `toPattern` writes an element whose format was set rather than
    /// read from the pattern: as the JDK does, by recognising the format.
    private String describeFormat(Format f) {
        if (f instanceof ChoiceFormat) {
            return describe(TYPE_CHOICE, "", f);
        }
        if (f instanceof NumberFormat) {
            return describe(TYPE_NUMBER, "", f);
        }
        if (f instanceof DateFormat) {
            String[] names = {",short", "", ",long", ",full"};
            int[] styles = {DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
            for (int i = 0; i < styles.length; i++) {
                if (f.equals(DateFormat.getDateInstance(styles[i]))) {
                    return ",date" + names[i];
                }
                if (f.equals(DateFormat.getTimeInstance(styles[i]))) {
                    return ",time" + names[i];
                }
            }
            if (f instanceof SimpleDateFormat) {
                return ",date," + ((SimpleDateFormat) f).toPattern();
            }
        }
        return "";
    }

    /// Reads the arguments back out of `source`, starting at `pos`: the
    /// literal text must match exactly, an element with a format is read by
    /// that format, and an element without one takes the text up to the
    /// literal that follows it. The result is indexed by argument number. On
    /// failure `pos` is left alone, its error index is set, and `null` is
    /// returned.
    public Object[] parse(String source, ParsePosition pos) {
        if (source == null) {
            return new Object[0];
        }
        Object[] result = new Object[maximumArgumentNumber() + 1];
        int at = pos.getIndex();
        ParsePosition inner = new ParsePosition(0);
        for (int i = 0; i < argumentNumbers.length; i++) {
            String literal = literals[i];
            if (!source.regionMatches(at, literal, 0, literal.length())) {
                pos.setErrorIndex(at);
                return null;
            }
            at += literal.length();
            Format format = formats[i];
            if (format == null) {
                // The text runs to the literal after this element; with
                // none, to the end of the source.
                String next = literals[i + 1];
                int end = next.length() == 0 ? source.length() : source.indexOf(next, at);
                if (end < 0) {
                    pos.setErrorIndex(at);
                    return null;
                }
                String value = source.substring(at, end);
                if (!value.equals("{" + argumentNumbers[i] + "}")) {
                    result[argumentNumbers[i]] = value;
                }
                at = end;
            } else {
                inner.setIndex(at);
                Object value = format.parseObject(source, inner);
                if (inner.getIndex() == at) {
                    pos.setErrorIndex(at);
                    return null;
                }
                result[argumentNumbers[i]] = value;
                at = inner.getIndex();
            }
        }
        String last = literals[argumentNumbers.length];
        if (!source.regionMatches(at, last, 0, last.length())) {
            pos.setErrorIndex(at);
            return null;
        }
        pos.setIndex(at + last.length());
        return result;
    }

    /// Reads the arguments back out of the beginning of `source`.
    ///
    /// #### Throws
    ///
    /// - `ParseException`: if `source` does not match the pattern
    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
        }
        return result;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    /// A copy of this format, built by applying its pattern again:
    /// `Object.clone()` answers `null` on the device for anything but an
    /// array. A format installed with `setFormat` that the pattern cannot
    /// spell is carried over as the same object, since it cannot be copied
    /// the same way.
    @Override
    public Object clone() {
        MessageFormat copy = new MessageFormat(toPattern(), locale);
        for (int i = 0; i < formats.length && i < copy.formats.length; i++) {
            Format custom = unspelled(formats[i]);
            if (custom != null) {
                copy.formats[i] = custom;
            }
        }
        return copy;
    }

    /// `f` when `toPattern()` cannot spell it, otherwise null.
    private Format unspelled(Format f) {
        return f != null && describeFormat(f).length() == 0 ? f : null;
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
        if (!toPattern().equals(other.toPattern())) {
            return false;
        }
        // An equal pattern spells every format but those it cannot, which
        // print as a bare argument: compare those as objects.
        for (int i = 0; i < formats.length && i < other.formats.length; i++) {
            Format mine = unspelled(formats[i]);
            Format theirs = other.unspelled(other.formats[i]);
            if (mine == null ? theirs != null : !mine.equals(theirs)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return toPattern().hashCode();
    }
}
