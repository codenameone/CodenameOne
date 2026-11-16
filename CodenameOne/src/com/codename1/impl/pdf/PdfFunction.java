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

/// A PDF function (types 0, 2, 3 and 4), used by shadings and by
/// Separation and DeviceN color spaces.
public final class PdfFunction {
    private static final int MAX_DEPTH = 8;

    private final int type;
    private final double[] domain;
    private final double[] range;
    // type 0
    private int[] size;
    private int bps;
    private double[] encode;
    private double[] decode;
    private byte[] samples;
    // type 2
    private double[] c0;
    private double[] c1;
    private double n;
    // type 3
    private PdfFunction[] parts;
    private double[] bounds;
    // type 4
    private Object[] program;

    private PdfFunction(int type, double[] domain, double[] range) {
        this.type = type;
        this.domain = domain;
        this.range = range;
    }

    /// Parses function `o` (a dictionary or stream); null when it is not
    /// one this reader understands.
    public static PdfFunction parse(Object o) {
        return parse(o, 0);
    }

    /// Parses an array of functions, one per output, or a single function.
    public static PdfFunction[] parseAll(PdfFile file, Object o) {
        Object v = file != null ? file.resolve(o) : o;
        if (v instanceof List) {
            List<?> l = (List<?>) v;
            PdfFunction[] out = new PdfFunction[l.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = parse(file != null ? file.resolve(l.get(i)) : l.get(i));
                if (out[i] == null) {
                    return null;
                }
            }
            return out;
        }
        PdfFunction f = parse(v);
        return f == null ? null : new PdfFunction[] {f};
    }

    private static PdfFunction parse(Object o, int depth) {
        if (!(o instanceof PdfDict) || depth > MAX_DEPTH) {
            return null;
        }
        PdfDict d = (PdfDict) o;
        int type = d.getInt("FunctionType", -1);
        PdfFunction f = new PdfFunction(type, nums(d.getArray("Domain")), nums(d.getArray("Range")));
        try {
            if (type == 0 && d instanceof PdfStream) {
                f.size = ints(d.getArray("Size"));
                f.bps = d.getInt("BitsPerSample", 8);
                f.encode = nums(d.getArray("Encode"));
                f.decode = nums(d.getArray("Decode"));
                f.samples = ((PdfStream) d).getData();
                if (f.size == null || f.range == null) {
                    return null;
                }
            } else if (type == 2) {
                f.c0 = nums(d.getArray("C0"));
                f.c1 = nums(d.getArray("C1"));
                if (f.c0 == null) {
                    f.c0 = new double[] {0};
                }
                if (f.c1 == null) {
                    f.c1 = new double[] {1};
                }
                f.n = d.getNumber("N", 1);
            } else if (type == 3) {
                List<Object> fs = d.getArray("Functions");
                if (fs == null) {
                    return null;
                }
                f.parts = new PdfFunction[fs.size()];
                for (int i = 0; i < fs.size(); i++) {
                    Object fo = d.getFile() != null ? d.getFile().resolve(fs.get(i)) : fs.get(i);
                    f.parts[i] = parse(fo, depth + 1);
                    if (f.parts[i] == null) {
                        return null;
                    }
                }
                f.bounds = nums(d.getArray("Bounds"));
                f.encode = nums(d.getArray("Encode"));
                if (f.bounds == null) {
                    f.bounds = new double[0];
                }
            } else if (type == 4 && d instanceof PdfStream) {
                f.program = PdfCalculator.compile(((PdfStream) d).getData());
                if (f.program == null) {
                    return null;
                }
            } else {
                return null;
            }
        } catch (java.io.IOException e) {
            return null;
        }
        if (f.domain == null) {
            return null;
        }
        return f;
    }

    private static double[] nums(List<Object> l) {
        if (l == null) {
            return null;
        }
        double[] v = new double[l.size()];
        for (int i = 0; i < v.length; i++) {
            Object o = l.get(i);
            v[i] = o instanceof Number ? ((Number) o).doubleValue() : 0;
        }
        return v;
    }

