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
package com.codename1.rbx;

import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceNode;
import com.codename1.rbx.place.PlaceValue;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.util.ArrayList;
import java.util.List;

/// A picture of a place file from a three-quarter view above it, painted in
/// pure Java: the parts are projected, shaded by the sun and drawn back to
/// front over a sky. It needs no GPU, so a store page can show what an
/// experience looks like before anything is played, on every target.
///
/// The geometry ([#faces]) is separate from the painting, so it is tested
/// without a display.
public final class PlaceThumbnail {
    /// Material token values (the `Material` enum) drawn specially.
    private static final int NEON = 288;
    private static final int CRACKED_LAVA = 804;
    private static final int GLASS = 1568;
    private static final int FORCE_FIELD = 1584;
    private static final int WATER = 2048;
    /// A part bigger than this on any axis is scenery (a sea, a baseplate):
    /// it is drawn but does not decide where the camera looks.
    private static final double SCENERY = 150;
    private static final int SKY_TOP = 0x3f7fd8;
    private static final int SKY_HORIZON = 0xb8d4f0;
    private static final double NEAR = 0.5;

    private PlaceThumbnail() {
    }

    /// One projected, shaded polygon.
    public static final class Face {
        /// Screen coordinates.
        public final int[] xs;
        public final int[] ys;
        public final int rgb;
        /// Distance from the eye; faces are drawn largest first.
        public final double depth;

        Face(int[] xs, int[] ys, int rgb, double depth) {
            this.xs = xs;
            this.ys = ys;
            this.rgb = rgb;
            this.depth = depth;
        }
    }

    /// Paints the place into a new `w` x `h` image.
    public static Image render(PlaceFile f, int w, int h) {
        Image img = Image.createImage(w, h, 0xff000000 | SKY_HORIZON);
        Graphics g = img.getGraphics();
        g.fillLinearGradient(SKY_TOP, SKY_HORIZON, 0, 0, w, h, false);
        // A soft sun high on the right: the warm glow every key-art sky has.
        g.fillRadialGradient(0xfff6d8, SKY_HORIZON, w * 5 / 8, -h / 3, w / 2, h * 3 / 4);
        g.setAntiAliased(true);
        List<Face> faces = faces(f, w, h);
        int edge = Math.max(1, w / 400);
        for (int i = 0; i < faces.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed walk of a sorted list
            Face face = faces.get(i);
            g.setColor(face.rgb);
            g.fillPolygon(face.xs, face.ys, face.xs.length);
            // A darker edge on near faces gives the crisp, toy-like outline
            // that reads at icon size; far faces would only add noise.
            if (face.depth < 260 && edge > 0) {
                g.setColor(shade(face.rgb, 0.72));
                g.drawPolygon(face.xs, face.ys, face.xs.length);
            }
        }
        vignette(g, w, h);
        return img;
    }

