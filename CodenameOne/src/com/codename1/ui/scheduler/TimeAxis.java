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
package com.codename1.ui.scheduler;

import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

import java.time.ZoneId;

/// Time laid out across: which pixel an instant is at, what the ticks are at
/// the current zoom, and the two rows of labels above them.
///
/// The resource timeline and the Gantt chart share it. It holds a stretch of
/// time and a scale in milliseconds per pixel, and picks the units of its
/// ticks from the scale: hours under days when zoomed in, through days under
/// months, to quarters under years.
final class TimeAxis {
    static final int HOUR = 0;
    static final int DAY = 1;
    static final int WEEK = 2;
    static final int MONTH = 3;
    static final int QUARTER = 4;
    static final int YEAR = 5;

    // The ladder of minor ticks, finest first: a unit and how many of it.
    private static final int[] LADDER_UNIT = {HOUR, HOUR, HOUR, HOUR, HOUR, DAY, WEEK, MONTH, QUARTER, YEAR};
    private static final int[] LADDER_COUNT = {1, 2, 3, 6, 12, 1, 1, 1, 1, 1};
    private static final long[] LADDER_MILLIS = {TimeMath.HOUR, 2 * TimeMath.HOUR, 3 * TimeMath.HOUR,
        6 * TimeMath.HOUR, 12 * TimeMath.HOUR, TimeMath.DAY, 7 * TimeMath.DAY, 30 * TimeMath.DAY,
        91 * TimeMath.DAY, 365 * TimeMath.DAY};

    private ZoneId zone;
    private int firstDayOfWeek = 1;
    private boolean h24;
    private long start;
    private long end;
    private double millisPerPixel = TimeMath.HOUR / 40.0;
    private double minMillisPerPixel = TimeMath.MINUTE / 2.0;
    private double maxMillisPerPixel = TimeMath.DAY;
    private int minorUnit = HOUR;
    private int minorCount = 1;
    private int majorUnit = DAY;
    private int tickPixels = -1;

    TimeAxis(ZoneId zone) {
        this.zone = zone;
    }

    void setZone(ZoneId z) {
        zone = z;
    }

    ZoneId getZone() {
        return zone;
    }

    void setFirstDayOfWeek(int isoDay) {
        firstDayOfWeek = isoDay;
    }

    void set24Hours(boolean v) {
        h24 = v;
    }

    void setRange(long from, long to) {
        start = from;
        end = Math.max(from + TimeMath.HOUR, to);
    }

    long getStart() {
        return start;
    }

    long getEnd() {
        return end;
    }

    double getMillisPerPixel() {
        return millisPerPixel;
    }

    /// Sets the scale, held between the limits.
    void setMillisPerPixel(double v) {
        millisPerPixel = Math.max(minMillisPerPixel, Math.min(maxMillisPerPixel, v));
        tickPixels = -1;
    }

    /// The limits of zooming: the finest and the coarsest scale.
    void setScaleLimits(double finest, double coarsest) {
        minMillisPerPixel = finest;
        maxMillisPerPixel = Math.max(finest, coarsest);
        setMillisPerPixel(millisPerPixel);
    }

    /// The width of the whole range in pixels.
    int width() {
        return (int) Math.ceil((end - start) / millisPerPixel);
    }

    /// The pixel an instant is at, from the start of the range.
    int x(long time) {
        return (int) Math.floor((time - start) / millisPerPixel);
    }

    long time(int x) {
        return start + (long) (x * millisPerPixel);
    }

    /// Picks the units of the ticks for the scale. `minTick` is the least
    /// width a minor tick needs for its label.
    void pickUnits(int minTick) {
        if (tickPixels == minTick) {
            return;
        }
        tickPixels = minTick;
        int pick = LADDER_UNIT.length - 1;
        for (int i = 0; i < LADDER_UNIT.length; i++) {
            if (LADDER_MILLIS[i] / millisPerPixel >= minTick) {
                pick = i;
                break;
            }
        }
        minorUnit = LADDER_UNIT[pick];
        minorCount = LADDER_COUNT[pick];
        if (minorUnit == HOUR) {
            majorUnit = DAY;
        } else if (minorUnit == DAY || minorUnit == WEEK) {
            majorUnit = MONTH;
        } else {
            majorUnit = YEAR;
        }
    }

