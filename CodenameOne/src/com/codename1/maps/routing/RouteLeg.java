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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The portion of a [Route] between two consecutive stops. A direct journey
/// has a single leg; each [RouteRequest#addWaypoint] adds another.
public final class RouteLeg {

    private final String summary;
    private final double distanceMeters;
    private final double durationSeconds;
    private final List steps;

    /// Creates a leg. Called by [RouteService] implementations.
    ///
    /// #### Parameters
    ///
    /// - `summary`: a short description such as the major roads used
    ///
    /// - `distanceMeters`: the length of the leg in meters
    ///
    /// - `durationSeconds`: the estimated travel time in seconds
    ///
    /// - `steps`: the [RouteStep]s of this leg, defensively copied
    public RouteLeg(String summary, double distanceMeters, double durationSeconds, List steps) {
        this.summary = summary == null ? "" : summary;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.steps = steps == null ? new ArrayList() : new ArrayList(steps);
    }

    /// A short description of the leg, typically the major roads it uses.
    public String getSummary() {
        return summary;
    }

    /// The length of this leg in meters.
    public double getDistanceMeters() {
        return distanceMeters;
    }

    /// The estimated travel time for this leg in seconds.
    public double getDurationSeconds() {
        return durationSeconds;
    }

    /// The unmodifiable list of [RouteStep]s making up this leg. Empty when
    /// the request had [RouteRequest#setSteps] turned off.
    public List getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "RouteLeg{" + distanceMeters + "m, " + steps.size() + " step(s)}";
    }
}
