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
package com.codename1.desktopcompat.rt;

import com.codename1.ui.Display;

/// The size of an AWT pixel.
///
/// Desktop code lays itself out in pixels and assumes roughly 96 of them to
/// the inch: a 24 pixel row, a 12 point font. A phone packs three or four
/// device pixels into that distance, so every AWT coordinate is a *logical*
/// pixel of 1/96 inch and this class holds the number of device pixels one
/// of them covers. Component bounds, font sizes and everything drawn through
/// [G2D] are multiplied by it on the way to Codename One, and peer bounds
/// are snapped to whole device pixels edge by edge, so two components that
/// touch in logical pixels touch on the screen too.
///
/// The scale comes from the display's density. The Display property
/// `desktopcompat.scale` overrides it for an application that wants its
/// pixels larger or smaller, and [#setScale(float)] overrides both -- that
/// is what tests use to get round numbers.
public final class Units {

    private static float override;
    private static float property = -1;

    private Units() {
    }

    /// Device pixels per logical pixel.
    public static float scale() {
        if (override > 0) {
            return override;
        }
        if (!Display.isInitialized()) {
            return 1f;
        }
        Display d = Display.getInstance();
        if (property < 0) {
            property = 0;
            String p = d.getProperty("desktopcompat.scale", null);
            if (p != null && p.length() > 0) {
                try {
                    property = Float.parseFloat(p);
                } catch (NumberFormatException e) {
                    property = 0;
                }
            }
        }
        if (property > 0) {
            return property;
        }
        // convertToPixels takes millimetres; a logical pixel is 25.4/96 of one.
        float perMm = d.convertToPixels(1000, true) / 1000f;
        float s = perMm * 25.4f / 96f;
        return s > 0 ? s : 1f;
    }

    /// Fixes the scale, or with 0 goes back to the display's own. Also
    /// forgets the `desktopcompat.scale` property so it is read again.
    public static void setScale(float scale) {
        override = scale;
        property = -1;
    }

    /// The device pixel a logical coordinate snaps to.
    public static int toDevice(float logical) {
        return Math.round(logical * scale());
    }

    /// The device length of the logical span `[position, position + size)`:
    /// the distance between its two snapped edges, not the snapped length,
    /// so adjacent spans never gap or overlap.
    public static int toDeviceSize(int position, int size) {
        float s = scale();
        return Math.round((position + size) * s) - Math.round(position * s);
    }

    /// The logical pixel a device coordinate falls in, and the largest
    /// logical length that fits in `device` pixels.
    public static int toLogical(int device) {
        return (int) Math.floor(device / scale() + 0.001f);
    }

    /// The smallest logical length that covers `device` pixels; what a
    /// preferred size measured by Codename One becomes.
    public static int toLogicalCeil(int device) {
        return (int) Math.ceil(device / scale() - 0.001f);
    }

    /// A device coordinate in logical pixels, unrounded.
    public static float toLogicalExact(float device) {
        return device / scale();
    }
}
