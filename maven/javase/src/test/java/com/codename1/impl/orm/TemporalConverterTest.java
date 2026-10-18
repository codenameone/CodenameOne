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
package com.codename1.impl.orm;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/// TemporalConverter is the whole of @Temporal's wire format, and it is hand
/// written because CLDC has no java.time. java.time is the oracle here.
class TemporalConverterTest {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZoneOffset.UTC);

    @Test void formatAndParseAgreeWithJavaTimeAcrossTheRange() {
        Random random = new Random(5989);
        long min = Instant.parse("0000-01-01T00:00:00Z").toEpochMilli();
        long max = Instant.parse("9999-12-31T23:59:59.999Z").toEpochMilli();
        long[] edges = {0, -1, 1, min, max, -14256000000L, 951782400000L, 1709214306789L, 4107542399999L};
        for (int i = 0; i < 20000 + edges.length; i++) {
            long millis = i < edges.length ? edges[i] : min + (long) (random.nextDouble() * (max - min));
            String stamp = TemporalConverter.format(millis, false);
            assertEquals(STAMP.format(Instant.ofEpochMilli(millis)), stamp);
            assertEquals(millis, TemporalConverter.parse(stamp), stamp);
            String day = TemporalConverter.format(millis, true);
            assertEquals(DAY.format(Instant.ofEpochMilli(millis)), day);
            assertEquals(Math.floorDiv(millis, 86400000L) * 86400000L, new TemporalConverter.Day().fromDatabase(day).getTime(), day);
        }
    }

    @Test void readsTheFormsEachEngineReturns() {
        long expected = Instant.parse("2024-02-29T13:45:06.789Z").toEpochMilli();
        for (String form : new String[]{
                "2024-02-29 13:45:06.789",          // SQLite text, MySQL DATETIME(3)
                "2024-02-29T13:45:06.789Z",         // ISO 8601
                "2024-02-29 13:45:06.789+00",       // PostgreSQL timestamptz under TimeZone=UTC
                "2024-02-29 15:45:06.789+02:00",
                "2024-02-29 08:15:06.789-0530",
                "2024-03-01 07:45:06.789+18:00",    // the widest offset java.time accepts
                "2024-02-29 13:45:06.789999",       // microseconds truncate, never round up
                " 2024-02-29 13:45:06.789 "}) {
            assertEquals(expected, TemporalConverter.parse(form), form);
        }
        assertEquals(Instant.parse("2024-02-29T13:45:00Z").toEpochMilli(), TemporalConverter.parse("2024-02-29 13:45"));
        assertEquals(Instant.parse("2024-02-29T13:45:06.500Z").toEpochMilli(), TemporalConverter.parse("2024-02-29T13:45:06.5"));
        // A DATE read through a value that carries a time still answers that day.
        assertEquals(Instant.parse("1969-07-20T00:00:00Z").toEpochMilli(),
                new TemporalConverter.Day().fromDatabase("1969-07-20 18:00:00").getTime());
        assertNull(new TemporalConverter.Timestamp().fromDatabase(null));
        // MySQL's DATE and DATETIME start at year 1000, so writes stop there.
        long floor = Instant.parse("1000-01-01T00:00:00Z").toEpochMilli();
        assertEquals("1000-01-01", new TemporalConverter.Day().toDatabase(new Date(floor)));
        assertThrows(IllegalArgumentException.class, () -> new TemporalConverter.Timestamp().toDatabase(new Date(floor - 1)));
        assertNull(new TemporalConverter.Timestamp().toDatabase(null));
        assertEquals("2024-02-29", new TemporalConverter.Day().toDatabase(new Date(expected)));
    }

    @Test void refusesWhatItCannotReadExactly() {
        for (String bad : new String[]{"", "1709214306789", "0000-00-00 00:00:00", "2023-02-29", "2024-13-01", "2024-02-29 24:00:00",
                "2024-02-29 13:60", "2024-02-29 13:45:06.", "2024-02-29 13:45:06.789 PST", "infinity", "2024-2-29", "2024-02-29X13:45", "2024-02-29+02:00", "2024-02-29-05",
                "2024-01-01T00:00+12:60", "2024-01-01T00:00+99:00", "2024-01-01T00:00-18:01", "2024-01-01T00:00+01:00:60"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TemporalConverter.parse(bad), bad);
            assertTrue(e.getMessage().startsWith("Not a @Temporal value"), e.getMessage());
        }
        assertThrows(IllegalArgumentException.class, () -> TemporalConverter.format(Instant.parse("+10000-01-01T00:00:00Z").toEpochMilli(), false));
        assertThrows(IllegalArgumentException.class, () -> TemporalConverter.format(Instant.parse("-0001-12-31T00:00:00Z").toEpochMilli(), true));
    }
}
