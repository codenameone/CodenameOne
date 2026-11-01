/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.log;

/// ISO-8601 UTC timestamps without java.time or a formatter.
final class Timestamps {
    private Timestamps() {
    }

    /// Appends `millis` as `yyyy-MM-ddTHH:mm:ss.SSSZ`.
    static void appendIso(StringBuilder out, long millis) {
        long days = Math.floorDiv(millis, 86400000L);
        long ofDay = millis - days * 86400000L;
        // Civil date from days since the epoch (H. Hinnant's days_from_civil inverse).
        long z = days + 719468;
        long era = Math.floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long day = doy - (153 * mp + 2) / 5 + 1;
        long month = mp < 10 ? mp + 3 : mp - 9;
        long year = yoe + era * 400 + (month <= 2 ? 1 : 0);
        pad(out, year, 4).append('-');
        pad(out, month, 2).append('-');
        pad(out, day, 2).append('T');
        pad(out, ofDay / 3600000L, 2).append(':');
        pad(out, ofDay / 60000L % 60, 2).append(':');
        pad(out, ofDay / 1000L % 60, 2).append('.');
        pad(out, ofDay % 1000L, 3).append('Z');
    }

    private static StringBuilder pad(StringBuilder out, long value, int width) {
        String digits = Long.toString(value);
        for (int i = digits.length(); i < width; i++) {
            out.append('0');
        }
        return out.append(digits);
    }
}
