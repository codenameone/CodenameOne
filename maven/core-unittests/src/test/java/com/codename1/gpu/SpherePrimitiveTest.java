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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link Primitives#sphere(float, int, int, boolean)}: vertex and
 * index counts, radius, equirectangular UV mapping with seam duplication and
 * the inside-out normal/winding flip used by the 360 media viewer.
 */
class SpherePrimitiveTest {

    private static final float EPS = 1e-5f;

    @Test
    void vertexAndIndexCountsMatchTessellation() {
        int lat = 6;
        int lon = 8;
        Mesh sphere = Primitives.sphere(2.0f, lat, lon, false);
        assertEquals(PrimitiveType.TRIANGLES, sphere.getPrimitiveType());
        assertSame(VertexFormat.POSITION_NORMAL_TEXCOORD, sphere.getVertices().getFormat());
        assertEquals((lat + 1) * (lon + 1), sphere.getVertices().getVertexCount());
        assertEquals(lat * lon * 6, sphere.getIndices().getIndexCount());
    }

    @Test
    void allPositionsLieOnTheRadius() {
        float radius = 3.5f;
        Mesh sphere = Primitives.sphere(radius, 5, 7, false);
        float[] v = sphere.getVertices().getData();
        int count = sphere.getVertices().getVertexCount();
        for (int i = 0; i < count; i++) {
            int o = i * 8;
            float len = (float) Math.sqrt(v[o] * v[o] + v[o + 1] * v[o + 1] + v[o + 2] * v[o + 2]);
            assertEquals(radius, len, 1e-4f, "vertex " + i);
        }
    }

    @Test
    void uvsAreEquirectangularWithSeamDuplication() {
        int lat = 4;
        int lon = 6;
        Mesh sphere = Primitives.sphere(1.0f, lat, lon, false);
        float[] v = sphere.getVertices().getData();
        int count = sphere.getVertices().getVertexCount();
        for (int i = 0; i < count; i++) {
            int o = i * 8;
            assertTrue(v[o + 6] >= 0f && v[o + 6] <= 1f, "u out of range at vertex " + i);
            assertTrue(v[o + 7] >= 0f && v[o + 7] <= 1f, "v out of range at vertex " + i);
        }
        // North pole row has v=0, south pole row v=1.
        assertEquals(0f, v[7], EPS);
        int lastRow = lat * (lon + 1) * 8;
        assertEquals(1f, v[lastRow + 7], EPS);

        // The seam duplicates positions: first and last vertex of a row share a
        // position but carry u=0 and u=1 respectively.
        int row = 2 * (lon + 1) * 8; // a mid-latitude row
        int rowEnd = row + lon * 8;
        assertEquals(v[row], v[rowEnd], 1e-4f);
        assertEquals(v[row + 1], v[rowEnd + 1], 1e-4f);
        assertEquals(v[row + 2], v[rowEnd + 2], 1e-4f);
        assertEquals(0f, v[row + 6], EPS);
        assertEquals(1f, v[rowEnd + 6], EPS);
    }

    @Test
    void outwardNormalsPointAwayFromCenter() {
        Mesh sphere = Primitives.sphere(2.0f, 5, 7, false);
        assertNormalOrientation(sphere, true);
    }

    @Test
    void insideOutNormalsPointTowardCenter() {
        Mesh sphere = Primitives.sphere(2.0f, 5, 7, true);
        assertNormalOrientation(sphere, false);
    }

    private static void assertNormalOrientation(Mesh sphere, boolean outward) {
        float[] v = sphere.getVertices().getData();
        int count = sphere.getVertices().getVertexCount();
        for (int i = 0; i < count; i++) {
            int o = i * 8;
            float dot = v[o] * v[o + 3] + v[o + 1] * v[o + 4] + v[o + 2] * v[o + 5];
            // Skip degenerate dot at poles is impossible: position length equals
            // the radius everywhere, so |dot| is always the radius.
            if (outward) {
                assertTrue(dot > 0f, "normal should point outward at vertex " + i);
            } else {
                assertTrue(dot < 0f, "normal should point inward at vertex " + i);
            }
            float nlen = (float) Math.sqrt(v[o + 3] * v[o + 3] + v[o + 4] * v[o + 4] + v[o + 5] * v[o + 5]);
            assertEquals(1f, nlen, 1e-4f, "normal not unit length at vertex " + i);
        }
    }

    @Test
    void windingFlipsWhenInsideOut() {
        // The geometric normal of a mid-latitude triangle must face the same way
        // as the vertex normals: away from the center outward, toward it inside
        // out.
        assertEquals(1, windingSign(Primitives.sphere(1.0f, 6, 8, false)));
        assertEquals(-1, windingSign(Primitives.sphere(1.0f, 6, 8, true)));
    }

    private static int windingSign(Mesh sphere) {
        float[] v = sphere.getVertices().getData();
        short[] idx = sphere.getIndices().getData();
        // First triangle of the second latitude band (away from the degenerate
        // pole row). 8 longitude bands -> band size is 8 * 6 indices.
        int t = 8 * 6;
        int a = (idx[t] & 0xffff) * 8;
        int b = (idx[t + 1] & 0xffff) * 8;
        int c = (idx[t + 2] & 0xffff) * 8;
        float ux = v[b] - v[a];
        float uy = v[b + 1] - v[a + 1];
        float uz = v[b + 2] - v[a + 2];
        float wx = v[c] - v[a];
        float wy = v[c + 1] - v[a + 1];
        float wz = v[c + 2] - v[a + 2];
        float nx = uy * wz - uz * wy;
        float ny = uz * wx - ux * wz;
        float nz = ux * wy - uy * wx;
        // Compare against the direction from the center to the triangle.
        float dot = nx * v[a] + ny * v[a + 1] + nz * v[a + 2];
        return dot > 0f ? 1 : -1;
    }

    @Test
    void deviceOverloadProducesTheSameMesh() {
        Mesh direct = Primitives.sphere(1.5f, 4, 6, true);
        Mesh viaDevice = Primitives.sphere(new PrimitivesTest.HeadlessDevice(), 1.5f, 4, 6, true);
        assertEquals(direct.getVertices().getVertexCount(), viaDevice.getVertices().getVertexCount());
        assertEquals(direct.getIndices().getIndexCount(), viaDevice.getIndices().getIndexCount());
        assertArrayEquals(direct.getVertices().getData(), viaDevice.getVertices().getData());
        assertArrayEquals(direct.getIndices().getData(), viaDevice.getIndices().getData());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> Primitives.sphere(0f, 4, 6, false));
        assertThrows(IllegalArgumentException.class, () -> Primitives.sphere(-1f, 4, 6, false));
        assertThrows(IllegalArgumentException.class, () -> Primitives.sphere(1f, 1, 6, false));
        assertThrows(IllegalArgumentException.class, () -> Primitives.sphere(1f, 4, 2, false));
        // (lat+1)*(lon+1) must stay within the unsigned short index range.
        assertThrows(IllegalArgumentException.class, () -> Primitives.sphere(1f, 300, 300, false));
    }
}
