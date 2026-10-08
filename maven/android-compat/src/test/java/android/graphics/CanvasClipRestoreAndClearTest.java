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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// A `clipPath` clip survives a save/restore pair as its shape, and
/// `drawColor(..., CLEAR)` erases a bitmap's clip.
public class CanvasClipRestoreAndClearTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void reset() {
        HeadlessImplementation.trackClip = false;
        HeadlessImplementation.shapeClip = null;
        HeadlessImplementation.clearedRects.clear();
    }

    @Test
    public void restoreReinstallsThePathClipNotItsBounds() {
        AndroidTestSupport.context();
        HeadlessImplementation.trackClip = true;
        Graphics g = Image.createImage(200, 200).getGraphics();
        g.setClip(0, 0, 200, 200);
        Canvas c = new Canvas(g, 0, 0, 200, 200);
        Path circle = new Path();
        circle.addCircle(50, 50, 40, Path.Direction.CW);
        c.clipPath(circle);
        com.codename1.ui.geom.Shape installed = HeadlessImplementation.shapeClip;
        int saved = c.save();
        c.clipRect(0, 0, 30, 30);
        assertNull("a rectangle replaced the shape", HeadlessImplementation.shapeClip);
        c.restoreToCount(saved);
        assertSame("the circle is the clip again", installed, HeadlessImplementation.shapeClip);
    }

    @Test
    public void restoreAfterAPathClipGoesBackToTheRectangle() {
        AndroidTestSupport.context();
        HeadlessImplementation.trackClip = true;
        Graphics g = Image.createImage(200, 200).getGraphics();
        g.setClip(0, 0, 200, 200);
        Canvas c = new Canvas(g, 0, 0, 200, 200);
        c.save();
        Path circle = new Path();
        circle.addCircle(50, 50, 40, Path.Direction.CW);
        c.clipPath(circle);
        c.restore();
        assertNull(HeadlessImplementation.shapeClip);
        assertEquals(200, g.getClipWidth());
    }

    @Test
    public void clearModeErasesABitmapsClip() {
        AndroidTestSupport.context();
        HeadlessImplementation.trackClip = true;
        HeadlessImplementation.clearedRects.clear();
        Bitmap b = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.getGraphics().setClip(0, 0, 40, 30);
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        assertEquals(1, HeadlessImplementation.clearedRects.size());
        assertArrayEquals(new int[]{0, 0, 40, 30}, HeadlessImplementation.clearedRects.get(0));
    }

    @Test
    public void clearModeOnAViewCanvasLeavesTheParentsPixels() {
        AndroidTestSupport.context();
        HeadlessImplementation.clearedRects.clear();
        Graphics g = Image.createImage(40, 30).getGraphics();
        Canvas c = new Canvas(g, 0, 0, 40, 30);
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        assertEquals(0, HeadlessImplementation.clearedRects.size());
    }
}
