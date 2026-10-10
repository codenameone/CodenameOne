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
package com.codename1.ui.geom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A path is equal to a rectangle only when it IS that rectangle.
 *
 * <p>This used to compare bounding boxes, so every non-rectangular path compared equal to
 * its own bounding box. The clip bookkeeping asks exactly this question before deciding a
 * setClip is a no-op, and Codename One narrows the clip to a component's bounds before
 * painting it -- so a shaped clip filling that component arrived with bounds equal to the
 * current rectangular clip, compared EQUAL, and was DISCARDED. The subtree then painted
 * square with nothing reported.</p>
 */
class GeneralPathRectangleEqualityTest {

    /** A 64-sided polygon inscribed in (x, y, w, h) -- a circle for these purposes. */
    private static GeneralPath circle(float x, float y, float w, float h) {
        GeneralPath p = new GeneralPath();
        float rx = w / 2f;
        float ry = h / 2f;
        float cx = x + rx;
        float cy = y + ry;
        for (int i = 0; i < 64; i++) {
            double a = 2 * Math.PI * i / 64;
            float px = (float) (cx + rx * Math.cos(a));
            float py = (float) (cy + ry * Math.sin(a));
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
        return p;
    }

    private static GeneralPath rectPath(float x, float y, float w, float h) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w, y + h);
        p.lineTo(x, y + h);
        p.closePath();
        return p;
    }

    @Test
    void aCircleIsNotItsBoundingRectangle() {
        GeneralPath c = circle(10, 20, 100, 100);
        assertFalse(c.isRectangle(), "a 64-gon is not a rectangle");
        Rectangle bounds = new Rectangle();
        c.getBounds(bounds);
        assertFalse(c.equals(bounds, null),
                "a circle must not compare equal to the rectangle it is inscribed in");
    }

    @Test
    void aTriangleIsNotItsBoundingRectangle() {
        GeneralPath t = new GeneralPath();
        t.moveTo(50f, 0f);
        t.lineTo(100f, 100f);
        t.lineTo(0f, 100f);
        t.closePath();
        Rectangle bounds = new Rectangle();
        t.getBounds(bounds);
        assertFalse(t.equals(bounds, null));
    }

    /// The case the comparison exists for still answers yes.
    @Test
    void aRectangularPathStillEqualsThatRectangle() {
        GeneralPath r = rectPath(10, 20, 100, 50);
        assertTrue(r.isRectangle());
        assertTrue(r.equals(new Rectangle(10, 20, 100, 50), null));
    }

    @Test
    void aRectangularPathDoesNotEqualADifferentRectangle() {
        GeneralPath r = rectPath(10, 20, 100, 50);
        assertFalse(r.equals(new Rectangle(10, 20, 100, 51), null));
    }
}
