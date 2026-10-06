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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Every bitmap derived from another (copy, crop, scale, matrix transform,
/// alpha extraction) keeps the source's density, as on Android. They used
/// to fall back to the device density, so a density-neutral operation
/// changed the derived bitmap's scaled size.
public class BitmapDerivedDensityTest {

    private static final int DENSITY = 120;

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

    private static Bitmap source(boolean mutable) {
        Bitmap b = Bitmap.createBitmap(new int[8 * 4], 8, 4, Bitmap.Config.ARGB_8888);
        if (mutable) {
            b = b.copy(Bitmap.Config.ARGB_8888, true);
        }
        b.setDensity(DENSITY);
        return b;
    }

    @Test
    public void copiesKeepTheDensity() {
        assertEquals(DENSITY, source(false).copy(Bitmap.Config.ARGB_8888, true).getDensity());
        assertEquals(DENSITY, source(false).copy(Bitmap.Config.ARGB_8888, false).getDensity());
        assertEquals(DENSITY, Bitmap.createBitmap(source(true)).getDensity());
        assertEquals(DENSITY, Bitmap.createBitmap(source(true), 0, 0, 8, 4).getDensity());
    }

    @Test
    public void cropsAndScalesKeepTheDensity() {
        assertEquals(DENSITY, Bitmap.createBitmap(source(false), 1, 1, 4, 2).getDensity());
        assertEquals(DENSITY, Bitmap.createScaledBitmap(source(false), 16, 8, true).getDensity());
        Matrix scale = new Matrix();
        scale.setScale(2, 2);
        assertEquals(DENSITY, Bitmap.createBitmap(source(false), 0, 0, 8, 4, scale, true).getDensity());
        Matrix rotate = new Matrix();
        rotate.setRotate(90);
        assertEquals(DENSITY, Bitmap.createBitmap(source(false), 0, 0, 8, 4, rotate, false).getDensity());
    }

    @Test
    public void extractedAlphaKeepsTheDensity() {
        assertEquals(DENSITY, source(false).extractAlpha().getDensity());
    }
}
