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

import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// The bundled glTF models the renderer draws in place of primitives: the
/// limbs of a blocky character and a few trees and rocks, all CC0 from
/// Kenney (see `NOTICE` and the licence files beside the models).
///
/// Every model is fitted to the unit cube centred on the origin, the space
/// the primitive meshes live in, so a part's model matrix stretches it to
/// the part's size exactly as it stretches a cube.
final class ModelMeshes {
    static final String DIR = "/com/codename1/rbx/models/";
    static final String CHARACTER = "character.glb";
    static final String[] TREES = {"tree_default.glb", "tree_oak.glb"};
    static final String[] ROCKS = {"stone_largeA.glb", "stone_largeC.glb"};

    private ModelMeshes() {
    }

    /// One primitive of a model, fitted: x,y,z, nx,ny,nz, u,v per vertex.
    static final class Piece {
        final float[] vertices;
        final int[] indices;
        /// The glTF base colour, RGB.
        final int rgb;

        Piece(float[] vertices, int[] indices, int rgb) {
            this.vertices = vertices;
            this.indices = indices;
            this.rgb = rgb;
        }
    }

    /// The character node a limb kind is drawn with, or null for none. The
    /// head is not mapped: the renderer keeps its own head and face decal.
    static String characterNode(int kind) {
        switch (kind) {
            case SceneSnapshot.TORSO:
                return "torso";
            case SceneSnapshot.LEFT_ARM:
                return "arm-left";
            case SceneSnapshot.RIGHT_ARM:
                return "arm-right";
            case SceneSnapshot.LEFT_LEG:
                return "leg-left";
            case SceneSnapshot.RIGHT_LEG:
                return "leg-right";
            default:
                return null;
        }
    }

    /// Reads a bundled model; null when it is missing or does not parse.
    static GltfScene scene(String name) {
        InputStream in = ModelMeshes.class.getResourceAsStream(DIR + name); // NOPMD CloseResource - closed in the finally below
        if (in == null) {
            return null;
        }
        try {
            return GltfLoader.loadScene(in);
        } catch (IOException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Read fully or failed either way; nothing to recover.
            }
        }
    }

    /// The primitives of `node` (every primitive when null), fitted together
    /// to the unit cube. `turn` rotates half a turn about Y: the character
    /// faces +Z and a Roblox character faces -Z. Null when nothing matches.
    static Piece[] fitted(GltfScene s, String node, boolean turn) {
        int want = node == null ? -1 : s.findNode(node);
        if (node != null && want < 0) {
            return null;
        }
        List<GltfScene.Primitive> prims = new ArrayList<GltfScene.Primitive>();
        float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < s.getPrimitiveCount(); i++) {
            GltfScene.Primitive p = s.getPrimitive(i);
            if (want >= 0 && p.getNode() != want || p.getVertexCount() == 0) {
                continue;
            }
            prims.add(p);
            float[] pos = p.getPositions();
            for (int v = 0; v < pos.length; v++) {
                lo[v % 3] = Math.min(lo[v % 3], pos[v]);
                hi[v % 3] = Math.max(hi[v % 3], pos[v]);
            }
        }
        if (prims.isEmpty()) {
            return null;
        }
        float[] size = new float[3];
        float[] mid = new float[3];
        for (int a = 0; a < 3; a++) {
            size[a] = Math.max(1e-4f, hi[a] - lo[a]);
            mid[a] = (lo[a] + hi[a]) / 2;
        }
        float flip = turn ? -1 : 1;
        Piece[] out = new Piece[prims.size()];
        for (int i = 0; i < out.length; i++) {
            GltfScene.Primitive p = prims.get(i);
            float[] pos = p.getPositions();
            float[] nor = p.getNormals();
            float[] uv = p.getTexcoords();
            int n = p.getVertexCount();
            float[] v = new float[n * 8];
            for (int k = 0; k < n; k++) {
                int d = k * 8;
                float x = (pos[k * 3] - mid[0]) / size[0];
                float y = (pos[k * 3 + 1] - mid[1]) / size[1];
                float z = (pos[k * 3 + 2] - mid[2]) / size[2];
                // A stretched normal scales by the inverse of the stretch.
                float nx = nor[k * 3] * size[0];
                float ny = nor[k * 3 + 1] * size[1];
                float nz = nor[k * 3 + 2] * size[2];
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                v[d] = x * flip;
                v[d + 1] = y;
                v[d + 2] = z * flip;
                v[d + 3] = nx * flip;
                v[d + 4] = ny;
                v[d + 5] = nz * flip;
                v[d + 6] = uv == null ? x + 0.5f : uv[k * 2];
                v[d + 7] = uv == null ? y + 0.5f : uv[k * 2 + 1];
            }
            int m = p.getMaterial();
            int rgb = m < 0 ? 0xffffff : s.getMaterial(m).getBaseColor() & 0xffffff;
            out[i] = new Piece(v, p.getIndices(), rgb);
        }
        return out;
    }

    /// Uploads a fitted piece.
    static Mesh mesh(GraphicsDevice device, Piece p) {
        VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, p.vertices.length / 8);
        vb.setData(p.vertices);
        IndexBuffer ib = device.createIndexBuffer(p.indices.length);
        ib.setData(p.indices);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }

    /// `p` with every vertex's normal averaged over all vertices at the same
    /// position: a low-poly crown then shades as a rounded mass of leaves
    /// instead of a cut gem.
    static Piece smoothed(Piece p) {
        float[] v = new float[p.vertices.length];
        System.arraycopy(p.vertices, 0, v, 0, v.length);
        java.util.Map<String, float[]> sums = new java.util.HashMap<String, float[]>();
        String[] keys = new String[v.length / 8];
        for (int i = 0; i < keys.length; i++) {
            int o = i * 8;
            keys[i] = Math.round(v[o] * 1000) + "," + Math.round(v[o + 1] * 1000) + "," + Math.round(v[o + 2] * 1000);
            float[] n = sums.get(keys[i]);
            if (n == null) {
                n = new float[3];
                sums.put(keys[i], n);
            }
            n[0] += v[o + 3];
            n[1] += v[o + 4];
            n[2] += v[o + 5];
        }
        for (int i = 0; i < keys.length; i++) {
            float[] n = sums.get(keys[i]);
            float l = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (l > 1e-6f) {
                int o = i * 8;
                v[o + 3] = n[0] / l;
                v[o + 4] = n[1] / l;
                v[o + 5] = n[2] / l;
            }
        }
        return new Piece(v, p.indices, p.rgb);
    }
}
