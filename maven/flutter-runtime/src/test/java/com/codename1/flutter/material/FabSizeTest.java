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

import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A floating action button is a fixed Material size, not whatever its glyph
 * happens to need.
 *
 * <p>The size came from the component's preferred size, which is the glyph plus
 * whatever padding the theme in force carries. With no theme entry for the UIID
 * that is 83 device pixels against the reference's 168 -- less than half.</p>
 */
class FabSizeTest {

    @Test
    void aRegularFabIsAFixedSquare() {
        Size s = FabRenderElement.materialSize(false, 12);
        assertEquals(Dp.px(56), s.width(), 0.001);
        assertEquals(Dp.px(56), s.height(), 0.001);
    }

    @Test
    void anExtendedFabFixesItsHeightAndFollowsItsLabel() {
        // Material 3 puts the extended form at 56 too, not Material 2's 48.
        Size s = FabRenderElement.materialSize(true, Dp.px(300));
        assertEquals(Dp.px(300), s.width(), 0.001);
        assertEquals(Dp.px(56), s.height(), 0.001);
    }

    @Test
    void anExtendedFabIsNeverNarrowerThanTheMinimum() {
        Size s = FabRenderElement.materialSize(true, 4);
        assertEquals(Dp.px(80), s.width(), 0.001);
    }
}
