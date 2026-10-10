/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.unitycompat.unityengine;

import com.codename1.gaming.physics.box2d.collision.shapes.ChainShape;
import com.codename1.gaming.physics.box2d.collision.shapes.CircleShape;
import com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.collision.shapes.Shape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Fixture;
import java.util.ArrayList;

/// The questions a script asks of the physics world: what a ray or a moving
/// shape meets, and what a shape placed somewhere overlaps.
///
/// #### One method for every shape
///
/// Every shape here is a convex polygon grown by a radius: a circle is one
/// point and a radius, a capsule two points and a radius, a box four points
/// and none, an edge two points and none. A collider that is not convex is
/// several such pieces, and is hit where the first of them is.
///
/// A shape A moving along a direction touches a shape B exactly when a
/// point moving the same way from the origin touches the set of all
/// differences `b - a`, grown by both radii. That set is again a convex
/// polygon -- the hull of the differences of the corners -- so every cast is
/// one ray against one rounded polygon, which has a closed answer: the ray
/// enters through a side moved outwards by the radius, or through the arc
/// round a corner. Nothing is stepped or iterated, so nothing can be
/// tunnelled through or stop early, and a ray is simply the cast of a point.
///
/// Whether two shapes overlap is the same polygon asked whether it holds
/// the origin.
///
/// #### What is exact and what is chosen
///
/// The distance, the normal and the place the moving shape stops are exact
/// to the rounding of `double` arithmetic. Two things are choices, where
/// Unity documents no rule:
///
/// - Where two flat sides meet, they touch along a stretch and `point` is
///   its middle.
/// - A shape that already overlaps a collider where it starts reports that
///   collider at distance zero, with the normal opposing the direction and
///   `point` at the shape's centre. Touching is not overlapping: a shape
///   resting against a wall and cast along it does not hit the wall.
///
/// #### What a query sees
///
/// Bodies where physics last put them, as in Unity with
/// `Physics2D.autoSyncTransforms` off: a transform moved by a script since
/// the last fixed step is seen once that step has run, or once the script
/// calls `Physics2D.SyncTransforms()`. A collider made since then is seen
/// where it was made.
///
/// Everything is computed in `double` from `float` inputs with the four
/// operations and square root only, which every target rounds alike, so a
/// query answers the same on the JVM, natively and in a browser.
final class PhysicsQuery {
    private static final int MAX = 64;
    private static final double EDGE_SLACK = 1e-9;
    private static final double BACK_SLACK = 1e-9;
    private static final double FLAT = 1e-6;

    // The moving shape, where it starts.
    private static final double[] cx = new double[8];
    private static final double[] cy = new double[8];
    private static int cn;
    private static double cr;
    private static double originX;
    private static double originY;

    // One piece of a collider, in the world.
    private static final double[] tx = new double[8];
    private static final double[] ty = new double[8];
    private static int tn;
    private static double tr;
    private static double loadedRadius;

    // Their differences, and the hull of those.
    private static final double[] qx = new double[MAX];
    private static final double[] qy = new double[MAX];
    private static final int[] order = new int[MAX];
    private static final double[] hx = new double[MAX * 2];
    private static final double[] hy = new double[MAX * 2];
    private static int hn;

    // What enter() found.
    private static double outT;
    private static double outNx;
    private static double outNy;
    // And where the surfaces touch.
    private static double outPx;
    private static double outPy;

    private static final ContactFilter2D rule = new ContactFilter2D();
    private static Collider2D skip;
    private static GameObject skipOwner;

    private static Collider2D[] found = new Collider2D[16];
    private static double[] ft = new double[16];
    private static double[] fnx = new double[16];
    private static double[] fny = new double[16];
    private static double[] fpx = new double[16];
    private static double[] fpy = new double[16];
    private static float[] fz = new float[16];
    private static int count;

    private PhysicsQuery() {
    }

    static void reset() {
        clear();
    }

    /// Lets go of the colliders the last query found.
    static void clear() {
        for (int i = 0; i < count; i++) {
            found[i] = null;
        }
        count = 0;
        skip = null;
        skipOwner = null;
    }

    // ------------------------------------------------------- the moving shape

    static void point(float x, float y) {
        originX = x;
        originY = y;
        cx[0] = x;
        cy[0] = y;
        cn = 1;
        cr = 0d;
    }

