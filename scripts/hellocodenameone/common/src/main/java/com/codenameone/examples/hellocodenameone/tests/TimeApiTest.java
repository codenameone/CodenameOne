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

package com.codenameone.examples.hellocodenameone.tests;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeApiTest extends BaseTest {
    private String zonedString(ZonedDateTime value) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").format(OffsetDateTime.of(value.toLocalDateTime(), value.getOffset()))
                + "[" + value.getZone().getId() + "]";
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            assertEqual("2000-02-29", LocalDate.of(2000, 2, 29).toString());
            assertEqual("1900-03-01", LocalDate.of(1900, 2, 28).plusDays(1).toString());
            assertEqual("2100-03-01", LocalDate.of(2100, 2, 28).plusDays(1).toString());
            assertEqual("2020-02-29", LocalDate.of(2020, 1, 31).plusMonths(1).toString());

            Clock fixedClock = Clock.fixed(Instant.parse("2020-06-01T10:15:30Z"), ZoneId.of("UTC"));
            assertEqual("2020-06-01T10:15:30", LocalDateTime.now(fixedClock).toString());

            ZonedDateTime beforeGap = ZonedDateTime.ofInstant(Instant.parse("2020-03-08T06:30:00Z"), ZoneId.of("America/New_York"));
            ZonedDateTime afterGap = ZonedDateTime.ofInstant(Instant.parse("2020-03-08T07:30:00Z"), ZoneId.of("America/New_York"));
            ZonedDateTime overlapEarly = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T05:30:00Z"), ZoneId.of("America/New_York"));
            ZonedDateTime overlapLate = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T06:30:00Z"), ZoneId.of("America/New_York"));

            assertEqual("2020-03-08T01:30:00-05:00[America/New_York]", zonedString(beforeGap));
            assertEqual("2020-03-08T03:30:00-04:00[America/New_York]", zonedString(afterGap));
            assertEqual("2020-11-01T01:30:00-04:00[America/New_York]", zonedString(overlapEarly));
            assertEqual("2020-11-01T01:30:00-05:00[America/New_York]", zonedString(overlapLate));

            OffsetDateTime berlin = OffsetDateTime.ofInstant(Instant.parse("2020-06-01T10:15:30Z"), ZoneId.of("Europe/Berlin"));
            assertEqual("2020-06-01 12:15 +02:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX").format(berlin));

            LocalDateTime parsed = LocalDateTime.parse("2020-02-29 23:45:17", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            assertEqual("2020-02-29T23:45:17", parsed.toString());

            String localized = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm", new Locale("en", "US"))
                    .format(LocalDateTime.of(2020, 2, 29, 23, 45));
            assertEqual("Sat Feb 29 2020 23:45", localized);

            assertEqual(95061L, Duration.ofMillis(90061).plus(Duration.ofSeconds(5)).toMillis());
            assertEqual("PT0.5S", Duration.ofMillis(500).toString());
            assertEqual("PT-0.5S", Duration.ofMillis(-500).toString());
            assertEqual(Duration.ofMillis(500), Duration.parse(Duration.ofMillis(500).toString()));
            assertEqual(Duration.ofMillis(-500), Duration.parse(Duration.ofMillis(-500).toString()));
            Period period = Period.of(1, 1, 1);
            assertEqual("2020-03-01", LocalDate.of(2019, 1, 31).plusYears(period.getYears()).plusMonths(period.getMonths()).plusDays(period.getDays()).toString());
            assertEqual("00:00:01", LocalTime.of(23, 59, 59).plusSeconds(2).toString());
        } catch (Throwable t) {
            fail("Time API test failed: " + t);
            return false;
        }
        done();
        return true;
    }
}
