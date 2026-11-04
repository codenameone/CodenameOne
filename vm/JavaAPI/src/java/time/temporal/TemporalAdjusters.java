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
package java.time.temporal;

import java.time.DayOfWeek;
import java.time.LocalDate;

/// The common [TemporalAdjuster]s. They adjust a `LocalDate`; any other
/// temporal throws `UnsupportedTemporalTypeException`, because this class
/// library has no field-based `Temporal.with` for them to go through.
public final class TemporalAdjusters {

    private static final int FIRST_OF_MONTH = 0;
    private static final int LAST_OF_MONTH = 1;
    private static final int FIRST_OF_NEXT_MONTH = 2;
    private static final int FIRST_OF_YEAR = 3;
    private static final int LAST_OF_YEAR = 4;
    private static final int FIRST_OF_NEXT_YEAR = 5;

    private TemporalAdjusters() {
    }

    public static TemporalAdjuster firstDayOfMonth() {
        return new Fixed(FIRST_OF_MONTH);
    }

    public static TemporalAdjuster lastDayOfMonth() {
        return new Fixed(LAST_OF_MONTH);
    }

    public static TemporalAdjuster firstDayOfNextMonth() {
        return new Fixed(FIRST_OF_NEXT_MONTH);
    }

    public static TemporalAdjuster firstDayOfYear() {
        return new Fixed(FIRST_OF_YEAR);
    }

    public static TemporalAdjuster lastDayOfYear() {
        return new Fixed(LAST_OF_YEAR);
    }

    public static TemporalAdjuster firstDayOfNextYear() {
        return new Fixed(FIRST_OF_NEXT_YEAR);
    }

    /// The next `dayOfWeek` strictly after the date.
    public static TemporalAdjuster next(DayOfWeek dayOfWeek) {
        return new Relative(dayOfWeek, true, false);
    }

    /// The next `dayOfWeek`, or the date itself when it already is one.
    public static TemporalAdjuster nextOrSame(DayOfWeek dayOfWeek) {
        return new Relative(dayOfWeek, true, true);
    }

    /// The previous `dayOfWeek` strictly before the date.
    public static TemporalAdjuster previous(DayOfWeek dayOfWeek) {
        return new Relative(dayOfWeek, false, false);
    }

    /// The previous `dayOfWeek`, or the date itself when it already is one.
    public static TemporalAdjuster previousOrSame(DayOfWeek dayOfWeek) {
        return new Relative(dayOfWeek, false, true);
    }

    private static LocalDate date(Temporal temporal) {
        if (temporal instanceof LocalDate) {
            return (LocalDate) temporal;
        }
        throw new UnsupportedTemporalTypeException("Only LocalDate can be adjusted");
    }

    private static final class Fixed implements TemporalAdjuster {
        private final int kind;

        Fixed(int kind) {
            this.kind = kind;
        }

        @Override
        public Temporal adjustInto(Temporal temporal) {
            LocalDate d = date(temporal);
            switch (kind) {
                case FIRST_OF_MONTH:
                    return d.withDayOfMonth(1);
                case LAST_OF_MONTH:
                    return d.withDayOfMonth(d.lengthOfMonth());
                case FIRST_OF_NEXT_MONTH:
                    return d.withDayOfMonth(1).plusMonths(1);
                case FIRST_OF_YEAR:
                    return d.withDayOfYear(1);
                case LAST_OF_YEAR:
                    return d.withDayOfYear(d.lengthOfYear());
                default:
                    return d.withDayOfYear(1).plusYears(1);
            }
        }
    }

    private static final class Relative implements TemporalAdjuster {
        private final int dow;
        private final boolean forward;
        private final boolean orSame;

        Relative(DayOfWeek dayOfWeek, boolean forward, boolean orSame) {
            if (dayOfWeek == null) {
                throw new NullPointerException("dayOfWeek");
            }
            this.dow = dayOfWeek.getValue();
            this.forward = forward;
            this.orSame = orSame;
        }

        @Override
        public Temporal adjustInto(Temporal temporal) {
            LocalDate d = date(temporal);
            int cur = d.getDayOfWeek().getValue();
            if (orSame && cur == dow) {
                return d;
            }
            if (forward) {
                int diff = dow - cur;
                return d.plusDays(diff <= 0 ? diff + 7 : diff);
            }
            int diff = cur - dow;
            return d.minusDays(diff <= 0 ? diff + 7 : diff);
        }
    }
}
