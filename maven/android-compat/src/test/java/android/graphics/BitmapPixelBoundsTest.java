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
import com.codename1.ui.Image;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/// Pixel coordinates are checked against their own dimension, as on Android.
/// The pixels are one flattened array, so `getPixel(width, 0)` used to return
/// the first pixel of the next row and a negative `x` one of the row before.
public class BitmapPixelBoundsTest {

    private static final int W = 4;
    private static final int H = 3;

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

    private static Bitmap bitmap() {
        AndroidTestSupport.context();
        int[] px = new int[W * H];
        for (int i = 0; i < px.length; i++) {
            px[i] = 0xff000000 | i;
        }
        return new Bitmap(Image.createImage(px, W, H), true, Bitmap.Config.ARGB_8888);
    }

    private interface Access {
        void run(Bitmap b);
    }

    private static void assertRefused(String what, Access access) {
        Bitmap b = bitmap();
        try {
            access.run(b);
            fail(what + " was not refused");
        } catch (IllegalArgumentException expected) {
            // as on Android
        }
        for (int i = 0; i < W * H; i++) {
            assertEquals(what + " wrote a pixel", 0xff000000 | i, b.getPixel(i % W, i / W));
        }
    }

    @Test
    public void inRangeAccessStillWorks() {
        Bitmap b = bitmap();
        assertEquals(0xff000000 | (W * H - 1), b.getPixel(W - 1, H - 1));
        int[] out = new int[W * H];
        b.getPixels(out, 0, W, 0, 0, W, H);
        assertEquals(0xff000000 | 5, out[5]);
    }

    @Test
    public void outOfRangeCoordinatesAreRefused() {
        assertRefused("getPixel(width, 0)", new Access() {
            public void run(Bitmap b) {
                b.getPixel(W, 0);
            }
        });
        assertRefused("getPixel(-1, 1)", new Access() {
            public void run(Bitmap b) {
                b.getPixel(-1, 1);
            }
        });
        assertRefused("setPixel(width, 0)", new Access() {
            public void run(Bitmap b) {
                b.setPixel(W, 0, 0);
            }
        });
        assertRefused("getPixels past the right edge", new Access() {
            public void run(Bitmap b) {
                b.getPixels(new int[W * H], 0, W, 1, 0, W, 1);
            }
        });
        assertRefused("setPixels past the right edge", new Access() {
            public void run(Bitmap b) {
                b.setPixels(new int[W * H], 0, W, 1, 0, W, 1);
            }
        });
    }
}
