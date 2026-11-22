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
package com.codename1.gpu;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuImageTest extends UITestBase {
    private static final class Probe extends GpuImage {
        int fallbacks;
        int frames;
        int[] last;
        boolean gpu = true;

        @Override
        public int getWidth() {
            return 20;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        protected boolean usesGpu() {
            return gpu;
        }

        @Override
        protected Renderer createRenderer() {
            throw new AssertionError("no surface without a GPU");
        }

        @Override
        protected void prepareFrame(int sw, int sh, int ox, int oy, int w, int h) {
            frames++;
        }

        @Override
        protected void drawFallback(Graphics g, int x, int y, int w, int h) {
            fallbacks++;
            last = new int[]{x, y, w, h};
        }
    }

    private static int[] rect(int ax, int ay, int w, int h, int cx, int cy, int cw, int ch, int[] prev) {
        int[] out = new int[4];
        return GpuImage.visibleRect(ax, ay, w, h, cx, cy, cw, ch, prev, out) ? out : null;
    }

    @Test
    void wholeImageInsideTheClip() {
        assertArrayEquals(new int[]{10, 20, 30, 40}, rect(10, 20, 30, 40, 0, 0, 500, 500, null));
    }

    @Test
    void scrolledPartlyOutOfTheViewport() {
        // a scrolling parent shows y 100..200; the image spans 80..120
        assertArrayEquals(new int[]{5, 100, 30, 20}, rect(5, 80, 30, 40, 0, 100, 300, 100, null));
    }

    @Test
    void entirelyOutsideTheClipIsNotVisible() {
        assertEquals(null, rect(5, 300, 30, 40, 0, 100, 300, 100, null));
    }

    @Test
    void partialRepaintOfAStillImageDoesNotShrinkTheSurface() {
        int[] prev = {10, 20, 30, 40};
        // a repaint of the image's top left corner only
        assertArrayEquals(new int[]{10, 20, 30, 40}, rect(10, 20, 30, 40, 0, 0, 15, 25, prev));
        // a repaint missing the image keeps the surface where it was
        assertArrayEquals(new int[]{10, 20, 30, 40}, rect(10, 20, 30, 40, 200, 200, 5, 5, prev));
    }

    @Test
    void previousAreaIsBoundedByTheImage() {
        int[] prev = {0, 0, 100, 100};
        assertArrayEquals(new int[]{10, 20, 30, 40}, rect(10, 20, 30, 40, 0, 0, 500, 500, prev));
    }

    @Test
    void imageGraphicsIsNotTheScreen() {
        Image target = Image.createImage(50, 50);
        assertFalse(target.getGraphics().isScreenGraphics());
    }

    @Test
    void drawsThroughGraphicsWithoutAGpu() {
        assertFalse(com.codename1.ui.Display.getInstance().isGpuSupported());
        Probe p = new Probe();
        Graphics g = Image.createImage(50, 50).getGraphics();
        g.drawImage(p, 3, 4);
        assertEquals(1, p.fallbacks);
        assertArrayEquals(new int[]{3, 4, 20, 10}, p.last);
        assertEquals(0, p.frames);
        assertFalse(p.isOnGpuSurface());
        assertEquals(0, p.getFramesRendered());
    }

    @Test
    void declinedGpuDrawsThroughGraphics() {
        Probe p = new Probe();
        p.gpu = false;
        Graphics g = Image.createImage(50, 50).getGraphics();
        g.drawImage(p, 0, 0);
        assertEquals(1, p.fallbacks);
        assertArrayEquals(new int[]{0, 0, 20, 10}, p.last);
        assertTrue(p.requiresDrawImage());
    }
}
