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
package com.codename1.gpu;

import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link Primitives} mesh factory. A hand-written
 * {@link GraphicsDevice} (its buffer-allocation methods are concrete on the
 * base class) lets the test build the cube and quad meshes and assert their
 * vertex/index layout without any GPU backend.
 */
class PrimitivesTest {

    /** Minimal device: only the abstract members are stubbed; the concrete
     * {@code createVertexBuffer}/{@code createIndexBuffer} are inherited.
     * Package-visible so sibling primitive tests can reuse it. */
    static final class HeadlessDevice extends GraphicsDevice {
        public GpuCapabilities getCapabilities() {
            return null;
        }

        public Texture createTexture(Image image) {
            return null;
        }

        public Texture createTexture(int width, int height, int[] argb) {
            return null;
        }

        public void clear(int argbColor, boolean color, boolean depth) {
        }

        public void setViewport(int x, int y, int width, int height) {
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        }

        public void dispose(VertexBuffer buffer) {
        }

        public void dispose(IndexBuffer buffer) {
        }

        public void dispose(Texture texture) {
        }
    }

    @Test
    void cubeHasSixFacesWorthOfVerticesAndIndices() {
        Mesh cube = Primitives.cube(new HeadlessDevice(), 2.0f);
        assertEquals(PrimitiveType.TRIANGLES, cube.getPrimitiveType());
        assertTrue(cube.isIndexed());
        // 6 faces * 4 verts = 24 vertices, 6 faces * 6 indices = 36 indices.
        assertEquals(24, cube.getVertices().getVertexCount());
        assertEquals(36, cube.getIndices().getIndexCount());
        assertSame(VertexFormat.POSITION_NORMAL_TEXCOORD, cube.getVertices().getFormat());
        // 24 vertices * 8 floats (pos3 + normal3 + uv2).
        assertEquals(24 * 8, cube.getVertices().getData().length);
    }

    @Test
    void cubeIsCenteredOnTheOriginWithHalfEdgeExtent() {
        // size 2 -> half-edge 1.0; first vertex is the front-bottom-left corner.
        float[] v = Primitives.cube(new HeadlessDevice(), 2.0f).getVertices().getData();
        assertEquals(-1.0f, v[0], 1e-6f);
        assertEquals(-1.0f, v[1], 1e-6f);
        assertEquals(1.0f, v[2], 1e-6f);
        // ...and its normal points along +Z (the front face).
        assertEquals(0.0f, v[3], 1e-6f);
        assertEquals(0.0f, v[4], 1e-6f);
        assertEquals(1.0f, v[5], 1e-6f);
    }

    @Test
    void quadHasFourVerticesAndTwoTriangles() {
        Mesh quad = Primitives.quad(new HeadlessDevice(), 4.0f);
        assertEquals(PrimitiveType.TRIANGLES, quad.getPrimitiveType());
        assertTrue(quad.isIndexed());
        assertEquals(4, quad.getVertices().getVertexCount());
        assertEquals(6, quad.getIndices().getIndexCount());
        assertEquals(4 * 8, quad.getVertices().getData().length);
    }
}
