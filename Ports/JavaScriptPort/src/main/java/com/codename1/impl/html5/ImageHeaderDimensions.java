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

/// Reads an encoded image's pixel size from its header, in Java, so a native image
/// made from bytes knows its size the moment it exists.
///
/// The browser decodes asynchronously, and the size it reports is a host query.
/// Asking it from `getWidth()` made the whole layout path -- every component that
/// measures an image -- wait on the host, and every caller of it a JavaScript
/// generator. The header holds the answer the decoder will give: PNG, GIF, JPEG,
/// WebP, BMP and ICO are read here, the rest answer null and keep the size the load
/// event reports.
final class ImageHeaderDimensions {
    private ImageHeaderDimensions() {
    }

    /// `{width, height}` as the browser will report them, or null when the format
    /// is not recognised or the header is cut short.
    static int[] read(byte[] b, int off, int len) {
        if (b == null || len < 10 || off < 0 || off + len > b.length) {
            return null;
        }
        int end = off + len;
        if (u8(b, off) == 0x89 && u8(b, off + 1) == 'P' && u8(b, off + 2) == 'N' && u8(b, off + 3) == 'G') {
            // Signature, then IHDR: length(4) "IHDR"(4) width(4) height(4).
            if (len < 24) {
                return null;
            }
            return size(be32(b, off + 16), be32(b, off + 20));
        }
        if (u8(b, off) == 'G' && u8(b, off + 1) == 'I' && u8(b, off + 2) == 'F') {
            return size(le16(b, off + 6), le16(b, off + 8));
        }
        if (u8(b, off) == 0xFF && u8(b, off + 1) == 0xD8) {
            return jpeg(b, off + 2, end);
        }
        if (len >= 30 && u8(b, off) == 'R' && u8(b, off + 1) == 'I' && u8(b, off + 2) == 'F' && u8(b, off + 3) == 'F'
                && u8(b, off + 8) == 'W' && u8(b, off + 9) == 'E' && u8(b, off + 10) == 'B' && u8(b, off + 11) == 'P') {
            return webp(b, off);
        }
        if (len >= 26 && u8(b, off) == 'B' && u8(b, off + 1) == 'M') {
            // BITMAPINFOHEADER: width and height are signed; a negative height is a
            // top-down bitmap of the same size.
            return size(le32(b, off + 18), Math.abs(le32(b, off + 22)));
        }
        if (len >= 22 && le16(b, off) == 0 && le16(b, off + 2) == 1 && le16(b, off + 4) > 0) {
            // ICO: the first directory entry, where 0 means 256.
            int w = u8(b, off + 6);
            int h = u8(b, off + 7);
            return size(w == 0 ? 256 : w, h == 0 ? 256 : h);
        }
        return null;
    }

    private static int[] jpeg(byte[] b, int p, int end) {
        int orientation = 1;
        while (p + 4 <= end) {
            if (u8(b, p) != 0xFF) {
                return null;
            }
            int marker = u8(b, p + 1);
            if (marker == 0xFF) {
                p++;
                continue;
            }
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                p += 2;
                continue;
            }
            if (marker == 0xD9 || marker == 0xDA) {
                return null;
            }
            int segment = be16(b, p + 2);
            if (segment < 2 || p + 2 + segment > end) {
                return null;
            }
            if (marker == 0xE1) {
                int o = exifOrientation(b, p + 4, p + 2 + segment);
                if (o > 0) {
                    orientation = o;
                }
            } else if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                if (segment < 7) {
                    return null;
                }
                int h = be16(b, p + 5);
                int w = be16(b, p + 7);
                // Browsers apply the EXIF orientation by default, and 5..8 turn the
                // picture a quarter: the reported width is the stored height.
                return orientation >= 5 && orientation <= 8 ? size(h, w) : size(w, h);
            }
            p += 2 + segment;
        }
        return null;
    }

    /// The EXIF orientation tag of an APP1 segment, or 0.
    private static int exifOrientation(byte[] b, int p, int end) {
        if (p + 14 > end || u8(b, p) != 'E' || u8(b, p + 1) != 'x' || u8(b, p + 2) != 'i' || u8(b, p + 3) != 'f'
                || u8(b, p + 4) != 0 || u8(b, p + 5) != 0) {
            return 0;
        }
        int tiff = p + 6;
        boolean little;
        if (u8(b, tiff) == 'I' && u8(b, tiff + 1) == 'I') {
            little = true;
        } else if (u8(b, tiff) == 'M' && u8(b, tiff + 1) == 'M') {
            little = false;
        } else {
            return 0;
        }
        int ifd = tiff + read32(b, tiff + 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return 0;
        }
        int count = read16(b, ifd, little);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 0;
            }
            if (read16(b, entry, little) == 0x0112) {
                return read16(b, entry + 8, little);
            }
        }
        return 0;
    }

    private static int[] webp(byte[] b, int off) {
        int chunk = off + 12;
        int c0 = u8(b, chunk);
        int c1 = u8(b, chunk + 1);
        int c2 = u8(b, chunk + 2);
        int c3 = u8(b, chunk + 3);
        if (c0 != 'V' || c1 != 'P' || c2 != '8') {
            return null;
        }
        int data = chunk + 8;
        if (c3 == ' ') {
            // Lossy: a 3-byte frame tag, the start code, then 14-bit sizes.
            if (u8(b, data + 3) != 0x9D || u8(b, data + 4) != 0x01 || u8(b, data + 5) != 0x2A) {
                return null;
            }
            return size(le16(b, data + 6) & 0x3FFF, le16(b, data + 8) & 0x3FFF);
        }
        if (c3 == 'L') {
            // Lossless: signature byte, then 14-bit width-1 and height-1.
            if (u8(b, data) != 0x2F) {
                return null;
            }
            int bits = le32(b, data + 1);
            return size((bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1);
        }
        if (c3 == 'X') {
            // Extended: 24-bit canvas width-1 and height-1 after the flags.
            return size(le24(b, data + 4) + 1, le24(b, data + 7) + 1);
        }
        return null;
    }

    private static int[] size(int w, int h) {
        return w > 0 && h > 0 ? new int[] {w, h} : null;
    }

    private static int u8(byte[] b, int i) {
        return i >= 0 && i < b.length ? b[i] & 0xFF : -1;
    }

    private static int be16(byte[] b, int i) {
        return (u8(b, i) << 8) | u8(b, i + 1);
    }

    private static int be32(byte[] b, int i) {
        return (u8(b, i) << 24) | (u8(b, i + 1) << 16) | (u8(b, i + 2) << 8) | u8(b, i + 3);
    }

    private static int le16(byte[] b, int i) {
        return u8(b, i) | (u8(b, i + 1) << 8);
    }

    private static int le24(byte[] b, int i) {
        return u8(b, i) | (u8(b, i + 1) << 8) | (u8(b, i + 2) << 16);
    }

    private static int le32(byte[] b, int i) {
        return u8(b, i) | (u8(b, i + 1) << 8) | (u8(b, i + 2) << 16) | (u8(b, i + 3) << 24);
    }

    private static int read16(byte[] b, int i, boolean little) {
        return little ? le16(b, i) : be16(b, i);
    }

    private static int read32(byte[] b, int i, boolean little) {
        return little ? le32(b, i) : be32(b, i);
    }
}
