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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A road network and a place index built from one OpenStreetMap `.osm.pbf`
/// extract, shared by [OsmRouting] and [OsmGeocoding].
///
/// Everything is read into memory once, at [#load(String)], and is immutable
/// afterwards, so any number of request threads can use one extract. The
/// structures are compact arrays, sized for a city, a region or a small
/// country: Monaco loads in well under a second, and a dense region of a few
/// million road nodes needs a few hundred megabytes of heap. A whole
/// continent is out of scope; cut an extract with `osmium extract` instead.
///
/// The data is OpenStreetMap's, under the Open Database License: an
/// application that answers routes or places from it must credit
/// "OpenStreetMap contributors" where its users see the results.
public final class OsmExtract {
    static final int CAR = 0;
    static final int BIKE = 1;
    static final int FOOT = 2;
    static final int PROFILES = 3;

    // Per-direction edge mask bits, one per profile.
    static final int MASK_CAR = 1;
    static final int MASK_BIKE = 2;
    static final int MASK_FOOT = 4;

    static final int WAY_ROUNDABOUT = 1;

    static final double METERS_PER_DEGREE = 111195.0;

    // Sorted ids of every node the extract needs, and their coordinates in
    // degrees times 1e7; a node absent from the file keeps HAVE unset.
    final long[] nodeIds;
    final int[] lat;
    final int[] lon;
    final boolean[] have;

    // Compressed adjacency: the edges leaving vertex v are
    // edgeStart[v] .. edgeStart[v + 1] - 1.
    final int[] edgeStart;
    final int[] edgeTarget;
    final int[] edgeDistDm;
    final int[] edgeWay;
    final byte[] edgeMask;

    final String[] wayName;
    final String[] wayRef;
    final byte[] waySpeedKmh;
    final byte[] wayFlags;

    // Bit p set when the vertex is in profile p's largest connected component.
    final byte[] inMainComponent;

    final SpatialGrid roadGrid;
    final GeocodeIndex places;

    private OsmExtract(Loader l) {
        nodeIds = l.ids;
        lat = l.lat;
        lon = l.lon;
        have = l.have;
        wayName = (String[]) l.wayNames.toArray(new String[l.wayNames.size()]);
        wayRef = (String[]) l.wayRefs.toArray(new String[l.wayRefs.size()]);
        waySpeedKmh = Arrays.copyOf(l.waySpeed, l.wayCount);
        wayFlags = Arrays.copyOf(l.wayFlagList, l.wayCount);
        int n = nodeIds.length;
        int[] degree = new int[n + 1];
        // Two passes over the ways: count, then fill.
        for (int pass = 0; pass < 2; pass++) {
            int[] fill = pass == 1 ? new int[n] : null;
            int[] targets = null;
            int[] dists = null;
            int[] ways = null;
            byte[] masks = null;
            if (pass == 1) {
                for (int v = 0; v < n; v++) {
                    degree[v + 1] += degree[v];
                }
                int total = degree[n];
                targets = new int[total];
                dists = new int[total];
                ways = new int[total];
                masks = new byte[total];
                System.arraycopy(degree, 0, fill, 0, n);
            }
            for (int w = 0; w < l.wayCount; w++) {
                int access = l.wayAccess[w];
                int from = l.wayStart[w];
                int to = l.wayStart[w + 1];
                for (int i = from; i + 1 < to; i++) {
                    int a = Arrays.binarySearch(nodeIds, l.refs[i]);
                    int b = Arrays.binarySearch(nodeIds, l.refs[i + 1]);
                    if (a < 0 || b < 0 || a == b || !have[a] || !have[b]) {
                        continue;
                    }
                    int fwd = access & 7;
                    int back = (access >> 3) & 7;
                    if (pass == 0) {
                        if (fwd != 0) {
                            degree[a + 1]++;
                        }
                        if (back != 0) {
                            degree[b + 1]++;
                        }
                        continue;
                    }
                    int dist = Math.max(1, (int) (distanceMeters(lat[a], lon[a], lat[b], lon[b]) * 10));
                    if (fwd != 0) {
                        int e = fill[a]++;
                        targets[e] = b;
                        dists[e] = dist;
                        ways[e] = w;
                        masks[e] = (byte) fwd;
                    }
                    if (back != 0) {
                        int e = fill[b]++;
                        targets[e] = a;
                        dists[e] = dist;
                        ways[e] = w;
                        masks[e] = (byte) back;
                    }
                }
            }
            if (pass == 1) {
                l.edgeTargets = targets;
                l.edgeDists = dists;
                l.edgeWays = ways;
                l.edgeMasks = masks;
            }
        }
        edgeStart = degree;
        edgeTarget = l.edgeTargets;
        edgeDistDm = l.edgeDists;
        edgeWay = l.edgeWays;
        edgeMask = l.edgeMasks;
        inMainComponent = components();
        int[] roadVertices = new int[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if (inMainComponent[v] != 0) {
                roadVertices[count++] = v;
            }
        }
        roadGrid = new SpatialGrid(lat, lon, Arrays.copyOf(roadVertices, count));
        places = new GeocodeIndex(l.entries);
    }

