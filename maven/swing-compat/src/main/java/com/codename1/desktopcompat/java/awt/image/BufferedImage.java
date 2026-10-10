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
package com.codename1.desktopcompat.java.awt.image;

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Transparency;
import com.codename1.desktopcompat.rt.G2D;

/// An image whose pixels are an array of ARGB integers.
///
/// Whatever type it is created with, it stores one `int` per pixel; the
/// type only decides whether the alpha channel is kept (`TYPE_INT_ARGB` and
/// the other alpha types) or every pixel is opaque. Drawing into it goes
/// through a Codename One mutable image that is created the first time a
/// graphics is asked for; pixels move between that image and the array only
/// when the other side has changed and is then read, so a program that only
/// calls `setRGB`, or only draws, never pays for the copy. There are no
/// rasters, color models or sample models.
public class BufferedImage extends Image implements RenderedImage, Transparency {

    public static final int TYPE_CUSTOM = 0;
    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_INT_ARGB_PRE = 3;
    public static final int TYPE_INT_BGR = 4;
    public static final int TYPE_3BYTE_BGR = 5;
    public static final int TYPE_4BYTE_ABGR = 6;
    public static final int TYPE_4BYTE_ABGR_PRE = 7;
    public static final int TYPE_USHORT_565_RGB = 8;
    public static final int TYPE_USHORT_555_RGB = 9;
    public static final int TYPE_BYTE_GRAY = 10;
    public static final int TYPE_USHORT_GRAY = 11;
    public static final int TYPE_BYTE_BINARY = 12;
    public static final int TYPE_BYTE_INDEXED = 13;

    private final int width;
    private final int height;
    private final int type;
    private final boolean alpha;
    private final int[] pixels;
    /// The mutable image drawing goes into, once a graphics was asked for.
    private com.codename1.ui.Image surface;
    /// An immutable image of [#pixels], for an image nobody draws into.
    private com.codename1.ui.Image snapshot;
    private boolean surfaceNewer;
    private boolean pixelsNewer;

    public BufferedImage(int width, int height, int imageType) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width (" + width + ") and height (" + height + ") cannot be <= 0");
        }
        this.width = width;
        this.height = height;
        this.type = imageType;
        this.alpha = imageType == TYPE_INT_ARGB || imageType == TYPE_INT_ARGB_PRE || imageType == TYPE_4BYTE_ABGR
                || imageType == TYPE_4BYTE_ABGR_PRE || imageType == TYPE_CUSTOM;
        this.pixels = new int[width * height];
        if (!alpha) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = 0xff000000;
            }
        }
    }

    public int getType() {
        return type;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getMinX() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return width;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return height;
    }

    private void pull() {
        if (surfaceNewer && surface != null) {
            surfaceNewer = false;
            int[] rgb = surface.getRGB();
            if (rgb != null && rgb.length == pixels.length) {
                System.arraycopy(rgb, 0, pixels, 0, rgb.length);
            }
            if (!alpha) {
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] |= 0xff000000;
                }
            }
        }
    }

    private void push() {
        if (pixelsNewer && surface != null) {
            pixelsNewer = false;
            com.codename1.ui.Graphics g = surface.getGraphics();
            g.clearRect(0, 0, width, height);
            g.drawImage(com.codename1.ui.Image.createImage(pixels, width, height), 0, 0);
        }
    }

    private void check(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    public int getRGB(int x, int y) {
        check(x, y);
        pull();
        return pixels[y * width + x];
    }

    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        int[] out = rgbArray == null ? new int[offset + h * scansize] : rgbArray;
        if (w <= 0 || h <= 0) {
            return out;
        }
        check(startX, startY);
        check(startX + w - 1, startY + h - 1);
        pull();
        for (int row = 0; row < h; row++) {
            System.arraycopy(pixels, (startY + row) * width + startX, out, offset + row * scansize, w);
        }
        return out;
    }

    public void setRGB(int x, int y, int rgb) {
        check(x, y);
        pull();
        pixels[y * width + x] = alpha ? rgb : rgb | 0xff000000;
        pixelsNewer = true;
        snapshot = null;
    }

    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        if (w <= 0 || h <= 0) {
            return;
        }
        check(startX, startY);
        check(startX + w - 1, startY + h - 1);
        pull();
        for (int row = 0; row < h; row++) {
            int to = (startY + row) * width + startX;
            int from = offset + row * scansize;
            for (int col = 0; col < w; col++) {
                pixels[to + col] = alpha ? rgbArray[from + col] : rgbArray[from + col] | 0xff000000;
            }
        }
        pixelsNewer = true;
        snapshot = null;
    }

    @Override
    public Graphics getGraphics() {
        return createGraphics();
    }

    public Graphics2D createGraphics() {
        if (surface == null) {
            surface = com.codename1.ui.Image.createImage(width, height, alpha ? 0 : 0xff000000);
            pixelsNewer = true;
            snapshot = null;
        }
        push();
        return G2D.forImage(this, surface.getGraphics(), width, height);
    }

    /// Called by a graphics of this image before it draws: the surface now
    /// holds pixels the array does not.
    public void cn1Drawing() {
        push();
        surfaceNewer = true;
    }

    @Override
    public com.codename1.ui.Image cn1Image() {
        if (surface != null) {
            push();
            return surface;
        }
        if (snapshot == null) {
            snapshot = com.codename1.ui.Image.createImage(pixels, width, height);
        }
        return snapshot;
    }

    /// A copy of the region. The JDK's subimage shares pixels with its
    /// parent; this one does not.
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, type);
        getRGB(x, y, w, h, out.pixels, 0, w);
        out.pixelsNewer = true;
        return out;
    }

    public boolean isAlphaPremultiplied() {
        return false;
    }

    @Override
    public int getTransparency() {
        return alpha ? TRANSLUCENT : OPAQUE;
    }

    @Override
    public void flush() {
        snapshot = null;
    }

    @Override
    public String toString() {
        return "BufferedImage@" + Integer.toHexString(hashCode()) + ": type = " + type + " " + width + "x" + height;
    }
}
