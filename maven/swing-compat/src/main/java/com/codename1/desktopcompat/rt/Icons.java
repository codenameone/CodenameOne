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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.ImageIcon;

/// Turns a Swing icon into the image a Codename One widget shows.
public final class Icons {

    private Icons() {
    }

    /// The icon as an image of its logical size in device pixels. An icon
    /// that is not an image is painted once, at one image pixel per logical
    /// pixel, and scaled.
    public static com.codename1.ui.Image toNative(Icon icon, Component c) {
        if (icon == null) {
            return null;
        }
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        com.codename1.ui.Image img = null;
        if (icon instanceof ImageIcon) {
            Image i = ((ImageIcon) icon).getImage();
            if (i != null) {
                img = i.cn1Image();
            }
        }
        if (img == null) {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = b.createGraphics();
            try {
                icon.paintIcon(c, g, 0, 0);
            } finally {
                g.dispose();
            }
            img = b.cn1Image();
        }
        if (img == null) {
            return null;
        }
        int dw = Math.max(1, Units.toDevice(w));
        int dh = Math.max(1, Units.toDevice(h));
        if (img.getWidth() != dw || img.getHeight() != dh) {
            img = img.scaled(dw, dh);
        }
        return img;
    }
}
