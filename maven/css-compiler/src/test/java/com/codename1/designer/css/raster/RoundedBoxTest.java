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
package com.codename1.designer.css.raster;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import org.junit.jupiter.api.Test;

/// Radius normalisation and the geometry of the box outline.
class RoundedBoxTest {
    private static double[] all(double r) {
        return new double[] {r, r, r, r, r, r, r, r};
    }

    @Test
    void radiiThatFitAreKept() {
        double[] r = {1, 2, 3, 4, 5, 6, 7, 8};
        assertArrayEquals(r, new RoundedBox(0, 0, 100, 100, r).getRadii());
        assertArrayEquals(new double[8], new RoundedBox(0, 0, 100, 100, null).getRadii());
    }

    @Test
    void overlappingRadiiAreAllScaledByTheSameFactor() {
        // 1000 on a 100x40 box: the left side allows 40 / 2000, the smallest factor.
        assertArrayEquals(all(20), new RoundedBox(0, 0, 100, 40, all(1000)).getRadii(), 1e-9);
        // 50% everywhere is an ellipse and needs no scaling.
        double[] ellipse = {50, 20, 50, 20, 50, 20, 50, 20};
        assertArrayEquals(ellipse, new RoundedBox(0, 0, 100, 40, ellipse).getRadii(), 1e-9);
        // Only the top side overflows (80 + 40 > 100), but every radius
        // shrinks by 100 / 120, including the ones that had room.
        double[] r = {80, 10, 40, 10, 6, 6, 12, 12};
        double f = 100.0 / 120;
        assertArrayEquals(new double[] {80 * f, 10 * f, 40 * f, 10 * f, 6 * f, 6 * f, 12 * f, 12 * f},
                new RoundedBox(0, 0, 100, 100, r).getRadii(), 1e-9);
    }

    @Test
    void cornerWithOneZeroRadiusIsSquare() {
        RoundedBox b = new RoundedBox(0, 0, 100, 100, new double[] {10, 0, 0, 10, -5, 5, 4, 4});
        assertArrayEquals(new double[] {0, 0, 0, 0, 0, 0, 4, 4}, b.getRadii());
    }

    @Test
    void insetShrinksEachRadiusByItsAdjacentSide() {
        RoundedBox b = new RoundedBox(10, 20, 100, 80, new double[] {20, 20, 20, 20, 20, 20, 20, 20});
        RoundedBox in = b.inset(5, 10, 25, 15);
        assertEquals(25, in.getX());
        assertEquals(25, in.getY());
        assertEquals(75, in.getWidth());
        assertEquals(50, in.getHeight());
        // TL: x - left, y - top. TR: x - right, y - top. BR: the bottom
        // border (25) is wider than the radius, so that corner is square.
        // BL: x - left, y - bottom < 0, square too.
        assertArrayEquals(new double[] {5, 15, 10, 15, 0, 0, 0, 0}, in.getRadii(), 1e-9);
        assertFalse(in.isEmpty());
        assertTrue(b.inset(50, 0, 30, 0).isEmpty());
        assertTrue(b.inset(0, 60, 0, 60).isEmpty());
    }

    @Test
    void growAndTranslate() {
        RoundedBox b = new RoundedBox(10, 20, 100, 80, all(20));
        RoundedBox g = b.grow(5);
        assertEquals(5, g.getX());
        assertEquals(15, g.getY());
        assertEquals(110, g.getWidth());
        assertEquals(90, g.getHeight());
        assertArrayEquals(all(25), g.getRadii(), 1e-9);
        assertArrayEquals(all(12), b.grow(-8).getRadii(), 1e-9);
        assertArrayEquals(all(0), b.grow(-30).getRadii(), 1e-9);
        assertTrue(b.grow(-40).isEmpty());
        assertArrayEquals(all(0), new RoundedBox(0, 0, 10, 10, null).grow(50).getRadii(), 0);
        RoundedBox t = b.translate(3, -4);
        assertEquals(13, t.getX());
        assertEquals(16, t.getY());
        assertArrayEquals(all(20), t.getRadii());
    }

