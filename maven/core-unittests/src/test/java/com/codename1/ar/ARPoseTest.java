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
package com.codename1.ar;

import com.codename1.gpu.Matrix4;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link ARPose} rigid-transform math: matrix conversion in
 * both directions, composition and point transformation. Pure math, no
 * Display required. Results are cross-checked against {@link Matrix4}.
 */
class ARPoseTest {

    private static final float EPS = 1e-5f;

    @Test
    void identityMapsToIdentityMatrix() {
        assertArrayEquals(Matrix4.identity(), ARPose.IDENTITY.toMatrix());
    }

    @Test
    void pureTranslationMatrix() {
        ARPose p = new ARPose(1, 2, 3, 0, 0, 0, 1);
        float[] m = p.toMatrix();
        assertArrayEquals(Matrix4.translation(1, 2, 3), m);
    }

    @Test
    void rotationMatchesMatrix4Rotation() {
        // 90 degrees around Y: quaternion (0, sin45, 0, cos45).
        float s = (float) Math.sin(Math.PI / 4);
        float c = (float) Math.cos(Math.PI / 4);
        ARPose p = new ARPose(0, 0, 0, 0, s, 0, c);
        float[] expected = Matrix4.rotation((float) Math.PI / 2, 0, 1, 0);
        float[] actual = p.toMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(expected[i], actual[i], EPS, "element " + i);
        }
    }

    @Test
    void toMatrixAndFromMatrixRoundTrip() {
        float s = (float) Math.sin(0.6);
        float c = (float) Math.cos(0.6);
        ARPose original = new ARPose(0.5f, -1.25f, 2f, s * 0.267f, s * 0.535f, s * 0.802f, c);
        ARPose restored = ARPose.fromMatrix(original.toMatrix());
        assertEquals(original.getTx(), restored.getTx(), EPS);
        assertEquals(original.getTy(), restored.getTy(), EPS);
        assertEquals(original.getTz(), restored.getTz(), EPS);
        // q and -q are the same rotation; compare via the matrices.
        float[] m1 = original.toMatrix();
        float[] m2 = restored.toMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(m1[i], m2[i], 1e-4f, "element " + i);
        }
    }

    @Test
    void transformComposesLikeMatrixMultiplication() {
        float s = (float) Math.sin(Math.PI / 4);
        float c = (float) Math.cos(Math.PI / 4);
        ARPose a = new ARPose(1, 0, 0, 0, s, 0, c);
        ARPose b = new ARPose(0, 2, 0, s, 0, 0, c);
        ARPose ab = a.transform(b);

        float[] expected = new float[16];
        Matrix4.multiply(a.toMatrix(), b.toMatrix(), expected);
        float[] actual = ab.toMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(expected[i], actual[i], EPS, "element " + i);
        }
    }

    @Test
    void transformPointRotatesThenTranslates() {
        // 90 degrees around Z maps +X to +Y, then translate by (10, 0, 0).
        float s = (float) Math.sin(Math.PI / 4);
        float c = (float) Math.cos(Math.PI / 4);
        ARPose p = new ARPose(10, 0, 0, 0, 0, s, c);
        float[] pt = {1, 0, 0};
        p.transformPoint(pt);
        assertEquals(10f, pt[0], EPS);
        assertEquals(1f, pt[1], EPS);
        assertEquals(0f, pt[2], EPS);
    }

    @Test
    void constructorNormalizesTheQuaternion() {
        ARPose p = new ARPose(0, 0, 0, 0, 0, 0, 2);
        assertEquals(1f, p.getQw(), EPS);
        // Zero quaternion falls back to identity.
        ARPose z = new ARPose(0, 0, 0, 0, 0, 0, 0);
        assertEquals(1f, z.getQw(), EPS);
    }
}
