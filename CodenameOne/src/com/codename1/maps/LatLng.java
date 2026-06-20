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

import com.codename1.util.MathUtil;

/// An immutable WGS84 geographic coordinate (latitude/longitude in degrees).
///
/// Unlike the legacy [Coord], `LatLng` is always unprojected (plain
/// lat/lon) and immutable, which makes it safe to share between the map
/// components, the vector engine and native providers. It is the value
/// type used throughout the modern maps API ([MapView], [NativeMap] and
/// [com.codename1.maps.spi.MapProvider]).
public final class LatLng {

    private static final double EARTH_RADIUS_METERS = 6378137.0;
    private static final double DELTA = 0.0000001;

    private final double latitude;
    private final double longitude;

    /// Creates a coordinate from a latitude/longitude pair in degrees.
    ///
    /// #### Parameters
    ///
    /// - `latitude`: the latitude in degrees, clamped to the valid range
    ///
    /// - `longitude`: the longitude in degrees, normalized to [-180, 180]
    public LatLng(double latitude, double longitude) {
        if (latitude > 90) {
            latitude = 90;
        } else if (latitude < -90) {
            latitude = -90;
        }
        while (longitude > 180) {
            longitude -= 360;
        }
        while (longitude < -180) {
            longitude += 360;
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /// Factory method mirroring the constructor for fluent call sites.
    public static LatLng create(double latitude, double longitude) {
        return new LatLng(latitude, longitude);
    }

    /// Converts a legacy [Coord] (assumed WGS84) into a `LatLng`.
    public static LatLng fromCoord(Coord c) {
        return new LatLng(c.getLatitude(), c.getLongitude());
    }

    /// The latitude in degrees in the range [-90, 90].
    public double getLatitude() {
        return latitude;
    }

    /// The longitude in degrees in the range [-180, 180].
    public double getLongitude() {
        return longitude;
    }

    /// Converts this coordinate into a legacy WGS84 [Coord].
    public Coord toCoord() {
        return new Coord(latitude, longitude, false);
    }

    /// The great-circle distance in meters between this coordinate and
    /// `other`, computed with the haversine formula.
    public double distanceTo(LatLng other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * MathUtil.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /// {@inheritDoc}
    public boolean equals(Object o) {
        if (!(o instanceof LatLng)) {
            return false;
        }
        LatLng l = (LatLng) o;
        return Math.abs(latitude - l.latitude) < DELTA
                && Math.abs(longitude - l.longitude) < DELTA;
    }

    /// {@inheritDoc}
    public int hashCode() {
        long lat = Double.doubleToLongBits(latitude);
        long lon = Double.doubleToLongBits(longitude);
        int hash = 7;
        hash = 31 * hash + (int) (lat ^ (lat >>> 32));
        hash = 31 * hash + (int) (lon ^ (lon >>> 32));
        return hash;
    }

    /// {@inheritDoc}
    public String toString() {
        return "LatLng{" + latitude + ", " + longitude + "}";
    }
}
