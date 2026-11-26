/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar;

/// Days, counted in UTC.
///
/// The charts and the earnings are by day, and a day has to mean the same on
/// SQLite, PostgreSQL and MySQL, whose date functions agree on nothing. So the
/// tables hold milliseconds since the epoch, and the arithmetic is done here.
/// UTC unless asked otherwise: a service in one city would shift by that
/// city's offset. A store keeps an IANA time zone (`Store.timeZone`), and the
/// `local` methods below count its days and hours in that zone, daylight
/// saving included, so an evening sale lands on that evening's bar.
public final class Days {
    /// The zone a store has until its merchant picks one.
    public static final String UTC = "UTC";
    private static final long HOUR = 3600000L;
    /// The areas an IANA zone name starts with. The class library cannot list
    /// the zones it knows, so a name is checked by its shape and its area.
    private static final String[] AREAS = {"Africa", "America", "Antarctica", "Arctic", "Asia",
        "Atlantic", "Australia", "Europe", "Indian", "Pacific", "Etc"};
    public static final long MILLIS = 86400000L;

    private Days() {
    }

    /// The number of the day `millis` falls in: whole days since the epoch.
    public static long number(long millis) {
        return millis / MILLIS;
    }

    /// The first millisecond of the day `millis` falls in.
    public static long start(long millis) {
        return millis - millis % MILLIS;
    }

    /// The stretch of time from `from` to `to` cut at each midnight: the
    /// number of each day it touches followed by how many of its milliseconds
    /// fall in that day, day after day. Empty when `to` is not after `from`.
    ///
    /// Time on line is credited to the days it was spent in. Credited whole
    /// to the day it ended in, a span across midnight had the
    /// minutes before it counted as the next day's.
    public static long[] split(long from, long to) {
        if (to <= from) {
            return new long[0];
        }
        long[] out = new long[(int) (number(to - 1) - number(from) + 1) * 2];
        long at = from;
        for (int iter = 0; iter < out.length; iter += 2) {
            long end = Math.min(to, start(at) + MILLIS);
            out[iter] = number(at);
            out[iter + 1] = end - at;
            at = end;
        }
        return out;
    }

