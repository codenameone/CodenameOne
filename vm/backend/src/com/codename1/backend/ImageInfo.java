/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// The format and dimensions of an image, read from its header without decoding
/// any pixels.
///
/// ```java
/// ImageInfo info = ImageInfo.read(upload);
/// if (info == null || info.getFormat() != ImageInfo.Format.PNG) {
///     throw new ResponseStatusException(400, "a PNG is required");
/// }
/// if (info.getWidth() != 1290 || info.getHeight() != 2796) {
///     throw new ResponseStatusException(400, "wrong screenshot size");
/// }
/// ```
///
/// PNG, JPEG, GIF and WebP are recognized by their signature, never by a file
/// name. The answer is what the header *claims*: a file with a valid header and a
/// corrupt body still reads, so this checks an upload's shape, not that it will
/// display.
///
/// - **PNG**: width and height from `IHDR`, plus [#getBitDepth()],
///   [#getColorType()] and [#hasAlpha()] (an alpha color type, or a `tRNS`
///   chunk before the image data).
/// - **JPEG**: the segments are walked to the first start-of-frame marker
///   (`SOF0`-`SOF15`, excluding `DHT`, `JPG` and `DAC`), skipping `APPn`
///   segments such as EXIF and fill bytes; [#isProgressive()] reports a
///   progressive frame and [#getBitDepth()] the sample precision. EXIF orientation
///   is not applied: the dimensions are the stored ones.
/// - **GIF**: the logical screen size.
/// - **WebP**: lossy (`VP8 `), lossless (`VP8L`) and extended (`VP8X`), with
///   [#hasAlpha()].
///
/// Anything else answers null.
public final class ImageInfo {
    /// An image format this class recognizes.
    public enum Format {
        /// Portable Network Graphics.
        PNG,
        /// JPEG / JFIF / EXIF.
        JPEG,
        /// Graphics Interchange Format.
        GIF,
        /// WebP, lossy or lossless.
        WEBP
    }

    private static final int MAX_STREAM_BYTES = 64 * 1024 * 1024;

    private final Format format;
    private final int width;
    private final int height;
    private final int bitDepth;
    private final int colorType;
    private final boolean alpha;
    private final boolean progressive;

    private ImageInfo(Format format, int width, int height, int bitDepth, int colorType, boolean alpha,
            boolean progressive) {
        this.format = format;
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.colorType = colorType;
        this.alpha = alpha;
        this.progressive = progressive;
    }

