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

import android.util.DisplayMetrics;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `createBitmap(DisplayMetrics, ...)` makes a bitmap at the density of the
/// metrics it is given, as on Android. It used to drop the metrics, so the
/// bitmap reported the device's density and scaled as if it were made for it.
public class BitmapDisplayMetricsDensityTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void theBitmapTakesTheMetricsDensity() {
        int device = AndroidTestSupport.context().getResources().getDisplayMetrics().densityDpi;
        DisplayMetrics dm = new DisplayMetrics();
        dm.densityDpi = device * 2;
        Bitmap b = Bitmap.createBitmap(dm, 10, 20, Bitmap.Config.ARGB_8888);
        assertEquals(device * 2, b.getDensity());
    }

    @Test
    public void noMetricsKeepsTheDeviceDensity() {
        int device = AndroidTestSupport.context().getResources().getDisplayMetrics().densityDpi;
        Bitmap b = Bitmap.createBitmap((DisplayMetrics) null, 10, 20, Bitmap.Config.ARGB_8888);
        assertEquals(device, b.getDensity());
    }
}
