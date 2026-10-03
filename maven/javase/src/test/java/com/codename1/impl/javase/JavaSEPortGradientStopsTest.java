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

import com.codename1.ui.LinearGradient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Equal adjacent gradient stops are how a hard color edge is written, and
/// Java2D's LinearGradientPaint rejects them ("Keyframe fractions must be
/// increasing"), which threw out of the paint of hellocodenameone's
/// DrawGradientStops.
public class JavaSEPortGradientStopsTest {
    private JavaSEPort originalInstance;

    @BeforeEach
    void setUp() {
        originalInstance = JavaSEPort.instance;
    }

    @AfterEach
    void tearDown() {
        JavaSEPort.instance = originalInstance;
    }

    @Test
    void hardStopFillsWithASharpEdge() {
        JavaSEPort port = new JavaSEPort();
        BufferedImage img = new BufferedImage(100, 10, BufferedImage.TYPE_INT_ARGB);
        Object graphics = port.getNativeGraphics(img);
        LinearGradient stripe = new LinearGradient(90f,
                new int[]{0xffff0000, 0xffff0000, 0xff0000ff, 0xff0000ff},
                new float[]{0f, 0.5f, 0.5f, 1f});
        port.fillGradient(graphics, stripe, 0, 0, 100, 10);
        assertEquals(0xffff0000, img.getRGB(10, 5));
        assertEquals(0xffff0000, img.getRGB(48, 5));
        assertEquals(0xff0000ff, img.getRGB(52, 5));
        assertEquals(0xff0000ff, img.getRGB(90, 5));
    }

    @Test
    void fractionsBecomeStrictlyIncreasingWithinTheUnitRange() {
        assertArrayEquals(new float[]{0f, 0.25f, 1f}, JavaSEPort.awtFractions(new float[]{0f, 0.25f, 1f}));
        float[] trailing = JavaSEPort.awtFractions(new float[]{0f, 1f, 1f, 1f});
        float[] leading = JavaSEPort.awtFractions(new float[]{0f, 0f, 0.5f});
        for (float[] f : new float[][]{trailing, leading}) {
            assertEquals(0f, f[0]);
            for (int i = 1; i < f.length; i++) {
                assertTrue(f[i] > f[i - 1], "strictly increasing at " + i);
                assertTrue(f[i] <= 1f, "within range at " + i);
            }
        }
        assertEquals(1f, trailing[trailing.length - 1]);
    }
}
