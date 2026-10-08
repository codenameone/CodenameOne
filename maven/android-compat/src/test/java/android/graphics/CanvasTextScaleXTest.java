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
package android.graphics;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `measureText` includes `textScaleX`, so the drawn run must cover the same
/// width. It used to be drawn at its natural width, and right-aligned or
/// centered text ended short of the point it was aligned to. The headless
/// graphics cannot transform, so this covers the path that scales the
/// advances; where the graphics can, the glyphs are stretched by the same
/// factor.
public class CanvasTextScaleXTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopRecording() {
        HeadlessImplementation.recordText = false;
        HeadlessImplementation.drawnText.clear();
    }

    private static Canvas canvas() {
        AndroidTestSupport.context();
        Graphics g = Image.createImage(400, 200).getGraphics();
        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        return new Canvas(g, 0, 0, 400, 200);
    }

    @Test
    public void glyphsAdvanceByTheScaledWidth() {
        Canvas c = canvas();
        Paint p = new Paint();
        p.setTextSize(20);
        p.setTextScaleX(2);
        int cw = p.cn1Font().charWidth('a');
        c.drawText("abc", 10, 50, p);
        assertEquals(3, HeadlessImplementation.drawnText.size());
        for (int i = 0; i < 3; i++) {
            Object[] call = HeadlessImplementation.drawnText.get(i);
            assertEquals(String.valueOf("abc".charAt(i)), call[0]);
            assertEquals(10 + 2 * cw * i, ((Integer) call[1]).intValue());
        }
    }

    @Test
    public void rightAlignedTextEndsAtItsAnchor() {
        Canvas c = canvas();
        Paint p = new Paint();
        p.setTextSize(20);
        p.setTextScaleX(1.5f);
        p.setTextAlign(Paint.Align.RIGHT);
        float w = p.measureText("abcd");
        int cw = p.cn1Font().charWidth('d');
        c.drawText("abcd", 300, 50, p);
        Object[] first = HeadlessImplementation.drawnText.get(0);
        assertEquals(300 - w, ((Integer) first[1]).intValue(), 1f);
        Object[] last = HeadlessImplementation.drawnText.get(HeadlessImplementation.drawnText.size() - 1);
        assertEquals("d", last[0]);
        // The last glyph's scaled advance ends at the anchor.
        assertEquals(300, ((Integer) last[1]).intValue() + cw * 1.5f, 1f);
    }
}