    /// The faces to paint, sorted back to front.
    public static List<Face> faces(PlaceFile f, int w, int h) {
        List<double[]> parts = new ArrayList<double[]>();
        List<PlaceNode> all = f.all();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed walk
            double[] p = part(all.get(i));
            if (p != null) {
                parts.add(p);
            }
        }
        List<Face> out = new ArrayList<Face>();
        if (parts.isEmpty()) {
            return out;
        }
        double[] eye = new double[3];
        double[] basis = new double[9];
        double focal = frame(parts, w, h, eye, basis);
        // Scenery (a sea, a baseplate) is one polygon whose centre can be
        // nearer than things standing far off on it; sorted with the rest it
        // would be painted over them. It is the ground, so it goes first.
        List<Face> ground = new ArrayList<Face>();
        for (int i = 0; i < parts.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed walk
            double[] p = parts.get(i);
            emit(p, eye, basis, focal, w, h, isScenery(p) ? ground : out);
        }
        sortByDepth(ground);
        sortByDepth(out);
        ground.addAll(out);
        return ground;
    }

    /// The part's 12-float frame (position, rotation), size, colour, material,
    /// transparency and shape; null for anything that is not a visible part.
    static double[] part(PlaceNode n) {
        String c = n.className;
        if (!("Part".equals(c) || "SpawnLocation".equals(c) || "WedgePart".equals(c)
                || "CornerWedgePart".equals(c) || "Seat".equals(c) || "TrussPart".equals(c)
                || "MeshPart".equals(c) || "UnionOperation".equals(c))) {
            return null;
        }
        Object cf = value(n, "CFrame", null);
        Object size = value(n, "size", "Size");
        if (!(cf instanceof CFrame) || !(size instanceof Vector3)) {
            return null;
        }
        double tr = num(value(n, "Transparency", null), 0);
        if (tr >= 0.9) {
            return null;
        }
        Object col = value(n, "Color3uint8", "Color");
        int rgb = col instanceof Color3 ? SceneSnapshot.rgb(col) : 0xa3a2a5;
        int material = (int) num(value(n, "Material", null), 256);
        int shape;
        if ("WedgePart".equals(c)) {
            shape = SceneSnapshot.WEDGE;
        } else if ("CornerWedgePart".equals(c)) {
            shape = SceneSnapshot.CORNER_WEDGE;
        } else {
            // PartType: Ball 0, Block 1, Cylinder 2.
            int t = (int) num(value(n, "shape", "Shape"), 1);
            shape = t == 0 ? SceneSnapshot.BALL : t == 2 ? SceneSnapshot.CYLINDER : SceneSnapshot.BLOCK;
        }
        CFrame m = (CFrame) cf;
        Vector3 s = (Vector3) size;
        return new double[] {m.x, m.y, m.z, m.r00, m.r01, m.r02, m.r10, m.r11, m.r12, m.r20, m.r21, m.r22,
            s.x, s.y, s.z, rgb, material, tr, shape};
    }

    private static Object value(PlaceNode n, String a, String b) {
        PlaceValue v = n.props.get(a);
        if (v == null && b != null) {
            v = n.props.get(b);
        }
        return v == null ? null : v.value;
    }

    private static double num(Object v, double def) {
        if (v instanceof Double) {
            return ((Double) v).doubleValue();
        }
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        return def;
    }

    /// Places the eye up and to the side of the gameplay parts, looking at
    /// their middle, far enough back that they fill the picture. Writes the
    /// eye and the camera basis (right, up, forward) and returns the focal
    /// length in pixels.
    static double frame(List<double[]> parts, int w, int h, double[] eye, double[] basis) {
        // Frame the middle of the place: the 15th to 85th percentile of part
        // positions, so a few outlying props (rock stacks, a far tower) do
        // not shrink the course to a speck.
        List<double[]> used = new ArrayList<double[]>();
        for (int i = 0; i < parts.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed walk
            if (!isScenery(parts.get(i))) {
                used.add(parts.get(i));
            }
        }
        double minX = -10;
        double minY = -10;
        double minZ = -10;
        double maxX = 10;
        double maxY = 10;
        double maxZ = 10;
        if (!used.isEmpty()) {
            double[] xs = new double[used.size()];
            double[] ys = new double[used.size()];
            double[] zs = new double[used.size()];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = used.get(i)[0];
                ys[i] = used.get(i)[1];
                zs[i] = used.get(i)[2];
            }
            java.util.Arrays.sort(xs);
            java.util.Arrays.sort(ys);
            java.util.Arrays.sort(zs);
            int lo = (int) (xs.length * 0.15);
            int hi = Math.max(lo, (int) Math.ceil(xs.length * 0.85) - 1);
            minX = xs[lo];
            maxX = xs[hi];
            minY = ys[lo];
            maxY = ys[hi];
            minZ = zs[lo];
            maxZ = zs[hi];
        }
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;
        double cz = (minZ + maxZ) / 2;
        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        double radius = Math.max(12, Math.sqrt(dx * dx + dy * dy + dz * dz) / 2);
        // A three-quarter view: from the start side of a course (+Z), raised
        // and turned, the way an experience's icon is usually shot.
        double yaw = Math.toRadians(32);
        // Shallow enough that the horizon stays inside a 16:9 frame (its
        // vertical half angle is about 15 degrees): a sky, not a wall of floor.
        double pitch = Math.toRadians(24);
        double fov = Math.toRadians(50);
        double dist = radius / Math.sin(fov / 2) * 0.55;
        double fx = Math.sin(yaw) * Math.cos(pitch);
        double fy = Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        eye[0] = cx + fx * dist;
        eye[1] = cy + fy * dist;
        eye[2] = cz + fz * dist;
        // forward = from the eye to the centre
        double kx = -fx;
        double ky = -fy;
        double kz = -fz;
        // right = forward x up
        double rx = -kz;
        double rz = kx;
        double rl = Math.sqrt(rx * rx + rz * rz);
        rx /= rl;
        rz /= rl;
        // up = right x forward
        double ux = -rz * ky;
        double uy = rz * kx - rx * kz;
        double uz = rx * ky;
        basis[0] = rx;
        basis[1] = 0;
        basis[2] = rz;
        basis[3] = ux;
        basis[4] = uy;
        basis[5] = uz;
        basis[6] = kx;
        basis[7] = ky;
        basis[8] = kz;
        return Math.max(w, h) / 2.0 / Math.tan(fov / 2);
    }

    static boolean isScenery(double[] p) {
        return p[12] > SCENERY || p[13] > SCENERY || p[14] > SCENERY;
    }

    /// The part's polygons in its own unit space: each is a list of x,y,z.
    static float[][] polygons(int shape) {
        float h = 0.5f;
        switch (shape) {
            case SceneSnapshot.CYLINDER: {
                int n = 16;
                float[][] out = new float[n + 2][];
                float[] capA = new float[n * 3];
                float[] capB = new float[n * 3];
                for (int i = 0; i < n; i++) {
                    double a0 = 2 * Math.PI * i / n;
                    double a1 = 2 * Math.PI * (i + 1) / n;
                    float y0 = (float) (Math.cos(a0) * h);
                    float z0 = (float) (Math.sin(a0) * h);
                    float y1 = (float) (Math.cos(a1) * h);
                    float z1 = (float) (Math.sin(a1) * h);
                    out[i] = new float[] {-h, y0, z0, -h, y1, z1, h, y1, z1, h, y0, z0};
                    capA[i * 3] = h;
                    capA[i * 3 + 1] = y0;
                    capA[i * 3 + 2] = z0;
                    int j = n - 1 - i;
                    capB[j * 3] = -h;
                    capB[j * 3 + 1] = y0;
                    capB[j * 3 + 2] = z0;
                }
                out[n] = capA;
                out[n + 1] = capB;
                return out;
            }
            case SceneSnapshot.BALL: {
                int lat = 8;
                int lon = 12;
                float[][] out = new float[lat * lon][];
                for (int a = 0; a < lat; a++) {
                    double t0 = Math.PI * a / lat - Math.PI / 2;
                    double t1 = Math.PI * (a + 1) / lat - Math.PI / 2;
                    for (int b = 0; b < lon; b++) {
                        double p0 = 2 * Math.PI * b / lon;
                        double p1 = 2 * Math.PI * (b + 1) / lon;
                        // counter-clockwise from outside; at a pole two corners
                        // meet, which [#outward] tolerates
                        out[a * lon + b] = new float[] {
                            sx(t1, p0), (float) (Math.sin(t1) * h), sz(t1, p0),
                            sx(t1, p1), (float) (Math.sin(t1) * h), sz(t1, p1),
                            sx(t0, p1), (float) (Math.sin(t0) * h), sz(t0, p1),
                            sx(t0, p0), (float) (Math.sin(t0) * h), sz(t0, p0),
                        };
                    }
                }
                return out;
            }
            case SceneSnapshot.WEDGE:
                return triangles(PartMeshes.wedgeTriangles());
            case SceneSnapshot.CORNER_WEDGE:
                return triangles(PartMeshes.cornerWedgeTriangles());
            default:
                return new float[][] {
                    {-h, -h, h, h, -h, h, h, h, h, -h, h, h},
                    {h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h},
                    {h, -h, h, h, -h, -h, h, h, -h, h, h, h},
                    {-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h},
                    {-h, h, h, h, h, h, h, h, -h, -h, h, -h},
                    {-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h},
                };
        }
    }

    private static float sx(double t, double p) {
        return (float) (Math.cos(t) * Math.cos(p) * 0.5);
    }

    private static float sz(double t, double p) {
        return (float) (Math.cos(t) * Math.sin(p) * 0.5);
    }

    private static float[][] triangles(float[] t) {
        float[][] out = new float[t.length / 9][];
        for (int i = 0; i < out.length; i++) {
            out[i] = new float[9];
            System.arraycopy(t, i * 9, out[i], 0, 9);
        }
        return out;
    }

    private static void emit(double[] p, double[] eye, double[] b, double focal, int w, int h, List<Face> out) {
        float[][] polys = polygons((int) p[18]);
        int base = (int) p[15];
        int material = (int) p[16];
        boolean glow = material == NEON || material == CRACKED_LAVA;
        boolean clear = material == GLASS || material == FORCE_FIELD || material == WATER || p[17] > 0.3;
        // sun from the upper left, behind the eye's shoulder
        double lx = -0.45;
        double ly = 0.8;
        double lz = 0.4;
        double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll;
        ly /= ll;
        lz /= ll;
        for (float[] poly : polys) {
            int n = poly.length / 3;
            double[] wx = new double[n];
            double[] wy = new double[n];
            double[] wz = new double[n];
            double mx = 0;
            double my = 0;
            double mz = 0;
            for (int i = 0; i < n; i++) {
                double ux = poly[i * 3] * p[12];
                double uy = poly[i * 3 + 1] * p[13];
                double uz = poly[i * 3 + 2] * p[14];
                wx[i] = p[0] + p[3] * ux + p[4] * uy + p[5] * uz;
                wy[i] = p[1] + p[6] * ux + p[7] * uy + p[8] * uz;
                wz[i] = p[2] + p[9] * ux + p[10] * uy + p[11] * uz;
                mx += wx[i];
                my += wy[i];
                mz += wz[i];
            }
            mx /= n;
            my /= n;
            mz /= n;
            double[] nrm = outward(wx, wy, wz);
            if (nrm == null) {
                continue;
            }
            double nx = nrm[0];
            double ny = nrm[1];
            double nz = nrm[2];
            if (nx * (eye[0] - mx) + ny * (eye[1] - my) + nz * (eye[2] - mz) <= 0) {
                continue;
            }
            // Camera space, then clipped against the near plane: a big part
            // (a sea, a baseplate) always has corners behind the eye.
            double[] cxs = new double[n];
            double[] cys = new double[n];
            double[] czs = new double[n];
            for (int i = 0; i < n; i++) {
                double vx = wx[i] - eye[0];
                double vy = wy[i] - eye[1];
                double vz = wz[i] - eye[2];
                cxs[i] = vx * b[0] + vy * b[1] + vz * b[2];
                cys[i] = vx * b[3] + vy * b[4] + vz * b[5];
                czs[i] = vx * b[6] + vy * b[7] + vz * b[8];
            }
            double[][] clipped = clipNear(cxs, cys, czs, NEAR);
            if (clipped == null) {
                continue;
            }
            int m = clipped[0].length;
            int[] xs = new int[m];
            int[] ys = new int[m];
            for (int i = 0; i < m; i++) {
                xs[i] = (int) Math.round(w / 2.0 + clipped[0][i] * focal / clipped[2][i]);
                ys[i] = (int) Math.round(h / 2.0 - clipped[1][i] * focal / clipped[2][i]);
            }
            double depth = Math.sqrt((mx - eye[0]) * (mx - eye[0]) + (my - eye[1]) * (my - eye[1])
                    + (mz - eye[2]) * (mz - eye[2]));
            int rgb;
            if (material == CRACKED_LAVA) {
                // dark crust over the glow, as the lava shader draws it
                rgb = mix(base, 0x3a1206, 0.45);
            } else if (glow) {
                rgb = base;
            } else {
                double lambert = Math.max(0, nx * lx + ny * ly + nz * lz);
                // sky light from above and a little bounce from below
                double k = 0.42 + 0.12 * ny + 0.62 * lambert;
                rgb = shade(base, k);
            }
            if (clear) {
                rgb = mix(rgb, SKY_HORIZON, 0.45);
            }
            // aerial perspective: far things fade toward the horizon
            rgb = mix(rgb, SKY_HORIZON, Math.min(0.55, depth / 900));
            out.add(new Face(xs, ys, rgb, depth));
        }
    }

    /// The unit outward normal of a polygon listed counter-clockwise from
    /// outside, or null for a degenerate one.
    static double[] outward(double[] wx, double[] wy, double[] wz) {
        // Newell's method: sums over every edge, so a corner that collapses
        // (a sphere's pole) still yields the right normal.
        int n = wx.length;
        double nx = 0;
        double ny = 0;
        double nz = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            nx += (wy[i] - wy[j]) * (wz[i] + wz[j]);
            ny += (wz[i] - wz[j]) * (wx[i] + wx[j]);
            nz += (wx[i] - wx[j]) * (wy[i] + wy[j]);
        }
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-12) {
            return null;
        }
        return new double[] {nx / nl, ny / nl, nz / nl};
    }

    /// The part of a camera-space polygon in front of `near`, as {x, y, z}
    /// arrays; null when nothing of it is.
    static double[][] clipNear(double[] x, double[] y, double[] z, double near) {
        int n = x.length;
        double[] ox = new double[n * 2];
        double[] oy = new double[n * 2];
        double[] oz = new double[n * 2];
        int m = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            boolean in = z[i] >= near;
            if (in) {
                ox[m] = x[i];
                oy[m] = y[i];
                oz[m] = z[i];
                m++;
            }
            if (in != (z[j] >= near)) {
                double t = (near - z[i]) / (z[j] - z[i]);
                ox[m] = x[i] + (x[j] - x[i]) * t;
                oy[m] = y[i] + (y[j] - y[i]) * t;
                oz[m] = near;
                m++;
            }
        }
        if (m < 3) {
            return null;
        }
        double[][] out = new double[3][m];
        System.arraycopy(ox, 0, out[0], 0, m);
        System.arraycopy(oy, 0, out[1], 0, m);
        System.arraycopy(oz, 0, out[2], 0, m);
        return out;
    }

    /// Darkens the frame's edges a little, pulling the eye to the middle.
    private static void vignette(Graphics g, int w, int h) {
        int band = Math.max(4, h / 5);
        for (int i = 0; i < band; i++) {
            int a = (int) (70 * (1 - i / (double) band) * (1 - i / (double) band));
            g.setColor(0);
            g.setAlpha(a);
            g.drawLine(0, h - 1 - i, w, h - 1 - i);
            g.drawLine(0, i, w, i);
            g.drawLine(i, 0, i, h);
            g.drawLine(w - 1 - i, 0, w - 1 - i, h);
        }
        g.setAlpha(255);
    }

    /// Scales `rgb` by `k` and lifts its saturation, so plastic reads vivid.
    static int shade(int rgb, double k) {
        rgb = saturate(rgb, 1.22);
        return scale(rgb, k);
    }

    static int saturate(int rgb, double s) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        double l = 0.299 * r + 0.587 * g + 0.114 * b;
        return (clamp((int) Math.round(l + (r - l) * s)) << 16) | (clamp((int) Math.round(l + (g - l) * s)) << 8)
                | clamp((int) Math.round(l + (b - l) * s));
    }

    private static int scale(int rgb, double k) {
        int r = clamp((int) Math.round(((rgb >> 16) & 255) * k));
        int g = clamp((int) Math.round(((rgb >> 8) & 255) * k));
        int bl = clamp((int) Math.round((rgb & 255) * k));
        return (r << 16) | (g << 8) | bl;
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /// Sorts farthest first with a stable merge sort, so faces at the same
    /// depth keep the order their part listed them in.
    private static void sortByDepth(List<Face> faces) {
        Face[] a = faces.toArray(new Face[faces.size()]);
        Face[] tmp = new Face[a.length];
        mergeSort(a, tmp, 0, a.length);
        faces.clear();
        for (Face f : a) {
            faces.add(f);
        }
    }

    private static void mergeSort(Face[] a, Face[] tmp, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, lo, mid);
        mergeSort(a, tmp, mid, hi);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            tmp[k++] = a[i].depth >= a[j].depth ? a[i++] : a[j++];
        }
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        while (j < hi) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }
}
