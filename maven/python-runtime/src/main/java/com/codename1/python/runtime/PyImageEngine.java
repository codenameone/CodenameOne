/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

/// What PIL needs beyond its own pixel loops: codecs, resampling and text.
///
/// The runtime keeps Pillow's simple pixel operations (modes, crop, paste,
/// point, the `ImageDraw` shapes) and asks this seam for the rest. There
/// are two implementations, and the generated `PythonModules` installs one:
///
/// - On a **server** (a backend build) the transpiler generates
///   `BackendImage` over `com.codename1.backend.image`: Pillow's resampling
///   byte for byte, PNG and JPEG, and TrueType text laid out as FreeType
///   lays it out, so results match Pillow's pixels ([#exact()] is true).
/// - On a **device** and in the simulator, [PyImageNative] goes through
///   Codename One's `Image`, `ImageIO`, `Graphics` and `Font`. The platform
///   decodes, encodes, scales and draws text, so results are close to
///   Pillow's but not identical.
///
/// The runtime itself references neither, so it compiles into both builds.
public interface PyImageEngine {
    /// Whether results are Pillow's exact pixels (the server engine).
    boolean exact();

    /// Whether the engine can work now (the client one needs a display).
    boolean available();

    /// Decodes any image file the platform reads, to RGBA.
    PyImageData decode(byte[] data);

    /// Encodes an RGBA image as `format` ("png", "jpeg", "gif"...).
    byte[] encode(PyImageData rgba, String format, float quality);

    /// Resizes with Pillow's filter number `filter` (1 LANCZOS, 2 BILINEAR,
    /// 3 BICUBIC, 4 BOX, 5 HAMMING) from the source rectangle `box`;
    /// `premultiplied` resamples LA/RGBA with premultiplied alpha, as Pillow.
    PyImageData resample(PyImageData s, int w, int h, double[] box, int filter, boolean premultiplied);

    /// Reverses PNG row filtering (exact engine only).
    byte[] pngUnfilter(byte[] data, int row, int h, int bpp);

    /// Expands unfiltered PNG rows to 8-bit bands (exact engine only).
    PyImageData pngExpand(byte[] raw, int w, int h, int ctype, int depth);

    /// Decodes a JPEG file to "L" (one band) or "RGB" (three).
    PyImageData jpegDecode(byte[] data);

    /// Encodes an "L" or "RGB" image as JPEG.
    byte[] jpegEncode(PyImageData s, float quality);

    /// Loads face `index` of a TrueType file at `size` pixels.
    Object loadFont(byte[] data, int index, double size);

    /// The bytes of the font file `ImageFont.load_default()` reads, or an empty
    /// array where there is none (the client), and [#defaultFont(double)] is used.
    byte[] defaultFontData();

    /// The font `ImageFont.load_default()` answers, at `size` pixels.
    Object defaultFont(double size);

    /// `font` at another size.
    Object deriveFont(Object font, double size);

    /// Family and style name.
    String[] fontName(Object font);

    /// Ascent and descent in pixels, both positive.
    int[] fontMetrics(Object font);

    /// The advance of `text` in 64ths of a pixel.
    long textLength(Object font, String text);

    /// Width, height and the offset of the box from the anchor point, as
    /// `{w, h, x, y}`; `anchor` is null or a valid two-letter anchor.
    int[] textBox(Object font, String text, String anchor);

    /// The coverage of `text` as a one-band image of the [#textBox] size.
    PyImageData textMask(Object font, String text, String anchor, boolean mono);
}
