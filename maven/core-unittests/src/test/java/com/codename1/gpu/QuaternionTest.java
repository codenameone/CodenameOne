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
 * Coverage for the {@link Quaternion} float[4] utilities. Pure math, so no
 * Display/platform setup is required. Results are cross-checked against
 * {@link Matrix4} where both APIs express the same rotation.
 */
class QuaternionTest {

    private static final float EPS = 1e-5f;

    private static void assertQuatEquals(float[] expected, float[] actual, float eps) {
        // q and -q encode the same rotation; normalize the sign before comparing.
        float sign = (expected[0] * actual[0] + expected[1] * actual[1]
                + expected[2] * actual[2] + expected[3] * actual[3]) < 0 ? -1f : 1f;
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i], sign * actual[i], eps, "component " + i);
        }
    }

    @Test
    void identityIsNoRotation() {
        float[] q = Quaternion.identity();
        assertArrayEquals(new float[]{0, 0, 0, 1}, q);

        float[] v = {1.5f, -2.5f, 3.5f};
        Quaternion.rotateVector(q, v);
        assertArrayEquals(new float[]{1.5f, -2.5f, 3.5f}, v);

        float[] m = new float[16];
        Quaternion.toMatrix(q, m);
        assertArrayEquals(Matrix4.identity(), m);
    }

    @Test
    void toMatrixMatchesMatrix4RotationForArbitraryAxes() {
        float[][] axes = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 2, 3}, {-1, 0.5f, 2}};
        float[] angles = {0.3f, 1.1f, (float) Math.PI / 2, 2.7f, -0.9f};
        float[] qm = new float[16];
        for (int i = 0; i < axes.length; i++) {
            float[] q = Quaternion.fromAxisAngle(angles[i], axes[i][0], axes[i][1], axes[i][2]);
            Quaternion.toMatrix(q, qm);
            float[] mm = Matrix4.rotation(angles[i], axes[i][0], axes[i][1], axes[i][2]);
            for (int c = 0; c < 16; c++) {
                assertEquals(mm[c], qm[c], EPS, "axis " + i + " element " + c);
            }
        }
    }

    @Test
    void zeroAxisProducesIdentity() {
        float[] q = Quaternion.fromAxisAngle(1.0f, 0, 0, 0);
        assertArrayEquals(new float[]{0, 0, 0, 1}, q);
    }

    @Test
    void multiplyComposesLikeMatrixMultiplication() {
        float[] a = Quaternion.fromAxisAngle((float) Math.PI / 2, 0, 0, 1);
        float[] b = Quaternion.fromAxisAngle((float) Math.PI / 3, 1, 0, 0);
        float[] ab = new float[4];
        Quaternion.multiply(a, b, ab);

        float[] ma = Matrix4.rotation((float) Math.PI / 2, 0, 0, 1);
        float[] mb = Matrix4.rotation((float) Math.PI / 3, 1, 0, 0);
        float[] mab = new float[16];
        Matrix4.multiply(ma, mb, mab);

        float[] qm = new float[16];
        Quaternion.toMatrix(ab, qm);
        for (int c = 0; c < 16; c++) {
            assertEquals(mab[c], qm[c], EPS, "element " + c);
        }
    }

    @Test
    void multiplyToleratesAliasedDestination() {
        float[] a = Quaternion.fromAxisAngle(0.7f, 0, 1, 0);
        float[] b = Quaternion.fromAxisAngle(0.4f, 1, 0, 0);
        float[] expected = new float[4];
        Quaternion.multiply(a, b, expected);
        Quaternion.multiply(a, b, a);
        assertArrayEquals(expected, a);
    }

    @Test
    void rotateVectorKnownCase() {
        // 90 degrees around +Z maps +X to +Y.
        float[] q = Quaternion.fromAxisAngle((float) Math.PI / 2, 0, 0, 1);
        float[] v = {1, 0, 0};
        Quaternion.rotateVector(q, v);
        assertEquals(0f, v[0], EPS);
        assertEquals(1f, v[1], EPS);
        assertEquals(0f, v[2], EPS);
    }

    @Test
    void conjugateUndoesRotation() {
        float[] q = Quaternion.fromAxisAngle(1.3f, 2, -1, 0.5f);
        float[] inv = new float[4];
        Quaternion.conjugate(q, inv);
        float[] v = {0.4f, -1.2f, 2.2f};
        Quaternion.rotateVector(q, v);
        Quaternion.rotateVector(inv, v);
        assertEquals(0.4f, v[0], EPS);
        assertEquals(-1.2f, v[1], EPS);
        assertEquals(2.2f, v[2], EPS);
    }

    @Test
    void normalizeRestoresUnitLengthAndResetsZero() {
        float[] q = {2, 0, 0, 2};
        Quaternion.normalize(q);
        float len = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        assertEquals(1f, len, EPS);

        float[] zero = {0, 0, 0, 0};
        Quaternion.normalize(zero);
        assertArrayEquals(new float[]{0, 0, 0, 1}, zero);
    }

    @Test
    void slerpEndpointsAndMidpoint() {
        float[] a = Quaternion.identity();
        float[] b = Quaternion.fromAxisAngle((float) Math.PI / 2, 0, 1, 0);
        float[] out = new float[4];

        Quaternion.slerp(a, b, 0f, out);
        assertQuatEquals(a, out, EPS);
        Quaternion.slerp(a, b, 1f, out);
        assertQuatEquals(b, out, EPS);

        // Halfway between identity and a 90 degree yaw is a 45 degree yaw.
        Quaternion.slerp(a, b, 0.5f, out);
        float[] mid = Quaternion.fromAxisAngle((float) Math.PI / 4, 0, 1, 0);
        assertQuatEquals(mid, out, EPS);
    }

    @Test
    void slerpTakesShortestArc() {
        float[] a = Quaternion.fromAxisAngle(0.1f, 0, 0, 1);
        // Same rotation as some b but with all components negated; slerp must
        // not swing the long way around.
        float[] b = Quaternion.fromAxisAngle(0.3f, 0, 0, 1);
        float[] negB = {-b[0], -b[1], -b[2], -b[3]};
        float[] out = new float[4];
        Quaternion.slerp(a, negB, 0.5f, out);
        float[] mid = Quaternion.fromAxisAngle(0.2f, 0, 0, 1);
        assertQuatEquals(mid, out, EPS);
    }

    @Test
    void integrateGyroConstantRateMatchesAxisAngle() {
        // 0.5 rad/s around body X for 100 steps of 10ms = 0.5 radians total.
        float[] q = Quaternion.identity();
        for (int i = 0; i < 100; i++) {
            Quaternion.integrateGyro(q, 0.5f, 0, 0, 0.01f, q);
        }
        float[] expected = Quaternion.fromAxisAngle(0.5f, 1, 0, 0);
        assertQuatEquals(expected, q, 1e-4f);
    }

    @Test
    void integrateGyroZeroRateKeepsOrientation() {
        float[] q = Quaternion.fromAxisAngle(0.8f, 0, 1, 0);
        float[] out = new float[4];
        Quaternion.integrateGyro(q, 0, 0, 0, 0.02f, out);
        assertArrayEquals(q, out);
    }

    @Test
    void integrateGyroStaysNormalizedOverLongRuns() {
        float[] q = Quaternion.identity();
        for (int i = 0; i < 10000; i++) {
            Quaternion.integrateGyro(q, 0.7f, -0.3f, 0.2f, 0.005f, q);
        }
        float len = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        assertEquals(1f, len, 1e-4f);
    }
}
