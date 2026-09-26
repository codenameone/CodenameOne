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
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A CustomPainter keeps the ground an ancestor Transform shifts it onto.
 *
 * <p>Flutter does not clip a CustomPainter to the box it was handed -- an
 * ancestor ClipRect does that -- so a painter may draw well outside its own
 * size. Codename One clips every component to its bounds, and when an ancestor
 * Transform has moved the origin those bounds move with it, so the part of the
 * drawing the shift brings into view is cut off instead. The 2D-transformations
 * demo centres a board wider than the screen by translating it, and lost a strip
 * down the right-hand side exactly as wide as the shift.</p>
 */
class PainterClipTest {

    /** The demo's real numbers: a 1029-wide surface at x=48, shifted 192 left. */
    @Test
    void aShiftedSurfaceClipsToWhereItIsOnScreen() {
        int[] box = CustomPaintRenderElement.onScreenClip(
                -144, 468, 0, 0, 48, 468, 1029, 1651);
        // Translated space, so screen x 48..1077 is 192..1221 here.
        assertArrayEquals(new int[] {192, 0, 1029, 1651}, box);
    }

    @Test
    void anUnshiftedSurfaceIsLeftAlone() {
        // translate + parent-relative == absolute, so no transform is in play
        // and the clip already in force is the right one.
        assertNull(CustomPaintRenderElement.onScreenClip(
                48, 468, 0, 0, 48, 468, 1029, 1651));
    }

    @Test
    void aVerticalShiftIsUndoneToo() {
        int[] box = CustomPaintRenderElement.onScreenClip(
                48, 300, 0, 0, 48, 468, 100, 200);
        assertArrayEquals(new int[] {0, 168, 100, 200}, box);
    }
}
