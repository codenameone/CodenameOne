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
package com.codename1.impl.html5;

import com.codename1.html5.js.typedarrays.Uint8ClampedArray;

public final class JavaScriptImageDataAdapter {
    private JavaScriptImageDataAdapter() {
    }

    public interface PixelWriter {
        void set(int index, int value);
    }

    public interface PixelReader {
        int get(int index);
        int length();
    }

    public static void writeArgbToRgba(int[] rgb, int offset, int width, int height, PixelWriter writer) {
        int j = 0;
        int end = offset + width * height;
        for (int i = offset; i < end; i++) {
            int argb = rgb[i];
            writer.set(j++, (argb >> 16) & 0xFF);
            writer.set(j++, (argb >> 8) & 0xFF);
            writer.set(j++, argb & 0xFF);
            writer.set(j++, (argb >> 24) & 0xFF);
        }
    }

    public static void readRgbaToArgb(PixelReader reader, int[] rgb, int offset) {
        int len = reader.length();
        for (int i = 0; i < len; i += 4) {
            rgb[offset + (i / 4)] =
                    ((reader.get(i + 3) << 24) & 0xff000000) |
                    ((reader.get(i) << 16) & 0x00ff0000) |
                    ((reader.get(i + 1) << 8) & 0x0000ff00) |
                    (reader.get(i + 2) & 0x000000ff);
        }
    }

    /**
     * Bulk RGBA->ARGB conversion implemented as a native intrinsic that
     * loops once in JS over the raw Uint8ClampedArray. Avoids the
     * per-byte JSO virtual dispatch the {@link PixelReader} path pays
     * (4 calls per pixel; 4.6M calls for a 1280x900 screenshot).
     *
     * <p>If the native binding is missing (test stubs etc.) callers must
     * fall back to {@link #readRgbaToArgb(PixelReader, int[], int)}.</p>
     *
     * @param src    the canvas image data buffer in RGBA order
     * @param dst    destination ARGB int[]
     * @param offset starting index in {@code dst}
     */
    public static native void readRgbaToArgbBulk(Uint8ClampedArray src, int[] dst, int offset);
}
