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
package com.codename1.flutter.material;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rounded clip a Material surface installs, intersected with the clip it inherits.
 *
 * <p>This used to go through {@code GeneralPath.intersection()}, a general polygon clipper
 * that allocated per card per frame and drove the collector from inside paint. It is now
 * computed directly, which is only correct if the intersection really does reduce to
 * "the intersected bounds, rounded at the corners the clip left alone" - so that is what
 * these assert.</p>
 */
class MaterialClipGeometryTest {

    /// Bounds and the four corner radii, clockwise from the top left.
    private static int[] geom(int x, int y, int w, int h, int r,
            int cx, int cy, int cw, int ch) {
        int[] out = new int[8];
        assertTrue(MaterialRenderElement.clipGeometry(out, x, y, w, h, r, cx, cy, cw, ch),
                "expected the surface to be at least partly visible");
        return out;
    }

    @Test
    @DisplayName("fully inside the clip: untouched bounds, all four corners rounded")
    void insideClipKeepsEveryCorner() {
        assertArrayEquals(new int[] {100, 100, 200, 150, 20, 20, 20, 20},
                geom(100, 100, 200, 150, 20, 0, 0, 1000, 1000));
    }

    @Test
    @DisplayName("a card half off the left of its viewport keeps only its right corners")
    void cutOnTheLeftSquaresTheLeftCorners() {
        // The viewport starts at x=200; the card runs 100..300, so its left half is gone.
        assertArrayEquals(new int[] {200, 100, 100, 150, 0, 20, 20, 0},
                geom(100, 100, 200, 150, 20, 200, 0, 800, 1000));
    }

    @Test
    @DisplayName("a card half off the right of its viewport keeps only its left corners")
    void cutOnTheRightSquaresTheRightCorners() {
        assertArrayEquals(new int[] {100, 100, 100, 150, 20, 0, 0, 20},
                geom(100, 100, 200, 150, 20, 0, 0, 200, 1000));
    }

    @Test
    @DisplayName("a card cut top and bottom keeps no corners but still clips to the band")
    void cutOnBothAxesSquaresEverything() {
        assertArrayEquals(new int[] {100, 120, 200, 100, 0, 0, 0, 0},
                geom(100, 100, 200, 150, 20, 0, 120, 1000, 100));
    }

    @Test
    @DisplayName("the surviving radius never exceeds half the visible box")
    void radiusIsClampedToTheVisibleSliver() {
        // Only 24px of the card's right edge is left, so a 20px corner would overlap itself.
        int[] q = geom(100, 100, 200, 150, 20, 276, 0, 800, 1000);
        assertArrayEquals(new int[] {276, 100, 24, 150}, new int[] {q[0], q[1], q[2], q[3]});
        assertArrayEquals(new int[] {0, 12, 12, 0}, new int[] {q[4], q[5], q[6], q[7]});
    }

    @Test
    @DisplayName("scrolled entirely out of the viewport: nothing to paint")
    void offscreenReportsNothingVisible() {
        assertFalse(MaterialRenderElement.clipGeometry(new int[8],
                100, 100, 200, 150, 20, 400, 0, 300, 1000));
        // Touching edges only - an empty intersection, not a one-pixel sliver.
        assertFalse(MaterialRenderElement.clipGeometry(new int[8],
                100, 100, 200, 150, 20, 300, 0, 300, 1000));
    }

    @Test
    @DisplayName("the clip path is a POLYGON, which is what keeps corners round on a GPU")
    void clipPathReducesToAPolygon() {
        // Not a stylistic preference. The ports ask isPolygon() to decide how to hand a clip
        // to the GPU: Codename One's iOS backend renders a polygon through a stencil, and a
        // shape it cannot reduce to one falls back to the BOUNDING BOX - a square-cornered
        // card. Built with quadTo this returned false, and the study card's corners were
        // square on iOS while correct on the desktop simulator for exactly that reason.
        com.codename1.ui.geom.GeneralPath p = (com.codename1.ui.geom.GeneralPath)
                new MaterialRenderElement(new Material())
                        .clipShape(100, 100, 300, 200, 30, 30, 30, 30);
        assertTrue(p.isPolygon(), "clip must reduce to a polygon");
        assertFalse(p.isRectangle(), "a rounded clip is not a rectangle");
        assertEquals(new com.codename1.ui.geom.Rectangle(100, 100, 300, 200).toString(),
                p.getBounds().toString(), "rounding must not move the bounds");

        // And it must still be a polygon after a child clips to its own rect, which is what
        // every component in the subtree does on its way down.
        p.intersect(new com.codename1.ui.geom.Rectangle(100, 100, 300, 200));
        assertTrue(p.isPolygon(), "clip must survive a child's clipRect as a polygon");
    }