    /// Reads an extract. Both the router and the geocoder are built from it.
    ///
    /// @param path the `.osm.pbf` file
    /// @return the loaded extract
    /// @throws IOException when the file is missing, is not an OSM PBF file,
    /// or uses a blob compression other than zlib
    public static OsmExtract load(String path) throws IOException {
        final Loader l = new Loader();
        OsmPbfReader.read(path, new OsmPbfReader.Sink() {
            @Override
            public boolean wantsNodes() {
                return false;
            }

            @Override

            public boolean wantsWays() {
                return true;
            }

            @Override

            public void node(long id, int latE7, int lonE7, String[] tags, int tagCount) {
            }

            @Override

            public void way(long id, long[] refs, int refCount, String[] tags, int tagCount) {
                l.way(id, refs, refCount, tags, tagCount);
            }
        });
        l.sortNeeded();
        OsmPbfReader.read(path, new OsmPbfReader.Sink() {
            @Override
            public boolean wantsNodes() {
                return true;
            }

            @Override

            public boolean wantsWays() {
                return false;
            }

            @Override

            public void node(long id, int latE7, int lonE7, String[] tags, int tagCount) {
                l.node(id, latE7, lonE7, tags, tagCount);
            }

            @Override

            public void way(long id, long[] refs, int refCount, String[] tags, int tagCount) {
            }
        });
        l.resolvePending();
        if (l.wayCount == 0 && l.entries.isEmpty()) {
            throw new IOException("The extract holds no roads and no places: " + path);
        }
        return new OsmExtract(l);
    }

    /// The number of road vertices a route can be snapped to.
    /// @return the count
    public int getRoadVertexCount() {
        return roadGrid.size();
    }

    /// The number of places and addresses the geocoder can find.
    /// @return the count
    public int getPlaceCount() {
        return places.size();
    }

