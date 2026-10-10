/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.ui.Display;

/**
 * Flutter logical pixel to CN1 device pixel conversion.
 *
 * <p>Flutter's logical pixel is defined as roughly 1/160 inch (a Material dp,
 * i.e. 0.15875mm). When a CN1 Display is available, logical values are
 * converted through {@code Display.convertToPixels} using that physical
 * definition; without a Display (headless unit tests) the scale is 1, so
 * logical values and pixels coincide.</p>
 */
public final class Dp {

    private static final double MM_PER_LP = 25.4 / 160.0;
    private static double cachedScale = -1;

    private Dp() {
    }

    /**
     * Device pixels per Flutter logical pixel.
     */
    public static double scale() {
        if (!Display.isInitialized()) {
            return 1;
        }
        if (cachedScale <= 0) {
            // Ask the platform for its OWN scale factor first. Flutter's logical pixel is
            // the platform's logical pixel, so where the platform reports one it is the
            // right answer by definition - and it is not always what the density bucket
            // implies. On iOS the bucket for a modern iPhone is DENSITY_560, which maps to
            // 3.5, while UIScreen.scale is 3: everything rendered 7/6 too large against
            // native Flutter on the same device.
            cachedScale = Display.getInstance().getDevicePixelRatio();
            if (cachedScale <= 0) {
                cachedScale = bucketScale(Display.getInstance().getDeviceDensity());
            }
            if (cachedScale <= 0) {
                // unknown bucket: fall back to physical measurement
                int px = Display.getInstance().convertToPixels((float) (MM_PER_LP * 100));
                cachedScale = px / 100.0;
            }
            if (cachedScale <= 0) {
                cachedScale = 1;
            }
        }
        return cachedScale;
    }

    /**
     * Flutter/Android-style devicePixelRatio per CN1 density bucket.
     * Flutter buckets its devicePixelRatio exactly like Android dp buckets
     * (mdpi=1, hdpi=1.5, xhdpi=2, xxhdpi=3, xxxhdpi=4), so mapping CN1's
     * density constants beats measuring physical millimeters — a 256dpi
     * panel is an xhdpi/2.0 device, not a 1.6 one.
     */
    private static double bucketScale(int density) {
        switch (density) {
            case Display.DENSITY_VERY_LOW:
                return 0.5;
            case Display.DENSITY_LOW:
                return 0.75;
            case Display.DENSITY_MEDIUM:
                return 1.0;
            case Display.DENSITY_HIGH:
                return 1.5;
            case Display.DENSITY_VERY_HIGH:
                return 2.0;
            case Display.DENSITY_HD:
                return 3.0;
            case Display.DENSITY_560:
                return 3.5;
            case Display.DENSITY_2HD:
                return 4.0;
            case Display.DENSITY_4K:
                return 5.0;
            default:
                return -1;
        }
    }

    /**
     * Converts logical pixels to (fractional) device pixels.
     */
    public static double px(double lp) {
        return lp * scale();
    }

    /**
     * Converts logical pixels to millimeters (for CN1 APIs that take mm sizes,
     * e.g. FontImage.createMaterial).
     */
    public static float mm(double lp) {
        return (float) (lp * MM_PER_LP);
    }

    /**
     * Test hook / hot-reload hook: forgets the cached scale.
     */
    public static void resetCache() {
        cachedScale = -1;
    }
}
