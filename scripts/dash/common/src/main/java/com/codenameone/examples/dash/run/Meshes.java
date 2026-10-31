/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.run;

import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;

/// Smooth shapes built in code: capsules for limbs and beams, rounded boxes
/// for anything that would otherwise be a hard cube, a coin with a rim, a
/// ring, a curved band for a visor and a faceted gem.
///
/// Every shape carries its own smooth normals, so PHONG lighting slides
/// across it the way it does across a sphere. Most are surfaces of
/// revolution about Y, swept by [#lathe] from a profile -- a list of
/// (radius, height) points with their normals -- in the layout
/// `Primitives.sphere` uses. Triangles are wound counter-clockwise seen from
/// the side their normals face, which [#tri] checks rather than trusts.
///
/// Build a shape once and share it: the mesh is the expensive part, a model
/// using it is not.
final class Meshes {
    private float[] v = new float[512];
    private int vn;
    private int[] ix = new int[512];
    private int in;

    private Meshes() {
    }

    /// A capsule along Y: a cylinder `length` long with a hemisphere of
    /// `radius` on each end, so `length + 2 * radius` tall overall.
    static Mesh capsule(float radius, float length, int rings, int segs) {
        int n = (rings + 1) * 2;
        float[] r = new float[n];
        float[] y = new float[n];
        float[] nr = new float[n];
        float[] ny = new float[n];
        int o = 0;
        for (int half = 0; half < 2; half++) {
            float cy = half == 0 ? length / 2f : -length / 2f;
            for (int k = 0; k <= rings; k++) {
                double t = Math.PI / 2 * (half + k / (double) rings);
                float s = (float) Math.sin(t);
                float c = (float) Math.cos(t);
                r[o] = radius * s;
                y[o] = cy + radius * c;
                nr[o] = s;
                ny[o] = c;
                o++;
            }
        }
        Meshes m = new Meshes();
        m.lathe(r, y, nr, ny, n, segs, 0, Math.PI * 2);
        return m.build();
    }

    /// A box `w` by `h` by `d` whose edges and corners are rounded to
    /// `radius`, with flat faces between them. Texture coordinates run
    /// across the front and back faces, so a striped texture reads there.
    static Mesh roundedBox(float w, float h, float d, float radius, int segs) {
        float rad = Math.min(radius, Math.min(w, Math.min(h, d)) / 2f - 0.001f);
        rad = Math.max(rad, 0.001f);
        float a = w / 2f - rad;
        float b = h / 2f - rad;
        float c = d / 2f - rad;
        Meshes m = new Meshes();
        int rows = 2 * (segs + 1);
        // Four quadrants of columns, each with its own edge vertices so the
        // faces between them stay flat, and the first column repeated to
        // close the last face.
        int cols = 4 * (segs + 1) + 1;
        for (int row = 0; row < rows; row++) {
            int hy = row / (segs + 1);
            int k = row % (segs + 1);
            double th = Math.PI / 2 * (hy + k / (double) segs);
            float st = (float) Math.sin(th);
            float ct = (float) Math.cos(th);
            float sy = hy == 0 ? 1f : -1f;
            for (int col = 0; col < cols; col++) {
                int q = col == cols - 1 ? 0 : col / (segs + 1);
                int j = col == cols - 1 ? 0 : col % (segs + 1);
                double ph = Math.PI / 2 * (q + j / (double) segs);
                float sx = q < 2 ? 1f : -1f;
                float sz = q == 0 || q == 3 ? 1f : -1f;
                float nx = st * (float) Math.sin(ph);
                float nz = st * (float) Math.cos(ph);
                float px = sx * a + nx * rad;
                float py = sy * b + ct * rad;
                float pz = sz * c + nz * rad;
                m.vert(px, py, pz, nx, ct, nz, px / w + 0.5f, 0.5f - py / h);
            }
        }
        m.grid(0, rows, cols);
        for (int s = 0; s < 2; s++) {
            float sy = s == 0 ? 1f : -1f;
            float py = sy * (b + rad);
            int p0 = m.vert(-a, py, -c, 0, sy, 0, 0.5f - a / w, 0.5f - c / d);
            int p1 = m.vert(a, py, -c, 0, sy, 0, 0.5f + a / w, 0.5f - c / d);
            int p2 = m.vert(a, py, c, 0, sy, 0, 0.5f + a / w, 0.5f + c / d);
            int p3 = m.vert(-a, py, c, 0, sy, 0, 0.5f - a / w, 0.5f + c / d);
            m.tri(p0, p1, p2);
            m.tri(p0, p2, p3);
        }
        return m.build();
    }

