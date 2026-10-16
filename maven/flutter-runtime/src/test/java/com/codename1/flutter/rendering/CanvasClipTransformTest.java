/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codename1.flutter.Color;
import com.codename1.flutter.Paint;
import com.codename1.flutter.Rect;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Canvas.clipRect under a transform clips to the transformed rectangle. It used to
/// place the clip at the mapped top-left corner and size it by the transform's AVERAGE
/// scale, so under scale(2, 1) a 20x10 clip came out 30x15 -- cutting off a third of
/// what it should show and leaking below it -- and under a rotation it was an upright
/// box of the wrong size.
class CanvasClipTransformTest {

    private Image img;
    private Graphics g;

    @BeforeEach
    void install() {
        RasterDisplay.install();
        img = Image.createImage(100, 100, 0xffffffff);
        g = img.getGraphics();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Paint red() {
        Paint p = new Paint();
        p.color(new Color(0xffff0000L));
        return p;
    }

    private int at(int x, int y) {
        return ((RasterDisplay.Surface) img.getImage()).at(x, y);
    }

    @Test
    void aNonUniformScaleClipsToTheScaledRectangle() {
        GraphicsCanvas canvas = new GraphicsCanvas(g, 0, 0, 1.0);
        canvas.scale(2, 1);
        canvas.clipRect(Rect.fromLTRB(10, 10, 30, 20));
        assertEquals(20, g.getClipX());
        assertEquals(10, g.getClipY());
        assertEquals(40, g.getClipWidth(), "20 wide at scale 2");
        assertEquals(10, g.getClipHeight(), "10 tall at scale 1");
    }

    @Test
    void aMirroringScaleStillClipsTheRightArea() {
        GraphicsCanvas canvas = new GraphicsCanvas(g, 0, 0, 1.0);
        canvas.translate(100, 0);
        canvas.scale(-1, 1);
        canvas.clipRect(Rect.fromLTRB(10, 10, 30, 20));
        assertEquals(70, g.getClipX(), "x 10..30 mirrors to 70..90");
        assertEquals(20, g.getClipWidth());
    }

    /// A quarter-ish turn: the clip is the turned rectangle, not an upright box.
    @Test
    void aRotationClipsToTheTurnedRectangle() {
        GraphicsCanvas canvas = new GraphicsCanvas(g, 0, 0, 1.0);
        canvas.translate(50, 50);
        canvas.rotate(Math.PI / 4);
        canvas.clipRect(Rect.fromLTRB(-20, -5, 20, 5));
        canvas.drawRect(Rect.fromLTRB(-100, -100, 100, 100), red());
        assertEquals("ffff0000", Integer.toHexString(at(50, 50)), "the centre is inside");
        assertEquals("ffff0000", Integer.toHexString(at(60, 60)), "along the turned bar");
        assertEquals("ffffffff", Integer.toHexString(at(62, 40)), "off the bar, inside its bounding box");
        assertEquals("ffffffff", Integer.toHexString(at(90, 50)), "outside the bar's reach");
    }

    /// restore() puts the clip back, transform and all.
    @Test
    void restoreUndoesATransformedClip() {
        GraphicsCanvas canvas = new GraphicsCanvas(g, 0, 0, 1.0);
        canvas.save();
        canvas.rotate(0.3);
        canvas.clipRect(Rect.fromLTRB(0, 0, 10, 10));
        canvas.restore();
        canvas.drawRect(Rect.fromLTRB(80, 80, 90, 90), red());
        assertEquals("ffff0000", Integer.toHexString(at(85, 85)));
    }
}
