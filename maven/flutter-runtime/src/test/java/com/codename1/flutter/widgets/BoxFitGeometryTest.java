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

import com.codename1.flutter.BoxFit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BoxFit's arithmetic, pinned. The runtime draws artwork into its box at paint
 * time rather than keeping a scaled copy of it, so this maths IS the rendering:
 * get it wrong and the image is cropped, stretched or centred incorrectly with
 * nothing to report.
 */
class BoxFitGeometryTest {

    /** A 200x100 image in a 100x100 box. */
    private static double[] wide(BoxFit fit) {
        return ImageRenderElement.fittedSize(fit, 100, 100, 200, 100);
    }

    @Test
    void fillTakesTheWholeBox() {
        double[] r = wide(BoxFit.fill);
        assertEquals(100, r[0]);
        assertEquals(100, r[1]);
    }

    @Test
    void containFitsEntirelyInside() {
        double[] r = wide(BoxFit.contain);
        assertEquals(100, r[0]);
        assertEquals(50, r[1]);
    }

    @Test
    void coverOverflowsTheBox() {
        double[] r = wide(BoxFit.cover);
        assertEquals(200, r[0]);
        assertEquals(100, r[1]);
        // The point of cover: it is never SMALLER than the box in either axis,
        // which is what lets the component's own clip crop it.
        org.junit.jupiter.api.Assertions.assertTrue(r[0] >= 100 && r[1] >= 100);
    }

    @Test
    void fitWidthAndFitHeightPinOneAxis() {
        double[] w = wide(BoxFit.fitWidth);
        assertEquals(100, w[0]);
        assertEquals(50, w[1]);
        double[] h = wide(BoxFit.fitHeight);
        assertEquals(200, h[0]);
        assertEquals(100, h[1]);
    }

    @Test
    void noneKeepsTheNaturalSize() {
        double[] r = wide(BoxFit.none);
        assertEquals(200, r[0]);
        assertEquals(100, r[1]);
    }

    /** A 40x20 image -- SMALLER than the box -- in that same 100x100 box. */
    private static double[] small(BoxFit fit) {
        return ImageRenderElement.fittedSize(fit, 100, 100, 40, 20);
    }

    @Test
    void scaleDownShrinksAnOversizedPictureLikeContain() {
        double[] r = wide(BoxFit.scaleDown);
        assertEquals(100, r[0]);
        assertEquals(50, r[1]);
    }

    @Test
    void scaleDownNeverEnlargesAnUndersizedPicture() {
        // The difference from `contain`, and the whole point of the fit:
        // contain would blow this up to 100x50 to fill the box.
        double[] r = small(BoxFit.scaleDown);
        assertEquals(40, r[0]);
        assertEquals(20, r[1]);
        assertEquals(100, small(BoxFit.contain)[0]);
    }

    @Test
    void aNullFitIsScaleDownNotContain() {
        // Flutter's paintImage does `fit ??= BoxFit.scaleDown`, so a widget that
        // names no fit never enlarges its artwork. Reading the default as
        // `contain` drew the reply study's 200px attachment thumbnails at 432px.
        double[] r = small(null);
        assertEquals(40, r[0]);
        assertEquals(20, r[1]);
    }

    @Test
    void decodeHintsBoundTheDecodedSizeWithoutEnlargingIt() {
        // One hint keeps the aspect ratio...
        double[] w = ImageRenderElement.decodeHinted(1000, 500, Long.valueOf(200), null);
        assertEquals(200, w[0]);
        assertEquals(100, w[1]);
        double[] h = ImageRenderElement.decodeHinted(1000, 500, null, Long.valueOf(100));
        assertEquals(200, h[0]);
        assertEquals(100, h[1]);
        // ...both do not, because dart:ui decodes to exactly the pair it is given.
        double[] both = ImageRenderElement.decodeHinted(1000, 500, Long.valueOf(200), Long.valueOf(200));
        assertEquals(200, both[0]);
        assertEquals(200, both[1]);
    }

    @Test
    void aDecodeHintLargerThanTheAssetIsIgnored() {
        // ResizeImage passes allowUpscaling: false, so a hint can only shrink.
        double[] r = ImageRenderElement.decodeHinted(100, 50, Long.valueOf(4000), null);
        assertEquals(100, r[0]);
        assertEquals(50, r[1]);
    }

    @Test
    void noDecodeHintLeavesTheSizeAlone() {
        double[] r = ImageRenderElement.decodeHinted(1000, 500, null, null);
        assertEquals(1000, r[0]);
        assertEquals(500, r[1]);
    }
}
