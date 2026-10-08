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

/// Letter-spaced text is drawn a character at a time. A surrogate pair (an
/// emoji) used to be drawn as two lone surrogates with the spacing between
/// them; it is now one character, drawn, measured and broken as a unit.
public class CanvasSpacedSurrogateTest {

    private static final String EMOJI = new String(new char[]{(char) 0xD83D, (char) 0xDE00});

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopRecording() {
        HeadlessImplementation.recordText = false;
        HeadlessImplementation.drawnText.clear();
    }

    private static Paint spaced() {
        Paint p = new Paint();
        p.setTextSize(20);
        p.setLetterSpacing(0.5f);
        return p;
    }

    @Test
    public void aSurrogatePairIsDrawnWhole() {
        AndroidTestSupport.context();
        Graphics g = Image.createImage(400, 200).getGraphics();
        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        Canvas c = new Canvas(g, 0, 0, 400, 200);
        Paint p = spaced();
        int cw = p.cn1Font().charWidth('a');
        float extra = 0.5f * 20;
        c.drawText("a" + EMOJI + "b", 10, 50, p);
        assertEquals(3, HeadlessImplementation.drawnText.size());
        assertEquals("a", HeadlessImplementation.drawnText.get(0)[0]);
        assertEquals(EMOJI, HeadlessImplementation.drawnText.get(1)[0]);
        assertEquals("b", HeadlessImplementation.drawnText.get(2)[0]);
        float pairWidth = p.cn1Font().stringWidth(EMOJI);
        int bx = ((Integer) HeadlessImplementation.drawnText.get(2)[1]).intValue();
        assertEquals(10 + cw + extra + pairWidth + extra, bx, 1f);
    }

    @Test
    public void measuringAgreesWithDrawing() {
        AndroidTestSupport.context();
        Paint p = spaced();
        int cw = p.cn1Font().charWidth('a');
        float pairWidth = p.cn1Font().stringWidth(EMOJI);
        assertEquals(cw + pairWidth + 2 * 10f, p.measureText("a" + EMOJI), 0.01f);
        float[] widths = new float[3];
        p.getTextWidths("a" + EMOJI, widths);
        assertEquals(pairWidth + 10f, widths[1], 0.01f);
        assertEquals(0f, widths[2], 0f);
    }

    @Test
    public void breakTextNeverSplitsAPair() {
        AndroidTestSupport.context();
        Paint p = spaced();
        int cw = p.cn1Font().charWidth('a');
        // Room for "a" and half the emoji: the pair must not be split.
        float max = cw + 10f + 1f;
        assertEquals(1, p.breakText("a" + EMOJI, true, max, null));
        assertEquals(0, p.breakText("a" + EMOJI, false, max, null));
    }
}
