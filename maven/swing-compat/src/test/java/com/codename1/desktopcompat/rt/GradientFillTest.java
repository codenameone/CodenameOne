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
package com.codename1.desktopcompat.rt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.GradientPaint;
import com.codename1.desktopcompat.java.awt.geom.Path2D;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;
import com.codename1.ui.Image;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/// A `GradientPaint` through a shape: the colors its definition gives, and
/// nothing outside the shape.
///
/// The tests run on a surface with shapes and no shape clip (see
/// `HeadlessImplementation.shapes`), which is no kinder than the ports whose
/// captures showed this fill as a hollow outline and as a block from the
/// panel's corner: a fill that needs a rectangle on top of a shape clip to
/// stay inside its shape paints outside it here.
public class GradientFillTest extends KernelTestBase {

    private static final int W = 480;
    private static final int H = 280;

    private Image surface;

    @Before
    public void shapes() {
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.pixelImages = true;
        HeadlessImplementation.shapes = true;
        surface = Image.createImage(W, H, 0);
    }

    @After
    public void noShapes() {
        HeadlessImplementation.rasterImages = false;
        HeadlessImplementation.pixelImages = false;
        HeadlessImplementation.shapes = false;
        HeadlessImplementation.resetRaster();
    }

    /// A graphics over the whole surface. The scale is the 2 of the base
    /// class, so a user unit is two pixels.
    private G2D graphics() {
        return G2D.forPeer(surface.getGraphics(), 0, 0, W, H);
    }

    private int pixel(int x, int y) {
        return surface.getRGB()[y * W + x];
    }

    private static void near(String what, int expected, int actual) {
        assertTrue(what + ": alpha " + (actual >>> 24), (actual >>> 24) >= 250);
        for (int shift = 0; shift <= 16; shift += 8) {
            int e = (expected >> shift) & 0xff;
            int a = (actual >> shift) & 0xff;
            assertTrue(what + ": expected " + Integer.toHexString(expected) + ", was " + Integer.toHexString(actual),
                    Math.abs(e - a) <= 4);
        }
    }

    private void nothingOutside(int x1, int y1, int x2, int y2) {
        int[] px = surface.getRGB();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (x < x1 || x >= x2 || y < y1 || y >= y2) {
                    assertEquals("painted outside the shape at " + x + "," + y, 0, px[y * W + x] >>> 24);
                }
            }
        }
    }

    /// The fill of the painting scene: a gradient from one end of a rounded
    /// rectangle to the other. User (16, 30, 150, 44) is device
    /// (32, 60, 300, 88).
    @Test
    public void aGradientFillsARoundedRectangleAndNothingElse() {
        G2D g = graphics();
        g.setPaint(new GradientPaint(16, 30, new Color(0x11998e), 166, 30, new Color(0x38ef7d)));
        g.fill(new RoundRectangle2D.Double(16, 30, 150, 44, 24, 24));
        g.finish();
        near("start", 0xff11998e, pixel(33, 104));
        near("middle", 0xff25c486, pixel(182, 104));
        near("end", 0xff38ef7d, pixel(330, 104));
        // Inside the bounds, outside the round corner.
        assertEquals("the corner of the bounds", 0, pixel(34, 62) >>> 24);
        assertEquals("the far corner of the bounds", 0, pixel(329, 145) >>> 24);
        nothingOutside(32, 60, 332, 148);
    }

    /// Past its two points a gradient that does not cycle goes on in the
    /// nearest color, and a cyclic one walks back: the first color returns
    /// as far past the second point as the points are apart.
    @Test
    public void pastItsPointsAGradientHoldsItsColorOrWalksBack() {
        Color first = new Color(0x000000);
        Color second = new Color(0xffffff);
        G2D g = graphics();
        g.setPaint(new GradientPaint(40, 0, first, 80, 0, second));
        g.fill(new RoundRectangle2D.Double(10, 10, 200, 20, 6, 6));
        g.setPaint(new GradientPaint(40, 0, first, 80, 0, second, true));
        g.fill(new RoundRectangle2D.Double(10, 50, 200, 20, 6, 6));
        g.finish();
        // Device x is twice the user x; rows 40 and 120 are mid shape.
        near("before the first point", 0xff000000, pixel(50, 40));
        near("half way", 0xff808080, pixel(120, 40));
        near("past the second point", 0xffffffff, pixel(300, 40));
        near("cyclic, half way", 0xff808080, pixel(120, 120));
        near("cyclic, at the second point", 0xffffffff, pixel(160, 120));
        near("cyclic, half way back", 0xff808080, pixel(200, 120));
        near("cyclic, the first color again", 0xff000000, pixel(240, 120));
        near("cyclic, before the first point", 0xff808080, pixel(40, 120));
    }

    /// The place on the gradient is measured in user space. Under a matrix
    /// that stretches one axis the colors still run from point to point, and
    /// a triangle is filled as a triangle.
    @Test
    public void aGradientUnderAMatrixFollowsTheUserSpace() {
        G2D g = graphics();
        g.translate(20, 10);
        g.scale(2, 1);
        // User (0,0)-(50,100) is device (40,20)-(240,220).
        g.setPaint(new GradientPaint(0, 0, new Color(0xff0000), 0, 100, new Color(0x0000ff)));
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(0, 0);
        triangle.lineTo(50, 0);
        triangle.lineTo(0, 100);
        triangle.closePath();
        g.fill(triangle);
        g.finish();
        near("top", 0xffff0000, pixel(60, 21));
        near("half way down", 0xff800080, pixel(60, 120));
        near("near the bottom", 0xff0a00f5, pixel(42, 212));
        // The half of the bounds the triangle does not cover.
        assertEquals(0, pixel(230, 200) >>> 24);
        assertEquals(0, pixel(160, 130) >>> 24);
        nothingOutside(40, 20, 240, 220);
    }
}
