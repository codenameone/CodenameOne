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
package com.codename1.qrcode;

import com.codename1.ui.Image;

/// Draws a [QrCode] as an [Image], with the four module quiet zone a scanner
/// needs around it.
public final class QrCodeImage {
    private QrCodeImage() {
    }

    /// Black on white, `scale` pixels per module.
    public static Image create(QrCode code, int scale) {
        return create(code, scale, 0xff000000, 0xffffffff);
    }

    /// `dark` and `light` are ARGB. Keep the contrast high; most scanners
    /// also expect dark modules on a light ground.
    public static Image create(QrCode code, int scale, int dark, int light) {
        if (code == null || scale < 1) {
            throw new IllegalArgumentException("a code and a scale of at least 1");
        }
        int modules = code.getSize() + 8;
        int side = modules * scale;
        int[] argb = new int[side * side];
        for (int my = 0; my < modules; my++) {
            for (int mx = 0; mx < modules; mx++) {
                int color = code.isDark(mx - 4, my - 4) ? dark : light;
                for (int py = my * scale, endY = py + scale; py < endY; py++) {
                    int row = py * side;
                    for (int px = mx * scale, endX = px + scale; px < endX; px++) {
                        argb[row + px] = color;
                    }
                }
            }
        }
        return Image.createImage(argb, side, side);
    }
}
