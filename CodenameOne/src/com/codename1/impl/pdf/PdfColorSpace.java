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

/// A color space reduced to what the renderer needs: how many components
/// it takes and how they become RGB. ICC profiles are not applied; an
/// ICC-based space is read as the device space with the same number of
/// components, which is what most viewers do for display.
public final class PdfColorSpace {
    /// The kinds.
    public static final int GRAY = 0;
    /// DeviceRGB, CalRGB and three-component ICC.
    public static final int RGB = 1;
    /// DeviceCMYK and four-component ICC.
    public static final int CMYK = 2;
    /// Indexed.
    public static final int INDEXED = 3;
    /// Separation and DeviceN, through the tint transform.
    public static final int TINT = 4;
    /// Pattern.
    public static final int PATTERN = 5;
    /// Lab.
    public static final int LAB = 6;

    private static final int MAX_DEPTH = 8;

    /// The shared DeviceGray instance.
    public static final PdfColorSpace DEVICE_GRAY = new PdfColorSpace(GRAY, 1);
    /// The shared DeviceRGB instance.
    public static final PdfColorSpace DEVICE_RGB = new PdfColorSpace(RGB, 3);
    /// The shared DeviceCMYK instance.
    public static final PdfColorSpace DEVICE_CMYK = new PdfColorSpace(CMYK, 4);

    private final int kind;
    private final int components;
    private PdfColorSpace base;
    private byte[] lookup;
    private int hival;
    private PdfFunction[] tint;
    private boolean none;
    private double[] whitePoint = {0.9505, 1, 1.089};
    private double[] labRange = {-100, 100, -100, 100};

    private PdfColorSpace(int kind, int components) {
        this.kind = kind;
        this.components = components;
        this.base = null;
    }

    /// The kind of space.
    public int getKind() {
        return kind;
    }

    /// The number of components a color in this space has.
    public int getComponents() {
        return components;
    }

    /// The base space of an Indexed or Pattern space, or null.
    public PdfColorSpace getBase() {
        return base;
    }

    /// The highest index of an Indexed space.
    public int getHival() {
        return hival;
    }

    /// Resolves color space `o` (a name or an array), looking names up in
    /// `resources`. Anything unknown reads as DeviceGray.
    public static PdfColorSpace resolve(PdfFile file, Object o, PdfDict resources) {
        return resolve(file, o, resources, 0);
    }

    private static PdfColorSpace resolve(PdfFile file, Object o, PdfDict resources, int depth) {
        Object v = file != null ? file.resolve(o) : o;
        if (depth > MAX_DEPTH) {
            return DEVICE_GRAY;
        }
        if (v instanceof PdfName) {
            String n = ((PdfName) v).value;
            if ("DeviceGray".equals(n) || "G".equals(n) || "CalGray".equals(n)) {
                return DEVICE_GRAY;
            }
            if ("DeviceRGB".equals(n) || "RGB".equals(n) || "CalRGB".equals(n)) {
                return DEVICE_RGB;
            }
            if ("DeviceCMYK".equals(n) || "CMYK".equals(n)) {
                return DEVICE_CMYK;
            }
            if ("Pattern".equals(n)) {
                return new PdfColorSpace(PATTERN, 0);
            }
            PdfDict cs = resources == null ? null : resources.getDict("ColorSpace");
            Object named = cs == null ? null : cs.getRaw(n);
            if (named != null) {
                return resolve(file, named, null, depth + 1);
            }
            return DEVICE_GRAY;
        }
        if (!(v instanceof List) || ((List<?>) v).isEmpty()) {
            return DEVICE_GRAY;
        }
        List<?> a = (List<?>) v;
        Object head = file != null ? file.resolve(a.get(0)) : a.get(0);
        String n = head instanceof PdfName ? ((PdfName) head).value : "";
        Object arg1 = a.size() > 1 ? (file != null ? file.resolve(a.get(1)) : a.get(1)) : null;
        if ("CalGray".equals(n)) {
            return DEVICE_GRAY;
        }
        if ("CalRGB".equals(n)) {
            return DEVICE_RGB;
        }
        if ("ICCBased".equals(n)) {
            if (arg1 instanceof PdfDict) {
                PdfDict d = (PdfDict) arg1;
                int count = d.getInt("N", 3);
                if (count == 1) {
                    return DEVICE_GRAY;
                }
                if (count == 4) {
                    return DEVICE_CMYK;
                }
                if (count == 3) {
                    return DEVICE_RGB;
                }
                Object alt = d.getRaw("Alternate");
                if (alt != null) {
                    return resolve(file, alt, resources, depth + 1);
                }
            }
            return DEVICE_RGB;
        }
        if ("Lab".equals(n)) {
            PdfColorSpace lab = new PdfColorSpace(LAB, 3);
            if (arg1 instanceof PdfDict) {
                double[] wp = nums(((PdfDict) arg1).getArray("WhitePoint"));
                double[] r = nums(((PdfDict) arg1).getArray("Range"));
                if (wp != null && wp.length == 3) {
                    lab.whitePoint = wp;
                }
                if (r != null && r.length == 4) {
                    lab.labRange = r;
                }
            }
            return lab;
        }
        if ("Indexed".equals(n) || "I".equals(n)) {
            PdfColorSpace s = new PdfColorSpace(INDEXED, 1);
            s.base = resolve(file, arg1, resources, depth + 1);
            Object hv = a.size() > 2 ? (file != null ? file.resolve(a.get(2)) : a.get(2)) : null;
            s.hival = Math.max(0, Math.min(255, hv instanceof Number ? ((Number) hv).intValue() : 0)); // the spec caps hival at 255
            Object lk = a.size() > 3 ? (file != null ? file.resolve(a.get(3)) : a.get(3)) : null;
            if (lk instanceof PdfString) {
                s.lookup = ((PdfString) lk).bytes;
            } else if (lk instanceof PdfStream) {
                try {
                    s.lookup = ((PdfStream) lk).getData();
                } catch (java.io.IOException e) {
                    s.lookup = new byte[0];
                }
            } else {
                s.lookup = new byte[0];
            }
            return s;
        }
        if ("Pattern".equals(n)) {
            PdfColorSpace s = new PdfColorSpace(PATTERN, 0);
            if (arg1 != null) {
                s.base = resolve(file, arg1, resources, depth + 1);
            }
            return s;
        }
        if ("Separation".equals(n) || "DeviceN".equals(n)) {
            int count = 1;
            boolean allNone = false;
            if ("DeviceN".equals(n) && arg1 instanceof List) {
                count = ((List<?>) arg1).size();
            } else if (arg1 instanceof PdfName) {
                allNone = "None".equals(((PdfName) arg1).value);
            }
            PdfColorSpace s = new PdfColorSpace(TINT, Math.max(1, count));
            s.none = allNone;
            s.base = resolve(file, a.size() > 2 ? a.get(2) : null, resources, depth + 1);
            s.tint = a.size() > 3 ? PdfFunction.parseAll(file, a.get(3)) : null;
            return s;
        }
        return DEVICE_GRAY;
    }

