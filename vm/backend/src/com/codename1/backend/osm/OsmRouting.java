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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Answers turn-by-turn routes in the shape of OSRM's HTTP API, so the
/// client's `OsrmRouteService` -- or any other OSRM client -- can point at
/// this backend instead of a separate routing server.
///
/// Mounted at a prefix, it answers
/// `GET {prefix}/route/v1/{profile}/{lon},{lat};{lon},{lat}[;...]` with the
/// profiles `driving`, `walking` and `cycling` (`car`, `foot` and `bike` are
/// accepted too). The query parameters understood are `steps`
/// (`true`/`false`), `overview` (`full`, `simplified` or `false`; simplified
/// is answered as full), `geometries` (`polyline` or `polyline6`) and
/// `alternatives`, which is accepted and always answered with one route.
///
/// A failure is an HTTP 400 whose JSON body carries OSRM's `code`:
/// `InvalidUrl`, `InvalidQuery`, `InvalidOptions`, `TooBig`, `NoSegment`
/// (a waypoint too far from any road) or `NoRoute`.
///
/// ```java
/// OsmExtract extract = OsmExtract.load("/data/region.osm.pbf");
/// Backend.builder()
///         .handler(new OsmRouting(extract, "/osrm"))
///         .handler(new OsmGeocoding(extract, "/geocode"))
///         .start();
/// ```
public final class OsmRouting implements HttpServer.Handler {
    private static final int MAX_WAYPOINTS = 25;
    private static final double MAX_SNAP_METERS = 5000;
    // Search work per request, in settled vertices; generated code has no safepoint
    // poll, so the loop also yields every YIELD_EVERY of them.
    private static final int MAX_SETTLED = 4000000;
    private static final int YIELD_EVERY = 65536;
    private static final int POOL = 8;

    private final OsmExtract extract;
    private final String prefix;
    private final List pool = new ArrayList();
    private int live;

    /// - `extract`: the loaded extract
    /// - `prefix`: the URL path the handler is mounted at, "" for the root
    public OsmRouting(OsmExtract extract, String prefix) {
        this.extract = extract;
        String p = prefix == null ? "" : prefix;
        this.prefix = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        String path = request.pathFrom(0);
        String head = prefix + "/route/v1/";
        if (!path.startsWith(head)) {
            return null;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            return HttpServer.Response.text(405, "method not allowed");
        }
        String rest = path.substring(head.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return error("InvalidUrl", "Expected /route/v1/{profile}/{coordinates}");
        }
        int profile = profile(rest.substring(0, slash));
        if (profile < 0) {
            return error("InvalidOptions", "Unknown profile " + rest.substring(0, slash));
        }
        String coords = rest.substring(slash + 1);
        if (coords.endsWith(".json")) {
            coords = coords.substring(0, coords.length() - 5);
        }
        int[] points = parseCoordinates(coords);
        if (points == null) {
            return error("InvalidQuery", "Coordinates must be lon,lat pairs separated by ';'");
        }
        int count = points.length / 2;
        if (count < 2) {
            return error("InvalidQuery", "At least two coordinates are needed");
        }
        if (count > MAX_WAYPOINTS) {
            return error("TooBig", "At most " + MAX_WAYPOINTS + " coordinates are accepted");
        }
        boolean steps = "true".equals(request.queryParam("steps"));
        String overview = request.queryParam("overview");
        boolean geometry = !"false".equals(overview);
        String geometries = request.queryParam("geometries");
        int precision;
        if (geometries == null || "polyline".equals(geometries)) {
            precision = 5;
        } else if ("polyline6".equals(geometries)) {
            precision = 6;
        } else {
            return error("InvalidOptions", "Only polyline and polyline6 geometries are supported");
        }
        Map result = route(profile, points, steps, geometry, precision);
        if ("Busy".equals(result.get("error"))) {
            Map busy = new LinkedHashMap();
            busy.put("code", "Busy");
            busy.put("message", result.get("message"));
            return HttpServer.Response.json(503, Json.write(busy));
        }
        if (result.containsKey("error")) {
            return error((String) result.get("error"), (String) result.get("message"));
        }
        return HttpServer.Response.json(200, Json.write(result));
    }