    static void circle(float x, float y, float radius) {
        point(x, y);
        cr = radius < 0f ? -radius : radius;
    }

    /// A box of a size, turned by degrees about its centre.
    static void box(float x, float y, float width, float height, float angle) {
        originX = x;
        originY = y;
        double hw = (width < 0f ? -width : width) * 0.5d;
        double hh = (height < 0f ? -height : height) * 0.5d;
        double a = angle * Transform.DEG2RAD;
        double c = angle == 0f ? 1d : Math.cos(a);
        double s = angle == 0f ? 0d : Math.sin(a);
        corner(0, -hw, -hh, c, s);
        corner(1, hw, -hh, c, s);
        corner(2, hw, hh, c, s);
        corner(3, -hw, hh, c, s);
        cn = 4;
        cr = 0d;
    }

    private static void corner(int i, double lx, double ly, double c, double s) {
        cx[i] = originX + (lx * c - ly * s);
        cy[i] = originY + (lx * s + ly * c);
    }

    /// The box with two opposite corners, its sides along the axes.
    static void area(float ax, float ay, float bx, float by) {
        float w = bx - ax;
        float h = by - ay;
        box((ax + bx) * 0.5f, (ay + by) * 0.5f, w, h, 0f);
    }

    /// A capsule of a size: `direction` 0 for upright, 1 for lying down.
    /// Its ends are half circles as wide as its short side; one no longer
    /// than it is wide is a circle.
    static void capsule(float x, float y, float width, float height, int direction, float angle) {
        originX = x;
        originY = y;
        double w = width < 0f ? -width : width;
        double h = height < 0f ? -height : height;
        double radius = (direction == 1 ? h : w) * 0.5d;
        double half = (direction == 1 ? w : h) * 0.5d - radius;
        if (!(half > 0d)) { // NOPMD LogicInversion
            cx[0] = x;
            cy[0] = y;
            cn = 1;
            cr = radius;
            return;
        }
        double a = angle * Transform.DEG2RAD;
        double c = angle == 0f ? 1d : Math.cos(a);
        double s = angle == 0f ? 0d : Math.sin(a);
        if (direction == 1) {
            corner(0, -half, 0d, c, s);
            corner(1, half, 0d, c, s);
        } else {
            corner(0, 0d, -half, c, s);
            corner(1, 0d, half, c, s);
        }
        cn = 2;
        cr = radius;
    }

    // ------------------------------------------------------------ the filter

    /// The filter of a query that names a layer mask and a depth range, and
    /// takes the world's setting for triggers.
    static void where(int layerMask, float minDepth, float maxDepth) {
        rule.useTriggers = Physics2D.hitTriggers;
        rule.useLayerMask = true;
        rule.layerMask.m_Bits = layerMask;
        rule.useDepth = true;
        rule.useOutsideDepth = false;
        rule.minDepth = minDepth;
        rule.maxDepth = maxDepth;
        rule.useNormalAngle = false;
        skip = null;
        skipOwner = null;
    }

    static void where(ContactFilter2D filter) {
        rule.$assign(filter);
        skip = null;
        skipOwner = null;
    }

    private static boolean accepts(Collider2D c) {
        if (c == skip || (skipOwner != null && c.owner == skipOwner)) { // NOPMD CompareObjectsWithEquals
            return false;
        }
        if (rule.IsFilteringTrigger(c) || rule.IsFilteringLayerMask(c.gameObject)) {
            return false;
        }
        return !rule.IsFilteringDepth(c.gameObject);
    }

    // ------------------------------------------------------------- fixtures

    static int pieces(Fixture f) {
        Shape s = f.getShape();
        return s instanceof ChainShape ? ((ChainShape) s).m_count - 1 : 1;
    }

