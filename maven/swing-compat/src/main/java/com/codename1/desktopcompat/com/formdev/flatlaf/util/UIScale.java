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
package com.codename1.desktopcompat.com.formdev.flatlaf.util;

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;

/// FlatLaf's user scale factor, which on this layer is always one.
///
/// A Swing coordinate here is a logical pixel, 1/96 inch, and the layer
/// itself turns it into device pixels at the density of the display.
/// Code that multiplies its sizes with `UIScale.scale(...)` therefore gets
/// them back unchanged and is scaled exactly once.
public class UIScale {

    public static final String PROP_USER_SCALE_FACTOR = "userScaleFactor";
    public static final String PROP_ZOOM_FACTOR = "zoomFactor";

    public UIScale() {
    }

    /// True: the display is scaled for the application, by the layer.
    public static boolean isSystemScalingEnabled() {
        return true;
    }

    /// One; see the class description.
    public static float getUserScaleFactor() {
        return 1f;
    }

    public static float scale(float value) {
        return value;
    }

    public static int scale(int value) {
        return value;
    }

    public static int scale2(int value) {
        return value;
    }

    public static float unscale(float value) {
        return value;
    }

    public static int unscale(int value) {
        return value;
    }

    public static Dimension scale(Dimension dimension) {
        return dimension;
    }

    public static Insets scale(Insets insets) {
        return insets;
    }

    /// One: there is no zoom of the look and feel.
    public static float getZoomFactor() {
        return 1f;
    }
}