    /// The day `millis` falls in, as `yyyy-MM-dd`.
    public static String iso(long millis) {
        // Days since the epoch to a civil date, by the usual era arithmetic:
        // shift the year to start in March so the leap day is its last day.
        long z = number(millis) + 719468L;
        long era = z / 146097L;
        long dayOfEra = z - era * 146097L;
        long yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36524L - dayOfEra / 146096L)
                / 365L;
        long dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L);
        long shifted = (5L * dayOfYear + 2L) / 153L;
        long day = dayOfYear - (153L * shifted + 2L) / 5L + 1L;
        long month = shifted < 10L ? shifted + 3L : shifted - 9L;
        long year = yearOfEra + era * 400L + (month <= 2L ? 1L : 0L);
        StringBuilder out = new StringBuilder(10);
        out.append(year).append('-');
        if (month < 10L) {
            out.append('0');
        }
        out.append(month).append('-');
        if (day < 10L) {
            out.append('0');
        }
        return out.append(day).toString();
    }

    /// Whether `zone` is "UTC" or an IANA zone name such as
    /// `America/New_York`: an area this class knows, then one or two segments
    /// of letters, digits, `_`, `-` and `+`. Nothing else ever reaches
    /// `TimeZone`, so a typo is refused here instead of read as GMT.
    public static boolean isZone(String zone) {
        if (zone == null || zone.length() == 0 || zone.length() > 64) {
            return false;
        }
        if (UTC.equals(zone)) {
            return true;
        }
        int slash = zone.indexOf('/');
        if (slash <= 0 || slash == zone.length() - 1) {
            return false;
        }
        String area = zone.substring(0, slash);
        boolean known = false;
        for (int iter = 0; iter < AREAS.length && !known; iter++) {
            known = AREAS[iter].equals(area);
        }
        if (!known) {
            return false;
        }
        char last = '/';
        int slashes = 0;
        for (int iter = slash + 1; iter < zone.length(); iter++) {
            char c = zone.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '+';
            if (c == '/') {
                slashes++;
                ok = last != '/' && iter < zone.length() - 1 && slashes < 2;
            }
            if (!ok) {
                return false;
            }
            last = c;
        }
        return true;
    }

    /// How far `zone` is ahead of UTC at the instant `millis`, daylight saving
    /// included: what a wall clock there adds to UTC. An empty or unknown
    /// zone is UTC.
    ///
    /// The class library's `TimeZone` answers an offset for calendar fields in
    /// local standard time only, so the instant is first moved by the zone's
    /// standard offset and taken apart here by the same arithmetic as [#iso].
    public static int offset(String zone, long millis) {
        if (zone == null || zone.length() == 0 || UTC.equals(zone) || !isZone(zone)) {
            return 0;
        }
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zone);
        int raw = tz.getRawOffset();
        long standard = millis + raw;
        long day = floorDay(standard);
        int ofDay = (int) (standard - day * MILLIS);
        long z = day + 719468L;
        long era = (z >= 0 ? z : z - 146096L) / 146097L;
        long dayOfEra = z - era * 146097L;
        long yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36524L - dayOfEra / 146096L)
                / 365L;
        long dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L);
        long shifted = (5L * dayOfYear + 2L) / 153L;
        int dom = (int) (dayOfYear - (153L * shifted + 2L) / 5L + 1L);
        int month = (int) (shifted < 10L ? shifted + 3L : shifted - 9L);
        int year = (int) (yearOfEra + era * 400L + (month <= 2 ? 1L : 0L));
        // 1970-01-01 was a Thursday; Calendar counts Sunday as 1.
        int dow = (int) (((day + 4) % 7 + 7) % 7) + 1;
        return tz.getOffset(1, year, month - 1, dom, dow, ofDay);
    }

    private static long floorDay(long millis) {
        long d = millis / MILLIS;
        return millis < 0 && d * MILLIS != millis ? d - 1 : d;
    }

    /// The number of the day `millis` falls in on a wall clock in `zone`.
    public static long localNumber(String zone, long millis) {
        return floorDay(millis + offset(zone, millis));
    }

    /// The hour, 0 to 23, that a wall clock in `zone` shows at `millis`.
    public static int localHour(String zone, long millis) {
        long local = millis + offset(zone, millis);
        return (int) ((local - floorDay(local) * MILLIS) / HOUR);
    }

    /// The date `millis` falls on in `zone`, `yyyy-MM-dd`.
    public static String localIso(String zone, long millis) {
        return iso(localNumber(zone, millis) * MILLIS);
    }

    /// The instant local midnight begins day number `day` in `zone`. The
    /// offset is asked twice, at the first guess and again at the answer, so a
    /// day that starts on either side of a daylight saving change is right.
    public static long localDayStart(String zone, long day) {
        long wall = day * MILLIS;
        long guess = wall - offset(zone, wall);
        return wall - offset(zone, guess);
    }

    /// The instant the local day `millis` falls in began, in `zone`.
    public static long localStart(String zone, long millis) {
        return localDayStart(zone, localNumber(zone, millis));
    }

    /// The number of the day `yyyy-MM-dd` names (whole days since the epoch,
    /// as [#number] counts them), or `Long.MIN_VALUE` for text that is not a
    /// date ([#isIso]).
    public static long numberOf(String iso) {
        if (!isIso(iso)) {
            return Long.MIN_VALUE;
        }
        long y = Long.parseLong(iso.substring(0, 4));
        long m = Long.parseLong(iso.substring(5, 7));
        long d = Long.parseLong(iso.substring(8, 10));
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// Whether `text` is a date written `yyyy-MM-dd`, with a month and a day
    /// that could exist.
    public static boolean isIso(String text) {
        if (text == null || text.length() != 10 || text.charAt(4) != '-' || text.charAt(7) != '-') {
            return false;
        }
        for (int iter = 0; iter < 10; iter++) {
            char c = text.charAt(iter);
            if (iter != 4 && iter != 7 && (c < '0' || c > '9')) {
                return false;
            }
        }
        int year = 0;
        for (int iter = 0; iter < 4; iter++) {
            year = year * 10 + (text.charAt(iter) - '0');
        }
        int month = (text.charAt(5) - '0') * 10 + (text.charAt(6) - '0');
        int day = (text.charAt(8) - '0') * 10 + (text.charAt(9) - '0');
        // The day is held to its own month: these strings are compared as
        // text and never parsed, so a 31st of February accepted here would be
        // stored, and approved, as somebody's date of birth.
        return month >= 1 && month <= 12 && day >= 1 && day <= length(year, month);
    }

    // How many days a month has, by the Gregorian rule for leap years.
    private static int length(int year, int month) {
        if (month == 2) {
            boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
            return leap ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }
}
