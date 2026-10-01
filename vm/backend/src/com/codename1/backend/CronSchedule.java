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
package com.codename1.backend;

import java.util.TimeZone;

/// When a cron job fires: six sets of allowed values, held as bit masks.
///
/// The build parses every literal `@Scheduled(cron = ...)` expression
/// and writes the masks into the entry point it generates, so a server never
/// parses one and a malformed one never reaches a server. [#parse] exists
/// for the other case -- an expression read from configuration, which only exists
/// at start-up -- and implements the same grammar; the plugin's tests hold the two
/// to agreeing on every expression they know.
///
/// The grammar is Spring's six fields: second, minute, hour, day of month,
/// month, day of week. Each is `*` (or `?` for the two day fields), a
/// value, a range `a-b`, a step `*``/n` or `a-b/n` or
/// `a/n`, or a comma-separated list of those. Months and days take their
/// three-letter English names; Sunday is 0 or 7. `L` as the day of month is
/// the last day of the month. The macros `@yearly`, `@annually`,
/// `@monthly`, `@weekly`, `@daily`, `@midnight` and
/// `@hourly` stand for their usual expressions.
///
/// When both day fields are restricted a day must match BOTH, which is
/// Spring's rule rather than Unix cron's either-or.
public final class CronSchedule {
    /// Bit n set when second n fires.
    private final long seconds;
    private final long minutes;
    private final long hours;
    /// Bits 1-31.
    private final long daysOfMonth;
    /// Bits 1-12.
    private final long months;
    /// Bits 0-6, Sunday = 0.
    private final long daysOfWeek;
    private final boolean lastDayOfMonth;
    private final String zone;
    /// Milliseconds east of UTC, for UTC and a fixed offset.
    private final int fixedOffset;
    /// Null for UTC and a fixed offset.
    private final TimeZone timeZone;
    private final String expression;

    private static final long DAY = 86400000L;
    /// How many steps to look for a match before concluding there is none. A full
    /// Gregorian cycle -- 400 years, 146097 days, after which weekdays and leap
    /// days repeat exactly -- because day of month AND day of week must both
    /// match, and a valid schedule can go decades between matches:
    /// `0 0 0 29 2 MON` fired in 2016 and next fires in 2044. A step that
    /// rejects a day moves to the next day or month, so a cycle's worth of steps
    /// -- plus a margin for the few that stay within a day at a DST transition --
    /// covers every date there is; a schedule that matches nothing in a cycle
    /// matches nothing ever.
    private static final int SEARCH_DAYS = 146097 + 366;

    /// The masks the build computed. Called by generated code.
    ///
    /// #### Parameters
    ///
    /// - `zone`: @param zone a zone ID, a fixed offset such as `+02:00`, or null or
    /// empty for UTC
    public CronSchedule(long seconds, long minutes, long hours, long daysOfMonth,
                        long months, long daysOfWeek, boolean lastDayOfMonth, String zone,
                        String expression) {
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.lastDayOfMonth = lastDayOfMonth;
        this.expression = expression;
        this.zone = zone == null || zone.length() == 0 ? "UTC" : zone;
        int fixed = parseFixedOffset(this.zone);
        if (fixed != Integer.MIN_VALUE) {
            fixedOffset = fixed;
            timeZone = null;
        } else {
            fixedOffset = 0;
            timeZone = TimeZone.getTimeZone(this.zone);
            if (!knownZone(this.zone, timeZone)) {
                // A misspelt zone is not an error to TimeZone: the JDK answers
                // GMT, and the job would fire at UTC times instead of local ones
                // with nothing said. The build checks a literal zone; this is the
                // check for one from configuration or code.
                throw new IllegalArgumentException("Unknown time zone \"" + this.zone
                        + "\" for cron expression " + expression);
            }
        }
        if (seconds == 0 || minutes == 0 || hours == 0 || months == 0 || daysOfWeek == 0
                || (daysOfMonth == 0 && !lastDayOfMonth)) {
            throw new IllegalArgumentException("A cron field allows no value: " + expression);
        }
    }

