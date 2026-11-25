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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/// Writes an `ArtDocument` as a layered Photoshop PSD: RGB, 8 bits per channel, RLE.
///
/// Groups become Photoshop groups with their blend mode, pass through included, and every
/// other node becomes a pixel layer keeping its name, opacity, blend mode and visibility. A
/// node Photoshop cannot hold as it is -- a shape, text, a node with effects, masks or a
/// clip path, an adjustment -- is rendered to pixels first; an adjustment is rendered over
/// the content below it. The merged image is the whole document, rendered.
public final class PsdWriter {
    private final ByteArrayOutputStream records = new ByteArrayOutputStream();
    private final ByteArrayOutputStream channels = new ByteArrayOutputStream();
    private int count;
    private ArtDocument doc;
    private int width;
    private int height;

    private PsdWriter() {
    }

    /// Writes the document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    /// - `out`: receives the file; not closed
    public static void write(ArtDocument doc, OutputStream out) throws IOException {
        out.write(toBytes(doc));
    }

    /// Returns the document as PSD bytes.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    public static byte[] toBytes(ArtDocument doc) throws IOException {
        return new PsdWriter().build(doc);
    }

    private byte[] build(ArtDocument d) throws IOException {
        doc = d;
        width = Math.max(1, (int) Math.ceil(d.getWidth()));
        height = Math.max(1, (int) Math.ceil(d.getHeight()));
        if (width > PsdReader.MAX_DIMENSION || height > PsdReader.MAX_DIMENSION
                || (long) width * height > Raster.MAX_PIXELS) {
            throw new IOException("Document is too large for PSD");
        }
        List<ArtNode> ch = d.getRoot().getChildren();
        for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            node(ch.get(i), d.getRoot().getTransform());
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(new byte[]{'8', 'B', 'P', 'S'});
        u16(o, 1);
        o.write(new byte[6]);
        u16(o, 4);
        u32(o, height);
        u32(o, width);
        u16(o, 8);
        u16(o, 3);
        u32(o, 0);
        u32(o, 0);
        ByteArrayOutputStream info = new ByteArrayOutputStream();
        u16(info, count == 0 ? 0 : -count);
        records.writeTo(info);
        channels.writeTo(info);
        if (info.size() % 2 != 0) {
            info.write(0);
        }
        u32(o, info.size() + 4 + 4);
        u32(o, info.size());
        info.writeTo(o);
        u32(o, 0);
        Raster merged = ArtRenderer.render(d, 1);
        Raster full = merged.getWidth() == width && merged.getHeight() == height
                ? merged : merged.resize(0, 0, width, height);
        u16(o, 1);
        int[] px = full.getPixels();
        byte[][] rows = new byte[4 * height][];
        int[] shifts = {16, 8, 0, 24};
        byte[] line = new byte[width];
        for (int c = 0; c < 4; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    line[x] = (byte) (px[y * width + x] >> shifts[c]);
                }
                rows[c * height + y] = packBits(line, width);
            }
        }
        for (int i = 0; i < rows.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            u16(o, rows[i].length);
        }
        for (int i = 0; i < rows.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            o.write(rows[i]);
        }
        return o.toByteArray();
    }

    private boolean knockout;

    private static boolean keepsAsGroup(ArtNode n) {
        if (!(n instanceof GroupNode)) {
            return false;
        }
        GroupNode g = (GroupNode) n;
        return n.getEffects().isEmpty() && n.getMask() == null && n.getClipPath() == null
                && !(g.getFrame() != null && g.isClipToFrame());
    }

    private void node(ArtNode n, Matrix parent) throws IOException {
        if (keepsAsGroup(n)) {
            GroupNode g = (GroupNode) n;
            record(null, "</Layer group>", 255, BlendMode.NORMAL, true, 3, false);
            Matrix m = parent.multiply(n.getTransform());
            List<ArtNode> ch = g.getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                node(ch.get(i), m);
            }
            record(null, name(n, "Group"), Math.round(n.getOpacity() * 255), n.getBlendMode(), n.isVisible(), 1,
                    g.isPassThrough());
            return;
        }
        Raster target = new Raster(0, 0, width, height);
        ArtNode copy = n.copy();
        copy.setOpacity(1);
        copy.setBlendMode(BlendMode.NORMAL);
        copy.setVisible(true);
        // Knockout erases the layers below it in the group, so it is written as Photoshop's
        // own flag rather than rendered into this layer's pixels.
        copy.setKnockout(false);
        knockout = n.isKnockout();
        BlendMode bm = n.getBlendMode();
        if (n instanceof AdjustmentNode) {
            // An adjustment changes what is under it; bake it over the content before it.
            ArtDocument under = SvgWriter.before(doc, n);
            new ArtRenderer().renderNode(under.getRoot(), Matrix.IDENTITY, target);
            bm = BlendMode.NORMAL;
        }
        new ArtRenderer().renderNode(copy, parent, target);
        record(target, name(n, "Layer"), Math.round(n.getOpacity() * 255), bm, n.isVisible(), -1, false);
        knockout = false;
    }

    private static String name(ArtNode n, String def) {
        return n.getName() == null || n.getName().length() == 0 ? def : n.getName();
    }

    private void record(Raster r, String name, int opacity, BlendMode mode, boolean visible, int section,
            boolean passThrough) throws IOException {
        count++;
        int top = 0;
        int left = 0;
        int bottom = 0;
        int right = 0;
        if (r != null) {
            int[] b = opaqueBounds(r);
            if (b != null) {
                left = b[0];
                top = b[1];
                right = b[2];
                bottom = b[3];
            }
        }
        int w = right - left;
        int h = bottom - top;
        byte[][] data = new byte[4][];
        int[] ids = {-1, 0, 1, 2};
        int[] shifts = {24, 16, 8, 0};
        for (int c = 0; c < 4; c++) {
            ByteArrayOutputStream cd = new ByteArrayOutputStream();
            if (w <= 0 || h <= 0) {
                u16(cd, 0);
            } else {
                u16(cd, 1);
                byte[][] rows = new byte[h][];
                byte[] line = new byte[w];
                int[] px = r.getPixels();
                int rw = r.getWidth();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        line[x] = (byte) (px[(top + y) * rw + left + x] >> shifts[c]);
                    }
                    rows[y] = packBits(line, w);
                }
                for (int y = 0; y < h; y++) {
                    u16(cd, rows[y].length);
                }
                for (int y = 0; y < h; y++) {
                    cd.write(rows[y]);
                }
            }
            data[c] = cd.toByteArray();
        }
        ByteArrayOutputStream rec = records;
        u32(rec, top);
        u32(rec, left);
        u32(rec, bottom);
        u32(rec, right);
        u16(rec, 4);
        for (int c = 0; c < 4; c++) {
            u16(rec, ids[c]);
            u32(rec, data[c].length);
        }
        rec.write(new byte[]{'8', 'B', 'I', 'M'});
        String key = passThrough ? "pass" : PsdReader.key(mode);
        if (section == 3) {
            key = "norm";
        }
        ascii(rec, key);
        rec.write(Math.max(0, Math.min(255, opacity)));
        rec.write(0);
        // Bit 3 says bit 4 is meaningful; bit 4 marks a group divider's pixel data as irrelevant.
        int flags = (visible ? 0 : 2) | 8 | (section >= 0 ? 16 : 0);
        rec.write(flags);
        rec.write(0);
        ByteArrayOutputStream extra = new ByteArrayOutputStream();
        u32(extra, 0);
        u32(extra, 0);
        String ascii = asciiName(name);
        int n = Math.min(255, ascii.length());
        extra.write(n);
        ascii(extra, ascii.substring(0, n));
        int total = n + 1;
        while (total % 4 != 0) {
            extra.write(0);
            total++;
        }
        ByteArrayOutputStream uni = new ByteArrayOutputStream();
        u32(uni, name.length());
        for (int i = 0; i < name.length(); i++) {
            u16(uni, name.charAt(i));
        }
        block(extra, "luni", uni);
        if (knockout) {
            ByteArrayOutputStream k = new ByteArrayOutputStream();
            k.write(1);
            block(extra, "knko", k);
        }
        if (section >= 0) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            u32(s, section);
            ascii(s, "8BIM");
            ascii(s, key);
            block(extra, "lsct", s);
        }
        u32(rec, extra.size());
        extra.writeTo(rec);
        for (int c = 0; c < 4; c++) {
            channels.write(data[c]);
        }
    }

    private static void block(ByteArrayOutputStream out, String key, ByteArrayOutputStream body) throws IOException {
        ascii(out, "8BIM");
        ascii(out, key);
        int len = body.size();
        int padded = (len + 3) & ~3;
        u32(out, padded);
        body.writeTo(out);
        for (int i = len; i < padded; i++) {
            out.write(0);
        }
    }

    private static String asciiName(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 32 && c < 127 ? c : '_');
        }
        return sb.toString();
    }

    private static int[] opaqueBounds(Raster r) {
        int[] px = r.getPixels();
        int w = r.getWidth();
        int h = r.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) != 0) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return null;
        }
        return new int[]{minX + r.getX(), minY + r.getY(), maxX + 1 + r.getX(), maxY + 1 + r.getY()};
    }

    /// PackBits: runs of three or more equal bytes are repeats, everything else literals.
    static byte[] packBits(byte[] src, int len) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(len + len / 128 + 2);
        int i = 0;
        while (i < len) {
            int run = 1;
            while (i + run < len && run < 128 && src[i + run] == src[i]) {
                run++;
            }
            if (run >= 3) {
                o.write(1 - run);
                o.write(src[i]);
                i += run;
                continue;
            }
            int start = i;
            int lit = 0;
            while (i < len && lit < 128) {
                if (i + 2 < len && src[i] == src[i + 1] && src[i] == src[i + 2]) {
                    break;
                }
                i++;
                lit++;
            }
            o.write(lit - 1);
            o.write(src, start, lit);
        }
        return o.toByteArray();
    }

    private static void ascii(OutputStream o, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            o.write(s.charAt(i));
        }
    }

    private static void u16(OutputStream o, int v) throws IOException {
        o.write((v >> 8) & 0xff);
        o.write(v & 0xff);
    }

    private static void u32(OutputStream o, int v) throws IOException {
        o.write((v >>> 24) & 0xff);
        o.write((v >>> 16) & 0xff);
        o.write((v >>> 8) & 0xff);
        o.write(v & 0xff);
    }
}
