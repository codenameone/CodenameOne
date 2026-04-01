/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

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
}
