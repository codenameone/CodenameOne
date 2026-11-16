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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Extracts the text of a page by running its content stream's text
/// operators. Text is emitted in content order; a line break is inserted
/// where the baseline moves and a space where a gap is wider than a
/// fraction of the font size. There is no reading-order analysis of
/// columns, and right-to-left text comes out in the order it was drawn.
@com.codename1.impl.SharedWithBackend
public final class PdfTextExtractor {
    private static final int MAX_FORM_DEPTH = 12;

    private final StringBuilder out = new StringBuilder(); // NOPMD AvoidStringBufferField - the builder accumulates the output for the object lifetime
    private final Map<Object, PdfFontDecoder> decoders = new HashMap<Object, PdfFontDecoder>();
    private boolean haveLast;
    private double lastX;
    private double lastY;
    private double lastSize;

    private PdfTextExtractor() {
    }

    /// The text of `page`.
    public static String extract(PdfDict page) {
        PdfTextExtractor x = new PdfTextExtractor();
        Object contents = page.get("Contents");
        byte[] data = PdfContent.concat(page.getFile(), contents);
        x.run(data, page.getDict("Resources"), new double[] {1, 0, 0, 1, 0, 0}, 0);
        String text = x.out.toString();
        if (!PdfBidi.hasRtl(text)) {
            return text;
        }
        // Text is drawn in display order; restore the reading order of
        // each line. Reordering is its own inverse on a line. In display
        // order the first strong character no longer tells the direction,
        // so it is taken from the script most of the line is in.
        StringBuilder sb = new StringBuilder(text.length());
        int start = 0;
        while (start <= text.length()) {
            int nl = text.indexOf('\n', start);
            int end = nl < 0 ? text.length() : nl;
            String line = text.substring(start, end);
            sb.append(PdfBidi.reorder(line, PdfBidi.majority(line)));
            if (nl < 0) {
                break;
            }
            sb.append('\n');
            start = nl + 1;
        }
        return sb.toString();
    }

    private void run(byte[] data, PdfDict resources, double[] baseCtm, int depth) {
        PdfLexer lx = new PdfLexer(data, 0, data.length, false);
        List<Object> ops = new ArrayList<Object>();
        double[] ctm = copy(baseCtm);
        List<double[]> stack = new ArrayList<double[]>();
        double[] tm = {1, 0, 0, 1, 0, 0};
        double[] tlm = {1, 0, 0, 1, 0, 0};
        PdfFontDecoder font = null;
        double size = 12;
        double charSpace = 0;
        double wordSpace = 0;
        double scale = 1;
        double leading = 0;
        double rise = 0;
        while (true) {
            Object o = lx.next();
            if (o == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                break;
            }
            if (!(o instanceof PdfLexer.Op)) {
                ops.add(o);
                continue;
            }
            String op = ((PdfLexer.Op) o).name;
            int n = ops.size();
            if ("q".equals(op)) {
                stack.add(copy(ctm));
            } else if ("Q".equals(op)) {
                if (!stack.isEmpty()) {
                    ctm = stack.remove(stack.size() - 1);
                }
            } else if ("cm".equals(op) && n >= 6) {
                ctm = mul(nums(ops, n - 6), ctm);
            } else if ("BT".equals(op)) {
                tm = new double[] {1, 0, 0, 1, 0, 0};
                tlm = copy(tm);
            } else if ("Tf".equals(op) && n >= 2) {
                size = num(ops.get(n - 1));
                font = decoder(resources, ops.get(n - 2));
            } else if ("Tc".equals(op) && n >= 1) {
                charSpace = num(ops.get(n - 1));
            } else if ("Tw".equals(op) && n >= 1) {
                wordSpace = num(ops.get(n - 1));
            } else if ("Tz".equals(op) && n >= 1) {
                scale = num(ops.get(n - 1)) / 100;
            } else if ("TL".equals(op) && n >= 1) {
                leading = num(ops.get(n - 1));
            } else if ("Ts".equals(op) && n >= 1) {
                rise = num(ops.get(n - 1));
            } else if (("Td".equals(op) || "TD".equals(op)) && n >= 2) {
                double tx = num(ops.get(n - 2));
                double ty = num(ops.get(n - 1));
                if ("TD".equals(op)) {
                    leading = -ty;
                }
                tlm = mul(new double[] {1, 0, 0, 1, tx, ty}, tlm);
                tm = copy(tlm);
            } else if ("Tm".equals(op) && n >= 6) {
                tlm = nums(ops, n - 6);
                tm = copy(tlm);
            } else if ("T*".equals(op)) {
                tlm = mul(new double[] {1, 0, 0, 1, 0, -leading}, tlm);
                tm = copy(tlm);
            } else if ("Tj".equals(op) || "'".equals(op) || "\"".equals(op) || "TJ".equals(op)) {
                if (!"Tj".equals(op) && !"TJ".equals(op)) {
                    if ("\"".equals(op) && n >= 3) {
                        wordSpace = num(ops.get(n - 3));
                        charSpace = num(ops.get(n - 2));
                    }
                    tlm = mul(new double[] {1, 0, 0, 1, 0, -leading}, tlm);
                    tm = copy(tlm);
                }
                if (n >= 1) {
                    if (font == null) {
                        font = new PdfFontDecoder(null);
                    }
                    Object arg = ops.get(n - 1);
                    List<?> items;
                    if (arg instanceof List) {
                        items = (List<?>) arg;
                    } else {
                        List<Object> one = new ArrayList<Object>();
                        one.add(arg);
                        items = one;
                    }
                    for (int i = 0; i < items.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                        Object it = items.get(i);
                        if (it instanceof Number) {
                            double adj = -((Number) it).doubleValue() / 1000 * size * scale;
                            tm = mul(new double[] {1, 0, 0, 1, adj, 0}, tm);
                        } else if (it instanceof PdfString) {
                            byte[] s = ((PdfString) it).bytes;
                            int p = 0;
                            while (p < s.length) {
                                int len = Math.max(1, font.codeLength(s, p));
                                int code = PdfFontDecoder.read(s, p, len);
                                double[] trm = mul(mul(new double[] {size * scale, 0, 0, size, 0, rise}, tm), ctm);
                                double fs = Math.sqrt(trm[2] * trm[2] + trm[3] * trm[3]);
                                emit(font.text(code), trm[4], trm[5], fs);
                                double w = font.width(code) / 1000 * size + charSpace;
                                if (len == 1 && code == 32) {
                                    w += wordSpace;
                                }
                                tm = mul(new double[] {1, 0, 0, 1, w * scale, 0}, tm);
                                p += len;
                                lastX = mul(tm, ctm)[4];
                            }
                        }
                    }
                }
            } else if ("Do".equals(op) && n >= 1 && depth < MAX_FORM_DEPTH) {
                Object name = ops.get(n - 1);
                PdfDict xo = resources == null ? null : resources.getDict("XObject");
                Object x = xo != null && name instanceof PdfName ? xo.get(((PdfName) name).value) : null;
                if (x instanceof PdfStream && "Form".equals(((PdfStream) x).getName("Subtype"))) {
                    PdfStream form = (PdfStream) x;
                    double[] m = {1, 0, 0, 1, 0, 0};
                    List<Object> fm = form.getArray("Matrix");
                    if (fm != null && fm.size() >= 6) {
                        m = nums(fm, 0);
                    }
                    PdfDict res = form.getDict("Resources");
                    try {
                        run(form.getData(), res != null ? res : resources, mul(m, ctm), depth + 1);
                    } catch (java.io.IOException e) {
                        // an unreadable form contributes no text
                    }
                }
            } else if ("BI".equals(op)) {
                lx.inlineImage();
            }
            ops.clear();
        }
    }

