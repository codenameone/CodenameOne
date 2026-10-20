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
package java.time.format;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Locale;

/// Formats and parses dates and times: the ISO formats as constants, and
/// any other through [#ofPattern(String)].
///
/// Patterns follow the JDK's letters and counts. Supported: `G` era, `u`
/// year, `y` year of era, `M`/`L` month, `d` day of month, `D` day of
/// year, `Q`/`q` quarter, `E` day of week, `a` AM/PM, `h`, `K`, `k`, `H`
/// hours, `m` minutes, `s` seconds, `S` fraction of a second, `n` nano of
/// second, `N` nano of day, `A` milli of day, `VV` zone id, `z` (the zone
/// id), `O`, `X`, `x` and `Z` offsets, quoted text, and `[...]` optional
/// sections. Text is English whatever the locale. The week-based letters
/// (`Y`, `w`, `W`, `e`, `c`, `F`, `g`), `v`, `B` and `p` are refused.
///
/// A pattern parses as the JDK's smart resolver does: a day of month past
/// the end of its month becomes the last day, an hour alone is that hour
/// on the dot, and a two-digit year is one of 2000 to 2099. The ISO
/// formats resolve strictly.
public final class DateTimeFormatter {
    private static final int KIND_PATTERN = 0;
    private static final int KIND_LOCAL_DATE = 1;
    private static final int KIND_LOCAL_TIME = 2;
    private static final int KIND_LOCAL_DATE_TIME = 3;
    private static final int KIND_OFFSET_DATE_TIME = 4;
    private static final int KIND_ZONED_DATE_TIME = 5;
    private static final int KIND_INSTANT = 6;

    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    public static final DateTimeFormatter ISO_LOCAL_DATE = new DateTimeFormatter(KIND_LOCAL_DATE, null, null,
            null, null);
    public static final DateTimeFormatter ISO_LOCAL_TIME = new DateTimeFormatter(KIND_LOCAL_TIME, null, null,
            null, null);
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatter(KIND_LOCAL_DATE_TIME, null,
            null, null, null);
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = new DateTimeFormatter(KIND_OFFSET_DATE_TIME, null,
            null, null, null);
    public static final DateTimeFormatter ISO_ZONED_DATE_TIME = new DateTimeFormatter(KIND_ZONED_DATE_TIME, null,
            null, null, null);
    public static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatter(KIND_INSTANT, null, null, null, null);

    private final int kind;
    private final String pattern;
    private final Token[] tokens;
    private final Locale locale;
    private final ZoneId zone;

    private DateTimeFormatter(int kind, String pattern, Token[] tokens, Locale locale, ZoneId zone) {
        this.kind = kind;
        this.pattern = pattern;
        this.tokens = tokens;
        this.locale = locale;
        this.zone = zone;
    }

