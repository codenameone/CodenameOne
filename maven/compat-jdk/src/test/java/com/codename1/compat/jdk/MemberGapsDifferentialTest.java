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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the single members added beside the streams -- on `String`,
/// `Optional` and the `java.time` classes -- against the JDK's. Those the
/// JDK this suite runs on does not have yet are held to their documentation.
public class MemberGapsDifferentialTest {

    // ---- String.split ----

    private static final String[] INPUTS = {"", "a", "a,b,c", ",a,,b,", ",,,", "a, b ,c", "one  two   three",
        "key=value=more", "a.b.c", "a|b|c", "x1y22z333", "  lead", "trail  ", "a\tb\nc d", "abc", "aXbxc",
        "1+2-3*4", "a$b$c", "\\a\\b", "a^b", "boo:and:foo", "a::b", "(a)(b)", "a[1]b[2]"};

    private static final String[] REGEXES = {",", ", *", "\\s+", " ", "=", "\\.", "\\|", "[0-9]+", "\\d", "\\t",
        "", "b", "[xX]", "[+*-]", "\\$", "\\\\", "\\^", ":", "o", "::", "[()]", "\\[|\\]", "a|c", ",|=", "\\s*,\\s*",
        "$", "z"};

    @Test
    public void splitMatchesTheJdkOverEveryInputAndPattern() {
        for (String input : INPUTS) {
            for (String regex : REGEXES) {
                String at = "\"" + input + "\".split(\"" + regex + "\")";
                assertEquals(at, Arrays.asList(input.split(regex)), Arrays.asList(JdkStrings.split(input, regex)));
            }
        }
    }

    @Test
    public void splitWithALimitMatchesTheJdk() {
        int[] limits = {-1, 0, 1, 2, 3, 5, 100};
        for (String input : INPUTS) {
            for (String regex : REGEXES) {
                for (int limit : limits) {
                    String at = "\"" + input + "\".split(\"" + regex + "\", " + limit + ")";
                    assertEquals(at, Arrays.asList(input.split(regex, limit)),
                            Arrays.asList(JdkStrings.split(input, regex, limit)));
                }
            }
        }
    }

