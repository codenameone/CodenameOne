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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

    private static Date parseIso8601(String s) {
        if (s.length() < 10 || s.charAt(4) != '-' || s.charAt(7) != '-') {
            return null;
        }
        Calendar c = Calendar.getInstance(
                TimeZone.getTimeZone("GMT"));
        c.clear();
        c.setLenient(false);
        int offsetMinutes = 0;
        try {
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(5, 7));
            int day = Integer.parseInt(s.substring(8, 10));
            int hour = 0;
            int minute = 0;
            int second = 0;
            int millis = 0;
            String rest = s.substring(10);
            if (rest.length() > 0) {
                char sep = rest.charAt(0);
                if (sep != 'T' && sep != 't' && sep != ' ') { return null; }
                rest = rest.substring(1);
                int zone = -1;
                for (int i = 0; i < rest.length(); i++) {
                    char ch = rest.charAt(i);
                    if (ch == 'Z' || ch == 'z' || ch == '+'
                            || (ch == '-' && i > 0)) { zone = i; break; }
                }
                String time = zone < 0 ? rest : rest.substring(0, zone);
                if (zone >= 0) {
                    String z = rest.substring(zone);
                    if (!"Z".equals(z) && !"z".equals(z)) {
                        String digits = z.substring(1).replace(":", "");
                        if (digits.length() != 4) { return null; }
                        int oh = Integer.parseInt(digits.substring(0, 2));
                        int om = Integer.parseInt(digits.substring(2));
                        if (oh > 18 || om > 59 || oh * 60 + om > 18 * 60) {
                            return null;
                        }
                        offsetMinutes = oh * 60 + om;
                        if (z.charAt(0) == '-') { offsetMinutes = -offsetMinutes; }
                    }
                }
                if (time.length() < 5 || time.charAt(2) != ':') { return null; }
                hour = Integer.parseInt(time.substring(0, 2));
                minute = Integer.parseInt(time.substring(3, 5));
                if (time.length() != 5) {
                    if (time.length() < 8 || time.charAt(5) != ':') { return null; }
                    second = Integer.parseInt(time.substring(6, 8));
                    if (time.length() != 8) {
                        if (time.length() < 10 || time.charAt(8) != '.') { return null; }
                        String digits = time.substring(9);
                        for (int f = 0; f < digits.length(); f++) {
                            char fc = digits.charAt(f);
                            if (fc < '0' || fc > '9') { return null; }
                        }
                        millis = Integer.parseInt((digits + "000").substring(0, 3));
                    }
                }
            }
            c.set(year, month - 1, day, hour, minute, second);
            c.set(Calendar.MILLISECOND, millis);
            return new Date(
                    c.getTime().getTime() - offsetMinutes * 60000L);
        } catch (NumberFormatException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