    @Test
    @DisplayName("a fully squared clip really is a plain rectangle")
    void squaredClipIsARectangle() {
        com.codename1.ui.geom.GeneralPath p = (com.codename1.ui.geom.GeneralPath)
                new MaterialRenderElement(new Material())
                        .clipShape(100, 100, 300, 200, 0, 0, 0, 0);
        assertTrue(p.isRectangle(), "no corners rounded means a rectangle, and the cheap path");
    }

    @Test
    @DisplayName("the clip is a genuine intersection, never wider than what it inherited")
    void neverPaintsOutsideTheInheritedClip() {
        // The bug this guards: setClip(Shape) REPLACES the clip, so a surface that ignored
        // the incoming one would paint its rounded rect over its neighbours.
        int[] q = geom(100, 100, 200, 150, 20, 150, 130, 100, 60);
        assertTrue(q[0] >= 150 && q[1] >= 130, "origin escaped the inherited clip");
        assertTrue(q[0] + q[2] <= 250 && q[1] + q[3] <= 190, "extent escaped the inherited clip");
    }

    private static int[] corners(int rtl, int rtr, int rbr, int rbl,
            int cx, int cy, int cw, int ch) {
        int[] out = new int[8];
        assertTrue(MaterialRenderElement.clipGeometry(out, 100, 100, 200, 150,
                rtl, rtr, rbr, rbl, cx, cy, cw, ch), "expected something visible");
        return out;
    }

    /// Flutter rounds corners independently, and the gallery's settings button rounds
    /// exactly one of them: BorderRadiusDirectional.only(bottomStart: 10). Collapsing
    /// that to a single radius -- the top-left one, which is zero -- is what drew it as
    /// a plain white block where the reference has a rounded bottom-left corner.
    @Test
    void oneRoundedCornerRoundsOnlyThatCorner() {
        assertArrayEquals(new int[] {100, 100, 200, 150, 0, 0, 0, 30},
                corners(0, 0, 0, 30, 0, 0, 1000, 1000));
    }

    @Test
    void everyCornerKeepsItsOwnRadius() {
        assertArrayEquals(new int[] {100, 100, 200, 150, 4, 8, 12, 16},
                corners(4, 8, 12, 16, 0, 0, 1000, 1000));
    }

    /// A clip that cuts an edge squares BOTH corners on it, whatever they asked for.
    @Test
    void aCutEdgeSquaresItsOwnCornersOnly() {
        // clipped away on the left: the two left corners go, the right two survive.
        assertArrayEquals(new int[] {200, 100, 100, 150, 0, 8, 12, 0},
                corners(4, 8, 12, 16, 200, 0, 1000, 1000));
    }

    /// Each corner is capped independently by the visible box, not by the largest.
    @Test
    void eachCornerIsCappedByTheVisibleBox() {
        int[] q = corners(200, 200, 10, 10, 0, 0, 1000, 1000);
        // half the shorter visible side is 75
        assertArrayEquals(new int[] {75, 75, 10, 10}, new int[] {q[4], q[5], q[6], q[7]});
    }

    /// The single-radius form still means what it used to.
    @Test
    void theUniformFormIsUnchanged() {
        int[] four = new int[8];
        int[] one = new int[8];
        MaterialRenderElement.clipGeometry(four, 100, 100, 200, 150, 20, 20, 20, 20,
                0, 0, 1000, 1000);
        MaterialRenderElement.clipGeometry(one, 100, 100, 200, 150, 20, 0, 0, 1000, 1000);
        assertArrayEquals(four, one);
    }
}