    private static int[] ints(List<Object> l) {
        if (l == null) {
            return null;
        }
        int[] v = new int[l.size()];
        for (int i = 0; i < v.length; i++) {
            Object o = l.get(i);
            v[i] = o instanceof Number ? ((Number) o).intValue() : 1;
        }
        return v;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /// Evaluates the function at `in`.
    public double[] eval(double[] in) {
        double[] x = new double[domain.length / 2];
        for (int i = 0; i < x.length; i++) {
            x[i] = clamp(i < in.length ? in[i] : 0, domain[i * 2], domain[i * 2 + 1]);
        }
        double[] out;
        switch (type) {
            case 0:
                out = sampled(x);
                break;
            case 2: {
                double t = x[0];
                double p = n == 1 ? t : com.codename1.util.MathUtil.pow(t, n);
                int m = Math.min(c0.length, c1.length);
                out = new double[m];
                for (int i = 0; i < m; i++) {
                    out[i] = c0[i] + p * (c1[i] - c0[i]);
                }
                break;
            }
            case 3:
                out = stitched(x[0]);
                break;
            default:
                out = PdfCalculator.run(program, x, range == null ? 1 : range.length / 2);
                break;
        }
        if (range != null) {
            for (int i = 0; i < out.length && i * 2 + 1 < range.length; i++) {
                out[i] = clamp(out[i], range[i * 2], range[i * 2 + 1]);
            }
        }
        return out;
    }

    private double[] stitched(double t) {
        int k = 0;
        while (k < bounds.length && t >= bounds[k]) {
            k++;
        }
        k = Math.min(k, parts.length - 1);
        double lo = k == 0 ? domain[0] : bounds[k - 1];
        double hi = k == bounds.length ? domain[1] : bounds[k];
        double e0 = encode != null && encode.length > k * 2 + 1 ? encode[k * 2] : 0;
        double e1 = encode != null && encode.length > k * 2 + 1 ? encode[k * 2 + 1] : 1;
        double u = hi == lo ? e0 : e0 + (t - lo) * (e1 - e0) / (hi - lo);
        return parts[k].eval(new double[] {u});
    }

    private double[] sampled(double[] x) {
        int m = x.length;
        int outs = range.length / 2;
        // Multilinear interpolation would be exact; nearest sample along
        // every axis but the first, and linear along the first, is what
        // shadings and tint transforms need in practice.
        double[] e = new double[m];
        for (int i = 0; i < m; i++) {
            double e0 = encode != null && encode.length > i * 2 + 1 ? encode[i * 2] : 0;
            double e1 = encode != null && encode.length > i * 2 + 1 ? encode[i * 2 + 1] : size[i] - 1;
            double d0 = domain[i * 2];
            double d1 = domain[i * 2 + 1];
            double v = d1 == d0 ? e0 : e0 + (x[i] - d0) * (e1 - e0) / (d1 - d0);
            e[i] = clamp(v, 0, size[i] - 1);
        }
        int base = 0;
        int stride = 1;
        for (int i = 1; i < m; i++) {
            stride *= size[i - 1];
            base += (int) Math.round(e[i]) * stride;
        }
        int i0 = (int) Math.floor(e[0]);
        int i1 = Math.min(i0 + 1, size[0] - 1);
        double frac = e[0] - i0;
        double[] out = new double[outs];
        double max = (1L << bps) - 1;
        for (int j = 0; j < outs; j++) {
            double s0 = sample((base + i0) * outs + j);
            double s1 = sample((base + i1) * outs + j);
            double s = s0 + (s1 - s0) * frac;
            double r0 = decode != null && decode.length > j * 2 + 1 ? decode[j * 2] : range[j * 2];
            double r1 = decode != null && decode.length > j * 2 + 1 ? decode[j * 2 + 1] : range[j * 2 + 1];
            out[j] = r0 + s * (r1 - r0) / max;
        }
        return out;
    }

    private double sample(int index) {
        long bit = (long) index * bps;
        int byteIndex = (int) (bit >> 3);
        if (bps == 8) {
            return byteIndex < samples.length ? samples[byteIndex] & 0xff : 0;
        }
        if (bps == 16) {
            return byteIndex + 1 < samples.length
                    ? ((samples[byteIndex] & 0xff) << 8) | (samples[byteIndex + 1] & 0xff) : 0;
        }
        long v = 0;
        int need = bps;
        long pos = bit;
        while (need > 0) {
            int bi = (int) (pos >> 3);
            int b = bi < samples.length ? samples[bi] & 0xff : 0;
            int off = (int) (pos & 7);
            int take = Math.min(8 - off, need);
            v = (v << take) | ((b >> (8 - off - take)) & ((1 << take) - 1));
            need -= take;
            pos += take;
        }
        return v;
    }
}
