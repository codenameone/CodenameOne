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

import com.codename1.gpu.Camera;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Quaternion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link VRCameraRig} per-eye camera math: interpupillary
 * separation along the head-right vector, orientation handling and lens
 * parameter propagation. Pure math, no Display required.
 */
class VRCameraRigTest {

    private static final float EPS = 1e-5f;

    @Test
    void identityOrientationSeparatesEyesAlongX() {
        VRCameraRig rig = new VRCameraRig(new VRSettings().ipdMeters(0.064f));
        Camera left = new Camera();
        Camera right = new Camera();
        rig.apply(left, VREye.LEFT, 1f);
        rig.apply(right, VREye.RIGHT, 1f);
        assertEquals(-0.032f, left.getEyeX(), EPS);
        assertEquals(0.032f, right.getEyeX(), EPS);
        assertEquals(0f, left.getEyeY(), EPS);
        assertEquals(0f, left.getEyeZ(), EPS);
        // Total separation is exactly the IPD.
        assertEquals(0.064f, right.getEyeX() - left.getEyeX(), EPS);
    }

    @Test
    void centerEyeHasNoOffset() {
        VRCameraRig rig = new VRCameraRig(new VRSettings().ipdMeters(0.064f));
        rig.setPosition(1f, 2f, 3f);
        Camera center = new Camera();
        rig.apply(center, VREye.CENTER, 1f);
        assertEquals(1f, center.getEyeX(), EPS);
        assertEquals(2f, center.getEyeY(), EPS);
        assertEquals(3f, center.getEyeZ(), EPS);
    }

    @Test
    void yawRotationRotatesTheSeparationAxis() {
        VRCameraRig rig = new VRCameraRig(new VRSettings().ipdMeters(0.1f));
        // Head yawed 90 degrees left (counterclockwise around Y): the head's
        // right vector becomes world -Z.
        rig.setOrientation(Quaternion.fromAxisAngle((float) Math.PI / 2, 0, 1, 0));
        Camera left = new Camera();
        Camera right = new Camera();
        rig.apply(left, VREye.LEFT, 1f);
        rig.apply(right, VREye.RIGHT, 1f);
        assertEquals(0f, left.getEyeX(), EPS);
        assertEquals(0.05f, left.getEyeZ(), EPS);
        assertEquals(-0.05f, right.getEyeZ(), EPS);
    }

    @Test
    void viewMatrixMatchesLookAtAlongTheHeadForward() {
        VRCameraRig rig = new VRCameraRig(new VRSettings().ipdMeters(0f));
        rig.setPosition(0f, 1.6f, 0f);
        Camera cam = new Camera();
        rig.apply(cam, VREye.CENTER, 1f);
        // Identity orientation looks along -Z with +Y up.
        float[] expected = Matrix4.lookAt(0f, 1.6f, 0f, 0f, 1.6f, -1f, 0f, 1f, 0f);
        float[] actual = cam.getViewMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(expected[i], actual[i], EPS, "element " + i);
        }
    }

    @Test
    void lensParametersPropagateToTheProjection() {
        VRCameraRig rig = new VRCameraRig(
                new VRSettings().fovYDegrees(100f).nearFar(0.5f, 200f));
        Camera cam = new Camera();
        rig.apply(cam, VREye.CENTER, 2f);
        float[] expected = Matrix4.perspective((float) Math.toRadians(100f), 2f, 0.5f, 200f);
        float[] actual = cam.getProjectionMatrix();
        for (int i = 0; i < 16; i++) {
            assertEquals(expected[i], actual[i], EPS, "element " + i);
        }
    }

    @Test
    void nullSettingsFallBackToDefaults() {
        VRCameraRig rig = new VRCameraRig(null);
        assertEquals(0.064f, rig.getSettings().getIpdMeters(), EPS);
        assertEquals(90f, rig.getSettings().getFovYDegrees(), EPS);
        assertTrue(rig.getSettings().isStereo());
    }
}
