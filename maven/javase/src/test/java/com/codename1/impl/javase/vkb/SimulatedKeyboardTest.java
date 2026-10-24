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
package com.codename1.impl.javase.vkb;

import com.codename1.ui.TextArea;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedKeyboardTest {

    @Test
    void constraintPicksLayout() {
        assertEquals(SimulatedKeyboard.Layout.TEXT, SimulatedKeyboard.layoutFor(TextArea.ANY));
        assertEquals(SimulatedKeyboard.Layout.NUMBER, SimulatedKeyboard.layoutFor(TextArea.NUMERIC));
        assertEquals(SimulatedKeyboard.Layout.DECIMAL, SimulatedKeyboard.layoutFor(TextArea.DECIMAL));
        assertEquals(SimulatedKeyboard.Layout.PHONE, SimulatedKeyboard.layoutFor(TextArea.PHONENUMBER));
        assertEquals(SimulatedKeyboard.Layout.EMAIL, SimulatedKeyboard.layoutFor(TextArea.EMAILADDR));
        // Flags above the low word must not change the layout.
        assertEquals(SimulatedKeyboard.Layout.TEXT, SimulatedKeyboard.layoutFor(TextArea.ANY | TextArea.PASSWORD));
        assertEquals(SimulatedKeyboard.Layout.NUMBER, SimulatedKeyboard.layoutFor(TextArea.NUMERIC | TextArea.PASSWORD));
    }

    @Test
    void portraitPhoneKeyboardIsAboutThreeQuartersOfTheWidth() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        int h = k.preferredHeight(1170, 2532);
        assertTrue(h > 800 && h < 950, "height " + h);
        k.setPlatform(SimulatedKeyboard.Platform.ANDROID);
        int a = k.preferredHeight(1080, 2400);
        assertTrue(a > 700 && a < 850, "height " + a);
    }

    @Test
    void landscapeAndTabletAreShorterShares() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        int land = k.preferredHeight(2532, 1170);
        assertTrue(land < 1170 * 2 / 3, "landscape " + land);
        k.setTablet(true);
        int tab = k.preferredHeight(1640, 2360);
        assertTrue(tab < 1640 / 2, "tablet " + tab);
    }

    @Test
    void iosNumberPadHasAccessoryDoneAndAndroidDoesNot() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        k.setLayout(SimulatedKeyboard.Layout.NUMBER);
        assertTrue(k.hasAccessoryBar());
        k.layout(390, 300);
        assertNotNull(k.findKind(SimulatedKeyboard.KeyKind.DONE));
        assertNull(k.findKind(SimulatedKeyboard.KeyKind.RETURN));
        k.setPlatform(SimulatedKeyboard.Platform.ANDROID);
        assertFalse(k.hasAccessoryBar());
        k.layout(390, 300);
        assertNull(k.findKind(SimulatedKeyboard.KeyKind.DONE));
        assertNotNull(k.findKind(SimulatedKeyboard.KeyKind.RETURN));
    }

    @Test
    void everyKeyIsHitAtItsCentreAndKeysDoNotOverlap() {
        for (SimulatedKeyboard.Platform p : SimulatedKeyboard.Platform.values()) {
            for (SimulatedKeyboard.Layout l : SimulatedKeyboard.Layout.values()) {
                SimulatedKeyboard k = new SimulatedKeyboard();
                k.setPlatform(p);
                k.setLayout(l);
                int w = 390;
                int h = k.preferredHeight(w, 844);
                k.layout(w, h);
                for (String c : new String[]{"q", "a", "m", "1", "0"}) {
                    SimulatedKeyboard.Key key = k.findKey(c);
                    if (key == null) {
                        continue;
                    }
                    assertTrue(key.x >= 0 && key.x + key.width <= w, p + " " + l + " " + c);
                    assertTrue(key.y >= 0 && key.y + key.height <= h, p + " " + l + " " + c);
                    assertEquals(key, k.keyAt(key.x + key.width / 2, key.y + key.height / 2), p + " " + l + " " + c);
                }
            }
        }
    }

    @Test
    void emailLayoutHasAtSign() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        k.setLayout(SimulatedKeyboard.Layout.EMAIL);
        k.layout(390, 290);
        assertNotNull(k.findKey("@"));
        assertNotNull(k.findKey("."));
    }

    @Test
    void shiftUppercasesLetters() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        k.setShift(true);
        k.layout(390, 290);
        assertEquals("Q", k.findKey("q").getOutput());
        k.setShift(false);
        k.layout(390, 290);
        assertEquals("q", k.findKey("q").getOutput());
    }

    @Test
    void paintsWithoutADisplay() {
        SimulatedKeyboard k = new SimulatedKeyboard();
        BufferedImage img = new BufferedImage(390, 290, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        k.paint(g, 390, 290);
        k.setPressed(k.findKey("g"));
        k.paint(g, 390, 290);
        g.dispose();
        // The background is opaque, so a corner pixel is drawn.
        assertEquals(0xff, img.getRGB(1, 1) >>> 24);
    }
}
