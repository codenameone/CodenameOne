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
package com.codenameone.examples.papercut;

/// Wall-clock time, in one place so day boundaries are computed the same way
/// everywhere (UTC days).
public final class Clock {
    public static final long DAY = 24L * 60 * 60 * 1000;

    private Clock() {
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    /// Midnight UTC at the start of the day `millis` falls in.
    public static long dayOf(long millis) {
        return millis - millis % DAY;
    }

    /// Midnight UTC on the Monday of the week `millis` falls in. The epoch was
    /// a Thursday, hence the three days.
    public static long weekOf(long millis) {
        long day = dayOf(millis) / DAY;
        long sinceMonday = ((day + 3) % 7 + 7) % 7;
        return (day - sinceMonday) * DAY;
    }

    /// The UTC date `millis` falls on, as `yyyy-mm-dd`. Worked out by hand
    /// (the civil-from-days algorithm) so it needs nothing of `java.time` past
    /// `Instant`, which is all the server's class library promises.
    public static String date(long millis) {
        long z = Math.floorDiv(millis, DAY) + 719468;
        long era = Math.floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        if (m <= 2) {
            y++;
        }
        StringBuilder out = new StringBuilder();
        out.append(y).append('-');
        if (m < 10) {
            out.append('0');
        }
        out.append(m).append('-');
        if (d < 10) {
            out.append('0');
        }
        return out.append(d).toString();
    }
}
