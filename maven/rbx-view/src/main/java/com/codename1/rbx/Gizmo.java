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
package com.codename1.rbx;

import java.util.ArrayList;
import java.util.List;

/// The studio viewport's selection outline and transform handles, and what
/// a drag on them does to the selection.
///
/// Handles are drawn as unlit bars over the finished frame, sized by their
/// distance from the eye so they keep their size on screen at any zoom. A
/// drag snaps: a stud for a move or a resize, 15 degrees for a turn, as the
/// grid an editor builds on.
public final class Gizmo {
    /// Selects only: an outline, no handles.
    public static final int SELECT = 0;
    /// Arrows along the world axes; a drag on the part slides it over the
    /// ground plane.
    public static final int MOVE = 1;
    /// A ring about the vertical; a drag turns the selection.
    public static final int ROTATE = 2;
    /// A handle on every face; a drag pulls that face out or in.
    public static final int SCALE = 3;

    static final double SNAP = 1;
    static final double TURN = Math.toRadians(15);
    static final int OUTLINE = 0xff4cc2ff;
    static final int[] AXIS_COLORS = {0xffff5a52, 0xff5bd16a, 0xff4c8dff};
    static final int RING = 0xff5bd16a;

    private int mode = MOVE;
    private Instance selection;

    // the drag under way
    private int grabbed = -1; // axis 0..2, face 0..5 for scale, 9 the ground plane, 10 a turn
    private CFrame[] startFrames = new CFrame[0];
    private Vector3[] startSizes = new Vector3[0];
    private List<Instance> parts = new ArrayList<Instance>();
    private double[] startCentre = new double[4];
    private double startX;
    private double startY;
    private double[] startGround = new double[3];

    public void setMode(int m) {
        mode = m;
    }

    public int getMode() {
        return mode;
    }

    public void setSelection(Instance i) {
        selection = i;
        grabbed = -1;
    }

    public Instance getSelection() {
        return selection;
    }

    /// True while a drag on a handle or the selection is under way.
    public boolean isDragging() {
        return grabbed >= 0;
    }

    // ---- geometry ----

    /// Every part of `i`: itself when it is one, its descendants otherwise.
    static List<Instance> partsOf(Instance i) {
        List<Instance> out = new ArrayList<Instance>();
        collect(i, out);
        return out;
    }

    private static void collect(Instance i, List<Instance> out) {
        if (i.isA("BasePart") && i.prop("Size") instanceof Vector3) {
            out.add(i);
        }
        List<Instance> kids = i.getChildren();
        for (int k = 0; k < kids.size(); k++) { // NOPMD ForLoopCanBeForeach - may be large
            collect(kids.get(k), out);
        }
    }