    int getMinorUnit() {
        return minorUnit;
    }

    /// What a drag snaps to at this scale when the application's own step is
    /// finer than a pixel: the start of a day once ticks are days or more.
    boolean snapsToDays() {
        return minorUnit != HOUR;
    }

    /// The start of the tick of a unit that `t` is in.
    long floor(long t, int unit, int count) {
        long day = TimeMath.epochDay(t, zone);
        switch (unit) {
            case HOUR: {
                int step = count * 60;
                int m = TimeMath.minuteOfDay(t, zone) / step * step;
                return TimeMath.at(day, m, zone);
            }
            case DAY:
                return TimeMath.startOfEpochDay(day, zone);
            case WEEK:
                return TimeMath.startOfEpochDay(TimeMath.startOfWeek(day, firstDayOfWeek), zone);
            case MONTH:
                return TimeMath.startOfEpochDay(TimeMath.startOfMonth(day), zone);
            case QUARTER: {
                long ms = TimeMath.startOfMonth(day);
                int back = (Dates.month(ms) - 1) % 3;
                return TimeMath.startOfEpochDay(TimeMath.plusMonths(ms, -back), zone);
            }
            default: {
                long ms = TimeMath.startOfMonth(day);
                return TimeMath.startOfEpochDay(TimeMath.plusMonths(ms, 1 - Dates.month(ms)), zone);
            }
        }
    }

    /// The start of the tick after the one that starts at `tick`.
    long next(long tick, int unit, int count) {
        long day = TimeMath.epochDay(tick, zone);
        switch (unit) {
            case HOUR: {
                int m = TimeMath.minuteOfDay(tick, zone) + count * 60;
                long n = m >= TimeMath.MINUTES_PER_DAY ? TimeMath.startOfEpochDay(day + 1, zone)
                        : TimeMath.at(day, m, zone);
                // A day that loses an hour to daylight saving has a minute of
                // the day that maps back to where it started.
                return n > tick ? n : tick + count * TimeMath.HOUR;
            }
            case DAY:
                return TimeMath.startOfEpochDay(day + 1, zone);
            case WEEK:
                return TimeMath.startOfEpochDay(day + 7, zone);
            case MONTH:
                return TimeMath.startOfEpochDay(TimeMath.plusMonths(day, 1), zone);
            case QUARTER:
                return TimeMath.startOfEpochDay(TimeMath.plusMonths(day, 3), zone);
            default:
                return TimeMath.startOfEpochDay(TimeMath.plusMonths(day, 12), zone);
        }
    }

    String label(long tick, int unit, int width, Font f) {
        long day = TimeMath.epochDay(tick, zone);
        switch (unit) {
            case HOUR:
                return Dates.time(TimeMath.minuteOfDay(tick, zone), h24, true);
            case DAY: {
                String wide = Dates.weekdayShort(TimeMath.dayOfWeek(day)) + " " + Dates.dayOfMonth(day);
                return f.stringWidth(wide) + Look.mm(1.5f) <= width ? wide : String.valueOf(Dates.dayOfMonth(day));
            }
            case WEEK:
                return Dates.monthDay(day);
            case MONTH:
                return Dates.monthShort(Dates.month(day));
            case QUARTER:
                return "Q" + ((Dates.month(day) - 1) / 3 + 1);
            default:
                return String.valueOf(Dates.year(day));
        }
    }

    private String majorLabel(long tick) {
        long day = TimeMath.epochDay(tick, zone);
        if (majorUnit == DAY) {
            return Dates.dayTitle(day, TimeMath.dayOfWeek(day));
        }
        if (majorUnit == MONTH) {
            return Dates.monthTitle(day);
        }
        return String.valueOf(Dates.year(day));
    }

    /// The height of the two rows of labels.
    int headerHeight(Style header) {
        return 2 * (header.getFont().getHeight() + Look.mm(1.4f));
    }