    private static int profile(String name) {
        if ("driving".equals(name) || "car".equals(name)) {
            return OsmExtract.CAR;
        }
        if ("cycling".equals(name) || "bike".equals(name) || "bicycle".equals(name)) {
            return OsmExtract.BIKE;
        }
        if ("walking".equals(name) || "foot".equals(name)) {
            return OsmExtract.FOOT;
        }
        return -1;
    }

    // {lat0, lon0, lat1, lon1, ...} in degrees times 1e7, or null.
    static int[] parseCoordinates(String s) {
        List parts = new ArrayList();
        int at = 0;
        while (at <= s.length()) {
            int semi = s.indexOf(';', at);
            parts.add(semi < 0 ? s.substring(at) : s.substring(at, semi));
            if (semi < 0) {
                break;
            }
            at = semi + 1;
            if (parts.size() > MAX_WAYPOINTS + 1) {
                break;
            }
        }
        int[] out = new int[parts.size() * 2];
        for (int i = 0; i < parts.size(); i++) {
            String p = (String) parts.get(i);
            int comma = p.indexOf(',');
            if (comma < 0) {
                return null;
            }
            long lonE7 = parseDegrees(p.substring(0, comma));
            long latE7 = parseDegrees(p.substring(comma + 1));
            if (lonE7 < -1800000000L || lonE7 > 1800000000L || latE7 < -900000000L || latE7 > 900000000L) {
                return null;
            }
            out[i * 2] = (int) latE7;
            out[i * 2 + 1] = (int) lonE7;
        }
        return out;
    }

