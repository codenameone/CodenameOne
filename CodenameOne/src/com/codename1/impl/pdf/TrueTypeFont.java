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

import com.codename1.pdf.PdfFont;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A TrueType font embedded as a subset: Type0 with Identity-H encoding over
/// a CIDFontType2 whose CIDs are the original glyph ids, plus a ToUnicode
/// map so text can be searched and copied.
///
/// The subset keeps every glyph id where it was and empties the outlines of
/// the glyphs nobody used, which keeps `cmap`-free embedding valid without
/// renumbering composite references.
@com.codename1.impl.SharedWithBackend
public final class TrueTypeFont extends PdfFont {
    private static final String[] KEPT_TABLES = {"cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca",
        "maxp", "prep"};

    private final byte[] data;
    private final Map<String, int[]> tables = new HashMap<String, int[]>();
    private String name = "Font";
    private int unitsPerEm = 1000;
    private boolean longLoca;
    private int numGlyphs;
    private int[] advances;
    private int ascender;
    private int descender;
    private int capHeight;
    private int italicAngle;
    private boolean fixedPitch;
    private final int[] bbox = new int[4];
    private int cmapOffset = -1;
    private int cmapFormat;

    /// Parses `data`; see [PdfFont#loadTrueType(byte[])].
    public TrueTypeFont(byte[] data) throws IOException {
        this.data = data;
        parse();
    }

    private void parse() throws IOException {
        if (data.length < 12) {
            throw new IOException("Not a TrueType font");
        }
        int version = u32(0);
        if (version == 0x74746366) {
            throw new IOException("Font collections (.ttc) are not supported");
        }
        if (version == 0x4f54544f) {
            throw new IOException("CFF-based OpenType fonts are not supported; use a TrueType font");
        }
        if (version != 0x00010000 && version != 0x74727565) {
            throw new IOException("Not a TrueType font");
        }
        int count = u16(4);
        for (int i = 0; i < count; i++) {
            int rec = 12 + i * 16;
            check(rec + 16);
            String tag = tag(rec);
            int off = u32(rec + 8);
            int len = u32(rec + 12);
            if (off < 0 || len < 0 || (long) off + len > data.length) {
                throw new IOException("Bad TrueType table " + tag);
            }
            tables.put(tag, new int[] {off, len});
        }
        for (int i = 0; i < 7; i++) {
            String t = new String[] {"head", "hhea", "maxp", "hmtx", "loca", "glyf", "cmap"}[i];
            if (!tables.containsKey(t)) {
                throw new IOException("TrueType font has no " + t + " table");
            }
        }
        int head = table("head");
        unitsPerEm = u16(head + 18);
        if (unitsPerEm == 0) {
            unitsPerEm = 1000;
        }
        bbox[0] = s16(head + 36);
        bbox[1] = s16(head + 38);
        bbox[2] = s16(head + 40);
        bbox[3] = s16(head + 42);
        longLoca = s16(head + 50) != 0;
        numGlyphs = u16(table("maxp") + 4);
        if (numGlyphs < 1) {
            throw new IOException("Font has no glyphs");
        }
        int hhea = table("hhea");
        ascender = s16(hhea + 4);
        descender = s16(hhea + 6);
        int numHMetrics = Math.max(1, Math.min(u16(hhea + 34), numGlyphs));
        int hmtx = table("hmtx");
        check(hmtx + numHMetrics * 4);
        advances = new int[numGlyphs];
        for (int g = 0; g < numGlyphs; g++) {
            advances[g] = u16(hmtx + Math.min(g, numHMetrics - 1) * 4);
        }
        capHeight = ascender;
        if (tables.containsKey("OS/2")) {
            int os2 = table("OS/2");
            int fsType = u16(os2 + 8);
            if ((fsType & 0x000f) == 0x0002) {
                throw new IOException("The font's license does not allow embedding");
            }
            if (u16(os2) >= 2 && tableLength("OS/2") >= 90) {
                capHeight = s16(os2 + 88);
            }
        }
        if (tables.containsKey("post")) {
            int post = table("post");
            italicAngle = s16(post + 4);
            fixedPitch = u32(post + 12) != 0;
        }
        readName();
        selectCmap();
    }

