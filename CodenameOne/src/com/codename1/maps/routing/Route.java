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
package com.codename1.maps.routing;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.maps.Polyline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A road-following journey returned by a [RouteService]: the geometry that
/// traces the actual roads, how long it is, how long it takes, and the legs
/// and turn-by-turn steps that make it up.
///
/// Drawing it is one call -- unlike handing raw endpoints to a
/// [Polyline], which would just join them with a straight line:
///
/// ```java
/// map.addPolyline(route.toPolyline());
/// map.fitBounds(route.getBounds(), 40);
/// ```
public final class Route {

    private final List points;
    private final List legs;
    private final double distanceMeters;
    private final double durationSeconds;
    private final String summary;
    private MapBounds bounds;

    /// Creates a route. Called by [RouteService] implementations.
    ///
    /// #### Parameters
    ///
    /// - `points`: the [LatLng] road geometry, defensively copied
    ///
    /// - `legs`: the [RouteLeg]s between consecutive stops, defensively copied
    ///
    /// - `distanceMeters`: the total length in meters
    ///
    /// - `durationSeconds`: the total estimated travel time in seconds
    ///
    /// - `summary`: a short human readable description of the route
    public Route(List points, List legs, double distanceMeters, double durationSeconds,
                 String summary) {
        this.points = points == null ? new ArrayList() : new ArrayList(points);
        this.legs = legs == null ? new ArrayList() : new ArrayList(legs);
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.summary = summary == null ? "" : summary;
    }

    /// The unmodifiable road geometry as [LatLng] vertices, dense enough to
    /// trace the shape of the roads travelled.
    public List getPoints() {
        return Collections.unmodifiableList(points);
    }

    /// The unmodifiable [RouteLeg]s, one per pair of consecutive stops.
    public List getLegs() {
        return Collections.unmodifiableList(legs);
    }

    /// The total length of the route in meters.
    public double getDistanceMeters() {
        return distanceMeters;
    }

    /// The total estimated travel time in seconds.
    public double getDurationSeconds() {
        return durationSeconds;
    }

    /// A short human readable description, typically the major roads used.
    public String getSummary() {
        return summary;
    }

    /// The smallest [MapBounds] containing the whole route, or `null` when the
    /// route has no geometry. Pass it to
    /// [com.codename1.maps.MapSurface#fitBounds] to frame the journey.
    public MapBounds getBounds() {
        if (bounds == null) {
            bounds = MapBounds.fromCoordinates(points);
        }
        return bounds;
    }

    /// Builds a [Polyline] tracing this route, ready to hand to
    /// [com.codename1.maps.MapSurface#addPolyline]. Each call returns a fresh
    /// polyline, so styling one does not affect another.
    public Polyline toPolyline() {
        Polyline pl = new Polyline();
        for (Object point : points) {
            pl.addPoint((LatLng) point);
        }
        return pl;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "Route{" + distanceMeters + "m, " + durationSeconds + "s, "
                + points.size() + " point(s)}";
    }
}
