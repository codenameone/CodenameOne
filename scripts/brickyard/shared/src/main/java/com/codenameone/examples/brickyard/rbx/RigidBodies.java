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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The engine's rigid-body world: gravity, collisions and `Touched` for
/// loose parts, exact ray casts against oriented boxes, and box queries.
///
/// Deliberately small. A body is an assembly (parts joined by welds move as
/// one); it translates and does not rotate, and contacts are resolved on the
/// axis of least penetration of world-aligned bounds. That is what the
/// templates and most obstacle and tycoon places need: things fall, stack,
/// slide and land. Characters are not bodies; their controller in
/// [Characters] is kinematic on purpose.
public final class RigidBodies implements Engine.PhysicsWorld {
    private static final double CELL = 16;
    private static final double SLEEP_SPEED = 0.05;

    /// Per assembly state, keyed by its root part.
    private static final class Body {
        final List<Instance> parts = new ArrayList<Instance>();
        double vx;
        double vy;
        double vz;
        int still;
    }

    private final Map<Instance, Body> bodies = new HashMap<Instance, Body>();
    private final Map<Instance, Set<Instance>> touching = new HashMap<Instance, Set<Instance>>();
    private final Map<Long, List<Instance>> grid = new HashMap<Long, List<Instance>>();

    @Override
    public void step(Engine e, double dt) {
        Instance ws = e.getWorkspace();
        List<Instance> all = Behaviors.parts(ws);
        List<Instance> solids = new ArrayList<Instance>();
        Map<Instance, Instance> rootOf = new HashMap<Instance, Instance>();
        Set<Instance> live = new HashSet<Instance>();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (p.bool("CanCollide") || p.bool("CanTouch")) {
                solids.add(p);
            }
            if (p.bool("Anchored") || inCharacter(p) || rootOf.containsKey(p)) {
                continue;
            }
            List<Instance> asm = Joints.connected(p, true);
            asm.add(0, p);
            if (anchoredIn(asm)) {
                continue;
            }
            Body b = bodies.get(p);
            if (b == null) {
                b = new Body();
                Vector3 v = (Vector3) vec(p, "AssemblyLinearVelocity");
                b.vx = v.x;
                b.vy = v.y;
                b.vz = v.z;
                bodies.put(p, b);
            }
            b.parts.clear();
            for (Instance q : asm) {
                if (q.isA("BasePart") && !rootOf.containsKey(q)) {
                    rootOf.put(q, p);
                    b.parts.add(q);
                }
            }
            live.add(p);
        }
        bodies.keySet().retainAll(live);
        index(solids);
        double g = ws.num("Gravity");
        double floor = ws.num("FallenPartsDestroyHeight");
        List<Instance> fallen = new ArrayList<Instance>();
        for (Map.Entry<Instance, Body> en : bodies.entrySet()) {
            Instance root = en.getKey();
            Body b = en.getValue();
            Vector3 set = (Vector3) vec(root, "AssemblyLinearVelocity");
            if (Math.abs(set.x - b.vx) + Math.abs(set.y - b.vy) + Math.abs(set.z - b.vz) > 1e-9) {
                // A script assigned a velocity since the last step.
                b.vx = set.x;
                b.vy = set.y;
                b.vz = set.z;
                b.still = 0;
            }
            if (b.still > 30) {
                continue;
            }
            b.vy -= g * dt;
            move(b, b.vx * dt, b.vy * dt, b.vz * dt);
            resolve(e, root, b, rootOf, dt);
            root.put("AssemblyLinearVelocity", new Vector3(b.vx, b.vy, b.vz));
            double sp = Math.abs(b.vx) + Math.abs(b.vy) + Math.abs(b.vz);
            b.still = sp < SLEEP_SPEED ? b.still + 1 : 0;
            if (Behaviors.cf(root).y < floor) {
                fallen.add(root);
            }
        }
        for (Instance f : fallen) {
            Instance m = f.getParent();
            if (m != null && m.isA("Model") && m != ws) { // NOPMD CompareObjectsWithEquals - identity of the workspace
                m.destroy();
            } else {
                f.destroy();
            }
        }
        touches();
    }

    private static Object vec(Instance p, String k) {
        Object v = p.prop(k);
        return v instanceof Vector3 ? v : Vector3.ZERO;
    }

    private static boolean inCharacter(Instance p) {
        Instance m = p.getParent();
        while (m != null && !m.isA("Workspace")) {
            if (m.findFirstChildOfClass("Humanoid", false) != null) {
                return true;
            }
            m = m.getParent();
        }
        return false;
    }

    private static boolean anchoredIn(List<Instance> asm) {
        for (Instance q : asm) {
            if (q.isA("BasePart") && q.bool("Anchored")) {
                return true;
            }
        }
        return false;
    }

    private static void move(Body b, double dx, double dy, double dz) {
        for (Instance q : b.parts) {
            CFrame c = Behaviors.cf(q);
            q.put("CFrame", new CFrame(c.x + dx, c.y + dy, c.z + dz, c.r00, c.r01, c.r02, c.r10, c.r11, c.r12,
                    c.r20, c.r21, c.r22));
        }
    }

    /// World-aligned bounds of an oriented box: {minx, miny, minz, maxx, maxy, maxz}.
    static double[] aabb(Instance p) {
        CFrame c = Behaviors.cf(p);
        Vector3 s = Behaviors.size(p);
        double hx = (Math.abs(c.r00) * s.x + Math.abs(c.r01) * s.y + Math.abs(c.r02) * s.z) / 2;
        double hy = (Math.abs(c.r10) * s.x + Math.abs(c.r11) * s.y + Math.abs(c.r12) * s.z) / 2;
        double hz = (Math.abs(c.r20) * s.x + Math.abs(c.r21) * s.y + Math.abs(c.r22) * s.z) / 2;
        return new double[] {c.x - hx, c.y - hy, c.z - hz, c.x + hx, c.y + hy, c.z + hz};
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42) | ((long) (y & 0x1fffff) << 21) | (z & 0x1fffff);
    }

    private void index(List<Instance> solids) {
        grid.clear();
        for (Instance p : solids) {
            double[] a = aabb(p);
            int x0 = (int) Math.floor(a[0] / CELL);
            int y0 = (int) Math.floor(a[1] / CELL);
            int z0 = (int) Math.floor(a[2] / CELL);
            int x1 = (int) Math.floor(a[3] / CELL);
            int y1 = (int) Math.floor(a[4] / CELL);
            int z1 = (int) Math.floor(a[5] / CELL);
            if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > 4096) {
                // A huge baseplate: keep it in one list every query checks.
                cell(Long.MIN_VALUE).add(p);
                continue;
            }
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        cell(Long.valueOf(key(x, y, z))).add(p);
                    }
                }
            }
        }
    }

    private List<Instance> cell(Long k) {
        List<Instance> l = grid.get(k);
        if (l == null) {
            l = new ArrayList<Instance>();
            grid.put(k, l);
        }
        return l;
    }

    private Set<Instance> near(double[] a) {
        Set<Instance> out = new HashSet<Instance>();
        List<Instance> big = grid.get(Long.valueOf(Long.MIN_VALUE));
        if (big != null) {
            out.addAll(big);
        }
        int x0 = (int) Math.floor(a[0] / CELL);
        int y0 = (int) Math.floor(a[1] / CELL);
        int z0 = (int) Math.floor(a[2] / CELL);
        int x1 = (int) Math.floor(a[3] / CELL);
        int y1 = (int) Math.floor(a[4] / CELL);
        int z1 = (int) Math.floor(a[5] / CELL);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    List<Instance> l = grid.get(Long.valueOf(key(x, y, z)));
                    if (l != null) {
                        out.addAll(l);
                    }
                }
            }
        }
        return out;
    }

    private static boolean overlap(double[] a, double[] b) {
        return a[0] < b[3] && a[3] > b[0] && a[1] < b[4] && a[4] > b[1] && a[2] < b[5] && a[5] > b[2];
    }

    private void resolve(Engine e, Instance root, Body b, Map<Instance, Instance> rootOf, double dt) {
        for (int iter = 0; iter < 4; iter++) {
            boolean moved = false;
            for (Instance q : b.parts) {
                if (!q.bool("CanCollide")) {
                    continue;
                }
                double[] a = aabb(q);
                for (Instance o : near(a)) {
                    if (rootOf.get(o) == root || !o.bool("CanCollide") // NOPMD CompareObjectsWithEquals - same assembly
                            || !ServiceBehaviors.groupsCollide(e, q.str("CollisionGroup"), o.str("CollisionGroup"))) {
                        continue;
                    }
                    double[] c = aabb(o);
                    if (!overlap(a, c)) {
                        continue;
                    }
                    double[] pen = {c[3] - a[0], a[3] - c[0], c[4] - a[1], a[4] - c[1], c[5] - a[2], a[5] - c[2]};
                    int best = 0;
                    for (int k = 1; k < 6; k++) {
                        if (pen[k] < pen[best]) {
                            best = k;
                        }
                    }
                    double d = pen[best] * (best % 2 == 0 ? 1 : -1);
                    int axis = best / 2;
                    boolean otherDynamic = rootOf.containsKey(o);
                    double share = otherDynamic ? 0.5 : 1;
                    move(b, axis == 0 ? d * share : 0, axis == 1 ? d * share : 0, axis == 2 ? d * share : 0);
                    a = aabb(q);
                    double bounce = Math.min(elasticity(q), elasticity(o));
                    if (axis == 0 && b.vx * d < 0) {
                        b.vx = -b.vx * bounce;
                    } else if (axis == 1 && b.vy * d < 0) {
                        b.vy = -b.vy * bounce;
                        if (Math.abs(b.vy) < 1) {
                            b.vy = 0;
                        }
                        if (d > 0) {
                            // Resting on top: friction slows the slide.
                            double f = Math.max(0, 1 - friction(q, o) * 10 * dt);
                            b.vx *= f;
                            b.vz *= f;
                            Vector3 carry = (Vector3) vec(o, "AssemblyLinearVelocity");
                            if (o.bool("Anchored") && carry.magnitude() > 0) {
                                // A conveyor: an anchored part's velocity moves what sits on it.
                                b.vx += (carry.x - b.vx) * Math.min(1, 10 * dt);
                                b.vz += (carry.z - b.vz) * Math.min(1, 10 * dt);
                            }
                        }
                    } else if (axis == 2 && b.vz * d < 0) {
                        b.vz = -b.vz * bounce;
                    }
                    moved = true;
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    private static double elasticity(Instance p) {
        Object pp = p.prop("CustomPhysicalProperties");
        if (pp instanceof QueryParams.PhysicalProperties) {
            return ((QueryParams.PhysicalProperties) pp).elasticity;
        }
        return Materials.elasticity(p);
    }

    private static double friction(Instance a, Instance b) {
        return Math.sqrt(fr(a) * fr(b));
    }

    private static double fr(Instance p) {
        Object pp = p.prop("CustomPhysicalProperties");
        if (pp instanceof QueryParams.PhysicalProperties) {
            return ((QueryParams.PhysicalProperties) pp).friction;
        }
        return Materials.friction(p);
    }

    /// `Touched`/`TouchEnded` between bodies and whatever they overlap.
    private void touches() {
        Set<Instance> seen = new HashSet<Instance>();
        for (Body b : bodies.values()) {
            for (Instance q : b.parts) {
                if (!q.bool("CanTouch")) {
                    continue;
                }
                seen.add(q);
                double[] a = grow(aabb(q));
                Set<Instance> now = new HashSet<Instance>();
                for (Instance o : near(a)) {
                    if (o != q && o.bool("CanTouch") && !b.parts.contains(o) // NOPMD CompareObjectsWithEquals - self
                            && overlap(a, aabb(o))) {
                        now.add(o);
                    }
                }
                Set<Instance> was = touching.get(q);
                if (was == null) {
                    was = new HashSet<Instance>();
                    touching.put(q, was);
                }
                for (Instance o : now) {
                    if (!was.contains(o)) {
                        q.fireEvent("Touched", o);
                        o.fireEvent("Touched", q);
                    }
                }
                for (Instance o : was) {
                    if (!now.contains(o)) {
                        q.fireEvent("TouchEnded", o);
                        o.fireEvent("TouchEnded", q);
                    }
                }
                was.clear();
                was.addAll(now);
            }
        }
        touching.keySet().retainAll(seen);
    }

    private static double[] grow(double[] a) {
        double m = 0.05;
        return new double[] {a[0] - m, a[1] - m, a[2] - m, a[3] + m, a[4] + m, a[5] + m};
    }

    @Override
    public Object[] raycast(Engine e, Vector3 origin, Vector3 dir, Engine.RaycastFilter filter) {
        List<Instance> all = Behaviors.parts(e.getWorkspace());
        double bestT = 1;
        Object[] best = null;
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (!p.bool("CanQuery") || filter != null && !filter.accepts(p)) {
                continue;
            }
            double[] hit = rayBox(p, origin, dir, bestT);
            if (hit != null) {
                bestT = hit[0];
                Vector3 pos = origin.add(dir.scale(hit[0]));
                best = new Object[] {p, pos, new Vector3(hit[1], hit[2], hit[3]), p.prop("Material")};
            }
        }
        return best;
    }

    /// Slab test in the box's own frame: {t, nx, ny, nz} of the entry, or
    /// null when the ray misses within `maxT`.
    static double[] rayBox(Instance p, Vector3 o, Vector3 d, double maxT) {
        CFrame c = Behaviors.cf(p);
        Vector3 s = Behaviors.size(p);
        double ox = o.x - c.x;
        double oy = o.y - c.y;
        double oz = o.z - c.z;
        // Local = R^T * world (columns of R are the box axes).
        double[] lo = {c.r00 * ox + c.r10 * oy + c.r20 * oz, c.r01 * ox + c.r11 * oy + c.r21 * oz,
            c.r02 * ox + c.r12 * oy + c.r22 * oz};
        double[] ld = {c.r00 * d.x + c.r10 * d.y + c.r20 * d.z, c.r01 * d.x + c.r11 * d.y + c.r21 * d.z,
            c.r02 * d.x + c.r12 * d.y + c.r22 * d.z};
        double[] half = {s.x / 2, s.y / 2, s.z / 2};
        double tmin = 0;
        double tmax = maxT;
        int axis = -1;
        double sign = 0;
        for (int k = 0; k < 3; k++) {
            if (Math.abs(ld[k]) < 1e-12) {
                if (lo[k] < -half[k] || lo[k] > half[k]) {
                    return null;
                }
                continue;
            }
            double t1 = (-half[k] - lo[k]) / ld[k];
            double t2 = (half[k] - lo[k]) / ld[k];
            double sg = -1;
            if (t1 > t2) {
                double tt = t1;
                t1 = t2;
                t2 = tt;
                sg = 1;
            }
            if (t1 > tmin) {
                tmin = t1;
                axis = k;
                sign = sg;
            }
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) {
                return null;
            }
        }
        if (axis < 0) {
            return null; // the origin is inside the box
        }
        double[] n = new double[3];
        n[axis] = sign;
        // Back to world: R * n.
        double wx = c.r00 * n[0] + c.r01 * n[1] + c.r02 * n[2];
        double wy = c.r10 * n[0] + c.r11 * n[1] + c.r12 * n[2];
        double wz = c.r20 * n[0] + c.r21 * n[1] + c.r22 * n[2];
        return new double[] {tmin, wx, wy, wz};
    }

    @Override
    public List<Instance> partsInBox(Engine e, CFrame cf, Vector3 size, Engine.RaycastFilter filter) {
        double hx = (Math.abs(cf.r00) * size.x + Math.abs(cf.r01) * size.y + Math.abs(cf.r02) * size.z) / 2;
        double hy = (Math.abs(cf.r10) * size.x + Math.abs(cf.r11) * size.y + Math.abs(cf.r12) * size.z) / 2;
        double hz = (Math.abs(cf.r20) * size.x + Math.abs(cf.r21) * size.y + Math.abs(cf.r22) * size.z) / 2;
        double[] box = {cf.x - hx, cf.y - hy, cf.z - hz, cf.x + hx, cf.y + hy, cf.z + hz};
        List<Instance> out = new ArrayList<Instance>();
        List<Instance> all = Behaviors.parts(e.getWorkspace());
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (p.bool("CanQuery") && (filter == null || filter.accepts(p)) && overlap(box, aabb(p))) {
                out.add(p);
            }
        }
        return out;
    }
}
