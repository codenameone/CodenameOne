/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/// PathfindingService's planner: A* over a grid of walkable cells laid on
/// the tops of collidable parts. Each cell's floor is the highest top a
/// downward probe meets; a step up within the agent's reach is a walk, a
/// higher one a jump, anything taller is a wall.
final class Pathfinder {
    private Pathfinder() {
    }

    /// The agent parameters of `PathfindingService:CreatePath`.
    static final class Agent {
        double radius = 2;
        double height = 5;
        boolean canJump = true;
        double spacing = 4;
    }

    static final double STEP = 2.1;
    static final double JUMP = 7.2;
    static final int MAX_NODES = 20000;
    static final double NO_FLOOR = -Double.MAX_VALUE;

    /// Fills `out` and answers a `PathStatus` item name.
    static String find(Engine e, Agent ag, Vector3 from, Vector3 to, List<QueryParams.Waypoint> out) {
        List<Instance> solids = new ArrayList<Instance>();
        List<Instance> all = Behaviors.parts(e.getWorkspace());
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (p.bool("CanCollide") && p.getParent() != null
                    && p.getParent().findFirstChildOfClass("Humanoid", false) == null) {
                solids.add(p);
            }
        }
        double cell = Math.max(1, ag.spacing);
        int sx = (int) Math.floor(from.x / cell);
        int sz = (int) Math.floor(from.z / cell);
        int gx = (int) Math.floor(to.x / cell);
        int gz = (int) Math.floor(to.z / cell);
        double startFloor = floor(solids, (sx + 0.5) * cell, (sz + 0.5) * cell, from.y + 1);
        if (startFloor == NO_FLOOR) {
            startFloor = from.y - 3;
        }
        double goalFloor = floor(solids, (gx + 0.5) * cell, (gz + 0.5) * cell, to.y + 1);
        Map<Long, double[]> nodes = new HashMap<Long, double[]>();
        // node: {x, z, g, f, parentKey, floor, jump}
        PriorityQueue<double[]> open = new PriorityQueue<double[]>(64, new java.util.Comparator<double[]>() {
            @Override
            public int compare(double[] a, double[] b) {
                return Double.compare(a[3], b[3]);
            }
        });
        double[] start = {sx, sz, 0, h(sx, sz, gx, gz), Double.NaN, startFloor, 0};
        nodes.put(key(sx, sz), start);
        open.add(start);
        double[] best = start;
        int expanded = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        java.util.Set<Long> closed = new java.util.HashSet<Long>();
        double[] goal = null;
        while (!open.isEmpty() && expanded < MAX_NODES) {
            double[] n = open.poll();
            long k = key((int) n[0], (int) n[1]);
            if (!closed.add(Long.valueOf(k))) {
                continue;
            }
            expanded++;
            if (h((int) n[0], (int) n[1], gx, gz) < h((int) best[0], (int) best[1], gx, gz)) {
                best = n;
            }
            if ((int) n[0] == gx && (int) n[1] == gz) {
                goal = n;
                break;
            }
            for (int[] d : dirs) {
                int nx = (int) n[0] + d[0];
                int nz = (int) n[1] + d[1];
                long nk = key(nx, nz);
                if (closed.contains(Long.valueOf(nk))) {
                    continue;
                }
                double f = floor(solids, (nx + 0.5) * cell, (nz + 0.5) * cell, n[5] + JUMP + 0.5);
                if (f == NO_FLOOR) {
                    continue;
                }
                double rise = f - n[5];
                boolean jump = rise > STEP;
                if (jump && (!ag.canJump || rise > JUMP) || rise < -40) {
                    continue;
                }
                if (blocked(solids, (nx + 0.5) * cell, f, (nz + 0.5) * cell, ag)) {
                    continue;
                }
                double g = n[2] + (d[0] != 0 && d[1] != 0 ? 1.414 : 1) + (jump ? 2 : 0);
                double[] old = nodes.get(Long.valueOf(nk));
                if (old != null && old[2] <= g) {
                    continue;
                }
                double[] nn = {nx, nz, g, g + h(nx, nz, gx, gz), k, f, jump ? 1 : 0};
                nodes.put(Long.valueOf(nk), nn);
                open.add(nn);
            }
        }
        double[] end = goal != null ? goal : best;
        List<double[]> chain = new ArrayList<double[]>();
        for (double[] n = end; n != null; ) {
            chain.add(0, n);
            if (Double.isNaN(n[4])) {
                break;
            }
            n = nodes.get(Long.valueOf((long) n[4]));
        }
        out.add(new QueryParams.Waypoint(from, "Walk"));
        for (int i = 1; i < chain.size(); i++) {
            double[] n = chain.get(i);
            out.add(new QueryParams.Waypoint(new Vector3((n[0] + 0.5) * cell, n[5], (n[1] + 0.5) * cell),
                    n[6] > 0 ? "Jump" : "Walk"));
        }
        if (goal != null) {
            out.add(new QueryParams.Waypoint(new Vector3(to.x, goalFloor == NO_FLOOR ? to.y : goalFloor, to.z), "Walk"));
            return "Success";
        }
        return chain.size() > 1 ? "ClosestNoPath" : "NoPath";
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    static double h(int x, int z, int gx, int gz) {
        double dx = x - gx;
        double dz = z - gz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /// Highest part top under (x, z) at or below `below`.
    static double floor(List<Instance> solids, double x, double z, double below) {
        double best = NO_FLOOR;
        for (int i = 0; i < solids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = solids.get(i);
            CFrame c = Behaviors.cf(p);
            Vector3 hs = Behaviors.size(p).scale(0.5);
            Vector3 l = c.pointToObject(new Vector3(x, c.y, z));
            if (Math.abs(l.x) > hs.x || Math.abs(l.z) > hs.z) {
                continue;
            }
            double ey = Math.abs(c.r10) * hs.x + Math.abs(c.r11) * hs.y + Math.abs(c.r12) * hs.z;
            double top = c.y + ey;
            if (top <= below && top > best) {
                best = top;
            }
        }
        return best;
    }

    /// Whether a part occupies the agent's body above a floor.
    static boolean blocked(List<Instance> solids, double x, double floor, double z, Agent ag) {
        double lo = floor + 0.2;
        double hi = floor + ag.height;
        for (int i = 0; i < solids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = solids.get(i);
            CFrame c = Behaviors.cf(p);
            Vector3 hs = Behaviors.size(p).scale(0.5);
            double ey = Math.abs(c.r10) * hs.x + Math.abs(c.r11) * hs.y + Math.abs(c.r12) * hs.z;
            if (c.y + ey <= lo || c.y - ey >= hi) {
                continue;
            }
            Vector3 l = c.pointToObject(new Vector3(x, Math.max(lo, Math.min(hi, c.y)), z));
            if (Math.abs(l.x) <= hs.x + ag.radius * 0.5 && Math.abs(l.z) <= hs.z + ag.radius * 0.5
                    && Math.abs(l.y) <= hs.y) {
                return true;
            }
        }
        return false;
    }
}