    /// Puts a piece of a fixture, in the world, into `x` and `y`; answers
    /// how many points it has and leaves its radius in `loadedRadius`.
    private static int load(Fixture f, int piece, double[] x, double[] y) {
        com.codename1.gaming.physics.box2d.common.Transform xf = f.getBody().getTransform();
        double c = xf.q.c;
        double s = xf.q.s;
        double px = xf.p.x;
        double py = xf.p.y;
        Shape shape = f.getShape();
        loadedRadius = 0d;
        if (shape instanceof CircleShape) {
            CircleShape circle = (CircleShape) shape;
            place(circle.m_p, 0, x, y, c, s, px, py);
            loadedRadius = circle.m_radius;
            return 1;
        }
        if (shape instanceof PolygonShape) {
            PolygonShape polygon = (PolygonShape) shape;
            int n = polygon.m_count > 8 ? 8 : polygon.m_count;
            for (int i = 0; i < n; i++) {
                place(polygon.m_vertices[i], i, x, y, c, s, px, py);
            }
            return n;
        }
        if (shape instanceof ChainShape) {
            ChainShape chain = (ChainShape) shape;
            place(chain.m_vertices[piece], 0, x, y, c, s, px, py);
            place(chain.m_vertices[piece + 1], 1, x, y, c, s, px, py);
            return 2;
        }
        if (shape instanceof EdgeShape) {
            EdgeShape edge = (EdgeShape) shape;
            place(edge.m_vertex1, 0, x, y, c, s, px, py);
            place(edge.m_vertex2, 1, x, y, c, s, px, py);
            return 2;
        }
        return 0;
    }

    private static void place(Vec2 v, int i, double[] x, double[] y, double c, double s, double px, double py) {
        double lx = v.x;
        double ly = v.y;
        x[i] = px + (lx * c - ly * s);
        y[i] = py + (lx * s + ly * c);
    }

    /// The rectangle round a collider's fixtures in the world, into
    /// `box` as least x, least y, greatest x, greatest y. False for a
    /// collider with no fixture.
    static boolean bounds(Collider2D collider, float[] box) {
        boolean any = false;
        double minX = 0d;
        double minY = 0d;
        double maxX = 0d;
        double maxY = 0d;
        for (int k = 0; k < collider.fixtures.size(); k++) {
            Fixture f = (Fixture) collider.fixtures.get(k);
            int pieces = pieces(f);
            for (int piece = 0; piece < pieces; piece++) {
                int n = load(f, piece, tx, ty);
                for (int i = 0; i < n; i++) {
                    if (!any || tx[i] - loadedRadius < minX) {
                        minX = tx[i] - loadedRadius;
                    }
                    if (!any || ty[i] - loadedRadius < minY) {
                        minY = ty[i] - loadedRadius;
                    }
                    if (!any || tx[i] + loadedRadius > maxX) {
                        maxX = tx[i] + loadedRadius;
                    }
                    if (!any || ty[i] + loadedRadius > maxY) {
                        maxY = ty[i] + loadedRadius;
                    }
                    any = true;
                }
            }
        }
        box[0] = (float) minX;
        box[1] = (float) minY;
        box[2] = (float) maxX;
        box[3] = (float) maxY;
        return any;
    }

    // ------------------------------------------------------------- geometry

    /// The hull of every difference of a point of the piece and a point of
    /// the moving shape, counter-clockwise, into `hx` and `hy`: one point,
    /// the two ends of a line, or a polygon with no three corners in line.
    private static void difference() {
        int k = 0;
        for (int i = 0; i < tn; i++) {
            for (int j = 0; j < cn; j++) {
                qx[k] = tx[i] - cx[j];
                qy[k] = ty[i] - cy[j];
                k++;
            }
        }
        // By x, then y; the few points make an insertion sort the fastest.
        for (int i = 0; i < k; i++) {
            int at = i;
            while (at > 0 && before(i, order[at - 1])) {
                order[at] = order[at - 1];
                at--;
            }
            order[at] = i;
        }
        int n = 0;
        for (int i = 0; i < k; i++) {
            int p = order[i];
            if (n > 0 && qx[p] == qx[order[n - 1]] && qy[p] == qy[order[n - 1]]) {
                continue;
            }
            order[n++] = p;
        }
        if (n < 3) {
            for (int i = 0; i < n; i++) {
                hx[i] = qx[order[i]];
                hy[i] = qy[order[i]];
            }
            hn = n;
            return;
        }
        int m = 0;
        for (int i = 0; i < n; i++) {
            int p = order[i];
            while (m >= 2 && turn(m, p) <= 0d) {
                m--;
            }
            hx[m] = qx[p];
            hy[m] = qy[p];
            m++;
        }
        int lower = m + 1;
        for (int i = n - 2; i >= 0; i--) {
            int p = order[i];
            while (m >= lower && turn(m, p) <= 0d) {
                m--;
            }
            hx[m] = qx[p];
            hy[m] = qy[p];
            m++;
        }
        // The last point is the first again.
        hn = m - 1;
    }

