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
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `createBitmap` with a matrix moves the pixels. It used to map only the
/// bounds and then scale the source into them, so a 90 degree rotation came
/// out with swapped dimensions but stretched, unrotated pixels.
public class BitmapMatrixTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void plainImages() {
        HeadlessImplementation.pixelImages = false;
    }

    /// A 2x3 bitmap whose pixel at (x, y) is 0xff000000 | (y * 2 + x).
    private static Bitmap source() {
        AndroidTestSupport.context();
        HeadlessImplementation.pixelImages = true;
        int[] px = new int[6];
        for (int i = 0; i < px.length; i++) {
            px[i] = 0xff000000 | i;
        }
        return Bitmap.createBitmap(px, 2, 3, Bitmap.Config.ARGB_8888);
    }

    @Test
    public void aQuarterTurnRotatesThePixels() {
        Bitmap src = source();
        Matrix m = new Matrix();
        m.setRotate(90);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, 2, 3, m, false);
        assertEquals(3, out.getWidth());
        assertEquals(2, out.getHeight());
        // Clockwise: output (x, y) shows source column y of row 2 - x.
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals("pixel " + x + "," + y, 0xff000000 | ((2 - x) * 2 + y), out.getPixel(x, y));
            }
        }
    }

    @Test
    public void aMirrorFlipsThePixels() {
        Bitmap src = source();
        Matrix m = new Matrix();
        m.setScale(-1, 1);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, 2, 3, m, true);
        assertEquals(2, out.getWidth());
        assertEquals(3, out.getHeight());
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                assertEquals("pixel " + x + "," + y, 0xff000000 | (y * 2 + 1 - x), out.getPixel(x, y));
            }
        }
    }
}
