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
package com.codename1.jdkparity;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JavaAPI's {@code java.time}, renamed to {@code jdkparity.java.time}, run beside
 * the JDK's over a grid of dates, times, moments, zones and units. Every
 * answer is compared as text: the value's {@code toString}, an enum's name, or
 * the simple name of what was thrown ({@code DateTimeException} for any of
 * its subclasses, as the JDK throws narrower ones in places).
 *
 * <p>Zoned values in a region are compared only from 1900 on. JavaAPI takes a
 * region's offsets from {@code java.util.TimeZone}, which on the JDK has no
 * rules before 1900 and answers the 1900 offset for anything earlier, where
 * {@code java.time} answers local mean time; that is a difference in the
 * host's data, not in the arithmetic under test.</p>
 */
class TimeParityTest {
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

    private static final String[] ZONES = {"UTC", "America/New_York", "Asia/Kolkata"};

    /// The first moment a region's offsets are compared at; see the class
    /// comment.
    private static final long REGION_RULES_FROM = LocalDate.of(1900, 1, 2).toEpochDay() * 86400L;

    // ---- the JavaAPI value standing for each JDK one ----

    private static jdkparity.java.time.LocalDate ld(LocalDate d) {
        return jdkparity.java.time.LocalDate.of(d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    private static jdkparity.java.time.LocalTime lt(LocalTime t) {
        return jdkparity.java.time.LocalTime.of(t.getHour(), t.getMinute(), t.getSecond(), t.getNano());
    }

    private static jdkparity.java.time.LocalDateTime ldt(LocalDateTime d) {
        return jdkparity.java.time.LocalDateTime.of(ld(d.toLocalDate()), lt(d.toLocalTime()));
    }

    private static jdkparity.java.time.Instant in(Instant i) {
        return jdkparity.java.time.Instant.ofEpochSecond(i.getEpochSecond(), i.getNano());
    }

    private static jdkparity.java.time.ZoneId zone(ZoneId z) {
        return jdkparity.java.time.ZoneId.of(z.getId());
    }

    private static jdkparity.java.time.ZoneOffset offset(ZoneOffset o) {
        return jdkparity.java.time.ZoneOffset.ofTotalSeconds(o.getTotalSeconds());
    }

    private static jdkparity.java.time.ZonedDateTime zdt(ZonedDateTime z) {
        return jdkparity.java.time.ZonedDateTime.ofInstant(in(z.toInstant()), zone(z.getZone()));
    }

    private static jdkparity.java.time.Period period(Period p) {
        return jdkparity.java.time.Period.of(p.getYears(), p.getMonths(), p.getDays());
    }

    private static jdkparity.java.time.Duration duration(Duration d) {
        return jdkparity.java.time.Duration.ofSeconds(d.getSeconds(), d.getNano());
    }

    private static jdkparity.java.time.temporal.TemporalAmount amount(Object a) {
        return a instanceof Period ? period((Period) a) : duration((Duration) a);
    }

    private static jdkparity.java.time.temporal.ChronoUnit unit(ChronoUnit u) {
        return jdkparity.java.time.temporal.ChronoUnit.valueOf(u.name());
    }

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

    /// What `call` answers, or `UNKNOWN_RULES` when it answers a moment in a
    /// region before that region's rules are known; see the class comment.
    private static String outcome(Callable<Object> call) {
        try {
            Object o = call.call();
            if (o instanceof Enum) {
                return ((Enum<?>) o).name();
            }
            if (JAVA_8 && (o instanceof Duration || o instanceof jdkparity.java.time.Duration)) {
                // Java 8 prints a negative duration with a fraction wrongly
                // (PT-24H0.000000001S for a nanosecond under a day); compare
                // the value itself there.
                return o instanceof Duration ? ((Duration) o).getSeconds() + "s" + ((Duration) o).getNano()
                        : ((jdkparity.java.time.Duration) o).getSeconds() + "s"
                        + ((jdkparity.java.time.Duration) o).getNano();
            }
            if (o instanceof ZonedDateTime) {
                ZonedDateTime z = (ZonedDateTime) o;
                if (!regionRulesKnown(z.toInstant(), z.getZone())) {
                    return UNKNOWN_RULES;
                }
            }
            return String.valueOf(o);
        } catch (DateTimeException | jdkparity.java.time.DateTimeException e) {
            return "DateTimeException";
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private static final String UNKNOWN_RULES = "(before the region's rules)";

    /// The JDK running the test is Java 8, whose java.time has defects later
    /// releases fixed; JavaAPI answers as the later releases do, and the
    /// comparisons those defects reach are made only on them.
    private static final boolean JAVA_8 = System.getProperty("java.specification.version").startsWith("1.");

    /// ISO_INSTANT reads an offset other than Z from Java 12 on, as JavaAPI's
    /// does.
    private static final boolean INSTANT_READS_OFFSETS = !JAVA_8
            && Integer.parseInt(System.getProperty("java.specification.version")) >= 12;

    private void same(String what, Callable<Object> jdk, Callable<Object> api) {
        String expected = outcome(jdk);
        if (expected == UNKNOWN_RULES) {
            return;
        }
        assertEquals(expected, outcome(api), what);
        compared++;
    }

    private static boolean regionRulesKnown(Instant i, ZoneId zone) {
        return zone instanceof ZoneOffset || "UTC".equals(zone.getId()) || i.getEpochSecond() >= REGION_RULES_FROM;
    }

    @Test
    void datesAreTheJdks() {
        for (final LocalDate d : DATES) {
            final jdkparity.java.time.LocalDate m = ld(d);
            same("text " + d, () -> d, () -> m);
            same("dow " + d, d::getDayOfWeek, m::getDayOfWeek);
            same("month " + d, d::getMonth, m::getMonth);
            same("doy " + d, d::getDayOfYear, m::getDayOfYear);
            same("loy " + d, d::lengthOfYear, m::lengthOfYear);
            same("lom " + d, d::lengthOfMonth, m::lengthOfMonth);
            same("leap " + d, d::isLeapYear, m::isLeapYear);
            same("epoch day " + d, d::toEpochDay, m::toEpochDay);
            same("hash " + d, d::hashCode, m::hashCode);
            same("start " + d, d::atStartOfDay, m::atStartOfDay);
            for (final int n : new int[] {0, 1, 2, 12, 13, 28, 29, 30, 31, 32, 60, 365, 366, 367}) {
                same("wdom " + d + n, () -> d.withDayOfMonth(n), () -> m.withDayOfMonth(n));
                same("wm " + d + n, () -> d.withMonth(n), () -> m.withMonth(n));
                same("wdoy " + d + n, () -> d.withDayOfYear(n), () -> m.withDayOfYear(n));
                same("wy " + d + n, () -> d.withYear(2000 + n), () -> m.withYear(2000 + n));
            }
            for (final ChronoUnit u : ChronoUnit.values()) {
                for (final long n : AMOUNTS) {
                    if (u.compareTo(ChronoUnit.CENTURIES) >= 0 && (n > 100 || n < -100)) {
                        continue;
                    }
                    same("plus " + d + n + u, () -> d.plus(n, u), () -> m.plus(n, unit(u)));
                    same("minus " + d + n + u, () -> d.minus(n, u), () -> m.minus(n, unit(u)));
                }
                for (final LocalDate e : DATES) {
                    same("until " + d + e + u, () -> d.until(e, u), () -> m.until(ld(e), unit(u)));
                    same("between " + d + e + u, () -> u.between(d, e), () -> unit(u).between(m, ld(e)));
                }
            }
            for (final LocalDate e : DATES) {
                same("period " + d + e, () -> d.until(e), () -> m.until(ld(e)));
                same("compare " + d + e, () -> d.compareTo(e), () -> m.compareTo(ld(e)));
                same("before " + d + e, () -> d.isBefore(e), () -> m.isBefore(ld(e)));
                same("after " + d + e, () -> d.isAfter(e), () -> m.isAfter(ld(e)));
            }
            for (final Period p : new Period[] {Period.of(1, 2, 3), Period.of(0, 13, 31), Period.of(-1, -1, -1),
                Period.ofDays(400)}) {
                same("plus " + d + p, () -> d.plus(p), () -> m.plus(period(p)));
                same("minus " + d + p, () -> d.minus(p), () -> m.minus(period(p)));
            }
            same("plus duration " + d, () -> d.plus(Duration.ofDays(1)),
                    () -> m.plus(jdkparity.java.time.Duration.ofDays(1)));
        }
        assertTrue(compared > 15000, "" + compared);
    }

    @Test
    void datesWithTimesAreTheJdks() {
        final List<LocalDateTime> all = dateTimes();
        for (final LocalDateTime d : all) {
            final jdkparity.java.time.LocalDateTime m = ldt(d);
            same("text " + d, () -> d, () -> m);
            same("time text " + d, d::toLocalTime, m::toLocalTime);
            same("hash " + d, d::hashCode, m::hashCode);
            same("dow " + d, d::getDayOfWeek, m::getDayOfWeek);
            same("month " + d, d::getMonth, m::getMonth);
            same("doy " + d, d::getDayOfYear, m::getDayOfYear);
            same("epoch " + d, () -> d.toEpochSecond(ZoneOffset.ofHours(2)),
                    () -> m.toEpochSecond(jdkparity.java.time.ZoneOffset.ofHours(2)));
            for (final long n : new long[] {-100000, -25, -1, 0, 1, 7, 13, 59, 100000, 86400000000001L}) {
                same("md " + d + n, () -> d.minusDays(n % 100000), () -> m.minusDays(n % 100000));
                same("mh " + d + n, () -> d.minusHours(n), () -> m.minusHours(n));
                same("mm " + d + n, () -> d.minusMinutes(n), () -> m.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n), () -> m.minusSeconds(n));
                same("pn " + d + n, () -> d.plusNanos(n), () -> m.plusNanos(n));
                same("mn " + d + n, () -> d.minusNanos(n), () -> m.minusNanos(n));
                same("tpn " + d + n, () -> d.toLocalTime().plusNanos(n), () -> m.toLocalTime().plusNanos(n));
                same("tms " + d + n, () -> d.toLocalTime().minusSeconds(n), () -> m.toLocalTime().minusSeconds(n));
                same("tmh " + d + n, () -> d.toLocalTime().minusHours(n), () -> m.toLocalTime().minusHours(n));
                final long few = n % 1000;
                same("pw " + d + n, () -> d.plusWeeks(few), () -> m.plusWeeks(few));
                same("mw " + d + n, () -> d.minusWeeks(few), () -> m.minusWeeks(few));
                same("pmo " + d + n, () -> d.plusMonths(few), () -> m.plusMonths(few));
                same("mmo " + d + n, () -> d.minusMonths(few), () -> m.minusMonths(few));
                same("py " + d + n, () -> d.plusYears(few), () -> m.plusYears(few));
                same("my " + d + n, () -> d.minusYears(few), () -> m.minusYears(few));
            }
            for (final int n : new int[] {0, 1, 23, 24, 59, 60, 999999999}) {
                same("wh " + d + n, () -> d.withHour(n), () -> m.withHour(n));
                same("wmi " + d + n, () -> d.withMinute(n), () -> m.withMinute(n));
                same("ws " + d + n, () -> d.withSecond(n), () -> m.withSecond(n));
                same("wn " + d + n, () -> d.withNano(n), () -> m.withNano(n));
            }
            for (final Object a : new Object[] {Period.of(1, 2, 3), Period.ofMonths(-13),
                Duration.ofSeconds(86399, 999999999), Duration.ofMillis(-1), Duration.ofDays(400)}) {
                same("plus " + d + a, () -> d.plus((java.time.temporal.TemporalAmount) a), () -> m.plus(amount(a)));
                same("minus " + d + a, () -> d.minus((java.time.temporal.TemporalAmount) a),
                        () -> m.minus(amount(a)));
            }
            for (final ChronoUnit u : ChronoUnit.values()) {
                same("trunc " + d + u, () -> d.truncatedTo(u), () -> m.truncatedTo(unit(u)));
                same("time trunc " + d + u, () -> d.toLocalTime().truncatedTo(u),
                        () -> m.toLocalTime().truncatedTo(unit(u)));
                for (final long n : AMOUNTS) {
                    if (u.compareTo(ChronoUnit.CENTURIES) >= 0 && (n > 100 || n < -100)) {
                        continue;
                    }
                    // The JDK counts 256 half days to a day here, so beyond
                    // that many its answer is not the sum. JavaAPI follows it,
                    // but the grid stays inside the range that means anything.
                    if (u == ChronoUnit.HALF_DAYS && (n >= 256 || n <= -256)) {
                        continue;
                    }
                    same("plus " + d + n + u, () -> d.plus(n, u), () -> m.plus(n, unit(u)));
                    same("minus " + d + n + u, () -> d.minus(n, u), () -> m.minus(n, unit(u)));
                    same("time plus " + d + n + u, () -> d.toLocalTime().plus(n, u),
                            () -> m.toLocalTime().plus(n, unit(u)));
                }
                for (final LocalDateTime e : all) {
                    same("until " + d + e + u, () -> d.until(e, u), () -> m.until(ldt(e), unit(u)));
                }
            }
            for (final LocalDateTime e : all) {
                final jdkparity.java.time.LocalDateTime me = ldt(e);
                same("before " + d + e, () -> d.isBefore(e), () -> m.isBefore(me));
                same("after " + d + e, () -> d.isAfter(e), () -> m.isAfter(me));
                same("equal " + d + e, () -> d.isEqual(e), () -> m.isEqual(me));
                same("compare " + d + e, () -> Integer.signum(d.compareTo(e)), () -> Integer.signum(m.compareTo(me)));
                same("duration " + d + e, () -> Duration.between(d, e), () -> jdkparity.java.time.Duration.between(m, me));
                same("time " + d + e, () -> Duration.between(d.toLocalTime(), e.toLocalTime()),
                        () -> jdkparity.java.time.Duration.between(m.toLocalTime(), me.toLocalTime()));
                same("time until " + d + e, () -> d.toLocalTime().until(e.toLocalTime(), ChronoUnit.MINUTES),
                        () -> m.toLocalTime().until(me.toLocalTime(), jdkparity.java.time.temporal.ChronoUnit.MINUTES));
                same("time before " + d + e, () -> d.toLocalTime().isBefore(e.toLocalTime()),
                        () -> m.toLocalTime().isBefore(me.toLocalTime()));
            }
        }
        assertTrue(compared > 50000, "" + compared);
    }

    @Test
    void momentsAndZonesAreTheJdks() {
        final List<Instant> all = instants();
        final ZoneId[] zones = new ZoneId[ZONES.length];
        for (int z = 0; z < ZONES.length; z++) {
            zones[z] = ZoneId.of(ZONES[z]);
        }
        for (final Instant i : all) {
            final jdkparity.java.time.Instant m = in(i);
            same("text " + i, () -> i, () -> m);
            same("hash " + i, i::hashCode, m::hashCode);
            same("millis " + i, i::toEpochMilli, m::toEpochMilli);
            for (final long n : new long[] {-86400000000001L, -1, 0, 1, 999999999, 1000000000, 123456789012L}) {
                same("pn " + i + n, () -> i.plusNanos(n), () -> m.plusNanos(n));
                same("mn " + i + n, () -> i.minusNanos(n), () -> m.minusNanos(n));
                final Duration d = Duration.ofSeconds(n % 100000, n);
                same("pd " + i + n, () -> i.plus(d), () -> m.plus(duration(d)));
                same("md " + i + n, () -> i.minus(d), () -> m.minus(duration(d)));
            }
            same("period " + i, () -> i.plus(Period.ofDays(1)), () -> m.plus(jdkparity.java.time.Period.ofDays(1)));
            same("months " + i, () -> i.plus(Period.ofMonths(1)),
                    () -> m.plus(jdkparity.java.time.Period.ofMonths(1)));
            for (final ChronoUnit u : ChronoUnit.values()) {
                // Java 8 rounds a moment before 1970 up, not down.
                if (!JAVA_8 || i.getEpochSecond() >= 0) {
                    same("trunc " + i + u, () -> i.truncatedTo(u), () -> m.truncatedTo(unit(u)));
                }
                for (final long n : AMOUNTS) {
                    same("plus " + i + n + u, () -> i.plus(n, u), () -> m.plus(n, unit(u)));
                    same("minus " + i + n + u, () -> i.minus(n, u), () -> m.minus(n, unit(u)));
                }
                for (final Instant e : all) {
                    same("until " + i + e + u, () -> i.until(e, u), () -> m.until(in(e), unit(u)));
                }
            }
            for (final Instant e : all) {
                same("before " + i + e, () -> i.isBefore(e), () -> m.isBefore(in(e)));
                same("after " + i + e, () -> i.isAfter(e), () -> m.isAfter(in(e)));
                same("duration " + i + e, () -> Duration.between(i, e),
                        () -> jdkparity.java.time.Duration.between(m, in(e)));
            }
            for (final ZoneId zone : zones) {
                if (!regionRulesKnown(i, zone)) {
                    continue;
                }
                final jdkparity.java.time.ZoneId mzone = zone(zone);
                same("atZone " + i + zone, () -> i.atZone(zone), () -> m.atZone(mzone));
                same("atOffset " + i + zone, () -> i.atOffset(ZoneOffset.ofHoursMinutes(5, 30)),
                        () -> m.atOffset(jdkparity.java.time.ZoneOffset.ofHoursMinutes(5, 30)));
                final ZonedDateTime z = i.atZone(zone);
                final jdkparity.java.time.ZonedDateTime mz = m.atZone(mzone);
                same("date " + z, z::toLocalDate, mz::toLocalDate);
                same("time " + z, z::toLocalTime, mz::toLocalTime);
                same("epoch " + z, z::toEpochSecond, mz::toEpochSecond);
                same("offset " + z, z::getOffset, mz::getOffset);
                same("odt " + z, z::toOffsetDateTime, mz::toOffsetDateTime);
                same("hash " + z, z::hashCode, mz::hashCode);
                same("fields " + z,
                        () -> z.getYear() + "-" + z.getMonthValue() + "-" + z.getDayOfMonth() + " " + z.getHour()
                        + ":" + z.getMinute() + ":" + z.getSecond() + "." + z.getNano() + " " + z.getDayOfYear()
                        + z.getMonth().name() + z.getDayOfWeek().name(),
                        () -> mz.getYear() + "-" + mz.getMonthValue() + "-" + mz.getDayOfMonth() + " " + mz.getHour()
                        + ":" + mz.getMinute() + ":" + mz.getSecond() + "." + mz.getNano() + " " + mz.getDayOfYear()
                        + mz.getMonth().name() + mz.getDayOfWeek().name());
                same("same instant " + z, () -> z.withZoneSameInstant(zones[2]),
                        () -> mz.withZoneSameInstant(zone(zones[2])));
                if (regionRulesKnown(i, zones[1])) {
                    same("local at " + z, () -> z.toLocalDateTime().atZone(zones[1]),
                            () -> mz.toLocalDateTime().atZone(zone(zones[1])));
                    same("same local " + z, () -> z.withZoneSameLocal(zones[1]),
                            () -> mz.withZoneSameLocal(zone(zones[1])));
                }
                for (final long n : new long[] {-400, -1, 0, 1, 25, 400}) {
                    if (!regionRulesKnown(i.minusSeconds(Math.abs(n) * 86400L * 366L), zone)) {
                        continue;
                    }
                    same("pd " + z + n, () -> z.plusDays(n), () -> mz.plusDays(n));
                    same("md " + z + n, () -> z.minusDays(n), () -> mz.minusDays(n));
                    same("pw " + z + n, () -> z.plusWeeks(n), () -> mz.plusWeeks(n));
                    same("mw " + z + n, () -> z.minusWeeks(n), () -> mz.minusWeeks(n));
                    same("pmo " + z + n, () -> z.plusMonths(n), () -> mz.plusMonths(n));
                    same("mmo " + z + n, () -> z.minusMonths(n), () -> mz.minusMonths(n));
                    same("py " + z + n, () -> z.plusYears(n), () -> mz.plusYears(n));
                    same("my " + z + n, () -> z.minusYears(n), () -> mz.minusYears(n));
                    same("ph " + z + n, () -> z.plusHours(n), () -> mz.plusHours(n));
                    same("mh " + z + n, () -> z.minusHours(n), () -> mz.minusHours(n));
                    same("pmi " + z + n, () -> z.plusMinutes(n), () -> mz.plusMinutes(n));
                    same("mmi " + z + n, () -> z.minusMinutes(n), () -> mz.minusMinutes(n));
                    same("ps " + z + n, () -> z.plusSeconds(n), () -> mz.plusSeconds(n));
                    same("ms " + z + n, () -> z.minusSeconds(n), () -> mz.minusSeconds(n));
                }
                for (int other = 0; other < all.size(); other += 3) {
                    if (!regionRulesKnown(all.get(other), zones[1])) {
                        continue;
                    }
                    final ZonedDateTime e = all.get(other).atZone(zones[1]);
                    final jdkparity.java.time.ZonedDateTime me = in(all.get(other)).atZone(zone(zones[1]));
                    same("before " + z + e, () -> z.isBefore(e), () -> mz.isBefore(me));
                    same("after " + z + e, () -> z.isAfter(e), () -> mz.isAfter(me));
                    same("equal " + z + e, () -> z.isEqual(e), () -> mz.isEqual(me));
                    same("compare " + z + e, () -> Integer.signum(z.compareTo(e)),
                            () -> Integer.signum(mz.compareTo(me)));
                    same("duration " + z + e, () -> Duration.between(z, e),
                            () -> jdkparity.java.time.Duration.between(mz, me));
                    for (final ChronoUnit u : ChronoUnit.values()) {
                        same("until " + z + e + u, () -> z.until(e, u), () -> mz.until(me, unit(u)));
                    }
                }
            }
        }
        assertTrue(compared > 50000, "" + compared);
    }

    /// The wall clock times a zone skips or passes twice, resolved as the JDK
    /// resolves them.
    @Test
    void gapsAndOverlapsAreTheJdks() {
        final ZoneId ny = ZoneId.of("America/New_York");
        final jdkparity.java.time.ZoneId mny = zone(ny);
        for (final LocalDateTime local : new LocalDateTime[] {LocalDateTime.of(2024, 3, 10, 2, 0),
            LocalDateTime.of(2024, 3, 10, 2, 30), LocalDateTime.of(2024, 3, 10, 3, 0),
            LocalDateTime.of(2024, 11, 3, 0, 59), LocalDateTime.of(2024, 11, 3, 1, 0),
            LocalDateTime.of(2024, 11, 3, 1, 30), LocalDateTime.of(2024, 11, 3, 2, 0)}) {
            same("of " + local, () -> ZonedDateTime.of(local, ny), () -> jdkparity.java.time.ZonedDateTime.of(ldt(local), mny));
            for (final int off : new int[] {-4, -5, 3}) {
                same("ofLocal " + local + off, () -> ZonedDateTime.ofLocal(local, ny, ZoneOffset.ofHours(off)),
                        () -> jdkparity.java.time.ZonedDateTime.ofLocal(ldt(local), mny,
                                jdkparity.java.time.ZoneOffset.ofHours(off)));
            }
            final ZonedDateTime z = ZonedDateTime.of(local, ny);
            final jdkparity.java.time.ZonedDateTime mz = jdkparity.java.time.ZonedDateTime.of(ldt(local), mny);
            same("earlier " + z, z::withEarlierOffsetAtOverlap, mz::withEarlierOffsetAtOverlap);
            same("later " + z, z::withLaterOffsetAtOverlap, mz::withLaterOffsetAtOverlap);
            same("later earlier " + z, () -> z.withLaterOffsetAtOverlap().withEarlierOffsetAtOverlap(),
                    () -> mz.withLaterOffsetAtOverlap().withEarlierOffsetAtOverlap());
            for (final long n : new long[] {-2, -1, 1, 2, 24}) {
                same("ph " + z + n, () -> z.plusHours(n), () -> mz.plusHours(n));
                same("pd " + z + n, () -> z.plusDays(n), () -> mz.plusDays(n));
                same("pmi " + z + n, () -> z.plusMinutes(n * 30), () -> mz.plusMinutes(n * 30));
            }
        }
    }

    @Test
    void durationsAreTheJdks() {
        final Duration[] all = {Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(-1), Duration.ofMillis(1500),
            Duration.ofMillis(-1500), Duration.ofSeconds(59), Duration.ofSeconds(3600), Duration.ofSeconds(86399, 5),
            Duration.ofSeconds(-86401, 999999999), Duration.ofDays(400), Duration.ofDays(-400),
            Duration.ofSeconds(90061, 123456789)};
        same("zero", () -> Duration.ZERO, () -> jdkparity.java.time.Duration.ZERO);
        for (final Duration d : all) {
            final jdkparity.java.time.Duration m = duration(d);
            same("text " + d, () -> d, () -> m);
            same("hash " + d, d::hashCode, m::hashCode);
            same("days " + d, d::toDays, m::toDays);
            same("hours " + d, d::toHours, m::toHours);
            same("minutes " + d, d::toMinutes, m::toMinutes);
            // Java 8 floors a negative fraction of a millisecond; later
            // releases truncate it toward zero, as JavaAPI does.
            if (!JAVA_8 || !d.isNegative() || d.getNano() == 0) {
                same("millis " + d, d::toMillis, m::toMillis);
            }
            same("seconds " + d, d::getSeconds, m::getSeconds);
            same("nanos " + d, d::toNanos, m::toNanos);
            same("zero " + d, d::isZero, m::isZero);
            same("negative " + d, d::isNegative, m::isNegative);
            same("negated " + d, d::negated, m::negated);
            same("abs " + d, d::abs, m::abs);
            // The parts came with Java 9; computed here from their definitions
            // so that the Java 8 run compares them too.
            same("hours part " + d, () -> (int) (d.toHours() % 24), m::toHoursPart);
            same("minutes part " + d, () -> (int) (d.toMinutes() % 60), m::toMinutesPart);
            same("seconds part " + d, () -> (int) (d.getSeconds() % 60), m::toSecondsPart);
            same("millis part " + d, () -> d.getNano() / 1000000, m::toMillisPart);
            for (final long n : new long[] {-100000, -7, -1, 0, 1, 3, 1000, 123456789}) {
                same("pd " + d + n, () -> d.plusDays(n), () -> m.plusDays(n));
                same("ph " + d + n, () -> d.plusHours(n), () -> m.plusHours(n));
                same("pmi " + d + n, () -> d.plusMinutes(n), () -> m.plusMinutes(n));
                same("ps " + d + n, () -> d.plusSeconds(n), () -> m.plusSeconds(n));
                same("pms " + d + n, () -> d.plusMillis(n), () -> m.plusMillis(n));
                same("pn " + d + n, () -> d.plusNanos(n), () -> m.plusNanos(n));
                same("md " + d + n, () -> d.minusDays(n), () -> m.minusDays(n));
                same("mh " + d + n, () -> d.minusHours(n), () -> m.minusHours(n));
                same("mmi " + d + n, () -> d.minusMinutes(n), () -> m.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n), () -> m.minusSeconds(n));
                same("mms " + d + n, () -> d.minusMillis(n), () -> m.minusMillis(n));
                same("mn " + d + n, () -> d.minusNanos(n), () -> m.minusNanos(n));
                same("times " + d + n, () -> d.multipliedBy(n % 1000), () -> m.multipliedBy(n % 1000));
                same("over " + d + n, () -> d.dividedBy(n), () -> m.dividedBy(n));
                same("ofNanos " + n, () -> Duration.ofNanos(n * 1000003),
                        () -> jdkparity.java.time.Duration.ofNanos(n * 1000003));
            }
            for (final Duration e : all) {
                same("plus " + d + e, () -> d.plus(e), () -> m.plus(duration(e)));
                same("minus " + d + e, () -> d.minus(e), () -> m.minus(duration(e)));
                same("compare " + d + e, () -> Integer.signum(d.compareTo(e)),
                        () -> Integer.signum(m.compareTo(duration(e))));
            }
        }
        // Past what a long counts in nanoseconds, and at its very edges:
        // the JDK throws rather than wrapping.
        final Duration[] extremes = {Duration.ofDays(365L * 300), Duration.ofDays(-365L * 300),
            Duration.ofNanos(Long.MAX_VALUE), Duration.ofNanos(Long.MIN_VALUE).plusNanos(-1),
            Duration.ofNanos(Long.MAX_VALUE).plusNanos(1), Duration.ofSeconds(-1, 999999999), Duration.ofSeconds(-1, 1)};
        // The most negative long: Java 8 overflows on the way and throws,
        // later releases borrow a second first and answer it, as JavaAPI does.
        assertEquals(Long.MIN_VALUE, jdkparity.java.time.Duration.ofNanos(Long.MIN_VALUE).toNanos());
        for (final Duration d : new Duration[] {Duration.ZERO, Duration.ofSeconds(-5, 1), Duration.ofSeconds(5),
            Duration.ofSeconds(Long.MIN_VALUE), Duration.ofSeconds(Long.MIN_VALUE, 1),
            Duration.ofSeconds(Long.MAX_VALUE, 999999999)}) {
            final jdkparity.java.time.Duration m = duration(d);
            same("negated " + d, d::negated, m::negated);
            for (final long n : new long[] {Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE / 60, -1}) {
                same("pd " + d + n, () -> d.plusDays(n), () -> m.plusDays(n));
                same("ph " + d + n, () -> d.plusHours(n), () -> m.plusHours(n));
                same("pmi " + d + n, () -> d.plusMinutes(n), () -> m.plusMinutes(n));
                same("ps " + d + n, () -> d.plusSeconds(n), () -> m.plusSeconds(n));
                same("pms " + d + n, () -> d.plusMillis(n), () -> m.plusMillis(n));
                same("pn " + d + n, () -> d.plusNanos(n), () -> m.plusNanos(n));
                same("md " + d + n, () -> d.minusDays(n), () -> m.minusDays(n));
                same("mh " + d + n, () -> d.minusHours(n), () -> m.minusHours(n));
                same("mmi " + d + n, () -> d.minusMinutes(n), () -> m.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n), () -> m.minusSeconds(n));
                same("mms " + d + n, () -> d.minusMillis(n), () -> m.minusMillis(n));
                same("mn " + d + n, () -> d.minusNanos(n), () -> m.minusNanos(n));
            }
        }
        for (final Duration d : extremes) {
            final jdkparity.java.time.Duration m = duration(d);
            same("nanos " + d, d::toNanos, m::toNanos);
            for (final long n : new long[] {-3, 2, 1000000007L, -999999999999L, Long.MAX_VALUE, Long.MIN_VALUE}) {
                same("times " + d + n, () -> d.multipliedBy(n), () -> m.multipliedBy(n));
            }
        }
        for (final ChronoUnit u : ChronoUnit.values()) {
            same("name " + u, u::toString, () -> unit(u).toString());
            same("length " + u, u::getDuration, () -> unit(u).getDuration());
            same("estimated " + u, u::isDurationEstimated, () -> unit(u).isDurationEstimated());
            same("date " + u, u::isDateBased, () -> unit(u).isDateBased());
            same("time " + u, u::isTimeBased, () -> unit(u).isTimeBased());
            for (final long n : AMOUNTS) {
                same("of " + n + u, () -> Duration.of(n, u), () -> jdkparity.java.time.Duration.of(n, unit(u)));
            }
        }
        assertEquals(ChronoUnit.values().length, jdkparity.java.time.temporal.ChronoUnit.values().length);
        for (final String text : new String[] {"PT0S", "PT1.5S", "P2DT3H4M", "PT-0.5S", "-PT6H3M", "PT25H", "nonsense",
            "P1Y"}) {
            // Java 8 loses the sign of a negative fraction of a second.
            if (!JAVA_8 || !text.startsWith("PT-0.")) {
                same("parse " + text, () -> Duration.parse(text), () -> jdkparity.java.time.Duration.parse(text));
            }
        }
        assertTrue(compared > 1500, "" + compared);
    }

    @Test
    void periodsAreTheJdks() {
        final Period[] all = {Period.ZERO, Period.of(1, 2, 3), Period.of(0, 13, 31), Period.of(-1, -1, -1),
            Period.ofDays(400), Period.ofWeeks(3), Period.of(2, -25, 0), Period.ofYears(Integer.MAX_VALUE)};
        for (final Period p : all) {
            final jdkparity.java.time.Period m = period(p);
            same("text " + p, () -> p, () -> m);
            same("hash " + p, p::hashCode, m::hashCode);
            same("zero " + p, p::isZero, m::isZero);
            same("negative " + p, p::isNegative, m::isNegative);
            same("months " + p, p::toTotalMonths, m::toTotalMonths);
            same("normalized " + p, p::normalized, m::normalized);
            same("negated " + p, p::negated, m::negated);
            for (final int n : new int[] {-1, 0, 2, Integer.MAX_VALUE}) {
                same("times " + p + n, () -> p.multipliedBy(n), () -> m.multipliedBy(n));
                same("py " + p + n, () -> p.plusYears(n), () -> m.plusYears(n));
                same("pm " + p + n, () -> p.plusMonths(n), () -> m.plusMonths(n));
                same("pd " + p + n, () -> p.plusDays(n), () -> m.plusDays(n));
            }
            for (final Period q : all) {
                same("plus " + p + q, () -> p.plus(q), () -> m.plus(period(q)));
                same("minus " + p + q, () -> p.minus(q), () -> m.minus(period(q)));
                same("equals " + p + q, () -> p.equals(q), () -> m.equals(period(q)));
            }
        }
        for (final LocalDate a : DATES) {
            for (final LocalDate b : DATES) {
                same("between " + a + b, () -> Period.between(a, b),
                        () -> jdkparity.java.time.Period.between(ld(a), ld(b)));
            }
        }
        for (final String text : new String[] {"P1Y2M3D", "P2W", "p1y2w3d", "-P1Y-2M", "+P-1D", "P1W-3D", "PT1S",
            "P", "P1Y1Y", "P1M1Y", "P1", "P2147483647W", "P99999999999D", "1Y", "", "P-0D", "P+5Y"}) {
            same("parse " + text, () -> Period.parse(text), () -> jdkparity.java.time.Period.parse(text));
        }
        for (final Object a : new Object[] {Period.ofDays(3), Period.ofMonths(1), Duration.ofSeconds(5, 7)}) {
            same("duration from " + a, () -> Duration.from((java.time.temporal.TemporalAmount) a),
                    () -> jdkparity.java.time.Duration.from(amount(a)));
            same("period from " + a, () -> Period.from((java.time.temporal.TemporalAmount) a),
                    () -> jdkparity.java.time.Period.from(amount(a)));
        }
    }

    @Test
    void offsetsZonesAndClocksAreTheJdks() {
        same("min", () -> OffsetDateTime.MIN, () -> jdkparity.java.time.OffsetDateTime.MIN);
        same("max", () -> OffsetDateTime.MAX, () -> jdkparity.java.time.OffsetDateTime.MAX);
        same("instant min", () -> Instant.MIN, () -> jdkparity.java.time.Instant.MIN);
        same("instant max", () -> Instant.MAX, () -> jdkparity.java.time.Instant.MAX);
        for (final LocalDateTime local : dateTimes()) {
            if (local.getYear() < 1901) {
                continue;
            }
            final OffsetDateTime o = local.atOffset(ZoneOffset.ofHours(3));
            final jdkparity.java.time.OffsetDateTime mo = ldt(local).atOffset(jdkparity.java.time.ZoneOffset.ofHours(3));
            final ZonedDateTime z = local.atZone(ZoneId.of("America/New_York"));
            final jdkparity.java.time.ZonedDateTime mz = ldt(local).atZone(zone(ZoneId.of("America/New_York")));
            same("fixed " + z, z::withFixedOffsetZone, mz::withFixedOffsetZone);
            for (final int n : new int[] {0, 1, 12, 28, 29, 31, 59, 60, 365, 366}) {
                same("y " + o + n, () -> o.withYear(2000 + n), () -> mo.withYear(2000 + n));
                same("mo " + o + n, () -> o.withMonth(n), () -> mo.withMonth(n));
                same("dom " + o + n, () -> o.withDayOfMonth(n), () -> mo.withDayOfMonth(n));
                same("doy " + o + n, () -> o.withDayOfYear(n), () -> mo.withDayOfYear(n));
                same("h " + o + n, () -> o.withHour(n), () -> mo.withHour(n));
                same("mi " + o + n, () -> o.withMinute(n), () -> mo.withMinute(n));
                same("s " + o + n, () -> o.withSecond(n), () -> mo.withSecond(n));
                same("n " + o + n, () -> o.withNano(n), () -> mo.withNano(n));
                same("zoned doy " + z + n, () -> z.withDayOfYear(n), () -> mz.withDayOfYear(n));
            }
        }
        final Instant at = Instant.ofEpochSecond(1700000000L, 5);
        final java.time.Clock fixed = java.time.Clock.fixed(at, ZoneOffset.UTC);
        final jdkparity.java.time.Clock mfixed = jdkparity.java.time.Clock.fixed(in(at),
                jdkparity.java.time.ZoneOffset.UTC);
        final ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        same("clock zone", () -> fixed.withZone(kolkata).getZone(), () -> mfixed.withZone(zone(kolkata)).getZone());
        same("clock zoned", () -> ZonedDateTime.now(fixed.withZone(kolkata)),
                () -> jdkparity.java.time.ZonedDateTime.now(mfixed.withZone(zone(kolkata))));
        same("clock offset", () -> java.time.Clock.offset(fixed, Duration.ofHours(-5)).instant(),
                () -> jdkparity.java.time.Clock.offset(mfixed, jdkparity.java.time.Duration.ofHours(-5)).instant());
        same("clock equals", () -> fixed.equals(java.time.Clock.fixed(at, ZoneOffset.UTC)) + " "
                + fixed.withZone(ZoneOffset.UTC).equals(fixed) + " " + fixed.equals(fixed.withZone(kolkata)),
                () -> mfixed.equals(jdkparity.java.time.Clock.fixed(in(at), jdkparity.java.time.ZoneOffset.UTC)) + " "
                + mfixed.withZone(jdkparity.java.time.ZoneOffset.UTC).equals(mfixed) + " "
                + mfixed.equals(mfixed.withZone(zone(kolkata))));
    }

    /// Patterns over a date alone, a time alone, a date with a time, and a
    /// zoned date and time; and what a pattern reads back.
    @Test
    void patternsFormatAndParseAsTheJdks() {
        final String[] patterns = {"HH:mm:ss", "H:m", "hh:mm a", "dd.MM.yyyy", "yyyy-MM-dd", "d/M/yy",
            "yyyy-MM-dd HH:mm", "'at' HH 'h'", "EEE d", "HH:mm:ss.SSS", "EEEE, MMMM d, uuuu", "MMM", "LLLLL",
            "G yyyy", "GGGG", "D", "DDD", "QQQ QQQQ q", "K k h", "n N A", "S SSSSSS", "''yy''", "yyyy[ HH:mm]",
            "yyyyMMdd", "HHmmss", "XXX", "xx", "Z", "ZZZZZ", "O", "OOOO", "VV", "yyyy-MM-dd'T'HH:mm:ssXXX"};
        final LocalTime[] times = {LocalTime.of(0, 0), LocalTime.of(1, 2, 3), LocalTime.of(13, 59, 58, 123000000),
            LocalTime.of(23, 59, 59), LocalTime.of(12, 0, 0, 1)};
        final LocalDate[] dates = {LocalDate.of(1970, 1, 1), LocalDate.of(1999, 2, 21), LocalDate.of(2024, 2, 29),
            LocalDate.of(2031, 12, 31), LocalDate.of(-5, 6, 7), LocalDate.of(12345, 1, 2)};
        final ZoneId[] zones = {ZoneOffset.UTC, ZoneOffset.ofHoursMinutes(5, 30), ZoneOffset.ofHours(-8),
            ZoneId.of("America/New_York")};
        for (final String pattern : patterns) {
            if (JAVA_8 && pattern.startsWith("L")) {
                // Java 8 has no stand-alone month names and prints the number.
                continue;
            }
            final DateTimeFormatter jdk = DateTimeFormatter.ofPattern(pattern, Locale.US);
            final jdkparity.java.time.format.DateTimeFormatter api =
                    jdkparity.java.time.format.DateTimeFormatter.ofPattern(pattern, Locale.US);
            for (final LocalTime t : times) {
                same("time " + pattern + t, () -> t.format(jdk), () -> lt(t).format(api));
                same("time of " + pattern + t, () -> jdk.format(t), () -> api.format(lt(t)));
            }
            for (final LocalDate d : dates) {
                same("date " + pattern + d, () -> d.format(jdk), () -> ld(d).format(api));
                same("both " + pattern + d, () -> d.atTime(4, 5, 6).format(jdk),
                        () -> ld(d).atTime(4, 5, 6).format(api));
                for (final ZoneId z : zones) {
                    final ZonedDateTime zd = d.atTime(4, 5, 6).atZone(z);
                    if (!regionRulesKnown(zd.toInstant(), z)) {
                        continue;
                    }
                    same("zoned " + pattern + zd, () -> zd.format(jdk), () -> zdt(zd).format(api));
                    same("offset " + pattern + zd, () -> zd.toOffsetDateTime().format(jdk),
                            () -> zdt(zd).toOffsetDateTime().format(api));
                }
            }
            for (final LocalDate d : dates) {
                final String text = d.atTime(4, 5, 6).atZone(ZoneOffset.ofHours(2)).format(jdk);
                same("read date " + pattern + text, () -> LocalDate.parse(text, jdk),
                        () -> jdkparity.java.time.LocalDate.parse(text, api));
                same("read time " + pattern + text, () -> LocalTime.parse(text, jdk),
                        () -> jdkparity.java.time.LocalTime.parse(text, api));
                same("read both " + pattern + text, () -> LocalDateTime.parse(text, jdk),
                        () -> jdkparity.java.time.LocalDateTime.parse(text, api));
            }
        }
        final DateTimeFormatter day = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        final DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm:ss");
        final jdkparity.java.time.format.DateTimeFormatter mday =
                jdkparity.java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        final jdkparity.java.time.format.DateTimeFormatter mclock =
                jdkparity.java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        for (final String text : new String[] {"21.02.1999", "29.02.2024", "30.02.2024", "31.04.2023", "32.01.2024",
            "31.12.2031", "1.1.2024", "nonsense", "12:13:14", "24:00:00", "21.02.1999 ", ""}) {
            same("parse date " + text, () -> LocalDate.parse(text, day),
                    () -> jdkparity.java.time.LocalDate.parse(text, mday));
            same("parse time " + text, () -> LocalTime.parse(text, clock),
                    () -> jdkparity.java.time.LocalTime.parse(text, mclock));
        }
        for (final String bad : new String[] {"yyyy-MM-dd ]", "'open", "{y}", "ddd", "aa", "V", "OO", "SSSSSSSSSS",
            "b"}) {
            same("refused " + bad, () -> DateTimeFormatter.ofPattern(bad),
                    () -> jdkparity.java.time.format.DateTimeFormatter.ofPattern(bad) == null ? "null" : "made");
        }
        assertTrue(compared > 1500, "" + compared);
    }

    @Test
    void isoFormatsAreTheJdks() {
        final List<ZonedDateTime> values = new ArrayList<ZonedDateTime>();
        for (final LocalDateTime local : dateTimes()) {
            values.add(local.atZone(ZoneOffset.UTC));
            values.add(local.atZone(ZoneOffset.ofHoursMinutes(-3, -30)));
            values.add(local.atZone(ZoneId.of("UTC")));
            if (local.getYear() >= 1901) {
                values.add(local.atZone(ZoneId.of("Asia/Kolkata")));
            }
        }
        final DateTimeFormatter[] jdk = {DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME, DateTimeFormatter.ISO_INSTANT};
        final jdkparity.java.time.format.DateTimeFormatter[] api = {
            jdkparity.java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
            jdkparity.java.time.format.DateTimeFormatter.ISO_LOCAL_TIME,
            jdkparity.java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            jdkparity.java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            jdkparity.java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME,
            jdkparity.java.time.format.DateTimeFormatter.ISO_INSTANT};
        for (final ZonedDateTime z : values) {
            final jdkparity.java.time.ZonedDateTime mz = zdt(z);
            same("zoned text " + z, () -> z, () -> mz);
            same("offset text " + z, z::toOffsetDateTime, mz::toOffsetDateTime);
            same("instant text " + z, z::toInstant, mz::toInstant);
            for (int f = 0; f < jdk.length; f++) {
                final DateTimeFormatter j = jdk[f];
                final jdkparity.java.time.format.DateTimeFormatter a = api[f];
                same("zoned " + f + z, () -> j.format(z), () -> a.format(mz));
                same("offset " + f + z, () -> j.format(z.toOffsetDateTime()), () -> a.format(mz.toOffsetDateTime()));
                same("local " + f + z, () -> j.format(z.toLocalDateTime()), () -> a.format(mz.toLocalDateTime()));
                same("date " + f + z, () -> j.format(z.toLocalDate()), () -> a.format(mz.toLocalDate()));
                same("time " + f + z, () -> j.format(z.toLocalTime()), () -> a.format(mz.toLocalTime()));
                same("instant " + f + z, () -> j.format(z.toInstant()), () -> a.format(mz.toInstant()));
                if (regionRulesKnown(z.toInstant(), ZoneId.of("Asia/Kolkata"))) {
                    same("in zone " + f + z, () -> j.withZone(ZoneId.of("Asia/Kolkata")).format(z.toInstant()),
                            () -> a.withZone(jdkparity.java.time.ZoneId.of("Asia/Kolkata")).format(mz.toInstant()));
                }
                final String text = j.format(z);
                same("reread zoned " + f + text, () -> ZonedDateTime.parse(text, j),
                        () -> jdkparity.java.time.ZonedDateTime.parse(text, a));
                same("reread offset " + f + text, () -> OffsetDateTime.parse(text, j),
                        () -> jdkparity.java.time.OffsetDateTime.parse(text, a));
                same("reread local " + f + text, () -> LocalDateTime.parse(text, j),
                        () -> jdkparity.java.time.LocalDateTime.parse(text, a));
                same("reread date " + f + text, () -> LocalDate.parse(text, j),
                        () -> jdkparity.java.time.LocalDate.parse(text, a));
                same("reread time " + f + text, () -> LocalTime.parse(text, j),
                        () -> jdkparity.java.time.LocalTime.parse(text, a));
                // Java 8 keeps the wall clock time of a text with an offset
                // and drops the offset when the formatter has a zone; later
                // releases keep the moment.
                if (!JAVA_8 || f <= 2) {
                    same("reread with zone " + f + text,
                            () -> ZonedDateTime.parse(text, j.withZone(ZoneId.of("America/New_York"))),
                            () -> jdkparity.java.time.ZonedDateTime.parse(text,
                                    a.withZone(jdkparity.java.time.ZoneId.of("America/New_York"))));
                }
            }
        }
        for (final String text : new String[] {"2024-02-29", "2023-02-29", "+12345-01-02", "12345-01-02", "-0005-06-07",
            "2024-1-01", "12:30", "12:30:45.5", "24:00", "2024-02-29T12:30", "2024-02-29t12:30:00Z",
            "2024-02-29T12:30:00+05:30", "2024-02-29T12:30:00-03:30[America/New_York]", "2024-02-29T12:30:00Z[UTC]",
            "2024-02-29T23:59:60Z", "2024-02-29T24:00:00Z", "2024-02-29T12:30:00.123456Z"}) {
            same("date " + text, () -> LocalDate.parse(text), () -> jdkparity.java.time.LocalDate.parse(text));
            same("time " + text, () -> LocalTime.parse(text), () -> jdkparity.java.time.LocalTime.parse(text));
            same("local " + text, () -> LocalDateTime.parse(text), () -> jdkparity.java.time.LocalDateTime.parse(text));
            same("offset " + text, () -> OffsetDateTime.parse(text),
                    () -> jdkparity.java.time.OffsetDateTime.parse(text));
            // An offset beside a region the offset is wrong for: Java 8 keeps
            // the wall clock time, later releases the moment.
            if (!JAVA_8 || !text.endsWith("[America/New_York]")) {
                same("zoned " + text, () -> ZonedDateTime.parse(text),
                        () -> jdkparity.java.time.ZonedDateTime.parse(text));
            }
            if (INSTANT_READS_OFFSETS || text.endsWith("Z") || text.indexOf('T') < 0) {
                same("instant " + text, () -> Instant.parse(text), () -> jdkparity.java.time.Instant.parse(text));
            }
        }
        assertTrue(compared > 5000, "" + compared);
    }

    @Test
    void zonesAndOffsetsAreTheJdks() {
        for (final String id : new String[] {"Z", "UTC", "GMT", "UT", "UTC+01:00", "GMT-5", "UT+0", "+05:30",
            "-08", "+1", "+0530", "+05:30:15", "+18:00", "+18:01", "-19", "America/New_York", "Bad Zone", "",
            "UTC+", "GMT0"}) {
            same("zone " + id, () -> ZoneId.of(id), () -> jdkparity.java.time.ZoneId.of(id));
            same("zone class " + id, () -> ZoneId.of(id) instanceof ZoneOffset,
                    () -> jdkparity.java.time.ZoneId.of(id) instanceof jdkparity.java.time.ZoneOffset);
            same("normalized " + id, () -> ZoneId.of(id).normalized(),
                    () -> jdkparity.java.time.ZoneId.of(id).normalized());
            same("offset " + id, () -> ZoneOffset.of(id), () -> jdkparity.java.time.ZoneOffset.of(id));
            same("utc equals " + id, () -> ZoneOffset.UTC.equals(ZoneId.of(id)),
                    () -> jdkparity.java.time.ZoneOffset.UTC.equals(jdkparity.java.time.ZoneId.of(id)));
        }
        for (final int seconds : new int[] {0, 1, -1, 3600, -3600, 19800, 64800, 64801, -64800, 45296}) {
            same("total " + seconds, () -> ZoneOffset.ofTotalSeconds(seconds),
                    () -> jdkparity.java.time.ZoneOffset.ofTotalSeconds(seconds));
        }
        for (final int[] hms : new int[][] {{1, 30, 0}, {-1, -30, 0}, {1, -30, 0}, {0, 30, -1}, {0, -30, -15},
            {18, 0, 0}, {18, 1, 0}, {5, 60, 0}}) {
            same("hms " + hms[0] + hms[1] + hms[2], () -> ZoneOffset.ofHoursMinutesSeconds(hms[0], hms[1], hms[2]),
                    () -> jdkparity.java.time.ZoneOffset.ofHoursMinutesSeconds(hms[0], hms[1], hms[2]));
        }
    }

    @Test
    void daysAndMonthsAreTheJdks() {
        for (final java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            final jdkparity.java.time.DayOfWeek mine = jdkparity.java.time.DayOfWeek.valueOf(day.name());
            same("value " + day, day::getValue, mine::getValue);
            same("text " + day, day::toString, mine::toString);
            for (final long n : new long[] {-15, -7, -1, 0, 1, 6, 7, 100, Long.MIN_VALUE, Long.MAX_VALUE}) {
                same("plus " + day + n, () -> day.plus(n), () -> mine.plus(n));
                same("minus " + day + n, () -> day.minus(n), () -> mine.minus(n));
            }
        }
        for (final java.time.Month month : java.time.Month.values()) {
            final jdkparity.java.time.Month mine = jdkparity.java.time.Month.valueOf(month.name());
            same("value " + month, month::getValue, mine::getValue);
            same("min " + month, month::minLength, mine::minLength);
            same("max " + month, month::maxLength, mine::maxLength);
            same("quarter " + month, month::firstMonthOfQuarter, mine::firstMonthOfQuarter);
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
            same("day of " + n, () -> java.time.DayOfWeek.of(n), () -> jdkparity.java.time.DayOfWeek.of(n));
            same("month of " + n, () -> java.time.Month.of(n), () -> jdkparity.java.time.Month.of(n));
            same("date of " + n, () -> LocalDate.of(2024, java.time.Month.of(n), 5),
                    () -> jdkparity.java.time.LocalDate.of(2024, jdkparity.java.time.Month.of(n), 5));
        }
        assertTrue(compared > 500, "" + compared);
    }

    /// Amounts too large for the seconds they stand for: the JDK refuses
    /// every one of them rather than wrapping around.
    @Test
    void amountsThatOverflowAreRefusedAsTheJdkRefusesThem() {
        final long[] huge = {4611686018427387904L, -4611686018427387904L, Long.MAX_VALUE, Long.MIN_VALUE,
            Long.MAX_VALUE / 60 + 1, Long.MAX_VALUE / 3600 + 1, 153722867280912930L, 2562047788015215L,
            31556889864403199L, -31557014167219200L, 1000000L * 365 * 24 * 3600};
        for (final long n : huge) {
            for (final ChronoUnit u : ChronoUnit.values()) {
                same("of " + n + u, () -> Duration.of(n, u), () -> jdkparity.java.time.Duration.of(n, unit(u)));
            }
            for (final String id : ZONES) {
                final ZonedDateTime z = Instant.ofEpochSecond(1700000000L, 5).atZone(ZoneId.of(id));
                final jdkparity.java.time.ZonedDateTime mz = zdt(z);
                same("pmi " + z + n, () -> z.plusMinutes(n), () -> mz.plusMinutes(n));
                same("mmi " + z + n, () -> z.minusMinutes(n), () -> mz.minusMinutes(n));
                same("ph " + z + n, () -> z.plusHours(n), () -> mz.plusHours(n));
                same("mh " + z + n, () -> z.minusHours(n), () -> mz.minusHours(n));
                same("ps " + z + n, () -> z.plusSeconds(n), () -> mz.plusSeconds(n));
                same("ms " + z + n, () -> z.minusSeconds(n), () -> mz.minusSeconds(n));
            }
            final Instant i = Instant.ofEpochSecond(1700000000L, 5);
            same("instant ps " + n, () -> i.plusSeconds(n), () -> in(i).plusSeconds(n));
            same("instant pms " + n, () -> i.plusMillis(n), () -> in(i).plusMillis(n));
            same("instant of " + n, () -> Instant.ofEpochSecond(n), () -> jdkparity.java.time.Instant.ofEpochSecond(n));
            same("instant ofMilli " + n, () -> Instant.ofEpochMilli(n),
                    () -> jdkparity.java.time.Instant.ofEpochMilli(n));
            // Java 8 overflows on the way and answers a date; later releases
            // refuse the day, as JavaAPI does.
            if (!JAVA_8) {
                same("epoch day " + n, () -> LocalDate.ofEpochDay(n),
                        () -> jdkparity.java.time.LocalDate.ofEpochDay(n));
            }
        }
    }
}