    public static DateTimeFormatter ofPattern(String pattern) {
        return ofPattern(pattern, Locale.getDefault());
    }

    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return new DateTimeFormatter(KIND_PATTERN, pattern, compile(pattern), locale, null);
    }

    /// This formatter with `zone` overriding the zone of what it formats and
    /// supplying one to what it parses; null removes the override.
    public DateTimeFormatter withZone(ZoneId zone) {
        if (zone == null ? this.zone == null : zone.equals(this.zone)) {
            return this;
        }
        return new DateTimeFormatter(kind, pattern, tokens, locale, zone);
    }

    /// The zone given to [#withZone(ZoneId)], or null.
    public ZoneId getZone() {
        return zone;
    }

    public DateTimeFormatter withLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        if (locale.equals(this.locale)) {
            return this;
        }
        return new DateTimeFormatter(kind, pattern, tokens, locale, zone);
    }

    public Locale getLocale() {
        return locale == null ? Locale.getDefault() : locale;
    }

    // ---- formatting ----

    public String format(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        Fields f = Fields.of(temporal, zone);
        StringBuilder out = new StringBuilder(32);
        switch (kind) {
            case KIND_PATTERN:
                formatPattern(f, out);
                break;
            case KIND_LOCAL_DATE:
                isoDate(f, out);
                break;
            case KIND_LOCAL_TIME:
                isoTime(f, out);
                break;
            case KIND_LOCAL_DATE_TIME:
                isoDate(f, out);
                out.append('T');
                isoTime(f, out);
                break;
            case KIND_OFFSET_DATE_TIME:
                isoDate(f, out);
                out.append('T');
                isoTime(f, out);
                out.append(f.offset("OffsetSeconds").getId());
                break;
            case KIND_ZONED_DATE_TIME:
                isoDate(f, out);
                out.append('T');
                isoTime(f, out);
                out.append(f.offset("OffsetSeconds").getId());
                if (f.zone != null && !(f.zone instanceof ZoneOffset)) {
                    out.append('[').append(f.zone.getId()).append(']');
                }
                break;
            default:
                isoInstant(f, out);
                break;
        }
        return out.toString();
    }

    private static void isoDate(Fields f, StringBuilder out) {
        LocalDate date = f.date("Year");
        appendYear(date.getYear(), out);
        out.append('-');
        pad(out, date.getMonthValue(), 2);
        out.append('-');
        pad(out, date.getDayOfMonth(), 2);
    }

    /// A year as the ISO formats print it: at least four digits, a sign
    /// when negative, and a `+` when more than four digits.
    private static void appendYear(int year, StringBuilder out) {
        if (year < 0) {
            out.append('-');
            pad(out, -(long) year, 4);
        } else {
            if (year > 9999) {
                out.append('+');
            }
            pad(out, year, 4);
        }
    }

    private static void isoTime(Fields f, StringBuilder out) {
        LocalTime time = f.time("HourOfDay");
        pad(out, time.getHour(), 2);
        out.append(':');
        pad(out, time.getMinute(), 2);
        out.append(':');
        pad(out, time.getSecond(), 2);
        int nano = time.getNano();
        if (nano != 0) {
            String digits = String.valueOf(1000000000 + nano).substring(1);
            int end = 9;
            while (digits.charAt(end - 1) == '0') {
                end--;
            }
            out.append('.').append(digits.substring(0, end));
        }
    }

    private static void isoInstant(Fields f, StringBuilder out) {
        if (f.epochSecond == null) {
            throw new UnsupportedTemporalTypeException("Unsupported field: InstantSeconds");
        }
        // An instant reaches a year past what LocalDate holds, so the year
        // is split off in whole 10000 year cycles first, as the JDK does.
        long seconds = f.epochSecond.longValue();
        long shifted = seconds + SECONDS_0000_TO_1970;
        long cycles = shifted / SECONDS_PER_10000_YEARS;
        if (shifted < 0 && cycles * SECONDS_PER_10000_YEARS != shifted) {
            cycles--;
        }
        long within = seconds - cycles * SECONDS_PER_10000_YEARS;
        LocalDateTime utc = LocalDateTime.ofEpochSecond(within, f.nano, ZoneOffset.UTC);
        appendYear((int) (cycles * 10000 + utc.getYear()), out);
        out.append('-');
        pad(out, utc.getMonthValue(), 2);
        out.append('-');
        pad(out, utc.getDayOfMonth(), 2);
        out.append('T');
        pad(out, utc.getHour(), 2);
        out.append(':');
        pad(out, utc.getMinute(), 2);
        out.append(':');
        pad(out, utc.getSecond(), 2);
        int nano = utc.getNano();
        if (nano != 0) {
            // Whole groups of three digits: millis, micros or nanos.
            String digits = String.valueOf(1000000000 + nano).substring(1);
            if (nano % 1000000 == 0) {
                digits = digits.substring(0, 3);
            } else if (nano % 1000 == 0) {
                digits = digits.substring(0, 6);
            }
            out.append('.').append(digits);
        }
        out.append('Z');
    }

    private void formatPattern(Fields f, StringBuilder out) {
        ArrayList<Integer> starts = new ArrayList<Integer>();
        int i = 0;
        while (i < tokens.length) {
            Token t = tokens[i];
            if (t.kind == Token.OPTIONAL_START) {
                starts.add(Integer.valueOf(out.length()));
                i++;
                continue;
            }
            if (t.kind == Token.OPTIONAL_END) {
                starts.remove(starts.size() - 1);
                i++;
                continue;
            }
            if (t.kind == Token.LITERAL) {
                out.append(t.text);
                i++;
                continue;
            }
            try {
                formatField(t, f, out);
                i++;
            } catch (UnsupportedTemporalTypeException missing) {
                if (starts.isEmpty()) {
                    throw missing;
                }
                // A field an optional section needs is missing: drop the
                // section, whatever of it was printed already included.
                out.setLength(starts.remove(starts.size() - 1).intValue());
                i = skipSection(i);
            }
        }
    }

    /// The index after the end of the optional section `i` is inside.
    private int skipSection(int i) {
        int depth = 1;
        i++;
        while (i < tokens.length && depth > 0) {
            if (tokens[i].kind == Token.OPTIONAL_START) {
                depth++;
            } else if (tokens[i].kind == Token.OPTIONAL_END) {
                depth--;
            }
            i++;
        }
        return i;
    }

    private static void formatField(Token t, Fields f, StringBuilder out) {
        int count = t.count;
        switch (t.letter) {
            case 'G': {
                int year = f.date("Era").getYear();
                text(out, count, year >= 1 ? ERA_FULL[1] : ERA_FULL[0], year >= 1 ? "AD" : "BC");
                return;
            }
            case 'u':
            case 'y': {
                int year = f.date(t.letter == 'u' ? "Year" : "YearOfEra").getYear();
                long value = t.letter == 'y' && year < 1 ? 1L - year : year;
                if (count == 2) {
                    pad(out, Math.abs(value) % 100, 2);
                } else if (count < 4) {
                    number(out, value, count, 19, SIGN_NORMAL, "Year");
                } else {
                    number(out, value, count, 19, SIGN_EXCEEDS_PAD, "Year");
                }
                return;
            }
            case 'M':
            case 'L': {
                int month = f.date("MonthOfYear").getMonthValue();
                if (count >= 3) {
                    text(out, count, MONTHS[month - 1], null);
                } else {
                    small(out, month, count);
                }
                return;
            }
            case 'Q':
            case 'q': {
                int quarter = (f.date("QuarterOfYear").getMonthValue() - 1) / 3 + 1;
                if (count == 3) {
                    out.append('Q').append(quarter);
                } else if (count == 4) {
                    out.append(QUARTERS[quarter - 1]).append(" quarter");
                } else if (count == 5) {
                    out.append(quarter);
                } else {
                    small(out, quarter, count);
                }
                return;
            }
            case 'd':
                small(out, f.date("DayOfMonth").getDayOfMonth(), count);
                return;
            case 'D': {
                int doy = f.date("DayOfYear").getDayOfYear();
                if (count == 2) {
                    number(out, doy, 2, 3, SIGN_NOT_NEGATIVE, "DayOfYear");
                } else {
                    small(out, doy, count);
                }
                return;
            }
            case 'E': {
                DayOfWeek dow = f.dayOfWeek();
                text(out, count, DAYS[dow.getValue() - 1], null);
                return;
            }
            case 'a':
                out.append(f.time("AmPmOfDay").getHour() < 12 ? "AM" : "PM");
                return;
            case 'h': {
                int h = f.time("ClockHourOfAmPm").getHour() % 12;
                small(out, h == 0 ? 12 : h, count);
                return;
            }
            case 'K':
                small(out, f.time("HourOfAmPm").getHour() % 12, count);
                return;
            case 'k': {
                int h = f.time("ClockHourOfDay").getHour();
                small(out, h == 0 ? 24 : h, count);
                return;
            }
            case 'H':
                small(out, f.time("HourOfDay").getHour(), count);
                return;
            case 'm':
                small(out, f.time("MinuteOfHour").getMinute(), count);
                return;
            case 's':
                small(out, f.time("SecondOfMinute").getSecond(), count);
                return;
            case 'S':
                out.append(String.valueOf(1000000000 + f.nanoOfSecond()).substring(1, 1 + count));
                return;
            case 'n':
                small(out, f.nanoOfSecond(), count);
                return;
            case 'N':
                small(out, f.time("NanoOfDay").toNanoOfDay(), count);
                return;
            case 'A':
                small(out, f.time("MilliOfDay").toNanoOfDay() / 1000000L, count);
                return;
            case 'V':
            case 'z':
                if (f.zone == null) {
                    throw new UnsupportedTemporalTypeException("Unable to extract ZoneId from temporal");
                }
                out.append(f.zone.getId());
                return;
            case 'O':
                localizedOffset(out, f.offset("OffsetSeconds").getTotalSeconds(), count == 4);
                return;
            case 'X':
                offset(out, f.offset("OffsetSeconds").getTotalSeconds(), count == 1 ? 1 : count, "Z");
                return;
            case 'x':
                offset(out, f.offset("OffsetSeconds").getTotalSeconds(), count, OFFSET_ZERO[count - 1]);
                return;
            case 'Z':
                if (count == 4) {
                    localizedOffset(out, f.offset("OffsetSeconds").getTotalSeconds(), true);
                } else if (count == 5) {
                    offset(out, f.offset("OffsetSeconds").getTotalSeconds(), 5, "Z");
                } else {
                    offset(out, f.offset("OffsetSeconds").getTotalSeconds(), 2, "+0000");
                }
                return;
            default:
                throw new IllegalStateException(String.valueOf(t.letter));
        }
    }

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"};
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
        "Sunday"};
    private static final String[] ERA_FULL = {"Before Christ", "Anno Domini"};
    private static final String[] QUARTERS = {"1st", "2nd", "3rd", "4th"};
    private static final String[] OFFSET_ZERO = {"+00", "+0000", "+00:00", "+0000", "+00:00"};

    private static final int SIGN_NORMAL = 0;
    private static final int SIGN_NOT_NEGATIVE = 1;
    private static final int SIGN_EXCEEDS_PAD = 2;

    /// Text in the style the count asks for: short (1 to 3), full (4) or
    /// narrow (5). `shortForm` stands in when the short form is not the
    /// first three letters.
    private static void text(StringBuilder out, int count, String full, String shortForm) {
        if (count == 4) {
            out.append(full);
        } else if (count == 5) {
            out.append(full.charAt(0));
        } else {
            out.append(shortForm != null ? shortForm : full.substring(0, 3));
        }
    }

    /// A field of one letter (any width) or two (exactly two digits).
    private static void small(StringBuilder out, long value, int count) {
        if (count == 1) {
            out.append(value);
        } else {
            pad(out, value, count);
        }
    }

    private static void number(StringBuilder out, long value, int minWidth, int maxWidth, int sign, String field) {
        String digits = String.valueOf(Math.abs(value));
        if (value == Long.MIN_VALUE || digits.length() > maxWidth) {
            throw new DateTimeException("Field " + field + " cannot be printed as the value " + value
                    + " exceeds the maximum print width of " + maxWidth);
        }
        if (value < 0) {
            if (sign == SIGN_NOT_NEGATIVE) {
                throw new DateTimeException("Field " + field + " cannot be printed as the value " + value
                        + " cannot be negative according to the SignStyle");
            }
            out.append('-');
        } else if (sign == SIGN_EXCEEDS_PAD && minWidth < 19 && digits.length() > minWidth) {
            out.append('+');
        }
        for (int i = digits.length(); i < minWidth; i++) {
            out.append('0');
        }
        out.append(digits);
    }

    private static void pad(StringBuilder out, long value, int width) {
        String digits = String.valueOf(value);
        for (int i = digits.length(); i < width; i++) {
            out.append('0');
        }
        out.append(digits);
    }

    /// An offset as the `X`, `x` and `Z` letters print it. `style` is the
    /// count of `X`: 1 `+HHmm`, 2 `+HHMM`, 3 `+HH:MM`, 4 `+HHMMss`, 5
    /// `+HH:MM:ss`.
    private static void offset(StringBuilder out, int total, int style, String zeroText) {
        if (total == 0) {
            out.append(zeroText);
            return;
        }
        int abs = Math.abs(total);
        int hours = abs / 3600;
        int minutes = (abs / 60) % 60;
        int seconds = abs % 60;
        boolean colon = style == 3 || style == 5;
        out.append(total < 0 ? '-' : '+');
        pad(out, hours, 2);
        if (style == 1 && minutes == 0) {
            return;
        }
        if (colon) {
            out.append(':');
        }
        pad(out, minutes, 2);
        if (style >= 4 && seconds != 0) {
            if (colon) {
                out.append(':');
            }
            pad(out, seconds, 2);
        }
    }

    /// `GMT+8` (short) or `GMT+08:00` (full); `GMT` alone for zero.
    /// The localized offset `O` and `ZZZZ` print: GMT, or GMT and a
    /// signed hours[:minutes[:seconds]] offset.
    private static void parseLocalizedOffset(Parsed p) {
        int start = p.pos;
        p.literal('G');
        p.literal('M');
        p.literal('T');
        if (p.pos < p.text.length() && (p.text.charAt(p.pos) == '+' || p.text.charAt(p.pos) == '-')) {
            boolean negative = p.text.charAt(p.pos) == '-';
            p.pos++;
            int hours = (int) p.digits(1, 2);
            int minutes = 0;
            int seconds = 0;
            if (p.pos < p.text.length() && p.text.charAt(p.pos) == ':') {
                p.pos++;
                minutes = (int) p.digits(2, 2);
                if (p.pos < p.text.length() && p.text.charAt(p.pos) == ':') {
                    p.pos++;
                    seconds = (int) p.digits(2, 2);
                }
            }
            int total = hours * 3600 + minutes * 60 + seconds;
            p.setOffset(negative ? -total : total, start);
        } else {
            p.setOffset(0, start);
        }
    }

    private static void localizedOffset(StringBuilder out, int total, boolean full) {
        out.append("GMT");
        if (total == 0) {
            return;
        }
        int abs = Math.abs(total);
        int hours = abs / 3600;
        int minutes = (abs / 60) % 60;
        int seconds = abs % 60;
        out.append(total < 0 ? '-' : '+');
        if (full) {
            pad(out, hours, 2);
            out.append(':');
            pad(out, minutes, 2);
            if (seconds != 0) {
                out.append(':');
                pad(out, seconds, 2);
            }
        } else {
            out.append(hours);
            if (minutes != 0 || seconds != 0) {
                out.append(':');
                pad(out, minutes, 2);
                if (seconds != 0) {
                    out.append(':');
                    pad(out, seconds, 2);
                }
            }
        }
    }

    // ---- parsing ----

    public TemporalAccessor parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        Parsed p = new Parsed(s);
        switch (kind) {
            case KIND_PATTERN:
                parsePattern(p);
                break;
            case KIND_LOCAL_DATE:
                parseIsoDate(p);
                break;
            case KIND_LOCAL_TIME:
                parseIsoTime(p);
                break;
            case KIND_LOCAL_DATE_TIME:
                parseIsoDateTime(p);
                break;
            case KIND_OFFSET_DATE_TIME:
                parseIsoDateTime(p);
                parseOffsetId(p);
                break;
            case KIND_ZONED_DATE_TIME:
                parseIsoDateTime(p);
                parseOffsetId(p);
                if (p.pos < s.length() && s.charAt(p.pos) == '[') {
                    p.pos++;
                    parseZoneId(p);
                    p.literal(']');
                }
                break;
            default:
                Instant instant = parseIsoInstant(p);
                // With a zone of its own the formatter places the moment
                // in it, so the result is a zoned date and time as well.
                return zone == null ? instant : ZonedDateTime.ofInstant(instant, zone);
        }
        if (p.pos < s.length()) {
            throw new DateTimeParseException("Text '" + s + "' could not be parsed, unparsed text found at index "
                    + p.pos, s, p.pos);
        }
        try {
            return p.resolve(zone, kind != KIND_PATTERN);
        } catch (DateTimeParseException ex) {
            throw ex;
        } catch (DateTimeException ex) {
            throw new DateTimeParseException("Text '" + s + "' could not be parsed: " + ex.getMessage(), s, 0, ex);
        }
    }

    /// Parses `text` and hands the result to `query`, as in
    /// `formatter.parse(text, LocalDate::from)`.
    public <T> T parse(CharSequence text, TemporalQuery<T> query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        TemporalAccessor parsed = parse(text);
        try {
            return query.queryFrom(parsed);
        } catch (DateTimeParseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            String s = text.toString();
            throw new DateTimeParseException("Text '" + s + "' could not be parsed: " + ex.getMessage(), s, 0, ex);
        }
    }

    private static void parseIsoDate(Parsed p) {
        int start = p.pos;
        boolean negative = false;
        boolean signed = false;
        if (p.pos < p.text.length() && (p.text.charAt(p.pos) == '+' || p.text.charAt(p.pos) == '-')) {
            negative = p.text.charAt(p.pos) == '-';
            signed = true;
            p.pos++;
        }
        int digitsStart = p.pos;
        long year = p.digits(4, 10);
        if (p.pos - digitsStart > 4 && !signed) {
            p.fail(start);
        }
        if (signed && !negative && p.pos - digitsStart <= 4) {
            p.fail(start);
        }
        if (negative && year == 0) {
            // Year zero is written 0000; the formatter never prints -0000.
            p.fail(start);
        }
        p.set(F_YEAR, negative ? -year : year);
        p.literal('-');
        p.set(F_MONTH, p.digits(2, 2));
        p.literal('-');
        p.set(F_DAY, p.digits(2, 2));
    }

    private static void parseIsoTime(Parsed p) {
        p.set(F_HOUR, p.digits(2, 2));
        p.literal(':');
        p.set(F_MINUTE, p.digits(2, 2));
        if (p.pos < p.text.length() && p.text.charAt(p.pos) == ':') {
            p.pos++;
            p.set(F_SECOND, p.digits(2, 2));
            if (p.pos < p.text.length() && p.text.charAt(p.pos) == '.') {
                p.pos++;
                int from = p.pos;
                long fraction = p.digits(0, 9);
                int width = p.pos - from;
                for (int i = width; i < 9; i++) {
                    fraction *= 10;
                }
                p.set(F_NANO, fraction);
            }
        }
    }

    private static void parseIsoDateTime(Parsed p) {
        parseIsoDate(p);
        if (p.pos >= p.text.length() || (p.text.charAt(p.pos) != 'T' && p.text.charAt(p.pos) != 't')) {
            p.fail(p.pos);
        }
        p.pos++;
        parseIsoTime(p);
    }

    /// `Z`, or the ISO offset `+HH` with optional `:MM` and `:ss`. The
    /// compact `+HHMM` is not ISO and is refused, as the JDK refuses it.
    private static void parseOffsetId(Parsed p) {
        if (p.pos < p.text.length() && (p.text.charAt(p.pos) == 'Z' || p.text.charAt(p.pos) == 'z')) {
            p.pos++;
            p.offset = ZoneOffset.UTC;
            return;
        }
        p.offset = ZoneOffset.ofTotalSeconds(p.offsetSeconds(true, 1, 3));
    }

    private static void parseZoneId(Parsed p) {
        int start = p.pos;
        while (p.pos < p.text.length()) {
            char c = p.text.charAt(p.pos);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '~' || c == '/'
                    || c == '.' || c == '_' || c == '+' || c == '-' || c == ':') {
                p.pos++;
            } else {
                break;
            }
        }
        if (p.pos == start) {
            p.fail(start);
        }
        try {
            p.zone = ZoneId.of(p.text.substring(start, p.pos));
        } catch (DateTimeException ex) {
            p.fail(start);
        }
    }

    private Instant parseIsoInstant(Parsed p) {
        parseIsoDate(p);
        if (p.pos >= p.text.length() || (p.text.charAt(p.pos) != 'T' && p.text.charAt(p.pos) != 't')) {
            p.fail(p.pos);
        }
        p.pos++;
        p.set(F_HOUR, p.digits(2, 2));
        p.literal(':');
        p.set(F_MINUTE, p.digits(2, 2));
        p.literal(':');
        p.set(F_SECOND, p.digits(2, 2));
        long nano = 0;
        if (p.pos < p.text.length() && p.text.charAt(p.pos) == '.') {
            p.pos++;
            int from = p.pos;
            nano = p.digits(0, 9);
            for (int i = p.pos - from; i < 9; i++) {
                nano *= 10;
            }
        }
        parseOffsetId(p);
        if (p.pos < p.text.length()) {
            throw new DateTimeParseException("Text '" + p.text + "' could not be parsed, unparsed text found at index "
                    + p.pos, p.text, p.pos);
        }
        try {
            int hour = (int) p.values[F_HOUR];
            int second = (int) p.values[F_SECOND];
            long dayAdjust = 0;
            if (hour == 24 && p.values[F_MINUTE] == 0 && second == 0 && nano == 0) {
                hour = 0;
                dayAdjust = 1;
            }
            if (second == 60 && hour == 23 && p.values[F_MINUTE] == 59) {
                // A leap second reads as the last second of its minute; only
                // 23:59:60 is one; any other :60 fails in LocalTime.of.
                second = 59;
            }
            LocalDate date = LocalDate.of((int) p.values[F_YEAR], (int) p.values[F_MONTH], (int) p.values[F_DAY])
                    .plusDays(dayAdjust);
            LocalTime time = LocalTime.of(hour, (int) p.values[F_MINUTE], second, (int) nano);
            return LocalDateTime.of(date, time).toInstant(p.offset);
        } catch (DateTimeException ex) {
            throw new DateTimeParseException("Text '" + p.text + "' could not be parsed: " + ex.getMessage(), p.text,
                    0, ex);
        }
    }

    private void parsePattern(Parsed p) {
        parseTokens(p, 0, tokens.length);
    }

    /// Parses tokens `from` to `to`; an optional section that fails is
    /// undone and skipped.
    private void parseTokens(Parsed p, int from, int to) {
        int i = from;
        while (i < to) {
            Token t = tokens[i];
            if (t.kind == Token.OPTIONAL_START) {
                int end = skipSection(i) - 1;
                Parsed saved = p.copy();
                try {
                    parseTokens(p, i + 1, end);
                } catch (DateTimeParseException ex) {
                    p.restore(saved);
                }
                i = end + 1;
                continue;
            }
            if (t.kind == Token.OPTIONAL_END) {
                i++;
                continue;
            }
            if (t.kind == Token.LITERAL) {
                for (int c = 0; c < t.text.length(); c++) {
                    p.literal(t.text.charAt(c));
                }
                i++;
                continue;
            }
            parseField(p, t, reserved(i + 1, to));
            i++;
        }
    }

    /// The digits the fixed-width numbers directly after token `i` need,
    /// which a variable-width number before them leaves for them.
    private int reserved(int i, int to) {
        int total = 0;
        while (i < to && tokens[i].kind == Token.FIELD) {
            int width = fixedWidth(tokens[i]);
            if (width <= 0) {
                break;
            }
            total += width;
            i++;
        }
        return total;
    }

    /// The width of a number that is always printed with exactly that many
    /// digits, or 0.
    private static int fixedWidth(Token t) {
        switch (t.letter) {
            case 'y':
            case 'u':
                return t.count == 2 ? 2 : 0;
            case 'M':
            case 'L':
            case 'Q':
            case 'q':
                return t.count == 2 ? 2 : 0;
            case 'd':
            case 'H':
            case 'h':
            case 'K':
            case 'k':
            case 'm':
            case 's':
                return t.count == 2 ? 2 : 0;
            case 'D':
                return t.count == 3 ? 3 : 0;
            case 'S':
                return t.count;
            default:
                return 0;
        }
    }

    private static void parseField(Parsed p, Token t, int reserve) {
        int count = t.count;
        switch (t.letter) {
            case 'G':
                p.set(F_ERA, p.oneOf(count == 4 ? ERA_FULL : (count == 5 ? new String[] {"B", "A"}
                        : new String[] {"BC", "AD"})));
                return;
            case 'u':
            case 'y': {
                int field = t.letter == 'u' ? F_YEAR : F_YEAR_OF_ERA;
                if (count == 2) {
                    p.set(field, 2000 + p.digits(2, 2));
                } else {
                    boolean negative = false;
                    int start = p.pos;
                    if (p.pos < p.text.length() && (p.text.charAt(p.pos) == '-'
                            || (count >= 4 && p.text.charAt(p.pos) == '+'))) {
                        negative = p.text.charAt(p.pos) == '-';
                        p.pos++;
                    }
                    int digitsStart = p.pos;
                    long value = p.digits(count, Math.max(count, 19 - reserve), reserve);
                    if (count >= 4 && !negative && (p.pos - digitsStart > count) != (start != digitsStart)) {
                        p.fail(start);
                    }
                    if (negative && t.letter == 'y') {
                        p.fail(start);
                    }
                    p.set(field, negative ? -value : value);
                }
                return;
            }
            case 'M':
            case 'L':
                if (count >= 3) {
                    p.set(F_MONTH, 1 + p.oneOf(styled(MONTHS, count)));
                } else {
                    p.set(F_MONTH, p.number(count, reserve));
                }
                return;
            case 'Q':
            case 'q':
                if (count == 3) {
                    p.literal('Q');
                    p.set(F_QUARTER, p.digits(1, 1));
                } else if (count == 4) {
                    p.set(F_QUARTER, 1 + p.oneOf(new String[] {"1st quarter", "2nd quarter", "3rd quarter",
                        "4th quarter"}));
                } else if (count == 5) {
                    p.set(F_QUARTER, p.digits(1, 1));
                } else {
                    p.set(F_QUARTER, p.number(count, reserve));
                }
                return;
            case 'd':
                p.set(F_DAY, p.number(count, reserve));
                return;
            case 'D':
                p.set(F_DAY_OF_YEAR, count == 2 ? p.digits(2, 3, reserve) : p.number(count, reserve));
                return;
            case 'E':
                p.set(F_DAY_OF_WEEK, 1 + p.oneOf(styled(DAYS, count)));
                return;
            case 'a':
                p.set(F_AMPM, p.oneOf(new String[] {"AM", "PM"}));
                return;
            case 'h':
                p.set(F_CLOCK_HOUR_AMPM, p.number(count, reserve));
                return;
            case 'K':
                p.set(F_HOUR_AMPM, p.number(count, reserve));
                return;
            case 'k':
                p.set(F_CLOCK_HOUR, p.number(count, reserve));
                return;
            case 'H':
                p.set(F_HOUR, p.number(count, reserve));
                return;
            case 'm':
                p.set(F_MINUTE, p.number(count, reserve));
                return;
            case 's':
                p.set(F_SECOND, p.number(count, reserve));
                return;
            case 'S': {
                long fraction = p.digits(count, count);
                for (int i = count; i < 9; i++) {
                    fraction *= 10;
                }
                p.set(F_NANO, fraction);
                return;
            }
            case 'n':
                p.set(F_NANO, p.number(count, reserve));
                return;
            case 'N':
                p.set(F_NANO_OF_DAY, p.number(count, reserve));
                return;
            case 'A':
                p.set(F_MILLI_OF_DAY, p.number(count, reserve));
                return;
            case 'V':
            case 'z':
                parseZoneId(p);
                return;
            case 'O':
                parseLocalizedOffset(p);
                return;
            case 'X':
            case 'Z':
                if ((t.letter == 'X' || count == 5) && p.pos < p.text.length() && p.text.charAt(p.pos) == 'Z') {
                    p.pos++;
                    p.offset = ZoneOffset.UTC;
                    return;
                }
                if (t.letter == 'Z' && count == 4) {
                    // ZZZZ prints the localized form, as OOOO does.
                    parseLocalizedOffset(p);
                    return;
                }
                p.setOffset(offsetOfWidth(p, count, t.letter == 'Z'), p.pos);
                return;
            case 'x':
                p.setOffset(offsetOfWidth(p, count, false), p.pos);
                return;
            default:
                throw new IllegalStateException(String.valueOf(t.letter));
        }
    }

    /// Parses the offset form one width of `X`, `x` or `Z` prints, and no
    /// other: `X` is +HH with optional minutes, `XX` +HHMM, `XXX` +HH:MM,
    /// `XXXX` +HHMM with optional seconds, `XXXXX` +HH:MM with optional
    /// seconds; one to three `Z` are +HHMM and five are +HH:MM[:SS].
    private static int offsetOfWidth(Parsed p, int count, boolean zone) {
        if (zone) {
            return count == 5 ? p.offsetSeconds(true, 2, 3) : p.offsetSeconds(false, 2, 2);
        }
        switch (count) {
            case 1:
                return p.offsetSeconds(false, 1, 2);
            case 2:
                return p.offsetSeconds(false, 2, 2);
            case 3:
                return p.offsetSeconds(true, 2, 2);
            case 4:
                return p.offsetSeconds(false, 2, 3);
            default:
                return p.offsetSeconds(true, 2, 3);
        }
    }

    /// The names of `full` in the style `count` asks for.
    private static String[] styled(String[] full, int count) {
        String[] out = new String[full.length];
        for (int i = 0; i < full.length; i++) {
            out[i] = count == 4 ? full[i] : (count == 5 ? full[i].substring(0, 1) : full[i].substring(0, 3));
        }
        return out;
    }

    // ---- what was read, and what it makes ----

    private static final int F_ERA = 0;
    private static final int F_YEAR = 1;
    private static final int F_YEAR_OF_ERA = 2;
    private static final int F_MONTH = 3;
    private static final int F_DAY = 4;
    private static final int F_DAY_OF_YEAR = 5;
    private static final int F_DAY_OF_WEEK = 6;
    private static final int F_QUARTER = 7;
    private static final int F_AMPM = 8;
    private static final int F_HOUR = 9;
    private static final int F_CLOCK_HOUR = 10;
    private static final int F_HOUR_AMPM = 11;
    private static final int F_CLOCK_HOUR_AMPM = 12;
    private static final int F_MINUTE = 13;
    private static final int F_SECOND = 14;
    private static final int F_NANO = 15;
    private static final int F_NANO_OF_DAY = 16;
    private static final int F_MILLI_OF_DAY = 17;
    private static final int FIELD_COUNT = 18;

    private static final String[] FIELD_NAMES = {"Era", "Year", "YearOfEra", "MonthOfYear", "DayOfMonth",
        "DayOfYear", "DayOfWeek", "QuarterOfYear", "AmPmOfDay", "HourOfDay", "ClockHourOfDay", "HourOfAmPm",
        "ClockHourOfAmPm", "MinuteOfHour", "SecondOfMinute", "NanoOfSecond", "NanoOfDay", "MilliOfDay"};
    private static final long[] FIELD_MIN = {0, -999999999L, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0};
    private static final long[] FIELD_MAX = {1, 999999999L, 1000000000L, 12, 31, 366, 7, 4, 1, 23, 24, 11, 12, 59,
        59, 999999999L, 86399999999999L, 86399999L};

    /// The state of one parse: where it is in the text and every field and
    /// offset read so far.
    private static final class Parsed implements TemporalAccessor {
        final String text;
        int pos;
        final long[] values = new long[FIELD_COUNT];
        final boolean[] present = new boolean[FIELD_COUNT];
        /// Whether the time read was 24:00, which ends the day it was read
        /// with and so moves a date read beside it on by one.
        private boolean endOfDay;
        ZoneOffset offset;
        ZoneId zone;

        Parsed(String text) {
            this.text = text;
        }

        Parsed copy() {
            Parsed c = new Parsed(text);
            c.pos = pos;
            System.arraycopy(values, 0, c.values, 0, FIELD_COUNT);
            System.arraycopy(present, 0, c.present, 0, FIELD_COUNT);
            c.offset = offset;
            c.zone = zone;
            return c;
        }

        void restore(Parsed saved) {
            pos = saved.pos;
            System.arraycopy(saved.values, 0, values, 0, FIELD_COUNT);
            System.arraycopy(saved.present, 0, present, 0, FIELD_COUNT);
            offset = saved.offset;
            zone = saved.zone;
        }

        void fail(int index) {
            throw new DateTimeParseException("Text '" + text + "' could not be parsed at index " + index, text, index);
        }

        void set(int field, long value) {
            if (present[field] && values[field] != value) {
                throw new DateTimeParseException("Text '" + text + "' could not be parsed: Conflict found: "
                        + FIELD_NAMES[field] + " " + values[field] + " differs from " + FIELD_NAMES[field] + " "
                        + value, text, 0);
            }
            values[field] = value;
            present[field] = true;
        }

        void setOffset(int seconds, int start) {
            try {
                offset = ZoneOffset.ofTotalSeconds(seconds);
            } catch (DateTimeException ex) {
                fail(start);
            }
        }

        void literal(char c) {
            if (pos >= text.length() || text.charAt(pos) != c) {
                fail(pos);
            }
            pos++;
        }

        long digits(int min, int max) {
            return digits(min, max, 0);
        }

        /// Between `min` and `max` digits, leaving `reserve` of them for
        /// the fixed-width numbers that follow when there are more.
        long digits(int min, int max, int reserve) {
            int start = pos;
            int end = start;
            while (end < text.length() && end - start < max && text.charAt(end) >= '0' && text.charAt(end) <= '9') {
                end++;
            }
            if (reserve > 0 && end - start - reserve >= min) {
                end -= reserve;
            }
            if (end - start < min) {
                fail(start);
            }
            long value = 0;
            for (int i = start; i < end; i++) {
                value = value * 10 + (text.charAt(i) - '0');
                if (value < 0) {
                    fail(start);
                }
            }
            pos = end;
            return value;
        }

        /// A number of one letter (one or more digits) or two (exactly two).
        long number(int count, int reserve) {
            return count == 1 ? digits(1, 19, reserve) : digits(count, count);
        }

        int oneOf(String[] names) {
            int best = -1;
            int bestLength = 0;
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                if (n.length() > bestLength && text.regionMatches(pos, n, 0, n.length())) {
                    best = i;
                    bestLength = n.length();
                }
            }
            if (best < 0) {
                fail(pos);
            }
            pos += bestLength;
            return best;
        }

        /// `+HH`, `+HHMM`, `+HH:MM`, `+HHMMss` or `+HH:MM:ss`.
        /// An offset of hours, minutes and seconds, each two digits: at least
        /// `required` and at most `allowed` of them, separated by colons
        /// exactly when `colon` is set.
        int offsetSeconds(boolean colon, int required, int allowed) {
            int start = pos;
            if (pos >= text.length() || (text.charAt(pos) != '+' && text.charAt(pos) != '-')) {
                fail(start);
            }
            boolean negative = text.charAt(pos) == '-';
            pos++;
            int[] fields = new int[3];
            fields[0] = (int) digits(2, 2);
            int found = 1;
            while (found < allowed) {
                int mark = pos;
                if (colon) {
                    if (pos >= text.length() || text.charAt(pos) != ':') {
                        break;
                    }
                    pos++;
                }
                if (pos + 1 < text.length() && isDigit(pos) && isDigit(pos + 1)) {
                    fields[found++] = (int) digits(2, 2);
                } else {
                    pos = mark;
                    break;
                }
            }
            if (found < required || fields[1] > 59 || fields[2] > 59) {
                fail(start);
            }
            int total = fields[0] * 3600 + fields[1] * 60 + fields[2];
            if (total > 18 * 3600) {
                fail(start);
            }
            return negative ? -total : total;
        }

        int offsetSeconds() {
            int start = pos;
            if (pos >= text.length() || (text.charAt(pos) != '+' && text.charAt(pos) != '-')) {
                fail(start);
            }
            boolean negative = text.charAt(pos) == '-';
            pos++;
            int hours = (int) digits(2, 2);
            int minutes = 0;
            int seconds = 0;
            boolean colon = pos < text.length() && text.charAt(pos) == ':';
            if (colon) {
                pos++;
            }
            if (pos + 1 < text.length() && isDigit(pos) && isDigit(pos + 1)) {
                minutes = (int) digits(2, 2);
                if (colon ? pos < text.length() && text.charAt(pos) == ':' : true) {
                    int mark = pos;
                    if (colon) {
                        pos++;
                    }
                    if (pos + 1 < text.length() && isDigit(pos) && isDigit(pos + 1)) {
                        seconds = (int) digits(2, 2);
                    } else {
                        pos = mark;
                    }
                }
            } else if (colon) {
                fail(pos);
            }
            if (minutes > 59 || seconds > 59) {
                fail(start);
            }
            int total = hours * 3600 + minutes * 60 + seconds;
            if (total > 18 * 3600) {
                fail(start);
            }
            return negative ? -total : total;
        }

        private boolean isDigit(int i) {
            char c = text.charAt(i);
            return c >= '0' && c <= '9';
        }

        private void check(int field, boolean strict) {
            long v = values[field];
            if (!strict) {
                // The smart resolver takes 24:00 as the end of the day and
                // a clock hour of 0 as midnight, as the JDK's does.
                if (field == F_HOUR && v == 24) {
                    return;
                }
                if ((field == F_CLOCK_HOUR || field == F_CLOCK_HOUR_AMPM) && v == 0) {
                    return;
                }
            }
            if (present[field] && (v < FIELD_MIN[field] || v > FIELD_MAX[field])) {
                throw new DateTimeException("Invalid value for " + FIELD_NAMES[field] + " (valid values "
                        + FIELD_MIN[field] + " - " + FIELD_MAX[field] + "): " + v);
            }
        }

        /// The date, time, offset and zone that were read, as the class
        /// they make: a zoned or offset date and time, a date and time, a
        /// date, a time, or this when they make none of those.
        TemporalAccessor resolve(ZoneId overrideZone, boolean strict) {
            for (int f = 0; f < FIELD_COUNT; f++) {
                check(f, strict);
            }
            LocalDate date = resolveDate(strict);
            LocalTime time = resolveTime();
            ZoneId z = zone != null ? zone : overrideZone;
            if (date != null && time != null) {
                LocalDateTime ldt = LocalDateTime.of(date, time);
                if (endOfDay) {
                    ldt = ldt.plusDays(1);
                }
                if (offset != null) {
                    if (z == null) {
                        return OffsetDateTime.of(ldt, offset);
                    }
                    return ZonedDateTime.ofInstant(ldt.toInstant(offset), z);
                }
                if (z != null) {
                    return ZonedDateTime.of(ldt, z);
                }
                return ldt;
            }
            if (date != null) {
                return date;
            }
            if (time != null) {
                return time;
            }
            return this;
        }

        private LocalDate resolveDate(boolean strict) {
            Long year = null;
            if (present[F_YEAR]) {
                year = Long.valueOf(values[F_YEAR]);
            }
            if (present[F_YEAR_OF_ERA]) {
                long yoe = values[F_YEAR_OF_ERA];
                long fromEra = !present[F_ERA] || values[F_ERA] == 1 ? yoe : 1 - yoe;
                if (year != null && year.longValue() != fromEra) {
                    throw new DateTimeException("Conflict found: Year " + year + " differs from Year " + fromEra);
                }
                year = Long.valueOf(fromEra);
            } else if (present[F_ERA] && year != null && (year.longValue() >= 1 ? 1 : 0) != values[F_ERA]) {
                throw new DateTimeException("Conflict found: Era " + values[F_ERA] + " differs from year " + year);
            }
            LocalDate date = null;
            if (year != null && present[F_MONTH] && present[F_DAY]) {
                int y = (int) year.longValue();
                int m = (int) values[F_MONTH];
                int d = (int) values[F_DAY];
                if (!strict) {
                    int length = LocalDate.of(y, m, 1).lengthOfMonth();
                    if (d > length) {
                        d = length;
                    }
                }
                date = LocalDate.of(y, m, d);
            } else if (year != null && present[F_DAY_OF_YEAR]) {
                date = LocalDate.ofYearDay((int) year.longValue(), (int) values[F_DAY_OF_YEAR]);
            }
            if (date != null) {
                if (present[F_DAY_OF_WEEK] && date.getDayOfWeek().getValue() != values[F_DAY_OF_WEEK]) {
                    throw new DateTimeException("Conflict found: Field DayOfWeek " + date.getDayOfWeek().getValue()
                            + " differs from DayOfWeek " + values[F_DAY_OF_WEEK] + " derived from " + date);
                }
                if (present[F_QUARTER] && (date.getMonthValue() - 1) / 3 + 1 != values[F_QUARTER]) {
                    throw new DateTimeException("Conflict found: Field QuarterOfYear differs from " + date);
                }
            }
            return date;
        }

        private LocalTime resolveTime() {
            Long hour = null;
            if (present[F_HOUR]) {
                hour = Long.valueOf(values[F_HOUR]);
            }
            if (present[F_CLOCK_HOUR]) {
                long h = values[F_CLOCK_HOUR] == 24 ? 0 : values[F_CLOCK_HOUR];
                hour = conflict(hour, h);
            }
            if (present[F_AMPM]) {
                if (present[F_CLOCK_HOUR_AMPM]) {
                    long h = values[F_CLOCK_HOUR_AMPM] == 12 ? 0 : values[F_CLOCK_HOUR_AMPM];
                    hour = conflict(hour, values[F_AMPM] * 12 + h);
                }
                if (present[F_HOUR_AMPM]) {
                    hour = conflict(hour, values[F_AMPM] * 12 + values[F_HOUR_AMPM]);
                }
            }
            if (hour == null) {
                if (present[F_NANO_OF_DAY]) {
                    return LocalTime.ofNanoOfDay(values[F_NANO_OF_DAY]);
                }
                if (present[F_MILLI_OF_DAY]) {
                    return LocalTime.ofNanoOfDay(values[F_MILLI_OF_DAY] * 1000000L);
                }
                return null;
            }
            if (present[F_AMPM] && hour.longValue() / 12 != values[F_AMPM]) {
                throw new DateTimeException("Conflict found: AmPmOfDay " + values[F_AMPM] + " differs from hour "
                        + hour);
            }
            boolean minute = present[F_MINUTE];
            boolean second = present[F_SECOND];
            boolean nano = present[F_NANO];
            if ((!minute && (second || nano)) || (minute && !second && nano)) {
                return null;
            }
            if (hour.longValue() == 24) {
                if ((minute && values[F_MINUTE] != 0) || (second && values[F_SECOND] != 0)
                        || (nano && values[F_NANO] != 0)) {
                    throw new DateTimeException("Invalid value for HourOfDay (valid values 0 - 23): 24");
                }
                endOfDay = true;
                return LocalTime.MIDNIGHT;
            }
            return LocalTime.of((int) hour.longValue(), minute ? (int) values[F_MINUTE] : 0,
                    second ? (int) values[F_SECOND] : 0, nano ? (int) values[F_NANO] : 0);
        }

        private static Long conflict(Long hour, long value) {
            if (hour != null && hour.longValue() != value) {
                throw new DateTimeException("Conflict found: HourOfDay " + hour + " differs from HourOfDay " + value);
            }
            return Long.valueOf(value);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (int f = 0; f < FIELD_COUNT; f++) {
                if (present[f]) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(FIELD_NAMES[f]).append('=').append(values[f]);
                    first = false;
                }
            }
            sb.append("},ISO");
            if (zone != null) {
                sb.append(',').append(zone);
            } else if (offset != null) {
                sb.append(',').append(offset);
            }
            return sb.toString();
        }
    }

    // ---- what a value to format has ----

    /// The date, time, offset and zone a value carries, after the
    /// formatter's override zone is applied.
    private static final class Fields {
        LocalDate date;
        LocalTime time;
        ZoneOffset offsetValue;
        ZoneId zone;
        Long epochSecond;
        int nano;
        DayOfWeek dayOfWeek;

        static Fields of(TemporalAccessor temporal, ZoneId override) {
            Fields f = new Fields();
            if (override != null) {
                Instant instant = null;
                if (temporal instanceof Instant) {
                    instant = (Instant) temporal;
                } else if (temporal instanceof OffsetDateTime) {
                    instant = ((OffsetDateTime) temporal).toInstant();
                } else if (temporal instanceof ZonedDateTime && !((ZonedDateTime) temporal).getZone().equals(override)) {
                    instant = ((ZonedDateTime) temporal).toInstant();
                }
                if (instant != null) {
                    temporal = ZonedDateTime.ofInstant(instant, override);
                }
            }
            if (temporal instanceof ZonedDateTime) {
                ZonedDateTime z = (ZonedDateTime) temporal;
                f.date = z.toLocalDate();
                f.time = z.toLocalTime();
                f.offsetValue = z.getOffset();
                f.zone = z.getZone();
                f.epochSecond = Long.valueOf(z.toEpochSecond());
                f.nano = z.getNano();
            } else if (temporal instanceof OffsetDateTime) {
                OffsetDateTime o = (OffsetDateTime) temporal;
                f.date = o.toLocalDate();
                f.time = o.toLocalTime();
                f.offsetValue = o.getOffset();
                f.epochSecond = Long.valueOf(o.toEpochSecond());
                f.nano = o.getNano();
            } else if (temporal instanceof LocalDateTime) {
                LocalDateTime l = (LocalDateTime) temporal;
                f.date = l.toLocalDate();
                f.time = l.toLocalTime();
                f.nano = l.getNano();
            } else if (temporal instanceof LocalDate) {
                f.date = (LocalDate) temporal;
            } else if (temporal instanceof LocalTime) {
                f.time = (LocalTime) temporal;
                f.nano = f.time.getNano();
            } else if (temporal instanceof Instant) {
                Instant i = (Instant) temporal;
                f.epochSecond = Long.valueOf(i.getEpochSecond());
                f.nano = i.getNano();
            } else if (temporal instanceof DayOfWeek) {
                f.dayOfWeek = (DayOfWeek) temporal;
            } else if (temporal instanceof ZoneOffset) {
                f.offsetValue = (ZoneOffset) temporal;
            }
            if (f.zone == null && override != null && !(temporal instanceof Instant)) {
                f.zone = override;
            }
            return f;
        }

        LocalDate date(String field) {
            if (date == null) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            return date;
        }

        LocalTime time(String field) {
            if (time == null) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            return time;
        }

        ZoneOffset offset(String field) {
            if (offsetValue == null) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            return offsetValue;
        }

        DayOfWeek dayOfWeek() {
            if (dayOfWeek != null) {
                return dayOfWeek;
            }
            return date("DayOfWeek").getDayOfWeek();
        }

        int nanoOfSecond() {
            if (time == null && epochSecond == null) {
                throw new UnsupportedTemporalTypeException("Unsupported field: NanoOfSecond");
            }
            return nano;
        }
    }

    // ---- the pattern itself ----

    private static final class Token {
        static final int FIELD = 0;
        static final int LITERAL = 1;
        static final int OPTIONAL_START = 2;
        static final int OPTIONAL_END = 3;

        final int kind;
        final char letter;
        final int count;
        final String text;

        Token(int kind, char letter, int count, String text) {
            this.kind = kind;
            this.letter = letter;
            this.count = count;
            this.text = text;
        }
    }

    private static final String SUPPORTED = "GuyMLQqdDEahKkHmsSnNAVzOXxZ";
    private static final String KNOWN_UNSUPPORTED = "YwWecFgvBp";

    private static Token[] compile(String pattern) {
        ArrayList<Token> out = new ArrayList<Token>();
        int depth = 0;
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                int start = i;
                while (i < pattern.length() && pattern.charAt(i) == c) {
                    i++;
                }
                int count = i - start;
                if (KNOWN_UNSUPPORTED.indexOf(c) >= 0) {
                    throw new IllegalArgumentException("Pattern letter not supported here: " + c);
                }
                if (SUPPORTED.indexOf(c) < 0) {
                    throw new IllegalArgumentException("Unknown pattern letter: " + c);
                }
                validateCount(c, count);
                out.add(new Token(Token.FIELD, c, count, null));
            } else if (c == '\'') {
                int start = i++;
                StringBuilder lit = new StringBuilder();
                boolean closed = false;
                while (i < pattern.length()) {
                    char q = pattern.charAt(i);
                    if (q == '\'') {
                        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                            lit.append('\'');
                            i += 2;
                            continue;
                        }
                        closed = true;
                        i++;
                        break;
                    }
                    lit.append(q);
                    i++;
                }
                if (!closed) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string literal: " + pattern);
                }
                if (i - start == 2) {
                    lit.append('\'');
                }
                out.add(new Token(Token.LITERAL, '\0', 0, lit.toString()));
            } else if (c == '[') {
                depth++;
                out.add(new Token(Token.OPTIONAL_START, '\0', 0, null));
                i++;
            } else if (c == ']') {
                if (depth == 0) {
                    throw new IllegalArgumentException("Pattern invalid as it contains ] without previous [");
                }
                depth--;
                out.add(new Token(Token.OPTIONAL_END, '\0', 0, null));
                i++;
            } else if (c == '{' || c == '}' || c == '#') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + c + "'");
            } else {
                out.add(new Token(Token.LITERAL, '\0', 0, String.valueOf(c)));
                i++;
            }
        }
        while (depth-- > 0) {
            out.add(new Token(Token.OPTIONAL_END, '\0', 0, null));
        }
        return out.toArray(new Token[out.size()]);
    }

    private static void validateCount(char c, int count) {
        int max;
        switch (c) {
            case 'd':
            case 'h':
            case 'H':
            case 'k':
            case 'K':
            case 'm':
            case 's':
                max = 2;
                break;
            case 'D':
                max = 3;
                break;
            case 'a':
                max = 1;
                break;
            case 'S':
                max = 9;
                break;
            case 'z':
                max = 4;
                break;
            case 'u':
            case 'y':
            case 'n':
            case 'N':
            case 'A':
                max = 19;
                break;
            default:
                max = 5;
                break;
        }
        if (count > max) {
            throw new IllegalArgumentException("Too many pattern letters: " + c);
        }
        if (c == 'V' && count != 2) {
            throw new IllegalArgumentException("Pattern letter count must be 2: " + c);
        }
        if (c == 'O' && count != 1 && count != 4) {
            throw new IllegalArgumentException("Pattern letter count must be 1 or 4: " + c);
        }
    }

    public String toString() {
        switch (kind) {
            case KIND_LOCAL_DATE:
                return "ISO_LOCAL_DATE";
            case KIND_LOCAL_TIME:
                return "ISO_LOCAL_TIME";
            case KIND_LOCAL_DATE_TIME:
                return "ISO_LOCAL_DATE_TIME";
            case KIND_OFFSET_DATE_TIME:
                return "ISO_OFFSET_DATE_TIME";
            case KIND_ZONED_DATE_TIME:
                return "ISO_ZONED_DATE_TIME";
            case KIND_INSTANT:
                return "ISO_INSTANT";
            default:
                return pattern;
        }
    }
}