    /// Paints the two rows of labels. `x0` is where the start of the range
    /// is on screen, and `left` to `right` the part of the screen to paint.
    void paintHeader(Graphics g, Style header, int x0, int y, int left, int right) {
        Font f = header.getFont();
        int rowH = f.getHeight() + Look.mm(1.4f);
        int pad = Look.mm(1);
        Look.fill(g, header, left, y, right - left, 2 * rowH);
        g.setFont(f);
        int line = Look.mix(header.getBgColor(), header.getFgColor(), 50);
        long from = time(left - x0);
        long to = time(right - x0);

        // The upper row. A label stays in view for as long as its tick does.
        long t = floor(from, majorUnit, 1);
        while (t < to) {
            long n = next(t, majorUnit, 1);
            int tx = x0 + x(t);
            int nx = x0 + x(n);
            String s = majorLabel(t);
            int lx = Math.max(tx, left) + pad;
            int room = nx - lx - pad;
            if (room > 0) {
                g.setColor(header.getFgColor());
                g.drawString(Look.fit(s, f, room), lx, y + (rowH - f.getHeight()) / 2);
            }
            if (tx >= left) {
                g.setColor(line);
                g.drawLine(tx, y, tx, y + rowH);
            }
            t = n;
        }

        t = floor(from, minorUnit, minorCount);
        int alpha = g.getAlpha();
        while (t < to) {
            long n = next(t, minorUnit, minorCount);
            int tx = x0 + x(t);
            int nx = x0 + x(n);
            String s = label(t, minorUnit, nx - tx, f);
            int sw = f.stringWidth(s);
            g.setColor(header.getFgColor());
            g.setAlpha(alpha * 190 / 255);
            if (minorUnit == HOUR) {
                g.drawString(s, tx + pad, y + rowH + (rowH - f.getHeight()) / 2);
            } else if (sw <= nx - tx) {
                g.drawString(s, tx + (nx - tx - sw) / 2, y + rowH + (rowH - f.getHeight()) / 2);
            }
            g.setAlpha(alpha);
            g.setColor(line);
            g.drawLine(tx, y + rowH, tx, y + 2 * rowH);
            t = n;
        }
        g.setColor(line);
        g.drawLine(left, y + 2 * rowH - 1, right, y + 2 * rowH - 1);
    }

    /// Paints a line down every tick: fainter for the minor ones.
    void paintGrid(Graphics g, int color, int background, int x0, int y, int height, int left, int right) {
        long from = time(left - x0);
        long to = time(right - x0);
        long t = floor(from, minorUnit, minorCount);
        int faint = Look.mix(background, color, 110);
        long major = floor(from, majorUnit, 1);
        while (t < to) {
            while (major < t) {
                major = next(major, majorUnit, 1);
            }
            g.setColor(major == t ? color : faint);
            int tx = x0 + x(t);
            g.drawLine(tx, y, tx, y + height);
            t = next(t, minorUnit, minorCount);
        }
    }

    /// Shades the time a calendar does not work: the hours while ticks are
    /// hours, whole days off while a day is still wide enough to see.
    void paintOffHours(Graphics g, WorkCalendar cal, Style off, int x0, int y, int height, int left, int right) {
        if (cal == null || (off.getBgTransparency() & 0xff) == 0) {
            return;
        }
        if (TimeMath.DAY / millisPerPixel < 3) {
            return;
        }
        ZoneId z = cal.getZone();
        long first = TimeMath.epochDay(time(left - x0), z);
        long last = TimeMath.epochDay(time(right - x0), z);
        for (long day = first; day <= last; day++) {
            int[] iv = cal.getWorkingHours(day);
            if (minorUnit != HOUR) {
                if (iv.length == 0) {
                    shade(g, off, x0, y, height, TimeMath.startOfEpochDay(day, z),
                            TimeMath.startOfEpochDay(day + 1, z));
                }
                continue;
            }
            int cursor = 0;
            for (int i = 0; i < iv.length; i += 2) {
                if (iv[i] > cursor) {
                    shade(g, off, x0, y, height, TimeMath.at(day, cursor, z), TimeMath.at(day, iv[i], z));
                }
                cursor = iv[i + 1];
            }
            if (cursor < TimeMath.MINUTES_PER_DAY) {
                shade(g, off, x0, y, height, TimeMath.at(day, cursor, z), TimeMath.startOfEpochDay(day + 1, z));
            }
        }
    }

    private void shade(Graphics g, Style off, int x0, int y, int height, long from, long to) {
        int a = x0 + x(from);
        int b = x0 + x(to);
        Look.fill(g, off, a, y, b - a, height);
    }
}
