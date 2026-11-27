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
package com.codename1.art;

/// Coverage of a rectangle of device pixels, one byte (0..255) per pixel.
final class Mask {
    final int x;
    final int y;
    final int w;
    final int h;
    final byte[] a;

    Mask(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
        this.a = new byte[this.w * this.h];
    }

    int get(int px, int py) {
        int lx = px - x;
        int ly = py - y;
        if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
            return 0;
        }
        return a[ly * w + lx] & 0xff;
    }

    boolean isEmpty() {
        return w == 0 || h == 0;
    }

    /// Multiplies by another mask; pixels outside `other` become 0.
    void intersect(Mask other) {
        int i = 0;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int v = a[i] & 0xff;
                if (v != 0) {
                    a[i] = (byte) ((v * other.get(x + xx, y + yy) + 127) / 255);
                }
                i++;
            }
        }
    }

    /// Multiplies by the inverse of another mask.
    void subtract(Mask other) {
        int i = 0;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int v = a[i] & 0xff;
                if (v != 0) {
                    a[i] = (byte) ((v * (255 - other.get(x + xx, y + yy)) + 127) / 255);
                }
                i++;
            }
        }
    }

    /// A raster of `argb`'s color with this coverage as alpha.
    Raster toRaster(int argb) {
        Raster r = new Raster(x, y, w, h);
        int[] p = r.getPixels();
        int alpha = argb >>> 24;
        int rgb = argb & 0xffffff;
        for (int i = 0; i < a.length; i++) {
            int v = a[i] & 0xff;
            if (v != 0) {
                p[i] = (((v * alpha + 127) / 255) << 24) | rgb;
            }
        }
        return r;
    }
}