    /// Reads the header of an image held in memory; null when the data is not a
    /// recognized format or its header is truncated or invalid.
    public static ImageInfo read(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }
        int b0 = data[0] & 0xff;
        if (b0 == 0x89 && ascii(data, 1, "PNG")) {
            return readPng(data);
        }
        if (b0 == 0xff && (data[1] & 0xff) == 0xd8) {
            return readJpeg(data);
        }
        if (ascii(data, 0, "GIF87a") || ascii(data, 0, "GIF89a")) {
            return new ImageInfo(Format.GIF, le16(data, 6), le16(data, 8), -1, -1, false, false);
        }
        if (ascii(data, 0, "RIFF") && ascii(data, 8, "WEBP")) {
            return readWebp(data);
        }
        return null;
    }

    /// Reads `in` to its end (at most 64 MB) and answers [#read(byte[])] of the
    /// bytes. The stream is not closed.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when reading fails or the stream is longer than 64 MB.
    public static ImageInfo read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            if (out.size() + n > MAX_STREAM_BYTES) {
                throw new IOException("image larger than " + MAX_STREAM_BYTES + " bytes");
            }
            out.write(buf, 0, n);
        }
        return read(out.toByteArray());
    }

    /// The format.
    public Format getFormat() {
        return format;
    }

    /// The width in pixels.
    public int getWidth() {
        return width;
    }

    /// The height in pixels.
    public int getHeight() {
        return height;
    }

    /// Bits per sample: the PNG bit depth or the JPEG sample precision; -1 for
    /// GIF and WebP.
    public int getBitDepth() {
        return bitDepth;
    }

    /// The PNG color type (0 gray, 2 RGB, 3 palette, 4 gray + alpha, 6 RGBA); -1
    /// for the other formats.
    public int getColorType() {
        return colorType;
    }

    /// True when the image can carry transparency: a PNG with an alpha color type
    /// or a `tRNS` chunk, or a WebP with an alpha flag. Always false for JPEG and
    /// GIF.
    public boolean hasAlpha() {
        return alpha;
    }

    /// True for a progressive JPEG (`SOF2`, `SOF6`, `SOF10`, `SOF14`).
    public boolean isProgressive() {
        return progressive;
    }

    @Override
    public String toString() {
        return format + " " + width + "x" + height;
    }

    private static ImageInfo readPng(byte[] d) {
        if (d.length < 29 || (d[4] & 0xff) != 0x0d || (d[5] & 0xff) != 0x0a || (d[6] & 0xff) != 0x1a
                || (d[7] & 0xff) != 0x0a || !ascii(d, 12, "IHDR")) {
            return null;
        }
        int w = be32(d, 16);
        int h = be32(d, 20);
        if (w <= 0 || h <= 0) {
            return null;
        }
        int depth = d[24] & 0xff;
        int type = d[25] & 0xff;
        boolean alpha = type == 4 || type == 6;
        // Walk the chunks after IHDR for a tRNS before the image data.
        int p = 8 + 8 + 13 + 4;
        while (!alpha && p + 8 <= d.length) {
            int len = be32(d, p);
            if (len < 0 || ascii(d, p + 4, "IDAT") || ascii(d, p + 4, "IEND")) {
                break;
            }
            if (ascii(d, p + 4, "tRNS")) {
                alpha = true;
            }
            long next = (long) p + 12 + len;
            if (next > d.length) {
                break;
            }
            p = (int) next;
        }
        return new ImageInfo(Format.PNG, w, h, depth, type, alpha, false);
    }

    private static ImageInfo readJpeg(byte[] d) {
        int p = 2;
        int len = d.length;
        while (p < len) {
            if ((d[p] & 0xff) != 0xff) {
                return null;
            }
            // Any number of 0xFF fill bytes may precede a marker.
            while (p < len && (d[p] & 0xff) == 0xff) {
                p++;
            }
            if (p >= len) {
                return null;
            }
            int marker = d[p++] & 0xff;
            if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                continue; // TEM and RSTn stand alone.
            }
            if (marker == 0xd9 || marker == 0xda || marker == 0x00) {
                return null; // End of image, or scan data, before any frame.
            }
            if (p + 2 > len) {
                return null;
            }
            int segment = ((d[p] & 0xff) << 8) | (d[p + 1] & 0xff);
            if (segment < 2) {
                return null;
            }
            if (marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc) {
                if (p + 7 > len) {
                    return null;
                }
                int precision = d[p + 2] & 0xff;
                int h = ((d[p + 3] & 0xff) << 8) | (d[p + 4] & 0xff);
                int w = ((d[p + 5] & 0xff) << 8) | (d[p + 6] & 0xff);
                if (w == 0 || h == 0) {
                    return null;
                }
                boolean progressive = marker == 0xc2 || marker == 0xc6 || marker == 0xca || marker == 0xce;
                return new ImageInfo(Format.JPEG, w, h, precision, -1, false, progressive);
            }
            p += segment;
        }
        return null;
    }

    private static ImageInfo readWebp(byte[] d) {
        if (d.length < 30) {
            return null;
        }
        if (ascii(d, 12, "VP8 ")) {
            if ((d[23] & 0xff) != 0x9d || (d[24] & 0xff) != 0x01 || (d[25] & 0xff) != 0x2a) {
                return null;
            }
            return new ImageInfo(Format.WEBP, le16(d, 26) & 0x3fff, le16(d, 28) & 0x3fff, -1, -1, false, false);
        }
        if (ascii(d, 12, "VP8L")) {
            if ((d[20] & 0xff) != 0x2f) {
                return null;
            }
            int b1 = d[21] & 0xff;
            int b2 = d[22] & 0xff;
            int b3 = d[23] & 0xff;
            int b4 = d[24] & 0xff;
            int w = 1 + (b1 | ((b2 & 0x3f) << 8));
            int h = 1 + ((b2 >> 6) | (b3 << 2) | ((b4 & 0x0f) << 10));
            boolean alpha = ((b4 >> 4) & 1) != 0;
            return new ImageInfo(Format.WEBP, w, h, -1, -1, alpha, false);
        }
        if (ascii(d, 12, "VP8X")) {
            boolean alpha = (d[20] & 0x10) != 0;
            int w = 1 + le24(d, 24);
            int h = 1 + le24(d, 27);
            return new ImageInfo(Format.WEBP, w, h, -1, -1, alpha, false);
        }
        return null;
    }

    private static boolean ascii(byte[] d, int at, String s) {
        if (at < 0 || at + s.length() > d.length) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (d[at + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int be32(byte[] d, int at) {
        return ((d[at] & 0xff) << 24) | ((d[at + 1] & 0xff) << 16) | ((d[at + 2] & 0xff) << 8) | (d[at + 3] & 0xff);
    }

    private static int le16(byte[] d, int at) {
        return (d[at] & 0xff) | ((d[at + 1] & 0xff) << 8);
    }

    private static int le24(byte[] d, int at) {
        return (d[at] & 0xff) | ((d[at + 1] & 0xff) << 8) | ((d[at + 2] & 0xff) << 16);
    }
}