    /// The world-aligned bounds of `ps`: min x, y, z then max x, y, z.
    static double[] bounds(List<Instance> ps) {
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - may be large
            Instance p = ps.get(i);
            CFrame f = Behaviors.cf(p);
            Vector3 s = (Vector3) p.prop("Size");
            // the box's half extent along each world axis
            double ex = Math.abs(f.r00) * s.x / 2 + Math.abs(f.r01) * s.y / 2 + Math.abs(f.r02) * s.z / 2;
            double ey = Math.abs(f.r10) * s.x / 2 + Math.abs(f.r11) * s.y / 2 + Math.abs(f.r12) * s.z / 2;
            double ez = Math.abs(f.r20) * s.x / 2 + Math.abs(f.r21) * s.y / 2 + Math.abs(f.r22) * s.z / 2;
            b[0] = Math.min(b[0], f.x - ex);
            b[1] = Math.min(b[1], f.y - ey);
            b[2] = Math.min(b[2], f.z - ez);
            b[3] = Math.max(b[3], f.x + ex);
            b[4] = Math.max(b[4], f.y + ey);
            b[5] = Math.max(b[5], f.z + ez);
        }
        return b;
    }

    /// The selection drawn as one box: a part's own oriented box, or the
    /// world-aligned box around a model. Centre, rotation and size.
    private static CFrame boxFrame(Instance sel, List<Instance> ps, double[] size) {
        if (ps.size() == 1 && ps.get(0) == sel) { // NOPMD CompareObjectsWithEquals - tree identity
            Vector3 s = (Vector3) sel.prop("Size");
            size[0] = s.x;
            size[1] = s.y;
            size[2] = s.z;
            return Behaviors.cf(sel);
        }
        double[] b = bounds(ps);
        size[0] = b[3] - b[0];
        size[1] = b[4] - b[1];
        size[2] = b[5] - b[2];
        return new CFrame((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2, 1, 0, 0, 0, 1, 0, 0, 0, 1);
    }

    // ---- drawing ----

    /// The bars to draw this frame for an eye at `eye`: 16 matrix floats
    /// then the ARGB colour (as float bits) per bar. Empty with nothing
    /// selected.
    float[] guides(double ex, double ey, double ez) {
        if (selection == null || selection.getParent() == null) {
            return new float[0];
        }
        List<Instance> ps = partsOf(selection);
        if (ps.isEmpty()) {
            return new float[0];
        }
        double[] size = new double[3];
        CFrame f = boxFrame(selection, ps, size);
        double d = Math.max(4, Math.sqrt((f.x - ex) * (f.x - ex) + (f.y - ey) * (f.y - ey) + (f.z - ez) * (f.z - ez)));
        List<float[]> bars = new ArrayList<float[]>();
        double t = d * 0.004;
        // the twelve edges of the box
        double[] h = {size[0] / 2, size[1] / 2, size[2] / 2};
        for (int axis = 0; axis < 3; axis++) {
            int a1 = (axis + 1) % 3;
            int a2 = (axis + 2) % 3;
            for (int s1 = -1; s1 <= 1; s1 += 2) {
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    double[] c = new double[3];
                    c[a1] = s1 * h[a1];
                    c[a2] = s2 * h[a2];
                    double[] len = {t, t, t};
                    len[axis] = size[axis] + t;
                    bars.add(bar(f, c, len, OUTLINE));
                }
            }
        }
        CFrame world = new CFrame(f.x, f.y, f.z, 1, 0, 0, 0, 1, 0, 0, 0, 1);
        double[] wh = worldHalf(f, h);
        if (mode == MOVE) {
            for (int axis = 0; axis < 3; axis++) {
                double reach = wh[axis] + d * 0.16;
                double[] c = new double[3];
                c[axis] = reach / 2;
                double[] len = {d * 0.008, d * 0.008, d * 0.008};
                len[axis] = reach;
                bars.add(bar(world, c, len, AXIS_COLORS[axis]));
                double[] tip = new double[3];
                tip[axis] = reach;
                double k = d * 0.03;
                bars.add(bar(world, tip, new double[] {k, k, k}, AXIS_COLORS[axis]));
            }
        } else if (mode == ROTATE) {
            double r = Math.max(wh[0], wh[2]) + d * 0.08;
            int n = 48;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * (i + 0.5) / n;
                double seg = Math.PI * 2 * r / n;
                double c = Math.cos(a);
                double s = Math.sin(a);
                // a short bar tangent to the ring
                CFrame tf = new CFrame(f.x + c * r, f.y, f.z + s * r, -s, 0, c, 0, 1, 0, c, 0, s);
                bars.add(bar(tf, new double[3], new double[] {seg * 1.05, d * 0.008, d * 0.008}, RING));
            }
        } else if (mode == SCALE && isPart(ps)) {
            for (int face = 0; face < 6; face++) {
                int axis = face / 2;
                double sgn = face % 2 == 0 ? 1 : -1;
                double[] c = new double[3];
                c[axis] = sgn * (h[axis] + d * 0.03);
                double k = d * 0.028;
                bars.add(bar(f, c, new double[] {k, k, k}, AXIS_COLORS[axis]));
            }
        }
        float[] out = new float[bars.size() * 17];
        for (int i = 0; i < bars.size(); i++) {
            System.arraycopy(bars.get(i), 0, out, i * 17, 17);
        }
        return out;
    }

    private boolean isPart(List<Instance> ps) {
        return ps.size() == 1 && ps.get(0) == selection; // NOPMD CompareObjectsWithEquals - tree identity
    }

    /// The box's half extent along each world axis.
    private static double[] worldHalf(CFrame f, double[] h) {
        return new double[] {
            Math.abs(f.r00) * h[0] + Math.abs(f.r01) * h[1] + Math.abs(f.r02) * h[2],
            Math.abs(f.r10) * h[0] + Math.abs(f.r11) * h[1] + Math.abs(f.r12) * h[2],
            Math.abs(f.r20) * h[0] + Math.abs(f.r21) * h[1] + Math.abs(f.r22) * h[2]};
    }

    /// A unit cube scaled by `len` along `f`'s axes and centred at `c` in
    /// `f`'s frame, as a column-major matrix plus a colour.
    static float[] bar(CFrame f, double[] c, double[] len, int argb) {
        float[] m = new float[17];
        double[][] cols = {{f.r00, f.r10, f.r20}, {f.r01, f.r11, f.r21}, {f.r02, f.r12, f.r22}};
        for (int j = 0; j < 3; j++) {
            m[j * 4] = (float) (cols[j][0] * len[j]);
            m[j * 4 + 1] = (float) (cols[j][1] * len[j]);
            m[j * 4 + 2] = (float) (cols[j][2] * len[j]);
        }
        m[12] = (float) (f.x + cols[0][0] * c[0] + cols[1][0] * c[1] + cols[2][0] * c[2]);
        m[13] = (float) (f.y + cols[0][1] * c[0] + cols[1][1] * c[1] + cols[2][1] * c[2]);
        m[14] = (float) (f.z + cols[0][2] * c[0] + cols[1][2] * c[1] + cols[2][2] * c[2]);
        m[15] = 1;
        m[16] = Float.intBitsToFloat(argb);
        return m;
    }

    // ---- dragging ----

    /// Where world point (x, y, z) lands in a `w` by `h` view, or null
    /// behind the eye.
    static double[] project(Engine e, double x, double y, double z, int w, int h) {
        Object c = e.getWorkspace().prop("CurrentCamera");
        if (!(c instanceof Instance)) {
            return null;
        }
        Instance cam = (Instance) c;
        CFrame f = Behaviors.cf(cam);
        double fov = cam.num("FieldOfView");
        double ty = Math.tan(Math.toRadians(fov <= 0 ? 70 : fov) / 2);
        double tx = ty * w / h;
        double rx = x - f.x;
        double ry = y - f.y;
        double rz = z - f.z;
        double vx = f.r00 * rx + f.r10 * ry + f.r20 * rz;
        double vy = f.r01 * rx + f.r11 * ry + f.r21 * rz;
        double vz = f.r02 * rx + f.r12 * ry + f.r22 * rz;
        if (vz >= -1e-6) {
            return null;
        }
        return new double[] {(vx / -vz / tx + 1) / 2 * w, (1 - vy / -vz / ty) / 2 * h};
    }

    /// The point where the view ray through (px, py) meets the horizontal
    /// plane at height `planeY`, or null when it never does.
    static double[] ground(Engine e, double px, double py, int w, int h, double planeY) {
        Instance cam = (Instance) e.getWorkspace().prop("CurrentCamera");
        CFrame f = Behaviors.cf(cam);
        double fov = cam.num("FieldOfView");
        double ty = Math.tan(Math.toRadians(fov <= 0 ? 70 : fov) / 2);
        double tx = ty * w / h;
        double nx = (2.0 * px / w - 1) * tx;
        double ny = (1 - 2.0 * py / h) * ty;
        double dx = f.r00 * nx + f.r01 * ny - f.r02;
        double dy = f.r10 * nx + f.r11 * ny - f.r12;
        double dz = f.r20 * nx + f.r21 * ny - f.r22;
        if (Math.abs(dy) < 1e-6) {
            return null;
        }
        double t = (planeY - f.y) / dy;
        if (t <= 0 || t > 5000) {
            return null;
        }
        return new double[] {f.x + dx * t, planeY, f.z + dz * t};
    }

    /// Starts a drag when (px, py) is on a handle or, for a move or a
    /// turn, on the selection itself. False leaves the pointer to the
    /// camera.
    boolean press(Engine e, int px, int py, int w, int h, Instance under) {
        grabbed = -1;
        if (selection == null || mode == SELECT || selection.getParent() == null) {
            return false;
        }
        parts = partsOf(selection);
        if (parts.isEmpty()) {
            return false;
        }
        double[] size = new double[3];
        CFrame f = boxFrame(selection, parts, size);
        CFrame eye = Behaviors.cf((Instance) e.getWorkspace().prop("CurrentCamera"));
        double d = Math.max(4, Math.sqrt((f.x - eye.x) * (f.x - eye.x) + (f.y - eye.y) * (f.y - eye.y)
                + (f.z - eye.z) * (f.z - eye.z)));
        double[] hh = {size[0] / 2, size[1] / 2, size[2] / 2};
        double[] wh = worldHalf(f, hh);
        double grab = Math.max(12, Math.min(w, h) * 0.035);
        double best = grab;
        if (mode == MOVE) {
            for (int axis = 0; axis < 3; axis++) {
                double[] tip = {f.x, f.y, f.z};
                tip[axis] += wh[axis] + d * 0.16;
                double[] s = project(e, tip[0], tip[1], tip[2], w, h);
                if (s != null && Math.hypot(s[0] - px, s[1] - py) < best) {
                    best = Math.hypot(s[0] - px, s[1] - py);
                    grabbed = axis;
                }
            }
        } else if (mode == SCALE && isPart(parts)) {
            for (int face = 0; face < 6; face++) {
                int axis = face / 2;
                double sgn = face % 2 == 0 ? 1 : -1;
                double off = sgn * (hh[axis] + d * 0.03);
                double[] s = project(e, f.x + col(f, axis)[0] * off, f.y + col(f, axis)[1] * off,
                        f.z + col(f, axis)[2] * off, w, h);
                if (s != null && Math.hypot(s[0] - px, s[1] - py) < best) {
                    best = Math.hypot(s[0] - px, s[1] - py);
                    grabbed = face;
                }
            }
        }
        if (grabbed < 0 && under != null && (under == selection || under.isDescendantOf(selection))) { // NOPMD CompareObjectsWithEquals - tree identity
            if (mode == MOVE) {
                grabbed = 9;
            } else if (mode == ROTATE) {
                grabbed = 10;
            }
        }
        if (grabbed < 0) {
            return false;
        }
        startFrames = new CFrame[parts.size()];
        startSizes = new Vector3[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            startFrames[i] = Behaviors.cf(parts.get(i));
            startSizes[i] = (Vector3) parts.get(i).prop("Size");
        }
        startCentre = new double[] {f.x, f.y, f.z, d};
        startX = px;
        startY = py;
        startGround = ground(e, px, py, w, h, f.y);
        return true;
    }

    private static double[] col(CFrame f, int axis) {
        switch (axis) {
            case 0:
                return new double[] {f.r00, f.r10, f.r20};
            case 1:
                return new double[] {f.r01, f.r11, f.r21};
            default:
                return new double[] {f.r02, f.r12, f.r22};
        }
    }

    static double snap(double v, double step) {
        return Math.round(v / step) * step;
    }

    /// Moves the drag to (px, py) and applies it to the selection.
    void drag(Engine e, int px, int py, int w, int h) {
        if (grabbed < 0) {
            return;
        }
        double cx = startCentre[0];
        double cy = startCentre[1];
        double cz = startCentre[2];
        if (grabbed == 9) {
            double[] g = ground(e, px, py, w, h, cy);
            if (g == null || startGround == null) {
                return;
            }
            translate(snap(g[0] - startGround[0], SNAP), 0, snap(g[2] - startGround[2], SNAP));
        } else if (grabbed == 10) {
            turn(snap(-(px - startX) * 0.012, TURN), cx, cz);
        } else if (mode == MOVE) {
            double amount = alongAxis(e, new double[] {cx, cy, cz}, unit(grabbed), px, py, w, h);
            double[] dv = new double[3];
            dv[grabbed] = snap(amount, SNAP);
            translate(dv[0], dv[1], dv[2]);
        } else if (mode == SCALE) {
            int axis = grabbed / 2;
            double sgn = grabbed % 2 == 0 ? 1 : -1;
            CFrame f = startFrames[0];
            double[] a = col(f, axis);
            double[] dir = {a[0] * sgn, a[1] * sgn, a[2] * sgn};
            double amount = snap(alongAxis(e, new double[] {cx, cy, cz}, dir, px, py, w, h), SNAP);
            Vector3 s0 = startSizes[0];
            double[] sz = {s0.x, s0.y, s0.z};
            double grown = Math.max(0.2, sz[axis] + amount);
            double moved = (grown - sz[axis]) / 2;
            sz[axis] = grown;
            Instance p = parts.get(0);
            p.put("Size", new Vector3(sz[0], sz[1], sz[2]));
            p.put("CFrame", new CFrame(f.x + dir[0] * moved, f.y + dir[1] * moved, f.z + dir[2] * moved,
                    f.r00, f.r01, f.r02, f.r10, f.r11, f.r12, f.r20, f.r21, f.r22));
        }
    }

    /// Ends the drag; true when it changed anything worth an undo step.
    boolean release() {
        boolean was = grabbed >= 0;
        grabbed = -1;
        return was;
    }

    private static double[] unit(int axis) {
        double[] u = new double[3];
        u[axis] = 1;
        return u;
    }

    /// How far along the world direction `dir` from `c` the pointer has
    /// moved since the press, in studs: the screen drag projected onto the
    /// direction's own screen image.
    private double alongAxis(Engine e, double[] c, double[] dir, int px, int py, int w, int h) {
        double k = startCentre[3] * 0.2;
        double[] s0 = project(e, c[0], c[1], c[2], w, h);
        double[] s1 = project(e, c[0] + dir[0] * k, c[1] + dir[1] * k, c[2] + dir[2] * k, w, h);
        if (s0 == null || s1 == null) {
            return 0;
        }
        double ax = s1[0] - s0[0];
        double ay = s1[1] - s0[1];
        double l2 = ax * ax + ay * ay;
        if (l2 < 1) {
            return 0;
        }
        return ((px - startX) * ax + (py - startY) * ay) / l2 * k;
    }

    private void translate(double dx, double dy, double dz) {
        for (int i = 0; i < parts.size(); i++) {
            CFrame f = startFrames[i];
            parts.get(i).put("CFrame", new CFrame(f.x + dx, f.y + dy, f.z + dz,
                    f.r00, f.r01, f.r02, f.r10, f.r11, f.r12, f.r20, f.r21, f.r22));
        }
    }

    private void turn(double a, double px, double pz) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        for (int i = 0; i < parts.size(); i++) {
            CFrame f = startFrames[i];
            double rx = f.x - px;
            double rz = f.z - pz;
            // Ry * R, and the position about the pivot
            parts.get(i).put("CFrame", new CFrame(px + c * rx + s * rz, f.y, pz - s * rx + c * rz,
                    c * f.r00 + s * f.r20, c * f.r01 + s * f.r21, c * f.r02 + s * f.r22,
                    f.r10, f.r11, f.r12,
                    -s * f.r00 + c * f.r20, -s * f.r01 + c * f.r21, -s * f.r02 + c * f.r22));
        }
    }
}
