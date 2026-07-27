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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Describes the journey to route: where it starts, where it ends, anything it
/// must pass through on the way, and how the traveller moves.
///
/// The setters return `this` so a request reads as one expression:
///
/// ```java
/// RouteRequest req = new RouteRequest(home, office)
///         .addWaypoint(daycare)
///         .setTravelMode(TravelMode.DRIVING)
///         .setAlternatives(true);
/// ```
public final class RouteRequest {

    private final LatLng origin;
    private final LatLng destination;
    private final List waypoints = new ArrayList();
    private TravelMode travelMode = TravelMode.DRIVING;
    private boolean alternatives;
    private boolean steps = true;

    /// Creates a request for the direct journey from `origin` to
    /// `destination`.
    public RouteRequest(LatLng origin, LatLng destination) {
        this.origin = origin;
        this.destination = destination;
    }

    /// Where the journey starts.
    public LatLng getOrigin() {
        return origin;
    }

    /// Where the journey ends.
    public LatLng getDestination() {
        return destination;
    }

    /// Adds an intermediate point the route must pass through, in visiting
    /// order. Backends route through waypoints in the order added; none of
    /// them reorder to optimize the trip.
    public RouteRequest addWaypoint(LatLng waypoint) {
        if (waypoint != null) {
            waypoints.add(waypoint);
        }
        return this;
    }

    /// The unmodifiable intermediate points ([LatLng]) in visiting order;
    /// empty for a direct journey. Add to it through [#addWaypoint(LatLng)],
    /// which keeps out the `null` and non-[LatLng] entries a
    /// [RouteService] would later choke on.
    public List getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    /// How the traveller moves; [TravelMode#DRIVING] unless changed.
    public TravelMode getTravelMode() {
        return travelMode;
    }

    /// Sets how the traveller moves. A `null` mode restores
    /// [TravelMode#DRIVING].
    public RouteRequest setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode == null ? TravelMode.DRIVING : travelMode;
        return this;
    }

    /// Whether the service may return more than one route.
    public boolean isAlternatives() {
        return alternatives;
    }

    /// Asks the service for alternative routes in addition to the best one.
    /// Backends treat this as a hint -- most return a single route when no
    /// meaningfully different alternative exists.
    public RouteRequest setAlternatives(boolean alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    /// Whether turn-by-turn steps are requested; true unless changed.
    public boolean isSteps() {
        return steps;
    }

    /// Requests (or suppresses) turn-by-turn [RouteStep]s. Turn them off when
    /// you only need the line on the map -- the response is much smaller.
    public RouteRequest setSteps(boolean steps) {
        this.steps = steps;
        return this;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "RouteRequest{" + origin + " -> " + destination
                + ", via " + waypoints.size() + " waypoint(s), " + travelMode + "}";
    }
}
