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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One leg of a route, from one waypoint to the next, cut into OSRM steps:
/// a step per stretch of one road, each opened by the maneuver onto it.
final class Leg {
    private final OsmExtract x;
    private final int profile;
    private final int start;
    private final int[] edges;
    // Index of the first edge of each step, plus edges.length at the end.
    private final int[] stepStart;
    private final int stepCount;
    final double distance;
    final double duration;

    Leg(OsmExtract x, int profile, int start, int[] edges) {
        this.x = x;
        this.profile = profile;
        this.start = start;
        this.edges = edges;
        int[] starts = new int[edges.length + 1];
        int n = 0;
        double d = 0;
        double t = 0;
        for (int i = 0; i < edges.length; i++) {
            if (i == 0 || !sameRoad(edges[i - 1], edges[i])) {
                starts[n++] = i;
            }
            d += x.edgeDistDm[edges[i]] / 10.0;
            t += OsmRouting.cost(x, edges[i], profile) / 10.0;
        }
        starts[n] = edges.length;
        stepStart = starts;
        stepCount = n;
        distance = d;
        duration = t;
    }

    private boolean sameRoad(int a, int b) {
        int wa = x.edgeWay[a];
        int wb = x.edgeWay[b];
        if (wa == wb) {
            return true;
        }
        if ((x.wayFlags[wa] & OsmExtract.WAY_ROUNDABOUT) != (x.wayFlags[wb] & OsmExtract.WAY_ROUNDABOUT)) {
            return false;
        }
        if ((x.wayFlags[wa] & OsmExtract.WAY_ROUNDABOUT) != 0) {
            return true;
        }
        String na = x.wayName[wa];
        return na != null && na.equals(x.wayName[wb]);
    }

    private int from(int edge) {
        // The vertex before edges[i] is the target of edges[i - 1], or start.
        return edge == 0 ? start : x.edgeTarget[edges[edge - 1]];
    }

    private int edgeBearing(int i) {
        int a = from(i);
        int b = x.edgeTarget[edges[i]];
        return OsmExtract.bearing(x.lat[a], x.lon[a], x.lat[b], x.lon[b]);
    }

    private int end() {
        return edges.length == 0 ? start : x.edgeTarget[edges[edges.length - 1]];
    }

    void appendPoints(List out, boolean skipFirst) {
        if (!skipFirst) {
            out.add(new int[] {x.lat[start], x.lon[start]});
        }
        for (int e : edges) {
            int v = x.edgeTarget[e];
            out.add(new int[] {x.lat[v], x.lon[v]});
        }
        if (edges.length == 0 && skipFirst) {
            return;
        }
        if (edges.length == 0) {
            out.add(new int[] {x.lat[start], x.lon[start]});
        }
    }

    Map toJson(boolean withSteps, int precision) {
        Map leg = new LinkedHashMap();
        List steps = new ArrayList();
        if (withSteps) {
            for (int s = 0; s < stepCount; s++) {
                steps.add(step(s, precision));
            }
            steps.add(arrive(precision));
            if (stepCount == 0) {
                steps.add(0, depart(precision));
            }
        }
        leg.put("steps", steps);
        leg.put("summary", summary());
        leg.put("weight", OsmRouting.round1(duration));
        leg.put("duration", OsmRouting.round1(duration));
        leg.put("distance", OsmRouting.round1(distance));
        return leg;
    }

    private String nameOf(int edge) {
        String n = x.wayName[x.edgeWay[edge]];
        return n == null ? "" : n;
    }

    private Map step(int s, int precision) {
        int first = stepStart[s];
        int last = stepStart[s + 1];
        List points = new ArrayList();
        int v0 = from(first);
        points.add(new int[] {x.lat[v0], x.lon[v0]});
        double d = 0;
        double t = 0;
        for (int i = first; i < last; i++) {
            int v = x.edgeTarget[edges[i]];
            points.add(new int[] {x.lat[v], x.lon[v]});
            d += x.edgeDistDm[edges[i]] / 10.0;
            t += OsmRouting.cost(x, edges[i], profile) / 10.0;
        }
        int after = edgeBearing(first);
        Map maneuver = new LinkedHashMap();
        boolean roundabout = (x.wayFlags[x.edgeWay[edges[first]]] & OsmExtract.WAY_ROUNDABOUT) != 0;
        if (s == 0) {
            maneuver.put("type", "depart");
            maneuver.put("bearing_before", Integer.valueOf(0));
        } else {
            int before = edgeBearing(first - 1);
            boolean leavingRoundabout = (x.wayFlags[x.edgeWay[edges[first - 1]]] & OsmExtract.WAY_ROUNDABOUT) != 0;
            if (roundabout) {
                int exitBearing = edgeBearing(last - 1);
                if (last < edges.length) {
                    exitBearing = edgeBearing(last);
                }
                maneuver.put("type", "roundabout");
                maneuver.put("modifier", modifier(before, exitBearing));
                maneuver.put("exit", Integer.valueOf(exits(first, last)));
            } else if (leavingRoundabout) {
                maneuver.put("type", "exit roundabout");
                maneuver.put("modifier", "straight");
            } else {
                String m = modifier(before, after);
                maneuver.put("type", "straight".equals(m) ? "new name" : "turn");
                maneuver.put("modifier", m);
            }
            maneuver.put("bearing_before", Integer.valueOf(before));
        }
        maneuver.put("bearing_after", Integer.valueOf(after));
        maneuver.put("location", OsmRouting.location(x.lat[v0], x.lon[v0]));
        Map step = new LinkedHashMap();
        step.put("geometry", Polyline.encode(points, precision));
        step.put("maneuver", maneuver);
        step.put("mode", mode());
        step.put("name", nameOf(edges[first]));
        String ref = x.wayRef[x.edgeWay[edges[first]]];
        if (ref != null) {
            step.put("ref", ref);
        }
        step.put("weight", OsmRouting.round1(t));
        step.put("duration", OsmRouting.round1(t));
        step.put("distance", OsmRouting.round1(d));
        return step;
    }