    /// Whether `id` names a zone this runtime knows. The JDK falls back to
    /// GMT for an id it does not know, which is detectable; where a tz database
    /// is installed the id must name one of its files, which also covers the
    /// packaged runtime, whose platform lookup cannot say "unknown". With neither
    /// to consult, the id is taken as given.
    static boolean knownZone(String id, TimeZone tz) {
        if ("UTC".equals(id) || "GMT".equalsIgnoreCase(id)) {
            return true;
        }
        if (id.indexOf("..") >= 0 || id.startsWith("/")) {
            return false;
        }
        if ("GMT".equals(tz.getID())) {
            return false;
        }
        java.io.File database = new java.io.File("/usr/share/zoneinfo");
        if (database.isDirectory()) {
            return new java.io.File(database, id).isFile();
        }
        return true;
    }

    /// The expression this was made from, for a listing.
    public String getExpression() {
        return expression;
    }

    /// The zone it is read in.
    public String getZone() {
        return zone;
    }

    /// Parses an expression at run time, for one that came from configuration.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: naming what is wrong
    public static CronSchedule parse(String expression, String zone) {
        if (expression == null) {
            throw new IllegalArgumentException("No cron expression");
        }
        String text = expression.trim();
        String macro = macro(text);
        if (macro != null) {
            text = macro;
        }
        String[] fields = split(text);
        if (fields.length != 6) {
            throw new IllegalArgumentException("A cron expression has six fields -- second, "
                    + "minute, hour, day of month, month, day of week -- and \"" + expression
                    + "\" has " + fields.length + ". (A five-field Unix expression needs a "
                    + "leading 0 for the second.)");
        }
        boolean last = "L".equalsIgnoreCase(fields[3]);
        long dom = last ? 0 : field(fields[3], 1, 31, null, true, expression, "day of month");
        long dow = field(fields[5], 0, 7, DAYS, true, expression, "day of week");
        if ((dow & (1L << 7)) != 0) {
            dow = (dow & ~(1L << 7)) | 1L;
        }
        long months = field(fields[4], 1, 12, MONTHS, false, expression, "month");
        if (!last && !canEverMatch(dom, months)) {
            // What the build refuses for a literal expression, refused here for
            // one read from configuration: accepted, it would start and then
            // never fire, and the scheduler would quietly disable the job.
            throw new IllegalArgumentException("\"" + expression + "\" names no day that "
                    + "exists in the months it allows, so it would never fire");
        }
        return new CronSchedule(
                field(fields[0], 0, 59, null, false, expression, "second"),
                field(fields[1], 0, 59, null, false, expression, "minute"),
                field(fields[2], 0, 23, null, false, expression, "hour"),
                dom, months, dow, last, zone, expression);
    }

