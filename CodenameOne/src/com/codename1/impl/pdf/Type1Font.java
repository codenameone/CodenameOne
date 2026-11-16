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

import java.util.HashMap;
import java.util.Map;

/// A Type 1 font (`FontFile`, or a PFB/PFA file): the eexec-encrypted
/// private part is decrypted and the charstrings run through a Type 1
/// interpreter. Glyph coordinates are in units of 1/1000 em unless the
/// font matrix says otherwise.
public final class Type1Font {
    private static final int MAX_SUBR_DEPTH = 10;

    private final Map<String, byte[]> charStrings = new HashMap<String, byte[]>();
    private final Map<Integer, byte[]> subrs = new HashMap<Integer, byte[]>();
    private final String[] encoding = new String[256];
    private double[] fontMatrix = {0.001, 0, 0, 0.001, 0, 0};

    /// Parses `data`, the whole font program.
    public Type1Font(byte[] data) throws java.io.IOException {
        byte[] b = data;
        if (b.length > 6 && (b[0] & 0xff) == 0x80) {
            b = unPfb(b);
        }
        int ee = PdfFile.indexOf(b, "eexec", 0, b.length);
        if (ee < 0) {
            throw new java.io.IOException("Type 1 font has no eexec section");
        }
        parseClear(b, ee);
        int p = ee + 5;
        while (p < b.length && (b[p] == '\r' || b[p] == '\n' || b[p] == ' ' || b[p] == '\t')) {
            p++;
        }
        byte[] enc = new byte[b.length - p];
        System.arraycopy(b, p, enc, 0, enc.length);
        if (isHex(enc)) {
            enc = PdfFilters.asciiHex(enc);
        }
        byte[] plain = decrypt(enc, 55665, 4);
        parsePrivate(plain);
    }

    private static boolean isHex(byte[] b) {
        int n = Math.min(4, b.length);
        for (int i = 0; i < n; i++) {
            int c = b[i] & 0xff;
            boolean h = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
            if (!h) {
                return false;
            }
        }
        return n == 4;
    }

    private static byte[] unPfb(byte[] b) {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(b.length);
        int p = 0;
        while (p + 6 <= b.length && (b[p] & 0xff) == 0x80) {
            int type = b[p + 1] & 0xff;
            if (type == 3) {
                break;
            }
            int len = (b[p + 2] & 0xff) | ((b[p + 3] & 0xff) << 8) | ((b[p + 4] & 0xff) << 16)
                    | ((b[p + 5] & 0xff) << 24);
            p += 6;
            len = Math.min(len, b.length - p);
            bo.write(b, p, len);
            p += len;
        }
        return bo.toByteArray();
    }

    static byte[] decrypt(byte[] in, int r, int skip) {
        int n = Math.max(0, in.length - skip);
        byte[] out = new byte[n];
        int key = r;
        for (int i = 0; i < in.length; i++) {
            int c = in[i] & 0xff;
            int p = c ^ (key >> 8);
            key = ((c + key) * 52845 + 22719) & 0xffff;
            if (i >= skip) {
                out[i - skip] = (byte) p;
            }
        }
        return out;
    }

    private void parseClear(byte[] b, int end) {
        PdfLexer lx = new PdfLexer(b, 0, end, false);
        while (true) {
            Object o = lx.next();
            if (o == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                break;
            }
            if (o instanceof PdfName && "FontMatrix".equals(((PdfName) o).value)) {
                Object m = lx.next();
                if (m instanceof java.util.List && ((java.util.List<?>) m).size() == 6) {
                    double[] fm = new double[6];
                    for (int i = 0; i < 6; i++) {
                        Object v = ((java.util.List<?>) m).get(i);
                        fm[i] = v instanceof Number ? ((Number) v).doubleValue() : 0;
                    }
                    fontMatrix = fm;
                }
            } else if (o instanceof PdfName && "Encoding".equals(((PdfName) o).value)) {
                Object e = lx.next();
                if (e instanceof PdfLexer.Op && "StandardEncoding".equals(((PdfLexer.Op) e).name)) {
                    System.arraycopy(PdfFontDecoder.encoding("StandardEncoding"), 0, encoding, 0, 256);
                }
            } else if (o instanceof PdfLexer.Op && "dup".equals(((PdfLexer.Op) o).name)) {
                Object code = lx.next();
                Object name = lx.next();
                if (code instanceof Integer && name instanceof PdfName) {
                    int c = ((Integer) code).intValue();
                    if (c >= 0 && c < 256) {
                        encoding[c] = ((PdfName) name).value;
                    }
                }
            }
        }
    }

