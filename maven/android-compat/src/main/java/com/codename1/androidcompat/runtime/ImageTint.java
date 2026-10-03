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
package com.codename1.androidcompat.runtime;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import com.codename1.ui.Image;

import java.util.HashMap;
import java.util.Map;

/// Recolors images for drawable tints and color filters. Codename One draws
/// images as they are, so a tinted icon is a recolored copy; copies are cached
/// per image and filter because tints are applied on every paint.
public final class ImageTint {

    /// Bounded rather than weak: Codename One has no weak maps on every port,
    /// and the set of tinted icons an app shows at once is small.
    private static final int MAX_IMAGES = 128;
    private static final Map<Image, Map<PorterDuffColorFilter, Image>> CACHE =
            new HashMap<Image, Map<PorterDuffColorFilter, Image>>();

    private ImageTint() {
    }

    public static Image tint(Image src, PorterDuffColorFilter filter) {
        Map<PorterDuffColorFilter, Image> perImage = CACHE.get(src);
        if (perImage == null) {
            if (CACHE.size() >= MAX_IMAGES) {
                CACHE.clear();
            }
            perImage = new HashMap<PorterDuffColorFilter, Image>();
            CACHE.put(src, perImage);
        }
        Image out = perImage.get(filter);
        if (out != null) {
            return out;
        }
        int[] px = src.getRGB();
        int color = filter.getColor();
        int ca = color >>> 24;
        int cr = (color >> 16) & 0xff;
        int cg = (color >> 8) & 0xff;
        int cb = color & 0xff;
        PorterDuff.Mode mode = filter.getMode();
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            switch (mode) {
                case MULTIPLY:
                    px[i] = (a * ca / 255 << 24) | (r * cr / 255 << 16) | (g * cg / 255 << 8) | (b * cb / 255);
                    break;
                case SRC_ATOP: {
                    int na = a;
                    int nr = (cr * ca + r * (255 - ca)) / 255;
                    int ng = (cg * ca + g * (255 - ca)) / 255;
                    int nb = (cb * ca + b * (255 - ca)) / 255;
                    px[i] = (na << 24) | (nr << 16) | (ng << 8) | nb;
                    break;
                }
                case SRC_OVER: {
                    int na = Math.min(255, ca + a * (255 - ca) / 255);
                    px[i] = (na << 24) | ((cr * ca + r * (255 - ca)) / 255 << 16)
                            | ((cg * ca + g * (255 - ca)) / 255 << 8) | ((cb * ca + b * (255 - ca)) / 255);
                    break;
                }
                default:
                    // SRC_IN, and the fallback for modes images rarely use:
                    // the tint color through the image's alpha.
                    px[i] = ((a * ca / 255) << 24) | (cr << 16) | (cg << 8) | cb;
                    break;
            }
        }
        out = Image.createImage(px, src.getWidth(), src.getHeight());
        perImage.put(filter, out);
        return out;
    }
}