    // A decimal number of degrees as degrees times 1e7; out of range when not one.
    private static long parseDegrees(String s) {
        int i = 0;
        boolean negative = false;
        if (s.length() > 0 && (s.charAt(0) == '-' || s.charAt(0) == '+')) {
            negative = s.charAt(0) == '-';
            i = 1;
        }
        long whole = 0;
        int digits = 0;
        while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            whole = whole * 10 + (s.charAt(i) - '0');
            i++;
            digits++;
            if (digits > 3) {
                return Long.MAX_VALUE;
            }
        }
        long frac = 0;
        int fracDigits = 0;
        if (i < s.length() && s.charAt(i) == '.') {
            i++;
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                if (fracDigits < 7) {
                    frac = frac * 10 + (s.charAt(i) - '0');
                    fracDigits++;
                }
                i++;
                digits++;
            }
        }
        if (i != s.length() || digits == 0) {
            return Long.MAX_VALUE;
        }
        while (fracDigits < 7) {
            frac *= 10;
            fracDigits++;
        }
        long v = whole * 10000000L + frac;
        return negative ? -v : v;
    }

    private static HttpServer.Response error(String code, String message) {
        Map doc = new LinkedHashMap();
        doc.put("code", code);
        doc.put("message", message);
        return HttpServer.Response.json(400, Json.write(doc));
    }

    /// The OSRM response document for one route through the points, or a map
    /// holding `error` and `message`.
    Map route(int profile, int[] points, boolean steps, boolean geometry, int precision) {
        int count = points.length / 2;
        int[] snapped = new int[count];
        double[] snapDist = new double[count];
        final int bit = 1 << profile;
        double[] out = new double[1];
        for (int i = 0; i < count; i++) {
            int v = extract.roadGrid.nearest(points[i * 2], points[i * 2 + 1], MAX_SNAP_METERS,
                    new SpatialGrid.Filter() {
                        @Override
                        public boolean accept(int id) {
                            return (extract.inMainComponent[id] & bit) != 0;
                        }
                    }, out);
            if (v < 0) {
                return failure("NoSegment", "Could not find a matching segment for coordinate " + i);
            }
            snapped[i] = v;
            snapDist[i] = out[0];
        }
        Workspace ws = acquire();
        if (ws == null) {
            return failure("Busy", "Too many routes are being computed; try again");
        }
        // One budget for the whole request, not per leg: 24 legs of
        // unreachable targets would otherwise each explore MAX_SETTLED.
        ws.budget = MAX_SETTLED;
        List legs = new ArrayList();
        List allPoints = new ArrayList();
        double totalDistance = 0;
        double totalDuration = 0;
        try {
            for (int i = 0; i + 1 < count; i++) {
                int[] edges = ws.search(extract, profile, snapped[i], snapped[i + 1]);
                if (edges == null && ws.budget < 0) {
                    return failure("TooBig", "The route needs more search than one request may use");
                }
                if (edges == null) {
                    return failure("NoRoute", "No route found between coordinates " + i + " and " + (i + 1));
                }
                Leg leg = new Leg(extract, profile, snapped[i], edges);
                legs.add(leg.toJson(steps, precision));
                totalDistance += leg.distance;
                totalDuration += leg.duration;
                leg.appendPoints(allPoints, !allPoints.isEmpty());
            }
        } finally {
            release(ws);
        }
        Map route = new LinkedHashMap();
        if (geometry) {
            route.put("geometry", Polyline.encode(allPoints, precision));
        }
        route.put("legs", legs);
        route.put("distance", round1(totalDistance));
        route.put("duration", round1(totalDuration));
        route.put("weight_name", "duration");
        route.put("weight", round1(totalDuration));
        List routes = new ArrayList();
        routes.add(route);
        List waypoints = new ArrayList();
        for (int i = 0; i < count; i++) {
            Map w = new LinkedHashMap();
            w.put("name", nameAt(snapped[i], profile));
            w.put("location", location(extract.lat[snapped[i]], extract.lon[snapped[i]]));
            w.put("distance", round1(snapDist[i]));
            waypoints.add(w);
        }
        Map doc = new LinkedHashMap();
        doc.put("code", "Ok");
        doc.put("routes", routes);
        doc.put("waypoints", waypoints);
        return doc;
    }

    private String nameAt(int v, int profile) {
        int bit = 1 << profile;
        for (int e = extract.edgeStart[v]; e < extract.edgeStart[v + 1]; e++) {
            if ((extract.edgeMask[e] & bit) != 0) {
                String n = extract.wayName[extract.edgeWay[e]];
                return n == null ? "" : n;
            }
        }
        return "";
    }

    private static Map failure(String code, String message) {
        Map m = new LinkedHashMap();
        m.put("error", code);
        m.put("message", message);
        return m;
    }

    static List location(int latE7, int lonE7) {
        List l = new ArrayList();
        l.add(Double.valueOf(lonE7 / 1e7));
        l.add(Double.valueOf(latE7 / 1e7));
        return l;
    }

    static Double round1(double v) {
        return Double.valueOf(Math.round(v * 10) / 10.0);
    }

    // A workspace is four ints per road vertex, so the number alive at once is
    // capped: a burst of requests past it is answered 503 rather than
    // allocating a country-sized workspace per request.
    private Workspace acquire() {
        synchronized (pool) {
            if (!pool.isEmpty()) {
                return (Workspace) pool.remove(pool.size() - 1);
            }
            if (live >= POOL) {
                return null;
            }
            live++;
        }
        return new Workspace(extract.nodeIds.length);
    }

    private void release(Workspace ws) {
        synchronized (pool) {
            pool.add(ws);
        }
    }

    /// Edge cost in tenths of a second for a profile; never 0.
    static int cost(OsmExtract x, int edge, int profile) {
        int speed;
        if (profile == OsmExtract.CAR) {
            speed = x.waySpeedKmh[x.edgeWay[edge]] & 0xff;
        } else if (profile == OsmExtract.BIKE) {
            speed = RoadRules.BIKE_SPEED;
        } else {
            speed = RoadRules.FOOT_SPEED;
        }
        if (speed <= 0) {
            speed = 5;
        }
        return (int) ((long) x.edgeDistDm[edge] * 36 / ((long) speed * 10)) + 1;
    }

    /// Reusable A* state, one per concurrent search. A stamp per vertex marks
    /// which entries belong to the current search, so nothing is cleared
    /// between searches.
    static final class Workspace {
        private final int[] g;
        private final int[] seen;
        private final int[] closed;
        private final int[] parentEdge;
        private long[] heap = new long[1024];
        private int heapSize;
        private int stamp;
        int budget;

        Workspace(int n) {
            g = new int[n];
            seen = new int[n];
            closed = new int[n];
            parentEdge = new int[n];
        }

        // The edges from source to target in order, or null when unreachable.
        int[] search(OsmExtract x, int profile, int source, int target) {
            if (stamp == Integer.MAX_VALUE) {
                java.util.Arrays.fill(seen, 0);
                java.util.Arrays.fill(closed, 0);
                stamp = 0;
            }
            stamp++;
            if (source == target) {
                return new int[0];
            }
            int bit = 1 << profile;
            int maxSpeed = profile == OsmExtract.CAR ? RoadRules.MAX_CAR_SPEED
                    : profile == OsmExtract.BIKE ? RoadRules.BIKE_SPEED : RoadRules.FOOT_SPEED;
            int tLat = x.lat[target];
            int tLon = x.lon[target];
            heapSize = 0;
            g[source] = 0;
            seen[source] = stamp;
            parentEdge[source] = -1;
            push(estimate(x, source, tLat, tLon, maxSpeed), source);
            int settled = 0;
            while (heapSize > 0) {
                long top = pop();
                int v = (int) top;
                if (closed[v] == stamp) {
                    continue;
                }
                closed[v] = stamp;
                if (v == target) {
                    return path(x, target);
                }
                settled++;
                budget--;
                if (budget < 0) {
                    return null;
                }
                if (settled % YIELD_EVERY == 0) {
                    Thread.yield();
                }
                int gv = g[v];
                for (int e = x.edgeStart[v]; e < x.edgeStart[v + 1]; e++) {
                    if ((x.edgeMask[e] & bit) == 0) {
                        continue;
                    }
                    int t = x.edgeTarget[e];
                    if (closed[t] == stamp) {
                        continue;
                    }
                    int ng = gv + cost(x, e, profile);
                    if (seen[t] != stamp || ng < g[t]) {
                        seen[t] = stamp;
                        g[t] = ng;
                        parentEdge[t] = e;
                        push(ng + estimate(x, t, tLat, tLon, maxSpeed), t);
                    }
                }
            }
            return null;
        }

        private static int estimate(OsmExtract x, int v, int tLat, int tLon, int maxSpeed) {
            double meters = OsmExtract.distanceMeters(x.lat[v], x.lon[v], tLat, tLon);
            // Nine tenths of the straight line at top speed: below every real cost.
            return (int) (meters * 10 * 36 / (maxSpeed * 10) * 0.9);
        }

        private int[] path(OsmExtract x, int target) {
            int n = 0;
            for (int v = target; parentEdge[v] >= 0; v = source(x, parentEdge[v])) {
                n++;
            }
            int[] edges = new int[n];
            int v = target;
            for (int i = n - 1; i >= 0; i--) {
                edges[i] = parentEdge[v];
                v = source(x, parentEdge[v]);
            }
            return edges;
        }

        // The vertex an edge leaves; edges are stored by source, so search.
        private static int source(OsmExtract x, int edge) {
            int lo = 0;
            int hi = x.edgeStart.length - 2;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (x.edgeStart[mid] <= edge) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            return lo;
        }

        private void push(int f, int v) {
            if (heapSize == heap.length) {
                long[] grown = new long[heap.length * 2];
                System.arraycopy(heap, 0, grown, 0, heapSize);
                heap = grown;
            }
            long key = (long) f << 32 | (v & 0xffffffffL);
            int i = heapSize++;
            while (i > 0) {
                int parent = (i - 1) >>> 1;
                if (heap[parent] <= key) {
                    break;
                }
                heap[i] = heap[parent];
                i = parent;
            }
            heap[i] = key;
        }

        private long pop() {
            long top = heap[0];
            long last = heap[--heapSize];
            int i = 0;
            while (true) {
                int child = i * 2 + 1;
                if (child >= heapSize) {
                    break;
                }
                if (child + 1 < heapSize && heap[child + 1] < heap[child]) {
                    child++;
                }
                if (heap[child] >= last) {
                    break;
                }
                heap[i] = heap[child];
                i = child;
            }
            if (heapSize > 0) {
                heap[i] = last;
            }
            return top;
        }
    }
}
