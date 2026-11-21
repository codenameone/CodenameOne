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
package com.codename1.wordprocessing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Word 97-2003 binary (`.doc`, MS-DOC) import. The compound file is decoded (FIB, piece table,
/// CHPX/PAPX FKPs, stylesheet, lists, tables, sections, notes, fields, comments and inline pictures)
/// and translated into the parts of an equivalent WordprocessingML package that `DocxReader` maps like
/// any other DOCX. Reading is lenient and bounded: hostile input only ever yields an `IOException`.
final class DocReader {
    private static final int MAX_PIECES = 1000000;
    private static final int MAX_BINS = 100000;
    private static final int MAX_STYLES = 4096;
    private static final int MAX_LISTS = 2000;
    private static final int MAX_SECTIONS = 1024;
    private static final int MAX_NOTES = 10000;
    private static final int MAX_PARTS = 4096;
    private static final int MAX_DEPTH = 16;
    private static final long MAX_CHARS = 50000000L;
    private static final String CP1252 =
            "\u20AC\uFFFD\u201A\u0192\u201E\u2026\u2020\u2021\u02C6\u2030\u0160\u2039\u0152\uFFFD\u017D\uFFFD"
            + "\uFFFD\u2018\u2019\u201C\u201D\u2022\u2013\u2014\u02DC\u2122\u0161\u203A\u0153\uFFFD\u017E\u0178";
    private static final String NS = " xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
            + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\"";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    private static final String CT = "application/vnd.openxmlformats-officedocument.wordprocessingml.";

    /// Receives each sprm of a grpprl: its opcode and the operand's offset and length.
    private abstract static class Prl {
        abstract void op(int op, byte[] b, int at, int len);
    }

    private final byte[] word;
    private final byte[] table;
    private final byte[] data;
    private final long[] pairs;
    private final long ccpText;
    private final long ccpFtn;
    private final long ccpHdd;
    private final long ccpAtn;
    private final long ccpEdn;
    private long[] cps = new long[0];
    private long[] pfc = new long[0];
    private boolean[] pcomp = new boolean[0];
    private int[] pprm = new int[0];
    private final List<byte[]> prc = new ArrayList<byte[]>();
    private long[] chpFcs;
    private long[] chpPns;
    private long[] papFcs;
    private long[] papPns;
    private final List<String> fonts = new ArrayList<String>();
    private final List<Style> styles = new ArrayList<Style>();
    private String[] styleIds = new String[0];
    private int lfoCount;
    private long next;
    private long budget;
    private int units;
    private int papIstd;
    private int papAt;
    private int papLen;
    private int cachePiece = -2;
    private final Map<Long, String> rprCache = new HashMap<Long, String>();
    private final Map<Long, Integer> footRefs = new HashMap<Long, Integer>();
    private final Map<Long, Integer> endRefs = new HashMap<Long, Integer>();
    private final Set<Long> sectEnds = new HashSet<Long>();
    private final LinkedHashMap<String, byte[]> parts = new LinkedHashMap<String, byte[]>();
    private final StringBuilder rels = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
    private final StringBuilder types = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
    private int pictures;
    private int relIds;
    /// Comment-range boundaries emitted before the main-text character at a CP.
    private final Map<Long, StringBuilder> marks = new HashMap<Long, StringBuilder>();
    private final Map<Long, Integer> atnRefs = new HashMap<Long, Integer>();
    private final Map<Long, String> floats = new HashMap<Long, String>();
    private final List<String> rmAuthors = new ArrayList<String>();
    /// OfficeArt BLIP store entries as {FBSE body offset, length} in the table stream.
    private final List<long[]> blips = new ArrayList<long[]>();
    /// Main-drawing shapes by spid: {spid, shape type, picture index (pib)}.
    private final Map<Long, long[]> shapes = new HashMap<Long, long[]>();
    private int revIds;
    private int floatIds;

    private static final class Style {
        int stk;
        int base = 0xFFF;
        String name = "";
        byte[] papx = new byte[0];
        byte[] chpx = new byte[0];
    }

    private static final class Chr {
        Boolean b;
        Boolean i;
        Boolean strike;
        Boolean caps;
        Boolean smallCaps;
        Boolean vanish;
        Boolean ins;
        Boolean del;
        int insBy = -1;
        int delBy = -1;
        long insAt;
        long delAt;
        String u;
        String color;
        String font;
        String highlight;
        String vert;
        String style;
        int sz;

        String xml() {
            StringBuilder s = new StringBuilder();
            if (style != null) {
                s.append("<w:rStyle w:val=\"").append(style).append("\"/>");
            }
            if (font != null) {
                s.append("<w:rFonts w:ascii=\"")
                        .append(font)
                        .append("\" w:hAnsi=\"")
                        .append(font)
                        .append("\" w:cs=\"")
                        .append(font)
                        .append("\"/>");
            }
            flag(s, "b", b);
            flag(s, "i", i);
            flag(s, "caps", caps);
            flag(s, "smallCaps", smallCaps);
            flag(s, "strike", strike);
            flag(s, "vanish", vanish);
            if (color != null) {
                s.append("<w:color w:val=\"").append(color).append("\"/>");
            }
            if (sz > 0) {
                s.append("<w:sz w:val=\"").append(sz).append("\"/>");
            }
            if (highlight != null) {
                s.append("<w:highlight w:val=\"").append(highlight).append("\"/>");
            }
            if (u != null) {
                s.append("<w:u w:val=\"").append(u).append("\"/>");
            }
            if (vert != null) {
                s.append("<w:vertAlign w:val=\"").append(vert).append("\"/>");
            }
            return s.length() == 0 ? "" : "<w:rPr>" + s + "</w:rPr>";
        }

        private static void flag(StringBuilder s, String name, Boolean v) {
            if (v != null) {
                s.append("<w:").append(name).append(v ? "/>" : " w:val=\"0\"/>");
            }
        }
    }

    private static final class PPr {
        String jc;
        String style;
        Integer left;
        Integer right;
        Integer first;
        Integer before;
        Integer after;
        Integer line;
        String lineRule;
        boolean keep;
        boolean keepNext;
        boolean pageBreak;
        boolean inTable;
        boolean ttp;
        boolean innerTtp;
        boolean borders;
        int outline = -1;
        int ilvl;
        int ilfo;
        int itap;
        int[] grid;
        int[] merge;
        /// Per-cell tcPr children (tcBorders from the TC80s, shd from the shading sprms).
        String[] cellBorders;
        String[] cellShd;
        /// The paragraph mark's own tracked insertion or deletion (an empty w:ins/w:del element).
        String markRev;

        String xml(int lists) {
            StringBuilder s = new StringBuilder();
            if (style != null) {
                s.append("<w:pStyle w:val=\"").append(style).append("\"/>");
            }
            if (keepNext) {
                s.append("<w:keepNext/>");
            }
            if (keep) {
                s.append("<w:keepLines/>");
            }
            if (pageBreak) {
                s.append("<w:pageBreakBefore/>");
            }
            int num = ilfo >= 1 && ilfo <= 0x7FE ? ilfo : ilfo <= -1 && ilfo >= -0x7FE ? -ilfo : 0;
            if (num > 0 && num <= lists) {
                s.append("<w:numPr><w:ilvl w:val=\"")
                        .append(Math.min(8, Math.max(0, ilvl)))
                        .append("\"/><w:numId w:val=\"")
                        .append(num)
                        .append("\"/></w:numPr>");
            }
            if (before != null || after != null || line != null) {
                s.append("<w:spacing");
                if (before != null) {
                    s.append(" w:before=\"").append(before).append('"');
                }
                if (after != null) {
                    s.append(" w:after=\"").append(after).append('"');
                }
                if (line != null) {
                    s.append(" w:line=\"").append(line).append("\" w:lineRule=\"").append(lineRule).append('"');
                }
                s.append("/>");
            }
            if (left != null || right != null || first != null) {
                s.append("<w:ind");
                if (left != null) {
                    s.append(" w:left=\"").append(left).append('"');
                }
                if (right != null) {
                    s.append(" w:right=\"").append(right).append('"');
                }
                if (first != null) {
                    s.append(first < 0 ? " w:hanging=\"" : " w:firstLine=\"").append(Math.abs(first)).append('"');
                }
                s.append("/>");
            }
            if (jc != null) {
                s.append("<w:jc w:val=\"").append(jc).append("\"/>");
            }
            if (outline >= 0 && outline < 9) {
                s.append("<w:outlineLvl w:val=\"").append(outline).append("\"/>");
            }
            if (markRev != null) {
                s.append("<w:rPr>").append(markRev).append("</w:rPr>");
            }
            return s.toString();
        }
    }

    private static final class Para {
        final StringBuilder runs = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output
        String pPr = "";
        String sect;
        boolean cellEnd;
        boolean rowEnd;
        boolean borders;
        int depth;
        int[] grid;
        int[] merge;
        String[] cellBorders;
        String[] cellShd;

        String xml() {
            String props = sect == null ? pPr : pPr + sect;
            return "<w:p>" + (props.length() == 0 ? "" : "<w:pPr>" + props + "</w:pPr>") + runs + "</w:p>";
        }
    }

    private DocReader(Map<String, byte[]> streams) throws IOException {
        byte[] w = streams.get("WordDocument");
        if (w == null) {
            throw new IOException("Not a Word document: no WordDocument stream");
        }
        if (w.length < 0x9A) {
            throw new IOException("Not a Word 97-2003 document");
        }
        int ident = u16(w, 0);
        if (ident == 0xA5DC) {
            throw new IOException("Word 6.0/95 documents are not supported");
        }
        if (ident != 0xA5EC) {
            throw new IOException("Not a Word 97-2003 document");
        }
        int flags = u16(w, 0x0A);
        if ((flags & 0x0100) != 0) {
            throw new IOException("encrypted .doc files are not supported");
        }
        if (u16(w, 0x20) != 14 || u16(w, 0x3E) != 22) {
            throw new IOException("Damaged .doc file header");
        }
        word = w;
        ccpText = ccp(3);
        ccpFtn = ccp(4);
        ccpHdd = ccp(5);
        ccpAtn = ccp(7);
        ccpEdn = ccp(8);
        int cb = u16(w, 0x98);
        if (cb == 0 || w.length < 0x9A + cb * 8) {
            throw new IOException("Damaged .doc file header");
        }
        pairs = new long[cb * 2];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = u32(w, 0x9A + i * 4);
        }
        boolean one = (flags & 0x0200) != 0;
        byte[] t = streams.get(one ? "1Table" : "0Table");
        if (t == null) {
            t = streams.get(one ? "0Table" : "1Table");
        }
        table = t == null ? new byte[0] : t;
        byte[] d = streams.get("Data");
        data = d == null ? new byte[0] : d;
    }

    /// Reads a `.doc` file into a `Document`.
    private static IOException damaged(String message, Throwable cause) {
        IOException e = new IOException(message);
        e.initCause(cause);
        return e;
    }

    static Document read(byte[] bytes) throws IOException {
        try {
            return DocxReader.read(convert(bytes));
        } catch (RuntimeException ex) {
            IOException e = new IOException("Unreadable .doc file: " + ex.getMessage());
            e.initCause(ex);
            throw e;
        }
    }

    /// Translates a `.doc` file into the parts of an equivalent DOCX package.
    static Map<String, byte[]> convert(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 512 || !CompoundFile.hasSignature(bytes)) {
            throw new IOException("Not a Word 97-2003 document");
        }
        Map<String, byte[]> streams;
        try {
            streams = CompoundFile.read(bytes);
        } catch (RuntimeException ex) {
            throw damaged("Damaged .doc container", ex);
        }
        try {
            return new DocReader(streams).build();
        } catch (RuntimeException ex) {
            throw damaged("Damaged .doc file: " + ex, ex);
        } catch (StackOverflowError ex) {
            throw damaged("Damaged .doc file: " + ex, ex);
        }
    }

    private long ccp(int i) {
        int v = (int) u32(word, 0x40 + i * 4);
        return Math.max(0, v);
    }

    private long[] pair(int i) {
        if (i * 2 + 1 >= pairs.length || pairs[i * 2 + 1] == 0) {
            return null;
        }
        return new long[] {pairs[i * 2], pairs[i * 2 + 1]};
    }

    private Map<String, byte[]> build() throws IOException {
        parsePieces();
        chpFcs = bins(12);
        chpPns = binPns;
        papFcs = bins(13);
        papPns = binPns;
        parseFonts();
        parseStyles();
        String numbering = numbering();
        rmAuthors.addAll(sttb(51, null));
        long ftnBase = ccpText;
        long ednBase = ccpText + ccpFtn + ccpHdd + ccpAtn;
        String footnotes = notes(2, 3, ftnBase, ccpFtn, "footnote", footRefs);
        String endnotes = notes(46, 47, ednBase, ccpEdn, "endnote", endRefs);
        String comments = comments();
        floats();
        List<long[]> sects = sections();
        for (long[] s : sects) {
            sectEnds.add(s[0]);
        }
        List<Ended> paras = walkEnded(0, ccpText, null);
        int pi = 0;
        for (int si = 0; si + 1 < sects.size(); si++) {
            long end = sects.get(si)[0];
            while (pi < paras.size() && paras.get(pi).end < end) {
                pi++;
            }
            if (pi < paras.size() && paras.get(pi).end == end) {
                paras.get(pi).para.sect = sectPr(sects.get(si));
            }
        }
        ArrayList<Para> list = new ArrayList<Para>();
        for (Ended e : paras) {
            list.add(e.para);
        }
        String body = blocks(list);
        if (body.length() == 0) {
            body = "<w:p/>";
        }
        body += sectPr(sects.get(sects.size() - 1));
        parts.put("word/document.xml",
                utf8(xmlDecl() + "<w:document" + NS + "><w:body>" + body + "</w:body></w:document>"));
        parts.put("word/styles.xml", utf8(stylesXml()));
        rel("styles", "styles.xml");
        override("/word/styles.xml", CT + "styles+xml");
        if (numbering != null) {
            parts.put("word/numbering.xml", utf8(numbering));
            rel("numbering", "numbering.xml");
            override("/word/numbering.xml", CT + "numbering+xml");
        }
        if (footnotes != null) {
            parts.put("word/footnotes.xml", utf8(footnotes));
            rel("footnotes", "footnotes.xml");
            override("/word/footnotes.xml", CT + "footnotes+xml");
        }
        if (endnotes != null) {
            parts.put("word/endnotes.xml", utf8(endnotes));
            rel("endnotes", "endnotes.xml");
            override("/word/endnotes.xml", CT + "endnotes+xml");
        }
        if (comments != null) {
            parts.put("word/comments.xml", utf8(comments));
            rel("comments", "comments.xml");
            override("/word/comments.xml", CT + "comments+xml");
        }
        parts.put("word/_rels/document.xml.rels",
                utf8(xmlDecl()
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + rels + "</Relationships>"));
        parts.put("_rels/.rels",
                utf8(xmlDecl()
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"" + REL
                        + "officeDocument\" Target=\"word/document.xml\"/></Relationships>"));
        parts.put("[Content_Types].xml",
                utf8(xmlDecl() + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + ("<Default Extension=\"rels\" "
                           + "ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Default Extension=\"png\" ContentType=\"image/png\"/>"
                        + "<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>"
                        + "<Default Extension=\"emf\" ContentType=\"image/x-emf\"/>"
                        + "<Default Extension=\"wmf\" ContentType=\"image/x-wmf\"/>"
                        + "<Override PartName=\"/word/document.xml\" ContentType=\"" + CT + "document.main+xml\"/>"
                        + types + "</Types>"));
        return parts;
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not supported"); //NOPMD PreserveStackTrace - UTF-8 always exists
        }
    }

    private static String xmlDecl() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    }

    private String rel(String type, String target) {
        String id = "rIdB" + (++relIds);
        rels.append("<Relationship Id=\"")
                .append(id)
                .append("\" Type=\"")
                .append(REL)
                .append(type)
                .append("\" Target=\"")
                .append(target)
                .append("\"/>");
        return id;
    }

    private void override(String part, String type) {
        types.append("<Override PartName=\"").append(part).append("\" ContentType=\"").append(type).append("\"/>");
    }

    // ---- byte helpers ----

    private static int u8(byte[] b, long at) {
        return at >= 0 && at < b.length ? b[(int) at] & 0xff : 0;
    }

    private static int u16(byte[] b, long at) {
        return u8(b, at) | u8(b, at + 1) << 8;
    }

    private static int s16(byte[] b, long at) {
        return (short) u16(b, at);
    }

    private static long u32(byte[] b, long at) {
        return u16(b, at) | ((long) u16(b, at + 2) << 16);
    }

    private static boolean in(byte[] b, long at, long len) {
        return at >= 0 && len >= 0 && at + len <= b.length;
    }

    private static byte[] copy(byte[] b, long at, long len) {
        if (!in(b, at, len)) {
            at = Math.max(0, Math.min(at, b.length));
            len = Math.max(0, Math.min(len, b.length - at));
        }
        byte[] out = new byte[(int) len];
        System.arraycopy(b, (int) at, out, 0, (int) len);
        return out;
    }

    /// Index of the last entry <= v, or -1.
    private static int floor(long[] a, int n, long v) {
        int lo = 0;
        int hi = Math.min(n, a.length) - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= v) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found;
    }

    private static void sprms(byte[] b, int start, int len, Prl f) {
        int i = start;
        int end = Math.min(b.length, start + len);
        while (i + 2 <= end) {
            int op = u16(b, i);
            int a = i + 2;
            int n;
            if (op == 0xC615) {
                if (a >= end) {
                    return;
                }
                int first = u8(b, a);
                if (first == 255) {
                    int del = u8(b, a + 1);
                    n = 1 + 1 + 4 * del + 1 + 3 * u8(b, a + 2 + 4 * del);
                } else {
                    n = 1 + first;
                }
            } else if (op == 0xD608) {
                n = 1 + u16(b, a);
            } else {
                switch (op >>> 13) {
                    case 0:
                    case 1:
                        n = 1;
                        break;
                    case 2:
                    case 4:
                    case 5:
                        n = 2;
                        break;
                    case 3:
                        n = 4;
                        break;
                    case 7:
                        n = 3;
                        break;
                    case 6:
                        if (a >= end) {
                            return;
                        }
                        n = 1 + u8(b, a);
                        break;
                    default:
                        return;
                }
            }
            if (a + n > end) {
                return;
            }
            f.op(op, b, a, n);
            i = a + n;
        }
    }

    // ---- piece table and FKPs ----

    private void parsePieces() throws IOException {
        long[] p = pair(33);
        if (p == null || !in(table, p[0], p[1])) {
            throw new IOException("Damaged .doc file: no piece table");
        }
        int i = (int) p[0];
        int end = (int) (p[0] + p[1]);
        int start;
        int lcb;
        while (true) {
            if (i >= end) {
                throw new IOException("Damaged .doc piece table");
            }
            int t = u8(table, i);
            if (t == 1) {
                int cb = u16(table, i + 1);
                if (i + 3 + cb > end) {
                    throw new IOException("Damaged .doc piece table");
                }
                if (prc.size() < MAX_PIECES) {
                    prc.add(copy(table, i + 3, cb));
                }
                i += 3 + cb;
            } else if (t == 2) {
                long l = u32(table, i + 1);
                start = i + 5;
                if (start + l > end) {
                    throw new IOException("Damaged .doc piece table");
                }
                lcb = (int) l;
                break;
            } else {
                throw new IOException("Damaged .doc piece table");
            }
        }
        if (lcb < 4 || (lcb - 4) % 12 != 0) {
            throw new IOException("Damaged .doc piece table");
        }
        int n = (lcb - 4) / 12;
        if (n > MAX_PIECES) {
            throw new IOException("Too many pieces in the .doc piece table");
        }
        cps = new long[n + 1];
        pfc = new long[n];
        pcomp = new boolean[n];
        pprm = new int[n];
        for (int j = 0; j <= n; j++) {
            cps[j] = Math.max(u32(table, start + j * 4), j == 0 ? 0 : cps[j - 1]);
        }
        for (int j = 0; j < n; j++) {
            int at = start + (n + 1) * 4 + j * 8 + 2;
            long raw = u32(table, at);
            pfc[j] = raw & 0x3FFFFFFFL;
            pcomp[j] = (raw & 0x40000000L) != 0;
            pprm[j] = u16(table, at + 4);
        }
    }

    private int piece(long cp) {
        int j = floor(cps, cps.length, cp);
        return j >= 0 && j < pfc.length ? j : -1;
    }

    private long fcOf(long cp) {
        int j = piece(cp);
        if (j < 0) {
            return -1;
        }
        long off = cp - cps[j];
        return pcomp[j] ? pfc[j] / 2 + off : pfc[j] + off * 2;
    }

    /// The character at cp (sets units), or -1 with next set to the next readable cp.
    private int charAt(long cp) {
        int j = floor(cps, cps.length, cp);
        if (j < 0) {
            next = cps.length > 0 ? cps[0] : Long.MAX_VALUE;
            return -1;
        }
        if (j >= pfc.length) {
            next = Long.MAX_VALUE;
            return -1;
        }
        long nx = cps[j + 1];
        long off = cp - cps[j];
        units = 1;
        if (pcomp[j]) {
            long at = pfc[j] / 2 + off;
            if (at >= word.length) {
                next = nx;
                return -1;
            }
            int b = word[(int) at] & 0xff;
            return b >= 0x80 && b < 0xA0 ? CP1252.charAt(b - 0x80) : b;
        }
        long at = pfc[j] + off * 2;
        if (at + 2 > word.length) {
            next = nx;
            return -1;
        }
        int u = u16(word, at);
        if (u >= 0xD800 && u < 0xDC00 && cp + 1 < nx && at + 4 <= word.length) {
            int lo = u16(word, at + 2);
            if (lo >= 0xDC00 && lo < 0xE000) {
                units = 2;
                return 0x10000 + ((u - 0xD800) << 10) + (lo - 0xDC00);
            }
        }
        if (u >= 0xD800 && u < 0xE000 || u == 0xFFFE || u == 0xFFFF) {
            return 0xFFFD;
        }
        return u;
    }

    private long[] binPns;

    private long[] bins(int pair) {
        binPns = new long[0];
        long[] p = pair(pair);
        if (p == null || p[1] < 4 || (p[1] - 4) % 8 != 0 || !in(table, p[0], p[1])) {
            return new long[0];
        }
        int n = (int) ((p[1] - 4) / 8);
        if (n > MAX_BINS) {
            return new long[0];
        }
        long[] fcs = new long[n + 1];
        binPns = new long[n];
        for (int i = 0; i <= n; i++) {
            fcs[i] = u32(table, p[0] + i * 4);
        }
        for (int i = 0; i < n; i++) {
            binPns[i] = u32(table, p[0] + (n + 1) * 4 + i * 4) & 0x3FFFFF;
        }
        return fcs;
    }

    private int page(long[] fcs, long[] pns, long fc) {
        int j = floor(fcs, fcs.length, fc);
        if (j < 0 || j >= pns.length) {
            return -1;
        }
        long at = pns[j] * 512;
        return in(word, at, 512) ? (int) at : -1;
    }

    /// The CHPX grpprl {offset, length} in the WordDocument stream for fc, or null.
    private int[] chpx(long fc) {
        int page = page(chpFcs, chpPns, fc);
        if (page < 0) {
            return null;
        }
        int crun = Math.min(u8(word, page + 511), 0x65);
        int j = -1;
        for (int k = crun - 1; k >= 0; k--) {
            if (u32(word, page + k * 4) <= fc) {
                j = k;
                break;
            }
        }
        if (j < 0) {
            return null;
        }
        int off = u8(word, page + (crun + 1) * 4 + j);
        if (off == 0) {
            return null;
        }
        int at = page + off * 2;
        int cb = Math.min(u8(word, at), Math.max(0, 511 - off * 2));
        return new int[] {at + 1, cb};
    }

    /// Locates the PAPX for fc into papIstd/papAt/papLen.
    private void papx(long fc) {
        papIstd = 0;
        papAt = 0;
        papLen = 0;
        int page = page(papFcs, papPns, fc);
        if (page < 0) {
            return;
        }
        int crun = u8(word, page + 511);
        crun = Math.min(crun, (511 - (crun + 1) * 4) / 13);
        int j = -1;
        for (int k = crun - 1; k >= 0; k--) {
            if (u32(word, page + k * 4) <= fc) {
                j = k;
                break;
            }
        }
        if (j < 0) {
            return;
        }
        int off = u8(word, page + (crun + 1) * 4 + j * 13);
        if (off == 0) {
            return;
        }
        int at = page + off * 2;
        int cb = u8(word, at);
        int dataAt;
        int len;
        if (cb != 0) {
            dataAt = at + 1;
            len = 2 * cb - 1;
        } else {
            dataAt = at + 2;
            len = 2 * u8(word, at + 1);
        }
        len = Math.min(len, page + 512 - dataAt);
        if (len < 2) {
            return;
        }
        papIstd = u16(word, dataAt);
        papAt = dataAt + 2;
        papLen = len - 2;
    }

    // ---- properties ----

    private static final String[] ICO = {null, "000000", "0000FF", "00FFFF", "00FF00", "FF00FF", "FF0000", "FFFF00",
            "FFFFFF", "000080", "008080", "008000", "800080", "800000", "808000", "808080", "C0C0C0"};
    private static final String[] HIGHLIGHT = {null, "black", "blue", "cyan", "green", "magenta", "red", "yellow",
            "white", "darkBlue", "darkCyan", "darkGreen", "darkMagenta", "darkRed", "darkYellow", "darkGray",
            "lightGray"};

    private static Boolean toggle(byte[] b, int at, Boolean old) {
        int v = u8(b, at);
        return v == 0 ? Boolean.FALSE : v == 0x80 ? old : Boolean.TRUE;
    }

    private static String hex(int r, int g, int b) {
        String s = Integer.toHexString(0x1000000 | r << 16 | g << 8 | b);
        StringBuilder out = new StringBuilder(6);
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= 'a' && c <= 'f' ? (char) (c - 'a' + 'A') : c);
        }
        return out.toString();
    }

    private void applyChr(Chr c, int op, byte[] b, int a) {
        switch (op) {
            case 0x0801:
                c.ins = toggle(b, a, c.ins);
                break;
            case 0x0800:
                c.del = toggle(b, a, c.del);
                break;
            case 0x4804:
                c.insBy = u16(b, a);
                break;
            case 0x6805:
                c.insAt = u32(b, a);
                break;
            case 0x4863:
                c.delBy = u16(b, a);
                break;
            case 0x6864:
                c.delAt = u32(b, a);
                break;
            case 0x0835:
                c.b = toggle(b, a, c.b);
                break;
            case 0x0836:
                c.i = toggle(b, a, c.i);
                break;
            case 0x0837:
                c.strike = toggle(b, a, c.strike);
                break;
            case 0x083A:
                c.smallCaps = toggle(b, a, c.smallCaps);
                break;
            case 0x083B:
                c.caps = toggle(b, a, c.caps);
                break;
            case 0x083C:
                c.vanish = toggle(b, a, c.vanish);
                break;
            case 0x2A3E: {
                int k = u8(b, a);
                String[] kul = {"none", "single", "words", "double", "dotted", null, "thick", "dash", null, "dotDash",
                        "dotDotDash", "wave"};
                c.u = k < kul.length && kul[k] != null ? kul[k] : "single";
                break;
            }
            case 0x2A42: {
                int k = u8(b, a);
                c.color = k > 0 && k < ICO.length ? ICO[k] : k == 0 ? "auto" : c.color;
                break;
            }
            case 0x6870:
                c.color = u8(b, a + 3) != 0 ? "auto" : hex(u8(b, a), u8(b, a + 1), u8(b, a + 2));
                break;
            case 0x4A43: {
                int v = u16(b, a);
                if (v > 0 && v <= 3276) {
                    c.sz = v;
                }
                break;
            }
            case 0x4845: {
                int v = (byte) u8(b, a);
                c.vert = v > 0 ? "superscript" : v < 0 ? "subscript" : null;
                break;
            }
            case 0x2A48: {
                int v = u8(b, a);
                c.vert = v == 1 ? "superscript" : v == 2 ? "subscript" : null;
                break;
            }
            case 0x4A4F: {
                int v = u16(b, a);
                if (v < fonts.size() && fonts.get(v).length() > 0) {
                    c.font = esc(fonts.get(v));
                }
                break;
            }
            case 0x2A0C: {
                int k = u8(b, a);
                c.highlight = k > 0 && k < HIGHLIGHT.length ? HIGHLIGHT[k] : null;
                break;
            }
            case 0x4A30: {
                int v = u16(b, a);
                if (v < styleIds.length && styleIds[v] != null && v < styles.size() && styles.get(v).stk == 2
                        && v != 10) {
                    c.style = styleIds[v];
                }
                break;
            }
            default:
        }
    }

    private void applyPara(PPr p, int op, byte[] b, int a, int n) {
        switch (op) {
            case 0x2461:
            case 0x2403: {
                int v = u8(b, a);
                p.jc = v == 1 ? "center" : v == 2 ? "right" : v == 3 || v == 4 ? "both" : "left";
                break;
            }
            case 0x845E:
            case 0x840F:
                p.left = s16(b, a);
                break;
            case 0x845D:
            case 0x840E:
                p.right = s16(b, a);
                break;
            case 0x8460:
            case 0x8411:
                p.first = s16(b, a);
                break;
            case 0xA413:
                p.before = u16(b, a);
                break;
            case 0xA414:
                p.after = u16(b, a);
                break;
            case 0x6412: {
                int dya = s16(b, a);
                int mult = s16(b, a + 2);
                if (mult != 0) {
                    p.line = Math.max(1, Math.abs(dya));
                    p.lineRule = "auto";
                } else {
                    p.line = Math.max(1, Math.abs(dya));
                    p.lineRule = dya < 0 ? "exact" : "atLeast";
                }
                break;
            }
            case 0x2405:
                p.keep = u8(b, a) != 0;
                break;
            case 0x2406:
                p.keepNext = u8(b, a) != 0;
                break;
            case 0x2407:
                p.pageBreak = u8(b, a) != 0;
                break;
            case 0x2640:
                p.outline = u8(b, a);
                break;
            case 0x260A:
                p.ilvl = u8(b, a);
                break;
            case 0x460B:
                p.ilfo = s16(b, a);
                break;
            case 0x2416:
                p.inTable = u8(b, a) != 0;
                break;
            case 0x2417:
                p.ttp = u8(b, a) != 0;
                break;
            case 0x244C:
                p.innerTtp = u8(b, a) != 0;
                break;
            case 0x6649:
                p.itap = (int) Math.min(64, Math.max(0, (int) u32(b, a)));
                if (p.itap > 0) {
                    p.inTable = true;
                }
                break;
            case 0xD608: {
                int count = Math.min(63, u8(b, a + 2));
                if (count < 1 || n < 3 + (count + 1) * 2) {
                    break;
                }
                p.grid = new int[count];
                for (int k = 0; k < count; k++) {
                    p.grid[k] = Math.max(1, s16(b, a + 3 + (k + 1) * 2) - s16(b, a + 3 + k * 2));
                }
                int tc = a + 3 + (count + 1) * 2;
                p.cellBorders = new String[count];
                for (int k = 0; k < count && tc + k * 20 + 20 <= a + n; k++) {
                    StringBuilder cb = new StringBuilder();
                    for (int e = 0; e < 4; e++) {
                        long brc = u32(b, tc + k * 20 + 4 + e * 4);
                        if (brc != 0 && brc != 0xFFFFFFFFL) {
                            p.borders = true;
                        }
                        // TC80 rgbrc: top, left, bottom, right Brc80s (nil = 0xFFFFFFFF, none = 0).
                        if (brc != 0xFFFFFFFFL) {
                            cb.append(brc80(new String[] {"top", "left", "bottom", "right"}[e], brc));
                        }
                    }
                    if (cb.length() > 0) {
                        p.cellBorders[k] = "<w:tcBorders>" + cb + "</w:tcBorders>";
                    }
                }
                // TC80 flags: fFirstMerged, fMerged, fVertMerge (0x20), fVertRestart (0x40)
                p.merge = new int[64];
                for (int k = 0; k < count && tc + k * 20 + 2 <= a + n; k++) {
                    int g = u16(b, tc + k * 20);
                    p.merge[k] = (g & 3) | ((g & 0x20) != 0 ? 4 : 0) | ((g & 0x40) != 0 ? 8 : 0);
                }
                break;
            }
            case 0xD609:
                // sprmTDefTableShd80: one Shd80 per cell.
                for (int k = 0; k < 63 && 1 + k * 2 + 2 <= n; k++) {
                    cellShd(p, k, shd80(u16(b, a + 1 + k * 2)));
                }
                break;
            case 0xD612:
                // sprmTDefTableShd: one 10-byte Shd per cell.
                for (int k = 0; k < 63 && 1 + k * 10 + 10 <= n; k++) {
                    cellShd(p, k, shd(b, a + 1 + k * 10));
                }
                break;
            case 0xD62D: {
                // sprmTSetShd: cb, itcFirst, itcLim, Shd.
                if (n < 13) {
                    break;
                }
                String fill = shd(b, a + 3);
                for (int k = u8(b, a + 1); k < Math.min(63, u8(b, a + 2)); k++) {
                    cellShd(p, k, fill);
                }
                break;
            }
            case 0x5624: {
                if (p.merge == null) {
                    p.merge = new int[64];
                }
                int f = u8(b, a);
                int l = Math.min(p.merge.length, u8(b, a + 1));
                for (int k = f; k < l; k++) {
                    p.merge[k] = (p.merge[k] & ~3) | (k == f ? 1 : 2);
                }
                break;
            }
            case 0xD62B: {
                if (p.merge == null) {
                    p.merge = new int[64];
                }
                int k = u8(b, a + 1);
                int v = u8(b, a + 2);
                if (k < p.merge.length) {
                    p.merge[k] = (p.merge[k] & 3) | (v == 1 ? 4 : v == 3 ? 12 : 0);
                }
                break;
            }
            default:
        }
    }

    private void applyPrm(int j, final Chr c, final PPr p) {
        if (j < 0) {
            return;
        }
        int prm = pprm[j];
        if ((prm & 1) == 0) {
            int isprm = (prm >> 1) & 0x7F;
            int op = fromPrm0(isprm);
            if (op == 0) {
                return;
            }
            byte[] v = {(byte) (prm >> 8), 0};
            if (c != null && (op >> 10 & 7) == 2) {
                applyChr(c, op, v, 0);
            }
            if (p != null && (op >> 10 & 7) == 1) {
                applyPara(p, op, v, 0, 1);
            }
        } else {
            int idx = prm >> 1;
            if (idx >= prc.size()) {
                return;
            }
            byte[] g = prc.get(idx);
            sprms(g, 0, g.length, new Prl() {
                @Override
                void op(int op, byte[] b, int a, int n) {
                    if (c != null && (op >> 10 & 7) == 2) {
                        applyChr(c, op, b, a);
                    }
                    if (p != null && (op >> 10 & 7) == 1) {
                        applyPara(p, op, b, a, n);
                    }
                }
            });
        }
    }

    private static void cellShd(PPr p, int k, String fill) {
        if (fill == null) {
            return;
        }
        if (p.cellShd == null) {
            p.cellShd = new String[63];
        }
        p.cellShd[k] = "<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + fill + "\"/>";
    }

    /// A Shd80's fill (icoFore for solid, else icoBack); null when none.
    private static String shd80(int v) {
        int fore = v & 0x1F;
        int back = v >> 5 & 0x1F;
        int ipat = v >> 10;
        if (ipat == 0 || (fore == 0x1F && back == 0x1F)) {
            return null;
        }
        int k = ipat == 1 ? fore : back;
        return k > 0 && k < ICO.length ? ICO[k] : null;
    }

    /// A 10-byte Shd's fill (cvFore for solid, else cvBack); null when none.
    private static String shd(byte[] b, int at) {
        int ipat = u16(b, at + 8);
        if (ipat == 0 || ipat == 0xFFFF) {
            return null;
        }
        int cv = ipat == 1 ? at : at + 4;
        if (u8(b, cv + 3) == 0xFF) {
            return null;
        }
        return hex(u8(b, cv), u8(b, cv + 1), u8(b, cv + 2));
    }

    /// One Brc80 as a tcBorders side: width in eighths of a point, brcType, ico colour.
    private static String brc80(String side, long brc) {
        int w = (int) (brc & 255);
        int type = (int) (brc >> 8 & 255);
        int ico = (int) (brc >> 16 & 255);
        if (type == 0 || w == 0) {
            return "<w:" + side + " w:val=\"nil\"/>";
        }
        String val = type == 3            ? "double"
                : type == 6               ? "dotted"
                : type == 7 || type == 22 ? "dashed"
                : type == 5               ? "thick"
                                          : "single";
        String color = ico > 0 && ico < ICO.length ? ICO[ico] : "auto";
        return "<w:" + side + " w:val=\"" + val + "\" w:sz=\"" + Math.max(2, Math.min(96, w))
                + "\" w:space=\"0\" w:color=\"" + color + "\"/>";
    }

    private static int fromPrm0(int isprm) {
        switch (isprm) {
            case 0x05:
                return 0x2461;
            case 0x07:
                return 0x2405;
            case 0x08:
                return 0x2406;
            case 0x09:
                return 0x2407;
            case 0x4D:
                return 0x2A0C;
            case 0x55:
                return 0x0835;
            case 0x56:
                return 0x0836;
            case 0x57:
                return 0x0837;
            case 0x5A:
                return 0x083A;
            case 0x5B:
                return 0x083B;
            case 0x5C:
                return 0x083C;
            case 0x5E:
                return 0x2A3E;
            case 0x62:
                return 0x2A42;
            default:
                return 0;
        }
    }

    private String rpr(long cp) {
        int j = piece(cp);
        if (j != cachePiece) {
            rprCache.clear();
            cachePiece = j;
        }
        int[] r = chpx(fcOf(cp));
        long key = r == null ? -1 : (long) r[0] << 8 | r[1];
        String s = rprCache.get(key);
        if (s != null) {
            return s;
        }
        final Chr c = new Chr();
        if (r != null) {
            sprms(word, r[0], r[1], new Prl() {
                @Override
                void op(int op, byte[] b, int a, int n) {
                    applyChr(c, op, b, a);
                }
            });
        }
        applyPrm(j, c, null);
        s = revision(c) + c.xml();
        rprCache.put(key, s);
        return s;
    }

    /// The paragraph mark's tracked insertion/deletion as an empty w:ins/w:del, or null.
    private String markRevision(long cp) {
        int[] r = chpx(fcOf(cp));
        final Chr c = new Chr();
        if (r != null) {
            sprms(word, r[0], r[1], new Prl() {
                @Override
                void op(int op, byte[] b, int a, int n) {
                    applyChr(c, op, b, a);
                }
            });
        }
        applyPrm(piece(cp), c, null);
        String w = revision(c);
        if (w.length() == 0) {
            return null;
        }
        return w.substring(0, 6) + " w:id=\"" + ++revIds + "\"" + w.substring(6, w.length() - 2) + "/>";
    }

    // ---- fonts and styles ----

    private void parseFonts() {
        long[] p = pair(15);
        if (p == null || !in(table, p[0], p[1])) {
            return;
        }
        int base = (int) p[0];
        int end = (int) (p[0] + p[1]);
        int first = u16(table, base);
        int count;
        int extra;
        int at;
        boolean ext = first == 0xFFFF;
        if (ext) {
            count = u16(table, base + 2);
            extra = u16(table, base + 4);
            at = base + 6;
        } else {
            count = first;
            extra = u16(table, base + 2);
            at = base + 4;
        }
        for (int i = 0; i < Math.min(count, 1024) && at < end; i++) {
            int cb = ext ? u16(table, at) * 2 : u8(table, at);
            int start = at + (ext ? 2 : 1);
            int stop = Math.min(start + cb, end);
            StringBuilder name = new StringBuilder();
            for (int k = start + 39; k + 1 < stop && name.length() < 64; k += 2) {
                int u = u16(table, k);
                if (u == 0) {
                    break;
                }
                name.append(u >= 0xD800 && u < 0xE000 ? '\uFFFD' : (char) u);
            }
            fonts.add(name.toString());
            at = start + cb + extra;
        }
    }

    private void parseStyles() {
        long[] p = pair(1);
        if (p == null || !in(table, p[0], p[1]) || p[1] < 4) {
            styleIds = new String[0];
            return;
        }
        int base = (int) p[0];
        int end = (int) (p[0] + p[1]);
        int cbStshi = u16(table, base);
        int cstd = Math.min(u16(table, base + 2), MAX_STYLES);
        int cbBase = u16(table, base + 4) == 0x12 ? 18 : 10;
        int at = base + 2 + cbStshi;
        for (int i = 0; i < cstd && at + 2 <= end; i++) {
            int cb = u16(table, at);
            int std = at;
            at = Math.min(end, at + 2 + cb);
            Style st = new Style();
            styles.add(st);
            if (cb < cbBase) {
                continue;
            }
            int stdEnd = at;
            st.stk = Math.max(1, u8(table, std + 2 + 2) & 0x0F);
            st.base = u16(table, std + 2 + 2) >> 4;
            int cupx = u16(table, std + 2 + 4) & 0x0F;
            int k = std + 2 + cbBase;
            int cch = Math.min(256, u16(table, k));
            StringBuilder name = new StringBuilder();
            for (int c = 0; c < cch && k + 2 + c * 2 + 1 < stdEnd; c++) {
                int u = u16(table, k + 2 + c * 2);
                name.append(u >= 0xD800 && u < 0xE000 ? '\uFFFD' : (char) u);
            }
            st.name = name.toString();
            k += 2 + cch * 2 + 2;
            ArrayList<byte[]> upx = new ArrayList<byte[]>();
            for (int u = 0; u < Math.min(cupx, 3) && k + 2 <= stdEnd; u++) {
                int len = u16(table, k);
                upx.add(copy(table, k + 2, Math.min(len, Math.max(0, stdEnd - k - 2))));
                k += 2 + len + (len & 1);
            }
            if (st.stk == 1) {
                if (!upx.isEmpty() && upx.get(0).length >= 2) {
                    st.papx = copy(upx.get(0), 2, upx.get(0).length - 2);
                }
                if (upx.size() > 1) {
                    st.chpx = upx.get(1);
                }
            } else if (st.stk == 2 && !upx.isEmpty()) {
                st.chpx = upx.get(0);
            }
        }
        styleIds = new String[styles.size()];
        HashSet<String> used = new HashSet<String>();
        for (int i = 0; i < styles.size(); i++) {
            Style st = styles.get(i);
            if (st.name.length() == 0 || (st.stk != 1 && st.stk != 2)) {
                continue;
            }
            String id = styleId(st.name);
            if (id.length() == 0) {
                id = "Style" + i;
            }
            if (!used.add(id)) {
                id = id + i;
                used.add(id);
            }
            styleIds[i] = id;
        }
    }

    static String styleId(String name) {
        StringBuilder s = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ',') {
                break;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                s.append(up ? Character.toUpperCase(c) : c);
                up = false;
            } else {
                up = true;
            }
        }
        return s.toString();
    }

    private String stylesXml() {
        String font = fonts.isEmpty() || fonts.get(0).length() == 0 ? "Times New Roman" : esc(fonts.get(0));
        StringBuilder s =
                new StringBuilder(
                        xmlDecl() + "<w:styles" + NS + "><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=\"")
                        .append(font)
                        .append("\" w:hAnsi=\"")
                        .append(font)
                        .append("\"/><w:sz w:val=\"20\"/></w:rPr></w:rPrDefault><w:pPrDefault/></w:docDefaults>");
        for (int i = 0; i < styles.size(); i++) {
            if (styleIds[i] == null) {
                continue;
            }
            Style st = styles.get(i);
            String type = st.stk == 1 ? "paragraph" : "character";
            s.append("<w:style w:type=\"").append(type).append("\" w:styleId=\"").append(styleIds[i]).append('"');
            if (i == 0 || i == 10) {
                s.append(" w:default=\"1\"");
            }
            s.append("><w:name w:val=\"").append(esc(st.name)).append("\"/>");
            if (st.base < styles.size() && st.base != i && styleIds[st.base] != null
                    && styles.get(st.base).stk == st.stk) {
                s.append("<w:basedOn w:val=\"").append(styleIds[st.base]).append("\"/>");
            }
            if (st.stk == 1) {
                final PPr p = new PPr();
                sprms(st.papx, 0, st.papx.length, new Prl() {
                    @Override
                    void op(int op, byte[] b, int a, int n) {
                        applyPara(p, op, b, a, n);
                    }
                });
                String x = p.xml(lfoCount);
                if (x.length() > 0) {
                    s.append("<w:pPr>").append(x).append("</w:pPr>");
                }
            }
            final Chr c = new Chr();
            sprms(st.chpx, 0, st.chpx.length, new Prl() {
                @Override
                void op(int op, byte[] b, int a, int n) {
                    applyChr(c, op, b, a);
                }
            });
            c.style = null;
            s.append(c.xml()).append("</w:style>");
        }
        return s.append("</w:styles>").toString();
    }

    // ---- lists ----

    private static final class Lvl {
        long start;
        int nfc;
        int jc;
        int follow;
        int len;
        byte[] papx;
        byte[] chpx;
        int[] xst;
    }

    private Lvl lvl(int at) {
        if (!in(table, at, 28)) {
            return null;
        }
        Lvl l = new Lvl();
        l.start = Math.min(u32(table, at), 0x7FFF);
        l.nfc = u8(table, at + 4);
        l.jc = u8(table, at + 5) & 3;
        l.follow = u8(table, at + 15);
        int cbChpx = u8(table, at + 24);
        int cbPapx = u8(table, at + 25);
        int k = at + 28;
        l.papx = copy(table, k, cbPapx);
        k += cbPapx;
        l.chpx = copy(table, k, cbChpx);
        k += cbChpx;
        int cch = Math.min(64, u16(table, k));
        if (!in(table, k + 2, cch * 2L)) {
            return null;
        }
        l.xst = new int[cch];
        for (int i = 0; i < cch; i++) {
            l.xst[i] = u16(table, k + 2 + i * 2);
        }
        l.len = 28 + cbPapx + cbChpx + 2 + cch * 2;
        return l;
    }

    private static String numFmt(int nfc) {
        switch (nfc) {
            case 0:
                return "decimal";
            case 1:
                return "upperRoman";
            case 2:
                return "lowerRoman";
            case 3:
                return "upperLetter";
            case 4:
                return "lowerLetter";
            case 5:
                return "ordinal";
            case 6:
                return "cardinalText";
            case 7:
                return "ordinalText";
            case 22:
                return "decimalZero";
            case 23:
                return "bullet";
            case 255:
                return "none";
            default:
                return "decimal";
        }
    }

    private static char bullet(int c) {
        switch (c & 0x0FFF) {
            case 0xB7:
                return '\u2022';
            case 0xA7:
                return '\u25AA';
            case 0x6F:
                return '\u25CB';
            case 0x77:
            case 0x78:
                return '\u25CF';
            default:
                return c >= 0xF000 || c < 0x20 || (c >= 0xD800 && c < 0xE000) ? '\u2022' : (char) c;
        }
    }

    private String lvlXml(Lvl l, int ilvl) {
        StringBuilder s = new StringBuilder("<w:lvl w:ilvl=\"")
                                  .append(ilvl)
                                  .append("\"><w:start w:val=\"")
                                  .append(l.start)
                                  .append("\"/><w:numFmt w:val=\"")
                                  .append(numFmt(l.nfc))
                                  .append("\"/><w:suff w:val=\"")
                                  .append(l.follow == 1           ? "space"
                                                  : l.follow == 2 ? "nothing"
                                                                  : "tab")
                                  .append("\"/>");
        StringBuilder text = new StringBuilder();
        boolean bulleted = l.nfc == 23;
        for (int c : l.xst) {
            if (c < 9 && !bulleted) {
                text.append('%').append(c + 1);
            } else {
                text.append(bulleted ? bullet(c) : c < 0x20 || (c >= 0xD800 && c < 0xE000) ? '\uFFFD' : (char) c);
            }
        }
        s.append("<w:lvlText w:val=\"")
                .append(esc(text.toString()))
                .append("\"/><w:lvlJc w:val=\"")
                .append(l.jc == 1           ? "center"
                                : l.jc == 2 ? "right"
                                            : "left")
                .append("\"/>");
        final PPr p = new PPr();
        sprms(l.papx, 0, l.papx.length, new Prl() {
            @Override
            void op(int op, byte[] b, int a, int n) {
                applyPara(p, op, b, a, n);
            }
        });
        if (p.left != null || p.first != null) {
            s.append("<w:pPr><w:ind");
            if (p.left != null) {
                s.append(" w:left=\"").append(p.left).append('"');
            }
            if (p.first != null) {
                s.append(p.first < 0 ? " w:hanging=\"" : " w:firstLine=\"").append(Math.abs(p.first)).append('"');
            }
            s.append("/></w:pPr>");
        }
        final Chr c = new Chr();
        sprms(l.chpx, 0, l.chpx.length, new Prl() {
            @Override
            void op(int op, byte[] b, int a, int n) {
                applyChr(c, op, b, a);
            }
        });
        if (bulleted) {
            c.font = null;
        }
        c.style = null;
        return s.append(c.xml()).append("</w:lvl>").toString();
    }

    private String numbering() {
        long[] p = pair(73);
        if (p == null) {
            return null;
        }
        int at = (int) Math.min(p[0], Integer.MAX_VALUE);
        int count = u16(table, at);
        if (count == 0 || count > MAX_LISTS || !in(table, at, 2 + count * 28L)) {
            return null;
        }
        long[] lsids = new long[count];
        StringBuilder abstracts = new StringBuilder();
        int off = at + 2 + count * 28;
        ArrayList<List<Lvl>> levels = new ArrayList<List<Lvl>>();
        for (int i = 0; i < count; i++) {
            int f = at + 2 + i * 28;
            lsids[i] = u32(table, f);
            boolean simple = (u8(table, f + 26) & 1) != 0;
            ArrayList<Lvl> ls = new ArrayList<Lvl>();
            for (int k = 0; k < (simple ? 1 : 9); k++) {
                Lvl l = lvl(off);
                if (l == null) {
                    break;
                }
                ls.add(l);
                off += l.len;
            }
            levels.add(ls);
        }
        for (int i = 0; i < count; i++) {
            abstracts.append("<w:abstractNum w:abstractNumId=\"").append(i).append("\">");
            for (int k = 0; k < levels.get(i).size(); k++) {
                abstracts.append(lvlXml(levels.get(i).get(k), k));
            }
            abstracts.append("</w:abstractNum>");
        }
        StringBuilder nums = new StringBuilder();
        long[] q = pair(74);
        if (q != null) {
            int lat = (int) Math.min(q[0], Integer.MAX_VALUE);
            int mac = (int) Math.min(u32(table, lat), MAX_LISTS);
            if (!in(table, lat, 4 + mac * 16L)) {
                mac = 0;
            }
            int dat = lat + 4 + mac * 16;
            int abstractIds = count;
            for (int i = 0; i < mac; i++) {
                int f = lat + 4 + i * 16;
                int clfolvl = u8(table, f + 12);
                int def = -1;
                long lsid = u32(table, f);
                for (int k = 0; k < count && def < 0; k++) {
                    if (lsids[k] == lsid) {
                        def = k;
                    }
                }
                StringBuilder overrides = new StringBuilder();
                Lvl[] formatted = new Lvl[9];
                if (clfolvl > 0) {
                    dat += 4;
                    for (int k = 0; k < clfolvl; k++) {
                        if (!in(table, dat, 8)) {
                            break;
                        }
                        long start = Math.min(u32(table, dat), 0x7FFF);
                        int flags = u8(table, dat + 4);
                        int ilvl = flags & 0x0F;
                        dat += 8;
                        if ((flags & 0x20) != 0) {
                            Lvl l = lvl(dat);
                            if (l == null) {
                                break;
                            }
                            dat += l.len;
                            if (ilvl < 9) {
                                formatted[ilvl] = l;
                            }
                        }
                        if ((flags & 0x10) != 0 && ilvl < 9) {
                            overrides.append("<w:lvlOverride w:ilvl=\"")
                                    .append(ilvl)
                                    .append("\"><w:startOverride w:val=\"")
                                    .append(start)
                                    .append("\"/></w:lvlOverride>");
                        }
                    }
                } else {
                    dat += 4;
                }
                if (def < 0) {
                    def = 0;
                }
                int abstractId = def;
                boolean anyFormat = false;
                for (Lvl l : formatted) {
                    anyFormat |= l != null;
                }
                if (anyFormat) {
                    abstractId = abstractIds++;
                    abstracts.append("<w:abstractNum w:abstractNumId=\"").append(abstractId).append("\">");
                    for (int k = 0; k < levels.get(def).size(); k++) {
                        abstracts.append(lvlXml(formatted[k] != null ? formatted[k] : levels.get(def).get(k), k));
                    }
                    abstracts.append("</w:abstractNum>");
                }
                nums.append("<w:num w:numId=\"")
                        .append(i + 1)
                        .append("\"><w:abstractNumId w:val=\"")
                        .append(abstractId)
                        .append("\"/>")
                        .append(overrides)
                        .append("</w:num>");
                lfoCount = i + 1;
            }
        }
        if (lfoCount == 0) {
            return null;
        }
        return xmlDecl() + "<w:numbering" + NS + ">" + abstracts + nums + "</w:numbering>";
    }

    // ---- PLCs, notes and sections ----

    /// The CPs of a PLC with data elements of the given size.
    private List<long[]> plc(int pair, int size) {
        ArrayList<long[]> out = new ArrayList<long[]>();
        long[] p = pair(pair);
        if (p == null || p[1] < 4 || !in(table, p[0], p[1])) {
            return out;
        }
        long n = (p[1] - size) / (4 + size);
        for (int i = 0; i <= Math.min(n, MAX_SECTIONS * 6 + 12L + MAX_NOTES); i++) {
            if (p[0] + i * 4L + 4 > p[0] + p[1]) {
                break;
            }
            out.add(new long[] {u32(table, p[0] + i * 4L)});
        }
        return out;
    }

    private String notes(int refPair, int txtPair, long base, long len, String kind, Map<Long, Integer> refs)
            throws IOException {
        List<long[]> cp = plc(refPair, 2);
        List<long[]> txt = plc(txtPair, 0);
        if (txt.size() < 2 || cp.isEmpty()) {
            return null;
        }
        StringBuilder s = new StringBuilder(xmlDecl() + "<w:" + kind + "s" + NS + ">");
        s.append("<w:")
                .append(kind)
                .append(" w:type=\"separator\" w:id=\"-1\"><w:p><w:r><w:separator/></w:r></w:p></w:")
                .append(kind)
                .append('>');
        s.append("<w:")
                .append(kind)
                .append(" w:type=\"continuationSeparator\" "
                        + "w:id=\"0\"><w:p><w:r><w:continuationSeparator/></w:r></w:p></w:")
                .append(kind)
                .append('>');
        int id = 0;
        long n = (pair(refPair)[1] - 4) / 6;
        for (int i = 0; i < Math.min(n, MAX_NOTES) && i + 1 < txt.size(); i++) {
            long a = Math.min(txt.get(i)[0], len);
            long b = Math.min(txt.get(i + 1)[0], len);
            if (b <= a) {
                continue;
            }
            String body = blocks(walk(base + a, base + b, kind));
            if (body.length() == 0) {
                continue;
            }
            refs.put(cp.get(i)[0], ++id);
            s.append("<w:")
                    .append(kind)
                    .append(" w:id=\"")
                    .append(id)
                    .append("\">")
                    .append(body)
                    .append("</w:")
                    .append(kind)
                    .append('>');
        }
        return id == 0 ? null : s.append("</w:").append(kind).append("s>").toString();
    }

    /// Sections as {endCp, bkc, titlePg, cols, pgnStart, orient, hdr, ftr, w, h, left, right, top, bottom, gutter}.
    private List<long[]> sections() {
        ArrayList<long[]> out = new ArrayList<long[]>();
        long[] p = pair(6);
        if (p != null && p[1] >= 4 && (p[1] - 4) % 16 == 0 && in(table, p[0], p[1])) {
            int n = (int) Math.min((p[1] - 4) / 16, MAX_SECTIONS);
            long total = (p[1] - 4) / 16;
            for (int i = 0; i < n; i++) {
                final long[] s = {u32(table, p[0] + (i + 1) * 4L), 2, 0, 1, -1, 0, 720, 720, 12240, 15840, 1800, 1800,
                        1440, 1440, 0};
                long fcSepx = u32(table, p[0] + (total + 1) * 4 + i * 12L + 2);
                if (fcSepx != 0xFFFFFFFFL && in(word, fcSepx, 2)) {
                    int cb = u16(word, fcSepx);
                    if (in(word, fcSepx + 2, cb)) {
                        sprms(word, (int) fcSepx + 2, cb, new Prl() {
                            @Override
                            void op(int op, byte[] b, int a, int len) {
                                section(s, op, b, a);
                            }
                        });
                    }
                }
                out.add(s);
            }
        }
        if (out.isEmpty()) {
            out.add(new long[] {ccpText, 2, 0, 1, -1, 0, 720, 720, 12240, 15840, 1800, 1800, 1440, 1440, 0});
        }
        return out;
    }

    private static void section(long[] s, int op, byte[] b, int a) {
        switch (op) {
            case 0x3009:
                s[1] = u8(b, a);
                break;
            case 0x300A:
                s[2] = u8(b, a);
                break;
            case 0x500B:
                s[3] = Math.min(44, Math.max(1, s16(b, a) + 1));
                break;
            case 0x300C:
                s[4] = Math.max(0, s16(b, a));
                break;
            case 0x301D:
                s[5] = u8(b, a);
                break;
            case 0xB017:
                s[6] = clampTw(u16(b, a));
                break;
            case 0xB018:
                s[7] = clampTw(u16(b, a));
                break;
            case 0xB01F:
                s[8] = Math.max(144, clampTw(u16(b, a)));
                break;
            case 0xB020:
                s[9] = Math.max(144, clampTw(u16(b, a)));
                break;
            case 0xB021:
                s[10] = clampTw(u16(b, a));
                break;
            case 0xB022:
                s[11] = clampTw(u16(b, a));
                break;
            case 0x9023:
                s[12] = s16(b, a);
                break;
            case 0x9024:
                s[13] = s16(b, a);
                break;
            case 0xB025:
                s[14] = clampTw(u16(b, a));
                break;
            default:
        }
    }

    private static long clampTw(int v) {
        return Math.max(0, Math.min(31680, v));
    }

    private static String sectPr(long[] s) {
        StringBuilder x = new StringBuilder("<w:sectPr>");
        int bkc = (int) s[1];
        x.append("<w:type w:val=\"")
                .append(bkc == 0           ? "continuous"
                                : bkc == 1 ? "nextColumn"
                                : bkc == 3 ? "evenPage"
                                : bkc == 4 ? "oddPage"
                                           : "nextPage")
                .append("\"/>");
        boolean landscape = s[5] == 2 || (s[5] == 0 && s[8] > s[9]);
        x.append("<w:pgSz w:w=\"").append(s[8]).append("\" w:h=\"").append(s[9]).append('"');
        if (landscape) {
            x.append(" w:orient=\"landscape\"");
        }
        x.append("/><w:pgMar w:top=\"")
                .append(s[12])
                .append("\" w:right=\"")
                .append(s[11])
                .append("\" w:bottom=\"")
                .append(s[13])
                .append("\" w:left=\"")
                .append(s[10])
                .append("\" w:header=\"")
                .append(s[6])
                .append("\" w:footer=\"")
                .append(s[7])
                .append("\" w:gutter=\"")
                .append(s[14])
                .append("\"/>");
        if (s[4] >= 0) {
            x.append("<w:pgNumType w:start=\"").append(s[4]).append("\"/>");
        }
        if (s[3] > 1) {
            x.append("<w:cols w:num=\"").append(s[3]).append("\"/>");
        }
        if (s[2] != 0) {
            x.append("<w:titlePg/>");
        }
        return x.append("</w:sectPr>").toString();
    }

    // ---- text walk ----

    private static final class Ended {
        final Para para;
        final long end;

        Ended(Para para, long end) {
            this.para = para;
            this.end = end;
        }
    }

    /// Builds runs for one paragraph, merging equal formatting.
    private final class Runs {
        final StringBuilder out; //NOPMD AvoidStringBufferField - a single-use writer owns its output
        String rPr = "";
        boolean instr;
        final StringBuilder text = new StringBuilder(); //NOPMD AvoidStringBufferField - a single-use writer owns its output

        Runs(StringBuilder out) {
            this.out = out;
        }

        void text(int c, String props, boolean instruction) {
            if (text.length() > 0 && (!props.equals(rPr) || instruction != instr)) {
                flush();
            }
            rPr = props;
            instr = instruction;
            if (c == '&') {
                text.append("&amp;");
            } else if (c == '<') {
                text.append("&lt;");
            } else if (c == '>') {
                text.append("&gt;");
            } else if (c >= 0x10000) {
                text.append((char) (0xD800 + ((c - 0x10000) >> 10))).append((char) (0xDC00 + ((c - 0x10000) & 0x3FF)));
            } else {
                text.append((char) c);
            }
        }

        void element(String props, String inner) {
            flush();
            int cut = props.indexOf(1);
            if (cut >= 0) {
                open(props.substring(0, cut));
            }
            out.append("<w:r>").append(cut < 0 ? props : props.substring(cut + 1)).append(inner).append("</w:r>");
            if (cut >= 0) {
                out.append(props.startsWith("<w:del") ? "</w:del>" : "</w:ins>");
            }
        }

        /// Opens a revision wrapper (the props before the char 1 marker), giving it a unique id.
        private void open(String wrap) {
            out.append(wrap.substring(0, 6)).append(" w:id=\"").append(++revIds).append('"').append(wrap.substring(6));
        }

        void flush() {
            if (text.length() == 0) {
                return;
            }
            int cut = rPr.indexOf(1);
            boolean del = cut >= 0 && rPr.startsWith("<w:del");
            String tag = instr ? (del ? "w:delInstrText" : "w:instrText") : del ? "w:delText" : "w:t";
            if (cut >= 0) {
                open(rPr.substring(0, cut));
            }
            out.append("<w:r>")
                    .append(cut < 0 ? rPr : rPr.substring(cut + 1))
                    .append('<')
                    .append(tag)
                    .append(" xml:space=\"preserve\">")
                    .append(text)
                    .append("</")
                    .append(tag)
                    .append("></w:r>");
            if (cut >= 0) {
                out.append(del ? "</w:del>" : "</w:ins>");
            }
            text.setLength(0);
        }
    }

    private List<Para> walk(long a, long b, String noteKind) throws IOException {
        ArrayList<Para> out = new ArrayList<Para>();
        for (Ended e : walkEnded(a, b, noteKind)) {
            out.add(e.para);
        }
        return out;
    }

    private List<Ended> walkEnded(long a, long b, String noteKind) throws IOException {
        ArrayList<Ended> out = new ArrayList<Ended>();
        long end = Math.min(b, cps.length == 0 ? 0 : cps[cps.length - 1]);
        long cp = a;
        Para para = new Para();
        Runs runs = new Runs(para.runs);
        ArrayList<Boolean> fields = new ArrayList<Boolean>();
        boolean main = noteKind == null && a == 0;
        while (cp < end) {
            int c = charAt(cp);
            if (c < 0) {
                cp = Math.max(cp + 1, next);
                continue;
            }
            if (++budget > MAX_CHARS) { //NOPMD AssignmentInOperand - the budget bounds a hostile file
                throw new IOException("The .doc file is too large");
            }
            if (main && !marks.isEmpty()) {
                StringBuilder m = marks.get(cp);
                if (m != null) {
                    runs.flush();
                    para.runs.append(m);
                }
            }
            int step = units;
            boolean sectMark = c == 12 && main && sectEnds.contains(cp + 1);
            boolean instr = !fields.isEmpty() && fields.get(fields.size() - 1);
            if (c == 13 || c == 7 || sectMark) {
                runs.flush();
                papx(fcOf(cp));
                final PPr p = new PPr();
                int istd = papIstd;
                if (papLen > 0) {
                    sprms(word, papAt, papLen, new Prl() {
                        @Override
                        void op(int op, byte[] b, int a, int n) {
                            applyPara(p, op, b, a, n);
                        }
                    });
                }
                applyPrm(piece(cp), null, p);
                if (istd < styleIds.length && styleIds[istd] != null && styles.get(istd).stk == 1 && istd != 0) {
                    p.style = styleIds[istd];
                }
                para.borders = p.borders;
                para.depth = p.inTable ? Math.max(1, Math.min(MAX_DEPTH, p.itap)) : 0;
                if (para.depth > 1) {
                    // nested table: inner row ends carry sprmPFInnerTtp on a paragraph mark
                    if (p.innerTtp) {
                        para.rowEnd = true;
                    } else if (c == 7) {
                        para.cellEnd = true;
                    }
                } else if (p.inTable) {
                    if (p.ttp) {
                        para.rowEnd = true;
                    } else if (c == 7) {
                        para.cellEnd = true;
                    }
                }
                para.grid = p.grid;
                para.merge = p.merge;
                para.cellBorders = p.cellBorders;
                para.cellShd = p.cellShd;
                p.markRev = markRevision(cp);
                para.pPr = p.xml(lfoCount);
                out.add(new Ended(para, cp + 1));
                para = new Para();
                runs = new Runs(para.runs);
                cp += step;
                continue;
            }
            switch (c) {
                case 0x13:
                    runs.element("", "<w:fldChar w:fldCharType=\"begin\"/>");
                    fields.add(Boolean.TRUE);
                    break;
                case 0x14:
                    if (instr) {
                        fields.set(fields.size() - 1, Boolean.FALSE);
                        runs.element("", "<w:fldChar w:fldCharType=\"separate\"/>");
                    }
                    break;
                case 0x15:
                    if (!fields.isEmpty()) {
                        fields.remove(fields.size() - 1);
                        runs.element("", "<w:fldChar w:fldCharType=\"end\"/>");
                    }
                    break;
                case 0x01: {
                    int[] r = chpx(fcOf(cp));
                    final long[] loc = {-1};
                    if (r != null) {
                        sprms(word, r[0], r[1], new Prl() {
                            @Override
                            void op(int op, byte[] bb, int at, int n) {
                                if (op == 0x6A03) {
                                    loc[0] = u32(bb, at);
                                }
                            }
                        });
                    }
                    String pic = loc[0] >= 0 && !instr ? picture(loc[0]) : null;
                    if (pic != null) {
                        runs.element(rpr(cp), pic);
                    }
                    break;
                }
                case 0x05: {
                    Integer id = main ? atnRefs.get(cp) : null;
                    if (id != null) {
                        runs.element("", "<w:commentReference w:id=\"" + id + "\"/>");
                    }
                    break;
                }
                case 0x08: {
                    String f = noteKind == null ? floats.get(cp) : null;
                    if (f != null) {
                        runs.element(rpr(cp), f);
                    }
                    break;
                }
                case 0x02:
                    if (noteKind != null && !"comment".equals(noteKind)) {
                        runs.element("<w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr>", "<w:" + noteKind + "Ref/>");
                    } else if (main) {
                        Integer f = footRefs.get(cp);
                        Integer e = endRefs.get(cp);
                        if (f != null) {
                            runs.element("<w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr>",
                                    "<w:footnoteReference w:id=\"" + f + "\"/>");
                        } else if (e != null) {
                            runs.element("<w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr>",
                                    "<w:endnoteReference w:id=\"" + e + "\"/>");
                        }
                    }
                    break;
                case 0x09:
                    if (instr) {
                        runs.text(' ', rpr(cp), true);
                    } else {
                        runs.element(rpr(cp), "<w:tab/>");
                    }
                    break;
                case 0x0B:
                    runs.element(rpr(cp), "<w:br/>");
                    break;
                case 0x0C:
                    runs.element(rpr(cp), "<w:br w:type=\"page\"/>");
                    break;
                case 0x0E:
                    runs.element(rpr(cp), "<w:br w:type=\"column\"/>");
                    break;
                case 0x1E:
                    runs.element(rpr(cp), "<w:noBreakHyphen/>");
                    break;
                case 0x1F:
                    runs.element(rpr(cp), "<w:softHyphen/>");
                    break;
                default:
                    if (c >= 0x20) {
                        runs.text(c, rpr(cp), instr);
                    }
            }
            cp += step;
        }
        runs.flush();
        for (int i = 0; i < fields.size(); i++) { //NOPMD ForLoopCanBeForeach - only the count matters
            runs.element("", "<w:fldChar w:fldCharType=\"end\"/>");
        }
        if (para.runs.length() > 0) {
            out.add(new Ended(para, end));
        }
        return out;
    }

    private String blocks(List<Para> ps) {
        return content(ps, 0);
    }

    /// Paragraphs at table depth level, with deeper runs rendered as (nested) tables.
    private String content(List<Para> ps, int level) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ps.size();) {
            if (ps.get(i).depth > level) {
                int j = i;
                while (j < ps.size() && ps.get(j).depth > level) {
                    j++;
                }
                out.append(table(ps.subList(i, j), level + 1));
                i = j;
            } else {
                out.append(ps.get(i++).xml());
            }
        }
        return out.toString();
    }

    private String table(List<Para> ps, int level) {
        StringBuilder rows = new StringBuilder();
        ArrayList<String> cells = new ArrayList<String>();
        ArrayList<Para> cell = new ArrayList<Para>();
        int[] grid = null;
        boolean borders = false;
        for (Para p : ps) {
            boolean own = p.depth == level;
            if (own && p.rowEnd) {
                if (!cell.isEmpty()) {
                    cells.add(content(cell, level));
                }
                cell.clear();
                if (p.runs.length() > 0) {
                    // Bookmark and comment boundaries on the row-end mark close in the row's last cell.
                    int last = cells.size() - 1;
                    String c = last < 0 ? "" : cells.get(last);
                    int at = c.lastIndexOf("</w:p>");
                    if (at < 0 || c.endsWith("</w:tbl>")) {
                        c = c + "<w:p>" + p.runs + "</w:p>";
                    } else {
                        c = c.substring(0, at) + p.runs + c.substring(at);
                    }
                    if (last < 0) {
                        cells.add(c);
                    } else {
                        cells.set(last, c);
                    }
                }
                rows.append(row(cells, p.grid, p.merge, p.cellBorders, p.cellShd));
                if (grid == null || (p.grid != null && p.grid.length > grid.length)) {
                    grid = p.grid;
                }
                borders |= p.borders;
                cells.clear();
                continue;
            }
            cell.add(p);
            if (own && p.cellEnd) {
                cells.add(content(cell, level));
                cell.clear();
            }
        }
        if (!cell.isEmpty()) {
            cells.add(content(cell, level));
        }
        if (!cells.isEmpty()) {
            rows.append(row(cells, grid, null, null, null));
        }
        if (rows.length() == 0) {
            return "";
        }
        StringBuilder s = new StringBuilder("<w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/>");
        if (borders) {
            s.append("<w:tblBorders>");
            for (String side : new String[] {"top", "left", "bottom", "right", "insideH", "insideV"}) {
                s.append("<w:").append(side).append(" w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>");
            }
            s.append("</w:tblBorders>");
        }
        s.append("</w:tblPr><w:tblGrid>");
        if (grid != null) {
            for (int w : grid) {
                s.append("<w:gridCol w:w=\"").append(w).append("\"/>");
            }
        }
        return s.append("</w:tblGrid>").append(rows).append("</w:tbl>").toString();
    }

    /// One row. merge holds per-cell flags from the TAP: 1 first of a horizontal merge, 2 merged into
    /// the previous cell, 4 vertically merged, 8 vertical merge restart.
    ///
    private static String row(List<String> cells, int[] grid, int[] merge, String[] borders, String[] shd) {
        StringBuilder s = new StringBuilder("<w:tr>");
        if (cells.isEmpty()) {
            s.append("<w:tc><w:p/></w:tc>");
        }
        for (int i = 0; i < cells.size();) {
            int m = merge != null && i < merge.length ? merge[i] : 0;
            StringBuilder body = new StringBuilder(cells.get(i));
            int span = 1;
            int w = grid != null && i < grid.length ? grid[i] : 0;
            while ((m & 2) == 0 && merge != null && i + span < cells.size() && i + span < merge.length
                    && (merge[i + span] & 2) != 0) {
                String extra = cells.get(i + span);
                if (extra.contains("<w:t ")) {
                    body.append(extra);
                }
                if (grid != null && i + span < grid.length) {
                    w += grid[i + span];
                }
                span++;
            }
            s.append("<w:tc>");
            String bd = borders != null && i < borders.length ? borders[i] : null;
            String sh = shd != null && i < shd.length ? shd[i] : null;
            if (w > 0 || span > 1 || (m & 4) != 0 || bd != null || sh != null) {
                s.append("<w:tcPr>");
                if (w > 0) {
                    s.append("<w:tcW w:w=\"").append(w).append("\" w:type=\"dxa\"/>");
                }
                if (span > 1) {
                    s.append("<w:gridSpan w:val=\"").append(span).append("\"/>");
                }
                if ((m & 4) != 0) {
                    s.append((m & 8) != 0 ? "<w:vMerge w:val=\"restart\"/>" : "<w:vMerge/>");
                }
                if (bd != null) {
                    s.append(bd);
                }
                if (sh != null) {
                    s.append(sh);
                }
                s.append("</w:tcPr>");
            }
            if (body.length() == 0 || body.toString().endsWith("</w:tbl>")) {
                body.append("<w:p/>");
            }
            s.append(body).append("</w:tc>");
            i += span;
        }
        return s.append("</w:tr>").toString();
    }

    // ---- pictures ----

    private String picture(long fc) {
        if (!in(data, fc, 0x44) || pictures >= MAX_PARTS) {
            return null;
        }
        long lcb = u32(data, fc);
        int cbHeader = u16(data, fc + 4);
        int mm = u16(data, fc + 6);
        if (cbHeader < 0x44 || lcb < cbHeader) {
            return null;
        }
        long end = Math.min(data.length, fc + lcb);
        long at = fc + cbHeader;
        if (mm == 0x66) {
            at += 1 + u8(data, at);
        }
        int dxa = s16(data, fc + 0x1C);
        int dya = s16(data, fc + 0x1E);
        int mx = u16(data, fc + 0x20);
        int my = u16(data, fc + 0x22);
        String[] ext = new String[1];
        byte[] blip = blip(data, at, end, 0, ext);
        if (blip == null) {
            return null;
        }
        long cx = (long) (dxa > 0 ? dxa : 1440) * (mx > 0 ? mx : 1000) / 1000 * 635;
        long cy = (long) (dya > 0 ? dya : 1440) * (my > 0 ? my : 1000) / 1000 * 635;
        cx = Math.max(12700, Math.min(cx, 20116800L));
        cy = Math.max(12700, Math.min(cy, 20116800L));
        String id = media(blip, ext[0]);
        int n = pictures;
        return "<w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\"><wp:extent cx=\"" + cx
                + "\" cy=\"" + cy + "\"/><wp:docPr id=\"" + n + "\" name=\"Picture " + n + "\"/>"
                + picGraphic(id, n, cx, cy) + "</wp:inline></w:drawing>";
    }

    private String media(byte[] blip, String ext) {
        int n = ++pictures;
        String name = "media/image" + n + "." + ext;
        parts.put("word/" + name, blip);
        return rel("image", name);
    }

    private static String picGraphic(String id, int n, long cx, long cy) {
        return ""
                + "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                + "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
                + "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><pic:nvPicPr>"
                + "<pic:cNvPr id=\"" + n + "\" name=\"image" + n + "\"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill>"
                + "<a:blip r:embed=\"" + id + "\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr>"
                + "<a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
                + ("<a:prstGeom "
                   + "prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>");
    }

    /// An OfficeArt metafile BLIP body: a 34-byte header (uncompressed size, bounds, EMU size, stored
    /// size, compression) then the usually deflated metafile. WMF bodies get the placeable header they
    /// are stored without. Null when the payload is malformed or larger than 20 MiB.
    ///
    private static byte[] metafileBlip(byte[] data, long at, long stop, boolean wmf) {
        if (at + 34 > stop) {
            return null;
        }
        long size = u32(data, at);
        long saved = u32(data, at + 28);
        int compression = u8(data, at + 32);
        long from = at + 34;
        if (size <= 0 || size > 20L * 1024 * 1024 || saved > stop - from) {
            return null;
        }
        byte[] raw;
        if (compression == 0xFE) {
            raw = copy(data, from, Math.min(size, stop - from));
        } else if (compression == 0) {
            java.util.zip.Inflater inf = new java.util.zip.Inflater();
            try {
                inf.setInput(data, (int) from, (int) saved);
                byte[] out = new byte[(int) size];
                int n = 0;
                while (n < out.length && !inf.finished()) {
                    int k = inf.inflate(out, n, out.length - n);
                    if (k == 0 && (inf.needsInput() || inf.needsDictionary())) {
                        break;
                    }
                    n += k;
                }
                if (n == out.length) {
                    raw = out;
                } else {
                    raw = new byte[n];
                    System.arraycopy(out, 0, raw, 0, n);
                }
            } catch (java.util.zip.DataFormatException ex) {
                return null;
            } finally {
                inf.end();
            }
        } else {
            return null;
        }
        if (!wmf || raw.length < 2 || (raw[0] & 255) == 0xD7) {
            return raw;
        }
        int l = (int) u32(data, at + 4);
        int t = (int) u32(data, at + 8);
        int r = (int) u32(data, at + 12);
        int bt = (int) u32(data, at + 16);
        long emuW = u32(data, at + 20);
        int span = Math.abs(r - l);
        long inch = emuW > 0 && span > 0 ? (long) span * 914400 / emuW : 1440;
        if (inch <= 0 || inch > 65535) {
            inch = 1440;
        }
        byte[] out = new byte[22 + raw.length];
        int[] head = {0xD7, 0xCD, 0xC6, 0x9A, 0, 0, l, l >> 8, t, t >> 8, r, r >> 8, bt, bt >> 8, (int) inch,
                (int) (inch >> 8), 0, 0, 0, 0};
        for (int i = 0; i < head.length; i++) {
            out[i] = (byte) head[i];
        }
        int sum = 0;
        for (int i = 0; i < 20; i += 2) {
            sum ^= (out[i] & 255) | (out[i + 1] & 255) << 8;
        }
        out[20] = (byte) sum;
        out[21] = (byte) (sum >> 8);
        System.arraycopy(raw, 0, out, 22, raw.length);
        return out;
    }

    /// Finds the first PNG or JPEG BLIP in the OfficeArt records between at and end.
    private static byte[] blip(byte[] data, long at, long end, int depth, String[] ext) {
        if (depth > 8) {
            return null;
        }
        for (int guard = 0; at + 8 <= end && guard < 1000; guard++) {
            int verInst = u16(data, at);
            int type = u16(data, at + 2);
            long len = u32(data, at + 4);
            long body = at + 8;
            long stop = Math.min(end, body + len);
            int inst = verInst >> 4;
            if ((verInst & 0xF) == 0xF) {
                byte[] found = blip(data, body, stop, depth + 1, ext);
                if (found != null) {
                    return found;
                }
            } else if (type == 0xF007) {
                byte[] found = blip(data, body + 36 + u8(data, body + 33), stop, depth + 1, ext);
                if (found != null) {
                    return found;
                }
            } else if (type == 0xF01E || type == 0xF01D) {
                boolean two = type == 0xF01E ? inst == 0x6E1 : inst == 0x46B || inst == 0x6E3;
                long from = body + (two ? 32 : 16) + 1;
                if (from < stop) {
                    byte[] b = copy(data, from, stop - from);
                    boolean png = b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
                    boolean jpg = b.length > 3 && (b[0] & 0xff) == 0xFF && (b[1] & 0xff) == 0xD8;
                    if (png || jpg) {
                        ext[0] = png ? "png" : "jpeg";
                        return b;
                    }
                }
            } else if (type == 0xF01A || type == 0xF01B) {
                boolean emf = type == 0xF01A;
                boolean two = inst == (emf ? 0x3D5 : 0x217);
                byte[] b = metafileBlip(data, body + (two ? 32 : 16), stop, !emf);
                if (b != null) {
                    ext[0] = emf ? "emf" : "wmf";
                    return b;
                }
            }
            if (len > end - body) {
                break;
            }
            at = body + len;
        }
        return null;
    }

    // ---- revisions, bookmarks, comments ----

    /// The w:ins / w:del opening tag (without its id) for a run, then a char-1 marker; empty if none.
    private String revision(Chr c) {
        boolean del = Boolean.TRUE.equals(c.del);
        if (!del && !Boolean.TRUE.equals(c.ins)) {
            return "";
        }
        int by = del ? c.delBy : c.insBy;
        String who = by >= 0 && by < rmAuthors.size() ? rmAuthors.get(by) : "";
        if (who.length() == 0) {
            who = "Unknown";
        }
        StringBuilder s =
                new StringBuilder(del ? "<w:del" : "<w:ins").append(" w:author=\"").append(esc(who)).append('"');
        String date = dttm(del ? c.delAt : c.insAt);
        if (date != null) {
            s.append(" w:date=\"").append(date).append('"');
        }
        return s.append(">\u0001").toString();
    }

    /// A DTTM ([MS-DOC] 2.9.62) as an ISO date, or null when unset or invalid.
    static String dttm(long v) {
        int min = (int) (v & 63);
        int hr = (int) (v >> 6 & 31);
        int dom = (int) (v >> 11 & 31);
        int mon = (int) (v >> 16 & 15);
        int yr = (int) (v >> 20 & 511) + 1900;
        if (v == 0 || mon < 1 || mon > 12 || dom < 1 || hr > 23 || min > 59) {
            return null;
        }
        return yr + "-" + two(mon) + "-" + two(dom) + "T" + two(hr) + ":" + two(min) + ":00Z";
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    static String esc(String v) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '&') {
                s.append("&amp;");
            } else if (c == '<') {
                s.append("&lt;");
            } else if (c == '>') {
                s.append("&gt;");
            } else if (c == '"') {
                s.append("&quot;");
            } else if (c >= 0x20 && c != 0xFFFE && c != 0xFFFF) {
                s.append(c);
            }
        }
        return s.toString();
    }

    private static String xst(byte[] b, long at, int cch) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < cch; i++) {
            s.append((char) u16(b, at + i * 2L));
        }
        return s.toString();
    }

    /// A PLC at FIB pair i as {count, CP array offset, data array offset}, or null.
    private long[] plcAt(int i, int cb) {
        long[] p = pair(i);
        if (p == null || p[1] < 4 + 4 + cb || !in(table, p[0], p[1])) {
            return null;
        }
        long n = (p[1] - 4) / (4 + cb);
        return new long[] {Math.min(n, MAX_NOTES), p[0], p[0] + (n + 1) * 4};
    }

    /// The strings of an STTB at FIB pair i; extra, when given, receives each entry's extra-data offset.
    private List<String> sttb(int i, List<Long> extra) {
        ArrayList<String> out = new ArrayList<String>();
        long[] p = pair(i);
        if (p == null || p[1] < 6 || !in(table, p[0], p[1])) {
            return out;
        }
        long at = p[0];
        long end = p[0] + p[1];
        boolean wide = u16(table, at) == 0xFFFF;
        if (wide) {
            at += 2;
        }
        int count = u16(table, at);
        int cbExtra = u16(table, at + 2);
        at += 4;
        for (int k = 0; k < Math.min(count, MAX_NOTES); k++) {
            int cch = wide ? u16(table, at) : u8(table, at);
            at += wide ? 2 : 1;
            long len = wide ? cch * 2L : cch;
            if (at + len + cbExtra > end) {
                break;
            }
            StringBuilder s = new StringBuilder();
            for (int c = 0; c < cch; c++) {
                s.append((char) (wide ? u16(table, at + c * 2L) : u8(table, at + c)));
            }
            out.add(s.toString());
            at += len;
            if (extra != null) {
                extra.add(at);
            }
            at += cbExtra;
        }
        return out;
    }

    private void mark(long cp, String xml) {
        StringBuilder m = marks.get(cp);
        if (m == null) {
            m = new StringBuilder();
            marks.put(cp, m);
        }
        m.append(xml);
    }

    /// Comments: PlcfandRef (ATRDPre10 per reference), PlcfandTxt (the comment stories), authors from
    /// GrpXstAtnOwners, and the commented ranges from SttbfAtnBkmk tags with PlcfAtnBkf/PlcfAtnBkl.
    ///
    private String comments() throws IOException {
        long[] ref = plcAt(4, 30);
        List<long[]> txt = plc(5, 0);
        if (ref == null || txt.size() < 2) {
            return null;
        }
        ArrayList<String> owners = new ArrayList<String>();
        long[] g = pair(36);
        if (g != null && in(table, g[0], g[1])) {
            long at = g[0];
            long end = g[0] + g[1];
            while (at + 2 <= end && owners.size() < MAX_NOTES) {
                int cch = u16(table, at);
                if (at + 2 + cch * 2L > end) {
                    break;
                }
                owners.add(xst(table, at + 2, cch));
                at += 2 + cch * 2L;
            }
        }
        ArrayList<Long> extra = new ArrayList<Long>();
        sttb(37, extra);
        long[] bf = plcAt(42, 4);
        long[] bl = plcAt(43, 0);
        HashMap<Long, Integer> byTag = new HashMap<Long, Integer>();
        for (int k = 0; k < extra.size(); k++) {
            long tag = u32(table, extra.get(k) + 2);
            if (!byTag.containsKey(tag)) {
                byTag.put(tag, k);
            }
        }
        StringBuilder s = new StringBuilder(xmlDecl() + "<w:comments" + NS + ">");
        long base = ccpText + ccpFtn + ccpHdd;
        int count = 0;
        for (int i = 0; i < ref[0] && i + 1 < txt.size(); i++) {
            long cp = u32(table, ref[1] + i * 4L);
            long atrd = ref[2] + i * 30L;
            if (cp >= ccpText || atnRefs.containsKey(cp)) {
                continue;
            }
            long a = Math.min(txt.get(i)[0], ccpAtn);
            long b = Math.min(txt.get(i + 1)[0], ccpAtn);
            String body = b > a ? blocks(walk(base + a, base + b, "comment")) : "";
            if (body.length() == 0) {
                body = "<w:p/>";
            }
            int id = count++;
            atnRefs.put(cp, id);
            int ibst = u16(table, atrd + 20);
            String author = ibst < owners.size() ? owners.get(ibst) : "";
            String initials = xst(table, atrd + 2, Math.min(9, u16(table, atrd)));
            Integer k = byTag.get(u32(table, atrd + 26));
            if (u32(table, atrd + 26) != 0xFFFFFFFFL && k != null && bf != null && bl != null && k < bf[0]) {
                long st = u32(table, bf[1] + k * 4L);
                int ibkl = u16(table, bf[2] + k * 4L);
                if (ibkl < bl[0]) {
                    long en = Math.min(u32(table, bl[1] + ibkl * 4L), ccpText - 1);
                    if (st <= en) {
                        mark(st, "<w:commentRangeStart w:id=\"" + id + "\"/>");
                        mark(en, "<w:commentRangeEnd w:id=\"" + id + "\"/>");
                    }
                }
            }
            s.append("<w:comment w:id=\"")
                    .append(id)
                    .append("\" w:author=\"")
                    .append(esc(author))
                    .append("\" w:initials=\"")
                    .append(esc(initials))
                    .append("\">")
                    .append(body)
                    .append("</w:comment>");
        }
        return count == 0 ? null : s.append("</w:comments>").toString();
    }

    // ---- floating pictures ----

    /// Floating pictures of the main document: PlcSpaMom anchors (FSPA) joined by spid with the shapes
    /// of the main OfficeArt drawing (DggInfo). Other floating shapes are not read.
    private void floats() {
        long[] p = pair(50);
        if (p == null || p[1] < 8 || !in(table, p[0], p[1])) {
            return;
        }
        long end = p[0] + p[1];
        long at = p[0];
        if (u16(table, at + 2) != 0xF000) {
            return;
        }
        long len = u32(table, at + 4);
        if (len > end - at - 8) {
            return;
        }
        art(at + 8, at + 8 + len, 0, null);
        at += 8 + len;
        if (at + 9 <= end) {
            long dl = u32(table, at + 5);
            if (u16(table, at + 3) == 0xF002 && dl <= end - at - 9) {
                art(at + 9, at + 9 + dl, 0, null);
            }
        }
        long[] spa = plcAt(40, 26);
        if (spa == null) {
            return;
        }
        for (int k = 0; k < spa[0]; k++) {
            long cp = u32(table, spa[1] + k * 4L);
            long d = spa[2] + k * 26L;
            long[] sh = shapes.get(u32(table, d));
            if (cp >= ccpText || sh == null || floats.containsKey(cp) || floatIds >= MAX_PARTS) {
                continue;
            }
            String x = anchor(sh, (int) u32(table, d + 4), (int) u32(table, d + 8), (int) u32(table, d + 12),
                    (int) u32(table, d + 16));
            if (x != null) {
                floats.put(cp, x);
            }
        }
    }

    private String blipMedia(long pib) {
        if (pib <= 0 || pib > blips.size()) {
            return null;
        }
        long[] e = blips.get((int) pib - 1);
        long body = e[0];
        long head = 36 + u8(table, body + 33);
        String[] ext = new String[1];
        byte[] img;
        if (e[1] > head) {
            img = blip(table, body + head, body + e[1], 0, ext);
        } else {
            long fo = u32(table, body + 28);
            img = in(word, fo, 8) ? blip(word, fo, Math.min(word.length, fo + 8 + u32(word, fo + 4)), 0, ext) : null;
        }
        if (img == null || pictures >= MAX_PARTS) {
            return null;
        }
        return media(img, ext[0]);
    }

    /// Collects BLIP store entries and each shape's id and picture index from OfficeArt records.
    private void art(long at, long end, int depth, long[] shape) {
        if (depth > 8) {
            return;
        }
        for (int guard = 0; at + 8 <= end && guard < 100000; guard++) {
            int verInst = u16(table, at);
            int type = u16(table, at + 2);
            long len = u32(table, at + 4);
            long body = at + 8;
            if (len > end - body) {
                return;
            }
            if (type == 0xF007) {
                if (blips.size() < MAX_PARTS) {
                    blips.add(new long[] {body, len});
                }
            } else if (type == 0xF004) {
                long[] sh = {-1, 0, 0};
                art(body, body + len, depth + 1, sh);
                if (sh[0] >= 0 && shapes.size() < MAX_PARTS * 4 && !shapes.containsKey(sh[0])) {
                    shapes.put(sh[0], sh);
                }
            } else if ((verInst & 0xF) == 0xF) {
                art(body, body + len, depth + 1, shape);
            } else if (type == 0xF00A && shape != null && len >= 8) {
                shape[0] = u32(table, body);
                shape[1] = verInst >> 4;
            } else if (type == 0xF00B && shape != null) {
                int n = verInst >> 4;
                for (int k = 0; k < n && k * 6L + 6 <= len; k++) {
                    if ((u16(table, body + k * 6L) & 0x3FFF) == 0x104) {
                        shape[2] = u32(table, body + k * 6L + 2);
                    }
                }
            }
            at = body + len;
        }
    }

    private static long emu(long twips) {
        return Math.max(-51206400L, Math.min(51206400L, twips * 635));
    }

    /// A `wp:anchor` drawing for one FSPA, or null when the shape is not a readable picture.
    private String anchor(long[] sh, int xl, int yt, int xr, int yb) {
        if (sh[2] <= 0 || sh[2] > blips.size()) {
            return null;
        }
        long cx = Math.max(12700, Math.min(20116800L, emu((long) xr - xl)));
        long cy = Math.max(12700, Math.min(20116800L, emu((long) yb - yt)));
        String id = blipMedia(sh[2]);
        if (id == null) {
            return null;
        }
        int n = ++floatIds;
        return "<w:drawing><wp:anchor distT=\"0\" distB=\"0\" distL=\"114300\" distR=\"114300\" simplePos=\"0\" "
               + "relativeHeight=\""
                + n
                + ("\" behindDoc=\"0\" locked=\"0\" layoutInCell=\"1\" allowOverlap=\"1\"><wp:simplePos x=\"0\" "
                   + "y=\"0\"/>")
                + "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/><wp:wrapSquare wrapText=\"bothSides\"/>"
                + "<wp:docPr id=\"" + (100000 + n) + "\" name=\"Shape " + n + "\"/>"
                + picGraphic(id, pictures, cx, cy) + "</wp:anchor></w:drawing>";
    }
}
