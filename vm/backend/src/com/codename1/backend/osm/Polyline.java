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
package com.codename1.backend.osm;

import java.util.List;

/// Google's encoded polyline format, the `polyline` and `polyline6`
/// geometries of OSRM.
final class Polyline {
    private Polyline() {
    }

    /// Encodes points held as `int[] {latE7, lonE7}` at 5 or 6 decimal places.
    static String encode(List points, int precision) {
        int divisor = precision == 6 ? 10 : 100;
        StringBuilder sb = new StringBuilder();
        long lastLat = 0;
        long lastLon = 0;
        for (Object point : points) {
            int[] p = (int[]) point;
            long lat = roundDiv(p[0], divisor);
            long lon = roundDiv(p[1], divisor);
            value(sb, lat - lastLat);
            value(sb, lon - lastLon);
            lastLat = lat;
            lastLon = lon;
        }
        return sb.toString();
    }

    private static long roundDiv(int v, int divisor) {
        return v >= 0 ? (v + divisor / 2) / divisor : -((-(long) v + divisor / 2) / divisor);
    }

    private static void value(StringBuilder sb, long v) {
        long s = v < 0 ? ~(v << 1) : v << 1;
        while (s >= 0x20) {
            sb.append((char) ((0x20 | (s & 0x1f)) + 63));
            s >>>= 5;
        }
        sb.append((char) (s + 63));
    }
}
