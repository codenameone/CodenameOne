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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.ui.plaf.GlassLensBlend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The region ops (lensRegion, glassLensRegion, colorMatrixRegion) read and write
 * the buffer directly. They must still honour the graphics clip, as every other
 * paint does and as the Metal and JavaScript implementations do: pixels outside
 * the clip stay exactly as they were, and the clipped part still changes.
 */
public class JavaSEPortRegionClipTest {
    private static final int SIZE = 64;
    private static final int CLIP_X = 20;
    private static final int CLIP_Y = 18;
    private static final int CLIP_W = 22;
    private static final int CLIP_H = 24;

    private JavaSEPort port;
    private BufferedImage image;
    private int[] before;
    private Object graphics;
    private JavaSEPort originalInstance;

    @BeforeEach
    void setUp() {
        // new JavaSEPort() replaces the global JavaSEPort.instance, which later test
        // classes (CodenameOneExtensionTest) drive the live Display through; put the
        // original back afterwards, as JavaSEPortFontMappingTest does.
        originalInstance = JavaSEPort.instance;
        port = new JavaSEPort();
        image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                // A gradient, so a lens that shifts or magnifies changes pixels too.
                image.setRGB(x, y, 0xff000000 | ((x * 4) << 16) | ((y * 4) << 8) | 0x40);
            }
        }
        before = image.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
        graphics = port.getNativeGraphics(image);
        port.setClip(graphics, CLIP_X, CLIP_Y, CLIP_W, CLIP_H);
    }

    @AfterEach
    void tearDown() {
        JavaSEPort.instance = originalInstance;
    }

    @Test
    void colorMatrixRegionStaysInsideTheClip() {
        float[] invert = {-1, 0, 0, 1, 0, -1, 0, 1, 0, 0, -1, 1};
        assertTrue(port.colorMatrixRegion(graphics, 0, 0, SIZE, SIZE, invert, null, 0, 1f));
        assertClipped("colorMatrixRegion");
    }

    @Test
    void glassLensRegionStaysInsideTheClip() {
        float[] optics = new float[GlassLensBlend.COUNT];
        optics[GlassLensBlend.BEVEL] = 8;
        optics[GlassLensBlend.MAX_SHIFT] = 6;
        optics[GlassLensBlend.BRIGHTNESS] = 0.25f;
        assertTrue(port.glassLensRegion(graphics, 8, 8, SIZE - 16, SIZE - 16, -1, optics, 1f));
        assertClipped("glassLensRegion");
    }

    @Test
    void lensRegionStaysInsideTheClip() {
        assertTrue(port.lensRegion(graphics, 0, 0, SIZE, SIZE, 0, 1.6f, 0f, 0x0000ff, 1f));
        assertClipped("lensRegion");
    }

    private void assertClipped(String op) {
        int[] after = image.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
        int changedInside = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean inside = x >= CLIP_X && x < CLIP_X + CLIP_W && y >= CLIP_Y && y < CLIP_Y + CLIP_H;
                int i = y * SIZE + x;
                if (inside) {
                    if (after[i] != before[i]) {
                        changedInside++;
                    }
                } else {
                    assertEquals(before[i], after[i], op + " wrote outside the clip at " + x + "," + y);
                }
            }
        }
        assertTrue(changedInside > 0, op + " must still change the pixels inside the clip");
    }
}