    // Labels profile by profile, treating every edge the profile may use as
    // two-way, and keeps the largest component of each. Snapping only into it
    // keeps a route from starting on an island of private driveway.
    private byte[] components() {
        int n = nodeIds.length;
        byte[] out = new byte[n];
        int[] label = new int[n];
        int[] queue = new int[Math.max(1, n)];
        // Reverse adjacency for the undirected walk.
        int[] revStart = new int[n + 1];
        for (int target : edgeTarget) {
            revStart[target + 1]++;
        }
        for (int v = 0; v < n; v++) {
            revStart[v + 1] += revStart[v];
        }
        int[] revSource = new int[edgeTarget.length];
        int[] revEdge = new int[edgeTarget.length];
        int[] fill = Arrays.copyOf(revStart, n);
        for (int v = 0; v < n; v++) {
            for (int e = edgeStart[v]; e < edgeStart[v + 1]; e++) {
                int slot = fill[edgeTarget[e]]++;
                revSource[slot] = v;
                revEdge[slot] = e;
            }
        }
        for (int p = 0; p < PROFILES; p++) {
            int bit = 1 << p;
            Arrays.fill(label, 0);
            int next = 0;
            int best = 0;
            int bestSize = 0;
            for (int s = 0; s < n; s++) {
                if (label[s] != 0 || !hasEdge(s, bit, revStart, revEdge)) {
                    continue;
                }
                next++;
                int head = 0;
                int tail = 0;
                queue[tail++] = s;
                label[s] = next;
                while (head < tail) {
                    int v = queue[head++];
                    for (int e = edgeStart[v]; e < edgeStart[v + 1]; e++) {
                        int t = edgeTarget[e];
                        if ((edgeMask[e] & bit) != 0 && label[t] == 0) {
                            label[t] = next;
                            queue[tail++] = t;
                        }
                    }
                    for (int r = revStart[v]; r < revStart[v + 1]; r++) {
                        int t = revSource[r];
                        if ((edgeMask[revEdge[r]] & bit) != 0 && label[t] == 0) {
                            label[t] = next;
                            queue[tail++] = t;
                        }
                    }
                }
                if (tail > bestSize) {
                    bestSize = tail;
                    best = next;
                }
            }
            for (int v = 0; v < n; v++) {
                if (best != 0 && label[v] == best) {
                    out[v] |= (byte) bit;
                }
            }
        }
        return out;
    }

    private boolean hasEdge(int v, int bit, int[] revStart, int[] revEdge) {
        for (int e = edgeStart[v]; e < edgeStart[v + 1]; e++) {
            if ((edgeMask[e] & bit) != 0) {
                return true;
            }
        }
        for (int r = revStart[v]; r < revStart[v + 1]; r++) {
            if ((edgeMask[revEdge[r]] & bit) != 0) {
                return true;
            }
        }
        return false;
    }

    /// Equirectangular distance in meters between two points given in degrees
    /// times 1e7; exact enough at the scale of a road segment or a city.
    static double distanceMeters(int lat1, int lon1, int lat2, int lon2) {
        double meanLat = Math.toRadians((lat1 / 2 + lat2 / 2) / 1e7);
        double dy = (lat2 - (double) lat1) / 1e7;
        double dx = (lon2 - (double) lon1) / 1e7 * Math.cos(meanLat);
        return Math.sqrt(dx * dx + dy * dy) * METERS_PER_DEGREE;
    }

    /// Initial bearing in whole degrees, 0 to 359, north 0, clockwise.
    static int bearing(int lat1, int lon1, int lat2, int lon2) {
        double meanLat = Math.toRadians((lat1 / 2 + lat2 / 2) / 1e7);
        double dy = lat2 - (double) lat1;
        double dx = (lon2 - (double) lon1) * Math.cos(meanLat);
        int deg = (int) Math.round(Math.toDegrees(Math.atan2(dx, dy)));
        return ((deg % 360) + 360) % 360;
    }

    /// Accumulates both passes of [#load(String)].
    private static final class Loader {
        long[] refs = new long[4096];
        int refCount;
        int[] wayStart = new int[1025];
        int[] wayAccess = new int[1024];
        byte[] waySpeed = new byte[1024];
        byte[] wayFlagList = new byte[1024];
        final List wayNames = new ArrayList();
        final List wayRefs = new ArrayList();
        int wayCount;

        // Places waiting for the coordinates of one node: the street's middle
        // node or the building's first one.
        long[] pendingRef = new long[256];
        final List pending = new ArrayList();
        final Set streetSeen = new HashSet();

        long[] needed = new long[4096];
        int neededCount;

        long[] ids;
        int[] lat;
        int[] lon;
        boolean[] have;
        final List entries = new ArrayList();

        int[] edgeTargets;
        int[] edgeDists;
        int[] edgeWays;
        byte[] edgeMasks;

