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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class ColorPickerTest extends UITestBase {

    @FormTest
    void hsvRoundTrips() {
        int[] colors = {0xff0000, 0x00ff00, 0x0000ff, 0x3b82f6, 0x123456, 0xffffff, 0x000000, 0x808080};
        for (int c : colors) {
            float[] hsv = ColorPicker.rgbToHsv(c);
            assertEquals(c, ColorPicker.hsvToRgb(hsv[0], hsv[1], hsv[2]), "colour " + Integer.toHexString(c));
        }
        assertEquals(120f, ColorPicker.rgbToHsv(0x00ff00)[0], 0.01f);
    }

    @FormTest
    void hexIsUpperCaseSixDigits() {
        assertEquals("#00A0FF", ColorPicker.toHex(0x00a0ff));
        assertEquals("#000000", ColorPicker.toHex(0));
    }

    @FormTest
    void greyKeepsTheHue() {
        ColorPicker p = new ColorPicker(0x00ff00);
        float hue = p.getHue();
        p.setColor(0x808080);
        assertEquals(hue, p.getHue(), 0.01f);
        assertEquals(0x808080, p.getColor());
    }

    @FormTest
    void presetsAreCopied() {
        ColorPicker p = new ColorPicker();
        p.setPresets(0xff112233, 0x445566);
        int[] got = p.getPresets();
        assertArrayEquals(new int[] {0x112233, 0x445566}, got);
        got[0] = 0;
        assertEquals(0x112233, p.getPresets()[0]);
    }
}
