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

import java.time.temporal.TemporalAccessor;

/// A fixed offset from UTC, between -18:00 and +18:00.
public final class ZoneOffset extends ZoneId implements TemporalAccessor, Comparable<ZoneOffset> {
    private static final int MAX_SECONDS = 18 * 3600;

    public static final ZoneOffset UTC = new ZoneOffset(0);
    public static final ZoneOffset MIN = new ZoneOffset(-MAX_SECONDS);
    public static final ZoneOffset MAX = new ZoneOffset(MAX_SECONDS);

    private final int totalSeconds;

    private ZoneOffset(int totalSeconds) {
        super(buildId(totalSeconds));
        this.totalSeconds = totalSeconds;
    }

    /// Reads `Z`, `+h`, `+hh`, `+hh:mm`, `+hhmm`, `+hh:mm:ss` and
    /// `+hhmmss`, with `-` in place of `+` for a negative offset.
    public static ZoneOffset of(String offsetId) {
        if (offsetId == null) {
            throw new NullPointerException("offsetId");
        }
        if ("Z".equals(offsetId)) {
            return UTC;
        }
        String text = offsetId;
        int hours;
        int minutes;
        int seconds;
        switch (text.length()) {
            case 2:
                text = text.charAt(0) + "0" + text.charAt(1);
                hours = parseNumber(offsetId, text, 1, false);
                minutes = 0;
                seconds = 0;
                break;
            case 3:
                hours = parseNumber(offsetId, text, 1, false);
                minutes = 0;
                seconds = 0;
                break;
            case 5:
                hours = parseNumber(offsetId, text, 1, false);
                minutes = parseNumber(offsetId, text, 3, false);
                seconds = 0;
                break;
            case 6:
                hours = parseNumber(offsetId, text, 1, false);
                minutes = parseNumber(offsetId, text, 4, true);
                seconds = 0;
                break;
            case 7:
                hours = parseNumber(offsetId, text, 1, false);
                minutes = parseNumber(offsetId, text, 3, false);
                seconds = parseNumber(offsetId, text, 5, false);
                break;
            case 9:
                hours = parseNumber(offsetId, text, 1, false);
                minutes = parseNumber(offsetId, text, 4, true);
                seconds = parseNumber(offsetId, text, 7, true);
                break;
            default:
                throw new DateTimeException("Invalid ID for ZoneOffset, invalid format: " + offsetId);
        }
        char first = text.charAt(0);
        if (first != '+' && first != '-') {
            throw new DateTimeException("Invalid ID for ZoneOffset, plus/minus not found when expected: "
                    + offsetId);
        }
        if (first == '-') {
            return ofHoursMinutesSeconds(-hours, -minutes, -seconds);
        }
        return ofHoursMinutesSeconds(hours, minutes, seconds);
    }

    private static int parseNumber(String offsetId, String text, int pos, boolean precededByColon) {
        if (precededByColon && text.charAt(pos - 1) != ':') {
            throw new DateTimeException("Invalid ID for ZoneOffset, colon not found when expected: " + offsetId);
        }
        char ch1 = text.charAt(pos);
        char ch2 = text.charAt(pos + 1);
        if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
            throw new DateTimeException("Invalid ID for ZoneOffset, non numeric characters found: " + offsetId);
        }
        return (ch1 - 48) * 10 + (ch2 - 48);
    }

    public static ZoneOffset ofHours(int hours) {
        return ofHoursMinutesSeconds(hours, 0, 0);
    }

    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        return ofHoursMinutesSeconds(hours, minutes, 0);
    }

    /// The offset of `hours`, `minutes` and `seconds`, which must all have
    /// the same sign.
    public static ZoneOffset ofHoursMinutesSeconds(int hours, int minutes, int seconds) {
        if (hours < -18 || hours > 18) {
            throw new DateTimeException("Zone offset hours not in valid range: value " + hours
                    + " is not in the range -18 to 18");
        }
        if (hours > 0) {
            if (minutes < 0 || seconds < 0) {
                throw new DateTimeException("Zone offset minutes and seconds must be positive because hours is positive");
            }
        } else if (hours < 0) {
            if (minutes > 0 || seconds > 0) {
                throw new DateTimeException("Zone offset minutes and seconds must be negative because hours is negative");
            }
        } else if ((minutes > 0 && seconds < 0) || (minutes < 0 && seconds > 0)) {
            throw new DateTimeException("Zone offset minutes and seconds must have the same sign");
        }
        if (minutes < -59 || minutes > 59) {
            throw new DateTimeException("Zone offset minutes not in valid range: value " + minutes
                    + " is not in the range -59 to 59");
        }
        if (seconds < -59 || seconds > 59) {
            throw new DateTimeException("Zone offset seconds not in valid range: value " + seconds
                    + " is not in the range -59 to 59");
        }
        if (Math.abs(hours) == 18 && (minutes | seconds) != 0) {
            throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
        return ofTotalSeconds(hours * 3600 + minutes * 60 + seconds);
    }

    public static ZoneOffset ofTotalSeconds(int totalSeconds) {
        if (totalSeconds < -MAX_SECONDS || totalSeconds > MAX_SECONDS) {
            throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
        if (totalSeconds == 0) {
            return UTC;
        }
        return new ZoneOffset(totalSeconds);
    }

    /// The offset of `temporal`.
    public static ZoneOffset from(TemporalAccessor temporal) {
        if (temporal instanceof ZoneOffset) {
            return (ZoneOffset) temporal;
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).getOffset();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).getOffset();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain ZoneOffset from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    @Override
    ZoneOffset fixedOffset() {
        return this;
    }

    /// Larger offsets sort first, as in the JDK: an offset further east is
    /// an earlier moment for the same wall clock time.
    public int compareTo(ZoneOffset other) {
        return other.totalSeconds - totalSeconds;
    }

    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ZoneOffset && ((ZoneOffset) obj).totalSeconds == totalSeconds);
    }

    public int hashCode() {
        return totalSeconds;
    }

    private static String buildId(int totalSeconds) {
        if (totalSeconds == 0) {
            return "Z";
        }
        int abs = Math.abs(totalSeconds);
        int hours = abs / 3600;
        int minutes = (abs / 60) % 60;
        int seconds = abs % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(totalSeconds < 0 ? '-' : '+');
        sb.append(hours < 10 ? "0" : "").append(hours);
        sb.append(minutes < 10 ? ":0" : ":").append(minutes);
        if (seconds != 0) {
            sb.append(seconds < 10 ? ":0" : ":").append(seconds);
        }
        return sb.toString();
    }
}
