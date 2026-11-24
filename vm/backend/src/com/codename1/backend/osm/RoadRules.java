/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.backend.osm;

/// Which ways each travel mode may use, in which direction, and how fast a
/// car goes on them. The rules follow the common OpenStreetMap tagging
/// defaults (the wiki's "OSM tags for routing" pages), not any one country's
/// law; a tag a mapper set explicitly always wins over the default.
final class RoadRules {
    private static final String[] CAR_HIGHWAYS = {
        "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link",
        "secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified",
        "residential", "living_street", "service", "road"
    };
    private static final int[] CAR_SPEEDS = {
        100, 60, 80, 50, 60, 45, 50, 40, 40, 35, 30, 30, 10, 15, 30
    };
    private static final String[] PATHS = {
        "cycleway", "path", "footway", "pedestrian", "steps", "track", "bridleway", "corridor"
    };

    /// The fastest car speed [#carSpeed] can answer, which keeps the A*
    /// estimate below the true cost.
    static final int MAX_CAR_SPEED = 100;
    static final int BIKE_SPEED = 16;
    static final int FOOT_SPEED = 5;

    private RoadRules() {
    }

    /// Profile mask bits for the way's direction (low three bits) and against
    /// it (the next three); 0 when no profile may use the way at all.
    static int access(String highway, String[] tags, int count) {
        boolean carRoad = indexOf(CAR_HIGHWAYS, highway) >= 0;
        boolean path = indexOf(PATHS, highway) >= 0;
        if (!carRoad && !path) {
            return 0;
        }
        String access = OsmExtract.tag(tags, count, "access");
        boolean closed = "no".equals(access) || "private".equals(access);
        String motorcar = OsmExtract.tag(tags, count, "motorcar");
        String motorVehicle = OsmExtract.tag(tags, count, "motor_vehicle");
        boolean car;
        if (!carRoad) {
            car = yes(motorcar) && !"steps".equals(highway);
        } else if (yes(motorcar) || motorcar == null && yes(motorVehicle)) {
            car = true;
        } else {
            car = !closed && !"no".equals(motorcar) && !"private".equals(motorcar)
                    && !"no".equals(motorVehicle) && !"private".equals(motorVehicle);
        }
        String bicycle = OsmExtract.tag(tags, count, "bicycle");
        boolean bike;
        if (yes(bicycle)) {
            bike = true;
        } else if ("no".equals(bicycle) || "use_sidepath".equals(bicycle) || closed) {
            bike = false;
        } else {
            bike = !highway.startsWith("motorway") && !"footway".equals(highway)
                    && !"pedestrian".equals(highway) && !"steps".equals(highway)
                    && !"corridor".equals(highway) && !"bridleway".equals(highway);
        }
        String foot = OsmExtract.tag(tags, count, "foot");
        boolean walk;
        if (yes(foot)) {
            walk = true;
        } else if ("no".equals(foot) || "use_sidepath".equals(foot) || closed) {
            walk = false;
        } else {
            walk = !highway.startsWith("motorway") && !highway.startsWith("trunk");
        }
        String oneway = OsmExtract.tag(tags, count, "oneway");
        boolean roundabout = "roundabout".equals(OsmExtract.tag(tags, count, "junction"))
                || "circular".equals(OsmExtract.tag(tags, count, "junction"));
        boolean forwardOnly = yes(oneway) || roundabout && !"no".equals(oneway)
                || "motorway".equals(highway) && !"no".equals(oneway);
        boolean backwardOnly = "-1".equals(oneway);
        String bikeOneway = OsmExtract.tag(tags, count, "oneway:bicycle");
        String cycleway = OsmExtract.tag(tags, count, "cycleway");
        boolean bikeBothWays = "no".equals(bikeOneway)
                || cycleway != null && cycleway.startsWith("opposite");
        int fwd = 0;
        int back = 0;
        if (car) {
            fwd |= backwardOnly ? 0 : OsmExtract.MASK_CAR;
            back |= forwardOnly ? 0 : OsmExtract.MASK_CAR;
        }
        if (bike) {
            fwd |= backwardOnly && !bikeBothWays ? 0 : OsmExtract.MASK_BIKE;
            back |= forwardOnly && !bikeBothWays ? 0 : OsmExtract.MASK_BIKE;
        }
        if (walk) {
            fwd |= OsmExtract.MASK_FOOT;
            back |= OsmExtract.MASK_FOOT;
        }
        return fwd | back << 3;
    }

    /// Car speed in km/h: the class default, or a lower posted `maxspeed`.
    static int carSpeed(String highway, String[] tags, int count) {
        int at = indexOf(CAR_HIGHWAYS, highway);
        int speed = at < 0 ? 10 : CAR_SPEEDS[at];
        String max = OsmExtract.tag(tags, count, "maxspeed");
        if (max != null) {
            int v = 0;
            int i = 0;
            while (i < max.length() && i < 4 && max.charAt(i) >= '0' && max.charAt(i) <= '9') {
                v = v * 10 + (max.charAt(i) - '0');
                i++;
            }
            if (max.indexOf("mph") >= 0) {
                v = v * 16 / 10;
            }
            if (v >= 5 && v < speed) {
                speed = v;
            }
        }
        return speed;
    }

    private static boolean yes(String v) {
        return "yes".equals(v) || "designated".equals(v) || "permissive".equals(v)
                || "true".equals(v) || "1".equals(v);
    }

    private static int indexOf(String[] values, String v) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(v)) {
                return i;
            }
        }
        return -1;
    }
}
