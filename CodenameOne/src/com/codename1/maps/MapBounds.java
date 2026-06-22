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

import java.util.List;

/// An immutable axis-aligned latitude/longitude rectangle delimited by its
/// south-west and north-east corners.
///
/// Replaces the legacy [BoundingBox] for the modern API, fixing the null
/// bounding-box issues of the old point layers and always operating in
/// WGS84 ([LatLng]) coordinates.
public final class MapBounds {

    private final LatLng southWest;
    private final LatLng northEast;

    /// Creates a bounding box from two opposing corners. The corners are
    /// normalized so that `southWest` always holds the minimum latitude and
    /// longitude and `northEast` the maximum.
    public MapBounds(LatLng southWest, LatLng northEast) {
        double minLat = Math.min(southWest.getLatitude(), northEast.getLatitude());
        double maxLat = Math.max(southWest.getLatitude(), northEast.getLatitude());
        double minLon = Math.min(southWest.getLongitude(), northEast.getLongitude());
        double maxLon = Math.max(southWest.getLongitude(), northEast.getLongitude());
        this.southWest = new LatLng(minLat, minLon);
        this.northEast = new LatLng(maxLat, maxLon);
    }

    /// Builds the smallest bounding box that contains every coordinate in
    /// `coords`. Returns `null` when the list is empty.
    public static MapBounds fromCoordinates(List coords) {
        if (coords == null || coords.isEmpty()) {
            return null;
        }
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        int size = coords.size();
        for (int i = 0; i < size; i++) {
            LatLng c = (LatLng) coords.get(i);
            minLat = Math.min(minLat, c.getLatitude());
            maxLat = Math.max(maxLat, c.getLatitude());
            minLon = Math.min(minLon, c.getLongitude());
            maxLon = Math.max(maxLon, c.getLongitude());
        }
        return new MapBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
    }

    /// The south-west (minimum latitude/longitude) corner.
    public LatLng getSouthWest() {
        return southWest;
    }

    /// The north-east (maximum latitude/longitude) corner.
    public LatLng getNorthEast() {
        return northEast;
    }

    /// The geometric center of this box.
    public LatLng getCenter() {
        return new LatLng((southWest.getLatitude() + northEast.getLatitude()) / 2,
                (southWest.getLongitude() + northEast.getLongitude()) / 2);
    }

    /// Returns true if `point` lies inside this box (inclusive).
    public boolean contains(LatLng point) {
        return point.getLatitude() >= southWest.getLatitude()
                && point.getLatitude() <= northEast.getLatitude()
                && point.getLongitude() >= southWest.getLongitude()
                && point.getLongitude() <= northEast.getLongitude();
    }

    /// Returns a new box that contains both this box and `point`.
    public MapBounds extend(LatLng point) {
        return new MapBounds(
                new LatLng(Math.min(southWest.getLatitude(), point.getLatitude()),
                        Math.min(southWest.getLongitude(), point.getLongitude())),
                new LatLng(Math.max(northEast.getLatitude(), point.getLatitude()),
                        Math.max(northEast.getLongitude(), point.getLongitude())));
    }

    /// The span between the north and south edges in degrees.
    public double getLatitudeSpan() {
        return northEast.getLatitude() - southWest.getLatitude();
    }

    /// The span between the east and west edges in degrees.
    public double getLongitudeSpan() {
        return northEast.getLongitude() - southWest.getLongitude();
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "MapBounds{" + southWest + " -> " + northEast + "}";
    }
}
