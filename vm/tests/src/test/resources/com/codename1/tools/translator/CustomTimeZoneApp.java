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
/**
 * Prints the id and raw offset java.util.TimeZone resolves for a set of custom
 * GMT-offset ids, including malformed ones.
 *
 * CustomTimeZoneIdTest runs this on the host JDK and through ParparVM and
 * requires identical output. Only offset ids appear here -- a named zone would
 * depend on the platform tz database rather than on the custom-id parser.
 */
public class CustomTimeZoneApp {

    private static final String[] IDS = {
        "GMT", "GMT+0", "GMT-0", "GMT+5", "GMT-8", "GMT+10", "GMT+23",
        "GMT+000", "GMT+123", "GMT+0130", "GMT+2359",
        "GMT+01:02", "GMT+5:00", "GMT+23:59", "GMT-08:00", "GMT+00:00",
        // "GMT+01:30:00" is deliberately absent: JDK 17 answers GMT and JDK 25
        // answers GMT+01:30 for it, so it cannot serve as a stable oracle. The
        // documented syntax has no seconds field and this port rejects it.
        // Each of these is malformed and must not become an offset.
        "GMT+1:2", "GMT+05:0", "GMT+013000", "GMT+12345",
        // "GMT+00000" is deliberately absent. Five digits are undefined by the
        // documented syntax and the JDKs disagree: 11 and 17 answer GMT+00:00,
        // 21 and 25 answer GMT. It cannot be a stable oracle, so this port
        // follows the documented form and rejects it.
        "GMT+24", "GMT+1:60", "GMT+23:60", "GMT+01:30:99", "GMT+01:30xyz",
        "GMT+1x", "GMT+", "UTC+5", "UT+5",
        // Z-suffixed pseudo ids name no zone; the JDK takes the unknown-id
        // fallback, so the id has to be GMT and not the spelling asked for.
        "GMTZ", "UTCZ", "UTZ"
    };

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("RESULT=");
        for (int i = 0; i < IDS.length; i++) {
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(IDS[i]);
            sb.append(IDS[i]).append('=').append(tz.getRawOffset())
              .append('/').append(tz.getID()).append(';');
        }
        System.out.println(sb.toString());
    }
}