    private void readName() {
        int[] t = tables.get("name");
        if (t == null) {
            return;
        }
        int base = t[0];
        int count = u16(base + 2);
        int strings = base + u16(base + 4);
        for (int i = 0; i < count; i++) {
            int rec = base + 6 + i * 12;
            if (rec + 12 > data.length) {
                return;
            }
            if (u16(rec + 6) != 6) {
                continue;
            }
            int platform = u16(rec);
            int len = u16(rec + 8);
            int off = strings + u16(rec + 10);
            if (off + len > data.length) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            if (platform == 3 || platform == 0) {
                for (int j = 0; j + 1 < len; j += 2) {
                    sb.append((char) u16(off + j));
                }
            } else {
                for (int j = 0; j < len; j++) {
                    sb.append((char) (data[off + j] & 0xff));
                }
            }
            StringBuilder clean = new StringBuilder();
            for (int j = 0; j < sb.length(); j++) {
                char c = sb.charAt(j);
                if (c > 0x20 && c < 0x7f && !PdfFormat.isDelimiter(c)) {
                    clean.append(c);
                }
            }
            if (clean.length() > 0) {
                name = clean.toString();
                return;
            }
        }
    }

    private void selectCmap() {
        int cmap = table("cmap");
        int count = u16(cmap + 2);
        int best = -1;
        int bestScore = 0;
        for (int i = 0; i < count; i++) {
            int rec = cmap + 4 + i * 8;
            if (rec + 8 > data.length) {
                break;
            }
            int platform = u16(rec);
            int encoding = u16(rec + 2);
            int off = cmap + u32(rec + 4);
            if (off + 2 > data.length) {
                continue;
            }
            int format = u16(off);
            int score = 0;
            if (format == 12 && (platform == 3 && encoding == 10 || platform == 0)) {
                score = 3;
            } else if (format == 4 && (platform == 3 && encoding == 1 || platform == 0)) {
                score = 2;
            } else if (format == 4 && platform == 3 && encoding == 0) {
                score = 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = off;
            }
        }
        if (best >= 0) {
            cmapOffset = best;
            cmapFormat = u16(best);
        }
    }

    /// The glyph id of code point `cp`, 0 when the font has none.
    public int glyph(int cp) {
        int g = lookup(cp);
        return g >= 0 && g < numGlyphs ? g : 0;
    }

    private int lookup(int cp) {
        if (cmapOffset < 0) {
            return 0;
        }
        int t = cmapOffset;
        if (cmapFormat == 12) {
            int groups = u32(t + 12);
            int lo = 0;
            int hi = groups - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int g = t + 16 + mid * 12;
                int start = u32(g);
                int end = u32(g + 4);
                if (cp < start) {
                    hi = mid - 1;
                } else if (cp > end) {
                    lo = mid + 1;
                } else {
                    return u32(g + 8) + cp - start;
                }
            }
            return 0;
        }
        if (cp > 0xffff) {
            return 0;
        }
        int segX2 = u16(t + 6);
        int ends = t + 14;
        int starts = ends + segX2 + 2;
        int deltas = starts + segX2;
        int ranges = deltas + segX2;
        for (int i = 0; i < segX2; i += 2) {
            int end = u16(ends + i);
            if (cp > end) {
                continue;
            }
            int start = u16(starts + i);
            if (cp < start) {
                return 0;
            }
            int delta = u16(deltas + i);
            int range = u16(ranges + i);
            if (range == 0) {
                return (cp + delta) & 0xffff;
            }
            int addr = ranges + i + range + (cp - start) * 2;
            if (addr + 2 > data.length) {
                return 0;
            }
            int g = u16(addr);
            return g == 0 ? 0 : (g + delta) & 0xffff; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop scans for the first match
        }
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    /// Units per em of the glyph coordinates.
    public int getUnitsPerEm() {
        return unitsPerEm;
    }

