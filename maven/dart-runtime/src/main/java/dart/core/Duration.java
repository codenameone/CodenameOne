/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/**
 * Dart's Duration: an immutable span of time stored in microseconds.
 */
public final class Duration implements Comparable<Duration> {

    public static final long microsecondsPerMillisecond = 1000;
    public static final long microsecondsPerSecond = 1000000;
    public static final long microsecondsPerMinute = 60 * microsecondsPerSecond;
    public static final long microsecondsPerHour = 60 * microsecondsPerMinute;
    public static final long microsecondsPerDay = 24 * microsecondsPerHour;

    public static final Duration zero = new Duration(0);

    private final long micros;

    private Duration(long micros) {
        this.micros = micros;
    }

    public static Duration ofMicroseconds(long micros) {
        return new Duration(micros);
    }

    /** Canonical constructor mirroring Dart's named parameters, in declared order. */
    public static Duration of(long days, long hours, long minutes, long seconds, long milliseconds, long microseconds) {
        return new Duration(days * microsecondsPerDay
                + hours * microsecondsPerHour
                + minutes * microsecondsPerMinute
                + seconds * microsecondsPerSecond
                + milliseconds * microsecondsPerMillisecond
                + microseconds);
    }

    public long inMicroseconds() {
        return micros;
    }

    public long inMilliseconds() {
        return micros / microsecondsPerMillisecond;
    }

    public long inSeconds() {
        return micros / microsecondsPerSecond;
    }

    public long inMinutes() {
        return micros / microsecondsPerMinute;
    }

    public long inHours() {
        return micros / microsecondsPerHour;
    }

    public long inDays() {
        return micros / microsecondsPerDay;
    }

    public Duration plus(Duration other) {
        return new Duration(micros + other.micros);
    }

    public Duration minus(Duration other) {
        return new Duration(micros - other.micros);
    }

    public Duration times(long factor) {
        return new Duration(micros * factor);
    }

    public boolean isNegative() {
        return micros < 0;
    }

    public Duration abs() {
        return micros < 0 ? new Duration(-micros) : this;
    }

    @Override
    public int compareTo(Duration other) {
        return Long.compare(micros, other.micros);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Duration d && d.micros == micros;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(micros);
    }

    @Override
    public String toString() {
        long us = micros;
        String sign = "";
        if (us < 0) {
            sign = "-";
            us = -us;
        }
        long hours = us / microsecondsPerHour;
        long minutes = (us % microsecondsPerHour) / microsecondsPerMinute;
        long seconds = (us % microsecondsPerMinute) / microsecondsPerSecond;
        long microsRem = us % microsecondsPerSecond;
        return sign + hours + ":" + pad2(minutes) + ":" + pad2(seconds) + "." + pad6(microsRem);
    }

    private static String pad2(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }

    private static String pad6(long v) {
        StringBuilder sb = new StringBuilder(Long.toString(v));
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