    private void parsePrivate(byte[] p) {
        int lenIV = 4;
        int li = PdfFile.indexOf(p, "/lenIV", 0, p.length);
        if (li >= 0) {
            PdfLexer lx = new PdfLexer(p, li + 6, p.length, false);
            Object v = lx.next();
            if (v instanceof Integer) {
                lenIV = ((Integer) v).intValue();
            }
        }
        int si = PdfFile.indexOf(p, "/Subrs", 0, p.length);
        int ci = PdfFile.indexOf(p, "/CharStrings", 0, p.length);
        if (si >= 0) {
            int q = si + 6;
            int stop = ci > si ? ci : p.length;
            while (true) {
                int dup = PdfFile.indexOf(p, "dup ", q, stop);
                if (dup < 0) {
                    break;
                }
                int[] r = binaryEntry(p, dup + 4);
                if (r == null) {
                    break;
                }
                subrs.put(Integer.valueOf(r[0]), charstring(p, r[1], r[2], lenIV));
                q = r[1] + r[2];
            }
        }
        if (ci >= 0) {
            int q = ci + 12;
            while (q < p.length) {
                int slash = -1;
                for (int i = q; i < p.length; i++) {
                    if (p[i] == '/') {
                        slash = i;
                        break;
                    }
                    if (p[i] == 'e' && PdfFile.indexOf(p, "end", i, i + 3) == i) {
                        break;
                    }
                }
                if (slash < 0) {
                    break;
                }
                int ne = slash + 1;
                while (ne < p.length && !PdfFormat.isWhitespace(p[ne] & 0xff) && !PdfFormat.isDelimiter(p[ne] & 0xff)) {
                    ne++;
                }
                StringBuilder name = new StringBuilder();
                for (int i = slash + 1; i < ne; i++) {
                    name.append((char) (p[i] & 0xff));
                }
                int[] r = binaryLength(p, ne);
                if (r == null) {
                    q = ne;
                    continue;
                }
                charStrings.put(name.toString(), charstring(p, r[0], r[1], lenIV));
                q = r[0] + r[1];
            }
        }
    }

    /// At "<index> <len> RD ", returns {index, dataStart, len}.
    private static int[] binaryEntry(byte[] p, int from) {
        int[] idx = readInt(p, from);
        if (idx == null) {
            return null;
        }
        int[] r = binaryLength(p, idx[1]);
        return r == null ? null : new int[] {idx[0], r[0], r[1]};
    }

    /// At " <len> RD ", returns {dataStart, len}.
    private static int[] binaryLength(byte[] p, int from) {
        int[] len = readInt(p, from);
        if (len == null) {
            return null;
        }
        int q = len[1];
        while (q < p.length && p[q] == ' ') {
            q++;
        }
        while (q < p.length && p[q] != ' ') {
            q++;
        }
        q++;
        if (q + len[0] > p.length || len[0] < 0) {
            return null;
        }
        return new int[] {q, len[0]};
    }

    private static int[] readInt(byte[] p, int from) {
        int q = from;
        while (q < p.length && PdfFormat.isWhitespace(p[q] & 0xff)) {
            q++;
        }
        int start = q;
        int v = 0;
        while (q < p.length && p[q] >= '0' && p[q] <= '9') {
            v = v * 10 + (p[q] - '0');
            q++;
        }
        return q == start ? null : new int[] {v, q};
    }

    private static byte[] charstring(byte[] p, int start, int len, int lenIV) {
        byte[] enc = new byte[len];
        System.arraycopy(p, start, enc, 0, len);
        return lenIV < 0 ? enc : decrypt(enc, 4330, lenIV);
    }

    /// The font matrix.
    public double[] getFontMatrix() {
        return fontMatrix;
    }

    /// The glyph name of `code` under the font's built-in encoding.
    public String nameForCode(int code) {
        return code >= 0 && code < 256 ? encoding[code] : null;
    }

    /// True when the font defines glyph `name`.
    public boolean hasGlyph(String name) {
        return charStrings.containsKey(name);
    }

