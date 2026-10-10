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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.image.BufferedImage;

/// The screens of the device: there is one, the display.
public abstract class GraphicsEnvironment {

    private static GraphicsEnvironment local;

    protected GraphicsEnvironment() {
    }

    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        if (local == null) {
            local = new ScreenEnvironment();
        }
        return local;
    }

    /// Always `false`: there is a display, a pointer and, where the
    /// device has one, a keyboard.
    public static boolean isHeadless() {
        return false;
    }

    public boolean isHeadlessInstance() {
        return false;
    }

    public abstract GraphicsDevice[] getScreenDevices();

    public abstract GraphicsDevice getDefaultScreenDevice();

    /// The family names `Font` maps to Codename One fonts, and those of
    /// the fonts that were registered.
    public abstract String[] getAvailableFontFamilyNames();

    /// A graphics that draws into `img`.
    public abstract Graphics2D createGraphics(BufferedImage img);

    /// Lets `new Font` find a font made by `Font.createFont` under its
    /// family and its name. `false` for a font that was not created from
    /// a file, and for one named as a font of the platform or as another
    /// registered font is.
    public boolean registerFont(Font font) {
        com.codename1.desktopcompat.rt.FontFiles.Face face = font == null ? null : font.cn1Face();
        if (face == null) {
            return false;
        }
        String family = face.family();
        if (family.equalsIgnoreCase(Font.DIALOG) || family.equalsIgnoreCase(Font.DIALOG_INPUT)
                || family.equalsIgnoreCase(Font.MONOSPACED) || family.equalsIgnoreCase(Font.SANS_SERIF)
                || family.equalsIgnoreCase(Font.SERIF)) {
            return false;
        }
        return com.codename1.desktopcompat.rt.FontFiles.register(face);
    }

    /// The center of the display, in logical pixels.
    public Point getCenterPoint() {
        Rectangle r = getMaximumWindowBounds();
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /// The whole display, in logical pixels.
    public Rectangle getMaximumWindowBounds() {
        return getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }
}