    /// The advance of glyph `g` in font units.
    public int advance(int g) {
        return g >= 0 && g < numGlyphs ? advances[g] : 0;
    }

    @Override
    public float getStringWidth(String text, float size) {
        Shaped sh = shape(text, false);
        long total = 0;
        for (int i = 0; i < sh.glyphs.length; i++) {
            total += advance(sh.glyphs[i]);
        }
        return total * size / unitsPerEm;
    }

    /// Glyphs and the text each stands for, in display order.
    static final class Shaped {
        int[] glyphs;
        String[] text;

        Shaped() {
            glyphs = null;
            text = null;
        }
    }

    private GsubTable gsub;
    private boolean gsubRead;

    private GsubTable gsub() {
        if (!gsubRead) {
            gsubRead = true;
            int[] t = tables.get("GSUB");
            if (t != null) {
                gsub = new GsubTable(data, t[0]);
            }
        }
        return gsub;
    }

    private static final String[] FORM_FEATURES = {"isol", "fina", "init", "medi"};

    /// Shapes `text` (logical order): Arabic letters take their contextual
    /// forms and lam-alef ligatures from the font's GSUB table, or from
    /// the Unicode presentation forms the font maps. With `rtl` the
    /// result is reversed and mirrored characters are swapped, for a
    /// right-to-left run.
    Shaped shape(String text, boolean rtl) {
        int n = text.length();
        int[] cps = new int[n];
        int[] starts = new int[n];
        int count = 0;
        boolean mirrored = false;
        for (int i = 0; i < n; i++) {
            int cp = text.charAt(i);
            starts[count] = i;
            if (isHighSurrogate(cp) && i + 1 < n) {
                cp = toCodePoint(cp, text.charAt(++i));
            } else if (rtl) {
                int m = PdfBidi.mirror((char) cp);
                if (m != cp) {
                    mirrored = true;
                    cp = m;
                }
            }
            cps[count++] = cp;
        }
        int[] g = new int[count];
        for (int i = 0; i < count; i++) {
            g[i] = glyph(cps[i]);
        }
        boolean[] joinedNext = new boolean[count];
        if (ArabicShaper.needsShaping(text)) {
            StringBuilder bmp = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                bmp.append(cps[i] > 0xffff ? '\ufffd' : (char) cps[i]);
            }
            String logical = bmp.toString();
            int[] forms = ArabicShaper.forms(logical);
            GsubTable gs = gsub();
            boolean useGsub = gs != null && (gs.has("init") || gs.has("fina"));
            for (int i = 0; i < count; i++) {
                char c = logical.charAt(i);
                if (c < 0x0620 || c > 0x08ff) {
                    continue;
                }
                if (useGsub) {
                    g[i] = gs.substitute(FORM_FEATURES[forms[i]], g[i]);
                } else {
                    char pf = ArabicShaper.presentation(c, forms[i]);
                    if (pf != c) {
                        int pg = glyph(pf);
                        if (pg != 0) {
                            g[i] = pg;
                        }
                    }
                }
            }
            // Lam-alef: a ligature through GSUB, or the presentation form.
            for (int i = 0; i + 1 < count; i++) {
                if (logical.charAt(i) != 0x0644) {
                    continue;
                }
                // A haraka between lam and alef (vocalized text) keeps the
                // two as joined letters rather than the ligature; both read
                // correctly, and fonts that care do it in their own GSUB.
                char alef = logical.charAt(i + 1);
                char lig = ArabicShaper.lamAlef(alef, forms[i] == ArabicShaper.MEDIAL
                        ? ArabicShaper.FINAL : forms[i] == ArabicShaper.INITIAL ? ArabicShaper.ISOLATED : forms[i]);
                if (lig == 0) {
                    continue;
                }
                int[] res = null;
                if (useGsub) {
                    for (int f = 0; f < 3 && res == null; f++) {
                        res = gs.ligature(f == 0 ? "rlig" : f == 1 ? "liga" : "calt", g, i, i + 2);
                    }
                }
                int lg = res != null && res[1] == 2 ? res[0] : glyph(lig);
                if (lg != 0) {
                    g[i] = lg;
                    g[i + 1] = -1;
                    joinedNext[i] = true;
                }
            }
        }
        List<Integer> outG = new ArrayList<Integer>(count);
        List<String> outT = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            if (g[i] < 0) {
                continue;
            }
            int from = starts[i];
            int last = joinedNext[i] ? i + 1 : i;
            int to = last + 1 < count ? starts[last + 1] : n;
            outG.add(Integer.valueOf(g[i]));
            // A mirrored glyph stands for the character it shows, so the
            // same glyph extracts the same way in either direction.
            outT.add(mirrored && to - from == 1 && cps[i] != text.charAt(from)
                    ? String.valueOf((char) cps[i]) : text.substring(from, to));
        }
        Shaped sh = new Shaped();
        int m = outG.size();
        sh.glyphs = new int[m];
        sh.text = new String[m];
        for (int i = 0; i < m; i++) {
            int k = rtl ? m - 1 - i : i;
            sh.glyphs[i] = outG.get(k).intValue();
            sh.text[i] = outT.get(k);
        }
        return sh;
    }

    @Override
    public float getAscent(float size) {
        return ascender * size / unitsPerEm;
    }

    @Override
    public float getDescent(float size) {
        return -descender * size / unitsPerEm;
    }

    @Override
    public boolean canDisplay(char c) {
        return glyph(c) != 0;
    }

    static boolean isHighSurrogate(int c) {
        return c >= 0xd800 && c <= 0xdbff;
    }

    static int toCodePoint(int high, int low) {
        return ((high - 0xd800) << 10) + (low - 0xdc00) + 0x10000;
    }

    /// The bytes of the glyph's `glyf` entry, offset and length, or null
    /// for an empty glyph.
    public int[] glyphRange(int g) {
        if (g < 0 || g >= numGlyphs) {
            return null;
        }
        int loca = table("loca");
        int start;
        int end;
        if (longLoca) {
            start = u32(loca + g * 4);
            end = u32(loca + g * 4 + 4);
        } else {
            start = u16(loca + g * 2) * 2;
            end = u16(loca + g * 2 + 2) * 2;
        }
        int glyf = table("glyf");
        if (end <= start || glyf + end > data.length) {
            return null;
        }
        return new int[] {glyf + start, end - start};
    }

    /// The raw font bytes, for the outline reader.
    public byte[] data() {
        return data;
    }

    @Override
    protected FontState createState() {
        return new State();
    }

    private final class State extends FontState {
        private final Map<Integer, String> unicode = new HashMap<Integer, String>();

        @Override
        public void showText(StringBuilder content, String text) {
            showText(content, text, false);
        }

        @Override
        public void showText(StringBuilder content, String text, boolean rtl) {
            Shaped sh = shape(text, rtl);
            StringBuilder hex = new StringBuilder(sh.glyphs.length * 4 + 2);
            hex.append('<');
            for (int i = 0; i < sh.glyphs.length; i++) {
                int g = sh.glyphs[i];
                Integer key = Integer.valueOf(g);
                if (!unicode.containsKey(key)) {
                    unicode.put(key, sh.text[i]);
                }
                hex4(hex, g);
            }
            hex.append('>');
            content.append(hex);
        }

        @Override
        public void write(PdfWriter writer) throws IOException {
            String tagName = subsetTag() + "+" + name;
            int cid = writer.reserve();
            int desc = writer.reserve();
            int file = writer.reserve();
            int toUnicode = writer.reserve();
            StringBuilder sb = new StringBuilder();
            sb.append("<</Type /Font /Subtype /Type0 /BaseFont ");
            PdfFormat.name(sb, tagName);
            sb.append(" /Encoding /Identity-H /DescendantFonts [").append(cid).append(" 0 R] /ToUnicode ")
                    .append(toUnicode).append(" 0 R>>");
            writer.object(objectNumber, sb.toString());

            sb.setLength(0);
            sb.append("<</Type /Font /Subtype /CIDFontType2 /BaseFont ");
            PdfFormat.name(sb, tagName);
            sb.append(" /CIDSystemInfo <</Registry (Adobe) /Ordering (Identity) /Supplement 0>> /FontDescriptor ")
                    .append(desc).append(" 0 R /CIDToGIDMap /Identity /DW ").append(scale(advance(0)))
                    .append(" /W [");
            int[] used = usedGlyphs();
            for (int i = 0; i < used.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                sb.append(used[i]).append(" [").append(scale(advance(used[i]))).append("] ");
            }
            sb.append("]>>");
            writer.object(cid, sb.toString());

            sb.setLength(0);
            sb.append("<</Type /FontDescriptor /FontName ");
            PdfFormat.name(sb, tagName);
            int flags = 32 | (fixedPitch ? 1 : 0) | (italicAngle != 0 ? 64 : 0);
            sb.append(" /Flags ").append(flags).append(" /FontBBox [").append(scale(bbox[0])).append(' ')
                    .append(scale(bbox[1])).append(' ').append(scale(bbox[2])).append(' ').append(scale(bbox[3]))
                    .append("] /ItalicAngle ").append(italicAngle).append(" /Ascent ").append(scale(ascender))
                    .append(" /Descent ").append(scale(descender)).append(" /CapHeight ").append(scale(capHeight))
                    .append(" /StemV 80 /FontFile2 ").append(file).append(" 0 R>>");
            writer.object(desc, sb.toString());

            byte[] subset = subset(used);
            writer.stream(file, "/Length1 " + subset.length, subset, true);
            writer.stream(toUnicode, "", toUnicodeCMap(used), true);
        }

        private int[] usedGlyphs() {
            int[] used = new int[unicode.size()];
            int i = 0;
            for (Integer g : unicode.keySet()) {
                used[i++] = g.intValue();
            }
            java.util.Arrays.sort(used);
            return used;
        }

        private String subsetTag() {
            long h = 1125899906842597L;
            int[] used = usedGlyphs();
            for (int i = 0; i < used.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                h = 31 * h + used[i];
            }
            h = 31 * h + name.hashCode();
            // Two font files can share a name and a glyph set (a regular and
            // a bold cut named alike); the object number keeps their tags
            // apart within the document.
            h = 31 * h + objectNumber;
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append((char) ('A' + (int) ((h >>> (i * 5)) & 0x1f) % 26));
            }
            return sb.toString();
        }

        private byte[] toUnicodeCMap(int[] usedWithNotdef) {
            // Glyph 0 is the box drawn for characters the font lacks; mapping
            // it would make copied text read as whichever such character
            // came last.
            int count = 0;
            for (int i = 0; i < usedWithNotdef.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding loops
                if (usedWithNotdef[i] != 0) {
                    count++;
                }
            }
            int[] used = new int[count];
            count = 0;
            for (int i = 0; i < usedWithNotdef.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding loops
                if (usedWithNotdef[i] != 0) {
                    used[count++] = usedWithNotdef[i];
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n")
                    .append("/CIDSystemInfo <</Registry (Adobe) /Ordering (UCS) /Supplement 0>> def\n")
                    .append("/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n")
                    .append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
            for (int start = 0; start < used.length; start += 100) {
                int n = Math.min(100, used.length - start);
                sb.append(n).append(" beginbfchar\n");
                for (int i = start; i < start + n; i++) {
                    sb.append('<');
                    hex4(sb, used[i]);
                    sb.append("> <");
                    // UTF-16, the letters the glyph stands for in reading
                    // order (a ligature maps to all of its letters).
                    String t = unicode.get(Integer.valueOf(used[i]));
                    for (int k = 0; k < t.length() && k < 64; k++) {
                        hex4(sb, t.charAt(k));
                    }
                    if (t.length() == 0) {
                        hex4(sb, 0xfffd);
                    }
                    sb.append(">\n");
                }
                sb.append("endbfchar\n");
            }
            sb.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n");
            return PdfFormat.ascii(sb.toString());
        }
    }

    private int scale(int units) {
        return (int) ((long) units * 1000 / unitsPerEm);
    }

    private static void hex4(StringBuilder sb, int v) {
        String h = "0123456789ABCDEF";
        sb.append(h.charAt((v >> 12) & 0xf)).append(h.charAt((v >> 8) & 0xf))
                .append(h.charAt((v >> 4) & 0xf)).append(h.charAt(v & 0xf));
    }

    /// A font file holding only the outlines of `used`, glyph 0 and the
    /// components composites among them refer to, every glyph keeping its
    /// id. Hinting tables are copied as they are.
    byte[] subset(int[] used) throws IOException {
        boolean[] keep = new boolean[numGlyphs];
        keep[0] = true;
        int[] stack = new int[numGlyphs + used.length + 1];
        int sp = 0;
        for (int i = 0; i < used.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            stack[sp++] = used[i];
        }
        while (sp > 0) {
            int g = stack[--sp];
            if (g < 0 || g >= numGlyphs || keep[g] && g != 0) {
                continue;
            }
            keep[g] = true;
            int[] r = glyphRange(g);
            if (r == null || r[1] < 10 || s16(r[0]) >= 0) {
                continue;
            }
            int p = r[0] + 10;
            int end = r[0] + r[1];
            while (p + 4 <= end) {
                int flags = u16(p);
                int comp = u16(p + 2);
                if (comp < numGlyphs && !keep[comp]) {
                    if (sp == stack.length) {
                        int[] grown = new int[sp * 2];
                        System.arraycopy(stack, 0, grown, 0, sp);
                        stack = grown;
                    }
                    stack[sp++] = comp;
                }
                p += 4 + ((flags & 1) != 0 ? 4 : 2);
                if ((flags & 8) != 0) {
                    p += 2;
                } else if ((flags & 0x40) != 0) {
                    p += 4;
                } else if ((flags & 0x80) != 0) {
                    p += 8;
                }
                if ((flags & 0x20) == 0) {
                    break;
                }
            }
        }
        ByteArrayOutputStream glyf = new ByteArrayOutputStream();
        int[] loca = new int[numGlyphs + 1];
        for (int g = 0; g < numGlyphs; g++) {
            loca[g] = glyf.size();
            if (keep[g]) {
                int[] r = glyphRange(g);
                if (r != null) {
                    glyf.write(data, r[0], r[1]);
                    while ((glyf.size() & 3) != 0) {
                        glyf.write(0);
                    }
                }
            }
        }
        loca[numGlyphs] = glyf.size();
        byte[] locaBytes = new byte[(numGlyphs + 1) * 4];
        for (int g = 0; g <= numGlyphs; g++) {
            put32(locaBytes, g * 4, loca[g]);
        }

        int count = 0;
        for (int i = 0; i < KEPT_TABLES.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            if (tables.containsKey(KEPT_TABLES[i])) {
                count++;
            }
        }
        byte[][] bodies = new byte[KEPT_TABLES.length][];
        for (int i = 0; i < KEPT_TABLES.length; i++) {
            String t = KEPT_TABLES[i];
            if (!tables.containsKey(t)) {
                continue;
            }
            if ("glyf".equals(t)) {
                bodies[i] = glyf.toByteArray();
            } else if ("loca".equals(t)) {
                bodies[i] = locaBytes;
            } else {
                int[] r = tables.get(t);
                bodies[i] = new byte[r[1]];
                System.arraycopy(data, r[0], bodies[i], 0, r[1]);
                if ("head".equals(t) && r[1] >= 54) {
                    put32(bodies[i], 8, 0);
                    bodies[i][50] = 0;
                    bodies[i][51] = 1;
                }
            }
        }
        int entrySelector = 0;
        while ((1 << (entrySelector + 1)) <= count) {
            entrySelector++;
        }
        int searchRange = (1 << entrySelector) * 16;
        int offset = 12 + count * 16;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] dir = new byte[offset];
        put32(dir, 0, 0x00010000);
        put16(dir, 4, count);
        put16(dir, 6, searchRange);
        put16(dir, 8, entrySelector);
        put16(dir, 10, count * 16 - searchRange);
        int rec = 12;
        int headOffset = -1;
        for (int i = 0; i < KEPT_TABLES.length; i++) {
            if (bodies[i] == null) {
                continue;
            }
            String t = KEPT_TABLES[i];
            for (int c = 0; c < 4; c++) {
                dir[rec + c] = (byte) t.charAt(c);
            }
            put32(dir, rec + 4, checksum(bodies[i]));
            put32(dir, rec + 8, offset);
            put32(dir, rec + 12, bodies[i].length);
            if ("head".equals(t)) {
                headOffset = offset;
            }
            offset += (bodies[i].length + 3) & ~3;
            rec += 16;
        }
        out.write(dir);
        for (int i = 0; i < KEPT_TABLES.length; i++) {
            if (bodies[i] != null) {
                out.write(bodies[i]);
                for (int pad = bodies[i].length; (pad & 3) != 0; pad++) {
                    out.write(0);
                }
            }
        }
        byte[] font = out.toByteArray();
        if (headOffset >= 0) {
            put32(font, headOffset + 8, (int) (0xB1B0AFBAL - (checksum(font) & 0xffffffffL)));
        }
        return font;
    }

    private static int checksum(byte[] b) {
        int sum = 0;
        for (int i = 0; i < b.length; i += 4) {
            int v = 0;
            for (int j = 0; j < 4; j++) {
                v = (v << 8) | (i + j < b.length ? b[i + j] & 0xff : 0);
            }
            sum += v;
        }
        return sum;
    }

    private static void put32(byte[] b, int p, int v) {
        b[p] = (byte) (v >>> 24);
        b[p + 1] = (byte) (v >>> 16);
        b[p + 2] = (byte) (v >>> 8);
        b[p + 3] = (byte) v;
    }

    private static void put16(byte[] b, int p, int v) {
        b[p] = (byte) (v >>> 8);
        b[p + 1] = (byte) v;
    }

    private int table(String tag) {
        return tables.get(tag)[0];
    }

    private int tableLength(String tag) {
        return tables.get(tag)[1];
    }

    private String tag(int p) {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append((char) (data[p + i] & 0xff));
        }
        return sb.toString();
    }

    private void check(int end) throws IOException {
        if (end > data.length) {
            throw new IOException("Truncated TrueType font");
        }
    }

    /// Unsigned 16 bits at `p`, 0 past the end.
    public int u16(int p) {
        if (p < 0 || p + 2 > data.length) {
            return 0;
        }
        return ((data[p] & 0xff) << 8) | (data[p + 1] & 0xff);
    }

    /// Signed 16 bits at `p`.
    public int s16(int p) {
        return (short) u16(p);
    }

    private int u32(int p) {
        if (p < 0 || p + 4 > data.length) {
            return 0;
        }
        return ((data[p] & 0xff) << 24) | ((data[p + 1] & 0xff) << 16) | ((data[p + 2] & 0xff) << 8)
                | (data[p + 3] & 0xff);
    }
}
