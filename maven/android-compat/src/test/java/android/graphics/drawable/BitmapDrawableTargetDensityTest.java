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
package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A bitmap drawable's intrinsic size is its bitmap's scaled from the
/// bitmap's density to the drawable's target density, as on Android.
/// `setTargetDensity` used to be ignored, so a bitmap made for another
/// density drew at its raw pixel size.
public class BitmapDrawableTargetDensityTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void intrinsicSizeFollowsTheTargetDensity() {
        Resources res = AndroidTestSupport.context().getResources();
        int device = res.getDisplayMetrics().densityDpi;
        // Pixel images, so the headless port reports the bitmap's size.
        HeadlessImplementation.pixelImages = true;
        Bitmap b;
        try {
            b = Bitmap.createBitmap(new int[10 * 20], 10, 20, Bitmap.Config.ARGB_8888);
        } finally {
            HeadlessImplementation.pixelImages = false;
        }
        // A bitmap starts at the device's density, so a drawable made with
        // the resources keeps its pixel size.
        BitmapDrawable d = new BitmapDrawable(res, b);
        assertEquals(10, d.getIntrinsicWidth());
        assertEquals(20, d.getIntrinsicHeight());

        b.setDensity(device / 2);
        assertEquals(20, d.getIntrinsicWidth());
        assertEquals(40, d.getIntrinsicHeight());

        d.setTargetDensity(device / 4);
        assertEquals(5, d.getIntrinsicWidth());
        assertEquals(10, d.getIntrinsicHeight());

        Drawable copy = d.getConstantState().newDrawable();
        assertEquals(5, copy.getIntrinsicWidth());
    }
}
