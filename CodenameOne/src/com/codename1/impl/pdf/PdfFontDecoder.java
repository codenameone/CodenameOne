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
import java.util.List;
import java.util.Map;

/// Decodes the strings shown with one font: how many bytes each code
/// takes, the code's text, and its advance width in text space units
/// (1/1000 of the font size).
///
/// Text comes from the font's `ToUnicode` CMap when it has one, otherwise
/// from its simple encoding (base encoding plus `Differences`) and the
/// Adobe glyph list. A composite (`Type0`) font without a usable map yields
/// no text; there is nothing else to derive it from.
@com.codename1.impl.SharedWithBackend
public final class PdfFontDecoder {
    private static Map<String, String> glyphList;
    // Caches filled on first use and published whole, so a race only
    // computes one twice.
    private static final String[][] ENCODINGS = new String[5][];

    private final boolean composite;
    /// For a composite font without a `ToUnicode` map: the Adobe
    /// collection its CIDs belong to when the codes are CIDs (an Identity
    /// encoding), else null.
    private String ordering;
    /// True when the composite font's codes are UTF-16 (a `Uni...UCS2` or
    /// `Uni...UTF16` CMap).
    private boolean unicodeCodes;
    private boolean vertical;
    private double dw2Vy = 880;
    private double dw2W1 = -1000;
    /// CID to {w1y, vx, vy} from `W2`, for vertical writing.
    private Map<Integer, double[]> verticalMetrics;
    private final String[] simpleNames = new String[256];
    private final Map<Integer, String> toUnicode = new HashMap<Integer, String>();
    private int[] codeSpaceLengths;
    private final Map<Integer, Double> widths = new HashMap<Integer, Double>();
    private double defaultWidth;
    private final boolean standard;
    private final int standardIndex;

    /// The decoder for font dictionary `font` (null gives Helvetica).
    public PdfFontDecoder(PdfDict font) {
        String subtype = font == null ? "Type1" : font.getName("Subtype");
        composite = "Type0".equals(subtype);
        String base = font == null ? "Helvetica" : font.getName("BaseFont");
        int std = standardIndex(base);
        standardIndex = std;
        standard = std >= 0 && (font == null || !font.has("Widths"));
        if (composite) {
            readComposite(font);
        } else {
            readSimple(font, std);
        }
        if (font != null) {
            Object tu = font.get("ToUnicode");
            if (tu instanceof PdfStream) {
                try {
                    readCMap(((PdfStream) tu).getData());
                } catch (java.io.IOException e) {
                    // fall back to the encoding
                } catch (RuntimeException e) {
                    // fall back to the encoding
                }
            }
        }
    }

