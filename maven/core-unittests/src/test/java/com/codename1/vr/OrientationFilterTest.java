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
package com.codename1.vr;

import com.codename1.gpu.Quaternion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the deterministic {@link OrientationFilter}: pure math with no
 * Display dependency, so identical inputs must always give identical output.
 */
class OrientationFilterTest {

    private static final float EPS = 1e-3f;

    private static float angleAroundAxis(float[] q, float ax, float ay, float az) {
        // Rotation angle of q assuming it is (close to) a rotation around the
        // given unit axis.
        float dot = q[0] * ax + q[1] * ay + q[2] * az;
        return 2f * (float) Math.atan2(dot, q[3]);
    }

    @Test
    void zeroInputKeepsIdentity() {
        OrientationFilter f = new OrientationFilter();
        for (int i = 0; i < 100; i++) {
            f.update(0, 0, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        }
        assertArrayEquals(new float[]{0, 0, 0, 1}, f.getOrientation(), EPS);
    }

    @Test
    void pureGyroIntegratesTheExpectedAngle() {
        OrientationFilter f = new OrientationFilter();
        // 0.5 rad/s around device Y for 2 seconds in 10ms steps = 1 radian.
        for (int i = 0; i < 200; i++) {
            f.update(0, 0.5f, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        }
        float[] q = f.getOrientation();
        assertEquals(1f, angleAroundAxis(q, 0, 1, 0), 1e-2f);
    }

    @Test
    void identicalInputsAreDeterministic() {
        OrientationFilter a = new OrientationFilter();
        OrientationFilter b = new OrientationFilter();
        for (int i = 0; i < 500; i++) {
            float gx = (i % 7) * 0.01f;
            float ay = 9.5f + (i % 3) * 0.1f;
            a.update(gx, 0.2f, -0.1f, 0.3f, ay, 0.2f, 20f, 0f, -30f, 0.01f);
            b.update(gx, 0.2f, -0.1f, 0.3f, ay, 0.2f, 20f, 0f, -30f, 0.01f);
        }
        assertArrayEquals(a.getOrientation(), b.getOrientation());
    }

    @Test
    void accelerometerConvergesTiltTowardGravity() {
        OrientationFilter f = new OrientationFilter();
        f.setGyroWeight(0.9f);
        // Device pitched forward 90 degrees: gravity reaction along device +Z
        // (the device Z axis points up in the world). The filter should
        // converge to an orientation whose world-up maps to device +Z.
        for (int i = 0; i < 2000; i++) {
            f.update(0, 0, 0, 0, 0, 9.81f, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        }
        float[] q = f.getOrientation();
        float[] deviceUp = {0, 0, 1};
        Quaternion.rotateVector(q, deviceUp);
        assertEquals(0f, deviceUp[0], EPS);
        assertEquals(1f, deviceUp[1], EPS);
        assertEquals(0f, deviceUp[2], EPS);
    }

    @Test
    void higherGyroWeightConvergesSlower() {
        OrientationFilter fast = new OrientationFilter();
        fast.setGyroWeight(0.5f);
        OrientationFilter slow = new OrientationFilter();
        slow.setGyroWeight(0.99f);
        for (int i = 0; i < 10; i++) {
            fast.update(0, 0, 0, 0, 0, 9.81f, Float.NaN, Float.NaN, Float.NaN, 0.01f);
            slow.update(0, 0, 0, 0, 0, 9.81f, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        }
        float[] up = {0, 0, 1};
        float[] q = fast.getOrientation();
        Quaternion.rotateVector(q, up);
        float fastY = up[1];
        up[0] = 0;
        up[1] = 0;
        up[2] = 1;
        q = slow.getOrientation();
        Quaternion.rotateVector(q, up);
        float slowY = up[1];
        assertTrue(fastY > slowY, "lower gyro weight must correct tilt faster");
    }

    @Test
    void magnetometerCorrectsYawAndNaNSkipsIt() {
        // Start yawed 45 degrees off; with north (-Z field in the device
        // frame when facing north) the magnetometer should pull yaw back.
        OrientationFilter f = new OrientationFilter();
        f.setGyroWeight(0.9f);
        // Introduce a yaw error via gyro.
        f.update(0, 0.785f, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 1f);
        float before = Math.abs(angleAroundAxis(f.getOrientation(), 0, 1, 0));
        // Field pointing along device -Z (device facing north, flat field).
        for (int i = 0; i < 500; i++) {
            f.update(0, 0, 0, 0, 0, 0, 0f, 0f, -30f, 0.01f);
        }
        float after = Math.abs(angleAroundAxis(f.getOrientation(), 0, 1, 0));
        assertTrue(after < before * 0.1f, "yaw error should shrink: " + before + " -> " + after);

        // NaN magnetometer leaves yaw untouched.
        OrientationFilter g = new OrientationFilter();
        g.update(0, 0.785f, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 1f);
        float yawBefore = angleAroundAxis(g.getOrientation(), 0, 1, 0);
        for (int i = 0; i < 100; i++) {
            g.update(0, 0, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        }
        assertEquals(yawBefore, angleAroundAxis(g.getOrientation(), 0, 1, 0), EPS);
    }

    @Test
    void dtScalingIsConsistent() {
        OrientationFilter one = new OrientationFilter();
        OrientationFilter two = new OrientationFilter();
        // One 20ms step vs two 10ms steps of the same constant rate.
        one.update(0.4f, 0, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.02f);
        two.update(0.4f, 0, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        two.update(0.4f, 0, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.01f);
        assertArrayEquals(one.getOrientation(), two.getOrientation(), 1e-4f);
    }

    @Test
    void quaternionStaysNormalizedOverLongRuns() {
        OrientationFilter f = new OrientationFilter();
        for (int i = 0; i < 20000; i++) {
            f.update(0.3f, -0.2f, 0.15f, 0.5f, 9.7f, 0.4f, 25f, -10f, -35f, 0.005f);
        }
        float[] q = f.getOrientation();
        float len = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        assertEquals(1f, len, 1e-3f);
    }

    @Test
    void recenterYawZeroesYawAndKeepsPitch() {
        OrientationFilter f = new OrientationFilter();
        // Yaw 60 degrees then pitch up 30 degrees (device frame).
        f.update(0, (float) Math.toRadians(60), 0, 0, 0, 0,
                Float.NaN, Float.NaN, Float.NaN, 1f);
        f.update((float) Math.toRadians(30), 0, 0, 0, 0, 0,
                Float.NaN, Float.NaN, Float.NaN, 1f);
        f.recenterYaw();
        float[] q = f.getOrientation();
        // Forward direction should now have no horizontal deviation.
        float[] fwd = {0, 0, -1};
        Quaternion.rotateVector(q, fwd);
        assertEquals(0f, fwd[0], EPS);
        assertTrue(fwd[2] < 0, "still facing forward");
        // The pitch (vertical component) survives the recenter.
        assertEquals(Math.sin(Math.toRadians(30)), fwd[1], 1e-2f);
    }

    @Test
    void invalidGyroWeightRejected() {
        final OrientationFilter f = new OrientationFilter();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                f.setGyroWeight(0f);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                f.setGyroWeight(1.5f);
            }
        });
    }

    @Test
    void resetReturnsToIdentity() {
        OrientationFilter f = new OrientationFilter();
        f.update(1f, 2f, 3f, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN, 0.5f);
        f.reset();
        assertArrayEquals(new float[]{0, 0, 0, 1}, f.getOrientation());
    }
}
