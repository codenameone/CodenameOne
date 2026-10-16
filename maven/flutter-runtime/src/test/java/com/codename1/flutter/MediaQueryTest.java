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
package com.codename1.flutter;

import com.codename1.flutter.rendering.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MediaQueryData reports LOGICAL pixels: device pixels divided by the
 * bucketed devicePixelRatio (Dp.scale()), matching Flutter.
 */
public class MediaQueryTest {

    @Test
    public void sizeIsDevicePixelsDividedByRatio() {
        MediaQueryData d = MediaQueryData.compute(1170, 2532, 3.0, Boolean.FALSE);
        Size s = d.size();
        assertEquals(390.0, s.width(), 0.001);
        assertEquals(844.0, s.height(), 0.001);
        assertEquals(3.0, d.devicePixelRatio(), 0.001);
        assertEquals(Brightness.light, d.platformBrightness());
    }

    @Test
    public void ratioOneIsIdentity() {
        MediaQueryData d = MediaQueryData.compute(800, 600, 1.0, null);
        assertEquals(800.0, d.size().width(), 0.001);
        assertEquals(600.0, d.size().height(), 0.001);
    }

    @Test
    public void darkModeFlagMapsToBrightness() {
        assertEquals(Brightness.dark,
                MediaQueryData.compute(100, 100, 2.0, Boolean.TRUE).platformBrightness());
        assertEquals(Brightness.light,
                MediaQueryData.compute(100, 100, 2.0, null).platformBrightness(),
                "unknown platform brightness defaults to light");
    }

    @Test
    public void nonPositiveScaleFallsBackToOne() {
        MediaQueryData d = MediaQueryData.compute(400, 400, 0, Boolean.FALSE);
        assertEquals(1.0, d.devicePixelRatio(), 0.001);
        assertEquals(400.0, d.size().width(), 0.001);
    }
}
