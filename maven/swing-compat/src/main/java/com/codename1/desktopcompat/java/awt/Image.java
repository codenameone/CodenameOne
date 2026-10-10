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

import com.codename1.desktopcompat.java.awt.image.ImageObserver;
import com.codename1.desktopcompat.rt.NativeImage;

/// A picture that can be drawn. One image pixel is one logical pixel.
///
/// Every image of the layer is backed by a Codename One image, which
/// [#cn1Image()] answers; that is what `Graphics.drawImage` draws.
public abstract class Image {

    public static final Object UndefinedProperty = new Object();
    public static final int SCALE_DEFAULT = 1;
    public static final int SCALE_FAST = 2;
    public static final int SCALE_SMOOTH = 4;
    public static final int SCALE_REPLICATE = 8;
    public static final int SCALE_AREA_AVERAGING = 16;

    public abstract int getWidth(ImageObserver observer);

    public abstract int getHeight(ImageObserver observer);

    public abstract Graphics getGraphics();

    /// The Codename One image holding this image's pixels as they are now,
    /// or null for an image that has none.
    public com.codename1.ui.Image cn1Image() {
        return null;
    }

    public Image getScaledInstance(int width, int height, int hints) {
        com.codename1.ui.Image src = cn1Image();
        if (src == null) {
            return this;
        }
        int w = width;
        int h = height;
        if (w < 0 && h < 0) {
            w = src.getWidth();
            h = src.getHeight();
        } else if (w < 0) {
            w = Math.max(1, src.getWidth() * h / Math.max(1, src.getHeight()));
        } else if (h < 0) {
            h = Math.max(1, src.getHeight() * w / Math.max(1, src.getWidth()));
        }
        return new NativeImage(src.scaled(w, h));
    }

    public void flush() {
    }
}
