/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

import java.util.ArrayList;
import java.util.List;

/// Reads and writes the *encoded polyline* format -- the compact ASCII
/// representation of a coordinate sequence that virtually every directions
/// and routing service uses for route geometry (Google Directions, OSRM,
/// Mapbox Directions, GraphHopper, Valhalla, OpenRouteService).
///
/// Use it to turn a route geometry straight into something drawable:
///
/// ```java
/// map.addPolyline(Polyline.fromEncoded(geometryFromMyDirectionsApi));
/// ```
///
/// The encoding stores each coordinate as a zig-zag, base-64-ish delta from
/// the previous one at a fixed decimal `precision`. Precision 5 is the
/// original Google format and the OSRM/Google/GraphHopper default; precision
/// 6 (`polyline6`) is what Valhalla and OSRM's `geometries=polyline6` emit.
/// Decoding with the wrong precision yields coordinates off by a factor of
/// ten, so pass the precision your service documents.
public final class PolylineCodec {

    private static final int DEFAULT_PRECISION = 5;
    private static final int CHUNK_BITS = 5;
    private static final int CHUNK_MASK = 0x1f;
    private static final int CONTINUATION_BIT = 0x20;
    private static final int ASCII_OFFSET = 63;

    private PolylineCodec() {
    }

    /// Decodes an encoded polyline at the default precision of 5.
    ///
    /// #### Parameters
    ///
    /// - `encoded`: the encoded geometry, may be `null`
    ///
    /// #### Returns
    ///
    /// the decoded [LatLng] vertices, empty when `encoded` is `null` or blank
    public static List decode(String encoded) {
        return decode(encoded, DEFAULT_PRECISION);
    }

    /// Decodes an encoded polyline at an explicit decimal precision (5 for the
    /// classic format, 6 for `polyline6`).
    ///
    /// Trailing bytes that do not form a complete coordinate pair -- the usual
    /// symptom of a truncated response -- are ignored rather than throwing, so
    /// a partial geometry still draws what it can.
    ///
    /// #### Parameters
    ///
    /// - `encoded`: the encoded geometry, may be `null`
    ///
    /// - `precision`: the number of decimal digits the encoder scaled by
    ///
    /// #### Returns
    ///
    /// the decoded [LatLng] vertices, never `null`
    public static List decode(String encoded, int precision) {
        List points = new ArrayList();
        if (encoded == null || encoded.length() == 0) {
            return points;
        }
        double factor = factor(precision);
        int[] cursor = new int[1];
        long lat = 0;
        long lng = 0;
        int len = encoded.length();
        while (cursor[0] < len) {
            long dLat = readValue(encoded, cursor);
            if (cursor[0] >= len) {
                // Odd number of values: the latitude has no matching longitude.
                break;
            }
            long dLng = readValue(encoded, cursor);
            lat += dLat;
            lng += dLng;
            points.add(new LatLng(lat / factor, lng / factor));
        }
        return points;
    }

    /// Encodes [LatLng] vertices at the default precision of 5.
    ///
    /// #### Parameters
    ///
    /// - `points`: the vertices to encode, may be `null`
    ///
    /// #### Returns
    ///
    /// the encoded geometry, empty for a `null` or empty list
    public static String encode(List points) {
        return encode(points, DEFAULT_PRECISION);
    }

    /// Encodes [LatLng] vertices at an explicit decimal precision.
    ///
    /// #### Parameters
    ///
    /// - `points`: the vertices to encode, may be `null`
    ///
    /// - `precision`: the number of decimal digits to scale by
    ///
    /// #### Returns
    ///
    /// the encoded geometry, never `null`
    public static String encode(List points, int precision) {
        StringBuilder sb = new StringBuilder();
        if (points == null) {
            return sb.toString();
        }
        double factor = factor(precision);
        long prevLat = 0;
        long prevLng = 0;
        for (Object pointObj : points) {
            LatLng p = (LatLng) pointObj;
            long lat = Math.round(p.getLatitude() * factor);
            long lng = Math.round(p.getLongitude() * factor);
            writeValue(sb, lat - prevLat);
            writeValue(sb, lng - prevLng);
            prevLat = lat;
            prevLng = lng;
        }
        return sb.toString();
    }

    private static double factor(int precision) {
        double f = 1;
        for (int i = 0; i < precision; i++) {
            f *= 10;
        }
        return f;
    }

    /// Reads one zig-zag encoded delta, advancing `cursor[0]` past it. A value
    /// cut short by the end of the string simply stops early.
    private static long readValue(String encoded, int[] cursor) {
        int len = encoded.length();
        long result = 0;
        int shift = 0;
        while (cursor[0] < len) {
            int b = encoded.charAt(cursor[0]++) - ASCII_OFFSET;
            result |= ((long) (b & CHUNK_MASK)) << shift;
            shift += CHUNK_BITS;
            if (b < CONTINUATION_BIT || shift >= 64) {
                break;
            }
        }
        return (result & 1) != 0 ? ~(result >>> 1) : result >>> 1;
    }

    private static void writeValue(StringBuilder sb, long value) {
        long v = value < 0 ? ~(value << 1) : value << 1;
        while (v >= CONTINUATION_BIT) {
            sb.append((char) ((CONTINUATION_BIT | (int) (v & CHUNK_MASK)) + ASCII_OFFSET));
            v >>= CHUNK_BITS;
        }
        sb.append((char) (v + ASCII_OFFSET));
    }
}
