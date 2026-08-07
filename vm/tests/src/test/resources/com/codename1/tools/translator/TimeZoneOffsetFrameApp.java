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
 * Offsets around the 2020 US spring-forward transition, from both directions.
 *
 * TimeZoneOffsetFrameTest runs this on the host JDK and through ParparVM and
 * requires identical output. The two halves are the two frames that were being
 * confused: getOffset takes local STANDARD time fields, while an Instant has to
 * be converted into that frame before it can be asked about.
 */
import java.util.Calendar;
import java.util.TimeZone;

public class TimeZoneOffsetFrameApp {

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("RESULT=");
        String[] zones = { "America/New_York", "Europe/Berlin", "Asia/Tokyo" };
        for (int z = 0; z < zones.length; z++) {
            TimeZone tz = TimeZone.getTimeZone(zones[z]);
            // Local-standard fields straight across the US transition.
            for (int hour = 0; hour < 6; hour++) {
                sb.append(zones[z]).append('@').append(hour).append('=')
                  .append(tz.getOffset(1, 2020, 2, 8, Calendar.SUNDAY, hour * 3600000))
                  .append(';');
            }
            // The same day approached as instants, one per hour of UTC.
            for (int hour = 0; hour < 12; hour++) {
                long instant = 1583625600000L + hour * 3600000L; // 2020-03-08T00:00Z
                java.util.Date d = new java.util.Date(instant);
                Calendar cal = Calendar.getInstance(tz);
                cal.setTime(d);
                sb.append(zones[z]).append('#').append(hour).append('=')
                  .append(cal.get(Calendar.HOUR_OF_DAY)).append(':')
                  .append(cal.get(Calendar.MINUTE)).append(';');
            }
        }
        System.out.println(sb.toString());
    }
}
