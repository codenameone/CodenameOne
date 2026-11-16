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
package com.codenameone.examples.fieldline.scheduler;

/// The offline default: straight-line distance stretched by a road factor and
/// driven at a fixed speed, plus a fixed parking allowance per leg.
///
/// Integer only. The east-west scale is the cosine of the region's latitude,
/// supplied in thousandths, because a planning region spans a city and a single
/// value is accurate to well under a percent there.
public final class GeoTravel implements TravelModel {
    private static final long METERS_PER_DEGREE = 111195;

    private final int cosLatPermille;
    private final int roadFactorPermille;
    private final int metersPerMinute;
    private final int parkingMinutes;

    /// For example `new GeoTravel(788, 1350, 40, 5)` for a city near 38 degrees
    /// north, roads 35% longer than the crow flies, 40 km/h, 5 minutes parking.
    public GeoTravel(int cosLatPermille, int roadFactorPermille, int kmh, int parkingMinutes) {
        this.cosLatPermille = cosLatPermille;
        this.roadFactorPermille = roadFactorPermille;
        this.metersPerMinute = Math.max(1, kmh * 1000 / 60);
        this.parkingMinutes = parkingMinutes;
    }

    /// Road meters between two points.
    public long meters(int lat1, int lon1, int lat2, int lon2) {
        long dy = (long) (lat2 - lat1) * METERS_PER_DEGREE / 1000000L;
        long dx = (long) (lon2 - lon1) * METERS_PER_DEGREE / 1000000L * cosLatPermille / 1000L;
        return isqrt(dx * dx + dy * dy) * roadFactorPermille / 1000L;
    }

    @Override
    public int minutes(int lat1, int lon1, int lat2, int lon2) {
        long m = meters(lat1, lon1, lat2, lon2);
        if (m == 0) {
            return 0;
        }
        return (int) ((m + metersPerMinute - 1) / metersPerMinute) + parkingMinutes;
    }

    static long isqrt(long v) {
        if (v <= 0) {
            return 0;
        }
        long x = v;
        long y = (x + 1) / 2;
        while (y < x) {
            x = y;
            y = (x + v / x) / 2;
        }
        return x;
    }
}
