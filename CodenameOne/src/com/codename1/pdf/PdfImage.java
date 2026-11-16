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
package com.codename1.pdf;

import com.codename1.impl.pdf.PdfWriter;
import com.codename1.impl.pdf.PngDecoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// An image to draw on a PDF page, built from encoded JPEG or PNG bytes or
/// from ARGB pixels. Works without the UI layer, so a server can use it.
///
/// A JPEG is embedded as it is, without decoding. A PNG is decoded and its
/// transparency kept as a soft mask.
///
/// ```java
/// PdfImage logo = PdfImage.create(Util.readInputStream(in));
/// canvas.drawImage(logo, 36, 36, 120, 120 * logo.getHeight() / logo.getWidth());
/// ```
@com.codename1.impl.SharedWithBackend
public final class PdfImage {
    private final int width;
    private final int height;
    private final byte[] jpeg;
    private final int jpegComponents;
    private final boolean adobeInverted;
    private final int[] argb;
    private final boolean alpha;

    private PdfImage(int width, int height, byte[] jpeg, int components, boolean inverted, int[] argb,
            boolean alpha) {
        this.width = width;
        this.height = height;
        this.jpeg = jpeg;
        this.jpegComponents = components;
        this.adobeInverted = inverted;
        this.argb = argb;
        this.alpha = alpha;
    }

    /// An image from the bytes of a JPEG or PNG file, told apart by their
    /// signatures. Throws [IOException] for any other format.
    public static PdfImage create(byte[] encoded) throws IOException {
        if (PngDecoder.isPng(encoded)) {
            PngDecoder png = PngDecoder.decode(encoded);
            return new PdfImage(png.width, png.height, null, 0, false, png.argb, png.hasAlpha);
        }
        if (encoded.length > 3 && (encoded[0] & 0xff) == 0xff && (encoded[1] & 0xff) == 0xd8) {
            return jpeg(encoded);
        }
        throw new IOException("Only JPEG and PNG images are supported");
    }

    /// An image from pixels `0xAARRGGBB`, row by row from the top.
    public static PdfImage create(int[] argb, int width, int height) {
        if (width <= 0 || height <= 0 || argb.length < width * height) {
            throw new IllegalArgumentException("Pixel array does not match " + width + "x" + height);
        }
        int[] copy = new int[width * height];
        System.arraycopy(argb, 0, copy, 0, copy.length);
        boolean alpha = false;
        for (int i = 0; i < copy.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            if ((copy[i] >>> 24) != 0xff) {
                alpha = true;
                break;
            }
        }
        return new PdfImage(width, height, null, 0, false, copy, alpha);
    }

    private static PdfImage jpeg(byte[] d) throws IOException {
        int p = 2;
        boolean adobe = false;
        while (p + 4 <= d.length) {
            if ((d[p] & 0xff) != 0xff) {
                p++;
                continue;
            }
            int marker = d[p + 1] & 0xff;
            if (marker == 0xff) {
                p++;
                continue;
            }
            if (marker == 0xd8 || marker >= 0xd0 && marker <= 0xd7 || marker == 0x01) {
                p += 2;
                continue;
            }
            int len = ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
            if (marker == 0xee && len >= 7 && p + 9 <= d.length && d[p + 4] == 'A' && d[p + 5] == 'd') {
                adobe = true;
            }
            boolean sof = marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
            if (sof && p + 10 <= d.length) {
                int h = ((d[p + 5] & 0xff) << 8) | (d[p + 6] & 0xff);
                int w = ((d[p + 7] & 0xff) << 8) | (d[p + 8] & 0xff);
                int comps = d[p + 9] & 0xff;
                if (w <= 0 || h <= 0 || comps != 1 && comps != 3 && comps != 4) {
                    break;
                }
                byte[] copy = new byte[d.length];
                System.arraycopy(d, 0, copy, 0, d.length);
                return new PdfImage(w, h, copy, comps, adobe && comps == 4, null, false);
            }
            p += 2 + len;
        }
        throw new IOException("JPEG has no readable frame header");
    }

    /// The width in pixels.
    public int getWidth() {
        return width;
    }

    /// The height in pixels.
    public int getHeight() {
        return height;
    }

    /// Writes the image XObject as `number`.
    void write(PdfWriter w, int number) throws IOException {
        String size = "/Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                + " /BitsPerComponent 8";
        if (jpeg != null) {
            String cs = jpegComponents == 1 ? "/DeviceGray" : jpegComponents == 3 ? "/DeviceRGB" : "/DeviceCMYK";
            String decode = adobeInverted ? " /Decode [1 0 1 0 1 0 1 0]" : "";
            w.stream(number, size + " /ColorSpace " + cs + decode + " /Filter /DCTDecode", jpeg, false);
            return;
        }
        String smask = "";
        if (alpha) {
            int mask = w.reserve();
            byte[] a = new byte[width * height];
            for (int i = 0; i < a.length; i++) {
                a[i] = (byte) (argb[i] >>> 24);
            }
            w.stream(mask, "/Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                    + " /BitsPerComponent 8 /ColorSpace /DeviceGray", a, true);
            smask = " /SMask " + mask + " 0 R";
        }
        ByteArrayOutputStream rgb = new ByteArrayOutputStream(width * height * 3);
        byte[] row = new byte[width * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = argb[y * width + x];
                row[x * 3] = (byte) (px >> 16);
                row[x * 3 + 1] = (byte) (px >> 8);
                row[x * 3 + 2] = (byte) px;
            }
            rgb.write(row, 0, row.length);
        }
        w.stream(number, size + " /ColorSpace /DeviceRGB" + smask, rgb.toByteArray(), true);
    }
}
