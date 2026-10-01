/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven.processors;

import java.util.ArrayList;
import java.util.List;

/// Parses a `@Scheduled(cron = ...)` expression at build time into the bit masks
/// the generated entry point hands to `com.codename1.backend.CronSchedule`.
///
/// The same grammar as that class's `parse`, which a server uses for an
/// expression it reads from configuration. The two are written twice because the
/// plugin cannot depend on the backend runtime, and the plugin's tests hold them
/// to agreeing on every expression they know -- a disagreement would mean a job
/// fires at one time when written literally and another when configured.
final class CronCompiler {
    /// The masks, in the order the CronSchedule constructor takes them.
    long seconds;
    long minutes;
    long hours;
    long daysOfMonth;
    long months;
    long daysOfWeek;
    boolean lastDayOfMonth;

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] DAYS = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private CronCompiler() {
    }

    /// The masks of `expression`, or an IllegalArgumentException naming what is
    /// wrong -- which the processor reports against the method.
    static CronCompiler compile(String expression) {
        if (expression == null || expression.trim().length() == 0) {
            throw new IllegalArgumentException("the cron expression is empty");
        }
        String text = expression.trim();
        String macro = macro(text);
        if (macro != null) {
            text = macro;
        }
        String[] fields = split(text);
        if (fields.length != 6) {
            throw new IllegalArgumentException("a cron expression has six fields -- second, "
                    + "minute, hour, day of month, month, day of week -- and \"" + expression
                    + "\" has " + fields.length + (fields.length == 5
                    ? "; a five-field Unix expression needs a leading 0 for the second" : ""));
        }
        CronCompiler out = new CronCompiler();
        out.seconds = field(fields[0], 0, 59, null, false, expression, "second");
        out.minutes = field(fields[1], 0, 59, null, false, expression, "minute");
        out.hours = field(fields[2], 0, 23, null, false, expression, "hour");
        out.lastDayOfMonth = "L".equalsIgnoreCase(fields[3]);
        out.daysOfMonth = out.lastDayOfMonth ? 0
                : field(fields[3], 1, 31, null, true, expression, "day of month");
        out.months = field(fields[4], 1, 12, MONTHS, false, expression, "month");
        long dow = field(fields[5], 0, 7, DAYS, true, expression, "day of week");
        if ((dow & (1L << 7)) != 0) {
            dow = (dow & ~(1L << 7)) | 1L;
        }
        out.daysOfWeek = dow;
        if (!out.lastDayOfMonth && !canEverMatch(out.daysOfMonth, out.months)) {
            throw new IllegalArgumentException("\"" + expression + "\" names no day that "
                    + "exists in the months it allows, so it would never fire");
        }
        return out;
    }

    /// Whether some allowed day of month exists in some allowed month.
    private static boolean canEverMatch(long days, long months) {
        int[] lengths = {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int m = 1; m <= 12; m++) {
            if ((months & (1L << m)) == 0) {
                continue;
            }
            for (int d = 1; d <= lengths[m]; d++) {
                if ((days & (1L << d)) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    static String macro(String text) {
        if ("@yearly".equalsIgnoreCase(text) || "@annually".equalsIgnoreCase(text)) {
            return "0 0 0 1 1 *";
        }
        if ("@monthly".equalsIgnoreCase(text)) {
            return "0 0 0 1 * *";
        }
        if ("@weekly".equalsIgnoreCase(text)) {
            return "0 0 0 * * 0";
        }
        if ("@daily".equalsIgnoreCase(text) || "@midnight".equalsIgnoreCase(text)) {
            return "0 0 0 * * *";
        }
        if ("@hourly".equalsIgnoreCase(text)) {
            return "0 0 * * * *";
        }
        return null;
    }

    private static String[] split(String text) {
        List<String> out = new ArrayList<String>();
        for (String part : text.split("[ \\t]+")) {
            if (part.length() > 0) {
                out.add(part);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static long field(String text, int min, int max, String[] names, boolean question,
                              String expression, String what) {
        if ("*".equals(text) || (question && "?".equals(text))) {
            return range(min, max, 1);
        }
        long mask = 0;
        for (String part : text.split(",", -1)) {
            if (part.length() == 0) {
                throw bad(expression, what, text);
            }
            int step = 1;
            int slash = part.indexOf('/');
            String span = part;
            if (slash >= 0) {
                step = number(part.substring(slash + 1), null, 0, expression, what);
                if (step <= 0) {
                    throw bad(expression, what, text);
                }
                span = part.substring(0, slash);
            }
            int from;
            int to;
            if ("*".equals(span) || (question && "?".equals(span))) {
                from = min;
                to = max;
            } else {
                int dash = span.indexOf('-');
                if (dash > 0) {
                    from = number(span.substring(0, dash), names, min, expression, what);
                    to = number(span.substring(dash + 1), names, min, expression, what);
                } else {
                    from = number(span, names, min, expression, what);
                    to = slash >= 0 ? max : from;
                }
            }
            if (from < min || to > max || from > to) {
                throw new IllegalArgumentException("the " + what + " field of \"" + expression
                        + "\" names " + part + ", outside " + min + "-" + max);
            }
            mask |= range(from, to, step);
        }
        return mask;
    }

    private static int number(String text, String[] names, int base, String expression,
                              String what) {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(text)) {
                    return i + base;
                }
            }
        }
        if (text.length() == 0 || text.length() > 4) {
            throw bad(expression, what, text);
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                throw bad(expression, what, text);
            }
        }
        return Integer.parseInt(text);
    }

    private static IllegalArgumentException bad(String expression, String what, String text) {
        return new IllegalArgumentException("the " + what + " field of \"" + expression
                + "\" cannot be read: \"" + text + "\"");
    }

    private static long range(int from, int to, int step) {
        long mask = 0;
        for (int i = from; i <= to; i += step) {
            mask |= 1L << i;
        }
        return mask;
    }

    /// Whether `zone` is one the runtime can read: UTC, a fixed offset, or an ID
    /// the JDK knows -- the build's JDK standing in for the server's time zone
    /// database, which has the same IDs.
    static boolean knownZone(String zone) {
        if (zone == null || zone.length() == 0 || "UTC".equalsIgnoreCase(zone)
                || "Z".equalsIgnoreCase(zone) || "GMT".equalsIgnoreCase(zone)) {
            return true;
        }
        if (zone.matches("[+-](0[0-9]|1[0-8]):[0-5][0-9]")) {
            return true;
        }
        for (String id : java.util.TimeZone.getAvailableIDs()) {
            if (id.equals(zone)) {
                return true;
            }
        }
        return false;
    }
}
