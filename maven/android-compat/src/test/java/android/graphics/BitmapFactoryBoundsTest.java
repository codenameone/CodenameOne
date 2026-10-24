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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// `Options.outWidth`/`outHeight` are written where AOSP's decoder writes
/// them: after `inSampleSize`, before the density scaling. A bounds probe
/// used to report the density-scaled size, so a two-pass sizing flow
/// computed its sample size from the wrong numbers.
public class BitmapFactoryBoundsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        HeadlessImplementation.pixelImages = true;
        AndroidTestSupport.context();
    }

    @After
    public void reset() {
        HeadlessImplementation.pixelImages = false;
    }

    private static byte[] png(int w, int h) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static BitmapFactory.Options doubling() {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inDensity = 160;
        o.inTargetDensity = 320;
        return o;
    }

    @Test
    public void boundsProbeIgnoresDensityScaling() throws Exception {
        byte[] data = png(12, 6);
        BitmapFactory.Options o = doubling();
        o.inJustDecodeBounds = true;
        assertNull(BitmapFactory.decodeByteArray(data, 0, data.length, o));
        assertEquals(12, o.outWidth);
        assertEquals(6, o.outHeight);
    }

    @Test
    public void boundsProbeAppliesSampleSizeAsGiven() throws Exception {
        byte[] data = png(12, 6);
        BitmapFactory.Options o = doubling();
        o.inJustDecodeBounds = true;
        o.inSampleSize = 3;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        assertEquals(4, o.outWidth);
        assertEquals(2, o.outHeight);
    }

    @Test
    public void fullDecodeStillScalesTheBitmap() throws Exception {
        byte[] data = png(12, 6);
        BitmapFactory.Options o = doubling();
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        assertEquals(24, b.getWidth());
        assertEquals(12, b.getHeight());
        assertEquals(12, o.outWidth);
        assertEquals(6, o.outHeight);
    }
}
