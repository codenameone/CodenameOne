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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TimeEdgeApp {
    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String localDateTimeString(LocalDateTime value) {
        return value.getYear() + "-" + two(value.getMonthValue()) + "-" + two(value.getDayOfMonth())
                + "T" + two(value.getHour()) + ":" + two(value.getMinute()) + ":" + two(value.getSecond());
    }

    private static String zonedString(ZonedDateTime value) {
        return localDateTimeString(value.toLocalDateTime()) + value.getOffset().getId() + "[" + value.getZone().getId() + "]";
    }

    private static String calculate() {
        LocalDate leap2000 = LocalDate.of(2000, 2, 29);
        LocalDate century1900 = LocalDate.of(1900, 2, 28).plusDays(1);
        LocalDate century2100 = LocalDate.of(2100, 2, 28).plusDays(1);
        LocalDate leap2004 = LocalDate.of(2004, 2, 29).plusYears(1);
        LocalDate rollover = LocalDate.of(2020, 1, 31).plusMonths(1);

        Instant baseInstant = Instant.parse("2020-03-08T06:30:00Z");
        ZonedDateTime nyBeforeGap = ZonedDateTime.ofInstant(baseInstant, ZoneId.of("America/New_York"));
        ZonedDateTime nyAfterGap = ZonedDateTime.ofInstant(Instant.parse("2020-03-08T07:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime nyOverlapEarly = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T05:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime nyOverlapLate = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T06:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime berlinSummer = ZonedDateTime.ofInstant(Instant.parse("2020-06-01T10:15:30Z"), ZoneId.of("Europe/Berlin"));

        Duration duration = Duration.ofMillis(90061).plus(Duration.ofSeconds(5));
        String durationParseError;
        try {
            Duration.parse("invalid");
            durationParseError = "missing";
        } catch (DateTimeParseException ex) {
            durationParseError = ex.getParsedString() + ":" + ex.getErrorIndex();
        }
        Period period = Period.of(1, 1, 1);
        LocalDate periodTarget = LocalDate.of(2019, 1, 31).plusYears(period.getYears()).plusMonths(period.getMonths()).plusDays(period.getDays());
        LocalDateTime utcLocal = LocalDateTime.ofInstant(baseInstant, ZoneOffset.UTC);

        StringBuilder result = new StringBuilder();
        result.append(leap2000).append('|');
        result.append(century1900).append('|');
        result.append(century2100).append('|');
        result.append(leap2004).append('|');
        result.append(rollover).append('|');
        result.append(zonedString(nyBeforeGap)).append('|');
        result.append(zonedString(nyAfterGap)).append('|');
        result.append(zonedString(nyOverlapEarly)).append('|');
        result.append(zonedString(nyOverlapLate)).append('|');
        result.append(zonedString(berlinSummer)).append('|');
        result.append(duration.toMillis()).append('|');
        result.append(periodTarget).append('|');
        result.append(LocalTime.of(23, 59, 59).plusSeconds(2)).append('|');
        result.append(localDateTimeString(utcLocal)).append('|');
        result.append(durationParseError).append('|');
        result.append(Duration.ofMillis(500)).append('|');
        result.append(Duration.ofMillis(-500)).append('|');
        result.append(Duration.parse(Duration.ofMillis(500).toString()).toMillis()).append('|');
        result.append(Duration.parse(Duration.ofMillis(-500).toString()).toMillis()).append('|');
        result.append(Duration.ofMinutes(15)).append('|');
        result.append(Duration.ofSeconds(7384)).append('|');
        result.append(Duration.ofSeconds(-7384)).append('|');
        result.append(Duration.ofSeconds(-60, 1)).append('|');
        result.append(Duration.parse("P1DT2H3M4.5S").toMillis()).append('|');
        result.append(Duration.parse("-P1DT1H1M").toMillis());
        result.append('|').append(newerApi());
        return result.toString();
    }

    /// The enums, units, formatter and java.util.Date members, through the
    /// translator: what a device runs, against what the JDK answers.
    private static String newerApi() {
        StringBuilder out = new StringBuilder();
        out.append(DayOfWeek.MONDAY.plus(-15)).append(',').append(Month.JANUARY.minus(25)).append(',')
                .append(Month.FEBRUARY.length(true)).append(',').append(Month.of(11).firstMonthOfQuarter())
                .append(',').append(LocalDate.of(2024, 2, 29).getDayOfWeek()).append('|');
        LocalDateTime a = LocalDateTime.of(2020, 1, 31, 23, 59, 59, 999999999);
        LocalDateTime b = LocalDateTime.of(2024, 2, 29, 0, 0);
        for (ChronoUnit u : ChronoUnit.values()) {
            if (u == ChronoUnit.FOREVER) {
                continue;
            }
            out.append(u).append('=').append(a.until(b, u)).append(',');
        }
        out.append(a.truncatedTo(ChronoUnit.HOURS)).append(',').append(b.plus(3, ChronoUnit.DECADES)).append('|');
        ZoneId ny = ZoneId.of("America/New_York");
        out.append(ZonedDateTime.of(LocalDateTime.of(2024, 3, 10, 2, 30), ny)).append(',')
                .append(ZonedDateTime.of(LocalDateTime.of(2024, 11, 3, 1, 30), ny).withLaterOffsetAtOverlap())
                .append(',').append(ZonedDateTime.of(LocalDateTime.of(2024, 3, 9, 2, 30), ny).plusDays(1))
                .append(',').append(ZonedDateTime.of(LocalDateTime.of(2024, 3, 9, 2, 30), ny).plusHours(24))
                .append('|');
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu 'at' hh:mm:ss.SSS a");
        out.append(LocalDateTime.of(2031, 12, 31, 13, 5, 7, 89000000).format(pattern)).append(',')
                .append(LocalDate.parse("29.02.2024", DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append(',')
                .append(LocalTime.parse("24:00:00", DateTimeFormatter.ofPattern("HH:mm:ss"))).append(',')
                .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        OffsetDateTime.of(2024, 2, 29, 12, 30, 0, 120000000, ZoneOffset.ofHoursMinutes(-3, -30))))
                .append(',').append(ZonedDateTime.parse("2024-06-01T12:00:00+02:00[Europe/Berlin]")).append(',')
                .append(Instant.ofEpochSecond(31556889864403199L, 999999999)).append(',')
                .append(Period.between(LocalDate.of(2020, 1, 31), LocalDate.of(2024, 2, 29))).append(',');
        try {
            LocalDate.parse("31.04.2023", DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            out.append("parsed");
        } catch (DateTimeParseException ex) {
            out.append("refused:").append(ex.getErrorIndex());
        }
        out.append('|');
        // Noon UTC in mid month: the same date in every zone the host may
        // have, so the default zone cannot change the answer.
        Date date = Date.from(Instant.parse("2024-02-14T12:00:00Z"));
        Date later = new Date(date.getTime() + 1);
        out.append(date.toInstant()).append(',').append(date.getYear()).append(',').append(date.getMonth())
                .append(',').append(date.getDate()).append(',').append(date.getDay()).append(',')
                .append(later.after(date)).append(',').append(later.before(date)).append(',');
        try {
            Date.from(Instant.MAX);
            out.append("made");
        } catch (IllegalArgumentException ex) {
            out.append("refused");
        }
        return out.toString();
    }

    public static void main(String[] args) {
        System.out.println("RESULT=" + calculate());
    }
}
