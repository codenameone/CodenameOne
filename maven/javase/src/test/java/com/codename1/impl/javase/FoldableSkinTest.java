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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.io.Properties;
import com.codename1.ui.DevicePosture;
import java.awt.Rectangle;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FoldableSkinTest {

    @Test
    void legacySkinIsNotFoldable() {
        Properties p = new Properties();
        p.setProperty("ppi", "460");
        assertNull(FoldableSkin.parse(p));
        assertNull(FoldableSkin.parse(null));
    }

    @Test
    void defaultsDescribeAContinuousBookFold() {
        Properties p = new Properties();
        p.setProperty("foldable", "true");
        FoldableSkin f = FoldableSkin.parse(p);
        assertTrue(f.isVertical());
        assertFalse(f.isDualScreen());
        assertFalse(f.hasCover());
        assertEquals(Arrays.asList("flat", "half", "closed"), f.getPostures());
        assertEquals(DevicePosture.FOLD_ORIENTATION_VERTICAL, f.getFoldOrientation());
    }

    @Test
    void parsesDualScreenHingeCoverAndPostures() {
        Properties p = new Properties();
        p.setProperty("foldable", "TRUE");
        p.setProperty("foldOrientation", "horizontal");
        p.setProperty("foldType", "dualScreen");
        p.setProperty("hingePortrait", "0,500,400,20");
        p.setProperty("foldPostures", "Flat, closed, bogus, flat");
        p.setProperty("coverPortraitScreenX", "10");
        p.setProperty("coverPortraitScreenY", "20");
        p.setProperty("coverPortraitScreenWidth", "300");
        p.setProperty("coverPortraitScreenHeight", "600");
        p.setProperty("coverPpi", "401.5");
        FoldableSkin f = FoldableSkin.parse(p);
        assertFalse(f.isVertical());
        assertTrue(f.isDualScreen());
        assertEquals(Arrays.asList("flat", "closed"), f.getPostures());
        assertEquals(new Rectangle(10, 20, 300, 600), f.getCoverScreen(true));
        assertEquals(new Rectangle(10, 20, 300, 600), f.getCoverScreen(false));
        assertEquals(401.5, f.getCoverPpi(), 0.001);
        assertEquals(new Rectangle(0, 500, 400, 20), f.getHinge(false));
    }

    @Test
    void anglesMapToPosturesAtWindowManagerThresholds() {
        assertEquals(DevicePosture.POSTURE_CLOSED, FoldableSkin.postureForAngle(0));
        assertEquals(DevicePosture.POSTURE_CLOSED, FoldableSkin.postureForAngle(29));
        assertEquals(DevicePosture.POSTURE_HALF_OPENED, FoldableSkin.postureForAngle(30));
        assertEquals(DevicePosture.POSTURE_HALF_OPENED, FoldableSkin.postureForAngle(150));
        assertEquals(DevicePosture.POSTURE_FLAT, FoldableSkin.postureForAngle(151));
        assertEquals(0, FoldableSkin.angleForPosture("closed"));
        assertEquals(90, FoldableSkin.angleForPosture("half"));
        assertEquals(180, FoldableSkin.angleForPosture("flat"));
    }

    @Test
    void foldBoundsScaleTheHingeIntoDisplayCoordinates() {
        Properties p = new Properties();
        p.setProperty("foldable", "true");
        p.setProperty("foldType", "dualScreen");
        p.setProperty("hingePortrait", "140,0,20,400");
        FoldableSkin dual = FoldableSkin.parse(p);
        // Screen 100..300 in skin pixels, displayed at 2x.
        Rectangle b = dual.foldBounds(true, new Rectangle(40, 0, 200, 400), 400, 800);
        assertEquals(new Rectangle(200, 0, 40, 800), b);

        p.setProperty("foldType", "continuous");
        FoldableSkin cont = FoldableSkin.parse(p);
        Rectangle c = cont.foldBounds(true, new Rectangle(40, 0, 200, 400), 400, 800);
        assertEquals(new Rectangle(219, 0, 2, 800), c);

        // No hinge key: the centre line.
        Properties q = new Properties();
        q.setProperty("foldable", "true");
        assertEquals(new Rectangle(199, 0, 2, 800),
                FoldableSkin.parse(q).foldBounds(true, new Rectangle(0, 0, 200, 400), 400, 800));
        // A book fold held in landscape folds across the height.
        assertEquals(new Rectangle(0, 399, 400, 2),
                FoldableSkin.parse(q).foldBounds(false, new Rectangle(0, 0, 200, 400), 400, 800));
    }
}
