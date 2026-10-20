/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.savor.geo;

/// Distances on the ground.
public final class Geo {
    private static final double EARTH_RADIUS_METERS = 6371000d;
    /// Metres in a degree of latitude, anywhere. A degree of longitude is this
    /// times the cosine of the latitude.
    public static final double METERS_PER_DEGREE = 111320d;

    private Geo() {
    }

    /// The great-circle distance between two points, in metres.
    public static double meters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double a = sinLat * sinLat + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * sinLng * sinLng;
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0d, 1 - a)));
    }

    public static boolean isPoint(double lat, double lng) {
        return lat >= -90d && lat <= 90d && lng >= -180d && lng <= 180d
                && !(lat == 0d && lng == 0d);
    }
}
