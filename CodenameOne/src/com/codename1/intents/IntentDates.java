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
package com.codename1.intents;

import java.util.Date;

/// The one definition of what counts as a moment in time.
///
/// This grammar used to exist only inside the generated coercion, and the framework's own
/// "could this value run?" check had to answer optimistically because of it -- so a donation
/// carrying "not-a-date", or 1.5 where epoch milliseconds belong, was published as a durable
/// shortcut that the coercion then rejected on every replay. Writing a second parser to close
/// that would have been worse: two grammars drifting apart is a harder bug than the one it
/// fixes. So there is one, here, and the generated code calls it.
///
/// Accepts a `Date`, epoch milliseconds as an integral number or a numeric string, or ISO-8601.
/// All three arrive in practice: the platforms send milliseconds, and a language model handed
/// this parameter through `Intents#asTools` writes a date the way it writes dates.
public final class IntentDates {

    private IntentDates() {
    }

    /// The string forms [#parse] accepts, written as a JSON-Schema pattern.
    ///
    /// It lives here rather than in the schema builder because this class owns the grammar --
    /// a pattern kept next to the consumer would drift from the parser the first time either
    /// changed, which is the whole reason the parsing was consolidated in the first place.
    ///
    /// It describes the *shape* and cannot describe the meaning: "2026-13-40" matches and is
    /// still refused, because no regex says a year has twelve months. That is the right split.
    /// The value of the pattern is that it rules out text which was never a date at all --
    /// "not-a-date" was schema-valid before, so a model obeying the schema could make a call
    /// that was certain to fail. Narrowing it to a grammar the parser recognizes removes the
    /// entire class of well-formed-but-unparseable calls; the remaining rejections are values
    /// that look like dates and are not.
    static final String SCHEMA_PATTERN =
            "^(-?[0-9]+"
            + "|[0-9]{4}-[0-9]{2}-[0-9]{2}"
            + "([Tt ][0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]+)?)?"
            + "([Zz]|[+-][0-9]{2}:?[0-9]{2})?)?)$";

    /// The moment this value names, or null when it names none.
    ///
    /// Null rather than an exception because both callers want to decide for themselves what an
    /// unusable value means: dispatch rejects the invocation naming the parameter, while the
    /// donation check refuses to publish a shortcut that could never run.
    public static Date parse(Object o) {
        if (o instanceof Date) {
            return (Date) o;
        }
        // Epoch millis are a number like any other: longValue() would truncate 1.5 and
        // saturate 1e20, naming a moment nobody chose.
        if (o instanceof Long || o instanceof Integer || o instanceof Short
                || o instanceof Byte) {
            return new Date(((Number) o).longValue());
        }
        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (d != Math.floor(d) || Double.isInfinite(d) || Double.isNaN(d)
                    || d < -9223372036854775808.0 || d >= 9223372036854775808.0) {
                return null;
            }
            return new Date(((Number) o).longValue());
        }
        if (o instanceof String) {
            String s = ((String) o).trim();
            if (s.length() == 0) {
                return null;
            }
            try {
                return new Date(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return parseIso8601(s);
            }
        }
        return null;
    }

