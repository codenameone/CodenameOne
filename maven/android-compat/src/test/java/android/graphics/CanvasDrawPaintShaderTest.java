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

/// `drawPaint` honours the paint's shader the way `drawRect` does, instead
/// of filling the clip with the paint's plain color.
public class CanvasDrawPaintShaderTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopRecording() {
        HeadlessImplementation.recordDraws = false;
        HeadlessImplementation.draws.clear();
    }

    private static Canvas canvas() {
        AndroidTestSupport.context();
        HeadlessImplementation.recordDraws = true;
        HeadlessImplementation.draws.clear();
        Graphics g = Image.createImage(200, 200).getGraphics();
        return new Canvas(g, 0, 0, 200, 200);
    }

    @Test
    public void aGradientPaintFillsWithTheGradient() {
        Canvas c = canvas();
        Paint p = new Paint();
        // Android fills with drawPaint whatever the style.
        p.setStyle(Paint.Style.STROKE);
        p.setShader(new LinearGradient(0, 0, 0, 200, 0xffff0000, 0xff0000ff, Shader.TileMode.CLAMP));
        c.drawPaint(p);
        assertEquals(1, HeadlessImplementation.draws.size());
        assertEquals("fillLinearGradient", HeadlessImplementation.draws.get(0)[0]);
    }

    @Test
    public void aPlainPaintStillFillsTheClip() {
        Canvas c = canvas();
        Paint p = new Paint();
        p.setColor(0xff00ff00);
        c.drawPaint(p);
        assertEquals(1, HeadlessImplementation.draws.size());
        assertEquals("fillRect", HeadlessImplementation.draws.get(0)[0]);
    }
}