    @Test
    public void splitRefusesNull() {
        try {
            JdkStrings.split("a", null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
        try {
            JdkStrings.split(null, ",");
            fail();
        } catch (NullPointerException expected) {
            // As a call on null would.
        }
    }

    // ---- the later String methods ----

    @Test
    public void isBlank() {
        assertTrue(JdkStrings.isBlank(""));
        assertTrue(JdkStrings.isBlank(" \t\n\r\f"));
        assertTrue(JdkStrings.isBlank("\u2003"));
        assertFalse(JdkStrings.isBlank(" a "));
        // A no-break space is not white space to Character.isWhitespace.
        assertFalse(JdkStrings.isBlank("\u00a0"));
    }

    @Test
    public void strip() {
        assertEquals("a b", JdkStrings.strip("  a b\t\n"));
        assertEquals("a b\t\n", JdkStrings.stripLeading("  a b\t\n"));
        assertEquals("  a b", JdkStrings.stripTrailing("  a b\t\n"));
        assertEquals("", JdkStrings.strip(" \t "));
        assertEquals("", JdkStrings.stripLeading(" \t "));
        assertEquals("", JdkStrings.stripTrailing(" \t "));
        assertEquals("", JdkStrings.strip(""));
        // Unlike trim, strip knows the wide spaces and leaves control
        // characters that are not white space.
        assertEquals("x", JdkStrings.strip("\u2003x\u2003"));
        assertEquals("\u0001x", JdkStrings.strip(" \u0001x "));
        String untouched = "abc";
        assertSame(untouched, JdkStrings.strip(untouched));
    }

    @Test
    public void repeat() {
        assertEquals("", JdkStrings.repeat("ab", 0));
        assertEquals("ab", JdkStrings.repeat("ab", 1));
        assertEquals("ababab", JdkStrings.repeat("ab", 3));
        assertEquals("", JdkStrings.repeat("", 1000));
        assertEquals(4096, JdkStrings.repeat("x", 4096).length());
        try {
            JdkStrings.repeat("ab", -1);
            fail();
        } catch (IllegalArgumentException expected) {
            // As documented.
        }
    }

    @Test
    public void lines() {
        assertEquals(Arrays.asList("a", "b", "", "c", "d"),
                JdkStrings.lines("a\nb\r\n\rc\nd").collect(Collectors.toList()));
        // A final terminator ends the last line; it does not start another.
        assertEquals(Arrays.asList("a", "b"), JdkStrings.lines("a\nb\n").collect(Collectors.toList()));
        assertEquals(Arrays.asList("a", "b"), JdkStrings.lines("a\r\nb\r\n").collect(Collectors.toList()));
        assertEquals(Arrays.asList("a", ""), JdkStrings.lines("a\n\n").collect(Collectors.toList()));
        assertEquals(Arrays.asList(""), JdkStrings.lines("\n").collect(Collectors.toList()));
        assertEquals(Arrays.asList("", ""), JdkStrings.lines("\r\r").collect(Collectors.toList()));
        assertTrue(JdkStrings.lines("").collect(Collectors.toList()).isEmpty());
        assertEquals(Arrays.asList("solo"), JdkStrings.lines("solo").collect(Collectors.toList()));
    }

    @Test
    public void formatted() {
        assertEquals(String.format("%s has %d", "list", 3), JdkStrings.formatted("%s has %d", "list", 3));
        assertEquals(String.format("plain"), JdkStrings.formatted("plain"));
        assertEquals(String.format("%5.2f|%-4s|%04d", 3.14159, "ab", 42),
                JdkStrings.formatted("%5.2f|%-4s|%04d", 3.14159, "ab", 42));
    }

    @Test
    public void chars() {
        String[] samples = {"", "a", "hello", "h\u00e9llo \ud83d\ude00"};
        for (String sample : samples) {
            assertArrayEquals(sample.chars().toArray(), JdkStrings.chars(sample).toArray());
            assertArrayEquals(new StringBuilder(sample).chars().toArray(),
                    JdkStrings.chars(new StringBuilder(sample)).toArray());
        }
        assertEquals("hello".chars().filter(c -> c == 'l').count(),
                JdkStrings.chars("hello").filter(c -> c == 'l').count());
        // Bound when the stream runs, as the JDK's is.
        StringBuilder growing = new StringBuilder("ab");
        java.util.stream.IntStream theirs = growing.chars();
        IntStream mine = JdkStrings.chars(growing);
        growing.append("cd");
        assertArrayEquals(theirs.toArray(), mine.toArray());
        try {
            JdkStrings.chars(null);
            fail();
        } catch (NullPointerException expected) {
            // As a call on null would.
        }
    }

    // ---- Optional ----

    @Test
    public void optionalIsEmptyAndOrElseThrow() {
        assertTrue(Optional.empty().isEmpty());
        assertFalse(Optional.of("a").isEmpty());
        assertEquals("a", Optional.of("a").orElseThrow());
        try {
            Optional.empty().orElseThrow();
            fail();
        } catch (NoSuchElementException expected) {
            assertEquals("No value present", expected.getMessage());
        }
    }

    @Test
    public void optionalIfPresentOrElse() {
        List<String> ran = new ArrayList<String>();
        Optional.of("a").ifPresentOrElse(v -> ran.add("value " + v), () -> ran.add("empty"));
        Optional.<String>empty().ifPresentOrElse(v -> ran.add("value " + v), () -> ran.add("empty"));
        assertEquals(Arrays.asList("value a", "empty"), ran);
        try {
            Optional.of("a").ifPresentOrElse(null, () -> { });
            fail();
        } catch (NullPointerException expected) {
            // The action that would run is required.
        }
        try {
            Optional.<String>empty().ifPresentOrElse(v -> { }, null);
            fail();
        } catch (NullPointerException expected) {
            // The action that would run is required.
        }
    }

    @Test
    public void optionalOr() {
        Optional<String> present = Optional.of("a");
        final int[] asked = {0};
        assertSame(present, present.or(() -> {
            asked[0]++;
            return Optional.of("b");
        }));
        assertEquals(0, asked[0]);
        assertEquals("b", Optional.<String>empty().or(() -> Optional.of("b")).get());
        assertFalse(Optional.<String>empty().or(() -> Optional.<String>empty()).isPresent());
        try {
            Optional.<String>empty().or(() -> null);
            fail();
        } catch (NullPointerException expected) {
            // The supplier may not answer null.
        }
        try {
            present.or(null);
            fail();
        } catch (NullPointerException expected) {
            // Even when it would not be asked.
        }
    }

    @Test
    public void optionalStream() {
        assertEquals(Arrays.asList("a"), Optional.of("a").stream().collect(Collectors.toList()));
        assertEquals(0L, Optional.empty().stream().count());
        assertEquals(Arrays.asList("a", "c"),
                Stream.of(Optional.of("a"), Optional.<String>empty(), Optional.of("c"))
                        .flatMap(Optional::stream).collect(Collectors.toList()));
    }

    // ---- java.time ----

    private static final LocalTime[] TIMES = {LocalTime.MIDNIGHT, LocalTime.NOON, LocalTime.of(23, 59, 59, 999999999),
        LocalTime.of(0, 0, 0, 1), LocalTime.of(13, 45, 30, 123456789), LocalTime.of(6, 0)};

    private static final long[] AMOUNTS = {0, 1, -1, 59, 60, -60, 999, 1000, 3600, 86399, 86400, 86401, -86400,
        1000000000L, -1000000000L, 86400000000000L, 86400000000001L, -86400000000001L, 123456789012345L,
        Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MIN_VALUE + 1};

    @Test
    public void localTimeNanos() {
        for (LocalTime time : TIMES) {
            for (long n : AMOUNTS) {
                assertEquals(time + " plusNanos " + n, time.plusNanos(n), JdkTime.plusNanos(time, n));
                assertEquals(time + " minusNanos " + n, time.minusNanos(n), JdkTime.minusNanos(time, n));
            }
        }
        LocalTime same = LocalTime.of(1, 2, 3);
        assertSame(same, JdkTime.plusNanos(same, 0));
    }

    @Test
    public void localTimeMinus() {
        for (LocalTime time : TIMES) {
            for (long n : AMOUNTS) {
                assertEquals(time + " minusSeconds " + n, time.minusSeconds(n), JdkTime.minusSeconds(time, n));
                assertEquals(time + " minusMinutes " + n, time.minusMinutes(n), JdkTime.minusMinutes(time, n));
                assertEquals(time + " minusHours " + n, time.minusHours(n), JdkTime.minusHours(time, n));
            }
        }
    }

    @Test
    public void localTimeOrder() {
        for (LocalTime a : TIMES) {
            for (LocalTime b : TIMES) {
                assertEquals(a.isBefore(b), JdkTime.isBefore(a, b));
                assertEquals(a.isAfter(b), JdkTime.isAfter(a, b));
            }
        }
    }

    private static final LocalDate[] DATES = {LocalDate.of(2024, 2, 29), LocalDate.of(2023, 12, 31),
        LocalDate.of(2024, 1, 1), LocalDate.of(1970, 1, 1), LocalDate.of(1969, 12, 31), LocalDate.of(2000, 3, 31),
        LocalDate.of(1, 1, 1), LocalDate.of(-44, 3, 15), LocalDate.of(2024, 2, 29)};

    @Test
    public void localDateOrder() {
        for (LocalDate a : DATES) {
            for (LocalDate b : DATES) {
                assertEquals(a.isBefore(b), JdkTime.isBefore(a, b));
                assertEquals(a.isAfter(b), JdkTime.isAfter(a, b));
                assertEquals(a.isEqual(b), JdkTime.isEqual(a, b));
                assertEquals(Integer.signum(a.compareTo(b)), Integer.signum(JdkTime.compareTo(a, b)));
            }
        }
        try {
            JdkTime.isBefore(DATES[0], null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
    }

    @Test
    public void aDateOfAnotherCalendarIsRefusedNotMisread() {
        java.time.chrono.ChronoLocalDate other = java.time.chrono.JapaneseDate.of(2020, 1, 1);
        try {
            JdkTime.isBefore(DATES[0], other);
            fail();
        } catch (DateTimeException expected) {
            // The device has the ISO calendar alone.
        }
    }

    @Test
    public void localDateArithmetic() {
        long[] amounts = {0, 1, -1, 2, 11, 12, 13, -12, 52, 100, -100, 1200, 4800};
        for (LocalDate date : DATES) {
            for (long n : amounts) {
                assertEquals(date + " minusMonths " + n, date.minusMonths(n), JdkTime.minusMonths(date, n));
                assertEquals(date + " minusYears " + n, date.minusYears(n), JdkTime.minusYears(date, n));
                assertEquals(date + " plusWeeks " + n, date.plusWeeks(n), JdkTime.plusWeeks(date, n));
                assertEquals(date + " minusWeeks " + n, date.minusWeeks(n), JdkTime.minusWeeks(date, n));
            }
        }
    }

    @Test
    public void fromATemporal() {
        LocalDateTime moment = LocalDateTime.of(2024, 2, 29, 13, 45, 30);
        java.time.ZonedDateTime zoned = moment.atZone(java.time.ZoneId.of("UTC"));
        java.time.OffsetDateTime offset = moment.atOffset(java.time.ZoneOffset.ofHours(2));
        assertEquals(LocalDate.from(moment), JdkTime.localDateFrom(moment));
        assertEquals(LocalDate.from(zoned), JdkTime.localDateFrom(zoned));
        assertEquals(LocalDate.from(offset), JdkTime.localDateFrom(offset));
        assertEquals(LocalDate.from(moment.toLocalDate()), JdkTime.localDateFrom(moment.toLocalDate()));
        assertEquals(LocalTime.from(moment), JdkTime.localTimeFrom(moment));
        assertEquals(LocalTime.from(zoned), JdkTime.localTimeFrom(zoned));
        assertEquals(LocalTime.from(offset), JdkTime.localTimeFrom(offset));
        assertEquals(LocalTime.from(moment.toLocalTime()), JdkTime.localTimeFrom(moment.toLocalTime()));
        assertEquals(LocalDateTime.from(moment), JdkTime.localDateTimeFrom(moment));
        assertEquals(LocalDateTime.from(zoned), JdkTime.localDateTimeFrom(zoned));
        assertEquals(LocalDateTime.from(offset), JdkTime.localDateTimeFrom(offset));
        // What holds no date has none to give, in the JDK and here.
        try {
            LocalDate.from(LocalTime.NOON);
            fail();
        } catch (DateTimeException expected) {
            // The JDK's answer.
        }
        try {
            JdkTime.localDateFrom(LocalTime.NOON);
            fail();
        } catch (DateTimeException expected) {
            // And the shim's.
        }
        try {
            JdkTime.localTimeFrom(LocalDate.of(2024, 1, 1));
            fail();
        } catch (DateTimeException expected) {
            // And the other way.
        }
        try {
            JdkTime.localDateTimeFrom(LocalDate.of(2024, 1, 1));
            fail();
        } catch (DateTimeException expected) {
            // A date alone is not a date and time.
        }
        try {
            JdkTime.localDateFrom(null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
    }

    @Test
    public void parseHandsTheParsedValueToTheQuery() {
        // On the device the formatter answers the date itself; here the JDK
        // answers its own parsed form, which the JDK's query can read. That
        // the query receives what the formatter produced is what is held.
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate parsed = JdkTime.parse(formatter, "2024-02-29", LocalDate::from);
        assertEquals(formatter.parse("2024-02-29", LocalDate::from), parsed);
        try {
            JdkTime.parse(formatter, null, LocalDate::from);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
        try {
            JdkTime.parse(formatter, "2024-02-29", (TemporalQuery<LocalDate>) null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
    }

    // ---- DateTimeFormatter.parse(text, query) ----

    /// `formatter.parse(text, LocalDate::from)` is how an application reads
    /// a date in its own pattern. Under this JVM the formatter answers the
    /// JDK's holder rather than a date, and the three `from` methods have
    /// to read it; an edit dialog that validates a birthday this way
    /// threw on every OK.
    @Test
    public void aParsedTextIsReadAsADateATimeOrBoth() {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        assertEquals(date.parse("21.02.1999", java.time.LocalDate::from),
                JdkTime.parse(date, "21.02.1999", JdkTime::localDateFrom));
        DateTimeFormatter both = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertEquals(both.parse("2001-12-31 23:59:58", java.time.LocalDateTime::from),
                JdkTime.parse(both, "2001-12-31 23:59:58", JdkTime::localDateTimeFrom));
        assertEquals(both.parse("2001-12-31 23:59:58", java.time.LocalDate::from),
                JdkTime.parse(both, "2001-12-31 23:59:58", JdkTime::localDateFrom));
        assertEquals(both.parse("2001-12-31 23:59:58", LocalTime::from),
                JdkTime.parse(both, "2001-12-31 23:59:58", JdkTime::localTimeFrom));
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
        assertEquals(time.parse("07:05", LocalTime::from), JdkTime.parse(time, "07:05", JdkTime::localTimeFrom));
        try {
            JdkTime.parse(time, "07:05", JdkTime::localDateFrom);
            org.junit.Assert.fail("a time is no date");
        } catch (java.time.DateTimeException expected) {
            org.junit.Assert.assertNotNull(expected.getMessage());
        }
        try {
            JdkTime.parse(date, "21.02.1999", JdkTime::localDateTimeFrom);
            org.junit.Assert.fail("a date is no date and time");
        } catch (java.time.DateTimeException expected) {
            org.junit.Assert.assertNotNull(expected.getMessage());
        }
    }

    // ---- DateTimeFormatter.withZone ----

    private static final String[] ZONES = {"UTC", "GMT+02:00", "GMT-05:30", "Europe/Paris", "Asia/Tokyo"};
    private static final String[] PATTERNS = {"HH:mm:ss", "yyyy-MM-dd HH:mm", "dd.MM.yyyy", "yyyy-MM-dd'T'HH:mm:ss"};
    private static final long[] SECONDS = {0L, 1L, 86399L, 1700000000L, 951782400L, 1711846800L, -1L};

    @Test
    public void aZonedFormatterShowsAnInstantInItsZone() {
        for (String pattern : PATTERNS) {
            DateTimeFormatter jdk = DateTimeFormatter.ofPattern(pattern);
            DateTimeFormatter shim = JdkTime.ofPattern(pattern);
            org.junit.Assert.assertNull(JdkTime.getZone(shim));
            for (String id : ZONES) {
                java.time.ZoneId zone = java.time.ZoneId.of(id);
                DateTimeFormatter zoned = JdkTime.withZone(shim, zone);
                org.junit.Assert.assertNotSame(shim, zoned);
                assertEquals(jdk.withZone(zone).getZone(), JdkTime.getZone(zoned));
                assertSame(zoned, JdkTime.withZone(zoned, zone));
                for (long s : SECONDS) {
                    java.time.Instant at = java.time.Instant.ofEpochSecond(s);
                    String what = pattern + " " + id + " " + s;
                    assertEquals(what, jdk.withZone(zone).format(at), JdkTime.format(zoned, at));
                    java.time.ZonedDateTime elsewhere = java.time.ZonedDateTime.ofInstant(at, java.time.ZoneId.of("Asia/Kolkata"));
                    assertEquals(what, jdk.withZone(zone).format(elsewhere), JdkTime.format(zoned, elsewhere));
                    assertEquals(what, elsewhere.format(jdk.withZone(zone)), JdkTime.format(elsewhere, zoned));
                    java.time.OffsetDateTime offset = at.atOffset(java.time.ZoneOffset.ofHours(-3));
                    assertEquals(what, jdk.withZone(zone).format(offset), JdkTime.format(offset, zoned));
                    java.time.LocalDateTime local = java.time.LocalDateTime.ofEpochSecond(s, 0,
                            java.time.ZoneOffset.UTC);
                    assertEquals(what, jdk.withZone(zone).format(local), JdkTime.format(zoned, local));
                }
                // The formatter it was made from is left without a zone.
                org.junit.Assert.assertNull(JdkTime.getZone(shim));
                // A second zone replaces the first, and null removes it.
                DateTimeFormatter again = JdkTime.withZone(zoned, java.time.ZoneId.of("GMT+01:00"));
                assertEquals(java.time.ZoneId.of("GMT+01:00"), JdkTime.getZone(again));
                org.junit.Assert.assertNull(JdkTime.getZone(JdkTime.withZone(zoned, null)));
            }
        }
    }

    @Test
    public void aZoneLeavesADateOrATimeAloneAsItIs() {
        DateTimeFormatter time = JdkTime.withZone(JdkTime.ofPattern("HH:mm:ss"), java.time.ZoneId.of("Asia/Tokyo"));
        DateTimeFormatter jdkTime = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.of("Asia/Tokyo"));
        for (LocalTime t : TIMES) {
            assertEquals(jdkTime.format(t), JdkTime.format(time, t));
            assertEquals(t.format(jdkTime), t.format(time));
        }
        DateTimeFormatter date = JdkTime.withZone(JdkTime.ofPattern("dd.MM.yyyy"), java.time.ZoneId.of("UTC"));
        DateTimeFormatter jdkDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(java.time.ZoneId.of("UTC"));
        for (LocalDate d : DATES) {
            assertEquals(jdkDate.format(d), JdkTime.format(date, d));
        }
        // One of the constants, which was not made through ofPattern.
        DateTimeFormatter iso = JdkTime.withZone(DateTimeFormatter.ISO_LOCAL_DATE, java.time.ZoneId.of("UTC"));
        java.time.Instant at = java.time.Instant.ofEpochSecond(1700000000L);
        assertEquals(DateTimeFormatter.ISO_LOCAL_DATE.withZone(java.time.ZoneId.of("UTC")).format(at),
                JdkTime.format(iso, at));
        try {
            JdkTime.format(iso, null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
    }
}