    private static double[] nums(List<Object> l) {
        if (l == null) {
            return null;
        }
        double[] v = new double[l.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = l.get(i) instanceof Number ? ((Number) l.get(i)).doubleValue() : 0;
        }
        return v;
    }

    /// The initial color of the space, as components.
    public double[] initial() {
        if (kind == CMYK) {
            return new double[] {0, 0, 0, 1};
        }
        if (kind == TINT) {
            double[] c = new double[components];
            for (int i = 0; i < c.length; i++) {
                c[i] = 1;
            }
            return c;
        }
        return new double[Math.max(1, components)];
    }

    /// Converts components (each 0..1, an index for Indexed) to `0xRRGGBB`.
    public int toRgb(double[] c) {
        switch (kind) {
            case GRAY: {
                int g = clamp(c.length > 0 ? c[0] : 0);
                return (g << 16) | (g << 8) | g;
            }
            case RGB:
                return (clamp(at(c, 0)) << 16) | (clamp(at(c, 1)) << 8) | clamp(at(c, 2));
            case CMYK:
                return cmyk(at(c, 0), at(c, 1), at(c, 2), at(c, 3));
            case LAB:
                return lab(at(c, 0), at(c, 1), at(c, 2));
            case INDEXED: {
                int i = (int) Math.round(at(c, 0));
                if (i < 0) {
                    i = 0;
                }
                if (i > hival) {
                    i = hival;
                }
                return base.indexed(lookup, i);
            }
            case TINT: {
                if (none) {
                    return 0xffffff;
                }
                if (tint == null) {
                    int g = clamp(1 - at(c, 0));
                    return (g << 16) | (g << 8) | g;
                }
                double[] out;
                if (tint.length == 1) {
                    out = tint[0].eval(c);
                } else {
                    out = new double[tint.length];
                    for (int i = 0; i < tint.length; i++) {
                        out[i] = tint[i].eval(c)[0];
                    }
                }
                return base.toRgb(out);
            }
            default:
                return 0;
        }
    }

    private int indexed(byte[] table, int i) {
        int n = components;
        double[] c = new double[n];
        for (int k = 0; k < n; k++) {
            int p = i * n + k;
            c[k] = p < table.length ? (table[p] & 0xff) / 255.0 : 0;
        }
        if (kind == LAB) {
            c[0] *= 100;
            c[1] = labRange[0] + c[1] * (labRange[1] - labRange[0]);
            c[2] = labRange[2] + c[2] * (labRange[3] - labRange[2]);
        }
        return toRgb(c);
    }

    private static double at(double[] c, int i) {
        return i < c.length ? c[i] : 0;
    }

    private static int clamp(double v) {
        int i = (int) (v * 255 + 0.5);
        return i < 0 ? 0 : i > 255 ? 255 : i;
    }

    /// A naive CMYK to RGB conversion, as viewers without color
    /// management use.
    public static int cmyk(double c, double m, double y, double k) {
        int r = clamp((1 - c) * (1 - k));
        int g = clamp((1 - m) * (1 - k));
        int b = clamp((1 - y) * (1 - k));
        return (r << 16) | (g << 8) | b;
    }

    private int lab(double l, double a, double b) {
        double fy = (l + 16) / 116;
        double fx = fy + a / 500;
        double fz = fy - b / 200;
        double x = whitePoint[0] * labInv(fx);
        double y = whitePoint[1] * labInv(fy);
        double z = whitePoint[2] * labInv(fz);
        double r = 3.2406 * x - 1.5372 * y - 0.4986 * z;
        double g = -0.9689 * x + 1.8758 * y + 0.0415 * z;
        double bl = 0.0557 * x - 0.2040 * y + 1.0570 * z;
        return (clamp(gamma(r)) << 16) | (clamp(gamma(g)) << 8) | clamp(gamma(bl));
    }

    private static double labInv(double t) {
        return t > 6.0 / 29 ? t * t * t : 3 * (6.0 / 29) * (6.0 / 29) * (t - 4.0 / 29);
    }

    private static double gamma(double v) {
        if (v <= 0) {
            return 0;
        }
        return v <= 0.0031308 ? 12.92 * v : 1.055 * com.codename1.util.MathUtil.pow(v, 1 / 2.4) - 0.055;
    }
}
