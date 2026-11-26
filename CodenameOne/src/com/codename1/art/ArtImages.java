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

import com.codename1.impl.pdf.PngDecoder;
import com.codename1.io.gzip.CRC32;
import com.codename1.io.gzip.DeflaterOutputStream;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// Encodes rasters as PNG and decodes PNG, JPEG and other image bytes into rasters.
public final class ArtImages {
    /// The largest width or height a decoded image may have.
    public static final int MAX_DIMENSION = 30000;

    /// The largest side, in pixels, `renderSvg` renders. A larger document fails rather than
    /// allocate hundreds of megabytes on a device; render it with `ArtRenderer.toImage` at a
    /// scale of your choosing instead.
    public static final int MAX_SVG_SIDE = 4096;

    /// Renders an SVG document, once, into an ordinary image at the size it declares. This is
    /// what `Image.createSVG` answers on a platform without native SVG.
    ///
    /// #### Parameters
    ///
    /// - `svg`: the document
    ///
    /// #### Returns
    ///
    /// the rendered image
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the data is not SVG or is larger than `MAX_SVG_SIDE` on a side
    public static Image renderSvg(byte[] svg) throws IOException {
        ArtDocument doc = SvgReader.read(svg);
        double w = Math.ceil(doc.getWidth());
        double h = Math.ceil(doc.getHeight());
        if (!(w <= MAX_SVG_SIDE && h <= MAX_SVG_SIDE)) {
            throw new IOException("SVG is " + w + "x" + h + ", larger than " + MAX_SVG_SIDE
                    + " pixels on a side; render it with ArtRenderer.toImage at a smaller scale");
        }
        return ArtRenderer.toImage(doc, 1);
    }

    private ArtImages() {
    }

    /// Decodes image bytes. PNG is decoded in pure Java; other formats go through the
    /// platform and need an initialized display.
    ///
    /// #### Parameters
    ///
    /// - `data`: the encoded image
    ///
    /// #### Returns
    ///
    /// the pixels, or null when the bytes are not an image this platform can decode
    public static Raster decode(byte[] data) {
        if (data == null || data.length < 8) {
            return null;
        }
        try {
            if (PngDecoder.isPng(data)) {
                // Check the header's size before decoding allocates it.
                if (data.length >= 24) {
                    long w = int32(data, 16) & 0xffffffffL;
                    long h = int32(data, 20) & 0xffffffffL;
                    if (w > MAX_DIMENSION || h > MAX_DIMENSION || w * h > Raster.MAX_PIXELS) {
                        return null;
                    }
                }
                PngDecoder d = PngDecoder.decode(data);
                if (d.width > MAX_DIMENSION || d.height > MAX_DIMENSION) {
                    return null;
                }
                return new Raster(0, 0, d.width, d.height, d.argb);
            }
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            Image img = Image.createImage(data, 0, data.length);
            if (img.getWidth() <= 0 || img.getHeight() <= 0
                    || img.getWidth() > MAX_DIMENSION || img.getHeight() > MAX_DIMENSION) {
                return null;
            }
            return new Raster(0, 0, img.getWidth(), img.getHeight(), img.getRGB());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Encodes pixels as a 32-bit RGBA PNG.
    ///
    /// #### Parameters
    ///
    /// - `r`: the pixels
    public static byte[] encodePng(Raster r) {
        try {
            int w = r.getWidth();
            int h = r.getHeight();
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DeflaterOutputStream z = new DeflaterOutputStream(raw);
            int[] px = r.getPixels();
            byte[] row = new byte[w * 4 + 1];
            for (int y = 0; y < h; y++) {
                row[0] = 0;
                for (int x = 0; x < w; x++) {
                    int p = px[y * w + x];
                    int o = 1 + x * 4;
                    row[o] = (byte) (p >> 16);
                    row[o + 1] = (byte) (p >> 8);
                    row[o + 2] = (byte) p;
                    row[o + 3] = (byte) (p >>> 24);
                }
                z.write(row, 0, row.length);
            }
            z.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            byte[] ihdr = new byte[13];
            put32(ihdr, 0, w);
            put32(ihdr, 4, h);
            ihdr[8] = 8;
            ihdr[9] = 6;
            chunk(out, "IHDR", ihdr);
            chunk(out, "IDAT", raw.toByteArray());
            chunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (IOException e) {
            IllegalStateException ex = new IllegalStateException(e.toString());
            ex.initCause(e);
            throw ex;
        }
    }

    private static void put32(byte[] b, int o, int v) {
        b[o] = (byte) (v >>> 24);
        b[o + 1] = (byte) (v >>> 16);
        b[o + 2] = (byte) (v >>> 8);
        b[o + 3] = (byte) v;
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] len = new byte[4];
        put32(len, 0, data.length);
        out.write(len);
        byte[] t = new byte[4];
        for (int i = 0; i < 4; i++) {
            t[i] = (byte) type.charAt(i);
        }
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t, 0, 4);
        crc.update(data, 0, data.length);
        byte[] c = new byte[4];
        put32(c, 0, (int) crc.getValue());
        out.write(c);
    }

    private static int int32(byte[] d, int p) {
        return ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
    }
}
