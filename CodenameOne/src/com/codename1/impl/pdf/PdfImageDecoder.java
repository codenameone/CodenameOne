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
package com.codename1.impl.pdf;

import java.util.List;

/// Turns an image XObject (or inline image) into ARGB pixels. JPEG data
/// is decoded by the device; JPEG 2000, JBIG2 and CCITT fax data by
/// [JpxDecoder], [Jbig2Decoder] and [CcittDecoder].
public final class PdfImageDecoder {
    /// Images above this many pixels are not decoded, to bound memory.
    public static final int MAX_PIXELS = 25 * 1000 * 1000;

    /// The decoded width.
    public int width;
    /// The decoded height.
    public int height;
    /// The pixels, row 0 at the top.
    public int[] argb;

    private PdfImageDecoder() {
        argb = null;
    }

    private static Object get(PdfDict s, String full, String abbr) {
        Object v = s.get(full);
        return v != null ? v : s.get(abbr);
    }

    private static int getInt(PdfDict s, String full, String abbr, int def) {
        Object v = get(s, full, abbr);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    /// Decodes `s`; `fillRgb` colors a stencil mask. Null when the image
    /// cannot be decoded.
    public static PdfImageDecoder decode(PdfStream s, PdfDict resources, PdfDevice device, int fillRgb) {
        try {
            return decode(s, resources, device, fillRgb, 0);
        } catch (java.io.IOException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static PdfImageDecoder decode(PdfStream s, PdfDict resources, PdfDevice device, int fillRgb, int depth)
            throws java.io.IOException {
        int w = getInt(s, "Width", "W", 0);
        int h = getInt(s, "Height", "H", 0);
        if (w <= 0 || h <= 0 || (long) w * h > MAX_PIXELS) {
            return null;
        }
        PdfFile file = s.getFile();
        boolean mask = Boolean.TRUE.equals(get(s, "ImageMask", "IM"));
        int bpc = mask ? 1 : getInt(s, "BitsPerComponent", "BPC", 8);
        List<String> filters = PdfFilters.names(s);
        String codec = null;
        for (int i = 0; i < filters.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            if (PdfFilters.isImageCodec(filters.get(i))) {
                codec = filters.get(i);
                break;
            }
        }
        byte[] data = s.getData();
        PdfImageDecoder out = new PdfImageDecoder();
        Object csObj = get(s, "ColorSpace", "CS");
        PdfColorSpace cs = mask ? PdfColorSpace.DEVICE_GRAY
                : csObj == null ? null : PdfColorSpace.resolve(file, csObj, resources);
        if ("DCTDecode".equals(codec)) {
            int[] size = new int[2];
            int[] px = device.decodeJpeg(data, size);
            if (px == null) {
                return null;
            }
            out.width = size[0];
            out.height = size[1];
            out.argb = px;
            double[] dec = nums(get(s, "Decode", "D"));
            if (dec != null && dec.length >= 2 && dec[0] == 1 && dec[1] == 0) {
                for (int i = 0; i < px.length; i++) {
                    px[i] = (px[i] & 0xff000000) | (~px[i] & 0xffffff);
                }
            }
        } else if ("CCITTFaxDecode".equals(codec) || "JBIG2Decode".equals(codec)) {
            int ci = filters.indexOf(codec);
            byte[] bits;
            if ("CCITTFaxDecode".equals(codec)) {
                bits = CcittDecoder.decode(data, PdfFilters.params(s, ci), w, h);
            } else {
                bits = Jbig2Decoder.decode(data, PdfFilters.params(s, ci), w, h);
            }
            if (bits == null) {
                return null;
            }
            out.width = w;
            out.height = h;
            out.argb = samples(bits, w, h, 1, mask ? PdfColorSpace.DEVICE_GRAY
                    : cs == null ? PdfColorSpace.DEVICE_GRAY : cs, nums(get(s, "Decode", "D")), mask, fillRgb, s);
        } else if ("JPXDecode".equals(codec)) {
            JpxDecoder j = JpxDecoder.decode(data);
            if (j == null) {
                return null;
            }
            out.width = j.width;
            out.height = j.height;
            out.argb = j.argb(cs, s.has("SMaskInData") && s.getInt("SMaskInData", 0) != 0);
            if (out.argb == null) {
                return null;
            }
        } else if (codec != null) {
            return null;
        } else {
            if (cs == null) {
                cs = PdfColorSpace.DEVICE_GRAY;
            }
            out.width = w;
            out.height = h;
            out.argb = samples(data, w, h, bpc, cs, nums(get(s, "Decode", "D")), mask, fillRgb, s);
        }
        if (depth == 0 && !mask) {
            Object sm = s.get("SMask");
            if (sm instanceof PdfStream) {
                PdfImageDecoder a = decode((PdfStream) sm, resources, device, 0, depth + 1);
                if (a != null) {
                    applyAlpha(out, a, false);
                }
            } else {
                Object m = s.get("Mask");
                if (m instanceof PdfStream) {
                    PdfImageDecoder a = decode((PdfStream) m, resources, device, 0, depth + 1);
                    if (a != null) {
                        // A stencil mask: painted samples (opaque) are where
                        // the image shows.
                        applyAlpha(out, a, true);
                    }
                }
            }
        }
        return out;
    }

    private static void applyAlpha(PdfImageDecoder img, PdfImageDecoder a, boolean useAlpha) {
        int w = img.width;
        int h = img.height;
        int[] px = img.argb;
        for (int y = 0; y < h; y++) {
            int ay = (int) ((long) y * a.height / h);
            for (int x = 0; x < w; x++) {
                int ax = (int) ((long) x * a.width / w);
                int v = a.argb[ay * a.width + ax];
                int alpha = useAlpha ? (v >>> 24) : (v >> 8) & 0xff;
                int p = px[y * w + x];
                int na = ((p >>> 24) * alpha + 127) / 255;
                px[y * w + x] = (na << 24) | (p & 0xffffff);
            }
        }
    }

    private static double[] nums(Object o) {
        if (!(o instanceof List)) {
            return null;
        }
        List<?> l = (List<?>) o;
        double[] v = new double[l.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = l.get(i) instanceof Number ? ((Number) l.get(i)).doubleValue() : 0;
        }
        return v;
    }

    private static int[] samples(byte[] d, int w, int h, int bpc, PdfColorSpace cs, double[] decode, boolean mask,
            int fillRgb, PdfStream s) {
        int n = cs.getComponents();
        if (bpc != 1 && bpc != 2 && bpc != 4 && bpc != 8 && bpc != 16) {
            bpc = 8;
        }
        int stride = (w * n * bpc + 7) / 8;
        int[] px = new int[w * h];
        // 16-bit samples are read by their high byte.
        int max = bpc == 16 ? 255 : (1 << bpc) - 1;
        boolean indexed = cs.getKind() == PdfColorSpace.INDEXED;
        double[] dmin = new double[n];
        double[] dmax = new double[n];
        for (int k = 0; k < n; k++) {
            dmin[k] = decode != null && decode.length > k * 2 + 1 ? decode[k * 2] : 0;
            dmax[k] = decode != null && decode.length > k * 2 + 1 ? decode[k * 2 + 1] : indexed ? max : 1;
        }
        int[] colorKey = null;
        Object m = s.get("Mask");
        if (m instanceof List) {
            double[] k = nums(m);
            if (k != null && k.length >= n * 2) {
                colorKey = new int[n * 2];
                for (int i = 0; i < colorKey.length; i++) {
                    colorKey[i] = (int) k[i];
                }
            }
        }
        boolean fastRgb = bpc == 8 && cs.getKind() == PdfColorSpace.RGB && decode == null && colorKey == null;
        boolean fastGray = bpc == 8 && cs.getKind() == PdfColorSpace.GRAY && decode == null && colorKey == null
                && !mask;
        // Indexed images and small-depth images repeat colors heavily:
        // convert each distinct sample value once.
        int[] lut = null;
        if (n == 1 && bpc <= 8 && !mask) {
            lut = new int[max + 1];
            double[] c = new double[1];
            for (int v = 0; v <= max; v++) {
                c[0] = dmin[0] + v * (dmax[0] - dmin[0]) / max;
                lut[v] = 0xff000000 | cs.toRgb(c);
            }
        }
        double[] comp = new double[n];
        int[] raw = new int[n];
        for (int y = 0; y < h; y++) {
            int row = y * stride;
            if (row >= d.length) {
                break;
            }
            for (int x = 0; x < w; x++) {
                int o = y * w + x;
                if (fastRgb) {
                    int p = row + x * 3;
                    if (p + 2 >= d.length) {
                        break;
                    }
                    px[o] = 0xff000000 | ((d[p] & 0xff) << 16) | ((d[p + 1] & 0xff) << 8) | (d[p + 2] & 0xff);
                    continue;
                }
                if (fastGray) {
                    int p = row + x;
                    if (p >= d.length) {
                        break;
                    }
                    int g = d[p] & 0xff;
                    px[o] = 0xff000000 | (g << 16) | (g << 8) | g;
                    continue;
                }
                for (int k = 0; k < n; k++) {
                    raw[k] = sample(d, row, (x * n + k), bpc);
                }
                if (mask) {
                    boolean paint = raw[0] == 0;
                    if (decode != null && decode.length >= 2 && decode[0] == 1) {
                        paint = !paint;
                    }
                    px[o] = paint ? 0xff000000 | fillRgb : 0;
                    continue;
                }
                if (lut != null) {
                    px[o] = lut[raw[0] & max];
                } else {
                    for (int k = 0; k < n; k++) {
                        comp[k] = dmin[k] + raw[k] * (dmax[k] - dmin[k]) / max;
                    }
                    px[o] = 0xff000000 | cs.toRgb(comp);
                }
                if (colorKey != null) {
                    boolean inside = true;
                    for (int k = 0; k < n && inside; k++) {
                        inside = raw[k] >= colorKey[k * 2] && raw[k] <= colorKey[k * 2 + 1];
                    }
                    if (inside) {
                        px[o] = 0;
                    }
                }
            }
        }
        return px;
    }

    private static int sample(byte[] d, int row, int index, int bpc) {
        if (bpc == 8) {
            int p = row + index;
            return p < d.length ? d[p] & 0xff : 0;
        }
        if (bpc == 16) {
            int p = row + index * 2;
            return p < d.length ? d[p] & 0xff : 0;
        }
        int bit = index * bpc;
        int p = row + (bit >> 3);
        if (p >= d.length) {
            return 0;
        }
        int shift = 8 - bpc - (bit & 7);
        return ((d[p] & 0xff) >> shift) & ((1 << bpc) - 1);
    }
}
