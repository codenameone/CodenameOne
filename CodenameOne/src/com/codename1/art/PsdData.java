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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A big-endian cursor over PSD bytes, with Photoshop's action descriptor parser.
final class PsdData {
    final byte[] b;
    int pos;
    final int end;

    PsdData(byte[] b, int pos, int end) {
        this.b = b;
        this.pos = pos;
        this.end = Math.min(end, b.length);
    }

    void need(long n) throws IOException {
        if (n < 0 || pos + n > end) {
            throw new IOException("PSD data is truncated");
        }
    }

    int u8() throws IOException {
        need(1);
        return b[pos++] & 0xff;
    }

    int u16() throws IOException {
        need(2);
        int v = ((b[pos] & 0xff) << 8) | (b[pos + 1] & 0xff);
        pos += 2;
        return v;
    }

    int s16() throws IOException {
        return (short) u16();
    }

    int s32() throws IOException {
        need(4);
        int v = ((b[pos] & 0xff) << 24) | ((b[pos + 1] & 0xff) << 16) | ((b[pos + 2] & 0xff) << 8) | (b[pos + 3] & 0xff);
        pos += 4;
        return v;
    }

    long u32() throws IOException {
        return s32() & 0xffffffffL;
    }

    long s64() throws IOException {
        return (u32() << 32) | u32();
    }

    /// A length, 8 bytes in a PSB where `big` is set.
    long len(boolean big) throws IOException {
        long v = big ? s64() : u32();
        if (v < 0 || v > end - pos) {
            throw new IOException("PSD length runs past the data");
        }
        return v;
    }

    double f64() throws IOException {
        return Double.longBitsToDouble(s64());
    }

    float f32() throws IOException {
        return Float.intBitsToFloat(s32());
    }