    // Junctions passed inside a roundabout, the one taken included.
    private int exits(int first, int last) {
        int bit = 1 << profile;
        int count = 0;
        for (int i = first; i < last; i++) {
            int v = x.edgeTarget[edges[i]];
            int out = 0;
            for (int e = x.edgeStart[v]; e < x.edgeStart[v + 1]; e++) {
                if ((x.edgeMask[e] & bit) != 0
                        && (x.wayFlags[x.edgeWay[e]] & OsmExtract.WAY_ROUNDABOUT) == 0) {
                    out++;
                }
            }
            if (out > 0) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private Map depart(int precision) {
        return endpoint("depart", start, precision);
    }

    private Map arrive(int precision) {
        Map m = endpoint("arrive", end(), precision);
        if (edges.length > 0) {
            ((Map) m.get("maneuver")).put("bearing_before",
                    Integer.valueOf(edgeBearing(edges.length - 1)));
            m.put("name", nameOf(edges[edges.length - 1]));
        }
        return m;
    }

    private Map endpoint(String type, int v, int precision) {
        List points = new ArrayList();
        points.add(new int[] {x.lat[v], x.lon[v]});
        points.add(new int[] {x.lat[v], x.lon[v]});
        Map maneuver = new LinkedHashMap();
        maneuver.put("type", type);
        maneuver.put("bearing_before", Integer.valueOf(0));
        maneuver.put("bearing_after", Integer.valueOf(0));
        maneuver.put("location", OsmRouting.location(x.lat[v], x.lon[v]));
        Map step = new LinkedHashMap();
        step.put("geometry", Polyline.encode(points, precision));
        step.put("maneuver", maneuver);
        step.put("mode", mode());
        step.put("name", "");
        step.put("weight", OsmRouting.round1(0));
        step.put("duration", OsmRouting.round1(0));
        step.put("distance", OsmRouting.round1(0));
        return step;
    }

    private String mode() {
        return profile == OsmExtract.CAR ? "driving" : profile == OsmExtract.BIKE ? "cycling" : "walking";
    }

    /// OSRM's turn modifier for a change of heading.
    static String modifier(int before, int after) {
        int d = ((after - before) % 360 + 540) % 360 - 180;
        int a = Math.abs(d);
        if (a < 20) {
            return "straight";
        }
        if (a >= 170) {
            return "uturn";
        }
        String side = d > 0 ? "right" : "left";
        if (a < 60) {
            return "slight " + side;
        }
        if (a < 140) {
            return side;
        }
        return "sharp " + side;
    }

    // The names of the two longest named steps, in route order.
    private String summary() {
        int bestA = -1;
        int bestB = -1;
        double lenA = 0;
        double lenB = 0;
        for (int s = 0; s < stepCount; s++) {
            if (nameOf(edges[stepStart[s]]).length() == 0) {
                continue;
            }
            double len = 0;
            for (int i = stepStart[s]; i < stepStart[s + 1]; i++) {
                len += x.edgeDistDm[edges[i]];
            }
            if (bestA < 0 || len > lenA) {
                bestB = bestA;
                lenB = lenA;
                bestA = s;
                lenA = len;
            } else if (bestB < 0 || len > lenB) {
                bestB = s;
                lenB = len;
            }
        }
        if (bestA < 0) {
            return "";
        }
        String a = nameOf(edges[stepStart[bestA]]);
        if (bestB < 0 || nameOf(edges[stepStart[bestB]]).equals(a)) {
            return a;
        }
        String b = nameOf(edges[stepStart[bestB]]);
        return bestA < bestB ? a + ", " + b : b + ", " + a;
    }
}
