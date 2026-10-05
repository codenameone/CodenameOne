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
import com.codename1.ui.Image;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Drawing through a `Canvas` keeps a bitmap's pixel reads current: a pixel
/// array cached by `getPixel` used to outlive the drawing, and pixels set
/// after the canvas was bound sent its drawing into a discarded image. The
/// headless port erases pixel images in `clearRect`, so a CLEAR draw is
/// what the tests observe.
public class CanvasBitmapPixelCacheTest {

    private static final int RED = 0xffff0000;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void pixels() {
        HeadlessImplementation.pixelImages = true;
    }

    @After
    public void reset() {
        HeadlessImplementation.pixelImages = false;
    }

    private static Bitmap redBitmap() {
        AndroidTestSupport.context();
        int[] px = new int[4 * 3];
        java.util.Arrays.fill(px, RED);
        return new Bitmap(Image.createImage(px, 4, 3), true, Bitmap.Config.ARGB_8888);
    }

    @Test
    public void drawingRefreshesCachedPixels() {
        Bitmap b = redBitmap();
        Canvas c = new Canvas(b);
        assertEquals(RED, b.getPixel(1, 1));
        c.drawColor(0, PorterDuff.Mode.CLEAR);
        assertEquals(0, b.getPixel(1, 1));
        // A later setPixel builds on the drawing, not on the stale cache.
        b.setPixel(0, 0, RED);
        assertEquals(RED, b.getPixel(0, 0));
        assertEquals(0, b.getPixel(2, 2));
    }

    @Test
    public void drawingAfterSetPixelLandsInTheBitmap() {
        Bitmap b = redBitmap();
        Canvas c = new Canvas(b);
        b.setPixel(0, 0, 0xff00ff00);
        c.drawColor(0, PorterDuff.Mode.CLEAR);
        assertEquals(0, b.getPixel(0, 0));
        assertEquals(0, b.getPixel(3, 2));
    }
}