    String key() throws IOException {
        need(4);
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append((char) (b[pos + i] & 0xff));
        }
        pos += 4;
        return sb.toString();
    }

    void skip(long n) throws IOException {
        need(n);
        pos += (int) n;
    }

    String unicode() throws IOException {
        long n = u32();
        if (n > (end - pos) / 2) {
            throw new IOException("PSD string runs past the data");
        }
        StringBuilder sb = new StringBuilder((int) n);
        for (int i = 0; i < n; i++) {
            char c = (char) u16();
            if (c != 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    String pascal(int pad) throws IOException {
        int n = u8();
        need(n);
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append((char) (b[pos + i] & 0xff));
        }
        pos += n;
        int total = n + 1;
        while (total % pad != 0) {
            skip(1);
            total++;
        }
        return sb.toString();
    }

    private String classId() throws IOException {
        long n = u32();
        if (n == 0) {
            return key();
        }
        if (n > end - pos) {
            throw new IOException("PSD key runs past the data");
        }
        StringBuilder sb = new StringBuilder((int) n);
        for (int i = 0; i < n; i++) {
            sb.append((char) u8());
        }
        return sb.toString();
    }

    /// Reads a descriptor (after its version), as a map of key to value. Values are Double,
    /// Integer, Boolean, String, a nested Map, a List, or `double[]{value, unit}` for unit
    /// floats (the unit's key in the map `"#unit"` of a one-element wrapper is not kept).
    Map<String, Object> descriptor(int depth) throws IOException {
        if (depth > 32) {
            throw new IOException("PSD descriptor nests too deeply");
        }
        unicode();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("#class", classId());
        long count = u32();
        if (count > 100000) {
            throw new IOException("PSD descriptor is too large");
        }
        for (int i = 0; i < count; i++) {
            String k = classId();
            m.put(k, value(key(), depth));
        }
        return m;
    }

    private Object value(String type, int depth) throws IOException {
        if ("Objc".equals(type) || "GlbO".equals(type)) {
            return descriptor(depth + 1);
        }
        if ("VlLs".equals(type)) {
            long n = u32();
            if (n > 100000) {
                throw new IOException("PSD list is too large");
            }
            List<Object> l = new ArrayList<Object>();
            for (int i = 0; i < n; i++) {
                l.add(value(key(), depth + 1));
            }
            return l;
        }
        if ("doub".equals(type)) {
            return Double.valueOf(f64());
        }
        if ("UntF".equals(type)) {
            // The unit key (pixels, percent, degrees) is skipped: each consumer knows its unit.
            key();
            double v = f64();
            return new UnitValue(v);
        }
        if ("UnFl".equals(type)) {
            key();
            long n = u32();
            if (n > 100000) {
                throw new IOException("PSD list is too large");
            }
            double first = 0;
            for (int i = 0; i < n; i++) {
                double v = f64();
                if (i == 0) {
                    first = v;
                }
            }
            return new UnitValue(first);
        }
        if ("TEXT".equals(type)) {
            return unicode();
        }
        if ("enum".equals(type)) {
            classId();
            return classId();
        }
        if ("long".equals(type)) {
            return Integer.valueOf(s32());
        }
        if ("comp".equals(type)) {
            return Double.valueOf(s64());
        }
        if ("bool".equals(type)) {
            return Boolean.valueOf(u8() != 0);
        }
        if ("type".equals(type) || "GlbC".equals(type)) {
            unicode();
            return classId();
        }
        if ("alis".equals(type) || "tdta".equals(type) || "Pth ".equals(type)) {
            long n = u32();
            skip(n);
            return null;
        }
        if ("obj ".equals(type)) {
            long n = u32();
            for (int i = 0; i < n; i++) {
                String t = key();
                if ("prop".equals(t)) {
                    unicode();
                    classId();
                    classId();
                } else if ("Clss".equals(t)) {
                    unicode();
                    classId();
                } else if ("Enmr".equals(t)) {
                    unicode();
                    classId();
                    classId();
                    classId();
                } else if ("rele".equals(t)) {
                    unicode();
                    classId();
                    s32();
                } else if ("Idnt".equals(t) || "indx".equals(t)) {
                    s32();
                } else if ("name".equals(t)) {
                    unicode();
                    classId();
                    unicode();
                } else {
                    throw new IOException("Unknown PSD reference " + t);
                }
            }
            return null;
        }
        throw new IOException("Unknown PSD descriptor type " + type);
    }

    /// A unit float: `#Pxl` pixels, `#Prc` percent, `#Ang` degrees and so on.
    static final class UnitValue {
        final double value;

        UnitValue(double value) {
            this.value = value;
        }
    }

    static double num(Map<String, Object> m, String k, double def) {
        Object o = m == null ? null : m.get(k);
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof UnitValue) {
            return ((UnitValue) o).value;
        }
        return def;
    }

    static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object o = m == null ? null : m.get(k);
        return o instanceof Boolean ? ((Boolean) o).booleanValue() : def;
    }

    static String str(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o instanceof String ? (String) o : null;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    static List<?> list(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o instanceof List ? (List<?>) o : null;
    }

    /// An `RGBC` (or grayscale, CMYK, HSB) color descriptor as ARGB.
    static int color(Map<String, Object> c, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        if (c == null) {
            return a << 24;
        }
        double r;
        double g;
        double bl;
        if (c.containsKey("Rd  ")) {
            r = num(c, "Rd  ", 0);
            g = num(c, "Grn ", 0);
            bl = num(c, "Bl  ", 0);
        } else if (c.containsKey("redFloat")) {
            r = num(c, "redFloat", 0) * 255;
            g = num(c, "greenFloat", 0) * 255;
            bl = num(c, "blueFloat", 0) * 255;
        } else if (c.containsKey("Gry ")) {
            r = 255 - num(c, "Gry ", 0) * 2.55;
            g = r;
            bl = r;
        } else if (c.containsKey("Cyn ")) {
            double k = num(c, "Blck", 0) / 100;
            r = 255 * (1 - num(c, "Cyn ", 0) / 100) * (1 - k);
            g = 255 * (1 - num(c, "Mgnt", 0) / 100) * (1 - k);
            bl = 255 * (1 - num(c, "Ylw ", 0) / 100) * (1 - k);
        } else if (c.containsKey("H   ")) {
            double h = num(c, "H   ", 0) / 360;
            double s = num(c, "Strt", 0) / 100;
            double v = num(c, "Brgh", 0) / 100;
            double l = v * (1 - s / 2);
            double sl = l == 0 || l == 1 ? 0 : (v - l) / Math.min(l, 1 - l);
            float[] rgb = Colors.hslToRgb((float) h, (float) sl, (float) l);
            r = rgb[0] * 255;
            g = rgb[1] * 255;
            bl = rgb[2] * 255;
        } else {
            r = 0;
            g = 0;
            bl = 0;
        }
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
    }

    private static int clamp(double v) {
        int i = (int) Math.round(v);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }
}
