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
package com.codename1.backend.aws;

import com.codename1.impl.backend.CivilTime;

/// The ISO basic timestamp AWS signs with: yyyyMMdd'T'HHmmss'Z', always UTC.
///
/// Written out by hand rather than through SimpleDateFormat because the translated
/// runtime's date formatting is locale-aware and this format must not be -- an
/// Arabic-Indic digit or a locale that renders the year differently produces a
/// signature the service cannot reproduce, and the only symptom is a 403.
final class Clock {
    private Clock() {
    }

    static String timestamp() {
        return timestamp(System.currentTimeMillis());
    }

    static String timestamp(long millis) {
        long seconds = millis / 1000L;
        if (millis < 0 && (millis % 1000L) != 0) {
            seconds--; // floor, so a pre-epoch instant does not round toward zero
        }
        long days = CivilTime.floorDiv(seconds, 86400L);
        int secondOfDay = (int) (seconds - days * 86400L);

        StringBuilder out = new StringBuilder(16);
        CivilTime.appendDate(out, days, (char) 0);
        out.append('T');
        pad(out, secondOfDay / 3600, 2);
        pad(out, (secondOfDay / 60) % 60, 2);
        pad(out, secondOfDay % 60, 2);
        out.append('Z');
        return out.toString();
    }

    private static void pad(StringBuilder out, int value, int width) {
        CivilTime.pad(out, value, width);
    }
}
