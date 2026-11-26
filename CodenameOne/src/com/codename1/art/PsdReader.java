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

import com.codename1.art.effects.Effect;
import com.codename1.art.effects.Effects;
import com.codename1.impl.pdf.PngDecoder;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads Photoshop PSD and PSB files into an `ArtDocument`.
///
/// Each pixel layer becomes a `RasterNode`, each group a `GroupNode` (pass through or
/// isolated), each adjustment layer an `AdjustmentNode`, and each solid or gradient fill
/// layer a `ShapeNode` over the canvas. Blend modes, opacity, fill opacity, visibility,
/// clipping masks, layer masks, vector masks and the layer style effects (shadows, glows,
/// bevel, satin, overlays and stroke) are kept and rendered. Text and smart object layers
/// are read as the pixels Photoshop stored for them.
///
/// RGB, grayscale, CMYK, Lab, indexed, duotone and bitmap images at 1, 8, 16 and 32 bits
/// per channel are read. Without layers, the merged image is the one layer.
public final class PsdReader {
    /// The largest width or height read: Photoshop's own PSB limit.
    public static final int MAX_DIMENSION = 300000;
    /// The most layers read.
    public static final int MAX_LAYERS = 10000;

    private static final int MODE_BITMAP = 0;
    private static final int MODE_GRAY = 1;
    private static final int MODE_INDEXED = 2;
    private static final int MODE_RGB = 3;
    private static final int MODE_CMYK = 4;
    private static final int MODE_DUOTONE = 8;
    private static final int MODE_LAB = 9;

    private PsdData d;
    private boolean big;
    private int width;
    private int height;
    private int depth;
    private int mode;
    private byte[] palette;
    private final List<ImportNote> notes = new ArrayList<ImportNote>();
    /// The document's pattern library, by pattern id.
    private final Map<String, Raster> patterns = new HashMap<String, Raster>();
    /// Pattern overlays read before the library: each effect and the id it names.
    private final List<Object[]> patternUses = new ArrayList<Object[]>();

    private static final class Channel {
        int id;
        long length;
    }

    private static final class Layer {
        int top;
        int left;
        int bottom;
        int right;
        Channel[] channels;
        String blend;
        int opacity;
        boolean clipping;
        int flags;
        String name;
        int section = -1;
        String sectionBlend;
        float fillOpacity = 1;
        int maskTop;
        int maskLeft;
        int maskBottom;
        int maskRight;
        int maskDefault;
        boolean maskDisabled;
        Effect adjustment;
        ArtPaint fillLayer;
        String patternId;
        float patternScale = 1;
        List<Effect> effects = new ArrayList<Effect>();
        GeneralPath vectorMask;
        String text;
        int[] pixels;
        byte[] mask;
        boolean knockout;
    }

    private PsdReader() {
    }

