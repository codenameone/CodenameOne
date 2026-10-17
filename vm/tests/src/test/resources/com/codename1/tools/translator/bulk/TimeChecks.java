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

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// java.time over a grid of dates, times, moments, zones and units. Every
/// answer is printed as text: the value's `toString`, an enum's name, or the
/// simple name of what was thrown (`DateTimeException` for any of its
/// subclasses, as the JDK throws narrower ones in places).
///
/// Zoned values in a region are printed only from 1900 on. JavaAPI takes a
/// region's offsets from `java.util.TimeZone`, which has no rules before 1900
/// and answers the 1900 offset for anything earlier, where the JDK's
/// `java.time` answers local mean time; that is a difference in the host's
/// data, not in the arithmetic under test. Such a result prints a marker
/// instead of its value.
public final class TimeChecks {
    private TimeChecks() {
    }

    /// One computation of the grid.
    private interface Call {
        Object call() throws Exception;
    }

    private static int compared;

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

    private static final String UNKNOWN_RULES = "(before the region's rules)";

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

    private static boolean regionRulesKnown(Instant i, ZoneId zone) {
        return zone instanceof ZoneOffset || "UTC".equals(zone.getId()) || i.getEpochSecond() >= REGION_RULES_FROM;
    }

    private static String simpleName(Throwable t) {
        String n = t.getClass().getName();
        int dot = n.lastIndexOf('.');
        return dot < 0 ? n : n.substring(dot + 1);
    }

    /// What `call` answers, or the marker when it answers a moment in a
    /// region before that region's rules are known; see the class comment.
    private static String outcome(Call call) {
        try {
            Object o = call.call();
            if (o instanceof Enum) {
                return ((Enum<?>) o).name();
            }
            if (o instanceof ZonedDateTime) {
                ZonedDateTime z = (ZonedDateTime) o;
                if (!regionRulesKnown(z.toInstant(), z.getZone())) {
                    return UNKNOWN_RULES;
                }
            }
            return String.valueOf(o);
        } catch (DateTimeException e) {
            return "DateTimeException";
        } catch (Exception e) {
            return simpleName(e);
        }
    }

    private static void same(String what, Call call) {
        Bulk.p(what, outcome(call));
        compared++;
    }

    private static void begin() {
        compared = 0;
    }

    private static void end(int atLeast) {
        Bulk.p("compared>" + atLeast, compared > atLeast);
    }

    /// Runs the group.
    public static void run() {
        // Past 292 years the nanosecond count overflows a long; the quotient
        // still has to be exact.
        Bulk.check("Time.aLongDurationDividesExactly", () -> {
            Bulk.p("max/max", java.time.Duration.ofSeconds(Long.MAX_VALUE).dividedBy(Long.MAX_VALUE));
            Bulk.p("max/2", java.time.Duration.ofSeconds(Long.MAX_VALUE, 999999999).dividedBy(2));
            Bulk.p("min/3", java.time.Duration.ofSeconds(Long.MIN_VALUE).dividedBy(3));
            Bulk.p("min/-7", java.time.Duration.ofSeconds(Long.MIN_VALUE, 1).dividedBy(-7));
            Bulk.p("max/minLong", java.time.Duration.ofSeconds(Long.MAX_VALUE).dividedBy(Long.MIN_VALUE));
            Bulk.p("big/1e9", java.time.Duration.ofSeconds(123456789012345L, 6789).dividedBy(1000000000L));
            try {
                Bulk.p("min/-1", java.time.Duration.ofSeconds(Long.MIN_VALUE).dividedBy(-1));
            } catch (ArithmeticException e) {
                Bulk.p("min/-1", "ArithmeticException");
            }
        });
        Bulk.check("Time.datesUntilWiderThanAnIntIsLazy", () -> {
            Bulk.p("first", java.time.LocalDate.MIN.datesUntil(java.time.LocalDate.MAX).limit(2)
                    .collect(java.util.stream.Collectors.toList()));
        });
        Bulk.check("Time.datesAreTheJdks", TimeChecks::datesAreTheJdks);
        Bulk.check("Time.datesWithTimesAreTheJdks", TimeChecks::datesWithTimesAreTheJdks);
        Bulk.check("Time.momentsAndZonesAreTheJdks", TimeChecks::momentsAndZonesAreTheJdks);
        Bulk.cn1Only("Time.instantUntilMillisAndMicrosAsJava17", TimeChecks::instantUntilMillisAndMicrosAsJava17);
        Bulk.check("Time.gapsAndOverlapsAreTheJdks", TimeChecks::gapsAndOverlapsAreTheJdks);
        Bulk.check("Time.durationsAreTheJdks", TimeChecks::durationsAreTheJdks);
        Bulk.check("Time.periodsAreTheJdks", TimeChecks::periodsAreTheJdks);
        Bulk.check("Time.offsetsZonesAndClocksAreTheJdks", TimeChecks::offsetsZonesAndClocksAreTheJdks);
        Bulk.check("Time.patternsFormatAndParseAsTheJdks", TimeChecks::patternsFormatAndParseAsTheJdks);
        Bulk.check("Time.isoFormatsAreTheJdks", TimeChecks::isoFormatsAreTheJdks);
        Bulk.check("Time.zonesAndOffsetsAreTheJdks", TimeChecks::zonesAndOffsetsAreTheJdks);
        Bulk.check("Time.daysAndMonthsAreTheJdks", TimeChecks::daysAndMonthsAreTheJdks);
        Bulk.check("Time.amountsThatOverflowAreRefusedAsTheJdkRefusesThem",
                TimeChecks::amountsThatOverflowAreRefusedAsTheJdkRefusesThem);
    }

