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

import java.time.Instant;

/// `java.nio.file.attribute.FileTime` for the Codename One runtime: a file's
/// time stamp, kept to the millisecond, which is as fine as the device's
/// file system reports it.
public final class FileTime implements Comparable<FileTime> {

    private final long millis;

    private FileTime(long millis) {
        this.millis = millis;
    }

    public static FileTime fromMillis(long value) {
        return new FileTime(value);
    }

    public static FileTime from(long value, TimeUnit unit) {
        return new FileTime(unit.toMillis(value));
    }

    public static FileTime from(Instant instant) {
        return new FileTime(instant.toEpochMilli());
    }

    public long toMillis() {
        return millis;
    }

    public long to(TimeUnit unit) {
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(millis);
    }

    @Override
    public int compareTo(FileTime other) {
        return millis < other.millis ? -1 : millis == other.millis ? 0 : 1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileTime && ((FileTime) o).millis == millis;
    }

    @Override
    public int hashCode() {
        return (int) (millis ^ (millis >>> 32));
    }

    private static void pad(StringBuilder sb, long value, int width) {
        String digits = Long.toString(value);
        for (int i = digits.length(); i < width; i++) {
            sb.append('0');
        }
        sb.append(digits);
    }

    /// The time in ISO 8601, in UTC, as the JDK writes it:
    /// `2024-02-29T12:30:05.25Z`, the fraction left out when it is zero.
    @Override
    public String toString() {
        long days = JdkNumbers.floorDiv(millis, 86400000L);
        long ofDay = JdkNumbers.floorMod(millis, 86400000L);
        // Days since 1970-01-01 to a civil date, in the proleptic Gregorian
        // calendar: eras of 400 years, counted from the first of March.
        long z = days + 719468L;
        long era = JdkNumbers.floorDiv(z, 146097L);
        long doe = z - era * 146097L;
        long yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L;
        long doy = doe - (365L * yoe + yoe / 4L - yoe / 100L);
        long mp = (5L * doy + 2L) / 153L;
        long day = doy - (153L * mp + 2L) / 5L + 1L;
        long month = mp < 10 ? mp + 3 : mp - 9;
        long year = yoe + era * 400L + (month <= 2 ? 1 : 0);
        StringBuilder sb = new StringBuilder(30);
        if (year < 0) {
            sb.append('-');
            pad(sb, -year, 4);
        } else {
            pad(sb, year, 4);
        }
        sb.append('-');
        pad(sb, month, 2);
        sb.append('-');
        pad(sb, day, 2);
        sb.append('T');
        pad(sb, ofDay / 3600000L, 2);
        sb.append(':');
        pad(sb, ofDay / 60000L % 60L, 2);
        sb.append(':');
        pad(sb, ofDay / 1000L % 60L, 2);
        long fraction = ofDay % 1000L;
        if (fraction != 0) {
            sb.append('.');
            pad(sb, fraction, 3);
            while (sb.charAt(sb.length() - 1) == '0') {
                sb.setLength(sb.length() - 1);
            }
        }
        return sb.append('Z').toString();
    }
}
