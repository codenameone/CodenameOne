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

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;

/// Unit meshes for the part shapes the GPU package has no primitive for. Each
/// spans -0.5..0.5 on every axis, so a part's size is its scale. Faces are
/// flat shaded: every triangle has its own three vertices.
final class PartMeshes {
    private PartMeshes() {
    }

    /// Interleaved px,py,pz of each triangle's corners, counter-clockwise from outside.
    static float[] cylinderTriangles(int sides) {
        float[] out = new float[sides * 4 * 9];
        int o = 0;
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides;
            double a1 = 2 * Math.PI * (i + 1) / sides;
            float y0 = (float) (Math.cos(a0) * 0.5);
            float z0 = (float) (Math.sin(a0) * 0.5);
            float y1 = (float) (Math.cos(a1) * 0.5);
            float z1 = (float) (Math.sin(a1) * 0.5);
            // A cylinder's axis is X, as a Part with Shape Cylinder.
            o = tri(out, o, 0.5f, 0, 0, 0.5f, y0, z0, 0.5f, y1, z1);
            o = tri(out, o, -0.5f, 0, 0, -0.5f, y1, z1, -0.5f, y0, z0);
            o = tri(out, o, -0.5f, y0, z0, -0.5f, y1, z1, 0.5f, y1, z1);
            o = tri(out, o, -0.5f, y0, z0, 0.5f, y1, z1, 0.5f, y0, z0);
        }
        return out;
    }

    /// A wedge: full bottom and back (+Z) faces, the slope rising from the
    /// front bottom edge to the back top edge.
    static float[] wedgeTriangles() {
        float h = 0.5f;
        return new float[] {
            // bottom
            -h, -h, -h, h, -h, -h, h, -h, h,
            -h, -h, -h, h, -h, h, -h, -h, h,
            // back
            -h, -h, h, h, -h, h, h, h, h,
            -h, -h, h, h, h, h, -h, h, h,
            // slope
            -h, -h, -h, -h, h, h, h, h, h,
            -h, -h, -h, h, h, h, h, -h, -h,
            // sides
            h, -h, -h, h, h, h, h, -h, h,
            -h, -h, -h, -h, -h, h, -h, h, h,
        };
    }

    /// A corner wedge: a square bottom and a peak over the (+X, +Z) corner.
    static float[] cornerWedgeTriangles() {
        float h = 0.5f;
        return new float[] {
            -h, -h, -h, h, -h, -h, h, -h, h,
            -h, -h, -h, h, -h, h, -h, -h, h,
            h, -h, -h, h, h, h, h, -h, h,
            -h, -h, h, h, -h, h, h, h, h,
            -h, -h, -h, h, h, h, h, -h, -h,
            -h, -h, -h, -h, -h, h, h, h, h,
        };
    }

    private static int tri(float[] out, int o, float... p) {
        System.arraycopy(p, 0, out, o, 9);
        return o + 9;
    }

    /// Flat-shaded mesh of a triangle list: computes each face normal.
    static Mesh build(GraphicsDevice device, float[] tris) {
        int verts = tris.length / 3;
        float[] v = new float[verts * 8];
        int[] idx = new int[verts];
        for (int t = 0; t < verts / 3; t++) {
            int b = t * 9;
            float ux = tris[b + 3] - tris[b];
            float uy = tris[b + 4] - tris[b + 1];
            float uz = tris[b + 5] - tris[b + 2];
            float wx = tris[b + 6] - tris[b];
            float wy = tris[b + 7] - tris[b + 1];
            float wz = tris[b + 8] - tris[b + 2];
            float nx = uy * wz - uz * wy;
            float ny = uz * wx - ux * wz;
            float nz = ux * wy - uy * wx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            for (int k = 0; k < 3; k++) {
                int vi = t * 3 + k;
                int d = vi * 8;
                v[d] = tris[b + k * 3];
                v[d + 1] = tris[b + k * 3 + 1];
                v[d + 2] = tris[b + k * 3 + 2];
                v[d + 3] = nx;
                v[d + 4] = ny;
                v[d + 5] = nz;
                v[d + 6] = v[d] + 0.5f;
                v[d + 7] = v[d + 1] + 0.5f;
                idx[vi] = vi;
            }
        }
        VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, verts);
        vb.setData(v);
        IndexBuffer ib = device.createIndexBuffer(verts);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }
}
