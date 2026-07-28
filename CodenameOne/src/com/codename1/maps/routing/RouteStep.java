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

/// One maneuver of a [RouteLeg] -- "turn left onto Elm Street and continue for
/// 300 m" -- together with the piece of road geometry it covers.
///
/// Instances are immutable and are produced by a [RouteService]; applications
/// read them to build a turn-by-turn list beside the map.
public final class RouteStep {

    private final String instruction;
    private final String roadName;
    private final double distanceMeters;
    private final double durationSeconds;
    private final LatLng start;
    private final List points;

    /// Creates a step. Called by [RouteService] implementations.
    ///
    /// #### Parameters
    ///
    /// - `instruction`: a human readable maneuver description
    ///
    /// - `roadName`: the road this step travels along, possibly empty
    ///
    /// - `distanceMeters`: the length of the step in meters
    ///
    /// - `durationSeconds`: the estimated time for the step in seconds
    ///
    /// - `start`: where the maneuver happens
    ///
    /// - `points`: the [LatLng] geometry of the step, defensively copied
    public RouteStep(String instruction, String roadName, double distanceMeters,
                     double durationSeconds, LatLng start, List points) {
        this.instruction = instruction == null ? "" : instruction;
        this.roadName = roadName == null ? "" : roadName;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.start = start;
        this.points = points == null ? new ArrayList() : new ArrayList(points);
    }

    /// A human readable description of the maneuver, for example
    /// `"Turn left onto Elm Street"`. Never `null`, but may be empty when the
    /// backend supplies no phrasing.
    public String getInstruction() {
        return instruction;
    }

    /// The name of the road travelled during this step; empty when unnamed.
    public String getRoadName() {
        return roadName;
    }

    /// The length of this step in meters.
    public double getDistanceMeters() {
        return distanceMeters;
    }

    /// The estimated time for this step in seconds.
    public double getDurationSeconds() {
        return durationSeconds;
    }

    /// Where the maneuver takes place, or `null` when the backend omits it.
    public LatLng getStart() {
        return start;
    }

    /// The unmodifiable [LatLng] geometry travelled by this step, useful to
    /// highlight the upcoming maneuver on the map.
    public List getPoints() {
        return Collections.unmodifiableList(points);
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "RouteStep{" + instruction + ", " + distanceMeters + "m}";
    }
}
