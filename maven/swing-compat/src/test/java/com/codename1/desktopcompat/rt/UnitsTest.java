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

import org.junit.After;
import org.junit.Test;

/// The conversion between logical pixels and device pixels.
public class UnitsTest {

    @After
    public void reset() {
        Units.setScale(0);
    }

    @Test
    public void scaleIsOneWithoutADisplayOrOverride() {
        Units.setScale(0);
        // No display is initialised in this class unless another test class
        // ran first in the same JVM, so only the override is asserted.
        Units.setScale(1f);
        assertEquals(1f, Units.scale(), 0f);
        assertEquals(17, Units.toDevice(17));
    }

    @Test
    public void positionsRoundToTheNearestDevicePixel() {
        Units.setScale(1.5f);
        assertEquals(0, Units.toDevice(0));
        assertEquals(2, Units.toDevice(1));
        assertEquals(3, Units.toDevice(2));
        assertEquals(15, Units.toDevice(10));
    }

    @Test
    public void adjacentRectanglesStayAdjacent() {
        float[] scales = {1f, 1.25f, 1.5f, 2f, 2.625f, 3.3333333f};
        for (int s = 0; s < scales.length; s++) {
            Units.setScale(scales[s]);
            for (int x = 0; x < 60; x++) {
                for (int w = 0; w < 40; w++) {
                    assertEquals("scale " + scales[s] + " x " + x + " w " + w, Units.toDevice(x + w),
                            Units.toDevice(x) + Units.toDeviceSize(x, w));
                }
            }
        }
    }

    @Test
    public void logicalSizesRoundDownAndUp() {
        Units.setScale(2f);
        assertEquals(5, Units.toLogical(11));
        assertEquals(6, Units.toLogicalCeil(11));
        assertEquals(5, Units.toLogicalCeil(10));
        assertEquals(5.5f, Units.toLogicalExact(11), 0.0001f);
    }

    @Test
    public void aLogicalSizeFitsInTheDeviceSizeItCameFrom() {
        float[] scales = {1.25f, 1.5f, 2.625f, 3.3333333f};
        for (int s = 0; s < scales.length; s++) {
            Units.setScale(scales[s]);
            for (int device = 0; device < 2000; device += 7) {
                int logical = Units.toLogical(device);
                assertEquals(true, Units.toDevice(logical) <= device);
            }
        }
    }
}
