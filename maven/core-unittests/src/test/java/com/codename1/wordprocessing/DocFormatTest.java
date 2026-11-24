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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Word 97-2003 `.doc` reading, over files synthesized with a minimal Word binary writer.
class DocFormatTest extends UITestBase {
    static final byte[] PNG = bytes(
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 0x49, 0x48, 0x44, 0x52, 0, 0, 0, 1, 0, 0, 0, 1,
            8, 6, 0, 0, 0, 0x1F, 0x15, 0xC4, 0x89, 0, 0, 0, 0x0D, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x63, 0, 1, 0, 0,
            5, 0, 1, 0x0D, 0x0A, 0x2D, 0xB4, 0, 0, 0, 0, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82);

    private static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) b[i] = (byte) v[i];
        return b;
    }

    /// A minimal Word 97 writer: one or two pieces, CHPX/PAPX FKPs and table-stream structures.
    static final class DocWriter {
        final StringBuilder text = new StringBuilder();
        final List<byte[]> chpx = new ArrayList<>();
        final TreeMap<Integer, byte[]> papx = new TreeMap<>();
        final Map<Integer, byte[]> tables = new TreeMap<>();
        final List<byte[]> sepx = new ArrayList<>();
        int ccpText, ccpFtn, ccpHdd, ccpAtn, ccpEdn, compressed, flags = 0x0200;
        byte[] data;

        DocWriter run(String s, int... grpprl) {
            for (int i = 0; i < s.length(); i++) {
                text.append(s.charAt(i));
                chpx.add(bytes(grpprl));
            }
            return this;
        }

        DocWriter para(int istd, int... grpprl) {
            return mark('\r', istd, grpprl);
        }

        DocWriter mark(char c, int istd, int... grpprl) {
            papx.put(text.length(), cat(le16(istd), bytes(grpprl)));
            text.append(c);
            chpx.add(new byte[0]);
            return this;
        }

        int cp() {
            return text.length();
        }

        int fc(int cp) {
            if (cp < compressed) return 1024 + cp;
            return utf16Start() + (cp - compressed) * 2;
        }

        int utf16Start() {
            return compressed == 0 ? 1024 : (1024 + compressed + 1) & ~1;
        }

        int width(int cp) {
            return cp < compressed ? 1 : 2;
        }

        byte[] build() throws IOException {
            ByteArrayOutputStream w = new ByteArrayOutputStream();
            w.write(new byte[1024]);
            for (int i = 0; i < compressed; i++) w.write(text.charAt(i) == '\u20AC' ? 0x80 : text.charAt(i));
            while (w.size() < utf16Start()) w.write(0);
            for (int i = compressed; i < text.length(); i++) w.write(le16(text.charAt(i)));
            ArrayList<Integer> sepxAt = new ArrayList<Integer>();
            for (byte[] s : sepx) {
                while (w.size() % 2 != 0) w.write(0);
                sepxAt.add(w.size());
                w.write(le16(s.length));
                w.write(s);
            }
            while (w.size() % 512 != 0) w.write(0);
            // CHPX FKPs: runs of equal grpprl, 20 per page
            ByteArrayOutputStream bteC = new ByteArrayOutputStream();
            ArrayList<Integer> pnC = new ArrayList<Integer>();
            ArrayList<int[]> runs = new ArrayList<int[]>();
            for (int i = 0; i < text.length(); ) {
                int j = i;
                while (j + 1 < text.length() && Arrays.equals(chpx.get(j + 1), chpx.get(i))) j++;
                runs.add(new int[] {i, j});
                i = j + 1;
            }
            for (int p = 0; p < runs.size(); p += 20) {
                byte[] page = new byte[512];
                List<int[]> chunk = runs.subList(p, Math.min(runs.size(), p + 20));
                int crun = chunk.size(), pos = 511;
                for (int k = 0; k < crun; k++) {
                    put32(page, k * 4, fc(chunk.get(k)[0]));
                    byte[] g = chpx.get(chunk.get(k)[0]);
                    if (g.length > 0) {
                        pos = (pos - 1 - g.length) & ~1;
                        page[pos] = (byte) g.length;
                        System.arraycopy(g, 0, page, pos + 1, g.length);
                        page[(crun + 1) * 4 + k] = (byte) (pos / 2);
                    }
                }
                int[] last = chunk.get(crun - 1);
                put32(page, crun * 4, fc(last[1]) + width(last[1]));
                page[511] = (byte) crun;
                bteC.write(le32(fc(chunk.get(0)[0])));
                pnC.add(w.size() / 512);
                w.write(page);
            }
            int textEnd = fc(text.length() - 1) + width(text.length() - 1);
            bteC.write(le32(textEnd));
            for (int pn : pnC) bteC.write(le32(pn));
            // PAPX FKPs, 8 paragraphs per page
            ByteArrayOutputStream bteP = new ByteArrayOutputStream();
            ArrayList<Integer> pnP = new ArrayList<Integer>();
            List<Integer> marks = new ArrayList<Integer>(papx.keySet());
            for (int p = 0; p < marks.size(); p += 8) {
                byte[] page = new byte[512];
                int crun = Math.min(8, marks.size() - p), pos = 511;
                for (int k = 0; k < crun; k++) {
                    int mark = marks.get(p + k), start = p + k == 0 ? 0 : marks.get(p + k - 1) + 1;
                    put32(page, k * 4, fc(start));
                    byte[] d = papx.get(mark);
                    int size = d.length % 2 == 1 ? 1 + d.length : 2 + d.length;
                    pos = (pos - size) & ~1;
                    if (d.length % 2 == 1) page[pos] = (byte) ((d.length + 1) / 2);
                    else {
                        page[pos] = 0;
                        page[pos + 1] = (byte) (d.length / 2);
                    }
                    System.arraycopy(d, 0, page, pos + size - d.length, d.length);
                    page[(crun + 1) * 4 + k * 13] = (byte) (pos / 2);
                }
                int lastMark = marks.get(p + crun - 1);
                put32(page, crun * 4, fc(lastMark) + width(lastMark));
                page[511] = (byte) crun;
                bteP.write(le32(fc(p == 0 ? 0 : marks.get(p - 1) + 1)));
                pnP.add(w.size() / 512);
                w.write(page);
            }
            bteP.write(le32(textEnd));
            for (int pn : pnP) bteP.write(le32(pn));
            // Table stream
            ByteArrayOutputStream clx = new ByteArrayOutputStream();
            clx.write(2);
            int pieces = compressed > 0 ? 2 : 1;
            clx.write(le32(4 + pieces * 12));
            if (pieces == 2) {
                clx.write(le32(0));
                clx.write(le32(compressed));
            } else clx.write(le32(0));
            clx.write(le32(text.length()));
            if (pieces == 2) {
                clx.write(le16(0));
                clx.write(le32((1024 * 2) | 0x40000000));
                clx.write(le16(0));
            }
            clx.write(le16(0));
            clx.write(le32(utf16Start()));
            clx.write(le16(0));
            tables.put(33, clx.toByteArray());
            tables.put(12, bteC.toByteArray());
            tables.put(13, bteP.toByteArray());
            if (!sepx.isEmpty()) {
                // PlcfSed: the CPs were placed by the caller in tables[6]; fill in the SEDs
                byte[] cps = tables.get(6);
                ByteArrayOutputStream sed = new ByteArrayOutputStream();
                sed.write(cps);
                for (int at : sepxAt) {
                    sed.write(le16(0));
                    sed.write(le32(at));
                    sed.write(le16(0));
                    sed.write(le32(-1));
                }
                tables.put(6, sed.toByteArray());
            }
            ByteArrayOutputStream t = new ByteArrayOutputStream();
            TreeMap<Integer, int[]> where = new TreeMap<Integer, int[]>();
            for (Map.Entry<Integer, byte[]> e : tables.entrySet()) {
                while (t.size() % 2 != 0) t.write(0);
                where.put(e.getKey(), new int[] {t.size(), e.getValue().length});
                t.write(e.getValue());
            }
            byte[] wd = w.toByteArray();
            put16(wd, 0, 0xA5EC);
            put16(wd, 2, 0x00C1);
            put16(wd, 0x0A, flags);
            put16(wd, 0x20, 14);
            put16(wd, 0x3E, 22);
            put32(wd, 0x40, wd.length);
            put32(wd, 0x40 + 3 * 4, ccpText);
            put32(wd, 0x40 + 4 * 4, ccpFtn);
            put32(wd, 0x40 + 5 * 4, ccpHdd);
            put32(wd, 0x40 + 7 * 4, ccpAtn);
            put32(wd, 0x40 + 8 * 4, ccpEdn);
            put16(wd, 0x98, 0x5D);
            for (Map.Entry<Integer, int[]> e : where.entrySet()) {
                put32(wd, 0x9A + e.getKey() * 8, e.getValue()[0]);
                put32(wd, 0x9A + e.getKey() * 8 + 4, e.getValue()[1]);
            }
            LinkedHashMap<String, byte[]> streams = new LinkedHashMap<String, byte[]>();
            streams.put("WordDocument", wd);
            streams.put("1Table", t.toByteArray());
            if (data != null) streams.put("Data", data);
            return CompoundFileWriter.write(streams);
        }
    }

    static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.write(p, 0, p.length);
        return out.toByteArray();
    }

    static byte[] le16(int v) {
        return bytes(v & 0xff, (v >> 8) & 0xff);
    }

    static byte[] le32(int v) {
        return bytes(v & 0xff, (v >> 8) & 0xff, (v >> 16) & 0xff, (v >>> 24) & 0xff);
    }

    static void put16(byte[] b, int at, int v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
    }

    static void put32(byte[] b, int at, int v) {
        put16(b, at, v);
        put16(b, at + 2, v >>> 16);
    }

    static byte[] utf16(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) out.write(le16(s.charAt(i)), 0, 2);
        return out.toByteArray();
    }

    static byte[] plc(int... cps) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int c : cps) out.write(le32(c), 0, 4);
        return out.toByteArray();
    }

    static byte[] fonts(String... names) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(le16(names.length), 0, 2);
        out.write(le16(0), 0, 2);
        for (String n : names) {
            byte[] ffn = cat(new byte[39], utf16(n), new byte[2]);
            out.write(ffn.length);
            out.write(ffn, 0, ffn.length);
        }
        return out.toByteArray();
    }

    static byte[] style(int stk, int base, String name, byte[] papx, byte[] chpx) {
        ByteArrayOutputStream std = new ByteArrayOutputStream();
        byte[] stdf = new byte[10];
        put16(stdf, 2, stk | base << 4);
        put16(stdf, 4, stk == 1 ? 2 : 1);
        std.write(stdf, 0, 10);
        std.write(le16(name.length()), 0, 2);
        byte[] n = cat(utf16(name), new byte[2]);
        std.write(n, 0, n.length);
        List<byte[]> upx = stk == 1 ? Arrays.asList(cat(le16(0), papx), chpx) : Arrays.asList(chpx);
        for (byte[] u : upx) {
            std.write(le16(u.length), 0, 2);
            std.write(u, 0, u.length);
            if (u.length % 2 == 1) std.write(0);
        }
        return cat(le16(std.size()), std.toByteArray());
    }

    static byte[] lvl(int nfc, int[] xst, byte[] papx) {
        byte[] l = new byte[28];
        put32(l, 0, 1);
        l[4] = (byte) nfc;
        if (nfc != 23) l[6] = 1;
        l[25] = (byte) papx.length;
        ByteArrayOutputStream x = new ByteArrayOutputStream();
        x.write(le16(xst.length), 0, 2);
        for (int c : xst) x.write(le16(c), 0, 2);
        return cat(l, papx, x.toByteArray());
    }

    static byte[] picture() {
        byte[] blip = cat(le16(0x6E0 << 4), le16(0xF01E), le32(17 + PNG.length), new byte[17], PNG);
        byte[] fbseBody = new byte[36];
        fbseBody[0] = 6;
        fbseBody[1] = 6;
        byte[] fbse = cat(le16(2 | 6 << 4), le16(0xF007), le32(36 + blip.length), fbseBody, blip);
        byte[] sp = cat(le16(0xF), le16(0xF004), le32(0));
        byte[] picf = new byte[0x44];
        put32(picf, 0, 0x44 + sp.length + fbse.length);
        put16(picf, 4, 0x44);
        put16(picf, 6, 0x64);
        put16(picf, 0x1C, 1440);
        put16(picf, 0x1E, 720);
        put16(picf, 0x20, 1000);
        put16(picf, 0x22, 1000);
        return cat(picf, sp, fbse);
    }

    static DocWriter rich() {
        DocWriter d = new DocWriter();
        d.run("Hello ").run("bold", 0x35, 0x08, 1).run(" ").run("italic", 0x36, 0x08, 1).run(" ")
                .run("big", 0x43, 0x4A, 48, 0).run(" ").run("arial", 0x4F, 0x4A, 1, 0)
                .para(0, 0x03, 0x24, 1, 0x0F, 0x84, 0xD0, 0x02);
        d.run("Styled heading").para(1);
        d.run("First bullet").para(0, 0x0B, 0x46, 1, 0, 0x0A, 0x26, 0);
        d.run("First number").para(0, 0x0B, 0x46, 2, 0, 0x0A, 0x26, 0);
        int[] tdef = {0x08, 0xD6, 1 + 1 + 6 + 40, 0, 2, 0, 0, 0x40, 0x06, 0x80, 0x0C};
        int[] tdefFull = Arrays.copyOf(tdef, tdef.length + 40);
        for (String[] row : new String[][] {{"A1", "B1"}, {"A2", "B2"}}) {
            for (String c : row) d.run(c).mark('\u0007', 0, 0x16, 0x24, 1);
            int[] end = new int[6 + tdefFull.length];
            System.arraycopy(new int[] {0x16, 0x24, 1, 0x17, 0x24, 1}, 0, end, 0, 6);
            System.arraycopy(tdefFull, 0, end, 6, tdefFull.length);
            d.mark('\u0007', 0, end);
        }
        d.run("Note here");
        int ref = d.cp();
        d.run("\u0002").para(0);
        d.run("Before break").mark('\u000c', 0);
        int sect0 = d.cp();
        d.run("Picture: ").run("\u0001", 0x03, 0x6A, 0, 0, 0, 0, 0x55, 0x08, 1).para(0);
        d.run("Last").para(0);
        d.ccpText = d.cp();
        d.run("\u0002", 0x48, 0x2A, 1).run(" Footnote text").para(0);
        int noteLen = d.cp() - d.ccpText;
        d.para(0);
        d.ccpFtn = d.cp() - d.ccpText;
        int hddStart = d.cp();
        d.run("Header text").para(0);
        int hlen = d.cp() - hddStart;
        d.para(0);
        d.ccpHdd = d.cp() - hddStart;
        int[] hdd = new int[20];
        for (int i = 8; i < 19; i++) hdd[i] = hlen;
        hdd[19] = hlen + 1;
        d.tables.put(11, plc(hdd));
        d.tables.put(2, cat(plc(ref, d.ccpText), le16(1)));
        d.tables.put(3, plc(0, noteLen, noteLen + 1));
        d.tables.put(6, plc(0, sect0, d.ccpText));
        d.sepx.add(bytes(0x09, 0x30, 2));
        d.sepx.add(bytes(0x21, 0xB0, 0xA0, 0x05));
        d.tables.put(15, fonts("Times New Roman", "Arial"));
        byte[] stshi = new byte[18];
        put16(stshi, 0, 2);
        put16(stshi, 2, 10);
        d.tables.put(1, cat(le16(18), stshi, style(1, 0xFFF, "Normal", new byte[0], new byte[0]),
                style(1, 0, "My Heading", bytes(0x03, 0x24, 1), bytes(0x35, 0x08, 1, 0x43, 0x4A, 32, 0))));
        byte[] ind = bytes(0x0F, 0x84, 0xD0, 0x02, 0x11, 0x84, 0x98, 0xFE);
        byte[] lstf = new byte[56];
        put32(lstf, 0, 100);
        lstf[26] = 1;
        put32(lstf, 28, 200);
        lstf[54] = 1;
        d.tables.put(73, cat(le16(2), lstf, lvl(23, new int[] {0xF0B7}, ind), lvl(0, new int[] {0, '.'}, ind)));
        byte[] lfo = new byte[32];
        put32(lfo, 0, 100);
        put32(lfo, 16, 200);
        d.tables.put(74, cat(le32(2), lfo, new byte[8]));
        d.data = picture();
        return d;
    }

    static byte[] sttb(int cbExtra, byte[] extra, String... names) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(le16(0xFFFF), 0, 2);
        out.write(le16(names.length), 0, 2);
        out.write(le16(cbExtra), 0, 2);
        for (String n : names) {
            out.write(le16(n.length()), 0, 2);
            byte[] u = utf16(n);
                        out.write(u, 0, u.length);
            if (cbExtra > 0) out.write(extra, 0, extra.length);
        }
        return out.toByteArray();
    }

    static byte[] rec(int verInst, int type, byte[]... body) {
        byte[] b = cat(body);
        return cat(le16(verInst), le16(type), le32(b.length), b);
    }

    /// Comment over "Alpha ins", a bookmark, and inserted/deleted runs by two authors.
    static DocWriter revised() {
        DocWriter d = new DocWriter();
        int dttm = 5 | 14 << 6 | 20 << 11 | 10 << 16 | 126 << 20;
        d.run("Alpha ").run("ins", 0x01, 0x08, 1, 0x04, 0x48, 1, 0, 0x05, 0x68, dttm & 255, dttm >> 8 & 255,
                dttm >> 16 & 255, dttm >>> 24).run("del", 0x00, 0x08, 1, 0x63, 0x48, 0, 0);
        int ref = d.cp();
        d.run("\u0005").para(0);
        d.run("Beta").para(0);
        d.ccpText = d.cp();
        d.run("\u0005Remark").para(0);
        int len = d.cp() - d.ccpText;
        d.para(0);
        d.ccpAtn = d.cp() - d.ccpText;
        byte[] atrd = new byte[30];
        put16(atrd, 0, 2);
        put16(atrd, 2, 'A');
        put16(atrd, 4, 'N');
        put32(atrd, 26, 7);
        d.tables.put(4, cat(plc(ref, d.ccpText), atrd));
        d.tables.put(5, plc(0, len, len + 1));
        d.tables.put(36, cat(le16(3), utf16("Ann")));
        d.tables.put(37, sttb(10, cat(le16(0x100), le32(7), le32(0)), ""));
        d.tables.put(42, cat(plc(0, d.ccpText), le32(0)));
        d.tables.put(43, plc(ref, d.ccpText));
        d.tables.put(21, sttb(0, null, "Mark"));
        d.tables.put(22, cat(plc(0, d.ccpText), le32(0)));
        d.tables.put(23, plc(5, d.ccpText));
        d.tables.put(51, sttb(0, null, "Bob", "Carol"));
        return d;
    }

    static int[] tdef(int f0, int f1) {
        int[] t = new int[11 + 40];
        System.arraycopy(new int[] {0x08, 0xD6, 48, 0, 2, 0, 0, 0x40, 0x06, 0x80, 0x0C}, 0, t, 0, 11);
        t[11] = f0;
        t[31] = f1;
        return t;
    }

    static int[] join(int[]... parts) {
        int n = 0;
        for (int[] p : parts) n += p.length;
        int[] out = new int[n];
        n = 0;
        for (int[] p : parts) {
            System.arraycopy(p, 0, out, n, p.length);
            n += p.length;
        }
        return out;
    }

    static DocWriter tables() {
        DocWriter d = new DocWriter();
        int[] cell = {0x16, 0x24, 1}, inner = {0x16, 0x24, 1, 0x49, 0x66, 2, 0, 0, 0}, row = {0x16, 0x24, 1, 0x17, 0x24, 1};
        d.run("A").mark('\u0007', 0, cell).run("B").mark('\u0007', 0, cell).mark('\u0007', 0, join(row, tdef(0x60, 0)));
        d.mark('\u0007', 0, cell);
        d.run("x").mark('\u0007', 0, inner).run("y").mark('\u0007', 0, inner);
        d.mark('\r', 0, join(inner, new int[] {0x4C, 0x24, 1}, tdef(0, 0)));
        d.mark('\u0007', 0, cell).mark('\u0007', 0, join(row, tdef(0x20, 0)));
        d.run("M").mark('\u0007', 0, cell).run("N").mark('\u0007', 0, cell).mark('\u0007', 0, join(row, tdef(1, 2)));
        d.run("After").para(0);
        d.ccpText = d.cp();
        return d;
    }

    // ------------------------------------------------------------------ tests

    static Paragraph paragraph(Document d, String text) {
        for (Block b : d.blocks) {
            if (b instanceof Paragraph && ((Paragraph) b).plainText().equals(text)) {
                return (Paragraph) b;
            }
        }
        throw new AssertionError("no paragraph \"" + text + "\" in " + d.plainText());
    }

    static TextStyle styleAt(RichText t, int offset) {
        int at = 0;
        for (int i = 0; i < t.runCount(); i++) {
            at += t.runText(i).length();
            if (offset < at) {
                return t.runStyle(i);
            }
        }
        throw new AssertionError("offset " + offset + " past " + t.plainText());
    }

    static Table firstTable(Document d) {
        for (Block b : d.blocks) {
            if (b instanceof Table) {
                return (Table) b;
            }
        }
        throw new AssertionError("no table");
    }

    static byte[] corruptable(byte[] file, Map<String, byte[]> streams, Random random, int i) {
        Map<String, byte[]> copy = new LinkedHashMap<String, byte[]>();
        for (Map.Entry<String, byte[]> e : streams.entrySet()) {
            copy.put(e.getKey(), e.getValue().clone());
        }
        byte[] target = copy.get(i % 3 == 0 ? "WordDocument" : i % 3 == 1 ? "1Table" : "Data");
        if (target == null || target.length == 0) {
            return file;
        }
        for (int k = 0; k < 1 + random.nextInt(12); k++) {
            target[random.nextInt(target.length)] = (byte) random.nextInt(256);
        }
        return CompoundFileWriter.write(copy);
    }

    @Test
    void textAndRunStylesRead() throws Exception {
        Document d = DocReader.read(rich().build());
        Paragraph first = paragraph(d, "Hello bold italic big arial");
        assertFalse(styleAt(first.text, 1).bold);
        assertTrue(styleAt(first.text, 7).bold);
        assertTrue(styleAt(first.text, 12).italic);
        assertEquals(24, styleAt(first.text, 19).size, 0.01);
        assertNotNull(styleAt(first.text, 23).font);
        assertEquals(Paragraph.CENTER, first.align);
        assertTrue(d.plainText().contains("Styled heading"));
    }

    @Test
    void colorsAndEmphasisRead() throws Exception {
        DocWriter w = new DocWriter();
        w.run("plain ").run("red", 0x70, 0x68, 0xFF, 0, 0, 0).run(" ").run("under", 0x3E, 0x2A, 1).run(" ")
                .run("struck", 0x37, 0x08, 1).para(0);
        w.ccpText = w.cp();
        Document d = DocReader.read(w.build());
        RichText t = paragraph(d, "plain red under struck").text;
        assertEquals(0xFF0000, styleAt(t, 7).color & 0xFFFFFF);
        assertTrue(styleAt(t, 12).underline);
        assertTrue(styleAt(t, 19).strike);
        assertFalse(styleAt(t, 1).underline);
    }

    @Test
    void stylesheetStyleAppliesToParagraph() throws Exception {
        Document d = DocReader.read(rich().build());
        Paragraph p = paragraph(d, "Styled heading");
        assertNotNull(p.style);
        assertTrue(p.style.indexOf("MyHeading") >= 0 || p.style.indexOf("My Heading") >= 0, p.style);
    }

    @Test
    void compressedAndUnicodePiecesDecode() throws Exception {
        DocWriter w = new DocWriter();
        w.run("Caf\u00E9 \u20AC ").run("\u4E2D\u6587 \uD83D\uDE00").para(0);
        w.compressed = 7;
        w.ccpText = w.cp();
        Document d = DocReader.read(w.build());
        assertTrue(d.plainText().contains("Caf\u00E9 \u20AC \u4E2D\u6587 \uD83D\uDE00"), d.plainText());
    }

    @Test
    void bulletAndNumberedListsRead() throws Exception {
        Document d = DocReader.read(rich().build());
        assertEquals(Paragraph.BULLET, paragraph(d, "First bullet").list);
        assertEquals(Paragraph.NUMBERED, paragraph(d, "First number").list);
        assertEquals(Paragraph.NO_LIST, paragraph(d, "Last").list);
    }

    @Test
    void tableCellsRead() throws Exception {
        Table t = firstTable(DocReader.read(rich().build()));
        assertEquals(2, t.rows());
        assertEquals(2, t.columns());
        assertEquals("A1", t.cellText(0, 0));
        assertEquals("B1", t.cellText(0, 1));
        assertEquals("A2", t.cellText(1, 0));
        assertEquals("B2", t.cellText(1, 1));
    }

    @Test
    void mergedAndNestedTablesRead() throws Exception {
        Document d = DocReader.read(tables().build());
        Table t = firstTable(d);
        assertFalse(t.merges.isEmpty());
        assertTrue(d.plainText().contains("After"), d.plainText());
        boolean spans = false;
        for (Table.Merge m : t.merges) {
            spans |= m.columns == 2 || m.rows > 1;
        }
        assertTrue(spans);
    }

    @Test
    void footnoteBecomesAnInlineNote() throws Exception {
        Document d = DocReader.read(rich().build());
        boolean found = false;
        for (Block b : d.blocks) {
            if (b instanceof Paragraph && ((Paragraph) b).text.hasObjects()) {
                RichText t = ((Paragraph) b).text;
                for (int i = 0; i < t.length(); i++) {
                    found |= t.objectAt(i) instanceof Note;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void pictureIsKept() throws Exception {
        Document d = DocReader.read(rich().build());
        Paragraph p = paragraph(d, "Picture: " + RichText.OBJECT);
        assertTrue(p.text.hasObjects());
    }

    @Test
    void pageSizeAndMarginsRead() throws Exception {
        Document d = DocReader.read(rich().build());
        assertNotNull(d.page);
        assertTrue(d.page.width > 0 && d.page.height > 0);
    }

    @Test
    void commentsAndRevisionsRead() throws Exception {
        Document d = DocReader.read(revised().build());
        assertEquals(1, d.comments.size());
        assertEquals("Ann", d.comments.get(0).author);
        assertTrue(d.comments.get(0).text.contains("Remark"), d.comments.get(0).text);
        Paragraph p = (Paragraph) d.blocks.get(0);
        boolean insert = false;
        for (int i = 0; i < p.text.runCount(); i++) {
            Revision r = p.text.runStyle(i).revision;
            if (r != null) {
                insert |= Revision.INSERT.equals(r.kind) && "Carol".equals(r.author);
            }
        }
        assertTrue(insert);
    }

    @Test
    void encryptedDocumentsAreRejected() throws Exception {
        DocWriter w = new DocWriter();
        w.run("secret").para(0);
        w.ccpText = w.cp();
        w.flags = 0x0300;
        try {
            DocReader.read(w.build());
            fail();
        } catch (IOException expected) {
            assertEquals("encrypted .doc files are not supported", expected.getMessage());
        }
    }

    @Test
    void badSignatureAndTruncationAreRejected() throws Exception {
        byte[] good = rich().build();
        assertThrows(IOException.class, () -> DocReader.read(null));
        assertThrows(IOException.class, () -> DocReader.read(new byte[0]));
        assertThrows(IOException.class, () -> DocReader.read(new byte[2048]));
        byte[] bad = good.clone();
        bad[0] ^= 0x55;
        assertThrows(IOException.class, () -> DocReader.read(bad));
        for (int cut : new int[] {600, 1024, good.length / 2, good.length - 100}) {
            byte[] shortened = Arrays.copyOf(good, cut);
            try {
                DocReader.read(shortened);
            } catch (IOException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    void sectorCycleIsRejected() throws Exception {
        byte[] file = rich().build();
        int dirStart = (file[48] & 0xff) | (file[49] & 0xff) << 8 | (file[50] & 0xff) << 16;
        int fatSector = (file[76] & 0xff) | (file[77] & 0xff) << 8;
        int at = 512 + fatSector * 512 + dirStart * 4;
        // The directory chain now points at itself.
        put32(file, at, dirStart);
        try {
            DocReader.read(file);
            fail();
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    void corruptedFilesOnlyRaiseIOException() throws Exception {
        Random random = new Random(7);
        byte[] good = rich().build();
        Map<String, byte[]> streams = CompoundFile.read(good);
        List<byte[]> inputs = new ArrayList<byte[]>();
        byte[] noise = new byte[4096];
        random.nextBytes(noise);
        inputs.add(noise);
        byte[] signed = noise.clone();
        System.arraycopy(bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1), 0, signed, 0, 8);
        inputs.add(signed);
        Map<String, byte[]> only = new LinkedHashMap<String, byte[]>();
        only.put("WordDocument", noise);
        inputs.add(CompoundFileWriter.write(only));
        for (DocWriter w : new DocWriter[] {revised(), tables()}) {
            Map<String, byte[]> s = CompoundFile.read(w.build());
            for (int i = 0; i < 100; i++) {
                inputs.add(corruptable(good, s, random, i));
            }
        }
        for (int i = 0; i < 100; i++) {
            inputs.add(corruptable(good, streams, random, i));
        }
        for (byte[] in : inputs) {
            try {
                DocReader.read(in);
            } catch (IOException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }
}