        void way(long id, long[] r, int count, String[] tags, int tagCount) {
            String highway = tag(tags, tagCount, "highway");
            String name = tag(tags, tagCount, "name");
            if (highway != null && count >= 2) {
                int access = RoadRules.access(highway, tags, tagCount);
                if (access != 0) {
                    addWay(r, count, access, RoadRules.carSpeed(highway, tags, tagCount),
                            "roundabout".equals(tag(tags, tagCount, "junction")) ? WAY_ROUNDABOUT : 0,
                            name, tag(tags, tagCount, "ref"));
                }
                if (name != null && GeocodeIndex.isStreet(highway)) {
                    addPending(r[count / 2], GeocodeIndex.street(id, name, highway));
                }
                return;
            }
            if (count >= 1) {
                GeocodeIndex.Entry e = GeocodeIndex.fromTags(false, id, tags, tagCount);
                if (e != null) {
                    addPending(r[0], e);
                }
            }
        }

        private void addWay(long[] r, int count, int access, int speed, int flags, String name,
                String ref) {
            if (wayCount + 1 >= wayAccess.length) {
                int size = wayAccess.length * 2;
                wayAccess = Arrays.copyOf(wayAccess, size);
                waySpeed = Arrays.copyOf(waySpeed, size);
                wayFlagList = Arrays.copyOf(wayFlagList, size);
                wayStart = Arrays.copyOf(wayStart, size + 1);
            }
            if (refCount + count > refs.length) {
                refs = Arrays.copyOf(refs, Math.max(refs.length * 2, refCount + count));
            }
            System.arraycopy(r, 0, refs, refCount, count);
            for (int i = 0; i < count; i++) {
                need(r[i]);
            }
            refCount += count;
            wayAccess[wayCount] = access;
            waySpeed[wayCount] = (byte) speed;
            wayFlagList[wayCount] = (byte) flags;
            wayNames.add(name);
            wayRefs.add(ref);
            wayCount++;
            wayStart[wayCount] = refCount;
        }

        private void addPending(long ref, GeocodeIndex.Entry e) {
            if (pending.size() == pendingRef.length) {
                pendingRef = Arrays.copyOf(pendingRef, pendingRef.length * 2);
            }
            pendingRef[pending.size()] = ref;
            pending.add(e);
            need(ref);
        }

        private void need(long id) {
            if (neededCount == needed.length) {
                needed = Arrays.copyOf(needed, needed.length * 2);
            }
            needed[neededCount++] = id;
        }

        void sortNeeded() {
            long[] all = Arrays.copyOf(needed, neededCount);
            needed = null;
            Arrays.sort(all);
            int unique = 0;
            for (long id : all) {
                if (unique == 0 || all[unique - 1] != id) {
                    all[unique++] = id;
                }
            }
            ids = Arrays.copyOf(all, unique);
            lat = new int[unique];
            lon = new int[unique];
            have = new boolean[unique];
        }

        void node(long id, int latE7, int lonE7, String[] tags, int tagCount) {
            int at = Arrays.binarySearch(ids, id);
            if (at >= 0) {
                lat[at] = latE7;
                lon[at] = lonE7;
                have[at] = true;
            }
            if (tagCount > 0) {
                GeocodeIndex.Entry e = GeocodeIndex.fromTags(true, id, tags, tagCount);
                if (e != null) {
                    e.lat = latE7;
                    e.lon = lonE7;
                    entries.add(e);
                }
            }
        }

        void resolvePending() {
            for (int i = 0; i < pending.size(); i++) { //NOPMD ForLoopCanBeForeach - indexes pendingRef too
                int at = Arrays.binarySearch(ids, pendingRef[i]);
                if (at >= 0 && have[at]) {
                    GeocodeIndex.Entry e = (GeocodeIndex.Entry) pending.get(i);
                    e.lat = lat[at];
                    e.lon = lon[at];
                    // One entry per street name per square kilometre or so,
                    // not one per segment: a long avenue is dozens of ways.
                    if (e.osmType == 'w' && "highway".equals(e.cls)
                            && !streetSeen.add(e.name + "|" + (e.lat / 100000) + "|" + (e.lon / 100000))) {
                        continue;
                    }
                    entries.add(e);
                }
            }
            pending.clear();
            pendingRef = null;
        }
    }

    static String tag(String[] tags, int count, String key) {
        for (int i = 0; i < count; i++) {
            if (key.equals(tags[i * 2])) {
                return tags[i * 2 + 1];
            }
        }
        return null;
    }
}
