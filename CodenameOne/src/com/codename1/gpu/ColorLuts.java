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
package com.codename1.gpu;

/// Builds colour lookup strips for `PostProcess.setColorLut`. Each method
/// returns the 256 by 16 ARGB pixels; upload them with
/// `GraphicsDevice.createTexture(ColorLuts.WIDTH, ColorLuts.HEIGHT, pixels)`.
///
/// ```java
/// Texture look = device.createTexture(ColorLuts.WIDTH, ColorLuts.HEIGHT,
///         ColorLuts.filmic(0.6f, 0.15f));
/// post.setColorLut(look, 1f);
/// ```
public final class ColorLuts {
    /// The strip's width in pixels.
    public static final int WIDTH = 256;

    /// The strip's height in pixels.
    public static final int HEIGHT = 16;

    private ColorLuts() {
    }

    private static int clamp255(float v) {
        int i = (int) (v * 255f + 0.5f);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private static int[] build(int mode, float a, float b) {
        int[] out = new int[WIDTH * HEIGHT];
        for (int g = 0; g < 16; g++) {
            for (int bl = 0; bl < 16; bl++) {
                for (int r = 0; r < 16; r++) {
                    float rf = r / 15f;
                    float gf = g / 15f;
                    float bf = bl / 15f;
                    if (mode == 1) {
                        // split toning: warm highlights, cool shadows, a lifted black
                        float l = rf * 0.2126f + gf * 0.7152f + bf * 0.0722f;
                        float s = l * l * (3f - 2f * l);
                        rf = rf + a * 0.12f * (s - 0.35f);
                        bf = bf - a * 0.12f * (s - 0.35f);
                        rf = b + rf * (1f - b);
                        gf = b + gf * (1f - b);
                        bf = b + bf * (1f - b);
                    } else if (mode == 2) {
                        float n = Math.max(1f, a - 1f);
                        rf = (int) (rf * n + 0.5f) / n;
                        gf = (int) (gf * n + 0.5f) / n;
                        bf = (int) (bf * n + 0.5f) / n;
                    }
                    out[g * WIDTH + bl * 16 + r] = 0xff000000 | (clamp255(rf) << 16)
                            | (clamp255(gf) << 8) | clamp255(bf);
                }
            }
        }
        return out;
    }

    /// Returns the table that changes nothing, a starting point to edit.
    public static int[] identity() {
        return build(0, 0f, 0f);
    }

    /// Returns a film look: warm highlights, cool shadows and lifted blacks.
    ///
    /// #### Parameters
    ///
    /// - `warmth`: 0 to 1, how far highlights and shadows are split
    ///
    /// - `fade`: 0 to 0.3, how far black is lifted
    public static int[] filmic(float warmth, float fade) {
        return build(1, Math.max(0f, Math.min(1f, warmth)), Math.max(0f, Math.min(0.3f, fade)));
    }

    /// Returns a table that snaps every channel to a few levels, for a
    /// poster or cel look.
    ///
    /// #### Parameters
    ///
    /// - `levels`: levels per channel, 2 to 16
    public static int[] posterize(int levels) {
        return build(2, Math.max(2, Math.min(16, levels)), 0f);
    }
}
