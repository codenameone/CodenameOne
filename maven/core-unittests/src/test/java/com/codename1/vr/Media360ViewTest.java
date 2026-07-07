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

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link Media360View} on the headless test platform: without a
 * GPU backend the component must degrade gracefully, and its look-angle state
 * machine (yaw wrap, pitch clamp, drag deltas, reset) must behave.
 */
class Media360ViewTest extends UITestBase {

    @Test
    void constructsWithoutGpuBackendAndReportsUnsupported() {
        Media360View view = new Media360View();
        assertFalse(view.isSupported());
        assertNotNull(view.getRenderView());
        // setImage before any GPU init must not crash headless.
        view.setImage(Image.createImage(4, 2, 0xff112233));
    }

    @Test
    void yawAndPitchSettersWithPitchClamp() {
        Media360View view = new Media360View();
        view.setYaw(123f);
        assertEquals(123f, view.getYaw(), 0f);
        view.setPitch(45f);
        assertEquals(45f, view.getPitch(), 0f);
        view.setPitch(120f);
        assertEquals(89f, view.getPitch(), 0f, "pitch clamps at the pole");
        view.setPitch(-120f);
        assertEquals(-89f, view.getPitch(), 0f);
    }

    @Test
    void dragUpdatesYawAndPitch() {
        Media360View view = new Media360View();
        view.setWidth(360);
        view.setHeight(180);
        view.pointerPressed(100, 100);
        // Drag right by 36px on a 360px wide view = 18 degrees; the view
        // turns the opposite way (grab-the-world).
        view.pointerDragged(136, 100);
        assertEquals(-18f, view.getYaw(), 0.01f);
        // Drag down by 18px on a 180px tall view = 18 degrees pitch up.
        view.pointerDragged(136, 118);
        assertEquals(18f, view.getPitch(), 0.01f);
        view.pointerReleased(136, 118);

        // A fresh press does not jump: deltas restart from the new point.
        view.pointerPressed(0, 0);
        view.pointerDragged(0, 0);
        assertEquals(-18f, view.getYaw(), 0.01f);
    }

    @Test
    void pitchClampsDuringDrag() {
        Media360View view = new Media360View();
        view.setWidth(100);
        view.setHeight(100);
        view.pointerPressed(50, 0);
        view.pointerDragged(50, 100);
        view.pointerDragged(50, 200);
        assertEquals(89f, view.getPitch(), 0f);
    }

    @Test
    void resetRestoresStraightAhead() {
        Media360View view = new Media360View();
        view.setYaw(90f);
        view.setPitch(-30f);
        view.reset();
        assertEquals(0f, view.getYaw(), 0f);
        assertEquals(0f, view.getPitch(), 0f);
    }

    @Test
    void stereoToggle() {
        Media360View view = new Media360View();
        assertFalse(view.isStereo());
        view.setStereo(true);
        assertTrue(view.isStereo());
    }

    @Test
    void headTrackingToggleWithoutSensorsIsSafe() {
        implementation.setMotionSensorManager(new com.codename1.sensors.FakeMotionSensorManager());
        Media360View view = new Media360View();
        view.setHeadTrackingEnabled(true);
        assertTrue(view.isHeadTrackingEnabled());
        view.setHeadTrackingEnabled(false);
        assertFalse(view.isHeadTrackingEnabled());
    }
}
