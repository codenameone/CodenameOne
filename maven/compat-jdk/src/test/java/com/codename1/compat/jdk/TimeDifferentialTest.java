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
package com.codename1.compat.jdk;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The `java.time` members this module adds, each run beside the JDK's own
/// over a grid of dates, times, moments, zones and units. The values are
/// the JDK's classes on both sides; what differs is who does the arithmetic.
public class TimeDifferentialTest {
    private int compared;

    private static final LocalDate[] DATES = {LocalDate.of(1600, 2, 29), LocalDate.of(1899, 12, 31),
        LocalDate.of(1969, 12, 31), LocalDate.of(1970, 1, 1), LocalDate.of(1999, 12, 31), LocalDate.of(2000, 2, 29),
        LocalDate.of(2000, 3, 1), LocalDate.of(2023, 1, 31), LocalDate.of(2023, 2, 28), LocalDate.of(2023, 12, 31),
        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31), LocalDate.of(2024, 5, 15),
        LocalDate.of(2024, 5, 16), LocalDate.of(2024, 6, 15), LocalDate.of(2025, 2, 28), LocalDate.of(2026, 10, 9),
        LocalDate.of(2100, 2, 28), LocalDate.of(3000, 7, 4)};

    private static final LocalTime[] TIMES = {LocalTime.of(0, 0), LocalTime.of(0, 0, 0, 1),
        LocalTime.of(12, 34, 56, 789000000), LocalTime.of(23, 59, 59, 999999999)};

    private static final long[] AMOUNTS = {-400, -25, -1, 0, 1, 7, 13, 1000};

    private static final ZoneId[] ZONES = {ZoneId.of("UTC"), ZoneId.of("America/New_York"),
        ZoneId.of("Asia/Kolkata")};

    private static List<LocalDateTime> dateTimes() {
        List<LocalDateTime> out = new ArrayList<LocalDateTime>();
        for (int d = 0; d < DATES.length; d += 2) {
            for (LocalTime t : TIMES) {
                out.add(LocalDateTime.of(DATES[d], t));
            }
        }
        out.add(LocalDateTime.of(2024, 3, 10, 1, 30));
        out.add(LocalDateTime.of(2024, 3, 10, 3, 30));
        out.add(LocalDateTime.of(2024, 11, 3, 1, 30));
        return out;
    }

    private static List<Instant> instants() {
        List<Instant> out = new ArrayList<Instant>();
        for (LocalDateTime dt : dateTimes()) {
            out.add(dt.toInstant(ZoneOffset.UTC));
        }
        return out;
    }

    private static ChronoUnit unit(java.time.temporal.ChronoUnit u) {
        return ChronoUnit.valueOf(u.name());
    }

    private static String outcome(Callable<Object> call) {
        try {
            Object o = call.call();
            if (o instanceof Enum) {
                return ((Enum<?>) o).name();
            }
            return String.valueOf(o);
        } catch (DateTimeException e) {
            // The JDK's is a subclass that says the unit is unsupported.
            return "DateTimeException";
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private void same(String what, Callable<Object> jdk, Callable<Object> shim) {
        assertEquals(what, outcome(jdk), outcome(shim));
        compared++;
    }

    @Test
    public void datesAreTheJdks() {
        for (final LocalDate d : DATES) {
            same("dow " + d, () -> d.getDayOfWeek(), () -> JdkDates.getDayOfWeek(d));
            same("month " + d, () -> d.getMonth(), () -> JdkDates.getMonth(d));
            same("doy " + d, () -> d.getDayOfYear(), () -> JdkDates.getDayOfYear(d));
            same("loy " + d, () -> d.lengthOfYear(), () -> JdkDates.lengthOfYear(d));
            same("start " + d, () -> d.atStartOfDay(), () -> JdkDates.atStartOfDay(d));
            for (final int n : new int[] {0, 1, 2, 12, 13, 28, 29, 30, 31, 32, 60, 365, 366, 367}) {
                same("wdom " + d + n, () -> d.withDayOfMonth(n), () -> JdkDates.withDayOfMonth(d, n));
                same("wm " + d + n, () -> d.withMonth(n), () -> JdkDates.withMonth(d, n));
                same("wdoy " + d + n, () -> d.withDayOfYear(n), () -> JdkDates.withDayOfYear(d, n));
                same("wy " + d + n, () -> d.withYear(2000 + n), () -> JdkDates.withYear(d, 2000 + n));
            }
            for (final java.time.temporal.ChronoUnit u : java.time.temporal.ChronoUnit.values()) {
                for (final long n : AMOUNTS) {
                    if (u.compareTo(java.time.temporal.ChronoUnit.CENTURIES) >= 0 && (n > 100 || n < -100)) {
                        continue;
                    }
                    same("plus " + d + n + u, () -> d.plus(n, u), () -> JdkDates.plus(d, n, unit(u)));
                    same("minus " + d + n + u, () -> d.minus(n, u), () -> JdkDates.minus(d, n, unit(u)));
                }
                for (final LocalDate e : DATES) {
                    same("until " + d + e + u, () -> d.until(e, u), () -> JdkDates.until(d, e, unit(u)));
                    same("between " + d + e + u, () -> u.between(d, e), () -> unit(u).between(d, e));
                }
            }
            for (final Period p : new Period[] {Period.of(1, 2, 3), Period.of(0, 13, 31), Period.of(-1, -1, -1),
                Period.ofDays(400)}) {
                same("plus " + d + p, () -> d.plus(p), () -> JdkDates.plusAmount(d, p));
                same("minus " + d + p, () -> d.minus(p), () -> JdkDates.minusAmount(d, p));
            }
            same("plus duration " + d, () -> d.plus(Duration.ofDays(1)),
                    () -> JdkDates.plusAmount(d, Duration.ofDays(1)));
        }
        assertTrue("" + compared, compared > 15000);
    }

    @Test
    public void datesWithTimesAreTheJdks() {
        final List<LocalDateTime> all = dateTimes();
        for (final LocalDateTime d : all) {
            same("dow " + d, () -> d.getDayOfWeek(), () -> JdkDates.getDayOfWeek(d));
            same("month " + d, () -> d.getMonth(), () -> JdkDates.getMonth(d));
            same("doy " + d, () -> d.getDayOfYear(), () -> JdkDates.getDayOfYear(d));
            same("epoch " + d, () -> d.toEpochSecond(ZoneOffset.ofHours(2)),
                    () -> JdkDates.toEpochSecond(d, ZoneOffset.ofHours(2)));
            for (final long n : new long[] {-100000, -25, -1, 0, 1, 7, 13, 59, 100000, 86400000000001L}) {
                same("md " + d + n, () -> d.minusDays(n % 100000), () -> JdkDates.minusDays(d, n % 100000));
                same("mh " + d + n, () -> d.minusHours(n), () -> JdkDates.minusHours(d, n));
                same("mm " + d + n, () -> d.minusMinutes(n), () -> JdkDates.minusMinutes(d, n));
                same("ms " + d + n, () -> d.minusSeconds(n), () -> JdkDates.minusSeconds(d, n));
                same("pn " + d + n, () -> d.plusNanos(n), () -> JdkDates.plusNanos(d, n));
                same("mn " + d + n, () -> d.minusNanos(n), () -> JdkDates.minusNanos(d, n));
                final long few = n % 1000;
                same("pw " + d + n, () -> d.plusWeeks(few), () -> JdkDates.plusWeeks(d, few));
                same("mw " + d + n, () -> d.minusWeeks(few), () -> JdkDates.minusWeeks(d, few));
                same("pmo " + d + n, () -> d.plusMonths(few), () -> JdkDates.plusMonths(d, few));
                same("mmo " + d + n, () -> d.minusMonths(few), () -> JdkDates.minusMonths(d, few));
                same("py " + d + n, () -> d.plusYears(few), () -> JdkDates.plusYears(d, few));
                same("my " + d + n, () -> d.minusYears(few), () -> JdkDates.minusYears(d, few));
            }
            for (final int n : new int[] {0, 1, 23, 24, 59, 60, 999999999}) {
                same("wh " + d + n, () -> d.withHour(n), () -> JdkDates.withHour(d, n));
                same("wmi " + d + n, () -> d.withMinute(n), () -> JdkDates.withMinute(d, n));
                same("ws " + d + n, () -> d.withSecond(n), () -> JdkDates.withSecond(d, n));
                same("wn " + d + n, () -> d.withNano(n), () -> JdkDates.withNano(d, n));
            }
            for (final Object amount : new Object[] {Period.of(1, 2, 3), Period.ofMonths(-13),
                Duration.ofSeconds(86399, 999999999), Duration.ofMillis(-1), Duration.ofDays(400)}) {
                same("plus " + d + amount, () -> d.plus((java.time.temporal.TemporalAmount) amount),
                        () -> JdkDates.plusAmount(d, amount));
                same("minus " + d + amount, () -> d.minus((java.time.temporal.TemporalAmount) amount),
                        () -> JdkDates.minusAmount(d, amount));
            }
            for (final java.time.temporal.ChronoUnit u : java.time.temporal.ChronoUnit.values()) {
                same("trunc " + d + u, () -> d.truncatedTo(u), () -> JdkDates.truncatedTo(d, unit(u)));
                for (final long n : AMOUNTS) {
                    if (u.compareTo(java.time.temporal.ChronoUnit.CENTURIES) >= 0 && (n > 100 || n < -100)) {
                        continue;
                    }
                    // The JDK counts 256 half days to a day here, so beyond
                    // that many its answer is not the sum; not followed.
                    if (u == java.time.temporal.ChronoUnit.HALF_DAYS && (n >= 256 || n <= -256)) {
                        continue;
                    }
                    same("plus " + d + n + u, () -> d.plus(n, u), () -> JdkDates.plus(d, n, unit(u)));
                    same("minus " + d + n + u, () -> d.minus(n, u), () -> JdkDates.minus(d, n, unit(u)));
                }
                for (final LocalDateTime e : all) {
                    same("until " + d + e + u, () -> d.until(e, u), () -> JdkDates.until(d, e, unit(u)));
                }
            }
            for (final LocalDateTime e : all) {
                same("before " + d + e, () -> d.isBefore(e), () -> JdkDates.isBefore(d, e));
                same("after " + d + e, () -> d.isAfter(e), () -> JdkDates.isAfter(d, e));
                same("equal " + d + e, () -> d.isEqual(e), () -> JdkDates.isEqual(d, e));
                same("duration " + d + e, () -> Duration.between(d, e), () -> JdkDates.between(d, e));
                same("time " + d + e, () -> Duration.between(d.toLocalTime(), e.toLocalTime()),
                        () -> JdkDates.between(d.toLocalTime(), e.toLocalTime()));
                same("time until " + d + e,
                        () -> d.toLocalTime().until(e.toLocalTime(), java.time.temporal.ChronoUnit.MINUTES),
                        () -> JdkDates.until(d.toLocalTime(), e.toLocalTime(), ChronoUnit.MINUTES));
            }
        }
        assertTrue("" + compared, compared > 50000);
    }

    @Test
    public void momentsAndZonesAreTheJdks() {
        final List<Instant> all = instants();
        for (final Instant i : all) {
            for (final long n : new long[] {-86400000000001L, -1, 0, 1, 999999999, 1000000000, 123456789012L}) {
                same("pn " + i + n, () -> i.plusNanos(n), () -> JdkDates.plusNanos(i, n));
                same("mn " + i + n, () -> i.minusNanos(n), () -> JdkDates.minusNanos(i, n));
                final Duration d = Duration.ofSeconds(n % 100000, n);
                same("pd " + i + n, () -> i.plus(d), () -> JdkDates.plusAmount(i, d));
                same("md " + i + n, () -> i.minus(d), () -> JdkDates.minusAmount(i, d));
            }
            same("period " + i, () -> i.plus(Period.ofDays(1)), () -> JdkDates.plusAmount(i, Period.ofDays(1)));
            for (final java.time.temporal.ChronoUnit u : java.time.temporal.ChronoUnit.values()) {
                for (final long n : AMOUNTS) {
                    same("plus " + i + n + u, () -> i.plus(n, u), () -> JdkDates.plus(i, n, unit(u)));
                    same("minus " + i + n + u, () -> i.minus(n, u), () -> JdkDates.minus(i, n, unit(u)));
                }
                for (final Instant e : all) {
                    same("until " + i + e + u, () -> i.until(e, u), () -> JdkDates.until(i, e, unit(u)));
                }
            }
            for (final Instant e : all) {
                same("before " + i + e, () -> i.isBefore(e), () -> JdkDates.isBefore(i, e));
                same("after " + i + e, () -> i.isAfter(e), () -> JdkDates.isAfter(i, e));
                same("duration " + i + e, () -> Duration.between(i, e), () -> JdkDates.between(i, e));
            }
            final Date date = new Date(i.toEpochMilli());
            same("toInstant " + i, () -> date.toInstant(), () -> JdkDates.toInstant(date));
            same("from " + i, () -> Date.from(i), () -> JdkDates.from(i));
            for (final ZoneId zone : ZONES) {
                same("atZone " + i + zone, () -> i.atZone(zone), () -> JdkDates.atZone(i, zone));
                final ZonedDateTime z = i.atZone(zone);
                same("date " + z, () -> z.toLocalDate(), () -> JdkDates.toLocalDate(z));
                same("time " + z, () -> z.toLocalTime(), () -> JdkDates.toLocalTime(z));
                same("epoch " + z, () -> z.toEpochSecond(), () -> JdkDates.toEpochSecond(z));
                same("fields " + z,
                        () -> z.getYear() + "-" + z.getMonthValue() + "-" + z.getDayOfMonth() + " " + z.getHour()
                        + ":" + z.getMinute() + ":" + z.getSecond() + "." + z.getNano() + " " + z.getDayOfYear()
                        + z.getMonth().name() + z.getDayOfWeek().name(),
                        () -> JdkDates.getYear(z) + "-" + JdkDates.getMonthValue(z) + "-" + JdkDates.getDayOfMonth(z)
                        + " " + JdkDates.getHour(z) + ":" + JdkDates.getMinute(z) + ":" + JdkDates.getSecond(z) + "."
                        + JdkDates.getNano(z) + " " + JdkDates.getDayOfYear(z) + JdkDates.getMonth(z).name()
                        + JdkDates.getDayOfWeek(z).name());
                same("same instant " + z, () -> z.withZoneSameInstant(ZONES[2]),
                        () -> JdkDates.withZoneSameInstant(z, ZONES[2]));
                same("local at " + z, () -> z.toLocalDateTime().atZone(ZONES[1]),
                        () -> JdkDates.atZone(z.toLocalDateTime(), ZONES[1]));
                for (final long n : new long[] {-400, -1, 0, 1, 25, 400}) {
                    same("pd " + z + n, () -> z.plusDays(n), () -> JdkDates.plusDays(z, n));
                    same("md " + z + n, () -> z.minusDays(n), () -> JdkDates.minusDays(z, n));
                    same("pw " + z + n, () -> z.plusWeeks(n), () -> JdkDates.plusWeeks(z, n));
                    same("mw " + z + n, () -> z.minusWeeks(n), () -> JdkDates.minusWeeks(z, n));
                    same("pmo " + z + n, () -> z.plusMonths(n), () -> JdkDates.plusMonths(z, n));
                    same("mmo " + z + n, () -> z.minusMonths(n), () -> JdkDates.minusMonths(z, n));
                    same("py " + z + n, () -> z.plusYears(n), () -> JdkDates.plusYears(z, n));
                    same("my " + z + n, () -> z.minusYears(n), () -> JdkDates.minusYears(z, n));
                    same("ph " + z + n, () -> z.plusHours(n), () -> JdkDates.plusHours(z, n));
                    same("mh " + z + n, () -> z.minusHours(n), () -> JdkDates.minusHours(z, n));
                    same("pmi " + z + n, () -> z.plusMinutes(n), () -> JdkDates.plusMinutes(z, n));
                    same("mmi " + z + n, () -> z.minusMinutes(n), () -> JdkDates.minusMinutes(z, n));
                    same("ps " + z + n, () -> z.plusSeconds(n), () -> JdkDates.plusSeconds(z, n));
                    same("ms " + z + n, () -> z.minusSeconds(n), () -> JdkDates.minusSeconds(z, n));
                }
                for (int other = 0; other < all.size(); other += 3) {
                    final ZonedDateTime e = all.get(other).atZone(ZONES[1]);
                    same("before " + z + e, () -> z.isBefore(e), () -> JdkDates.isBefore(z, e));
                    same("after " + z + e, () -> z.isAfter(e), () -> JdkDates.isAfter(z, e));
                    same("equal " + z + e, () -> z.isEqual(e), () -> JdkDates.isEqual(z, e));
                    same("duration " + z + e, () -> Duration.between(z, e), () -> JdkDates.between(z, e));
                    for (final java.time.temporal.ChronoUnit u : java.time.temporal.ChronoUnit.values()) {
                        same("until " + z + e + u, () -> z.until(e, u), () -> JdkDates.until(z, e, unit(u)));
                    }
                }
            }
        }
        assertTrue("" + compared, compared > 60000);
    }

    @Test
    public void durationsAreTheJdks() {
        final Duration[] all = {Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(-1), Duration.ofMillis(1500),
            Duration.ofMillis(-1500), Duration.ofSeconds(59), Duration.ofSeconds(3600), Duration.ofSeconds(86399, 5),
            Duration.ofSeconds(-86401, 999999999), Duration.ofDays(400), Duration.ofDays(-400),
            Duration.ofSeconds(90061, 123456789)};
        same("zero", () -> Duration.ZERO, () -> JdkDates.ZERO);
        for (final Duration d : all) {
            same("days " + d, () -> d.toDays(), () -> JdkDates.toDays(d));
            same("hours " + d, () -> d.toHours(), () -> JdkDates.toHours(d));
            same("minutes " + d, () -> d.toMinutes(), () -> JdkDates.toMinutes(d));
            same("seconds " + d, () -> d.getSeconds(), () -> JdkDates.toSeconds(d));
            same("nanos " + d, () -> d.toNanos(), () -> JdkDates.toNanos(d));
            same("zero " + d, () -> d.isZero(), () -> JdkDates.isZero(d));
            same("negative " + d, () -> d.isNegative(), () -> JdkDates.isNegative(d));
            same("negated " + d, () -> d.negated(), () -> JdkDates.negated(d));
            same("abs " + d, () -> d.abs(), () -> JdkDates.abs(d));
            // The parts came with Java 9; the JDK this runs on has their
            // definitions to compute them from, and the JDK 17 run in
            // build-engine has the methods.
            same("hours part " + d, () -> (int) (d.toHours() % 24), () -> JdkDates.toHoursPart(d));
            same("minutes part " + d, () -> (int) (d.toMinutes() % 60), () -> JdkDates.toMinutesPart(d));
            same("seconds part " + d, () -> (int) (d.getSeconds() % 60), () -> JdkDates.toSecondsPart(d));
            same("millis part " + d, () -> d.getNano() / 1000000, () -> JdkDates.toMillisPart(d));
            for (final long n : new long[] {-100000, -7, -1, 0, 1, 3, 1000, 123456789}) {
                same("pd " + d + n, () -> d.plusDays(n), () -> JdkDates.plusDays(d, n));
                same("ph " + d + n, () -> d.plusHours(n), () -> JdkDates.plusHours(d, n));
                same("pmi " + d + n, () -> d.plusMinutes(n), () -> JdkDates.plusMinutes(d, n));
                same("ps " + d + n, () -> d.plusSeconds(n), () -> JdkDates.plusSeconds(d, n));
                same("pms " + d + n, () -> d.plusMillis(n), () -> JdkDates.plusMillis(d, n));
                same("pn " + d + n, () -> d.plusNanos(n), () -> JdkDates.plusNanos(d, n));
                same("md " + d + n, () -> d.minusDays(n), () -> JdkDates.minusDays(d, n));
                same("mh " + d + n, () -> d.minusHours(n), () -> JdkDates.minusHours(d, n));
                same("mmi " + d + n, () -> d.minusMinutes(n), () -> JdkDates.minusMinutes(d, n));
                same("ms " + d + n, () -> d.minusSeconds(n), () -> JdkDates.minusSeconds(d, n));
                same("mms " + d + n, () -> d.minusMillis(n), () -> JdkDates.minusMillis(d, n));
                same("mn " + d + n, () -> d.minusNanos(n), () -> JdkDates.minusNanos(d, n));
                same("times " + d + n, () -> d.multipliedBy(n % 1000), () -> JdkDates.multipliedBy(d, n % 1000));
                same("over " + d + n, () -> d.dividedBy(n), () -> JdkDates.dividedBy(d, n));
                same("ofNanos " + n, () -> Duration.ofNanos(n * 1000003), () -> JdkDates.ofNanos(n * 1000003));
            }
        }
        for (final java.time.temporal.ChronoUnit u : java.time.temporal.ChronoUnit.values()) {
            same("name " + u, () -> u.toString(), () -> unit(u).toString());
            same("length " + u, () -> u.getDuration(), () -> unit(u).getDuration());
            same("estimated " + u, () -> u.isDurationEstimated(), () -> unit(u).isDurationEstimated());
            same("date " + u, () -> u.isDateBased(), () -> unit(u).isDateBased());
            same("time " + u, () -> u.isTimeBased(), () -> unit(u).isTimeBased());
            for (final long n : AMOUNTS) {
                same("of " + n + u, () -> Duration.of(n, u), () -> JdkDates.of(n, unit(u)));
            }
        }
        assertEquals(java.time.temporal.ChronoUnit.values().length, ChronoUnit.values().length);
        assertTrue("" + compared, compared > 1500);
    }

    @Test
    public void daysAndMonthsAreTheJdks() {
        for (final java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            final DayOfWeek mine = DayOfWeek.valueOf(day.name());
            same("value " + day, () -> day.getValue(), () -> mine.getValue());
            same("text " + day, () -> day.toString(), () -> mine.toString());
            for (final long n : new long[] {-15, -7, -1, 0, 1, 6, 7, 100, Long.MIN_VALUE, Long.MAX_VALUE}) {
                same("plus " + day + n, () -> day.plus(n), () -> mine.plus(n));
                same("minus " + day + n, () -> day.minus(n), () -> mine.minus(n));
            }
        }
        for (final java.time.Month month : java.time.Month.values()) {
            final Month mine = Month.valueOf(month.name());
            same("value " + month, () -> month.getValue(), () -> mine.getValue());
            same("min " + month, () -> month.minLength(), () -> mine.minLength());
            same("max " + month, () -> month.maxLength(), () -> mine.maxLength());
            same("quarter " + month, () -> month.firstMonthOfQuarter(), () -> mine.firstMonthOfQuarter());
            for (final boolean leap : new boolean[] {false, true}) {
                same("length " + month, () -> month.length(leap), () -> mine.length(leap));
                same("first " + month, () -> month.firstDayOfYear(leap), () -> mine.firstDayOfYear(leap));
            }
            for (final long n : new long[] {-25, -12, -1, 0, 1, 11, 12, 100, Long.MIN_VALUE, Long.MAX_VALUE}) {
                same("plus " + month + n, () -> month.plus(n), () -> mine.plus(n));
                same("minus " + month + n, () -> month.minus(n), () -> mine.minus(n));
            }
        }
        for (final int n : new int[] {-1, 0, 1, 7, 8, 12, 13}) {
            same("day of " + n, () -> java.time.DayOfWeek.of(n), () -> DayOfWeek.of(n));
            same("month of " + n, () -> java.time.Month.of(n), () -> Month.of(n));
            same("date of " + n, () -> LocalDate.of(2024, java.time.Month.of(n), 5),
                    () -> JdkDates.localDateOf(2024, Month.of(n), 5));
        }
        assertTrue("" + compared, compared > 500);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void theFieldsOfADateAreTheJdks() {
        final long[] moments = {0L, 1L, -1L, 86399999L, 951782400000L, 1709164800000L, 1791590400123L,
            4102444799999L, -2208988800000L};
        for (final long a : moments) {
            final Date d = new Date(a);
            same("year " + a, () -> d.getYear(), () -> JdkDates.getYear(d));
            same("month " + a, () -> d.getMonth(), () -> JdkDates.getMonth(d));
            same("date " + a, () -> d.getDate(), () -> JdkDates.getDate(d));
            same("day " + a, () -> d.getDay(), () -> JdkDates.getDay(d));
            same("hours " + a, () -> d.getHours(), () -> JdkDates.getHours(d));
            same("minutes " + a, () -> d.getMinutes(), () -> JdkDates.getMinutes(d));
            same("seconds " + a, () -> d.getSeconds(), () -> JdkDates.getSeconds(d));
            for (final long b : moments) {
                same("after " + a + b, () -> d.after(new Date(b)), () -> JdkDates.after(d, new Date(b)));
                same("before " + a + b, () -> d.before(new Date(b)), () -> JdkDates.before(d, new Date(b)));
            }
        }
        assertTrue("" + compared, compared > 200);
    }
}
