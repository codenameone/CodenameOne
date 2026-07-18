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
package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;
import java.util.Hashtable;

public class DrawString extends AbstractGraphicsScreenshotTest {
    @Override
    public boolean runTest() {
        verifyFontHashing();
        return super.runTest();
    }

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g.setColor(0xffffff);
        int y = bounds.getY();
        // Emoji are embedded in the MIDDLE of each label, across the different
        // fonts/styles/colors below, so we verify the surrounding text does not
        // break around a supplementary code point. The system/native fonts have
        // no glyph for these code points, so the platform font-substitutes an
        // emoji font: on the iOS Metal renderer this used to rasterise the
        // substituted-run glyph ids against the base font and showed random CJK
        // ("Chinese") glyphs. Built from code points -- exactly as a user-
        // entered "U+1F3E0" value would be via Character.toChars -- to keep this
        // source ASCII.
        g.drawString("Default " + emoji(0x1F3E0) + " Font", bounds.getX(), y);
        y += g.getFont().getHeight();
        g.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString("Small " + emoji(0x1F389) + " Bold Monospace", bounds.getX(), y);
        y += g.getFont().getHeight();
        String[] ttfFonts = {"native:MainThin", "native:MainLight", "native:MainRegular",
                "native:MainBold", "native:MainBlack", "native:ItalicThin", "native:ItalicLight",
                "native:ItalicRegular", "native:ItalicBold", "native:ItalicBlack"};
        for(int i = 0; i < ttfFonts.length; i++) {
            String name = ttfFonts[i];
            g.setFont(Font.createTrueTypeFont(name, 4));
            // Inject an emoji mid-name into one bold and one italic native font
            // so substitution is exercised inside styled custom-font runs too.
            String label = name;
            if (i == 3) {
                label = insertMid(name, emoji(0x1F680));
            } else if (i == 8) {
                label = insertMid(name, emoji(0x1F600));
            }
            g.drawString(label, bounds.getX(), y);
            y += g.getFont().getHeight();
        }

        g.setColor(0xff0000);
        g.drawStringBaseline("Baseline " + emoji(0x1F525) + " and עברית",
                bounds.getX(), bounds.getY() + bounds.getHeight());
    }

    /// Builds the string for a single Unicode code point, mirroring the
    /// customer's "U+1F3E0" -> codePoint -> Character.toChars path. Kept here so
    /// no literal emoji appear in this source (ASCII-only).
    private static String emoji(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    /// Inserts {@code ins} at the middle of {@code s} so the label has text on
    /// both sides of the emoji.
    private static String insertMid(String s, String ins) {
        int mid = s.length() / 2;
        return s.substring(0, mid) + ins + s.substring(mid);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-string";
    }

    @Override
    protected long extraSettleBeforeCaptureMillis() {
        // Android can finish loading the native/emoji fallback run after the
        // mutable-image AA-on cell's first paint. Without a forced repaint the
        // screenshot occasionally preserves that transient frame only in the
        // final baseline label (white fallback text instead of settled red
        // italic text). Repaint after the normal form settle and let that paint
        // complete before capturing.
        return 1000;
    }

    private void verifyFontHashing() {
        if (!Font.isNativeFontSchemeSupported()) {
            return;
        }
        Font base = Font.createTrueTypeFont("native:MainRegular", 4);
        if (base == null) {
            return;
        }
        float derivedSize = base.getHeight() + 1;
        Font derived = base.derive(derivedSize, Font.STYLE_PLAIN);
        Hashtable<Font, String> cache = new Hashtable<Font, String>();
        int baseHashBeforePut = base.hashCode();
        cache.put(base, "base");
        cache.put(derived, "derived");
        Font baseCopy = Font.createTrueTypeFont("native:MainRegular", 4);
        assertTrue(base.equals(baseCopy), "Expected native font instances to be equal.");
        assertTrue(baseHashBeforePut == base.hashCode(),
                "Expected native font hashCode to remain stable.");
        assertTrue(base.hashCode() == baseCopy.hashCode(),
                "Expected equal native font instances to share hashCode.");
        assertTrue("base".equals(cache.get(baseCopy)),
                "Expected native font hash/equals to match Hashtable lookup.");
        Font derivedCopy = baseCopy.derive(derivedSize, Font.STYLE_PLAIN);
        assertTrue(derived.equals(derivedCopy), "Expected derived native font instances to be equal.");
        assertTrue(derived.hashCode() == derivedCopy.hashCode(),
                "Expected equal derived native font instances to share hashCode.");
        assertTrue("derived".equals(cache.get(derivedCopy)),
                "Expected derived font hash/equals to match Hashtable lookup.");
    }
}
