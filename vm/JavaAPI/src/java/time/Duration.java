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

import java.time.format.DateTimeParseException;

public final class Duration implements Comparable<Duration> {
    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    public static Duration ofDays(long days) {
        return ofSeconds(days * 86400L);
    }

    public static Duration ofHours(long hours) {
        return ofSeconds(hours * 3600L);
    }

    public static Duration ofMinutes(long minutes) {
        return ofSeconds(minutes * 60L);
    }

    public static Duration ofSeconds(long seconds) {
        return new Duration(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = seconds + DateTimeSupport.floorDiv(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        int nanos = (int) DateTimeSupport.floorMod(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        return new Duration(secs, nanos);
    }

    public static Duration ofMillis(long millis) {
        return ofSeconds(DateTimeSupport.floorDiv(millis, 1000L), DateTimeSupport.floorMod(millis, 1000L) * 1000000L);
    }

    public static Duration parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException();
        }
        String value = text.toString();
        int length = value.length();
        int position = 0;
        int overallSign = 1;
        if (position < length && (value.charAt(position) == '+' || value.charAt(position) == '-')) {
            if (value.charAt(position++) == '-') {
                overallSign = -1;
            }
        }
        if (position >= length) {
            throw invalidDuration(value);
        }
        char prefix = value.charAt(position++);
        if (prefix != 'P' && prefix != 'p') {
            throw invalidDuration(value);
        }
        boolean time = false;
        boolean found = false;
        boolean foundTime = false;
        boolean foundDays = false;
        int lastTimeUnit = 0;
        long seconds = 0L;
        int nanos = 0;
        while (position < length) {
            if (value.charAt(position) == 'T' || value.charAt(position) == 't') {
                if (time) {
                    throw invalidDuration(value);
                }
                time = true;
                position++;
                continue;
            }
            int numberStart = position;
            if (value.charAt(position) == '+' || value.charAt(position) == '-') {
                position++;
            }
            int digitStart = position;
            while (position < length && Character.isDigit(value.charAt(position))) {
                position++;
            }
            boolean hasWholeDigits = position > digitStart;
            int fractionStart = -1;
            if (position < length && (value.charAt(position) == '.' || value.charAt(position) == ',')) {
                fractionStart = ++position;
                while (position < length && Character.isDigit(value.charAt(position))) {
                    position++;
                }
            }
            if (!hasWholeDigits || position >= length) {
                throw invalidDuration(value);
            }
            char unit = value.charAt(position++);
            if (unit >= 'a' && unit <= 'z') {
                unit = (char) (unit - ('a' - 'A'));
            }
            String wholeText = value.substring(numberStart, fractionStart < 0 ? position - 1 : fractionStart - 1);
            long amount;
            try {
                amount = Long.parseLong(wholeText);
            } catch (NumberFormatException ex) {
                throw invalidDuration(value);
            }
            if (unit == 'D' && !time && !foundDays && fractionStart < 0) {
                seconds += amount * 86400L;
                foundDays = true;
            } else if (unit == 'H' && time && lastTimeUnit < 1 && fractionStart < 0) {
                seconds += amount * 3600L;
                lastTimeUnit = 1;
                foundTime = true;
            } else if (unit == 'M' && time && lastTimeUnit < 2 && fractionStart < 0) {
                seconds += amount * 60L;
                lastTimeUnit = 2;
                foundTime = true;
            } else if (unit == 'S' && time && lastTimeUnit < 3) {
                seconds += amount;
                lastTimeUnit = 3;
                foundTime = true;
                if (fractionStart >= 0) {
                    String fraction = value.substring(fractionStart, position - 1);
                    if (fraction.length() == 0 || fraction.length() > 9) {
                        throw invalidDuration(value);
                    }
                    while (fraction.length() < 9) {
                        fraction += "0";
                    }
                    nanos = Integer.parseInt(fraction);
                    if (amount < 0 || wholeText.startsWith("-")) {
                        nanos = -nanos;
                    }
                }
            } else {
                throw invalidDuration(value);
            }
            found = true;
        }
        if (!found || (time && !foundTime)) {
            throw invalidDuration(value);
        }
        return ofSeconds(overallSign * seconds, overallSign * (long) nanos);
    }

    private static DateTimeParseException invalidDuration(String value) {
        return new DateTimeParseException("Text cannot be parsed to a Duration", value, 0);
    }

    public long getSeconds() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    public long toMillis() {
        return seconds * 1000L + nanos / 1000000L;
    }

    public Duration plus(Duration other) {
        return ofSeconds(seconds + other.seconds, nanos + other.nanos);
    }

    public Duration minus(Duration other) {
        return ofSeconds(seconds - other.seconds, nanos - other.nanos);
    }

    public int compareTo(Duration other) {
        if (seconds != other.seconds) {
            return seconds < other.seconds ? -1 : 1;
        }
        return nanos < other.nanos ? -1 : nanos > other.nanos ? 1 : 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof Duration && compareTo((Duration) obj) == 0;
    }

    public int hashCode() {
        return (int) (seconds ^ (seconds >>> 32)) + nanos * 31;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("PT");
        long absSeconds = Math.abs(seconds);
        if (seconds < 0 && absSeconds > 0) {
            sb.append('-');
        }
        sb.append(absSeconds);
        if (nanos != 0) {
            sb.append('.');
            String frac = String.valueOf(1000000000L + nanos).substring(1);
            while (frac.endsWith("0")) {
                frac = frac.substring(0, frac.length() - 1);
            }
            sb.append(frac);
        }
        sb.append('S');
        return sb.toString();
    }
}
