/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.ui.DevicePosture;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The fold animation, the recording script and the 3D rendering of a fold.
class FoldMotionTest {

    private static final class Log implements FoldController.Host {
        final List<String> events = new ArrayList<String>();
        int lastAngle = -1;

        public void angleChanged(int angle, boolean animating) {
            lastAngle = angle;
        }

        public void postureChanged(int posture) {
            events.add("posture:" + posture);
        }

        public void coverChanged(boolean closed) {
            events.add(closed ? "cover" : "inner");
        }
    }

    @Test
    void closingSweepsThroughHalfOpenedThenSwapsToTheCover() {
        Log log = new Log();
        FoldController fc = new FoldController(log);
        fc.moveTo(0, 1000, 0);
        assertTrue(fc.isAnimating());
        for (long t = 0; t <= 1000; t += 16) {
            fc.step(t);
        }
        fc.step(1001);
        assertFalse(fc.isAnimating());
        assertEquals(0, fc.getAngle());
        assertTrue(fc.isClosed());
        assertEquals(Arrays.asList("posture:" + DevicePosture.POSTURE_HALF_OPENED,
                "posture:" + DevicePosture.POSTURE_CLOSED, "cover"), log.events);
    }

    @Test
    void openingRestoresTheInnerDisplayBeforeAnyPostureEvent() {
        Log log = new Log();
        FoldController fc = new FoldController(log);
        fc.reset(0);
        fc.moveTo(180, 500, 0);
        assertEquals("inner", log.events.get(0));
        for (long t = 0; t <= 520; t += 16) {
            fc.step(t);
        }
        assertEquals(Arrays.asList("inner", "posture:" + DevicePosture.POSTURE_HALF_OPENED,
                "posture:" + DevicePosture.POSTURE_FLAT), log.events);
        assertEquals(180, log.lastAngle);
    }

    @Test
    void instantMovesNotifyInTheSameOrder() {
        Log log = new Log();
        FoldController fc = new FoldController(log);
        fc.moveTo(0, 0, 0);
        assertEquals(Arrays.asList("posture:" + DevicePosture.POSTURE_CLOSED, "cover"), log.events);
        log.events.clear();
        fc.moveTo(180, 0, 0);
        assertEquals(Arrays.asList("inner", "posture:" + DevicePosture.POSTURE_FLAT), log.events);
    }

    @Test
    void easedMotionIsMonotonicAndPartialMovesAreShorter() {
        Log log = new Log();
        FoldController fc = new FoldController(log);
        fc.moveTo(90, 1000, 0);
        double prev = 180;
        for (long t = 0; t < 500; t += 10) {
            fc.step(t);
            assertTrue(fc.getExactAngle() <= prev);
            prev = fc.getExactAngle();
        }
        // Half the sweep takes half the time.
        assertFalse(fc.step(501));
        assertEquals(90, fc.getAngle());
    }

    @Test
    void scriptParsesSortsAndRejectsBadSteps() {
        List<Object> json = new ArrayList<Object>();
        json.add(step("at", 3000, "posture", "closed"));
        json.add(step("at", 1000, "angle", 120));
        FoldScript s = FoldScript.parse(json);
        assertEquals(1000, s.getSteps().get(0).at);
        assertEquals(120, s.getSteps().get(0).angle);
        assertEquals("closed", s.getSteps().get(1).posture);

        final List<Object> bad = new ArrayList<Object>();
        bad.add(step("at", 10, "posture", "twisted"));
        assertThrows(IllegalArgumentException.class, () -> FoldScript.parse(bad));
        final List<Object> noTime = new ArrayList<Object>();
        noTime.add(step("posture", "flat", "x", 1));
        assertThrows(IllegalArgumentException.class, () -> FoldScript.parse(noTime));
        assertThrows(IllegalArgumentException.class, () -> FoldScript.parse("nope"));
    }