    /// Writes the outline of glyph `name` to `sink`, in glyph space.
    public void outline(String name, GlyphSink sink) {
        byte[] cs = charStrings.get(name);
        if (cs == null) {
            return;
        }
        Interp t = new Interp(sink);
        t.run(cs, 0);
        t.finish();
    }

    private final class Interp {
        private final GlyphSink sink;
        private final double[] stack = new double[32];
        private int sp;
        private final double[] ps = new double[32];
        private int psp;
        private double x;
        private double y;
        private boolean open;
        private boolean flex;
        private final double[] flexPts = new double[14];
        private int flexCount;
        private boolean ended;

        Interp(GlyphSink sink) {
            this.sink = sink;
        }

        void finish() {
            if (open) {
                sink.closePath();
                open = false;
            }
        }

        private void push(double v) {
            if (sp < stack.length) {
                stack[sp++] = v;
            }
        }

        private void moveTo(double nx, double ny) {
            x = nx;
            y = ny;
            if (flex) {
                if (flexCount < 7) {
                    flexPts[flexCount * 2] = x;
                    flexPts[flexCount * 2 + 1] = y;
                    flexCount++;
                }
                return;
            }
            finish();
            sink.moveTo(x, y);
            open = true;
        }

        private void lineTo(double nx, double ny) {
            if (!open) {
                sink.moveTo(x, y);
                open = true;
            }
            x = nx;
            y = ny;
            sink.lineTo(x, y);
        }

        private void curve(double dx1, double dy1, double dx2, double dy2, double dx3, double dy3) {
            if (!open) {
                sink.moveTo(x, y);
                open = true;
            }
            double x1 = x + dx1;
            double y1 = y + dy1;
            double x2 = x1 + dx2;
            double y2 = y1 + dy2;
            x = x2 + dx3;
            y = y2 + dy3;
            sink.curveTo(x1, y1, x2, y2, x, y);
        }

        void run(byte[] cs, int depth) {
            int p = 0;
            while (p < cs.length && !ended) {
                int v = cs[p++] & 0xff;
                if (v >= 32) {
                    if (v <= 246) {
                        push(v - 139);
                    } else if (v <= 250) {
                        push((v - 247) * 256 + (p < cs.length ? cs[p++] & 0xff : 0) + 108);
                    } else if (v <= 254) {
                        push(-(v - 251) * 256 - (p < cs.length ? cs[p++] & 0xff : 0) - 108);
                    } else if (p + 4 <= cs.length) {
                        push(((cs[p] & 0xff) << 24) | ((cs[p + 1] & 0xff) << 16) | ((cs[p + 2] & 0xff) << 8)
                                | (cs[p + 3] & 0xff));
                        p += 4;
                    }
                    continue;
                }
                switch (v) {
                    case 13:
                        if (sp >= 2) {
                            x = stack[0];
                            y = 0;
                        }
                        sp = 0;
                        break;
                    case 21:
                        if (sp >= 2) {
                            moveTo(x + stack[sp - 2], y + stack[sp - 1]);
                        }
                        sp = 0;
                        break;
                    case 22:
                        if (sp >= 1) {
                            moveTo(x + stack[sp - 1], y);
                        }
                        sp = 0;
                        break;
                    case 4:
                        if (sp >= 1) {
                            moveTo(x, y + stack[sp - 1]);
                        }
                        sp = 0;
                        break;
                    case 5:
                        if (sp >= 2) {
                            lineTo(x + stack[0], y + stack[1]);
                        }
                        sp = 0;
                        break;
                    case 6:
                        if (sp >= 1) {
                            lineTo(x + stack[0], y);
                        }
                        sp = 0;
                        break;
                    case 7:
                        if (sp >= 1) {
                            lineTo(x, y + stack[0]);
                        }
                        sp = 0;
                        break;
                    case 8:
                        if (sp >= 6) {
                            curve(stack[0], stack[1], stack[2], stack[3], stack[4], stack[5]);
                        }
                        sp = 0;
                        break;
                    case 30:
                        if (sp >= 4) {
                            curve(0, stack[0], stack[1], stack[2], stack[3], 0);
                        }
                        sp = 0;
                        break;
                    case 31:
                        if (sp >= 4) {
                            curve(stack[0], 0, stack[1], stack[2], 0, stack[3]);
                        }
                        sp = 0;
                        break;
                    case 9:
                        finish();
                        sp = 0;
                        break;
                    case 10:
                        if (sp >= 1 && depth < MAX_SUBR_DEPTH) {
                            byte[] s = subrs.get(Integer.valueOf((int) stack[--sp]));
                            if (s != null) {
                                run(s, depth + 1);
                            }
                        }
                        break;
                    case 11:
                        return;
                    case 14:
                        finish();
                        ended = true;
                        sp = 0;
                        return;
                    case 1:
                    case 3:
                        sp = 0;
                        break;
                    case 12:
                        if (p < cs.length) {
                            escape(cs[p++] & 0xff, depth);
                        }
                        break;
                    default:
                        sp = 0;
                        break;
                }
            }
        }

