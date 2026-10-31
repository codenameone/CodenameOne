/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.UITimer;

import java.io.InputStream;

/// Font.createTrueTypeFontFromData on the device: a real .ttf from bytes measures and paints
/// with its own glyphs, and bytes that are not a font answer null. A browser loads the
/// FontFace asynchronously, so the measuring half polls until the face is in. Assertion-only.
public class TrueTypeFontFromDataTest extends BaseTest {
    // A Material icon codepoint: the default font has no glyph for it.
    private static final String SAMPLE = "\ue87c\ue87c\ue87c";
    private Font font;
    private Font plain;
    private int attempts;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            if (!Font.isTrueTypeStreamSupported()) {
                done();
                return true;
            }
            if (Font.createTrueTypeFontFromData("cn1ssNotAFont", new byte[64]) != null) {
                fail("bytes without an SFNT header must answer null");
                return false;
            }
            InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/material-design-font.ttf");
            if (in == null) {
                fail("material-design-font.ttf is not in the bundle");
                return false;
            }
            byte[] data = Util.readInputStream(in);
            Util.cleanup(in);
            Font base = Font.createTrueTypeFontFromData("cn1ssMaterialFromData", data);
            if (base == null) {
                fail("createTrueTypeFontFromData answered null for a real .ttf");
                return false;
            }
            font = base.derive(48, Font.STYLE_PLAIN);
            plain = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
            poll();
        } catch (Throwable t) {
            fail("unexpected " + t);
        }
        return true;
    }

    private void poll() {
        int w = font.stringWidth(SAMPLE);
        int inked = inkedPixels();
        System.out.println("CN1SS:TTFDATA width=" + w + " plainWidth=" + plain.stringWidth(SAMPLE)
                + " inked=" + inked + " attempt=" + attempts);
        if (w > 0 && w != plain.stringWidth(SAMPLE) && inked > 0) {
            done();
            return;
        }
        if (++attempts > 50) {
            fail("font from data never took effect: width=" + w + " inked=" + inked);
            return;
        }
        UITimer.timer(100, false, Display.getInstance().getCurrent(), new Runnable() {
            public void run() {
                poll();
            }
        });
    }

    private int inkedPixels() {
        Image img = Image.createImage(200, 80, 0xffffffff);
        Graphics g = img.getGraphics();
        g.setColor(0x000000);
        g.setFont(font);
        g.drawString(SAMPLE, 4, 4);
        int[] rgb = img.getRGB();
        int n = 0;
        for (int i = 0; i < rgb.length; i++) {
            if ((rgb[i] & 0xffffff) != 0xffffff) {
                n++;
            }
        }
        return n;
    }
}