    /// A coin with its faces on +-Y: a disc of `radius`, `thick` through,
    /// with a raised rounded rim around a recessed face.
    static Mesh coin(float radius, float thick, int segs) {
        float h = thick / 2f;
        float bev = Math.min(h * 0.9f, radius * 0.12f);
        // The top half, from the centre outwards and over the edge.
        float[] tr = {0f, radius * 0.66f, radius * 0.72f, radius * 0.76f, 0f, 0f, 0f, 0f, radius};
        float[] ty = {h * 0.45f, h * 0.45f, h, h, 0f, 0f, 0f, 0f, 0f};
        float[] tnr = {0f, 0f, -0.55f, 0f, 0f, 0f, 0f, 0f, 1f};
        float[] tny = {1f, 1f, 0.83f, 1f, 0f, 0f, 0f, 0f, 0f};
        for (int k = 0; k < 4; k++) {
            double t = Math.PI / 2 * k / 3.0;
            float s = (float) Math.sin(t);
            float c = (float) Math.cos(t);
            tr[4 + k] = radius - bev + bev * s;
            ty[4 + k] = h - bev + bev * c;
            tnr[4 + k] = s;
            tny[4 + k] = c;
        }
        int t = tr.length;
        float[] r = new float[t * 2];
        float[] y = new float[t * 2];
        float[] nr = new float[t * 2];
        float[] ny = new float[t * 2];
        for (int i = 0; i < t; i++) {
            r[i] = tr[i];
            y[i] = ty[i];
            nr[i] = tnr[i];
            ny[i] = tny[i];
            int j = t * 2 - 1 - i;
            r[j] = tr[i];
            y[j] = -ty[i];
            nr[j] = tnr[i];
            ny[j] = -tny[i];
        }
        Meshes m = new Meshes();
        m.lathe(r, y, nr, ny, t * 2, segs, 0, Math.PI * 2);
        return m.build();
    }

    /// A ring round Y: a tube of `tube` radius bent into a circle of `radius`.
    static Mesh torus(float radius, float tube, int rings, int segs) {
        int n = rings + 1;
        float[] r = new float[n];
        float[] y = new float[n];
        float[] nr = new float[n];
        float[] ny = new float[n];
        for (int k = 0; k < n; k++) {
            double a = Math.PI * 2 * k / rings;
            float c = (float) Math.cos(a);
            float s = (float) Math.sin(a);
            r[k] = radius + tube * c;
            y[k] = tube * s;
            nr[k] = c;
            ny[k] = s;
        }
        Meshes m = new Meshes();
        m.lathe(r, y, nr, ny, n, segs, 0, Math.PI * 2);
        return m.build();
    }

    /// A curved strip `height` tall at `radius` round Y, `arc` degrees wide
    /// and centred on -z: the wrap-around visor of a helmet.
    static Mesh band(float radius, float height, float arc, int segs) {
        float h = height / 2f;
        float[] r = {radius - height * 0.18f, radius, radius - height * 0.18f};
        float[] y = {h, 0f, -h};
        float[] nr = {0.7f, 1f, 0.7f};
        float[] ny = {0.7f, 0f, -0.7f};
        double half = Math.toRadians(arc) / 2;
        Meshes m = new Meshes();
        m.lathe(r, y, nr, ny, 3, segs, Math.PI - half, Math.PI + half);
        return m.build();
    }