        private void escape(int op, int depth) {
            switch (op) {
                case 7:
                    if (sp >= 4) {
                        x = stack[0];
                        y = stack[1];
                    }
                    sp = 0;
                    break;
                case 6:
                    if (sp >= 5) {
                        seac(stack[1], stack[2], (int) stack[3], (int) stack[4], depth);
                    }
                    ended = true;
                    sp = 0;
                    break;
                case 12:
                    if (sp >= 2) {
                        double b = stack[--sp];
                        stack[sp - 1] = b == 0 ? 0 : stack[sp - 1] / b;
                    }
                    break;
                case 16:
                    othersubr();
                    break;
                case 17:
                    if (psp > 0) {
                        push(ps[--psp]);
                    }
                    break;
                case 33:
                    if (sp >= 2) {
                        x = stack[sp - 2];
                        y = stack[sp - 1];
                    }
                    sp = 0;
                    break;
                default:
                    sp = 0;
                    break;
            }
        }

        private void othersubr() {
            if (sp < 2) {
                sp = 0;
                return;
            }
            int n = (int) stack[--sp];
            int count = (int) stack[--sp];
            if (n == 1) {
                flex = true;
                flexCount = 0;
            } else if (n == 0 && flex) {
                // Flex: the first recorded point is the reference point,
                // the next six are the two curves.
                flex = false;
                if (flexCount >= 7) {
                    if (!open) {
                        sink.moveTo(flexPts[0], flexPts[1]);
                        open = true;
                    }
                    sink.curveTo(flexPts[2], flexPts[3], flexPts[4], flexPts[5], flexPts[6], flexPts[7]);
                    sink.curveTo(flexPts[8], flexPts[9], flexPts[10], flexPts[11], flexPts[12], flexPts[13]);
                    x = flexPts[12];
                    y = flexPts[13];
                }
                psp = 0;
                // flex returns the end point for setcurrentpoint
                ps[psp++] = y;
                ps[psp++] = x;
                sp = Math.max(0, sp - count);
                return;
            }
            // Other subroutines (hint replacement): their arguments come
            // back unchanged through pop.
            psp = 0;
            for (int i = 0; i < count && sp > 0; i++) {
                ps[psp++] = stack[--sp];
            }
        }

        private void seac(double adx, double ady, int bchar, int achar, int depth) {
            String[] std = PdfFontDecoder.encoding("StandardEncoding");
            finish();
            String base = bchar >= 0 && bchar < 256 ? std[bchar] : null;
            String accent = achar >= 0 && achar < 256 ? std[achar] : null;
            if (base != null && charStrings.containsKey(base)) {
                Interp t = new Interp(sink);
                t.run(charStrings.get(base), depth + 1);
                t.finish();
            }
            if (accent != null && charStrings.containsKey(accent)) {
                final double ox = adx;
                final double oy = ady;
                GlyphSink shifted = new GlyphSink() {
                    @Override
                    public void moveTo(double px, double py) {
                        sink.moveTo(px + ox, py + oy);
                    }

                    @Override
                    public void lineTo(double px, double py) {
                        sink.lineTo(px + ox, py + oy);
                    }

                    @Override
                    public void quadTo(double cx, double cy, double px, double py) {
                        sink.quadTo(cx + ox, cy + oy, px + ox, py + oy);
                    }

                    @Override
                    public void curveTo(double c1x, double c1y, double c2x, double c2y, double px, double py) {
                        sink.curveTo(c1x + ox, c1y + oy, c2x + ox, c2y + oy, px + ox, py + oy);
                    }

                    @Override
                    public void closePath() {
                        sink.closePath();
                    }
                };
                Interp t = new Interp(shifted);
                t.run(charStrings.get(accent), depth + 1);
                t.finish();
            }
        }
    }
}
