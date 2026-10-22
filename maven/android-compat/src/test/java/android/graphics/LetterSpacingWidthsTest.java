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
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `getTextWidths` and `breakText` use the same per-character advance as
/// `measureText` and `Canvas.drawText`, letter spacing included. They used to
/// leave the spacing out, so glyphs positioned from the widths overlapped and
/// `breakText` claimed more characters fit than are drawn in the space.
public class LetterSpacingWidthsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static Paint spaced() {
        AndroidTestSupport.context();
        Paint p = new Paint();
        p.setTextSize(20);
        p.setLetterSpacing(0.25f);
        return p;
    }

    @Test
    public void widthsSumToMeasuredWidth() {
        Paint p = spaced();
        String s = "Spaced";
        float[] widths = new float[s.length()];
        p.getTextWidths(s, widths);
        float sum = 0;
        for (float w : widths) {
            sum += w;
        }
        assertEquals(p.measureText(s), sum, 0.01f);
    }

    @Test
    public void breakTextCountsSpacedAdvances() {
        Paint p = spaced();
        String s = "iiiiiiiiii";
        float three = p.measureText(s, 0, 3);
        float[] measured = new float[1];
        // Room for three spaced characters and a little more, but not four.
        assertEquals(3, p.breakText(s, true, three + 1, measured));
        assertEquals(three, measured[0], 0.01f);
        assertEquals(3, p.breakText(s, false, three + 1, measured));
        assertEquals(three, measured[0], 0.01f);
    }
}