    /// An octahedron with one flat normal per face, so each facet catches
    /// the light on its own: a cut gem.
    static Mesh gem(float radius, float height) {
        Meshes m = new Meshes();
        float[][] ring = {{radius, 0f}, {0f, radius}, {-radius, 0f}, {0f, -radius}};
        for (int half = 0; half < 2; half++) {
            float tip = half == 0 ? height / 2f : -height / 2f;
            for (int i = 0; i < 4; i++) {
                float[] p = ring[i];
                float[] q = ring[(i + 1) % 4];
                float ux = p[0];
                float uy = -tip;
                float uz = p[1];
                float wx = q[0];
                float wy = -tip;
                float wz = q[1];
                float nx = uy * wz - uz * wy;
                float ny = uz * wx - ux * wz;
                float nz = ux * wy - uy * wx;
                // Point the face normal away from the centre.
                if (nx * (p[0] + q[0]) + ny * tip + nz * (p[1] + q[1]) < 0) {
                    nx = -nx;
                    ny = -ny;
                    nz = -nz;
                }
                int t0 = m.vert(0f, tip, 0f, nx, ny, nz, 0.5f, half);
                int t1 = m.vert(p[0], 0f, p[1], nx, ny, nz, 0f, 0.5f);
                int t2 = m.vert(q[0], 0f, q[1], nx, ny, nz, 1f, 0.5f);
                m.tri(t0, t1, t2);
            }
        }
        return m.build();
    }

    /// Sweeps a profile of `n` points round Y from `phi0` to `phi1`; angle 0
    /// is +z, as in `Primitives.sphere`.
    private void lathe(float[] r, float[] y, float[] nr, float[] ny, int n, int segs, double phi0, double phi1) {
        int base = vn / 8;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= segs; j++) {
                double ph = phi0 + (phi1 - phi0) * j / segs;
                float s = (float) Math.sin(ph);
                float c = (float) Math.cos(ph);
                vert(r[i] * s, y[i], r[i] * c, nr[i] * s, ny[i], nr[i] * c, j / (float) segs,
                        i / (float) (n - 1));
            }
        }
        grid(base, n, segs + 1);
    }

    private void grid(int base, int rows, int cols) {
        for (int rr = 0; rr < rows - 1; rr++) {
            for (int cc = 0; cc < cols - 1; cc++) {
                int a = base + rr * cols + cc;
                int b = a + cols;
                tri(a, b, a + 1);
                tri(b, b + 1, a + 1);
            }
        }
    }

    private int vert(float x, float y, float z, float nx, float ny, float nz, float u, float t) {
        if (vn + 8 > v.length) {
            float[] g = new float[v.length * 2];
            System.arraycopy(v, 0, g, 0, vn);
            v = g;
        }
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        v[vn] = x;
        v[vn + 1] = y;
        v[vn + 2] = z;
        v[vn + 3] = nx;
        v[vn + 4] = ny;
        v[vn + 5] = nz;
        v[vn + 6] = u;
        v[vn + 7] = t;
        vn += 8;
        return vn / 8 - 1;
    }

    /// Adds a triangle, flipped if need be so it winds counter-clockwise
    /// seen from the side its vertex normals face. A degenerate one (at a
    /// pole) is skipped.
    private void tri(int a, int b, int c) {
        int pa = a * 8;
        int pb = b * 8;
        int pc = c * 8;
        float ux = v[pb] - v[pa];
        float uy = v[pb + 1] - v[pa + 1];
        float uz = v[pb + 2] - v[pa + 2];
        float wx = v[pc] - v[pa];
        float wy = v[pc + 1] - v[pa + 1];
        float wz = v[pc + 2] - v[pa + 2];
        float fx = uy * wz - uz * wy;
        float fy = uz * wx - ux * wz;
        float fz = ux * wy - uy * wx;
        if (fx * fx + fy * fy + fz * fz < 1e-14f) {
            return;
        }
        float nx = v[pa + 3] + v[pb + 3] + v[pc + 3];
        float ny = v[pa + 4] + v[pb + 4] + v[pc + 4];
        float nz = v[pa + 5] + v[pb + 5] + v[pc + 5];
        if (in + 3 > ix.length) {
            int[] g = new int[ix.length * 2];
            System.arraycopy(ix, 0, g, 0, in);
            ix = g;
        }
        ix[in] = a;
        if (fx * nx + fy * ny + fz * nz >= 0) {
            ix[in + 1] = b;
            ix[in + 2] = c;
        } else {
            ix[in + 1] = c;
            ix[in + 2] = b;
        }
        in += 3;
    }

    private Mesh build() {
        int count = vn / 8;
        if (count > 65536) {
            throw new IllegalStateException("mesh exceeds the 16 bit index range");
        }
        float[] data = new float[vn];
        System.arraycopy(v, 0, data, 0, vn);
        int[] idx = new int[in];
        System.arraycopy(ix, 0, idx, 0, in);
        VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, count);
        vb.setData(data);
        IndexBuffer ib = new IndexBuffer(idx.length);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }
}