    /// Reads a PSD or PSB file.
    ///
    /// #### Parameters
    ///
    /// - `in`: the file; read to its end, not closed
    public static ArtDocument read(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > Integer.MAX_VALUE - 64) {
                throw new IOException("PSD file is too large to read");
            }
            bo.write(buf, 0, n);
        }
        return read(bo.toByteArray());
    }

    /// Reads a PSD or PSB file from bytes.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    public static ArtDocument read(byte[] data) throws IOException {
        try {
            return new PsdReader().parse(data);
        } catch (RuntimeException e) {
            throw ArtFormats.damaged(e);
        }
    }

    private void note(String where, String msg) {
        for (ImportNote n : notes) {
            if (n.getWhere().equals(where) && n.getMessage().equals(msg)) {
                return;
            }
        }
        if (notes.size() < 500) {
            notes.add(new ImportNote(where, msg));
        }
    }

    private ArtDocument parse(byte[] data) throws IOException {
        d = new PsdData(data, 0, data.length);
        if (!"8BPS".equals(d.key())) {
            throw new IOException("Not a PSD file");
        }
        int version = d.u16();
        if (version != 1 && version != 2) {
            throw new IOException("Unknown PSD version " + version);
        }
        big = version == 2;
        d.skip(6);
        int channels = d.u16();
        height = d.s32();
        width = d.s32();
        depth = d.u16();
        mode = d.u16();
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IOException("PSD dimensions are out of range");
        }
        if ((long) width * height > Raster.MAX_PIXELS) {
            throw new IOException("PSD image is too large");
        }
        if (depth != 1 && depth != 8 && depth != 16 && depth != 32) {
            throw new IOException("Unsupported PSD depth " + depth);
        }
        if (channels < 1 || channels > 56) {
            throw new IOException("PSD channel count is out of range");
        }
        long cmLen = d.u32();
        if (mode == MODE_INDEXED && cmLen >= 768) {
            palette = new byte[768];
            System.arraycopy(data, d.pos, palette, 0, 768);
        }
        if (mode == MODE_DUOTONE) {
            note("color mode", "duotone is shown as grayscale");
        } else if (mode != MODE_RGB && mode != MODE_GRAY && mode != MODE_CMYK && mode != MODE_LAB
                && mode != MODE_INDEXED && mode != MODE_BITMAP) {
            note("color mode", "multichannel image read from its first channels as grayscale");
        }
        d.skip(cmLen);
        long resLen = d.u32();
        d.skip(resLen);
        long lmLen = d.len(big);
        int lmEnd = d.pos + (int) lmLen;
        List<Layer> layers = new ArrayList<Layer>();
        if (lmLen > 0) {
            long liLen = d.len(big);
            int liEnd = d.pos + (int) liLen;
            if (liLen > 0) {
                readLayers(layers, liEnd, data);
            }
            d.pos = liEnd;
            // Additional layer information after the global mask: 16-bit and 32-bit files keep
            // their layers here, under Lr16 / Lr32.
            if (d.pos + 4 <= lmEnd) {
                long gm = d.u32();
                d.skip(Math.min(gm, lmEnd - d.pos));
            }
            while (d.pos + 12 <= lmEnd) {
                String sig = d.key();
                String k = d.key();
                if (!"8BIM".equals(sig) && !"8B64".equals(sig)) {
                    break;
                }
                long l = bigKey(k) ? d.len(big) : d.u32();
                int st = d.pos;
                if (layers.isEmpty() && ("Lr16".equals(k) || "Lr32".equals(k) || "Layr".equals(k))) {
                    readLayers(layers, st + (int) l, data);
                } else if (isPatternKey(k)) {
                    try {
                        readPatterns(new PsdData(d.b, st, st + (int) l));
                    } catch (IOException e) {
                        note("patterns", "pattern library could not be read: " + e.getMessage());
                    }
                }
                d.pos = st + (int) l;
                while ((d.pos - st) % 4 != 0 && d.pos < lmEnd) {
                    d.pos++;
                }
            }
        }
        d.pos = lmEnd;
        for (Object[] u : patternUses) {
            Raster tile = patterns.get((String) u[1]);
            if (tile == null) {
                note("patterns", "pattern " + u[1] + " is not in the file");
            } else {
                ((Effect) u[0]).set("pattern", tile);
            }
        }
        ArtDocument doc = new ArtDocument(width, height);
        GroupNode root = new GroupNode();
        if (layers.isEmpty()) {
            int[] merged = readMerged(channels);
            RasterNode n = new RasterNode(new Raster(0, 0, width, height, merged));
            n.setName("Background");
            root.add(n);
        } else {
            build(layers, root);
        }
        doc.setRoot(root);
        doc.getNotes().addAll(notes);
        return doc;
    }

    private static boolean bigKey(String k) {
        return "LMsk".equals(k) || "Lr16".equals(k) || "Lr32".equals(k) || "Layr".equals(k) || "Mt16".equals(k)
                || "Mt32".equals(k) || "Mtrn".equals(k) || "Alph".equals(k) || "FMsk".equals(k) || "lnk2".equals(k)
                || "FEid".equals(k) || "FXid".equals(k) || "PxSD".equals(k);
    }

    private void readLayers(List<Layer> layers, int end, byte[] data) throws IOException {
        int count = d.s16();
        if (count < 0) {
            count = -count;
        }
        if (count > MAX_LAYERS) {
            throw new IOException("PSD has too many layers");
        }
        List<Layer> read = new ArrayList<Layer>();
        for (int i = 0; i < count; i++) {
            read.add(record());
        }
        for (int i = 0; i < count; i++) {
            Layer l = read.get(i);
            pixels(l, data);
        }
        if (d.pos > end) {
            throw new IOException("PSD layer data runs past its section");
        }
        layers.addAll(read);
    }

    private Layer record() throws IOException {
        Layer l = new Layer();
        l.top = d.s32();
        l.left = d.s32();
        l.bottom = d.s32();
        l.right = d.s32();
        int nc = d.u16();
        if (nc > 56) {
            throw new IOException("PSD layer has too many channels");
        }
        l.channels = new Channel[nc];
        for (int i = 0; i < nc; i++) {
            Channel c = new Channel();
            c.id = d.s16();
            c.length = big ? d.s64() : d.u32();
            l.channels[i] = c;
        }
        if (!"8BIM".equals(d.key())) {
            throw new IOException("PSD layer record is damaged");
        }
        l.blend = d.key();
        l.opacity = d.u8();
        l.clipping = d.u8() != 0;
        l.flags = d.u8();
        d.u8();
        long extra = d.u32();
        d.need(extra);
        int end = d.pos + (int) extra;
        long maskLen = d.u32();
        if (maskLen > end - d.pos) {
            throw new IOException("Layer mask data runs past its record");
        }
        int maskEnd = d.pos + (int) maskLen;
        if (maskLen >= 18) {
            l.maskTop = d.s32();
            l.maskLeft = d.s32();
            l.maskBottom = d.s32();
            l.maskRight = d.s32();
            l.maskDefault = d.u8();
            int mf = d.u8();
            l.maskDisabled = (mf & 2) != 0;
        }
        d.pos = maskEnd;
        long br = d.u32();
        if (br > end - d.pos) {
            throw new IOException("Blending ranges run past their record");
        }
        d.skip(br);
        l.name = d.pascal(4);
        while (d.pos + 12 <= end) {
            String sig = d.key();
            if (!"8BIM".equals(sig) && !"8B64".equals(sig)) {
                break;
            }
            String k = d.key();
            long len = big && bigKey(k) ? d.s64() : d.u32();
            if (len < 0 || d.pos + len > end) {
                break;
            }
            int st = d.pos;
            try {
                info(l, k, new PsdData(d.b, st, st + (int) len));
            } catch (IOException e) {
                note(l.name, "layer data '" + k + "' could not be read: " + e.getMessage());
            }
            d.pos = st + (int) len;
        }
        d.pos = end;
        return l;
    }

    /// Reads one pattern library block and answers the pattern with the given id, for tests.
    static Raster readPattern(byte[] block, String id) throws IOException {
        PsdReader r = new PsdReader();
        r.readPatterns(new PsdData(block, 0, block.length));
        return r.patterns.get(id);
    }

    private static boolean isPatternKey(String k) {
        return "Patt".equals(k) || "Pat2".equals(k) || "Pat3".equals(k);
    }

    /// Reads a pattern library block: each pattern's id, mode and pixels.
    private void readPatterns(PsdData p) throws IOException {
        int count = 0;
        while (p.pos + 4 <= p.end && count++ < 4096) {
            long len = p.u32();
            int st = p.pos;
            if (len <= 0 || st + len > p.end) {
                break;
            }
            PsdData q = new PsdData(p.b, st, st + (int) len);
            q.u32();
            int pm = (int) q.u32();
            q.u16();
            q.u16();
            q.unicode();
            String id = q.pascal(1);
            int[] table = null;
            if (pm == 2) {
                table = new int[256];
                for (int i = 0; i < 256; i++) {
                    table[i] = 0xff000000 | (q.u8() << 16) | (q.u8() << 8) | q.u8();
                }
            }
            Raster tile = patternPixels(q, pm, table);
            if (tile != null) {
                patterns.put(id, tile);
            }
            p.pos = st + (int) ((len + 3) & ~3L);
        }
    }

    /// The pixels of one pattern: a virtual memory array list of its channels, the user mask
    /// and the sheet mask, each raw or RLE.
    private Raster patternPixels(PsdData q, int pm, int[] table) throws IOException {
        q.u32();
        q.u32();
        int top = q.s32();
        int left = q.s32();
        int bottom = q.s32();
        int right = q.s32();
        int w = right - left;
        int h = bottom - top;
        if (w <= 0 || h <= 0 || w > 4096 || h > 4096) {
            return null;
        }
        int nc = (int) q.u32();
        if (nc < 1 || nc > 56) {
            return null;
        }
        byte[][] ch = new byte[nc + 2][];
        for (int c = 0; c < nc + 2 && q.pos + 8 <= q.end; c++) {
            if (q.u32() == 0) {
                continue;
            }
            long alen = q.u32();
            if (alen == 0) {
                continue;
            }
            int ast = q.pos;
            int pd = (int) q.u32();
            q.skip(16);
            q.u16();
            int comp = q.u8();
            if (pd == 8) {
                byte[] out = new byte[w * h];
                if (comp == 0) {
                    q.need(out.length);
                    System.arraycopy(q.b, q.pos, out, 0, out.length);
                } else {
                    int[] counts = new int[h];
                    int total = 0;
                    for (int y = 0; y < h; y++) {
                        counts[y] = q.u16();
                        total += counts[y];
                    }
                    q.need(total);
                    int src = q.pos;
                    for (int y = 0; y < h; y++) {
                        packBits(q.b, src, src + counts[y], out, y * w, w);
                        src += counts[y];
                    }
                }
                ch[c] = out;
            }
            q.pos = ast + (int) alen;
        }
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            int r;
            int g;
            int b;
            if (pm == 2 && ch[0] != null) {
                int v = table[ch[0][i] & 0xff];
                r = (v >> 16) & 0xff;
                g = (v >> 8) & 0xff;
                b = v & 0xff;
            } else if ((pm == 3 || pm == 4) && nc >= 3 && ch[0] != null && ch[1] != null && ch[2] != null) {
                r = ch[0][i] & 0xff;
                g = ch[1][i] & 0xff;
                b = ch[2][i] & 0xff;
                if (pm == 4) {
                    // CMYK is stored inverted: 255 is no ink.
                    int k = nc >= 4 && ch[3] != null ? ch[3][i] & 0xff : 255;
                    r = r * k / 255;
                    g = g * k / 255;
                    b = b * k / 255;
                }
            } else if (ch[0] != null) {
                r = ch[0][i] & 0xff;
                g = r;
                b = r;
            } else {
                return null;
            }
            // Transparency is kept in the last written array after the color channels.
            int a = 255;
            for (int c = nc + 1; c >= colorChannels(pm); c--) {
                if (ch[c] != null) {
                    a = ch[c][i] & 0xff;
                    break;
                }
            }
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return new Raster(0, 0, w, h, px);
    }

    private static int colorChannels(int pm) {
        return pm == 4 ? 4 : (pm == 3 ? 3 : 1);
    }

    private void info(Layer l, String k, PsdData p) throws IOException {
        if (isPatternKey(k)) {
            readPatterns(p);
            return;
        }
        if ("luni".equals(k)) {
            l.name = p.unicode();
        } else if ("lsct".equals(k) || "lsdk".equals(k)) {
            l.section = p.s32();
            if (p.pos + 8 <= p.end) {
                p.key();
                l.sectionBlend = p.key();
            }
        } else if ("iOpa".equals(k)) {
            l.fillOpacity = p.u8() / 255f;
        } else if ("knko".equals(k)) {
            l.knockout = p.u8() != 0;
        } else if ("lfx2".equals(k) || "lmfx".equals(k)) {
            p.s32();
            p.s32();
            layerEffects(l, p.descriptor(0));
        } else if ("vmsk".equals(k) || "vsms".equals(k)) {
            p.s32();
            int flags = p.s32();
            if ((flags & 2) == 0) {
                l.vectorMask = vectorPath(p);
            }
        } else if ("TySh".equals(k)) {
            p.u16();
            p.skip(48);
            p.u16();
            p.s32();
            Map<String, Object> t = p.descriptor(0);
            l.text = PsdData.str(t, "Txt ");
        } else if ("SoLd".equals(k) || "PlLd".equals(k)) {
            note(l.name, "smart object shown as its stored pixels");
        } else {
            Effect adj = adjustment(k, p, l);
            if (adj != null) {
                l.adjustment = adj;
            }
        }
    }

    private Effect adjustment(String k, PsdData p, Layer l) throws IOException {
        if ("nvrt".equals(k)) {
            return Effects.invert();
        }
        if ("post".equals(k)) {
            return Effects.posterize(Math.max(2, p.u16()));
        }
        if ("thrs".equals(k)) {
            return Effects.threshold(p.u16() / 255f);
        }
        if ("brit".equals(k)) {
            int b = p.s16();
            int c = p.s16();
            return Effects.brightnessContrast(b / 150f, c / 100f);
        }
        if ("CgEd".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            return Effects.brightnessContrast((float) PsdData.num(m, "Brgh", 0) / 150f,
                    (float) PsdData.num(m, "Cntr", 0) / 100f);
        }
        if ("levl".equals(k)) {
            p.u16();
            int ib = p.u16();
            int iw = p.u16();
            int ob = p.u16();
            int ow = p.u16();
            int g = p.u16();
            Effect e = Effects.levels(ib / 255f, iw / 255f, g <= 0 ? 1 : g / 100f, ob / 255f, ow / 255f);
            float[][] ch = new float[3][];
            for (int c = 0; c < 3 && p.pos + 10 <= p.end; c++) {
                int a = p.u16();
                int bb = p.u16();
                int cc = p.u16();
                int dd = p.u16();
                int gg = p.u16();
                ch[c] = new float[]{a / 255f, bb / 255f, gg <= 0 ? 1 : gg / 100f, cc / 255f, dd / 255f};
            }
            if (ch[2] != null) {
                e.set("red", ch[0]).set("green", ch[1]).set("blue", ch[2]);
            }
            return e;
        }
        if ("curv".equals(k)) {
            p.u8();
            p.u16();
            long bits = p.u32();
            float[][] curves = new float[4][];
            for (int c = 0; c < 16 && p.pos < p.end; c++) {
                if ((bits & (1L << c)) == 0) {
                    continue;
                }
                int n = p.u16();
                if (n > 19 * 4) {
                    break;
                }
                float[] pts = new float[n * 2];
                for (int i = 0; i < n; i++) {
                    float out = p.u16() / 255f;
                    float in = p.u16() / 255f;
                    pts[i * 2] = in;
                    pts[i * 2 + 1] = out;
                }
                if (c < 4) {
                    curves[c] = pts;
                }
            }
            return Effects.curves(curves[0], curves[1], curves[2], curves[3]);
        }
        if ("hue2".equals(k) || "hue ".equals(k)) {
            p.u16();
            boolean colorize = p.u8() != 0;
            p.u8();
            int ch = p.s16();
            int cs = p.s16();
            int cl = p.s16();
            int mh = p.s16();
            int ms = p.s16();
            int ml = p.s16();
            if (colorize) {
                return Effects.hueSaturation(ch, cs / 100f, cl / 100f).set("colorize", 1);
            }
            return Effects.hueSaturation(mh, ms / 100f, ml / 100f);
        }
        if ("expA".equals(k)) {
            p.u16();
            return Effects.exposure(p.f32(), p.f32(), p.f32());
        }
        if ("vibA".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            float v = (float) PsdData.num(m, "vibrance", 0) / 100f;
            float s = (float) PsdData.num(m, "Strt", 0) / 100f;
            Effect e = Effects.vibrance(v);
            if (s != 0) {
                e.set("saturation", s);
            }
            return e;
        }
        if ("phfl".equals(k)) {
            int ver = p.u16();
            int color;
            if (ver == 3) {
                double lab0 = p.s32() / 100.0;
                double lab1 = p.s32() / 100.0;
                double lab2 = p.s32() / 100.0;
                color = lab(lab0, lab1, lab2);
            } else {
                color = colorRecord(p);
            }
            int density = p.s32();
            boolean keep = p.u8() != 0;
            return Effects.photoFilter(color, density / 100f).set("preserveLuminosity", keep ? 1 : 0);
        }
        if ("mixr".equals(k)) {
            p.u16();
            boolean mono = p.u16() != 0;
            float[] m = new float[12];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    m[r * 4 + c] = p.s16() / 100f;
                }
                p.s16();
            }
            if (mono) {
                for (int c = 0; c < 4; c++) {
                    m[4 + c] = m[c];
                    m[8 + c] = m[c];
                }
            }
            return Effects.channelMixer(m);
        }
        if ("grdm".equals(k)) {
            p.u16();
            boolean reverse = p.u8() != 0;
            p.u8();
            p.unicode();
            int n = p.u16();
            if (n > 1000) {
                throw new IOException("Too many gradient stops");
            }
            float[] pos = new float[n];
            int[] cols = new int[n];
            for (int i = 0; i < n; i++) {
                pos[i] = p.s32() / 4096f;
                p.s32();
                cols[i] = colorRecord(p);
                p.u16();
            }
            if (reverse) {
                for (int i = 0; i < n; i++) {
                    pos[i] = 1 - pos[i];
                }
                for (int i = 0; i < n / 2; i++) {
                    float tp = pos[i];
                    pos[i] = pos[n - 1 - i];
                    pos[n - 1 - i] = tp;
                    int tc = cols[i];
                    cols[i] = cols[n - 1 - i];
                    cols[n - 1 - i] = tc;
                }
            }
            return Effects.gradientMap(pos, cols);
        }
        if ("blwh".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            float[] w = {(float) PsdData.num(m, "Rd  ", 40) / 100f, (float) PsdData.num(m, "Yllw", 60) / 100f,
                (float) PsdData.num(m, "Grn ", 40) / 100f, (float) PsdData.num(m, "Cyn ", 60) / 100f,
                (float) PsdData.num(m, "Bl  ", 20) / 100f, (float) PsdData.num(m, "Mgnt", 80) / 100f};
            return Effects.blackAndWhite().set("weights", w);
        }
        if ("blnc".equals(k)) {
            float[][] r = new float[3][3];
            for (int t = 0; t < 3; t++) {
                for (int c = 0; c < 3; c++) {
                    r[t][c] = p.s16() / 100f;
                }
            }
            boolean keep = p.u8() != 0;
            return Effects.colorBalance(r[0], r[1], r[2], keep);
        }
        if ("selc".equals(k)) {
            p.u16();
            boolean abs = p.u16() != 0;
            float[] ranges = new float[36];
            p.skip(8);
            for (int i = 0; i < 9; i++) {
                for (int c = 0; c < 4; c++) {
                    ranges[i * 4 + c] = p.s16() / 100f;
                }
            }
            return Effects.selectiveColor(ranges, abs);
        }
        if ("SoCo".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            l.fillLayer = new SolidPaint(PsdData.color(PsdData.obj(m, "Clr "), 1));
            return null;
        }
        if ("GdFl".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            l.fillLayer = gradientPaint(m);
            return null;
        }
        if ("PtFl".equals(k)) {
            p.s32();
            Map<String, Object> m = p.descriptor(0);
            l.patternId = PsdData.str(PsdData.obj(m, "Ptrn"), "Idnt");
            l.patternScale = pct(m, "Scl ", 100);
            return null;
        }
        return null;
    }

    private int colorRecord(PsdData p) throws IOException {
        int space = p.u16();
        int a = p.u16();
        int b = p.u16();
        int c = p.u16();
        int dd = p.u16();
        if (space == 0) {
            return 0xff000000 | ((a >> 8) << 16) | ((b >> 8) << 8) | (c >> 8);
        }
        if (space == 8) {
            int g = 255 - a * 255 / 10000;
            return 0xff000000 | (g << 16) | (g << 8) | g;
        }
        if (space == 2) {
            double k = 1 - dd / 65535.0;
            int r = (int) (255 * (a / 65535.0) * k);
            int g = (int) (255 * (b / 65535.0) * k);
            int bl = (int) (255 * (c / 65535.0) * k);
            return 0xff000000 | (r << 16) | (g << 8) | bl;
        }
        if (space == 7) {
            return lab(a / 100.0, (short) b / 100.0, (short) c / 100.0);
        }
        return 0xff000000 | ((a >> 8) << 16) | ((b >> 8) << 8) | (c >> 8);
    }

    static int lab(double l, double a, double b) {
        double fy = (l + 16) / 116;
        double fx = fy + a / 500;
        double fz = fy - b / 200;
        double x = 0.95047 * finv(fx);
        double y = finv(fy);
        double z = 1.08883 * finv(fz);
        double r = 3.2406 * x - 1.5372 * y - 0.4986 * z;
        double g = -0.9689 * x + 1.8758 * y + 0.0415 * z;
        double bl = 0.0557 * x - 0.2040 * y + 1.0570 * z;
        return 0xff000000 | (srgb(r) << 16) | (srgb(g) << 8) | srgb(bl);
    }

    private static double finv(double t) {
        return t > 6.0 / 29 ? t * t * t : 3 * (6.0 / 29) * (6.0 / 29) * (t - 4.0 / 29);
    }

    private static int srgb(double v) {
        v = v <= 0.0031308 ? 12.92 * v : 1.055 * MathUtil.pow(v, 1 / 2.4) - 0.055;
        int i = (int) Math.round(v * 255);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private GradientPaint gradientPaint(Map<String, Object> m) {
        Map<String, Object> g = PsdData.obj(m, "Grad");
        float[][] pos = new float[1][];
        int[][] cols = new int[1][];
        stops(g, pos, cols);
        double angle = Math.toRadians(PsdData.num(m, "Angl", 90));
        String type = PsdData.str(m, "Type");
        double cx = width / 2.0;
        double cy = height / 2.0;
        double half = Math.abs(Math.cos(angle)) * width / 2 + Math.abs(Math.sin(angle)) * height / 2;
        double dx = Math.cos(angle) * half;
        double dy = -Math.sin(angle) * half;
        if (PsdData.bool(m, "Rvrs", false)) {
            dx = -dx;
            dy = -dy;
        }
        GradientPaint gp;
        if ("Rdl ".equals(type)) {
            gp = GradientPaint.radial(cx, cy, half, pos[0], cols[0]);
        } else if ("Angl".equals(type)) {
            gp = GradientPaint.conic(cx, cy, -angle, pos[0], cols[0]);
        } else {
            gp = GradientPaint.linear(cx - dx, cy - dy, cx + dx, cy + dy, pos[0], cols[0]);
            if ("Rflc".equals(type)) {
                gp.setPoints(cx, cy, cx + dx, cy + dy);
                gp.setSpread(GradientPaint.SPREAD_REFLECT);
            } else if ("Dmnd".equals(type)) {
                gp = GradientPaint.diamond(cx, cy, half, pos[0], cols[0]);
            }
        }
        return gp;
    }

    private static void stops(Map<String, Object> g, float[][] posOut, int[][] colsOut) {
        List<?> clrs = PsdData.list(g, "Clrs");
        List<?> trns = PsdData.list(g, "Trns");
        int n = clrs == null ? 0 : clrs.size();
        if (n < 1) {
            posOut[0] = new float[]{0, 1};
            colsOut[0] = new int[]{0xff000000, 0xffffffff};
            return;
        }
        float[] pos = new float[n];
        int[] cols = new int[n];
        for (int i = 0; i < n; i++) {
            Object o = clrs.get(i);
            @SuppressWarnings("unchecked")
            Map<String, Object> c = o instanceof Map ? (Map<String, Object>) o : null;
            pos[i] = (float) PsdData.num(c, "Lctn", 0) / 4096f;
            double a = 1;
            if (trns != null && !trns.isEmpty()) {
                a = opacityAt(trns, pos[i]);
            }
            cols[i] = PsdData.color(PsdData.obj(c, "Clr "), (float) a);
        }
        posOut[0] = pos;
        colsOut[0] = cols;
    }

    private static double opacityAt(List<?> trns, float at) {
        double prevL = -1;
        double prevO = 1;
        for (Object o : trns) {
            if (!(o instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> t = (Map<String, Object>) o;
            double l = PsdData.num(t, "Lctn", 0) / 4096;
            double op = PsdData.num(t, "Opct", 100) / 100;
            if (l >= at) {
                if (prevL < 0 || l == prevL) {
                    return op;
                }
                return prevO + (op - prevO) * (at - prevL) / (l - prevL);
            }
            prevL = l;
            prevO = op;
        }
        return prevO;
    }

    private static float pct(Map<String, Object> m, String k, double def) {
        return (float) (PsdData.num(m, k, def) / 100);
    }

    private static BlendMode modeOf(Map<String, Object> m, BlendMode def) {
        String md = PsdData.str(m, "Md  ");
        return md == null ? def : descriptorBlend(md);
    }

    private void layerEffects(Layer l, Map<String, Object> fx) {
        if (!PsdData.bool(fx, "masterFXSwitch", true)) {
            return;
        }
        double globalAngle = 120;
        java.util.Iterator<Map.Entry<String, Object>> it = fx.entrySet().iterator();
        List<Object[]> items = new ArrayList<Object[]>();
        while (it.hasNext()) {
            Map.Entry<String, Object> en = it.next();
            Object v = en.getValue();
            if (v instanceof Map) {
                items.add(new Object[]{en.getKey(), v});
            } else if (v instanceof List) {
                List<?> lst = (List<?>) v;
                String key = en.getKey();
                String single = key.endsWith("Multi") ? key.substring(0, key.length() - 5) : key;
                for (int i = 0; i < lst.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    items.add(new Object[]{single, lst.get(i)});
                }
            }
        }
        // Photoshop paints a layer's styles in a fixed order, not in the order it stores them.
        String[] order = {"DrSh", "dropShadow", "OrGl", "IrSh", "innerShadow", "IrGl", "ebbl", "ChFX",
            "SoFi", "solidFill", "GrFl", "gradientFill", "patternFill", "FrFX", "frameFX"};
        for (int o = 0; o < order.length; o++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            for (Object[] item : items) {
                if (!order[o].equals(item[0]) || !(item[1] instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> e = (Map<String, Object>) item[1];
                if (!PsdData.bool(e, "enab", true)) {
                    continue;
                }
                Effect ef = effect(order[o], e, globalAngle);
                if (ef != null) {
                    l.effects.add(ef);
                }
            }
        }
        // A stroke emboss lights the stroke, so it runs after the stroke it lights.
        Effect strokeFx = null;
        Effect emboss = null;
        for (Effect ef : l.effects) {
            if (Effect.STROKE.equals(ef.getKind())) {
                strokeFx = ef;
            } else if (Effect.BEVEL.equals(ef.getKind()) && "stroke".equals(ef.getString("style", null))) {
                emboss = ef;
            }
        }
        if (emboss != null) {
            if (strokeFx == null) {
                // Photoshop draws nothing for a stroke emboss on a layer without a stroke.
                l.effects.remove(emboss);
            } else {
                emboss.set("strokeWidth", strokeFx.getFloat("width", 3));
                l.effects.remove(emboss);
                l.effects.add(l.effects.indexOf(strokeFx) + 1, emboss);
            }
        }
    }

    private Effect effect(String k, Map<String, Object> e, double globalAngle) {
        float op = pct(e, "Opct", 100);
        int color = PsdData.color(PsdData.obj(e, "Clr "), op);
        double ang = PsdData.bool(e, "uglg", false) ? globalAngle : PsdData.num(e, "lagl", globalAngle);
        float size = (float) PsdData.num(e, "blur", 5);
        float spread = pct(e, "Ckmt", 0);
        float dist = (float) PsdData.num(e, "Dstn", 5);
        float dx = (float) (-Math.cos(Math.toRadians(ang)) * dist);
        float dy = (float) (Math.sin(Math.toRadians(ang)) * dist);
        if ("DrSh".equals(k) || "dropShadow".equals(k)) {
            Effect ef = Effects.dropShadow(color, dx, dy, size, spread * size);
            ef.set("blend", modeOf(e, BlendMode.MULTIPLY));
            if (!PsdData.bool(e, "layerConceals", true)) {
                ef.set("knockout", 0);
            }
            return ef;
        }
        if ("IrSh".equals(k) || "innerShadow".equals(k)) {
            return Effects.innerShadow(color, dx, dy, size, spread * size).set("blend", modeOf(e, BlendMode.MULTIPLY));
        }
        if ("OrGl".equals(k)) {
            color = glowColor(e, op, color);
            return Effects.outerGlow(color, size, spread * size).set("blend", modeOf(e, BlendMode.SCREEN));
        }
        if ("IrGl".equals(k)) {
            color = glowColor(e, op, color);
            boolean center = "SrcC".equals(PsdData.str(e, "glwS"));
            return Effects.innerGlow(color, size, center).set("blend", modeOf(e, BlendMode.SCREEN))
                    .set("spread", spread * size);
        }
        if ("ebbl".equals(k)) {
            String st = PsdData.str(e, "bvlS");
            String style = "OtrB".equals(st) ? "outer" : "Embs".equals(st) ? "emboss" : "PlEb".equals(st) ? "pillow" : "inner";
            if ("strokeEmboss".equals(st)) {
                style = "stroke";
            }
            Effect ef = Effects.bevel(style, size, pct(e, "srgR", 100), (float) ang, (float) PsdData.num(e, "Lald", 30));
            ef.setColor("highlight", PsdData.color(PsdData.obj(e, "hglC"), 1));
            ef.setColor("shadow", PsdData.color(PsdData.obj(e, "sdwC"), 1));
            ef.set("highlightOpacity", pct(e, "hglO", 75));
            ef.set("shadowOpacity", pct(e, "sdwO", 75));
            ef.set("highlightBlend", modeOf(PsdData.obj(e, "hglM") == null ? null : e, BlendMode.SCREEN));
            if ("Dwn ".equals(PsdData.str(e, "bvlD"))) {
                ef.set("angle", (float) (ang + 180));
            }
            ef.set("soften", (float) PsdData.num(e, "Sftn", 0));
            return ef;
        }
        if ("ChFX".equals(k)) {
            Effect ef = Effects.satin(color, (float) ang, dist, size).set("blend", modeOf(e, BlendMode.MULTIPLY));
            ef.set("invert", PsdData.bool(e, "Invr", true) ? 1 : 0);
            return ef;
        }
        if ("SoFi".equals(k) || "solidFill".equals(k)) {
            return Effects.colorOverlay(color).set("blend", modeOf(e, BlendMode.NORMAL));
        }
        if ("GrFl".equals(k) || "gradientFill".equals(k)) {
            float[][] pos = new float[1][];
            int[][] cols = new int[1][];
            stops(PsdData.obj(e, "Grad"), pos, cols);
            String t = PsdData.str(e, "Type");
            String style = "Rdl ".equals(t) ? "radial" : "Angl".equals(t) ? "angle" : "Rflc".equals(t) ? "reflected"
                    : "Dmnd".equals(t) ? "diamond" : "linear";
            Effect ef = Effects.gradientOverlay(pos[0], cols[0], (float) PsdData.num(e, "Angl", 90), style);
            ef.set("opacity", op).set("blend", modeOf(e, BlendMode.NORMAL)).set("scale", pct(e, "Scl ", 100));
            if (PsdData.bool(e, "Rvrs", false)) {
                ef.set("reverse", 1);
            }
            return ef;
        }
        if ("patternFill".equals(k)) {
            Effect ef = Effects.patternOverlay(null, pct(e, "Scl ", 100));
            ef.set("opacity", op).set("blend", modeOf(e, BlendMode.NORMAL));
            String id = PsdData.str(PsdData.obj(e, "Ptrn"), "Idnt");
            if (id != null) {
                patternUses.add(new Object[]{ef, id});
            }
            return ef;
        }
        if ("FrFX".equals(k) || "frameFX".equals(k)) {
            String st = PsdData.str(e, "Styl");
            String pos = "InsF".equals(st) ? "inside" : "CtrF".equals(st) ? "center" : "outside";
            Effect ef = Effects.stroke(color, (float) PsdData.num(e, "Sz  ", 3), pos);
            ef.set("blend", modeOf(e, BlendMode.NORMAL));
            if ("GrFl".equals(PsdData.str(e, "PntT"))) {
                float[][] gp = new float[1][];
                int[][] gc = new int[1][];
                stops(PsdData.obj(e, "Grad"), gp, gc);
                ef.set("positions", gp[0]).set("colors", gc[0]).set("angle", (float) PsdData.num(e, "Angl", 90));
            }
            return ef;
        }
        return null;
    }

    private static int glowColor(Map<String, Object> e, float op, int color) {
        Map<String, Object> g = PsdData.obj(e, "Grad");
        if (PsdData.obj(e, "Clr ") == null && g != null) {
            float[][] pos = new float[1][];
            int[][] cols = new int[1][];
            stops(g, pos, cols);
            int c = cols[0][0];
            return (Math.round((c >>> 24) * op) << 24) | (c & 0xffffff);
        }
        return color;
    }

    private GeneralPath vectorPath(PsdData p) throws IOException {
        GeneralPath path = new GeneralPath();
        double[] first = null;
        double[] prev = null;
        int remaining = 0;
        boolean closed = false;
        while (p.pos + 26 <= p.end) {
            int sel = p.u16();
            int st = p.pos;
            if (sel == 0 || sel == 3) {
                if (first != null && prev != null && closed) {
                    path.curveTo(prev[4], prev[5], first[0], first[1], first[2], first[3]);
                    path.closePath();
                }
                remaining = p.u16();
                closed = sel == 0;
                first = null;
                prev = null;
            } else if (sel == 1 || sel == 2 || sel == 4 || sel == 5) {
                double[] k = new double[6];
                for (int i = 0; i < 3; i++) {
                    double y = fixed(p.s32()) * height;
                    double x = fixed(p.s32()) * width;
                    k[i * 2] = x;
                    k[i * 2 + 1] = y;
                }
                if (prev == null) {
                    path.moveTo(k[2], k[3]);
                    first = k;
                } else {
                    path.curveTo(prev[4], prev[5], k[0], k[1], k[2], k[3]);
                }
                prev = k;
                remaining--;
                if (remaining == 0 && closed && first != null) {
                    path.curveTo(k[4], k[5], first[0], first[1], first[2], first[3]);
                    path.closePath();
                    first = null;
                    prev = null;
                }
            }
            p.pos = st + 24;
        }
        return path;
    }

    private static double fixed(int v) {
        return v / 16777216.0;
    }

    private void pixels(Layer l, byte[] data) throws IOException {
        int w = l.right - l.left;
        int h = l.bottom - l.top;
        boolean hasImage = w > 0 && h > 0 && w <= MAX_DIMENSION && h <= MAX_DIMENSION
                && (long) w * h <= Raster.MAX_PIXELS;
        int mw = l.maskRight - l.maskLeft;
        int mh = l.maskBottom - l.maskTop;
        byte[][] planes = new byte[5][];
        int[] planeIndex = {-1, 0, 1, 2, 3};
        byte[] alpha = null;
        for (int i = 0; i < l.channels.length; i++) {
            Channel c = l.channels[i];
            int st = d.pos;
            d.need(c.length);
            int cw = w;
            int ch = h;
            if (c.id == -2 || c.id == -3) {
                cw = mw;
                ch = mh;
            }
            byte[] plane = null;
            if (c.length >= 2 && cw > 0 && ch > 0 && (long) cw * ch <= Raster.MAX_PIXELS
                    && (hasImage || c.id < -1)) {
                plane = channel(data, d.pos, d.pos + (int) c.length, cw, ch);
            }
            d.pos = st + (int) c.length;
            if (plane == null) {
                continue;
            }
            if (c.id == -1) {
                alpha = plane;
            } else if (c.id == -2) {
                if (!l.maskDisabled) {
                    l.mask = plane;
                }
            } else if (c.id >= 0 && c.id < 4) {
                planes[c.id] = plane;
            }
        }
        if (!hasImage) {
            return;
        }
        int n = w * h;
        int[] px = new int[n];
        toArgb(planes, alpha, n, px, planeIndex);
        l.pixels = px;
    }

    private void toArgb(byte[][] pl, byte[] alpha, int n, int[] px, int[] unused) {
        for (int i = 0; i < n; i++) {
            int a = alpha == null ? 255 : alpha[i] & 0xff;
            int r;
            int g;
            int b;
            if (mode == MODE_RGB) {
                r = pl[0] == null ? 0 : pl[0][i] & 0xff;
                g = pl[1] == null ? 0 : pl[1][i] & 0xff;
                b = pl[2] == null ? 0 : pl[2][i] & 0xff;
            } else if (mode == MODE_CMYK) {
                int c = pl[0] == null ? 255 : pl[0][i] & 0xff;
                int m = pl[1] == null ? 255 : pl[1][i] & 0xff;
                int y = pl[2] == null ? 255 : pl[2][i] & 0xff;
                int k = pl[3] == null ? 255 : pl[3][i] & 0xff;
                // PSD stores CMYK inverted: 255 is no ink.
                r = c * k / 255;
                g = m * k / 255;
                b = y * k / 255;
            } else if (mode == MODE_LAB) {
                double ll = (pl[0] == null ? 0 : pl[0][i] & 0xff) * 100.0 / 255;
                double aa = (pl[1] == null ? 128 : pl[1][i] & 0xff) - 128.0;
                double bb = (pl[2] == null ? 128 : pl[2][i] & 0xff) - 128.0;
                int rgb = lab(ll, aa, bb);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = rgb & 0xff;
            } else if (mode == MODE_INDEXED && palette != null) {
                int idx = pl[0] == null ? 0 : pl[0][i] & 0xff;
                r = palette[idx] & 0xff;
                g = palette[256 + idx] & 0xff;
                b = palette[512 + idx] & 0xff;
            } else {
                r = pl[0] == null ? 0 : pl[0][i] & 0xff;
                g = r;
                b = r;
            }
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /// Decodes one channel to 8 bits per pixel.
    private byte[] channel(byte[] data, int start, int end, int w, int h) throws IOException {
        PsdData p = new PsdData(data, start, end);
        int comp = p.u16();
        return decode(p, comp, w, h);
    }

    private byte[] decode(PsdData p, int comp, int w, int h) throws IOException {
        int bpr = depth == 1 ? (w + 7) / 8 : w * (depth / 8);
        long rawLen = (long) bpr * h;
        if (rawLen > Integer.MAX_VALUE / 2) {
            throw new IOException("PSD channel is too large");
        }
        byte[] raw;
        if (comp == 0) {
            p.need(rawLen);
            raw = new byte[(int) rawLen];
            System.arraycopy(p.b, p.pos, raw, 0, raw.length);
            p.pos += raw.length;
        } else if (comp == 1) {
            raw = new byte[(int) rawLen];
            int[] counts = new int[h];
            for (int y = 0; y < h; y++) {
                counts[y] = big ? p.s32() : p.u16();
            }
            for (int y = 0; y < h; y++) {
                int rowEnd = p.pos + counts[y];
                p.need(counts[y]);
                packBits(p.b, p.pos, rowEnd, raw, y * bpr, bpr);
                p.pos = rowEnd;
            }
        } else if (comp == 2 || comp == 3) {
            byte[] z = new byte[p.end - p.pos];
            System.arraycopy(p.b, p.pos, z, 0, z.length);
            // Only rawLen bytes are kept, so inflating stops there: a small deflate bomb per
            // channel must not cost the decoder's general limit each time.
            byte[] inf = PngDecoder.inflate(z, (int) rawLen);
            raw = new byte[(int) rawLen];
            System.arraycopy(inf, 0, raw, 0, Math.min(inf.length, raw.length));
            if (comp == 3) {
                unpredict(raw, w, h, bpr);
            }
        } else {
            throw new IOException("Unknown PSD compression " + comp);
        }
        return to8(raw, w, h, bpr);
    }

    private void unpredict(byte[] raw, int w, int h, int bpr) {
        if (depth == 8) {
            for (int y = 0; y < h; y++) {
                int o = y * bpr;
                for (int x = 1; x < w; x++) {
                    raw[o + x] = (byte) (raw[o + x] + raw[o + x - 1]);
                }
            }
        } else if (depth == 16) {
            for (int y = 0; y < h; y++) {
                int o = y * bpr;
                int prev = 0;
                for (int x = 0; x < w; x++) {
                    int v = (((raw[o + 2 * x] & 0xff) << 8) | (raw[o + 2 * x + 1] & 0xff)) + prev;
                    v &= 0xffff;
                    raw[o + 2 * x] = (byte) (v >> 8);
                    raw[o + 2 * x + 1] = (byte) v;
                    prev = v;
                }
            }
        } else if (depth == 32) {
            // 32-bit prediction: byte deltas over the row, then the bytes of each value are
            // stored planar (all first bytes, then all second bytes...).
            byte[] row = new byte[bpr];
            for (int y = 0; y < h; y++) {
                int o = y * bpr;
                for (int i = 1; i < bpr; i++) {
                    raw[o + i] = (byte) (raw[o + i] + raw[o + i - 1]);
                }
                for (int x = 0; x < w; x++) {
                    for (int k = 0; k < 4; k++) {
                        row[x * 4 + k] = raw[o + k * w + x];
                    }
                }
                System.arraycopy(row, 0, raw, o, bpr);
            }
        }
    }

    private byte[] to8(byte[] raw, int w, int h, int bpr) {
        if (depth == 8) {
            return raw;
        }
        byte[] out = new byte[w * h];
        for (int y = 0; y < h; y++) {
            int o = y * bpr;
            for (int x = 0; x < w; x++) {
                int v;
                if (depth == 1) {
                    v = (raw[o + (x >> 3)] >> (7 - (x & 7)) & 1) != 0 ? 0 : 255;
                } else if (depth == 16) {
                    v = raw[o + 2 * x] & 0xff;
                } else {
                    int bits = ((raw[o + 4 * x] & 0xff) << 24) | ((raw[o + 4 * x + 1] & 0xff) << 16)
                            | ((raw[o + 4 * x + 2] & 0xff) << 8) | (raw[o + 4 * x + 3] & 0xff);
                    float f = Float.intBitsToFloat(bits);
                    // 32-bit documents are linear light; show them through the sRGB curve.
                    double s = f <= 0 ? 0 : (f >= 1 ? 1 : (f <= 0.0031308 ? 12.92 * f : 1.055 * MathUtil.pow(f, 1 / 2.4) - 0.055));
                    v = (int) Math.round(s * 255);
                }
                out[y * w + x] = (byte) v;
            }
        }
        return out;
    }

    /// Unpacks PackBits: a header n of 0..127 copies n+1 bytes, -1..-127 repeats the next
    /// byte 1-n times, and -128 is skipped. Not shared with the PDF RunLengthDecode filter:
    /// there -128 ends the data, and a PSD row is bounded by its stored byte count instead.
    static void packBits(byte[] src, int pos, int end, byte[] dst, int off, int len) {
        int o = off;
        int lim = off + len;
        while (pos < end && o < lim) {
            int n = src[pos++];
            if (n >= 0) {
                int c = Math.min(n + 1, Math.min(end - pos, lim - o));
                System.arraycopy(src, pos, dst, o, c);
                pos += n + 1;
                o += c;
            } else if (n != -128) {
                if (pos >= end) {
                    return;
                }
                byte v = src[pos++];
                int c = Math.min(1 - n, lim - o);
                for (int i = 0; i < c; i++) {
                    dst[o++] = v;
                }
            }
        }
    }

    private int[] readMerged(int channels) throws IOException {
        int comp = d.u16();
        int n = width * height;
        int bpr = depth == 1 ? (width + 7) / 8 : width * (depth / 8);
        byte[][] planes = new byte[5][];
        byte[] alpha = null;
        int used = Math.min(channels, mode == MODE_CMYK ? 5 : (mode == MODE_RGB || mode == MODE_LAB ? 4 : 2));
        if (comp == 1) {
            int[] counts = new int[channels * height];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = big ? d.s32() : d.u16();
            }
            for (int c = 0; c < channels; c++) {
                byte[] raw = new byte[bpr * height];
                for (int y = 0; y < height; y++) {
                    int cnt = counts[c * height + y];
                    d.need(cnt);
                    packBits(d.b, d.pos, d.pos + cnt, raw, y * bpr, bpr);
                    d.pos += cnt;
                }
                assign(planes, c, to8(raw, width, height, bpr));
            }
        } else if (comp == 0) {
            for (int c = 0; c < channels; c++) {
                d.need((long) bpr * height);
                byte[] raw = new byte[bpr * height];
                System.arraycopy(d.b, d.pos, raw, 0, raw.length);
                d.pos += raw.length;
                assign(planes, c, to8(raw, width, height, bpr));
            }
        } else {
            for (int c = 0; c < channels && d.pos < d.end; c++) {
                assign(planes, c, decode(d, comp, width, height));
            }
        }
        int colorCh = mode == MODE_CMYK ? 4 : (mode == MODE_RGB || mode == MODE_LAB ? 3 : 1);
        if (used > colorCh) {
            alpha = planes[colorCh];
        }
        int[] px = new int[n];
        toArgb(planes, mode == MODE_RGB || mode == MODE_GRAY ? alpha : null, n, px, null);
        return px;
    }

    private static void assign(byte[][] planes, int c, byte[] v) {
        if (c < planes.length) {
            planes[c] = v;
        }
    }

    private void build(List<Layer> layers, GroupNode root) {
        List<GroupNode> stack = new ArrayList<GroupNode>();
        stack.add(root);
        // Records are stored bottom to top; a group is a divider (section 3), its children,
        // then the folder record (section 1 or 2).
        for (Layer l : layers) {
            GroupNode parent = stack.get(stack.size() - 1);
            if (l.section == 3) {
                GroupNode g = new GroupNode();
                stack.add(g);
                continue;
            }
            ArtNode node;
            if (l.section == 1 || l.section == 2) {
                if (stack.size() > 1) {
                    GroupNode g = stack.remove(stack.size() - 1);
                    parent = stack.get(stack.size() - 1);
                    String bk = l.sectionBlend != null ? l.sectionBlend : l.blend;
                    g.setPassThrough("pass".equals(bk));
                    if (!"pass".equals(bk)) {
                        g.setBlendMode(blend(bk));
                    }
                    g.setKnockout(l.knockout);
                    node = g;
                } else {
                    continue;
                }
            } else {
                node = leaf(l);
                if (!(node instanceof GroupNode) && l.section != 1 && l.section != 2) {
                    node.setBlendMode(blend(l.blend));
                }
                node.setKnockout(l.knockout);
            }
            node.setName(l.text != null && l.name == null ? l.text : l.name);
            node.setOpacity(l.opacity / 255f);
            node.setVisible((l.flags & 2) == 0);
            applyMasks(l, node);
            node.getEffects().addAll(l.effects);
            if (l.clipping && !parent.getChildren().isEmpty()) {
                clipTo(parent, node);
            } else {
                parent.add(node);
            }
        }
        while (stack.size() > 1) {
            GroupNode g = stack.remove(stack.size() - 1);
            stack.get(stack.size() - 1).add(g);
        }
    }

    /// A clipped layer is masked by the alpha of the layer below it: the base and every
    /// clipped layer above it go into a group, and the clipped layers into an isolated
    /// group masked by the base's alpha.
    private static void clipTo(GroupNode parent, ArtNode node) {
        List<ArtNode> ch = parent.getChildren();
        ArtNode last = ch.get(ch.size() - 1);
        GroupNode clipSet;
        if (last instanceof GroupNode && "clipping".equals(last.getId())) {
            clipSet = (GroupNode) last;
        } else {
            ch.remove(ch.size() - 1);
            clipSet = new GroupNode();
            clipSet.setId("clipping");
            clipSet.setName(last.getName());
            clipSet.add(last);
            GroupNode clipped = new GroupNode();
            clipped.setPassThrough(true);
            ArtNode baseMask = last.copy();
            baseMask.getEffects().clear();
            baseMask.setOpacity(1);
            baseMask.setBlendMode(BlendMode.NORMAL);
            clipped.setMask(baseMask, false);
            clipSet.add(clipped);
            parent.add(clipSet);
        }
        GroupNode clipped = (GroupNode) clipSet.getChildren().get(clipSet.getChildren().size() - 1);
        clipped.add(node);
    }

    private ArtNode leaf(Layer l) {
        float fill = l.fillOpacity;
        if (l.adjustment != null) {
            AdjustmentNode a = new AdjustmentNode(l.adjustment);
            if (fill < 1) {
                a.setOpacity(fill);
            }
            return a;
        }
        if (l.fillLayer == null && l.patternId != null) {
            Raster tile = patterns.get(l.patternId);
            if (tile == null) {
                note(l.name, "pattern " + l.patternId + " is not in the file; shown as its stored pixels");
            } else {
                ImagePaint ip = new ImagePaint(tile);
                ip.setTiled(true);
                ip.setTransform(Matrix.scale(l.patternScale, l.patternScale));
                l.fillLayer = ip;
            }
        }
        if (l.fillLayer != null) {
            ShapeNode s = ShapeNode.rect(0, 0, width, height, 0);
            l.fillLayer.setOpacity(fill);
            s.fill(l.fillLayer);
            return s;
        }
        if (l.pixels == null) {
            if (l.text != null || l.vectorMask != null) {
                return new GroupNode();
            }
            return new GroupNode();
        }
        Raster r = new Raster(l.left, l.top, l.right - l.left, l.bottom - l.top, l.pixels);
        if (fill < 1) {
            // Fill opacity fades the pixels but not the layer's styles.
            int[] p = r.getPixels();
            for (int i = 0; i < p.length; i++) {
                int a = Math.round((p[i] >>> 24) * fill);
                p[i] = (a << 24) | (p[i] & 0xffffff);
            }
        }
        RasterNode n = new RasterNode(r);
        if (l.text != null) {
            n.setId(null);
            note(l.name == null ? "text" : l.name, "text layer shown as its stored pixels");
        }
        return n;
    }

    private void applyMasks(Layer l, ArtNode node) {
        if (l.vectorMask != null) {
            node.setClipPath(l.vectorMask);
        }
        if (l.mask != null) {
            int mw = l.maskRight - l.maskLeft;
            int mh = l.maskBottom - l.maskTop;
            int[] px = new int[mw * mh];
            for (int i = 0; i < px.length; i++) {
                int v = l.mask[i] & 0xff;
                px[i] = 0xff000000 | (v << 16) | (v << 8) | v;
            }
            GroupNode m = new GroupNode();
            int dv = l.maskDefault;
            ShapeNode bg = ShapeNode.rect(-MAX_DIMENSION, -MAX_DIMENSION, 3.0 * MAX_DIMENSION, 3.0 * MAX_DIMENSION, 0);
            bg.fill(new SolidPaint(0xff000000 | (dv << 16) | (dv << 8) | dv));
            m.add(bg);
            m.add(new RasterNode(new Raster(l.maskLeft, l.maskTop, mw, mh, px)));
            node.setMask(m, true);
        }
    }

    static BlendMode blend(String k) {
        String[] keys = {"norm", "diss", "dark", "mul ", "idiv", "lbrn", "dkCl", "lite", "scrn", "div ", "lddg",
            "lgCl", "over", "sLit", "hLit", "vLit", "lLit", "pLit", "hMix", "diff", "smud", "fsub", "fdiv",
            "hue ", "sat ", "colr", "lum "};
        BlendMode[] modes = {BlendMode.NORMAL, BlendMode.DISSOLVE, BlendMode.DARKEN, BlendMode.MULTIPLY,
            BlendMode.COLOR_BURN, BlendMode.LINEAR_BURN, BlendMode.DARKER_COLOR, BlendMode.LIGHTEN, BlendMode.SCREEN,
            BlendMode.COLOR_DODGE, BlendMode.LINEAR_DODGE, BlendMode.LIGHTER_COLOR, BlendMode.OVERLAY,
            BlendMode.SOFT_LIGHT, BlendMode.HARD_LIGHT, BlendMode.VIVID_LIGHT, BlendMode.LINEAR_LIGHT,
            BlendMode.PIN_LIGHT, BlendMode.HARD_MIX, BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.SUBTRACT,
            BlendMode.DIVIDE, BlendMode.HUE, BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(k)) {
                return modes[i];
            }
        }
        return BlendMode.NORMAL;
    }

    /// The blend key for a mode, for writing.
    static String key(BlendMode m) {
        String[] keys = {"norm", "mul ", "scrn", "over", "dark", "lite", "div ", "idiv", "hLit", "sLit", "diff",
            "smud", "hue ", "sat ", "colr", "lum ", "lbrn", "lddg", "vLit", "lLit", "pLit", "hMix", "fsub", "fdiv",
            "dkCl", "lgCl", "diss", "lddg", "mul "};
        int i = m.ordinal();
        return i < keys.length ? keys[i] : "norm";
    }

    static BlendMode descriptorBlend(String md) {
        String[] keys = {"Nrml", "Dslv", "Drkn", "Mltp", "CBrn", "linearBurn", "darkerColor", "Lghn", "Scrn",
            "CDdg", "linearDodge", "lighterColor", "Ovrl", "SftL", "HrdL", "vividLight", "linearLight", "pinLight",
            "hardMix", "Dfrn", "Xclu", "blendSubtraction", "blendDivide", "H   ", "Strt", "Clr ", "Lmns"};
        BlendMode[] modes = {BlendMode.NORMAL, BlendMode.DISSOLVE, BlendMode.DARKEN, BlendMode.MULTIPLY,
            BlendMode.COLOR_BURN, BlendMode.LINEAR_BURN, BlendMode.DARKER_COLOR, BlendMode.LIGHTEN, BlendMode.SCREEN,
            BlendMode.COLOR_DODGE, BlendMode.LINEAR_DODGE, BlendMode.LIGHTER_COLOR, BlendMode.OVERLAY,
            BlendMode.SOFT_LIGHT, BlendMode.HARD_LIGHT, BlendMode.VIVID_LIGHT, BlendMode.LINEAR_LIGHT,
            BlendMode.PIN_LIGHT, BlendMode.HARD_MIX, BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.SUBTRACT,
            BlendMode.DIVIDE, BlendMode.HUE, BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(md)) {
                return modes[i];
            }
        }
        return BlendMode.NORMAL;
    }
}
