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
package com.codename1.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class CalendarDateUtil {

    private static final DateTimeFormatter BASIC_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter ISO_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private CalendarDateUtil() {
    }

    static String[] split(String value, char delimiter) {
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == delimiter) {
                count++;
            }
        }
        String[] out = new String[count];
        int start = 0;
        int index = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == delimiter) {
                out[index++] = value.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }

    static String formatBasic(Instant value, ZoneId zone) {
        return BASIC_DATE_TIME.format(ZonedDateTime.ofInstant(value, zone));
    }

    static String formatIso(Instant value, ZoneId zone, boolean milliseconds) {
        return (milliseconds ? ISO_DATE_TIME_MILLIS : ISO_DATE_TIME).format(ZonedDateTime.ofInstant(value, zone));
    }

    static Instant parseDateTime(String value, ZoneId defaultZone) {
        String normalized = normalizeDateTime(value);
        if (normalized.endsWith("Z") || normalized.endsWith("z")) {
            return Instant.parse(normalized.substring(0, normalized.length() - 1) + "Z");
        }
        int timeSeparator = normalized.indexOf('T');
        int plus = normalized.lastIndexOf('+');
        int minus = normalized.lastIndexOf('-');
        if (plus > timeSeparator || minus > timeSeparator) {
            return OffsetDateTime.parse(normalized).toInstant();
        }
        ZoneId zone = defaultZone == null ? ZoneOffset.UTC : defaultZone;
        return ZonedDateTime.of(LocalDateTime.parse(normalized), zone).toInstant();
    }

    static Instant allDayInstant(LocalDate date) {
        return ZonedDateTime.of(date.atTime(0, 0), ZoneOffset.UTC).toInstant();
    }

    private static String normalizeDateTime(String value) {
        if (value == null || value.length() < 15) {
            throw new IllegalArgumentException("Invalid date: " + value);
        }
        String normalized = value;
        if (value.charAt(4) != '-') {
            normalized = value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8) + "T" + value.substring(9, 11) + ":" + value.substring(11, 13) + ":" + value.substring(13);
        }
        int timeSeparator = normalized.indexOf('T');
        int plus = normalized.lastIndexOf('+');
        int minus = normalized.lastIndexOf('-');
        int offset = plus > timeSeparator ? plus : minus > timeSeparator ? minus : -1;
        if (offset >= 0 && normalized.length() - offset == 5) {
            normalized = normalized.substring(0, offset + 3) + ":" + normalized.substring(offset + 3);
        }
        return normalized;
    }
}