    private static Map<String, Object> step(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static final int RED = 0xffff0000;
    private static final int BLUE = 0xff0000ff;
    private static final int GREEN = 0xff00ff00;

    private static BufferedImage device(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, x < w / 2 ? RED : BLUE);
            }
        }
        return img;
    }

    private static BufferedImage solid(int w, int h, int c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    private static boolean near(int argb, int expected) {
        for (int shift = 0; shift <= 24; shift += 8) {
            if (Math.abs(((argb >>> shift) & 0xff) - ((expected >>> shift) & 0xff)) > 24) {
                return false;
            }
        }
        return true;
    }

    @Test
    void flatRenderMatchesTheDevice() {
        BufferedImage out = new FoldRenderer().render(device(200, 300), null, null, true, 180);
        assertTrue(near(out.getRGB(50, 150), RED), Integer.toHexString(out.getRGB(50, 150)));
        assertTrue(near(out.getRGB(150, 150), BLUE), Integer.toHexString(out.getRGB(150, 150)));
    }

    @Test
    void halfOpenedShowsBothHalvesTurnedTowardTheViewer() {
        BufferedImage out = new FoldRenderer().render(device(200, 300), null, null, true, 90);
        // Both halves stay visible either side of the hinge, shaded by
        // their tilt, so their dominant channel is what identifies them.
        int left = out.getRGB(75, 150);
        int right = out.getRGB(125, 150);
        assertTrue(((left >> 16) & 0xff) > (left & 0xff) + 40, Integer.toHexString(left));
        assertTrue((right & 0xff) > ((right >> 16) & 0xff) + 40, Integer.toHexString(right));
        // ...and foreshortened: neither reaches its flat outer edge, where
        // at most the faint drop shadow is left.
        assertTrue((out.getRGB(3, 150) >>> 24) < 64, Integer.toHexString(out.getRGB(3, 150)));
        assertTrue((out.getRGB(196, 150) >>> 24) < 64, Integer.toHexString(out.getRGB(196, 150)));
    }

    @Test
    void closedShowsTheCoverOverTheFixedHalf() {
        BufferedImage out = new FoldRenderer().render(device(200, 300), solid(100, 300, GREEN),
                new Rectangle(100, 0, 0, 300), true, 0);
        assertTrue(near(out.getRGB(50, 150), GREEN), Integer.toHexString(out.getRGB(50, 150)));
        assertEquals(0, out.getRGB(150, 150) >>> 24);
    }

    @Test
    void flipFoldsTheLowerPanelUp() {
        BufferedImage img = new BufferedImage(200, 300, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 200; x++) {
                img.setRGB(x, y, y < 150 ? RED : BLUE);
            }
        }
        BufferedImage out = new FoldRenderer().render(img, solid(200, 150, GREEN), null, false, 0);
        assertTrue(near(out.getRGB(100, 75), GREEN), Integer.toHexString(out.getRGB(100, 75)));
        assertEquals(0, out.getRGB(100, 260) >>> 24);
    }

    @Test
    void aQuarterTurnLandsOnTheLandscapeDevice() {
        // Turned a full quarter clockwise, the 200x300 device fills a
        // 300x200 frame with its left (red) half on top, as the clockwise
        // landscape skin has it.
        BufferedImage out = new FoldRenderer().renderRotation(device(200, 300), -90, 0, 300, 200);
        assertTrue(near(out.getRGB(150, 50), RED), Integer.toHexString(out.getRGB(150, 50)));
        assertTrue(near(out.getRGB(150, 150), BLUE), Integer.toHexString(out.getRGB(150, 150)));
    }

    @Test
    void halfwayThroughATurnTheDeviceIsLiftedAndCornersAreClear() {
        BufferedImage out = new FoldRenderer().renderRotation(device(200, 300), -45, 1, 300, 300);
        // Diagonal and shrunk: the frame's corners are background, never
        // the opaque black the transparent texels used to blend into.
        assertTrue((out.getRGB(2, 2) >>> 24) < 64, Integer.toHexString(out.getRGB(2, 2)));
        assertTrue((out.getRGB(150, 150) >>> 24) == 0xff, Integer.toHexString(out.getRGB(150, 150)));
    }

    @Test
    void inASquareFrameTheDeviceKeepsItsSizeAtTheStartOfATurn() {
        // 200x300 in a 300x300 frame, not yet turned: full height, never
        // shrunk to fit the shorter side of the new orientation.
        BufferedImage out = new FoldRenderer().renderRotation(device(200, 300), 0, 0, 300, 300);
        assertTrue((out.getRGB(150, 3) >>> 24) == 0xff, Integer.toHexString(out.getRGB(150, 3)));
        assertTrue((out.getRGB(150, 296) >>> 24) == 0xff, Integer.toHexString(out.getRGB(150, 296)));
        assertTrue((out.getRGB(20, 150) >>> 24) < 64, Integer.toHexString(out.getRGB(20, 150)));
    }

    @Test
    void aScriptCanTurnTheDevice() {
        java.util.List<Object> steps = new java.util.ArrayList<Object>();
        java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
        m.put("at", Long.valueOf(10));
        m.put("orientation", "landscape");
        steps.add(m);
        FoldScript script = FoldScript.parse(steps);
        assertEquals("landscape", script.getSteps().get(0).orientation);
        assertFalse(script.foldsTheDevice());
        m.put("orientation", "sideways");
        assertThrows(IllegalArgumentException.class, () -> FoldScript.parse(steps));
    }

    @Test
    void theCameraIslandIsOnTheOuterEdgeOfThePlainBack() {
        BufferedImage back = FoldRenderer.paintBackPlate(200, 300, true, 20);
        // Lenses are near black, the plate is a mid grey.
        int lens = back.getRGB(200 - 10 - (int) (84 * 0.27f), 10 + (int) (84 * 0.27f)) & 0xff;
        int plate = back.getRGB(30, 280) & 0xff;
        assertTrue(lens < plate, lens + " vs " + plate);
    }
}