    private static boolean before(int a, int b) {
        return qx[a] < qx[b] || (qx[a] == qx[b] && qy[a] < qy[b]);
    }

    /// Which way the hull turns from its last two points to a new one:
    /// positive for left.
    private static double turn(int m, int p) {
        double ax = hx[m - 1] - hx[m - 2];
        double ay = hy[m - 1] - hy[m - 2];
        double bx = qx[p] - hx[m - 2];
        double by = qy[p] - hy[m - 2];
        return ax * by - ay * bx;
    }

    /// The square of the distance from the origin to the hull's outline.
    private static double outline() {
        double best = Double.POSITIVE_INFINITY;
        int sides = hn == 2 ? 1 : hn;
        for (int i = 0; i < sides; i++) {
            int j = i + 1 == hn ? 0 : i + 1;
            double ex = hx[j] - hx[i];
            double ey = hy[j] - hy[i];
            double ll = ex * ex + ey * ey;
            double s = -(hx[i] * ex + hy[i] * ey) / ll;
            if (s < 0d) {
                s = 0d;
            } else if (s > 1d) {
                s = 1d;
            }
            double px = hx[i] + ex * s;
            double py = hy[i] + ey * s;
            double d = px * px + py * py;
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    /// 1 when the origin is strictly inside the hull, 0 when it is on its
    /// outline, -1 outside. A hull with no area has no inside.
    private static int side() {
        if (hn < 3) {
            return -1;
        }
        boolean on = false;
        for (int i = 0; i < hn; i++) {
            int j = i + 1 == hn ? 0 : i + 1;
            double ex = hx[j] - hx[i];
            double ey = hy[j] - hy[i];
            double cross = ey * hx[i] - ex * hy[i];
            if (cross < 0d) {
                return -1;
            }
            if (cross == 0d) {
                on = true;
            }
        }
        return on ? 0 : 1;
    }

    /// Whether the two shapes the hull was made from overlap, touching
    /// counted or not. `radius` is the sum of theirs.
    private static boolean overlapping(double radius, boolean touchingCounts) {
        if (hn == 0) {
            return false;
        }
        double rr = radius * radius;
        if (hn == 1) {
            double d = hx[0] * hx[0] + hy[0] * hy[0];
            return radius > 0d && (touchingCounts ? d <= rr : d < rr);
        }
        int side = side();
        if (side > 0 || (side == 0 && touchingCounts)) {
            return true;
        }
        if (!(radius > 0d)) { // NOPMD LogicInversion
            return false;
        }
        double d = outline();
        return touchingCounts ? d <= rr : d < rr;
    }

    /// Where a point leaving the origin along a unit direction first meets
    /// the hull grown by a radius, no further than `length`: 0 for nowhere,
    /// 1 for a hit left in `outT`, `outNx` and `outNy`, 2 when the shapes
    /// overlap before moving at all.
    private static int enter(double dx, double dy, double length, double radius) {
        if (hn == 0) {
            return 0;
        }
        if (overlapping(radius, false)) {
            return 2;
        }
        if (dx == 0d && dy == 0d) {
            return 0;
        }
        double best = Double.POSITIVE_INFINITY;
        double bnx = 0d;
        double bny = 0d;
        // Through a side moved outwards by the radius. A line has two.
        int sides = hn == 1 ? 0 : hn;
        for (int i = 0; i < sides; i++) {
            int j = i + 1 == hn ? 0 : i + 1;
            double ex = hx[j] - hx[i];
            double ey = hy[j] - hy[i];
            double l = Math.sqrt(ex * ex + ey * ey);
            double nx = ey / l;
            double ny = -ex / l;
            double den = nx * dx + ny * dy;
            if (!(den < 0d)) { // NOPMD LogicInversion
                continue;
            }
            // How far outside the side's line the origin is.
            double off = -(nx * hx[i] + ny * hy[i]);
            double t = (radius - off) / den;
            if (t < 0d) {
                if (t < -BACK_SLACK) {
                    continue;
                }
                t = 0d;
            }
            if (!(t < best)) { // NOPMD LogicInversion
                continue;
            }
            double along = ((t * dx - hx[i]) * ex + (t * dy - hy[i]) * ey) / (l * l);
            if (along < -EDGE_SLACK || along > 1d + EDGE_SLACK) {
                continue;
            }
            best = t;
            bnx = nx;
            bny = ny;
        }
        // Through the arc round a corner.
        if (radius > 0d) {
            for (int i = 0; i < hn; i++) {
                double b = -(dx * hx[i] + dy * hy[i]);
                double c = hx[i] * hx[i] + hy[i] * hy[i] - radius * radius;
                double disc = b * b - c;
                if (disc < 0d) {
                    continue;
                }
                double t = -b - Math.sqrt(disc);
                if (t < 0d) {
                    if (t < -BACK_SLACK) {
                        continue;
                    }
                    t = 0d;
                }
                if (!(t < best)) { // NOPMD LogicInversion
                    continue;
                }
                best = t;
                bnx = (t * dx - hx[i]) / radius;
                bny = (t * dy - hy[i]) / radius;
            }
        }
        // Nothing was met at all, which an unlimited length would let through.
        if (!(best < Double.POSITIVE_INFINITY && best <= length)) { // NOPMD LogicInversion
            return 0;
        }
        outT = best;
        outNx = bnx;
        outNy = bny;
        return 1;
    }

    /// Where the surfaces touch once the moving shape has gone `outT` along
    /// the direction, into `outPx` and `outPy`. Each shape is touched at a
    /// corner or along a side; of two sides, the middle of what they share.
    private static void contact(double dx, double dy) {
        if (cn == 1 && cr == 0d) {
            outPx = cx[0] + outT * dx;
            outPy = cy[0] + outT * dy;
            return;
        }
        double nx = outNx;
        double ny = outNy;
        // The piece: its points furthest along the normal.
        double top = 0d;
        for (int i = 0; i < tn; i++) {
            double h = tx[i] * nx + ty[i] * ny;
            if (i == 0 || h > top) {
                top = h;
            }
        }
        double tMin = 0d;
        double tMax = 0d;
        boolean first = true;
        for (int i = 0; i < tn; i++) {
            if (tx[i] * nx + ty[i] * ny < top - FLAT) {
                continue;
            }
            double u = ty[i] * nx - tx[i] * ny;
            if (first || u < tMin) {
                tMin = u;
            }
            if (first || u > tMax) {
                tMax = u;
            }
            first = false;
        }
        // The moving shape, where it stopped: its points nearest the piece.
        double sx = outT * dx;
        double sy = outT * dy;
        double bottom = 0d;
        for (int i = 0; i < cn; i++) {
            double h = (cx[i] + sx) * nx + (cy[i] + sy) * ny;
            if (i == 0 || h < bottom) {
                bottom = h;
            }
        }
        double cMin = 0d;
        double cMax = 0d;
        first = true;
        for (int i = 0; i < cn; i++) {
            if ((cx[i] + sx) * nx + (cy[i] + sy) * ny > bottom + FLAT) {
                continue;
            }
            double u = (cy[i] + sy) * nx - (cx[i] + sx) * ny;
            if (first || u < cMin) {
                cMin = u;
            }
            if (first || u > cMax) {
                cMax = u;
            }
            first = false;
        }
        double u;
        if (tMax - tMin <= FLAT) {
            u = (tMin + tMax) * 0.5d;
        } else if (cMax - cMin <= FLAT) {
            u = (cMin + cMax) * 0.5d;
        } else {
            double low = tMin > cMin ? tMin : cMin;
            double high = tMax < cMax ? tMax : cMax;
            u = (low + high) * 0.5d;
        }
        double h = top + tr;
        // u runs along the tangent (-ny, nx).
        outPx = h * nx - u * ny;
        outPy = h * ny + u * nx;
    }

    // ---------------------------------------------------------------- casts

    private static void grow() {
        int size = found.length * 2;
        Collider2D[] f = new Collider2D[size];
        System.arraycopy(found, 0, f, 0, count);
        found = f;
        ft = grown(ft, size);
        fnx = grown(fnx, size);
        fny = grown(fny, size);
        fpx = grown(fpx, size);
        fpy = grown(fpy, size);
        float[] z = new float[size];
        System.arraycopy(fz, 0, z, 0, count);
        fz = z;
    }

    private static double[] grown(double[] from, int size) {
        double[] to = new double[size];
        System.arraycopy(from, 0, to, 0, count);
        return to;
    }

    private static int indexOf(Collider2D c) {
        for (int i = 0; i < count; i++) {
            if (found[i] == c) { // NOPMD CompareObjectsWithEquals
                return i;
            }
        }
        return -1;
    }

    private static void record(Collider2D c, double t, double nx, double ny, double px, double py) {
        if (rule.useNormalAngle) {
            double degrees = com.codename1.unitycompat.system.Math.Atan2(ny, nx) * Transform.RAD2DEG;
            if (rule.IsFilteringNormalAngle((float) degrees)) {
                return;
            }
        }
        int at = indexOf(c);
        if (at < 0) {
            if (count == found.length) {
                grow();
            }
            at = count++;
            found[at] = c;
        } else if (!(t < ft[at])) { // NOPMD LogicInversion
            return;
        }
        ft[at] = t;
        fnx[at] = nx;
        fny[at] = ny;
        fpx[at] = px;
        fpy[at] = py;
    }

    /// Forgets the last query's results and brings the world up to date
    /// with what scripts created and destroyed.
    static void begin() {
        clear();
        PhysicsWorld.flush();
        if (Physics2D.autoSync) {
            PhysicsWorld.syncTransforms();
        }
    }

    /// Moves the shape along a unit direction and records every collider it
    /// meets within `length`, each where it is first met. `ray` says the
    /// shape is a point, which `queriesStartInColliders` applies to.
    static void sweep(double dx, double dy, double length, boolean ray) {
        int owners = PhysicsWorld.ownerCount();
        for (int i = 0; i < owners; i++) {
            GameObject go = PhysicsWorld.owner(i);
            ArrayList colliders = go.fixturesOf;
            if (colliders == null || go.body == null || !go.body.isActive()) {
                continue;
            }
            for (int j = 0; j < colliders.size(); j++) { // NOPMD ForLoopCanBeForeach
                Collider2D c = (Collider2D) colliders.get(j);
                if (!accepts(c)) {
                    continue;
                }
                int state = 0;
                double best = 0d;
                double bnx = 0d;
                double bny = 0d;
                double bpx = 0d;
                double bpy = 0d;
                for (int k = 0; k < c.fixtures.size() && state != 2; k++) {
                    Fixture f = (Fixture) c.fixtures.get(k);
                    int pieces = pieces(f);
                    for (int piece = 0; piece < pieces && state != 2; piece++) {
                        tn = load(f, piece, tx, ty);
                        tr = loadedRadius;
                        difference();
                        int r = enter(dx, dy, length, cr + tr);
                        if (r == 2) {
                            state = 2;
                        } else if (r == 1 && (state == 0 || outT < best)) {
                            state = 1;
                            best = outT;
                            bnx = outNx;
                            bny = outNy;
                            contact(dx, dy);
                            bpx = outPx;
                            bpy = outPy;
                        }
                    }
                }
                if (state == 2) {
                    if (!ray || Physics2D.startInColliders) {
                        record(c, 0d, -dx, -dy, originX, originY);
                    }
                } else if (state == 1) {
                    record(c, best, bnx, bny, bpx, bpy);
                }
            }
        }
    }

    /// Moves a collider's own shapes, leaving itself out and, when asked,
    /// every other collider of its body.
    static void sweep(Collider2D collider, boolean ignoreSiblings, double dx, double dy, double length) {
        Transform t = collider.gameObject.transform;
        t.update();
        originX = t.wx;
        originY = t.wy;
        skip = collider;
        skipOwner = ignoreSiblings ? collider.owner : null;
        for (int k = 0; k < collider.fixtures.size(); k++) {
            Fixture f = (Fixture) collider.fixtures.get(k);
            int pieces = pieces(f);
            for (int piece = 0; piece < pieces; piece++) {
                cn = load(f, piece, cx, cy);
                cr = loadedRadius;
                if (cn > 0) {
                    sweep(dx, dy, length, false);
                }
            }
        }
    }

    /// Puts the hits in the order they were met; of two met together, the
    /// one whose body was made first stays first. Answers how many.
    static int nearestFirst() {
        for (int i = 1; i < count; i++) {
            for (int j = i; j > 0 && ft[j] < ft[j - 1]; j--) {
                swap(j, j - 1);
            }
        }
        return count;
    }

    private static void swap(int a, int b) {
        Collider2D c = found[a];
        found[a] = found[b];
        found[b] = c;
        double d = ft[a];
        ft[a] = ft[b];
        ft[b] = d;
        d = fnx[a];
        fnx[a] = fnx[b];
        fnx[b] = d;
        d = fny[a];
        fny[a] = fny[b];
        fny[b] = d;
        d = fpx[a];
        fpx[a] = fpx[b];
        fpx[b] = d;
        d = fpy[a];
        fpy[a] = fpy[b];
        fpy[b] = d;
        float z = fz[a];
        fz[a] = fz[b];
        fz[b] = z;
    }

    /// Writes hit `i` of a cast along a unit direction and of a length.
    static void hit(int i, double dx, double dy, double length, RaycastHit2D to) {
        double t = ft[i];
        to.collider = found[i];
        to.distance = (float) t;
        to.fraction = length > 0d && length < Double.POSITIVE_INFINITY ? (float) (t / length) : 0f;
        to.centroidX = (float) (originX + t * dx);
        to.centroidY = (float) (originY + t * dy);
        to.pointX = (float) fpx[i];
        to.pointY = (float) fpy[i];
        to.normalX = (float) fnx[i];
        to.normalY = (float) fny[i];
    }

    // ------------------------------------------------------------- overlaps

    /// Records every collider the shape overlaps where it is; touching
    /// counts.
    static void overlap() {
        int owners = PhysicsWorld.ownerCount();
        for (int i = 0; i < owners; i++) {
            GameObject go = PhysicsWorld.owner(i);
            ArrayList colliders = go.fixturesOf;
            if (colliders == null || go.body == null || !go.body.isActive()) {
                continue;
            }
            for (int j = 0; j < colliders.size(); j++) { // NOPMD ForLoopCanBeForeach
                Collider2D c = (Collider2D) colliders.get(j);
                if (!accepts(c) || indexOf(c) >= 0) {
                    continue;
                }
                boolean over = false;
                for (int k = 0; k < c.fixtures.size() && !over; k++) {
                    Fixture f = (Fixture) c.fixtures.get(k);
                    int pieces = pieces(f);
                    for (int piece = 0; piece < pieces && !over; piece++) {
                        tn = load(f, piece, tx, ty);
                        tr = loadedRadius;
                        difference();
                        over = overlapping(cr + tr, true);
                    }
                }
                if (over) {
                    if (count == found.length) {
                        grow();
                    }
                    Transform t = c.gameObject.transform;
                    t.update();
                    fz[count] = t.wz;
                    found[count++] = c;
                }
            }
        }
    }

    /// The same for a collider's own shapes, leaving itself out.
    static void overlap(Collider2D collider) {
        skip = collider;
        skipOwner = null;
        for (int k = 0; k < collider.fixtures.size(); k++) {
            Fixture f = (Fixture) collider.fixtures.get(k);
            int pieces = pieces(f);
            for (int piece = 0; piece < pieces; piece++) {
                cn = load(f, piece, cx, cy);
                cr = loadedRadius;
                if (cn > 0) {
                    overlap();
                }
            }
        }
    }

    /// Whether the shape overlaps one given collider, whatever the filter.
    static boolean overlaps(Collider2D c) {
        for (int k = 0; k < c.fixtures.size(); k++) {
            Fixture f = (Fixture) c.fixtures.get(k);
            int pieces = pieces(f);
            for (int piece = 0; piece < pieces; piece++) {
                tn = load(f, piece, tx, ty);
                tr = loadedRadius;
                difference();
                if (overlapping(cr + tr, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Puts the overlapped colliders in the order a camera looking along z
    /// meets them: least z first, and of equals the first made. Answers how
    /// many.
    static int frontFirst() {
        for (int i = 1; i < count; i++) {
            for (int j = i; j > 0 && fz[j] < fz[j - 1]; j--) {
                swap(j, j - 1);
            }
        }
        return count;
    }

    static Collider2D collider(int i) {
        return found[i];
    }
}