    @Test
    void pathBoundsAreTheBox() {
        RoundedBox b = new RoundedBox(10.5, 20.25, 100.5, 80, new double[] {30, 10, 5, 5, 0, 0, 20, 40});
        Rectangle2D r = b.toPath().getBounds2D();
        assertEquals(10.5, r.getMinX(), 1e-6);
        assertEquals(20.25, r.getMinY(), 1e-6);
        assertEquals(111, r.getMaxX(), 1e-6);
        assertEquals(100.25, r.getMaxY(), 1e-6);
        assertTrue(new RoundedBox(0, 0, 0, 10, null).toPath().getBounds2D().isEmpty());
    }

    @Test
    void arcsAreEllipticalToWithinAHairOfTheTrueCurve() {
        // Top-left radii 40 x 10 centred on (40,10). Every point of that
        // corner's curve must satisfy the ellipse equation.
        RoundedBox b = new RoundedBox(0, 0, 100, 60, new double[] {40, 10, 0, 0, 0, 0, 0, 0});
        Path2D.Double p = b.toPath();
        assertFalse(p.contains(10.5, 1.5), "outside the ellipse");
        assertTrue(p.contains(20.5, 5.5), "inside the ellipse");
        assertTrue(p.contains(99.9, 0.1), "the other corners are square");
        assertTrue(p.contains(0.1, 59.9));
        PathIterator it = p.getPathIterator(null, 0.001);
        double[] c = new double[6];
        int onCurve = 0;
        while (!it.isDone()) {
            it.currentSegment(c);
            if (c[0] < 40 - 1e-9 && c[1] < 10 - 1e-9) {
                double ex = (c[0] - 40) / 40;
                double ey = (c[1] - 10) / 10;
                assertEquals(1, Math.sqrt(ex * ex + ey * ey), 0.0005, "point " + c[0] + "," + c[1]);
                onCurve++;
            }
            it.next();
        }
        assertTrue(onCurve > 8, "the flattened corner has points to check: " + onCurve);
    }

    @Test
    void ringIsTheOuterBoxMinusTheInnerOne() {
        RoundedBox outer = new RoundedBox(0, 0, 100, 100, all(30));
        RoundedBox inner = outer.inset(10, 10, 10, 10);
        Path2D.Double ring = RoundedBox.ring(outer, inner);
        assertTrue(ring.contains(50, 5));
        assertFalse(ring.contains(50, 15));
        assertFalse(ring.contains(50, 50));
        assertTrue(ring.contains(12.5, 12.5));
        assertFalse(ring.contains(19.5, 19.5));
        assertFalse(ring.contains(5, 5));
        // With an empty inner box the ring is the whole outer box.
        assertTrue(RoundedBox.ring(outer, outer.inset(60, 0, 60, 0)).contains(50, 50));
    }

    @Test
    void sidePathRunsFromMidCornerToMidCorner() {
        RoundedBox b = new RoundedBox(0, 0, 100, 60, all(20));
        double mid = 20 - 20 * Math.sqrt(0.5);
        Rectangle2D top = b.sidePath(RoundedBox.TOP).getBounds2D();
        assertEquals(mid, top.getMinX(), 1e-6);
        assertEquals(100 - mid, top.getMaxX(), 1e-6);
        assertEquals(0, top.getMinY(), 1e-6);
        assertEquals(mid, top.getMaxY(), 1e-6);
        Rectangle2D left = b.sidePath(RoundedBox.LEFT).getBounds2D();
        assertEquals(0, left.getMinX(), 1e-6);
        assertEquals(mid, left.getMaxX(), 1e-6);
        assertEquals(mid, left.getMinY(), 1e-6);
        assertEquals(60 - mid, left.getMaxY(), 1e-6);
        Rectangle2D bottom = b.sidePath(RoundedBox.BOTTOM).getBounds2D();
        assertEquals(60, bottom.getMaxY(), 1e-6);
        assertEquals(60 - mid, bottom.getMinY(), 1e-6);
        Rectangle2D right = b.sidePath(RoundedBox.RIGHT).getBounds2D();
        assertEquals(100, right.getMaxX(), 1e-6);
        assertEquals(100 - mid, right.getMinX(), 1e-6);
        // Square corners: the side is the plain edge.
        Rectangle2D square = new RoundedBox(5, 5, 50, 30, null).sidePath(RoundedBox.TOP).getBounds2D();
        assertEquals(5, square.getMinX(), 1e-9);
        assertEquals(55, square.getMaxX(), 1e-9);
        assertEquals(5, square.getMinY(), 1e-9);
        assertEquals(5, square.getMaxY(), 1e-9);
    }
}