    private static void datesAreTheJdks() {
        begin();
        for (final LocalDate d : DATES) {
            same("text " + d, () -> d);
            same("dow " + d, d::getDayOfWeek);
            same("month " + d, d::getMonth);
            same("doy " + d, d::getDayOfYear);
            same("loy " + d, d::lengthOfYear);
            same("lom " + d, d::lengthOfMonth);
            same("leap " + d, d::isLeapYear);
            same("epoch day " + d, d::toEpochDay);
            same("hash " + d, d::hashCode);
            same("start " + d, d::atStartOfDay);
            for (final int n : new int[] {0, 1, 2, 12, 13, 28, 29, 30, 31, 32, 60, 365, 366, 367}) {
                same("wdom " + d + n, () -> d.withDayOfMonth(n));
                same("wm " + d + n, () -> d.withMonth(n));
                same("wdoy " + d + n, () -> d.withDayOfYear(n));
                same("wy " + d + n, () -> d.withYear(2000 + n));
            }
            for (final ChronoUnit u : ChronoUnit.values()) {
                for (final long n : AMOUNTS) {
                    if (u.compareTo(ChronoUnit.CENTURIES) >= 0 && (n > 100 || n < -100)) {
                        continue;
                    }
                    same("plus " + d + n + u, () -> d.plus(n, u));
                    same("minus " + d + n + u, () -> d.minus(n, u));
                }
                for (final LocalDate e : DATES) {
                    same("until " + d + e + u, () -> d.until(e, u));
                    same("between " + d + e + u, () -> u.between(d, e));
                }
            }
            for (final LocalDate e : DATES) {
                same("period " + d + e, () -> d.until(e));
                same("compare " + d + e, () -> d.compareTo(e));
                same("before " + d + e, () -> d.isBefore(e));
                same("after " + d + e, () -> d.isAfter(e));
            }
            for (final Period p : new Period[] {Period.of(1, 2, 3), Period.of(0, 13, 31), Period.of(-1, -1, -1),
                Period.ofDays(400)}) {
                same("plus " + d + p, () -> d.plus(p));
                same("minus " + d + p, () -> d.minus(p));
            }
            same("plus duration " + d, () -> d.plus(Duration.ofDays(1)));
        }
        end(15000);
    }

