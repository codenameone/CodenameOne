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
package java.time;

import java.util.TimeZone;

/// Where `java.time` meets the platform's time zone rules.
final class DateTimeSupport {
    private DateTimeSupport() {
    }

    static LocalDateTime localDateTimeFromInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = offsetFromInstant(instant, zone);
        return LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
    }

    /// The offset `zone` has at `instant`.
    static ZoneOffset offsetFromInstant(Instant instant, ZoneId zone) {
        ZoneOffset fixed = zone.fixedOffset();
        if (fixed != null) {
            // A fixed offset already is the answer; round-tripping it through
            // the host time zone database can only lose or invert it.
            return fixed;
        }
        TimeZone tz = zone.toTimeZone();
        // Ask the zone for its offset at this instant rather than reading the
        // fields back out of a Calendar. Calendar reconstructs the local time
        // from a raw offset plus a fixed one-hour daylight guess, which loses
        // the saving on the desktop ports (Europe/Berlin in June came back as
        // UTC), while TimeZone.getOffset consults the platform's own rules.
        // getOffset takes LOCAL STANDARD time fields, so the instant is
        // turned into local standard time first by adding the raw offset.
        // Worked in seconds, not milliseconds: the instants java.time accepts
        // reach past what a long counts in milliseconds.
        long localStandard = instant.getEpochSecond() + tz.getRawOffset() / 1000;
        long epochDay = TimeArithmetic.floorDiv(localStandard, 86400L);
        int millisOfDay = (int) TimeArithmetic.floorMod(localStandard, 86400L) * 1000
                + instant.getNano() / 1000000;
        LocalDate utcDate = LocalDate.ofEpochDay(epochDay);
        // Calendar.SUNDAY is 1 and epoch day 0 was a Thursday.
        int dayOfWeek = (int) TimeArithmetic.floorMod(epochDay + 4, 7) + 1;
        // getOffset takes an era plus a year *of that era*, not a proleptic ISO
        // year: ISO 0 is 1 BC, ISO -1 is 2 BC, hence 1 - isoYear.
        int isoYear = utcDate.getYear();
        int era = isoYear > 0 ? 1 /* GregorianCalendar.AD */ : 0 /* GregorianCalendar.BC */;
        int yearOfEra = isoYear > 0 ? isoYear : 1 - isoYear;
        int offsetMillis = tz.getOffset(era, yearOfEra,
                utcDate.getMonthValue() - 1, utcDate.getDayOfMonth(), dayOfWeek, millisOfDay);
        return ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
    }
}