    private void emit(String text, double x, double y, double fs) {
        if (text.length() == 0) {
            return;
        }
        if (haveLast) {
            double ref = Math.max(1, Math.min(fs, lastSize));
            if (Math.abs(y - lastY) > ref * 0.5) {
                trimSpace();
                out.append('\n');
            } else if (x - lastX > ref * 0.2 && !endsWithSpace() && text.charAt(0) != ' ') {
                out.append(' ');
            }
        }
        appendExpanded(logicalCluster(text));
        haveLast = true;
        lastY = y;
        lastSize = fs;
    }

    /// The text of one glyph prepared for the line reordering in
    /// [#extract(PdfDict)]: Arabic presentation forms become the letters
    /// they show, and a right-to-left cluster of several letters (a
    /// lam-alef ligature) is reversed, so that reordering the line puts
    /// it back in reading order.
    static String logicalCluster(String text) {
        if (!PdfBidi.hasRtl(text)) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length() + 1);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String l = ArabicShaper.letters(c);
            if (l != null) {
                sb.append(l);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() < 2) {
            return sb.toString();
        }
        sb.reverse();
        return sb.toString();
    }

    /// Appends `text` with the Latin presentation ligatures (U+FB00 to
    /// U+FB06) spelled out, so that a search for "file" finds the word set
    /// with an fi ligature.
    private void appendExpanded(String text) {
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\uFB00':
                    out.append("ff");
                    break;
                case '\uFB01':
                    out.append("fi");
                    break;
                case '\uFB02':
                    out.append("fl");
                    break;
                case '\uFB03':
                    out.append("ffi");
                    break;
                case '\uFB04':
                    out.append("ffl");
                    break;
                case '\uFB05':
                case '\uFB06':
                    out.append("st");
                    break;
                default:
                    out.append(c);
            }
        }
    }

    private boolean endsWithSpace() {
        return out.length() == 0 || out.charAt(out.length() - 1) == ' ' || out.charAt(out.length() - 1) == '\n';
    }

    private void trimSpace() {
        while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
            out.setLength(out.length() - 1);
        }
    }

    private PdfFontDecoder decoder(PdfDict resources, Object name) {
        PdfDict fonts = resources == null ? null : resources.getDict("Font");
        Object f = fonts != null && name instanceof PdfName ? fonts.get(((PdfName) name).value) : null;
        if (!(f instanceof PdfDict)) {
            return new PdfFontDecoder(null);
        }
        PdfFontDecoder d = decoders.get(f);
        if (d == null) {
            d = new PdfFontDecoder((PdfDict) f);
            decoders.put(f, d);
        }
        return d;
    }

    static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    static double[] nums(List<?> l, int from) {
        double[] m = new double[6];
        for (int i = 0; i < 6; i++) {
            m[i] = num(l.get(from + i));
        }
        return m;
    }

    static double[] copy(double[] m) {
        double[] c = new double[m.length];
        System.arraycopy(m, 0, c, 0, m.length);
        return c;
    }

    /// `a x b` for PDF matrices `[a b c d e f]`.
    static double[] mul(double[] a, double[] b) {
        return new double[] {
            a[0] * b[0] + a[1] * b[2], a[0] * b[1] + a[1] * b[3],
            a[2] * b[0] + a[3] * b[2], a[2] * b[1] + a[3] * b[3],
            a[4] * b[0] + a[5] * b[2] + b[4], a[4] * b[1] + a[5] * b[3] + b[5]};
    }
}
