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

/// A uniform grid over points given in degrees times 1e7, answering the
/// nearest point that a filter accepts. Built once and read concurrently.
final class SpatialGrid {
    // Cells are about 550 m on a side at the equator; widened for a large
    // extract so the cell table stays bounded.
    private static final int BASE_CELL = 50000;
    private static final long MAX_CELLS = 4L * 1024 * 1024;

    /// Decides whether a candidate point may be answered.
    interface Filter {
        boolean accept(int id);
    }

    private final int[] lat;
    private final int[] lon;
    private final int cell;
    private final int minX;
    private final int minY;
    private final int width;
    private final int height;
    private final int[] cellStart;
    private final int[] members;
    private final double cellMeters;

    SpatialGrid(int[] lat, int[] lon, int[] ids) {
        this.lat = lat;
        this.lon = lon;
        int loLat = Integer.MAX_VALUE;
        int hiLat = Integer.MIN_VALUE;
        int loLon = Integer.MAX_VALUE;
        int hiLon = Integer.MIN_VALUE;
        for (int id : ids) {
            loLat = Math.min(loLat, lat[id]);
            hiLat = Math.max(hiLat, lat[id]);
            loLon = Math.min(loLon, lon[id]);
            hiLon = Math.max(hiLon, lon[id]);
        }
        if (ids.length == 0) {
            loLat = 0;
            hiLat = 0;
            loLon = 0;
            hiLon = 0;
        }
        int c = BASE_CELL;
        // Spans in long: an extract across the antimeridian spans 360 degrees,
        // 3.6e9 in E7, which wraps an int.
        while ((((long) hiLat - loLat) / c + 1) * (((long) hiLon - loLon) / c + 1) > MAX_CELLS) {
            c *= 2;
        }
        cell = c;
        minX = floorDiv(loLon, c);
        minY = floorDiv(loLat, c);
        width = floorDiv(hiLon, c) - minX + 1;
        height = floorDiv(hiLat, c) - minY + 1;
        cellStart = new int[width * height + 1];
        for (int id : ids) {
            cellStart[cellOf(id) + 1]++;
        }
        for (int i = 0; i < width * height; i++) {
            cellStart[i + 1] += cellStart[i];
        }
        int[] fill = new int[width * height];
        System.arraycopy(cellStart, 0, fill, 0, fill.length);
        members = new int[ids.length];
        for (int id : ids) {
            members[fill[cellOf(id)]++] = id;
        }
        // The narrowest side of a cell, at the extract's widest latitude.
        double worstLat = Math.max(Math.abs(loLat), Math.abs(hiLat)) / 1e7;
        cellMeters = c / 1e7 * OsmExtract.METERS_PER_DEGREE * Math.max(0.05, Math.cos(Math.toRadians(worstLat)));
    }

    int size() {
        return members.length;
    }

    private int cellOf(int id) {
        return (floorDiv(lat[id], cell) - minY) * width + floorDiv(lon[id], cell) - minX;
    }

    /// The accepted point nearest to (latE7, lonE7) within maxMeters, or -1.
    /// out[0] receives its distance in meters when one is found.
    int nearest(int latE7, int lonE7, double maxMeters, Filter filter, double[] out) {
        int cx = floorDiv(lonE7, cell) - minX;
        int cy = floorDiv(latE7, cell) - minY;
        int best = -1;
        double bestDist = maxMeters;
        int maxRing = (int) Math.min(Math.max(width, height) + 1, maxMeters / cellMeters + 2);
        for (int ring = 0; ring <= maxRing; ring++) {
            for (int y = cy - ring; y <= cy + ring; y++) {
                if (y < 0 || y >= height) {
                    continue;
                }
                boolean edgeRow = y == cy - ring || y == cy + ring;
                int step = edgeRow ? 1 : Math.max(1, 2 * ring);
                for (int x = cx - ring; x <= cx + ring; x += step) {
                    if (x < 0 || x >= width) {
                        continue;
                    }
                    int c = y * width + x;
                    for (int i = cellStart[c]; i < cellStart[c + 1]; i++) {
                        int id = members[i];
                        double d = OsmExtract.distanceMeters(latE7, lonE7, lat[id], lon[id]);
                        if (d <= bestDist && (filter == null || filter.accept(id))) {
                            bestDist = d;
                            best = id;
                        }
                    }
                }
            }
            // Every point of the next ring is at least this far away.
            if (best >= 0 && bestDist <= ring * cellMeters) {
                break;
            }
        }
        if (best >= 0 && out != null) {
            out[0] = bestDist;
        }
        return best;
    }

    // vm/JavaAPI has no Math.floorDiv.
    private static int floorDiv(int x, int y) {
        int q = x / y;
        return (x % y != 0 && (x < 0) != (y < 0)) ? q - 1 : q;
    }
}