    private static void datesWithTimesAreTheJdks() {
        begin();
        final List<LocalDateTime> all = dateTimes();
        for (final LocalDateTime d : all) {
            same("text " + d, () -> d);
            same("time text " + d, d::toLocalTime);
            same("hash " + d, d::hashCode);
            same("dow " + d, d::getDayOfWeek);
            same("month " + d, d::getMonth);
            same("doy " + d, d::getDayOfYear);
            same("epoch " + d, () -> d.toEpochSecond(ZoneOffset.ofHours(2)));
            for (final long n : new long[] {-100000, -25, -1, 0, 1, 7, 13, 59, 100000, 86400000000001L}) {
                same("md " + d + n, () -> d.minusDays(n % 100000));
                same("mh " + d + n, () -> d.minusHours(n));
                same("mm " + d + n, () -> d.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n));
                same("pn " + d + n, () -> d.plusNanos(n));
                same("mn " + d + n, () -> d.minusNanos(n));
                same("tpn " + d + n, () -> d.toLocalTime().plusNanos(n));
                same("tms " + d + n, () -> d.toLocalTime().minusSeconds(n));
                same("tmh " + d + n, () -> d.toLocalTime().minusHours(n));
                final long few = n % 1000;
                same("pw " + d + n, () -> d.plusWeeks(few));
                same("mw " + d + n, () -> d.minusWeeks(few));
                same("pmo " + d + n, () -> d.plusMonths(few));
                same("mmo " + d + n, () -> d.minusMonths(few));
                same("py " + d + n, () -> d.plusYears(few));
                same("my " + d + n, () -> d.minusYears(few));
            }
            for (final int n : new int[] {0, 1, 23, 24, 59, 60, 999999999}) {
                same("wh " + d + n, () -> d.withHour(n));
                same("wmi " + d + n, () -> d.withMinute(n));
                same("ws " + d + n, () -> d.withSecond(n));
                same("wn " + d + n, () -> d.withNano(n));
            }
            for (final TemporalAmount a : new TemporalAmount[] {Period.of(1, 2, 3), Period.ofMonths(-13),
                Duration.ofSeconds(86399, 999999999), Duration.ofMillis(-1), Duration.ofDays(400)}) {
                same("plus " + d + a, () -> d.plus(a));
                same("minus " + d + a, () -> d.minus(a));
            }
            for (final ChronoUnit u : ChronoUnit.values()) {
                same("trunc " + d + u, () -> d.truncatedTo(u));
                same("time trunc " + d + u, () -> d.toLocalTime().truncatedTo(u));
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
                    same("plus " + d + n + u, () -> d.plus(n, u));
                    same("minus " + d + n + u, () -> d.minus(n, u));
                    same("time plus " + d + n + u, () -> d.toLocalTime().plus(n, u));
                }
                for (final LocalDateTime e : all) {
                    same("until " + d + e + u, () -> d.until(e, u));
                }
            }
            for (final LocalDateTime e : all) {
                same("before " + d + e, () -> d.isBefore(e));
                same("after " + d + e, () -> d.isAfter(e));
                same("equal " + d + e, () -> d.isEqual(e));
                same("compare " + d + e, () -> Integer.signum(d.compareTo(e)));
                same("duration " + d + e, () -> Duration.between(d, e));
                same("time " + d + e, () -> Duration.between(d.toLocalTime(), e.toLocalTime()));
                same("time until " + d + e, () -> d.toLocalTime().until(e.toLocalTime(), ChronoUnit.MINUTES));
                same("time before " + d + e, () -> d.toLocalTime().isBefore(e.toLocalTime()));
            }
        }
        end(50000);
    }

    private static void momentsAndZonesAreTheJdks() {
        begin();
        final List<Instant> all = instants();
        final ZoneId[] zones = new ZoneId[ZONES.length];
        for (int z = 0; z < ZONES.length; z++) {
            zones[z] = ZoneId.of(ZONES[z]);
        }
        for (final Instant i : all) {
            same("text " + i, () -> i);
            same("hash " + i, i::hashCode);
            same("millis " + i, i::toEpochMilli);
            for (final long n : new long[] {-86400000000001L, -1, 0, 1, 999999999, 1000000000, 123456789012L}) {
                same("pn " + i + n, () -> i.plusNanos(n));
                same("mn " + i + n, () -> i.minusNanos(n));
                final Duration d = Duration.ofSeconds(n % 100000, n);
                same("pd " + i + n, () -> i.plus(d));
                same("md " + i + n, () -> i.minus(d));
            }
            same("period " + i, () -> i.plus(Period.ofDays(1)));
            same("months " + i, () -> i.plus(Period.ofMonths(1)));
            for (final ChronoUnit u : ChronoUnit.values()) {
                same("trunc " + i + u, () -> i.truncatedTo(u));
                for (final long n : AMOUNTS) {
                    same("plus " + i + n + u, () -> i.plus(n, u));
                    same("minus " + i + n + u, () -> i.minus(n, u));
                }
                if (u == ChronoUnit.MILLIS || u == ChronoUnit.MICROS) {
                    // Java 21 changed these; see instantUntilMillisAndMicrosAsJava17.
                    continue;
                }
                for (final Instant e : all) {
                    same("until " + i + e + u, () -> i.until(e, u));
                }
            }
            for (final Instant e : all) {
                same("before " + i + e, () -> i.isBefore(e));
                same("after " + i + e, () -> i.isAfter(e));
                same("duration " + i + e, () -> Duration.between(i, e));
            }
            for (final ZoneId zone : zones) {
                if (!regionRulesKnown(i, zone)) {
                    continue;
                }
                same("atZone " + i + zone, () -> i.atZone(zone));
                same("atOffset " + i + zone, () -> i.atOffset(ZoneOffset.ofHoursMinutes(5, 30)));
                final ZonedDateTime z = i.atZone(zone);
                same("date " + z, z::toLocalDate);
                same("time " + z, z::toLocalTime);
                same("epoch " + z, z::toEpochSecond);
                same("offset " + z, z::getOffset);
                same("odt " + z, z::toOffsetDateTime);
                same("hash " + z, z::hashCode);
                same("fields " + z,
                        () -> z.getYear() + "-" + z.getMonthValue() + "-" + z.getDayOfMonth() + " " + z.getHour()
                        + ":" + z.getMinute() + ":" + z.getSecond() + "." + z.getNano() + " " + z.getDayOfYear()
                        + z.getMonth().name() + z.getDayOfWeek().name());
                same("same instant " + z, () -> z.withZoneSameInstant(zones[2]));
                if (regionRulesKnown(i, zones[1])) {
                    same("local at " + z, () -> z.toLocalDateTime().atZone(zones[1]));
                    same("same local " + z, () -> z.withZoneSameLocal(zones[1]));
                }
                for (final long n : new long[] {-400, -1, 0, 1, 25, 400}) {
                    if (!regionRulesKnown(i.minusSeconds(Math.abs(n) * 86400L * 366L), zone)) {
                        continue;
                    }
                    same("pd " + z + n, () -> z.plusDays(n));
                    same("md " + z + n, () -> z.minusDays(n));
                    same("pw " + z + n, () -> z.plusWeeks(n));
                    same("mw " + z + n, () -> z.minusWeeks(n));
                    same("pmo " + z + n, () -> z.plusMonths(n));
                    same("mmo " + z + n, () -> z.minusMonths(n));
                    same("py " + z + n, () -> z.plusYears(n));
                    same("my " + z + n, () -> z.minusYears(n));
                    same("ph " + z + n, () -> z.plusHours(n));
                    same("mh " + z + n, () -> z.minusHours(n));
                    same("pmi " + z + n, () -> z.plusMinutes(n));
                    same("mmi " + z + n, () -> z.minusMinutes(n));
                    same("ps " + z + n, () -> z.plusSeconds(n));
                    same("ms " + z + n, () -> z.minusSeconds(n));
                }
                for (int other = 0; other < all.size(); other += 3) {
                    if (!regionRulesKnown(all.get(other), zones[1])) {
                        continue;
                    }
                    final ZonedDateTime e = all.get(other).atZone(zones[1]);
                    same("before " + z + e, () -> z.isBefore(e));
                    same("after " + z + e, () -> z.isAfter(e));
                    same("equal " + z + e, () -> z.isEqual(e));
                    same("compare " + z + e, () -> Integer.signum(z.compareTo(e)));
                    same("duration " + z + e, () -> Duration.between(z, e));
                    for (final ChronoUnit u : ChronoUnit.values()) {
                        same("until " + z + e + u, () -> z.until(e, u));
                    }
                }
            }
        }
        end(50000);
    }

    /// `Instant.until` in milliseconds and microseconds. Through Java 17 the
    /// JDK counts milliseconds between the two moments each cut to its
    /// millisecond, and microseconds from a count of nanoseconds that
    /// overflows past 292 years; Java 21 truncates the exact difference and
    /// counts microseconds without the overflow. JavaAPI answers as Java 8
    /// through 17 do, so the HotSpot reference cannot be compared with; this
    /// holds the grid to that definition, and to values taken from Java 8.
    private static void instantUntilMillisAndMicrosAsJava17() {
        final List<Instant> all = instants();
        for (final Instant i : all) {
            for (final Instant e : all) {
                String millis = outcome(() -> Math.subtractExact(e.toEpochMilli(), i.toEpochMilli()));
                String micros = outcome(() -> Math.addExact(Math.multiplyExact(
                        Math.subtractExact(e.getEpochSecond(), i.getEpochSecond()), 1000000000L),
                        (long) (e.getNano() - i.getNano())) / 1000);
                Bulk.expectEquals(millis, outcome(() -> i.until(e, ChronoUnit.MILLIS)), "millis " + i + " " + e);
                Bulk.expectEquals(micros, outcome(() -> i.until(e, ChronoUnit.MICROS)), "micros " + i + " " + e);
            }
        }
        final Instant a = Instant.parse("1600-02-29T00:00:00.000000001Z");
        final Instant b = Instant.parse("1600-02-29T12:34:56.789Z");
        final Instant c = Instant.parse("1969-12-31T00:00:00Z");
        Bulk.expectEquals("45296789", outcome(() -> a.until(b, ChronoUnit.MILLIS)), "Java 8 millis, short");
        Bulk.expectEquals("11670912000000", outcome(() -> a.until(c, ChronoUnit.MILLIS)), "Java 8 millis, long");
        Bulk.expectEquals("ArithmeticException", outcome(() -> a.until(c, ChronoUnit.MICROS)), "Java 8 micros");
    }

    /// The wall clock times a zone skips or passes twice, resolved as the JDK
    /// resolves them.
    private static void gapsAndOverlapsAreTheJdks() {
        begin();
        final ZoneId ny = ZoneId.of("America/New_York");
        for (final LocalDateTime local : new LocalDateTime[] {LocalDateTime.of(2024, 3, 10, 2, 0),
            LocalDateTime.of(2024, 3, 10, 2, 30), LocalDateTime.of(2024, 3, 10, 3, 0),
            LocalDateTime.of(2024, 11, 3, 0, 59), LocalDateTime.of(2024, 11, 3, 1, 0),
            LocalDateTime.of(2024, 11, 3, 1, 30), LocalDateTime.of(2024, 11, 3, 2, 0)}) {
            same("of " + local, () -> ZonedDateTime.of(local, ny));
            for (final int off : new int[] {-4, -5, 3}) {
                same("ofLocal " + local + off, () -> ZonedDateTime.ofLocal(local, ny, ZoneOffset.ofHours(off)));
            }
            final ZonedDateTime z = ZonedDateTime.of(local, ny);
            same("earlier " + z, z::withEarlierOffsetAtOverlap);
            same("later " + z, z::withLaterOffsetAtOverlap);
            same("later earlier " + z, () -> z.withLaterOffsetAtOverlap().withEarlierOffsetAtOverlap());
            for (final long n : new long[] {-2, -1, 1, 2, 24}) {
                same("ph " + z + n, () -> z.plusHours(n));
                same("pd " + z + n, () -> z.plusDays(n));
                same("pmi " + z + n, () -> z.plusMinutes(n * 30));
            }
        }
    }

    private static void durationsAreTheJdks() {
        begin();
        final Duration[] all = {Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(-1), Duration.ofMillis(1500),
            Duration.ofMillis(-1500), Duration.ofSeconds(59), Duration.ofSeconds(3600), Duration.ofSeconds(86399, 5),
            Duration.ofSeconds(-86401, 999999999), Duration.ofDays(400), Duration.ofDays(-400),
            Duration.ofSeconds(90061, 123456789)};
        same("zero", () -> Duration.ZERO);
        for (final Duration d : all) {
            same("text " + d, () -> d);
            same("hash " + d, d::hashCode);
            same("days " + d, d::toDays);
            same("hours " + d, d::toHours);
            same("minutes " + d, d::toMinutes);
            same("millis " + d, d::toMillis);
            same("seconds " + d, d::getSeconds);
            same("nanos " + d, d::toNanos);
            same("zero " + d, d::isZero);
            same("negative " + d, d::isNegative);
            same("negated " + d, d::negated);
            same("abs " + d, d::abs);
            same("hours part " + d, d::toHoursPart);
            same("minutes part " + d, d::toMinutesPart);
            same("seconds part " + d, d::toSecondsPart);
            same("millis part " + d, d::toMillisPart);
            for (final long n : new long[] {-100000, -7, -1, 0, 1, 3, 1000, 123456789}) {
                same("pd " + d + n, () -> d.plusDays(n));
                same("ph " + d + n, () -> d.plusHours(n));
                same("pmi " + d + n, () -> d.plusMinutes(n));
                same("ps " + d + n, () -> d.plusSeconds(n));
                same("pms " + d + n, () -> d.plusMillis(n));
                same("pn " + d + n, () -> d.plusNanos(n));
                same("md " + d + n, () -> d.minusDays(n));
                same("mh " + d + n, () -> d.minusHours(n));
                same("mmi " + d + n, () -> d.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n));
                same("mms " + d + n, () -> d.minusMillis(n));
                same("mn " + d + n, () -> d.minusNanos(n));
                same("times " + d + n, () -> d.multipliedBy(n % 1000));
                same("over " + d + n, () -> d.dividedBy(n));
                same("ofNanos " + n, () -> Duration.ofNanos(n * 1000003));
            }
            for (final Duration e : all) {
                same("plus " + d + e, () -> d.plus(e));
                same("minus " + d + e, () -> d.minus(e));
                same("compare " + d + e, () -> Integer.signum(d.compareTo(e)));
            }
        }
        // Past what a long counts in nanoseconds, and at its very edges:
        // the JDK throws rather than wrapping.
        final Duration[] extremes = {Duration.ofDays(365L * 300), Duration.ofDays(-365L * 300),
            Duration.ofNanos(Long.MAX_VALUE), Duration.ofNanos(Long.MIN_VALUE).plusNanos(-1),
            Duration.ofNanos(Long.MAX_VALUE).plusNanos(1), Duration.ofSeconds(-1, 999999999), Duration.ofSeconds(-1, 1)};
        // The most negative long: later releases than Java 8 borrow a second
        // first and answer it, as JavaAPI does.
        same("min nanos", () -> Duration.ofNanos(Long.MIN_VALUE).toNanos());
        for (final Duration d : new Duration[] {Duration.ZERO, Duration.ofSeconds(-5, 1), Duration.ofSeconds(5),
            Duration.ofSeconds(Long.MIN_VALUE), Duration.ofSeconds(Long.MIN_VALUE, 1),
            Duration.ofSeconds(Long.MAX_VALUE, 999999999)}) {
            same("negated " + d, d::negated);
            for (final long n : new long[] {Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE / 60, -1}) {
                same("pd " + d + n, () -> d.plusDays(n));
                same("ph " + d + n, () -> d.plusHours(n));
                same("pmi " + d + n, () -> d.plusMinutes(n));
                same("ps " + d + n, () -> d.plusSeconds(n));
                same("pms " + d + n, () -> d.plusMillis(n));
                same("pn " + d + n, () -> d.plusNanos(n));
                same("md " + d + n, () -> d.minusDays(n));
                same("mh " + d + n, () -> d.minusHours(n));
                same("mmi " + d + n, () -> d.minusMinutes(n));
                same("ms " + d + n, () -> d.minusSeconds(n));
                same("mms " + d + n, () -> d.minusMillis(n));
                same("mn " + d + n, () -> d.minusNanos(n));
            }
        }
        for (final Duration d : extremes) {
            same("nanos " + d, d::toNanos);
            for (final long n : new long[] {-3, 2, 1000000007L, -999999999999L, Long.MAX_VALUE, Long.MIN_VALUE}) {
                same("times " + d + n, () -> d.multipliedBy(n));
            }
        }
        for (final ChronoUnit u : ChronoUnit.values()) {
            same("name " + u, u::toString);
            same("length " + u, u::getDuration);
            same("estimated " + u, u::isDurationEstimated);
            same("date " + u, u::isDateBased);
            same("time " + u, u::isTimeBased);
            for (final long n : AMOUNTS) {
                same("of " + n + u, () -> Duration.of(n, u));
            }
        }
        same("units", () -> ChronoUnit.values().length);
        for (final String text : new String[] {"PT0S", "PT1.5S", "P2DT3H4M", "PT-0.5S", "-PT6H3M", "PT25H", "nonsense",
            "P1Y", "P106751991167301D", "PT2562047788015216H", "PT153722867280912931M",
            "PT9223372036854775807S", "-PT9223372036854775808S", "P106751991167300DT24H"}) {
            same("parse " + text, () -> Duration.parse(text));
        }
        end(1500);
    }

    private static void periodsAreTheJdks() {
        begin();
        final Period[] all = {Period.ZERO, Period.of(1, 2, 3), Period.of(0, 13, 31), Period.of(-1, -1, -1),
            Period.ofDays(400), Period.ofWeeks(3), Period.of(2, -25, 0), Period.ofYears(Integer.MAX_VALUE)};
        for (final Period p : all) {
            same("text " + p, () -> p);
            same("hash " + p, p::hashCode);
            same("zero " + p, p::isZero);
            same("negative " + p, p::isNegative);
            same("months " + p, p::toTotalMonths);
            same("normalized " + p, p::normalized);
            same("negated " + p, p::negated);
            for (final int n : new int[] {-1, 0, 2, Integer.MAX_VALUE}) {
                same("times " + p + n, () -> p.multipliedBy(n));
                same("py " + p + n, () -> p.plusYears(n));
                same("pm " + p + n, () -> p.plusMonths(n));
                same("pd " + p + n, () -> p.plusDays(n));
            }
            for (final Period q : all) {
                same("plus " + p + q, () -> p.plus(q));
                same("minus " + p + q, () -> p.minus(q));
                same("equals " + p + q, () -> p.equals(q));
            }
        }
        for (final LocalDate a : DATES) {
            for (final LocalDate b : DATES) {
                same("between " + a + b, () -> Period.between(a, b));
            }
        }
        for (final String text : new String[] {"P1Y2M3D", "P2W", "p1y2w3d", "-P1Y-2M", "+P-1D", "P1W-3D", "PT1S",
            "P", "P1Y1Y", "P1M1Y", "P1", "P2147483647W", "P99999999999D", "1Y", "", "P-0D", "P+5Y"}) {
            same("parse " + text, () -> Period.parse(text));
        }
        for (final TemporalAmount a : new TemporalAmount[] {Period.ofDays(3), Period.ofMonths(1),
            Duration.ofSeconds(5, 7)}) {
            same("duration from " + a, () -> Duration.from(a));
            same("period from " + a, () -> Period.from(a));
        }
    }

    private static void offsetsZonesAndClocksAreTheJdks() {
        begin();
        same("min", () -> OffsetDateTime.MIN);
        same("max", () -> OffsetDateTime.MAX);
        same("instant min", () -> Instant.MIN);
        same("instant max", () -> Instant.MAX);
        for (final LocalDateTime local : dateTimes()) {
            if (local.getYear() < 1901) {
                continue;
            }
            final OffsetDateTime o = local.atOffset(ZoneOffset.ofHours(3));
            final ZonedDateTime z = local.atZone(ZoneId.of("America/New_York"));
            same("fixed " + z, z::withFixedOffsetZone);
            for (final int n : new int[] {0, 1, 12, 28, 29, 31, 59, 60, 365, 366}) {
                same("y " + o + n, () -> o.withYear(2000 + n));
                same("mo " + o + n, () -> o.withMonth(n));
                same("dom " + o + n, () -> o.withDayOfMonth(n));
                same("doy " + o + n, () -> o.withDayOfYear(n));
                same("h " + o + n, () -> o.withHour(n));
                same("mi " + o + n, () -> o.withMinute(n));
                same("s " + o + n, () -> o.withSecond(n));
                same("n " + o + n, () -> o.withNano(n));
                same("zoned doy " + z + n, () -> z.withDayOfYear(n));
            }
        }
        final Instant at = Instant.ofEpochSecond(1700000000L, 5);
        final Clock fixed = Clock.fixed(at, ZoneOffset.UTC);
        final ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        same("clock zone", () -> fixed.withZone(kolkata).getZone());
        same("clock zoned", () -> ZonedDateTime.now(fixed.withZone(kolkata)));
        same("clock offset", () -> Clock.offset(fixed, Duration.ofHours(-5)).instant());
        same("clock equals", () -> fixed.equals(Clock.fixed(at, ZoneOffset.UTC)) + " "
                + fixed.withZone(ZoneOffset.UTC).equals(fixed) + " " + fixed.equals(fixed.withZone(kolkata)));
    }

    /// Patterns over a date alone, a time alone, a date with a time, and a
    /// zoned date and time; and what a pattern reads back.
    private static void patternsFormatAndParseAsTheJdks() {
        begin();
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
            final DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern, Locale.US);
            for (final LocalTime t : times) {
                same("time " + pattern + t, () -> t.format(f));
                same("time of " + pattern + t, () -> f.format(t));
            }
            for (final LocalDate d : dates) {
                same("date " + pattern + d, () -> d.format(f));
                same("both " + pattern + d, () -> d.atTime(4, 5, 6).format(f));
                for (final ZoneId z : zones) {
                    final ZonedDateTime zd = d.atTime(4, 5, 6).atZone(z);
                    if (!regionRulesKnown(zd.toInstant(), z)) {
                        continue;
                    }
                    same("zoned " + pattern + zd, () -> zd.format(f));
                    same("offset " + pattern + zd, () -> zd.toOffsetDateTime().format(f));
                }
            }
            for (final LocalDate d : dates) {
                final String text = d.atTime(4, 5, 6).atZone(ZoneOffset.ofHours(2)).format(f);
                same("read date " + pattern + text, () -> LocalDate.parse(text, f));
                same("read time " + pattern + text, () -> LocalTime.parse(text, f));
                same("read both " + pattern + text, () -> LocalDateTime.parse(text, f));
            }
        }
        final DateTimeFormatter day = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        final DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (final String text : new String[] {"21.02.1999", "29.02.2024", "30.02.2024", "31.04.2023", "32.01.2024",
            "31.12.2031", "1.1.2024", "nonsense", "12:13:14", "24:00:00", "21.02.1999 ", ""}) {
            same("parse date " + text, () -> LocalDate.parse(text, day));
            same("parse time " + text, () -> LocalTime.parse(text, clock));
        }
        for (final String bad : new String[] {"yyyy-MM-dd ]", "'open", "{y}", "ddd", "aa", "V", "OO", "SSSSSSSSSS",
            "b"}) {
            same("refused " + bad, () -> DateTimeFormatter.ofPattern(bad) == null ? "null" : "made");
        }
        end(1500);
    }

    private static void isoFormatsAreTheJdks() {
        begin();
        final List<ZonedDateTime> values = new ArrayList<ZonedDateTime>();
        for (final LocalDateTime local : dateTimes()) {
            values.add(local.atZone(ZoneOffset.UTC));
            values.add(local.atZone(ZoneOffset.ofHoursMinutes(-3, -30)));
            values.add(local.atZone(ZoneId.of("UTC")));
            if (local.getYear() >= 1901) {
                values.add(local.atZone(ZoneId.of("Asia/Kolkata")));
            }
        }
        final DateTimeFormatter[] formats = {DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME, DateTimeFormatter.ISO_INSTANT};
        for (final ZonedDateTime z : values) {
            same("zoned text " + z, () -> z);
            same("offset text " + z, z::toOffsetDateTime);
            same("instant text " + z, z::toInstant);
            for (int f = 0; f < formats.length; f++) {
                final DateTimeFormatter j = formats[f];
                same("zoned " + f + z, () -> j.format(z));
                same("offset " + f + z, () -> j.format(z.toOffsetDateTime()));
                same("local " + f + z, () -> j.format(z.toLocalDateTime()));
                same("date " + f + z, () -> j.format(z.toLocalDate()));
                same("time " + f + z, () -> j.format(z.toLocalTime()));
                same("instant " + f + z, () -> j.format(z.toInstant()));
                if (regionRulesKnown(z.toInstant(), ZoneId.of("Asia/Kolkata"))) {
                    same("in zone " + f + z, () -> j.withZone(ZoneId.of("Asia/Kolkata")).format(z.toInstant()));
                }
                final String text = outcome(() -> j.format(z));
                same("reread zoned " + f + text, () -> ZonedDateTime.parse(text, j));
                same("reread offset " + f + text, () -> OffsetDateTime.parse(text, j));
                same("reread local " + f + text, () -> LocalDateTime.parse(text, j));
                same("reread date " + f + text, () -> LocalDate.parse(text, j));
                same("reread time " + f + text, () -> LocalTime.parse(text, j));
                same("reread with zone " + f + text,
                        () -> ZonedDateTime.parse(text, j.withZone(ZoneId.of("America/New_York"))));
            }
        }
        for (final String text : new String[] {"2024-02-29", "2023-02-29", "+12345-01-02", "12345-01-02", "-0005-06-07",
            "2024-1-01", "12:30", "12:30:45.5", "24:00", "2024-02-29T12:30", "2024-02-29t12:30:00Z",
            "2024-02-29T12:30:00+05:30", "2024-02-29T12:30:00-03:30[America/New_York]", "2024-02-29T12:30:00Z[UTC]",
            "2024-02-29T23:59:60Z", "2024-02-29T24:00:00Z", "2024-02-29T12:30:00.123456Z"}) {
            same("date " + text, () -> LocalDate.parse(text));
            same("time " + text, () -> LocalTime.parse(text));
            same("local " + text, () -> LocalDateTime.parse(text));
            same("offset " + text, () -> OffsetDateTime.parse(text));
            same("zoned " + text, () -> ZonedDateTime.parse(text));
            same("instant " + text, () -> Instant.parse(text));
        }
        end(5000);
    }

    private static void zonesAndOffsetsAreTheJdks() {
        begin();
        for (final String id : new String[] {"Z", "UTC", "GMT", "UT", "UTC+01:00", "GMT-5", "UT+0", "+05:30",
            "-08", "+1", "+0530", "+05:30:15", "+18:00", "+18:01", "-19", "America/New_York", "Bad Zone", "",
            "UTC+", "GMT0"}) {
            same("zone " + id, () -> ZoneId.of(id));
            same("zone class " + id, () -> ZoneId.of(id) instanceof ZoneOffset);
            same("normalized " + id, () -> ZoneId.of(id).normalized());
            same("offset " + id, () -> ZoneOffset.of(id));
            same("utc equals " + id, () -> ZoneOffset.UTC.equals(ZoneId.of(id)));
        }
        for (final int seconds : new int[] {0, 1, -1, 3600, -3600, 19800, 64800, 64801, -64800, 45296}) {
            same("total " + seconds, () -> ZoneOffset.ofTotalSeconds(seconds));
        }
        for (final int[] hms : new int[][] {{1, 30, 0}, {-1, -30, 0}, {1, -30, 0}, {0, 30, -1}, {0, -30, -15},
            {18, 0, 0}, {18, 1, 0}, {5, 60, 0}}) {
            same("hms " + hms[0] + hms[1] + hms[2], () -> ZoneOffset.ofHoursMinutesSeconds(hms[0], hms[1], hms[2]));
        }
    }

    private static void daysAndMonthsAreTheJdks() {
        begin();
        for (final DayOfWeek day : DayOfWeek.values()) {
            same("value " + day, day::getValue);
            same("text " + day, day::toString);
            for (final long n : new long[] {-15, -7, -1, 0, 1, 6, 7, 100, Long.MIN_VALUE, Long.MAX_VALUE}) {
                same("plus " + day + n, () -> day.plus(n));
                same("minus " + day + n, () -> day.minus(n));
            }
        }
        for (final Month month : Month.values()) {
            same("value " + month, month::getValue);
            same("min " + month, month::minLength);
            same("max " + month, month::maxLength);
            same("quarter " + month, month::firstMonthOfQuarter);
            for (final boolean leap : new boolean[] {false, true}) {
                same("length " + month, () -> month.length(leap));
                same("first " + month, () -> month.firstDayOfYear(leap));
            }
            for (final long n : new long[] {-25, -12, -1, 0, 1, 11, 12, 100, Long.MIN_VALUE, Long.MAX_VALUE}) {
                same("plus " + month + n, () -> month.plus(n));
                same("minus " + month + n, () -> month.minus(n));
            }
        }
        for (final int n : new int[] {-1, 0, 1, 7, 8, 12, 13}) {
            same("day of " + n, () -> DayOfWeek.of(n));
            same("month of " + n, () -> Month.of(n));
            same("date of " + n, () -> LocalDate.of(2024, Month.of(n), 5));
        }
        end(500);
    }

    /// Amounts too large for the seconds they stand for: the JDK refuses
    /// every one of them rather than wrapping around.
    private static void amountsThatOverflowAreRefusedAsTheJdkRefusesThem() {
        begin();
        final long[] huge = {4611686018427387904L, -4611686018427387904L, Long.MAX_VALUE, Long.MIN_VALUE,
            Long.MAX_VALUE / 60 + 1, Long.MAX_VALUE / 3600 + 1, 153722867280912930L, 2562047788015215L,
            31556889864403199L, -31557014167219200L, 1000000L * 365 * 24 * 3600};
        for (final long n : huge) {
            for (final ChronoUnit u : ChronoUnit.values()) {
                same("of " + n + u, () -> Duration.of(n, u));
            }
            for (final String id : ZONES) {
                final ZonedDateTime z = Instant.ofEpochSecond(1700000000L, 5).atZone(ZoneId.of(id));
                same("pmi " + z + n, () -> z.plusMinutes(n));
                same("mmi " + z + n, () -> z.minusMinutes(n));
                same("ph " + z + n, () -> z.plusHours(n));
                same("mh " + z + n, () -> z.minusHours(n));
                same("ps " + z + n, () -> z.plusSeconds(n));
                same("ms " + z + n, () -> z.minusSeconds(n));
            }
            final Instant i = Instant.ofEpochSecond(1700000000L, 5);
            same("instant ps " + n, () -> i.plusSeconds(n));
            same("instant pms " + n, () -> i.plusMillis(n));
            same("instant of " + n, () -> Instant.ofEpochSecond(n));
            same("instant ofMilli " + n, () -> Instant.ofEpochMilli(n));
            same("epoch day " + n, () -> LocalDate.ofEpochDay(n));
        }
    }
}
