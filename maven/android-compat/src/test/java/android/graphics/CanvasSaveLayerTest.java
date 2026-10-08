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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/// A translucent `saveLayer` composites the finished layer once at its
/// alpha, as Android does, rather than applying the alpha to every draw
/// inside it -- which made overlapping draws darker where they overlap.
public class CanvasSaveLayerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void stopRecording() {
        HeadlessImplementation.recordDraws = false;
        HeadlessImplementation.draws.clear();
    }

    private static Graphics recordingGraphics() {
        AndroidTestSupport.context();
        HeadlessImplementation.recordDraws = true;
        HeadlessImplementation.draws.clear();
        Graphics g = Image.createImage(200, 200).getGraphics();
        g.setAlpha(255);
        return g;
    }

    @Test
    public void overlappingDrawsInATranslucentLayerAreCompositedOnce() {
        Graphics g = recordingGraphics();
        Canvas c = new Canvas(g, 0, 0, 200, 200);
        Paint layerPaint = new Paint();
        layerPaint.setAlpha(128);
        int count = c.saveLayer(null, layerPaint);
        Paint red = new Paint();
        red.setColor(0xffff0000);
        c.drawRect(10, 10, 60, 60, red);
        c.drawRect(40, 40, 90, 90, red);
        assertNotSame("the layer draws offscreen", g, c.getGraphics());
        c.restoreToCount(count);
        assertSame("restore returns to the view's graphics", g, c.getGraphics());

        List<Object[]> draws = HeadlessImplementation.draws;
        assertEquals(3, draws.size());
        for (int i = 0; i < 2; i++) {
            assertEquals("fillRect", draws.get(i)[0]);
            assertEquals("draws inside the layer are opaque", 255, ((Integer) draws.get(i)[2]).intValue());
        }
        assertEquals("the layer is drawn back once", "drawImage", draws.get(2)[0]);
        assertEquals("at the layer's alpha", 128, ((Integer) draws.get(2)[2]).intValue());
        assertNotSame("into another context than the shapes", draws.get(0)[1], draws.get(2)[1]);
        assertEquals("the view's alpha is restored", 255, g.getAlpha());
    }

    @Test
    public void anOpaqueLayerDrawsDirectly() {
        Graphics g = recordingGraphics();
        Canvas c = new Canvas(g, 0, 0, 200, 200);
        int count = c.saveLayer(null, new Paint());
        assertSame(g, c.getGraphics());
        c.restoreToCount(count);
        assertSame(g, c.getGraphics());
    }
}