    /// The standard-14 index of a base font name, ignoring a subset tag, or -1.
    public static int standardIndex(String base) {
        if (base == null) {
            return -1;
        }
        int plus = base.indexOf('+');
        if (plus == 6) {
            base = base.substring(7);
        }
        String[] names = StandardFontMetrics.NAMES;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(base)) {
                return i;
            }
        }
        if ("Arial".equals(base) || "ArialMT".equals(base)) {
            return 4;
        }
        if (base.startsWith("Arial,Bold") || "Arial-BoldMT".equals(base)) {
            return 5;
        }
        if ("TimesNewRoman".equals(base) || "TimesNewRomanPSMT".equals(base)) {
            return 0;
        }
        if ("CourierNew".equals(base) || "CourierNewPSMT".equals(base)) {
            return 8;
        }
        return -1;
    }

    /// True for a composite font.
    public boolean isComposite() {
        return composite;
    }

    /// The standard-14 index this font resolves to, or -1.
    public int getStandardIndex() {
        return standardIndex;
    }

    /// The glyph name of single-byte code `code`, or null.
    public String glyphName(int code) {
        return code >= 0 && code < 256 ? simpleNames[code] : null;
    }

    private void readSimple(PdfDict font, int std) {
        String[] base = encoding(std == 12 ? "Symbol" : std == 13 ? "ZapfDingbats" : "StandardEncoding");
        Object enc = font == null ? null : font.get("Encoding");
        boolean symbolic = font != null && isSymbolic(font);
        if (enc instanceof PdfName) {
            base = encoding(((PdfName) enc).value);
        } else if (enc instanceof PdfDict) {
            String be = ((PdfDict) enc).getName("BaseEncoding");
            if (be != null) {
                base = encoding(be);
            } else if (!symbolic && std < 12) {
                base = encoding("StandardEncoding");
            }
        } else if (!symbolic && std < 0 && font != null && "TrueType".equals(font.getName("Subtype"))) {
            base = encoding("WinAnsiEncoding");
        }
        System.arraycopy(base, 0, simpleNames, 0, 256);
        if (enc instanceof PdfDict) {
            List<Object> diff = ((PdfDict) enc).getArray("Differences");
            if (diff != null) {
                int code = 0;
                for (int i = 0; i < diff.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                    Object o = diff.get(i);
                    if (o instanceof Number) {
                        code = ((Number) o).intValue();
                    } else if (o instanceof PdfName && code >= 0 && code < 256) {
                        simpleNames[code++] = ((PdfName) o).value;
                    }
                }
            }
        }
        if (font != null) {
            List<Object> w = font.getArray("Widths");
            int first = font.getInt("FirstChar", 0);
            if (w != null) {
                for (int i = 0; i < w.size(); i++) {
                    Object o = font.getFile() != null ? font.getFile().resolve(w.get(i)) : w.get(i);
                    if (o instanceof Number) {
                        widths.put(Integer.valueOf(first + i), Double.valueOf(((Number) o).doubleValue()));
                    }
                }
            }
            PdfDict fd = font.getDict("FontDescriptor");
            if (fd != null) {
                defaultWidth = fd.getNumber("MissingWidth", 0);
            }
        }
    }

    private static boolean isSymbolic(PdfDict font) {
        PdfDict fd = font.getDict("FontDescriptor");
        int flags = fd == null ? 0 : fd.getInt("Flags", 0);
        return (flags & 4) != 0 && (flags & 32) == 0;
    }

    private void readComposite(PdfDict font) {
        Object enc = font.get("Encoding");
        String encName = enc instanceof PdfName ? ((PdfName) enc).value : null;
        boolean identity = encName == null || encName.startsWith("Identity");
        vertical = encName != null && encName.endsWith("-V");
        if (enc instanceof PdfStream && ((PdfStream) enc).getInt("WMode", 0) == 1) {
            vertical = true;
        }
        unicodeCodes = encName != null && encName.startsWith("Uni")
                && (encName.indexOf("UCS2") >= 0 || encName.indexOf("UTF16") >= 0);
        if (enc instanceof PdfStream) {
            try {
                readCodeSpace(((PdfStream) enc).getData());
            } catch (java.io.IOException e) {
                codeSpaceLengths = null;
            }
        }
        List<Object> desc = font.getArray("DescendantFonts");
        PdfDict cid = null;
        if (desc != null && !desc.isEmpty()) {
            Object o = font.getFile() != null ? font.getFile().resolve(desc.get(0)) : desc.get(0);
            if (o instanceof PdfDict) {
                cid = (PdfDict) o;
            }
        }
        defaultWidth = 1000;
        if (cid == null) {
            return;
        }
        PdfDict info = cid.getDict("CIDSystemInfo");
        if (identity && info != null) {
            String o = info.getText("Ordering");
            if (o == null) {
                o = info.getName("Ordering");
            }
            ordering = o;
        }
        defaultWidth = cid.getNumber("DW", 1000);
        if (vertical) {
            readVertical(cid);
        }
        List<Object> w = cid.getArray("W");
        if (w == null) {
            return;
        }
        PdfFile f = cid.getFile();
        int i = 0;
        while (i < w.size()) {
            Object a = f != null ? f.resolve(w.get(i)) : w.get(i);
            Object b = i + 1 < w.size() ? (f != null ? f.resolve(w.get(i + 1)) : w.get(i + 1)) : null;
            if (!(a instanceof Number)) {
                break;
            }
            int first = ((Number) a).intValue();
            if (b instanceof List) {
                List<?> l = (List<?>) b;
                for (int k = 0; k < l.size(); k++) {
                    Object v = f != null ? f.resolve(l.get(k)) : l.get(k);
                    if (v instanceof Number) {
                        widths.put(Integer.valueOf(first + k), Double.valueOf(((Number) v).doubleValue()));
                    }
                }
                i += 2;
            } else {
                Object c = i + 2 < w.size() ? (f != null ? f.resolve(w.get(i + 2)) : w.get(i + 2)) : null;
                if (!(b instanceof Number) || !(c instanceof Number)) {
                    break;
                }
                int last = Math.min(((Number) b).intValue(), first + 65535);
                Double v = Double.valueOf(((Number) c).doubleValue());
                for (int k = first; k <= last; k++) {
                    widths.put(Integer.valueOf(k), v);
                }
                i += 3;
            }
        }
    }

    private void readVertical(PdfDict cid) {
        PdfFile f = cid.getFile();
        List<Object> dw2 = cid.getArray("DW2");
        if (dw2 != null && dw2.size() >= 2) {
            Object a = f != null ? f.resolve(dw2.get(0)) : dw2.get(0);
            Object b = f != null ? f.resolve(dw2.get(1)) : dw2.get(1);
            if (a instanceof Number && b instanceof Number) {
                dw2Vy = ((Number) a).doubleValue();
                dw2W1 = ((Number) b).doubleValue();
            }
        }
        List<Object> w2 = cid.getArray("W2");
        if (w2 == null) {
            return;
        }
        verticalMetrics = new HashMap<Integer, double[]>();
        int i = 0;
        while (i < w2.size()) {
            Object a = f != null ? f.resolve(w2.get(i)) : w2.get(i);
            Object b = i + 1 < w2.size() ? (f != null ? f.resolve(w2.get(i + 1)) : w2.get(i + 1)) : null;
            if (!(a instanceof Number)) {
                break;
            }
            int first = ((Number) a).intValue();
            if (b instanceof List) {
                List<?> l = (List<?>) b;
                for (int k = 0; k + 2 < l.size(); k += 3) {
                    double[] m = triple(f, l.get(k), l.get(k + 1), l.get(k + 2));
                    if (m != null) {
                        verticalMetrics.put(Integer.valueOf(first + k / 3), m);
                    }
                }
                i += 2;
            } else {
                if (i + 4 >= w2.size() || !(b instanceof Number)) {
                    break;
                }
                double[] m = triple(f, w2.get(i + 2), w2.get(i + 3), w2.get(i + 4));
                if (m == null) {
                    break;
                }
                int last = Math.min(((Number) b).intValue(), first + 65535);
                for (int k = first; k <= last; k++) {
                    verticalMetrics.put(Integer.valueOf(k), m);
                }
                i += 5;
            }
        }
    }

    private static double[] triple(PdfFile f, Object a, Object b, Object c) {
        Object x = f != null ? f.resolve(a) : a;
        Object y = f != null ? f.resolve(b) : b;
        Object z = f != null ? f.resolve(c) : c;
        if (x instanceof Number && y instanceof Number && z instanceof Number) {
            return new double[] {((Number) x).doubleValue(), ((Number) y).doubleValue(), ((Number) z).doubleValue()};
        }
        return null;
    }

    /// True when the font writes vertically (a `-V` CMap or `WMode 1`).
    public boolean isVertical() {
        return vertical;
    }

    /// The vertical metrics of `code` in 1/1000 text space units:
    /// `{w1y, vx, vy}`, the vertical advance (negative, downwards) and the
    /// position vector from the horizontal origin to the vertical one.
    public double[] verticalMetrics(int code) {
        double[] m = verticalMetrics == null ? null : verticalMetrics.get(Integer.valueOf(code));
        if (m != null) {
            return m;
        }
        return new double[] {dw2W1, width(code) / 2, dw2Vy};
    }

    private void readCodeSpace(byte[] d) {
        PdfLexer lx = new PdfLexer(d, 0, d.length, false);
        int[] lens = new int[5];
        boolean any = false;
        while (true) {
            Object o = lx.next();
            if (o == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                break;
            }
            if (o instanceof PdfLexer.Op && "begincodespacerange".equals(((PdfLexer.Op) o).name)) {
                while (true) {
                    Object lo = lx.next();
                    if (!(lo instanceof PdfString)) {
                        break;
                    }
                    lx.next();
                    int n = ((PdfString) lo).bytes.length;
                    if (n >= 1 && n <= 4) {
                        lens[n] = 1;
                        any = true;
                    }
                }
            }
        }
        if (any) {
            codeSpaceLengths = lens;
        }
    }

    private void readCMap(byte[] d) {
        PdfLexer lx = new PdfLexer(d, 0, d.length, false);
        java.util.ArrayList<Object> operands = new java.util.ArrayList<Object>();
        int[] lens = new int[5];
        boolean anyLen = false;
        // Ranges are cheap to write and each expands to up to 65536 entries;
        // past this many entries in total the rest of the map is ignored.
        int budget = 1 << 20;
        while (true) {
            Object o = lx.next();
            if (o == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                break;
            }
            if (!(o instanceof PdfLexer.Op)) {
                operands.add(o);
                continue;
            }
            String op = ((PdfLexer.Op) o).name;
            if ("begincodespacerange".equals(op) && !composite) {
                operands.clear();
            } else if ("endcodespacerange".equals(op)) {
                for (int i = 0; i < operands.size(); i += 2) {
                    if (operands.get(i) instanceof PdfString) {
                        int n = ((PdfString) operands.get(i)).bytes.length;
                        if (n >= 1 && n <= 4) {
                            lens[n] = 1;
                            anyLen = true;
                        }
                    }
                }
            } else if ("endbfchar".equals(op)) {
                for (int i = 0; i + 1 < operands.size(); i += 2) {
                    Object a = operands.get(i);
                    Object b = operands.get(i + 1);
                    if (a instanceof PdfString && b instanceof PdfString) {
                        toUnicode.put(Integer.valueOf(code(((PdfString) a).bytes)), utf16(((PdfString) b).bytes));
                    }
                }
            } else if ("endbfrange".equals(op)) {
                for (int i = 0; i + 2 < operands.size(); i += 3) {
                    Object a = operands.get(i);
                    Object b = operands.get(i + 1);
                    Object c = operands.get(i + 2);
                    if (!(a instanceof PdfString) || !(b instanceof PdfString)) {
                        continue;
                    }
                    int lo = code(((PdfString) a).bytes);
                    int hi = Math.min(code(((PdfString) b).bytes), lo + 65535);
                    if (hi < lo || hi - lo >= budget) {
                        continue;
                    }
                    budget -= hi - lo + 1;
                    if (c instanceof PdfString && ((PdfString) c).bytes.length <= 64) {
                        byte[] dst = ((PdfString) c).bytes;
                        for (int k = lo; k <= hi; k++) {
                            byte[] t = new byte[dst.length];
                            System.arraycopy(dst, 0, t, 0, dst.length);
                            int add = k - lo;
                            for (int j = t.length - 1; j >= 0 && add != 0; j--) {
                                int v = (t[j] & 0xff) + add;
                                t[j] = (byte) v;
                                add = v >> 8;
                            }
                            toUnicode.put(Integer.valueOf(k), utf16(t));
                        }
                    } else if (c instanceof List) {
                        List<?> l = (List<?>) c;
                        for (int k = 0; k < l.size() && lo + k <= hi; k++) {
                            if (l.get(k) instanceof PdfString) {
                                toUnicode.put(Integer.valueOf(lo + k), utf16(((PdfString) l.get(k)).bytes));
                            }
                        }
                    }
                }
            }
            if (!"begincodespacerange".equals(op) || composite) {
                operands.clear();
            }
        }
        if (anyLen && composite && codeSpaceLengths == null) {
            codeSpaceLengths = lens;
        }
    }

    private static int code(byte[] b) {
        int v = 0;
        for (int i = 0; i < b.length && i < 4; i++) {
            v = (v << 8) | (b[i] & 0xff);
        }
        return v;
    }

    private static String utf16(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length / 2);
        for (int i = 0; i + 1 < b.length; i += 2) {
            sb.append((char) (((b[i] & 0xff) << 8) | (b[i + 1] & 0xff)));
        }
        if (b.length == 1) {
            sb.append((char) (b[0] & 0xff));
        }
        return sb.toString();
    }

    /// The length in bytes of the code starting at `s[pos]`.
    public int codeLength(byte[] s, int pos) {
        if (!composite) {
            return 1;
        }
        if (codeSpaceLengths == null) {
            return 2;
        }
        for (int n = 1; n <= 4; n++) {
            if (codeSpaceLengths[n] != 0 && (n == 4 || !hasLonger(n) || pos + n >= s.length
                    || toUnicode.containsKey(Integer.valueOf(read(s, pos, n))))) {
                return Math.min(n, s.length - pos);
            }
        }
        return Math.min(2, s.length - pos);
    }

    private boolean hasLonger(int n) {
        for (int k = n + 1; k <= 4; k++) {
            if (codeSpaceLengths[k] != 0) {
                return true;
            }
        }
        return false;
    }

    /// Reads a big-endian code of `n` bytes.
    public static int read(byte[] s, int pos, int n) {
        int v = 0;
        for (int i = 0; i < n && pos + i < s.length; i++) {
            v = (v << 8) | (s[pos + i] & 0xff);
        }
        return v;
    }

    /// The text of `code`, or the empty string when there is none.
    public String text(int code) {
        String t = toUnicode.get(Integer.valueOf(code));
        if (t != null) {
            return t;
        }
        if (composite) {
            if (unicodeCodes) {
                return code > 0xffff ? String.valueOf((char) (code >> 16)) + (char) (code & 0xffff)
                        : String.valueOf((char) code);
            }
            if (ordering != null) {
                int u = CidToUnicode.lookup(ordering, code);
                if (u > 0xffff) {
                    int v = u - 0x10000;
                    return String.valueOf((char) (0xd800 + (v >> 10))) + (char) (0xdc00 + (v & 0x3ff));
                }
                return u > 0 ? String.valueOf((char) u) : "";
            }
            return "";
        }
        String name = code >= 0 && code < 256 ? simpleNames[code] : null;
        if (name == null) {
            return code >= 32 && code < 127 ? String.valueOf((char) code) : "";
        }
        return unicodeOf(name);
    }

    /// The Unicode text of a glyph name, from the Adobe glyph list or the
    /// `uniXXXX` / `uXXXX[XX]` conventions.
    public static String unicodeOf(String name) {
        int dot = name.indexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        String v = glyphs().get(name);
        if (v != null) {
            return v;
        }
        if (name.startsWith("uni") && name.length() >= 7) {
            int c = parseHex(name, 3, 7);
            if (c >= 0) {
                return String.valueOf((char) c);
            }
        } else if (name.startsWith("u") && name.length() >= 5 && name.length() <= 7) {
            int c = parseHex(name, 1, name.length());
            if (c >= 0 && c < 0x10000) {
                return String.valueOf((char) c);
            }
            if (c >= 0x10000 && c <= 0x10ffff) {
                c -= 0x10000;
                return new StringBuilder().append((char) (0xd800 + (c >> 10))).append((char) (0xdc00 + (c & 0x3ff)))
                        .toString();
            }
        }
        return "";
    }

    private static int parseHex(String s, int from, int to) {
        int v = 0;
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            int h = c >= '0' && c <= '9' ? c - '0' : c >= 'A' && c <= 'F' ? c - 'A' + 10
                    : c >= 'a' && c <= 'f' ? c - 'a' + 10 : -1;
            if (h < 0) {
                return -1;
            }
            v = v * 16 + h;
        }
        return v;
    }

    /// The advance of `code` in 1/1000 text space units.
    public double width(int code) {
        Double w = widths.get(Integer.valueOf(code));
        if (w != null) {
            return w.doubleValue();
        }
        if (!composite && standardIndex >= 0) {
            String name = glyphName(code);
            int win = name == null ? -1 : winAnsiCode(name);
            if (win >= 0) {
                return StandardFont.widthTable(standardIndex)[win];
            }
        }
        return defaultWidth;
    }

    private static int winAnsiCode(String name) {
        String[] win = encoding("WinAnsiEncoding");
        for (int i = 0; i < 256; i++) {
            if (name.equals(win[i])) {
                return i;
            }
        }
        return -1;
    }

    /// True when the widths come from the standard metrics.
    public boolean usesStandardMetrics() {
        return standard;
    }

    /// The 256 glyph names of a named encoding (StandardEncoding if unknown).
    public static String[] encoding(String name) {
        int slot = "WinAnsiEncoding".equals(name) ? 1 : "MacRomanEncoding".equals(name) ? 2
                : "Symbol".equals(name) ? 3 : "ZapfDingbats".equals(name) ? 4 : 0;
        String[] e = ENCODINGS[slot];
        if (e != null) {
            return e;
        }
        String src = PdfEncodings.table(slot);
        e = new String[256];
        int i = 0;
        int start = 0;
        for (int p = 0; p <= src.length() && i < 256; p++) {
            if (p == src.length() || src.charAt(p) == ' ') {
                if (p > start) {
                    String n = src.substring(start, p);
                    e[i++] = ".".equals(n) ? null : n;
                }
                start = p + 1;
            }
        }
        ENCODINGS[slot] = e;
        return e;
    }

    private static Map<String, String> glyphs() {
        Map<String, String> g = glyphList;
        if (g != null) {
            return g;
        }
        g = new HashMap<String, String>();
        String src = PdfEncodings.unicode();
        int start = 0;
        for (int p = 0; p <= src.length(); p++) {
            if (p == src.length() || src.charAt(p) == ' ') {
                int colon = src.indexOf(':', start);
                if (colon > start && colon < p) {
                    int c = parseHex(src, colon + 1, p);
                    if (c >= 0) {
                        g.put(src.substring(start, colon), String.valueOf((char) c));
                    }
                }
                start = p + 1;
            }
        }
        glyphList = g;
        return g;
    }
}