    /// Parses ISO-8601, strictly, using arithmetic rather than Calendar.
    ///
    /// #### Why not Calendar
    ///
    /// Codename One's `java.util.Calendar` is far smaller than the JDK's: it has `set(int,int)`
    /// and no `clear()`, no `setLenient()` and no multi-argument `set`. The version of this
    /// parser that lived in generated code used all three, so an application declaring a date
    /// parameter could not have compiled for a device at all -- and nothing caught it, because
    /// the processor's tests compile their fixtures against the full JDK. Moving the grammar
    /// here is what surfaced it, since core is built against the reduced runtime.
    ///
    /// Civil-date arithmetic is the better answer anyway. It is exact for the proleptic
    /// Gregorian calendar, which is the calendar ISO-8601 defines, and it is strict by
    /// construction: there is no leniency to switch off, so 2026-13-40 is rejected because 13
    /// is not a month rather than because a flag was set.
    private static Date parseIso8601(String s) {
        if (s.length() < 10 || s.charAt(4) != '-' || s.charAt(7) != '-') {
            return null;
        }
        int offsetMinutes = 0;
        try {
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(5, 7));
            int day = Integer.parseInt(s.substring(8, 10));
            if (month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) {
                return null;
            }
            int hour = 0;
            int minute = 0;
            int second = 0;
            int millis = 0;
            String rest = s.substring(10);
            if (rest.length() > 0) {
                char sep = rest.charAt(0);
                if (sep != 'T' && sep != 't' && sep != ' ') {
                    return null;
                }
                rest = rest.substring(1);
                int zone = -1;
                for (int i = 0; i < rest.length(); i++) {
                    char ch = rest.charAt(i);
                    if (ch == 'Z' || ch == 'z' || ch == '+' || (ch == '-' && i > 0)) {
                        zone = i;
                        break;
                    }
                }
                String time = zone < 0 ? rest : rest.substring(0, zone);
                String tz = zone < 0 ? "" : rest.substring(zone);
                if (time.length() < 5 || time.charAt(2) != ':') {
                    return null;
                }
                hour = Integer.parseInt(time.substring(0, 2));
                minute = Integer.parseInt(time.substring(3, 5));
                if (time.length() >= 8) {
                    if (time.charAt(5) != ':') {
                        return null;
                    }
                    second = Integer.parseInt(time.substring(6, 8));
                    if (time.length() > 8) {
                        if (time.charAt(8) != '.') {
                            return null;
                        }
                        // Only the leading three digits are milliseconds; ISO allows more
                        // precision than a Date can hold, and truncating is the honest answer.
                        String frac = time.substring(9);
                        for (int i = 0; i < frac.length(); i++) {
                            if (frac.charAt(i) < '0' || frac.charAt(i) > '9') {
                                return null;
                            }
                        }
                        if (frac.length() == 0) {
                            return null;
                        }
                        String three = (frac + "000").substring(0, 3);
                        millis = Integer.parseInt(three);
                    }
                } else if (time.length() != 5) {
                    return null;
                }
                if (hour > 23 || minute > 59 || second > 59) {
                    return null;
                }
                if (tz.length() > 0 && (tz.charAt(0) == 'Z' || tz.charAt(0) == 'z')) {
                    // Zulu is the whole suffix or the string is not a date. Taking the Z and
                    // ignoring what followed accepted "...T12:00:00Zjunk", and worse
                    // "...Z+05:00", as UTC -- so a value that names two different moments, or
                    // none, reached a handler looking valid. Every caller shares this parser,
                    // so that reached declared defaults, donations and dispatch alike.
                    if (tz.length() != 1) {
                        return null;
                    }
                } else if (tz.length() > 0) {
                    // Built by hand: this runtime's String has replace(char,char) and not the
                    // CharSequence overload, and an offset may be written +0200 or +02:00.
                    StringBuilder buf = new StringBuilder();
                    String off = tz.substring(1);
                    for (int i = 0; i < off.length(); i++) {
                        if (off.charAt(i) != ':') {
                            buf.append(off.charAt(i));
                        }
                    }
                    String digits = buf.toString();
                    if (digits.length() != 4) {
                        return null;
                    }
                    int oh = Integer.parseInt(digits.substring(0, 2));
                    int om = Integer.parseInt(digits.substring(2, 4));
                    // No real zone is further from UTC than 18 hours, and a number past that is
                    // a typo rather than a place; accepting it would silently move the moment.
                    if (oh > 18 || om > 59 || oh * 60 + om > 18 * 60) {
                        return null;
                    }
                    offsetMinutes = oh * 60 + om;
                    if (tz.charAt(0) == '-') {
                        offsetMinutes = -offsetMinutes;
                    }
                }
            }
            long epochDay = daysFromCivil(year, month, day);
            long millisOfDay = hour * 3600000L + minute * 60000L + second * 1000L + millis;
            return new Date(epochDay * 86400000L + millisOfDay - offsetMinutes * 60000L);
        } catch (NumberFormatException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /// Days from 1970-01-01 to this civil date, proleptic Gregorian.
    ///
    /// Shifting the year to start in March puts the leap day at the end of the cycle, which is
    /// what removes every special case from the arithmetic below.
    private static long daysFromCivil(int year, int month, int day) {
        long y = year;
        y -= month <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    private static int daysInMonth(int year, int month) {
        if (month == 2) {
            boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
            return leap ? 29 : 28;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }
}
