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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Display;

/// The conversion between JavaFX coordinates and device pixels.
///
/// A JavaFX coordinate is a density independent logical pixel, 1/96 of an
/// inch. The scale is the number of device pixels in one of them, derived
/// from the display density Codename One reports. The Display property
/// `desktopcompat.scale` overrides it, and so does [#setScale(double)].
///
/// Sizes handed to Codename One are snapped to whole device pixels by
/// converting both edges of a rectangle and subtracting, so two rectangles
/// that touch in logical coordinates touch on the screen too.
public final class Units {

    private static double override;
    private static double cached;

    private Units() {
    }

    /// Returns the number of device pixels in one logical pixel.
    public static double scale() {
        if (override > 0) {
            return override;
        }
        if (cached > 0) {
            return cached;
        }
        if (!Display.isInitialized()) {
            return 1;
        }
        double s = 0;
        String property = Display.getInstance().getProperty("desktopcompat.scale", null);
        if (property != null) {
            try {
                s = Double.parseDouble(property);
            } catch (NumberFormatException malformed) {
                s = 0;
            }
        }
        if (!(s > 0)) {
            double pixelsPerMm = Display.getInstance().convertToPixels(1000, true) / 1000.0;
            s = pixelsPerMm * 25.4 / 96.0;
        }
        if (!(s > 0)) {
            s = 1;
        }
        cached = s;
        return s;
    }

    /// Fixes the scale; a value that is not positive returns to the
    /// display's own.
    public static void setScale(double scale) {
        override = scale > 0 ? scale : 0;
        cached = 0;
    }

    /// Converts a logical coordinate to the nearest device pixel.
    public static int toPixels(double logical) {
        return (int) Math.round(logical * scale());
    }

    /// Converts a logical length to device pixels, never rounding a visible
    /// length down to nothing.
    public static int sizeToPixels(double logical) {
        if (!(logical > 0)) {
            return 0;
        }
        int px = (int) Math.round(logical * scale());
        return px < 1 ? 1 : px;
    }

    /// Converts device pixels to logical pixels.
    public static double toLogical(double pixels) {
        return pixels / scale();
    }

    /// Moves a logical coordinate onto the nearest device pixel boundary.
    public static double snap(double logical) {
        double s = scale();
        return Math.round(logical * s) / s;
    }

    /// Grows a logical length to the next device pixel boundary.
    public static double snapSize(double logical) {
        double s = scale();
        return Math.ceil(logical * s - 1e-6) / s;
    }
}
