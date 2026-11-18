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
package com.codename1.ui.chart.axis;

import com.codename1.l10n.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

/// A time axis whose values are milliseconds since the epoch. Ticks land on calendar
/// boundaries (whole minutes, midnights, first of the month, new year) and the label
/// format follows the tick unit, so zooming from years down to milliseconds stays readable.
public class TimeAxis extends Axis {
    private static final long SEC = 1000L;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    /// Candidate tick steps, smallest first, as `{approximate millis, calendar field, amount}`.
    private static final long[][] STEPS = {
        {1, Calendar.MILLISECOND, 1}, {2, Calendar.MILLISECOND, 2}, {5, Calendar.MILLISECOND, 5},
        {10, Calendar.MILLISECOND, 10}, {20, Calendar.MILLISECOND, 20}, {50, Calendar.MILLISECOND, 50},
        {100, Calendar.MILLISECOND, 100}, {200, Calendar.MILLISECOND, 200},
        {500, Calendar.MILLISECOND, 500},
        {SEC, Calendar.SECOND, 1}, {5 * SEC, Calendar.SECOND, 5}, {15 * SEC, Calendar.SECOND, 15},
        {30 * SEC, Calendar.SECOND, 30}, {MIN, Calendar.MINUTE, 1}, {5 * MIN, Calendar.MINUTE, 5},
        {15 * MIN, Calendar.MINUTE, 15}, {30 * MIN, Calendar.MINUTE, 30}, {HOUR, Calendar.HOUR_OF_DAY, 1},
        {3 * HOUR, Calendar.HOUR_OF_DAY, 3}, {6 * HOUR, Calendar.HOUR_OF_DAY, 6},
        {12 * HOUR, Calendar.HOUR_OF_DAY, 12}, {DAY, Calendar.DAY_OF_MONTH, 1},
        {2 * DAY, Calendar.DAY_OF_MONTH, 2}, {7 * DAY, Calendar.DAY_OF_MONTH, 7},
        {MONTH, Calendar.MONTH, 1}, {3 * MONTH, Calendar.MONTH, 3}, {6 * MONTH, Calendar.MONTH, 6},
        {YEAR, Calendar.YEAR, 1}, {2 * YEAR, Calendar.YEAR, 2}, {5 * YEAR, Calendar.YEAR, 5},
        {10 * YEAR, Calendar.YEAR, 10}, {25 * YEAR, Calendar.YEAR, 25}, {50 * YEAR, Calendar.YEAR, 50},
        {100 * YEAR, Calendar.YEAR, 100}
    };

    private int lastField = Calendar.DAY_OF_MONTH;

    /// Creates a time axis.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of `Axis.BOTTOM`, `Axis.LEFT`, `Axis.RIGHT` or `Axis.TOP`
    public TimeAxis(int position) {
        super(position);
        setGridVisible(true);
    }

    @Override
    protected double[] adjustBounds(double min, double max) {
        return new double[]{min, max};
    }

    @Override
    public double[] ticks(int maxCount) {
        int[] field = new int[1];
        double[] out = timeTicks(getVisibleMin(), getVisibleMax(), maxCount, field);
        lastField = field[0];
        return out;
    }

    /// Calendar aligned ticks between two instants.
    static double[] timeTicks(double min, double max, int maxCount, int[] fieldOut) {
        double span = max - min;
        fieldOut[0] = Calendar.DAY_OF_MONTH;
        if (!(span > 0)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            return new double[]{min};
        }
        int index = STEPS.length - 1;
        for (int i = 0; i < STEPS.length; i++) {
            if (span / STEPS[i][0] <= Math.max(1, maxCount - 1)) {
                index = i;
                break;
            }
        }
        double[] tmp = new double[Math.max(2, maxCount) + 3];
        // a step as wide as the span can leave one calendar boundary inside it, or none;
        // step down until two land inside, as Ticks.linear does, so an axis never has a
        // single label
        while (true) {
            long[] step = STEPS[index];
            int field = (int) step[1];
            int n = fill(min, max, field, (int) step[2], tmp);
            if (n >= 2 || index == 0) {
                fieldOut[0] = field;
                if (n < 2) {
                    return new double[]{min, max};
                }
                double[] out = new double[n];
                System.arraycopy(tmp, 0, out, 0, n);
                return out;
            }
            index--;
        }
    }

    private static int fill(double min, double max, int field, int amount, double[] tmp) {
        int n = 0;
        if (field == Calendar.MILLISECOND) {
            // sub-second steps divide a second evenly, so epoch multiples are on boundaries
            long t = (long) Math.ceil(min / amount) * amount;
            while (n < tmp.length && t <= max) {
                tmp[n++] = t;
                t += amount;
            }
            return n;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date((long) min));
        truncate(c, field, amount);
        int guard = 0;
        while (n < tmp.length && guard++ < 1000) {
            long t = c.getTime().getTime();
            if (t > max) {
                break;
            }
            if (t >= min) {
                tmp[n++] = t;
            }
            c.add(field, amount);
        }
        return n;
    }

    /// The date pattern for ticks spaced by `field`.
    static String patternFor(int field) {
        if (field == Calendar.MILLISECOND) {
            return "HH:mm:ss.SSS";
        }
        if (field == Calendar.SECOND) {
            return "HH:mm:ss";
        }
        if (field == Calendar.MINUTE || field == Calendar.HOUR_OF_DAY) {
            return "HH:mm";
        }
        if (field == Calendar.DAY_OF_MONTH) {
            return "MMM d";
        }
        if (field == Calendar.MONTH) {
            return "MMM yyyy";
        }
        return "yyyy";
    }

    private static void truncate(Calendar c, int field, int amount) {
        c.set(Calendar.MILLISECOND, 0);
        if (field == Calendar.SECOND) {
            c.set(Calendar.SECOND, c.get(Calendar.SECOND) / amount * amount);
            return;
        }
        c.set(Calendar.SECOND, 0);
        if (field == Calendar.MINUTE) {
            c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) / amount * amount);
            return;
        }
        c.set(Calendar.MINUTE, 0);
        if (field == Calendar.HOUR_OF_DAY) {
            c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) / amount * amount);
            return;
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        if (field == Calendar.DAY_OF_MONTH) {
            if (amount > 1 && amount < 7) {
                c.set(Calendar.DAY_OF_MONTH, (c.get(Calendar.DAY_OF_MONTH) - 1) / amount * amount + 1);
            }
            return;
        }
        c.set(Calendar.DAY_OF_MONTH, 1);
        if (field == Calendar.MONTH) {
            c.set(Calendar.MONTH, c.get(Calendar.MONTH) / amount * amount);
            return;
        }
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) / amount * amount);
    }

    private SimpleDateFormat formatter;
    private String formatterPattern;

    @Override
    protected String defaultFormat(double value, double step) {
        String pattern = step <= 0 ? "MMM d, yyyy HH:mm" : patternFor(lastField);
        if (formatter == null || !pattern.equals(formatterPattern)) {
            // one formatter per pattern, not one per label per frame
            formatter = new SimpleDateFormat(pattern);
            formatterPattern = pattern;
        }
        return formatter.format(new Date((long) value));
    }
}