    /// Whether some allowed day of month exists in some allowed month.
    private static boolean canEverMatch(long days, long months) {
        int[] lengths = {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int m = 1 ; m <= 12 ; m++) {
            if ((months & (1L << m)) == 0) {
                continue;
            }
            for (int d = 1 ; d <= lengths[m] ; d++) {
                if ((days & (1L << d)) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] DAYS = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    /// The expansion of a macro, or null.
    public static String macro(String text) {
        // equalsIgnoreCase, never toLowerCase: the latter follows the device
        // locale, and under a Turkish one "@HOURLY" folds its I to a dotless i.
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
        java.util.List out = new java.util.ArrayList();
        int start = -1;
        for (int iter = 0 ; iter <= text.length() ; iter++) {
            boolean space = iter == text.length() || text.charAt(iter) == ' '
                    || text.charAt(iter) == '\t';
            if (space) {
                if (start >= 0) {
                    out.add(text.substring(start, iter));
                    start = -1;
                }
            } else if (start < 0) {
                start = iter;
            }
        }
        String[] result = new String[out.size()];
        for (int iter = 0 ; iter < result.length ; iter++) {
            result[iter] = (String) out.get(iter);
        }
        return result;
    }

    private static long field(String text, int min, int max, String[] names, boolean question,
                              String expression, String what) {
        if ("*".equals(text) || (question && "?".equals(text))) {
            return range(min, max, 1);
        }
        long mask = 0;
        int start = 0;
        while (start <= text.length()) {
            int comma = text.indexOf(',', start);
            if (comma < 0) {
                comma = text.length();
            }
            String part = text.substring(start, comma);
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
                throw new IllegalArgumentException("The " + what + " field of \"" + expression
                        + "\" names " + part + ", outside " + min + "-" + max);
            }
            mask |= range(from, to, step);
            start = comma + 1;
        }
        return mask;
    }

    private static int number(String text, String[] names, int base, String expression,
                              String what) {
        if (names != null) {
            for (int iter = 0 ; iter < names.length ; iter++) {
                if (names[iter].equalsIgnoreCase(text)) {
                    return iter + base;
                }
            }
        }
        if (text.length() == 0 || text.length() > 4) {
            throw bad(expression, what, text);
        }
        int value = 0;
        for (int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                throw bad(expression, what, text);
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    private static IllegalArgumentException bad(String expression, String what, String text) {
        return new IllegalArgumentException("The " + what + " field of \"" + expression
                + "\" cannot be read: \"" + text + "\"");
    }

    private static long range(int from, int to, int step) {
        long mask = 0;
        for (int iter = from ; iter <= to ; iter += step) {
            mask |= 1L << iter;
        }
        return mask;
    }

    /// Milliseconds east of UTC for "UTC", "Z", "GMT" or "+hh:mm"; MIN_VALUE otherwise.
    public static int parseFixedOffset(String zone) {
        if ("UTC".equalsIgnoreCase(zone) || "Z".equalsIgnoreCase(zone)
                || "GMT".equalsIgnoreCase(zone)) {
            return 0;
        }
        if (zone.length() != 6 || (zone.charAt(0) != '+' && zone.charAt(0) != '-')
                || zone.charAt(3) != ':') {
            return Integer.MIN_VALUE;
        }
        int h = digits(zone, 1);
        int m = digits(zone, 4);
        if (h < 0 || m < 0 || h > 18 || m > 59) {
            return Integer.MIN_VALUE;
        }
        int offset = (h * 60 + m) * 60000;
        return zone.charAt(0) == '-' ? -offset : offset;
    }

    private static int digits(String s, int at) {
        char a = s.charAt(at);
        char b = s.charAt(at + 1);
        if (a < '0' || a > '9' || b < '0' || b > '9') {
            return -1;
        }
        return (a - '0') * 10 + (b - '0');
    }

    /// The first time strictly after `afterMillis` this fires, as epoch
    /// milliseconds, or -1 if it never does (the 30th of February).
    public long next(long afterMillis) {
        long t = afterMillis - floorMod(afterMillis, 1000L) + 1000L;
        for (int guard = 0 ; guard < SEARCH_DAYS ; guard++) {
            long local = t + offsetAt(t);
            long day = floorDiv(local, DAY);
            int timeOfDay = (int) (local - day * DAY);
            int[] civil = civilFromDays(day);
            int month = civil[1];
            if ((months & (1L << month)) == 0) {
                t = startOfLocalDay(firstDayOfNextMonth(civil), t);
                continue;
            }
            if (!dayMatches(civil[0], month, civil[2], (int) floorMod(day + 4, 7))) {
                t = startOfLocalDay(day + 1, t);
                continue;
            }
            int found = nextTimeOfDay(timeOfDay / 1000);
            if (found < 0) {
                t = startOfLocalDay(day + 1, t);
                continue;
            }
            long candidateLocal = day * DAY + found * 1000L;
            long first = localToUtc(candidateLocal, true);
            if (first == Long.MIN_VALUE) {
                // A wall-clock time the zone skips -- 02:30 on a spring-forward
                // night -- does not exist, and toUtc lands on another one (03:30)
                // that the fields never asked for. Skipped, as Quartz does: search
                // on from the first real time after the gap.
                long candidate = toUtc(candidateLocal);
                t = candidate > t ? candidate : t + 1000L;
                continue;
            }
            // A time the clocks pass twice -- 01:30 on a fall-back night -- fires
            // ONCE, at the first of the two, as Quartz and Spring fire it. Once the
            // first has passed that day's is spent, whichever side of the change
            // the search starts on: after a run at the first, the scheduler asks
            // again from just past it, and a search that could still return the
            // second would run a daily job twice that night. A server started
            // inside the repeated hour missed that day's run like any other.
            if (first > afterMillis) {
                return first;
            }
            // Spent: search on from just past its SECOND occurrence, so the other
            // times of that day -- 02:00 for an every-half-hour job -- still count.
            long second = localToUtc(candidateLocal, false);
            t = second >= t ? second + 1000L : t + 1000L;
        }
        return -1;
    }

    private boolean dayMatches(int year, int month, int day, int dow) {
        if ((daysOfWeek & (1L << dow)) == 0) {
            return false;
        }
        if (lastDayOfMonth) {
            return day == daysInMonth(year, month);
        }
        return (daysOfMonth & (1L << day)) != 0;
    }

    /// The first allowed second of the day at or after `second`, or -1.
    private int nextTimeOfDay(int second) {
        int h = second / 3600;
        int m = (second / 60) % 60;
        int s = second % 60;
        for (int hh = h ; hh < 24 ; hh++) {
            if ((hours & (1L << hh)) == 0) {
                continue;
            }
            for (int mm = hh == h ? m : 0 ; mm < 60 ; mm++) {
                if ((minutes & (1L << mm)) == 0) {
                    continue;
                }
                int ss = nextBit(seconds, hh == h && mm == m ? s : 0, 59);
                if (ss >= 0) {
                    return hh * 3600 + mm * 60 + ss;
                }
            }
        }
        return -1;
    }

    private static int nextBit(long mask, int from, int max) {
        for (int iter = from ; iter <= max ; iter++) {
            if ((mask & (1L << iter)) != 0) {
                return iter;
            }
        }
        return -1;
    }

    private long startOfLocalDay(long day, long notBefore) {
        long t = toUtc(day * DAY);
        return t <= notBefore ? notBefore + 1000L : t;
    }

    private static long firstDayOfNextMonth(int[] civil) {
        int y = civil[0];
        int m = civil[1] + 1;
        if (m > 12) {
            m = 1;
            y++;
        }
        return daysFromCivil(y, m, 1);
    }

    /// Milliseconds east of UTC at the instant `utc`.
    private int offsetAt(long utc) {
        if (timeZone == null) {
            return fixedOffset;
        }
        int raw = timeZone.getRawOffset();
        long standard = utc + raw;
        long day = floorDiv(standard, DAY);
        int[] civil = civilFromDays(day);
        int dow = (int) floorMod(day + 4, 7);
        return timeZone.getOffset(1, civil[0], civil[1] - 1, civil[2], dow + 1,
                (int) (standard - day * DAY));
    }

    /// The instant a wall-clock time falls on: the earlier of the two in a
    /// fall-back overlap when `earliest` is set, the later otherwise, and
    /// Long.MIN_VALUE when the zone skips that time. The offsets half a day
    /// either side are the only two it can have: transitions are months apart.
    private long localToUtc(long local, boolean earliest) {
        if (timeZone == null) {
            return local - fixedOffset;
        }
        int raw = timeZone.getRawOffset();
        long before = local - offsetAt(local - raw - DAY / 2);
        long after = local - offsetAt(local - raw + DAY / 2);
        boolean beforeValid = before + offsetAt(before) == local;
        boolean afterValid = after + offsetAt(after) == local;
        if (beforeValid && afterValid) {
            return earliest ? Math.min(before, after) : Math.max(before, after);
        }
        if (beforeValid) {
            return before;
        }
        return afterValid ? after : Long.MIN_VALUE;
    }

    private long toUtc(long local) {
        if (timeZone == null) {
            return local - fixedOffset;
        }
        long guess = local - timeZone.getRawOffset();
        long utc = local - offsetAt(guess);
        // Once more, for a local time on the other side of a transition from the
        // raw-offset guess.
        return local - offsetAt(utc);
    }

    static long floorDiv(long value, long divisor) {
        long q = value / divisor;
        if ((value % divisor != 0) && ((value < 0) != (divisor < 0))) {
            q--;
        }
        return q;
    }

    static long floorMod(long value, long divisor) {
        return value - floorDiv(value, divisor) * divisor;
    }

    static int daysInMonth(int year, int month) {
        switch (month) {
            case 2:
                boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                return leap ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    static long daysFromCivil(int y, int m, int d) {
        int adjusted = y - (m <= 2 ? 1 : 0);
        long era = (adjusted >= 0 ? adjusted : adjusted - 399) / 400;
        int yoe = (int) (adjusted - era * 400);
        int doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468L;
    }

    static int[] civilFromDays(long z) {
        long shifted = z + 719468L;
        long era = (shifted >= 0 ? shifted : shifted - 146096) / 146097;
        long doe = shifted - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        return new int[] {(int) (y + (m <= 2 ? 1 : 0)), (int) m, (int) d};
    }

    @Override
    public String toString() {
        return expression + ("UTC".equals(zone) ? "" : " (" + zone + ")");
    }
}
