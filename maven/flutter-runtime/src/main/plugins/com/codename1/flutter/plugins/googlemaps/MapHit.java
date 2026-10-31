/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.googlemaps;

/**
 * Hit testing of taps against map overlays, in map coordinates. The core map reports
 * where a tap landed as a latitude and longitude and has no tap for a line or shape,
 * so the element tests the point itself: positions are projected to meters around the
 * tap (equirectangular, exact enough over the few hundred meters a finger covers).
 */
final class MapHit {
    private static final double METERS_PER_DEGREE_LAT = 111320.0;
    private static final double EQUATOR_METERS_PER_PIXEL_AT_ZOOM_0 = 156543.03392;

    private MapHit() {
    }

    /// Meters one device pixel covers at this latitude and zoom (256 px tiles).
    static double metersPerPixel(double latitude, double zoom) {
        return EQUATOR_METERS_PER_PIXEL_AT_ZOOM_0 * Math.cos(Math.toRadians(latitude)) / Math.pow(2, zoom);
    }

    /// East offset in meters of {@code lng} from {@code lng0}, at latitude {@code lat0}.
    private static double east(double lng0, double lng, double lat0) {
        double d = lng - lng0;
        while (d > 180) {
            d -= 360;
        }
        while (d < -180) {
            d += 360;
        }
        return d * METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat0));
    }

    private static double north(double lat0, double lat) {
        return (lat - lat0) * METERS_PER_DEGREE_LAT;
    }

    /// Distance in meters between two positions.
    static double distance(LatLng a, LatLng b) {
        double dx = east(a.longitude(), b.longitude(), a.latitude());
        double dy = north(a.latitude(), b.latitude());
        return Math.sqrt(dx * dx + dy * dy);
    }

    /// Distance in meters from {@code p} to the segment {@code a}-{@code b}.
    static double distanceToSegment(LatLng p, LatLng a, LatLng b) {
        double ax = east(p.longitude(), a.longitude(), p.latitude());
        double ay = north(p.latitude(), a.latitude());
        double bx = east(p.longitude(), b.longitude(), p.latitude());
        double by = north(p.latitude(), b.latitude());
        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / len2));
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        return Math.sqrt(cx * cx + cy * cy);
    }
}
