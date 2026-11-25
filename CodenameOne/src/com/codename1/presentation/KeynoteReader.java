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
package com.codename1.presentation;

import com.codename1.impl.office.Protobuf;
import com.codename1.impl.office.Snappy;
import com.codename1.impl.office.ZipParts;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Reads a modern Keynote package: a zip of `Index/*.iwa` object archives, `Data/` media and
/// preview images.
///
/// Each `.iwa` is Snappy compressed in Apple's chunk framing. Decompressed, it is a run of
/// length-prefixed `ArchiveInfo` messages, each followed by the payloads it describes. Every
/// payload is a protobuf message with an object identifier and a type number; objects name
/// each other by identifier. The field and type numbers used here were written from public
/// descriptions of the format and cover the common archives only: geometry, shape fill and
/// stroke, text with bold, italic, size, font and colour, images, placeholders and notes.
/// A slide that yields nothing is shown as its thumbnail picture, with a warning, rather than
/// left blank.
final class KeynoteReader {
    private static final int MAX_OBJECTS = 200000;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_SLIDES = 5000;
    private static final char PARAGRAPH_SEPARATOR = (char) 0x2029;

    private static final int T_DOCUMENT = 1;
    private static final int T_SHOW = 2;
    private static final int T_SLIDE_NODE = 4;
    private static final int T_SLIDE = 5;
    private static final int T_PLACEHOLDER = 7;
    private static final int T_NOTE = 15;
    private static final int T_STORAGE = 2001;
    private static final int T_SHAPE_INFO = 2011;
    private static final int T_CHAR_STYLE = 2021;
    private static final int T_SHAPE = 3004;
    private static final int T_IMAGE = 3005;
    private static final int T_SHAPE_STYLE = 3016;
    private static final int T_TEXT_SHAPE_STYLE = 2025;
    private static final int T_CONNECTION_LINE = 3009;
    private static final int T_PACKAGE_METADATA = 11006;

    private final Map<Long, Integer> types = new HashMap<Long, Integer>();
    private final Map<Long, byte[]> payloads = new HashMap<Long, byte[]>();
    private final Map<Long, String> dataNames = new HashMap<Long, String>();
    private final List<String> warnings = new ArrayList<String>();
    private final Set<String> warned = new HashSet<String>();
    private Map<String, byte[]> parts;
    private int counter;

    private KeynoteReader() {
    }

    static Presentation read(InputStream in) throws IOException {
        return new KeynoteReader().readPackage(in);
    }

    private void warn(String w) {
        if (warned.add(w)) {
            warnings.add(w);
        }
    }

    private Presentation readPackage(InputStream in) throws IOException {
        parts = ZipParts.read(in, "Keynote");
        boolean iwa = false;
        Iterator<String> names = parts.keySet().iterator();
        while (names.hasNext()) {
            String n = names.next();
            if (n.endsWith(".iwph")) {
                throw new IOException("This Keynote presentation is encrypted");
            }
            if ("index.apxl".equals(n) || n.endsWith("/index.apxl")
                    || "index.apxl.gz".equals(n)) {
                throw new IOException("Keynote 09 format not supported");
            }
            if (n.startsWith("Index/") && n.endsWith(".iwa")) {
                iwa = true;
            }
        }
        if (!iwa) {
            throw new IOException("Not a Keynote package: no Index/*.iwa archives");
        }
        // one budget for every archive: Snappy expands about twenty times, so a cap per
        // archive would let a package of many archives unpack to gigabytes
        int budget = Snappy.DEFAULT_LIMIT;
        Iterator<Map.Entry<String, byte[]>> entries = parts.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, byte[]> e = entries.next();
            String n = e.getKey();
            if (n.startsWith("Index/") && n.endsWith(".iwa")) {
                byte[] raw = Snappy.decompressIwa(e.getValue(), budget);
                budget -= raw.length;
                readArchive(raw);
            }
        }
        readMetadata();
        return build();
    }

    private void readArchive(byte[] raw) throws IOException {
        int[] at = new int[1];
        while (at[0] < raw.length) {
            long infoLen = Protobuf.readVarint(raw, at, raw.length);
            if (infoLen < 0 || infoLen > raw.length - at[0]) {
                throw new IOException("Damaged Keynote archive: bad header length");
            }
            Protobuf info = new Protobuf(raw, at[0], (int) infoLen);
            at[0] += (int) infoLen;
            long id = -1;
            List<long[]> msgs = new ArrayList<long[]>();
            while (info.next()) {
                if (info.field() == 1 && info.wireType() == Protobuf.VARINT) {
                    id = info.varint();
                } else if (info.field() == 2 && info.wireType() == Protobuf.BYTES) {
                    Protobuf mi = info.message();
                    long type = 0;
                    long len = 0;
                    while (mi.next()) {
                        if (mi.wireType() == Protobuf.VARINT) {
                            if (mi.field() == 1) {
                                type = mi.varint();
                            } else if (mi.field() == 3) {
                                len = mi.varint();
                            }
                        }
                    }
                    msgs.add(new long[] {type, len});
                }
            }
            for (int i = 0; i < msgs.size(); i++) {
                long[] m = msgs.get(i);
                if (m[1] < 0 || m[1] > raw.length - at[0]) {
                    throw new IOException("Damaged Keynote archive: bad object length");
                }
                int len = (int) m[1];
                if (i == 0 && id >= 0) {
                    if (types.size() >= MAX_OBJECTS) {
                        throw new IOException("Keynote package has too many objects");
                    }
                    byte[] b = new byte[len];
                    System.arraycopy(raw, at[0], b, 0, len);
                    Long key = Long.valueOf(id);
                    types.put(key, Integer.valueOf((int) m[0]));
                    payloads.put(key, b);
                }
                at[0] += len;
            }
        }
    }

    private void readMetadata() throws IOException {
        Iterator<Map.Entry<Long, Integer>> it = types.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Integer> e = it.next();
            if (e.getValue().intValue() != T_PACKAGE_METADATA) {
                continue;
            }
            Protobuf p = msg(payloads.get(e.getKey()));
            while (p.next()) {
                if (p.field() == 3 && p.wireType() == Protobuf.BYTES) {
                    readDataInfo(p.message());
                }
            }
        }
    }

    private void readDataInfo(Protobuf d) throws IOException {
        long id = -1;
        String preferred = null;
        String file = null;
        while (d.next()) {
            if (d.field() == 1 && d.wireType() == Protobuf.VARINT) {
                id = d.varint();
            } else if (d.field() == 3 && d.wireType() == Protobuf.BYTES) {
                preferred = d.string();
            } else if (d.field() == 4 && d.wireType() == Protobuf.BYTES) {
                file = d.string();
            }
        }
        if (id >= 0) {
            dataNames.put(Long.valueOf(id), file != null ? file : preferred);
        }
    }

    private static Protobuf msg(byte[] b) {
        return new Protobuf(b, 0, b.length);
    }

    private static Protobuf empty() {
        return new Protobuf(new byte[0], 0, 0);
    }

    private Protobuf object(long id, int type) {
        if (typeOf(id) != type) {
            return null;
        }
        return msg(payloads.get(Long.valueOf(id)));
    }

    private int typeOf(long id) {
        Integer t = types.get(Long.valueOf(id));
        return t == null ? -1 : t.intValue();
    }

    /// The identifier inside a `TSP.Reference` at the reader's current field, or -1.
    private static long ref(Protobuf p) throws IOException {
        if (p.wireType() != Protobuf.BYTES) {
            return -1;
        }
        Protobuf r = p.message();
        while (r.next()) {
            if (r.field() == 1 && r.wireType() == Protobuf.VARINT) {
                return r.varint();
            }
        }
        return -1;
    }

    /// The first nested message with this field number, or null.
    private static Protobuf sub(Protobuf p, int field) throws IOException {
        while (p.next()) {
            if (p.field() == field && p.wireType() == Protobuf.BYTES) {
                return p.message();
            }
        }
        return null;
    }

    private String nextId(String prefix) {
        counter++;
        return prefix + counter;
    }

    private Protobuf findShow() throws IOException {
        long docId = findFirst(T_DOCUMENT);
        if (docId >= 0) {
            Protobuf doc = object(docId, T_DOCUMENT);
            while (doc.next()) {
                if (doc.field() == 2) {
                    Protobuf show = object(ref(doc), T_SHOW);
                    if (show != null) {
                        return show;
                    }
                }
            }
        }
        long s = findFirst(T_SHOW);
        return s >= 0 ? object(s, T_SHOW) : null;
    }

    private Presentation build() throws IOException {
        float width = 1024;
        float height = 768;
        List<Long> slideNodes = new ArrayList<Long>();
        Protobuf show = findShow();
        if (show == null) {
            warn("No Keynote show archive found");
        } else {
            while (show.next()) {
                if (show.field() == 4 && show.wireType() == Protobuf.BYTES) {
                    float[] wh = pair(show.message());
                    if (wh[0] > 0 && wh[1] > 0 && wh[0] <= 100000 && wh[1] <= 100000) {
                        width = wh[0];
                        height = wh[1];
                    }
                } else if (show.field() == 3 && show.wireType() == Protobuf.BYTES) {
                    // KN.ShowArchive.slideTree; the tree's slides are field 2 (a flat list in
                    // every current file; older ones nest children under each node)
                    Protobuf tree = show.message();
                    Set<Long> seen = new HashSet<Long>();
                    while (tree.next()) {
                        if (tree.field() == 2 && tree.wireType() == Protobuf.BYTES) {
                            collectNodes(ref(tree), 0, seen, slideNodes);
                        }
                    }
                }
            }
        }
        List<String> thumbs = thumbnails();
        List<Slide> slides = new ArrayList<Slide>();
        for (int i = 0; i < slideNodes.size(); i++) {
            slides.add(readSlideNode(slideNodes.get(i).longValue(), i, width, height, thumbs));
        }
        if (slides.isEmpty()) {
            slides.add(fallback(new Slide(nextId("s")), 0, width, height, thumbs));
        }
        return new Presentation(width, height, slides, null, null, counter + 1L,
                warnings.isEmpty() ? null : warnings);
    }

    private long findFirst(int type) {
        long best = -1;
        Iterator<Map.Entry<Long, Integer>> it = types.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Integer> e = it.next();
            if (e.getValue().intValue() == type) {
                long k = e.getKey().longValue();
                if (best < 0 || k < best) {
                    best = k;
                }
            }
        }
        return best;
    }

    /// Walks the slide tree depth first; a node's own slide comes before its children's.
    private void collectNodes(long id, int depth, Set<Long> seen, List<Long> out)
            throws IOException {
        if (id < 0 || out.size() >= MAX_SLIDES) {
            return;
        }
        if (depth > MAX_DEPTH) {
            warn("Keynote slide tree is nested too deeply; deeper slides were skipped");
            return;
        }
        if (!seen.add(Long.valueOf(id))) {
            warn("Keynote slide tree refers to a slide twice; the repeat was skipped");
            return;
        }
        Protobuf n = object(id, T_SLIDE_NODE);
        if (n == null) {
            return;
        }
        List<Long> children = new ArrayList<Long>();
        boolean hasSlide = false;
        while (n.next()) {
            if (n.field() == 1) {
                children.add(Long.valueOf(ref(n)));
            } else if (n.field() == 2) {
                hasSlide = true;
            }
        }
        if (hasSlide) {
            out.add(Long.valueOf(id));
        }
        for (Long child : children) {
            collectNodes(child.longValue(), depth + 1, seen, out);
        }
    }

    private Slide readSlideNode(long nodeId, int index, float width, float height,
            List<String> thumbs) throws IOException {
        Protobuf n = object(nodeId, T_SLIDE_NODE);
        long slideId = -1;
        boolean hidden = false;
        while (n.next()) {
            if (n.field() == 2) {
                slideId = ref(n);
            } else if (n.field() == 4 && n.wireType() == Protobuf.VARINT) {
                hidden = n.varint() != 0;
            }
        }
        String sid = nextId("s");
        List<SlideElement> elements = new ArrayList<SlideElement>();
        List<Block> notes = null;
        Protobuf s = object(slideId, T_SLIDE);
        if (s != null) {
            List<long[]> refs = new ArrayList<long[]>();
            long note = -1;
            while (s.next()) {
                int f = s.field();
                // 5 title placeholder, 6 body placeholder, 7 the slide's own drawables (42
                // repeats them in z-order)
                if (f == 5 || f == 6 || f == 7) {
                    long r = ref(s);
                    if (r >= 0) {
                        refs.add(new long[] {r, f});
                    }
                } else if (f == 27) {
                    note = ref(s);
                }
            }
            Set<Long> done = new HashSet<Long>();
            for (long[] r : refs) {
                if (done.add(Long.valueOf(r[0]))) {
                    String role = null;
                    if (r[1] == 5) {
                        role = ShapeElement.TITLE;
                    } else if (r[1] == 6) {
                        role = ShapeElement.BODY;
                    }
                    SlideElement e = drawable(r[0], role);
                    if (e != null) {
                        elements.add(e);
                    }
                }
            }
            notes = readNote(note);
        }
        Slide slide = new Slide(sid, null, null, elements, notes, null, null, hidden);
        if (elements.isEmpty()) {
            slide = fallback(slide, index, width, height, thumbs);
        }
        return slide;
    }

    private List<Block> readNote(long noteId) throws IOException {
        Protobuf n = object(noteId, T_NOTE);
        if (n == null) {
            return null;
        }
        while (n.next()) {
            if (n.field() == 1) {
                TextFrame t = storage(ref(n));
                return t == null ? null : t.blocks;
            }
        }
        return null;
    }

    private Slide fallback(Slide slide, int index, float width, float height,
            List<String> thumbs) {
        String name = null;
        if (index < thumbs.size()) {
            name = thumbs.get(index);
        } else if (index == 0 && parts.containsKey("preview.jpg")) {
            name = "preview.jpg";
        }
        List<SlideElement> l = new ArrayList<SlideElement>();
        String eid = nextId("e");
        if (name == null) {
            warn("Slide " + (index + 1) + " could not be read and has no thumbnail");
            String msg = "Slide " + (index + 1) + " could not be read";
            l.add(ShapeElement.textBox(eid, width / 10, height / 2 - 30, width * 8 / 10, 60,
                    TextFrame.of(eid + "t", msg, TextStyle.DEFAULT, "center")));
        } else {
            warn("Slide " + (index + 1) + " could not be read; showing its thumbnail");
            Picture pic = new Picture(eid + "p", parts.get(name), mime(name), width, height,
                    null);
            l.add(new PictureElement(eid, pic, 0, 0, width, height));
        }
        return slide.withElements(l);
    }

    private List<String> thumbnails() {
        List<String> st = new ArrayList<String>();
        List<String> mt = new ArrayList<String>();
        Iterator<String> it = parts.keySet().iterator();
        while (it.hasNext()) {
            String n = it.next();
            if (n.startsWith("Data/st-")) {
                st.add(n);
            } else if (n.startsWith("Data/mt-")) {
                mt.add(n);
            }
        }
        Collections.sort(st);
        Collections.sort(mt);
        return st.isEmpty() ? mt : st;
    }

    private static String mime(String name) {
        if (name.endsWith(".png") || name.endsWith(".PNG")) {
            return "image/png";
        }
        if (name.endsWith(".gif") || name.endsWith(".GIF")) {
            return "image/gif";
        }
        if (name.endsWith(".tif") || name.endsWith(".tiff")) {
            return "image/tiff";
        }
        return "image/jpeg";
    }

    /// Reads one drawable. Unknown kinds are warned about and skipped.
    private SlideElement drawable(long id, String role) throws IOException {
        int t = typeOf(id);
        if (t == T_PLACEHOLDER) {
            Protobuf info = sub(object(id, T_PLACEHOLDER), 1);
            return info == null ? null : shapeInfo(info, role);
        }
        if (t == T_SHAPE_INFO) {
            return shapeInfo(object(id, T_SHAPE_INFO), role);
        }
        if (t == T_SHAPE) {
            return shape(object(id, T_SHAPE), null, role);
        }
        if (t == T_IMAGE) {
            return image(object(id, T_IMAGE));
        }
        if (t == T_CONNECTION_LINE) {
            Protobuf line = sub(object(id, T_CONNECTION_LINE), 1);
            return line == null ? null : shape(line, null, null);
        }
        if (t >= 0) {
            warn("Keynote object type " + t + " is not supported and was skipped");
        }
        return null;
    }

    private SlideElement shapeInfo(Protobuf p, String role) throws IOException {
        Protobuf shape = null;
        long storage = -1;
        while (p.next()) {
            if (p.field() == 1 && p.wireType() == Protobuf.BYTES) {
                shape = p.message();
            } else if (p.field() == 2) {
                storage = ref(p);
            }
        }
        if (shape == null) {
            return null;
        }
        return shape(shape, storage(storage), role);
    }

    /// Geometry of a `TSD.DrawableArchive`: x, y, width, height and angle.
    private static float[] geometry(Protobuf drawable) throws IOException {
        float[] g = new float[] {0, 0, 100, 100, 0};
        Protobuf geo = sub(drawable, 1);
        if (geo == null) {
            return g;
        }
        while (geo.next()) {
            if (geo.field() == 1 && geo.wireType() == Protobuf.BYTES) {
                float[] xy = pair(geo.message());
                g[0] = xy[0];
                g[1] = xy[1];
            } else if (geo.field() == 2 && geo.wireType() == Protobuf.BYTES) {
                float[] wh = pair(geo.message());
                g[2] = wh[0];
                g[3] = wh[1];
            } else if (geo.field() == 4 && geo.wireType() == Protobuf.FIXED32) {
                g[4] = geo.float32();
            }
        }
        for (int i = 0; i < 5; i++) {
            if (Float.isNaN(g[i]) || Math.abs(g[i]) > 100000) {
                g[i] = i == 2 || i == 3 ? 100 : 0;
            }
        }
        if (g[2] <= 0) {
            g[2] = 1;
        }
        if (g[3] <= 0) {
            g[3] = 1;
        }
        return g;
    }

    /// Two floats in fields 1 and 2: a point or a size.
    private static float[] pair(Protobuf p) throws IOException {
        float[] r = new float[2];
        while (p.next()) {
            if (p.wireType() == Protobuf.FIXED32 && (p.field() == 1 || p.field() == 2)) {
                r[p.field() - 1] = p.float32();
            }
        }
        return r;
    }

    private SlideElement shape(Protobuf p, TextFrame text, String role) throws IOException {
        Protobuf drawable = empty();
        long style = -1;
        String geometry = "rect";
        float[] ends = null;
        while (p.next()) {
            if (p.field() == 1 && p.wireType() == Protobuf.BYTES) {
                drawable = p.message();
            } else if (p.field() == 2) {
                style = ref(p);
            } else if (p.field() == 3 && p.wireType() == Protobuf.BYTES) {
                geometry = pathGeometry(p.message());
                ends = connectionEnds(p.message());
            }
        }
        float[] g = geometry(drawable);
        boolean flipV = false;
        if (ends != null) {
            // a connection line: drawn corner to corner of its box, flipped when it rises
            geometry = "line";
            flipV = ends[1] > ends[3] == ends[0] < ends[2];
        }
        Object[] fl = new Object[2];
        shapeStyle(style, fl, 0);
        Fill fill = (Fill) fl[0];
        LineStyle line = (LineStyle) fl[1];
        return new ShapeElement(nextId("e"), null, geometry, g[0], g[1], g[2], g[3], g[4],
                false, flipV, fill, line, text, role, null);
    }

    /// The fill and stroke of a shape style, taken from its parents where it sets none.
    /// A `TSWP.ShapeStyleArchive` (2025, a text box's) wraps the `TSD.ShapeStyleArchive`
    /// (3016) in field 1; the properties are field 11 of the latter and the parent style is
    /// field 3 of its `TSS.StyleArchive` (field 1).
    private void shapeStyle(long style, Object[] out, int depth) throws IOException {
        if (style < 0 || depth > 16 || out[0] != null && out[1] != null) {
            return;
        }
        int t = typeOf(style);
        Protobuf st;
        if (t == T_TEXT_SHAPE_STYLE) {
            st = sub(object(style, T_TEXT_SHAPE_STYLE), 1);
        } else if (t == T_SHAPE_STYLE) {
            st = object(style, T_SHAPE_STYLE);
        } else {
            return;
        }
        if (st == null) {
            return;
        }
        long parent = -1;
        while (st.next()) {
            if (st.field() == 1 && st.wireType() == Protobuf.BYTES) {
                Protobuf base = st.message();
                while (base.next()) {
                    if (base.field() == 3 && base.wireType() == Protobuf.BYTES) {
                        parent = ref(base);
                    }
                }
            } else if (st.field() == 11 && st.wireType() == Protobuf.BYTES) {
                Protobuf props = st.message();
                while (props.next()) {
                    if (props.field() == 1 && props.wireType() == Protobuf.BYTES
                            && out[0] == null) {
                        Protobuf f = sub(props.message(), 1);
                        out[0] = f == null ? Fill.NO_FILL : Fill.solid(color(f));
                    } else if (props.field() == 2 && props.wireType() == Protobuf.BYTES
                            && out[1] == null) {
                        out[1] = stroke(props.message());
                    }
                }
            }
        }
        shapeStyle(parent, out, depth + 1);
    }

    /// The first and last points of a connection line's path (field 7 of a
    /// `TSD.PathSourceArchive`), or null for any other path.
    private static float[] connectionEnds(Protobuf p) throws IOException {
        Protobuf conn = sub(p, 7);
        Protobuf path = conn == null ? null : sub(conn, 1);
        Protobuf bezier = path == null ? null : sub(path, 3);
        if (bezier == null) {
            return null;
        }
        float[] r = null;
        while (bezier.next()) {
            if (bezier.field() == 1 && bezier.wireType() == Protobuf.BYTES) {
                Protobuf el = bezier.message();
                float[] last = null;
                while (el.next()) {
                    if (el.field() == 2 && el.wireType() == Protobuf.BYTES) {
                        last = pair(el.message());
                    }
                }
                if (last != null) {
                    if (r == null) {
                        r = new float[] {last[0], last[1], last[0], last[1]};
                    } else {
                        r[2] = last[0];
                        r[3] = last[1];
                    }
                }
            }
        }
        return r;
    }

    private static LineStyle stroke(Protobuf s) throws IOException {
        int c = 0;
        float w = 1;
        while (s.next()) {
            if (s.field() == 1 && s.wireType() == Protobuf.BYTES) {
                c = color(s.message());
            } else if (s.field() == 2 && s.wireType() == Protobuf.FIXED32) {
                float v = s.float32();
                if (v >= 0 && v < 1000) {
                    w = v;
                }
            }
        }
        return w > 0 ? new LineStyle(c, w) : null;
    }

    /// A `TSD.PathSourceArchive`. A scalar path of type 0 is a (rounded) rectangle, 1 a
    /// polygon, 2 a star; a bezier path is drawn as an ellipse only when it is one, and
    /// anything not recognized as its bounding rectangle.
    private static String pathGeometry(Protobuf p) throws IOException {
        while (p.next()) {
            if (p.field() == 2 && p.wireType() == Protobuf.BYTES) {
                return scalarGeometry(p.message());
            }
            if (p.field() == 1 && p.wireType() == Protobuf.BYTES) {
                Protobuf pp = p.message();
                while (pp.next()) {
                    if (pp.field() == 1 && pp.wireType() == Protobuf.VARINT) {
                        long t = pp.varint();
                        if (t == 1 || t == 10) {
                            return "rightArrow";
                        }
                        if (t == 100) {
                            return "star5";
                        }
                    }
                }
            } else if (p.field() == 3 && p.wireType() == Protobuf.BYTES) {
                return "ellipse";
            }
        }
        return "rect";
    }

    private static String scalarGeometry(Protobuf sc) throws IOException {
        long type = 0;
        float scalar = 0;
        while (sc.next()) {
            if (sc.field() == 1 && sc.wireType() == Protobuf.VARINT) {
                type = sc.varint();
            } else if (sc.field() == 2 && sc.wireType() == Protobuf.FIXED32) {
                scalar = sc.float32();
            }
        }
        if (type == 1) {
            return "triangle";
        }
        if (type == 2) {
            return "star5";
        }
        return scalar > 0 ? "roundRect" : "rect";
    }

    /// A `TSP.Color`: RGB floats in fields 3, 4 and 5.
    private static int color(Protobuf c) throws IOException {
        float[] rgb = new float[3];
        while (c.next()) {
            int f = c.field();
            if (f >= 3 && f <= 5 && c.wireType() == Protobuf.FIXED32) {
                rgb[f - 3] = c.float32();
            }
        }
        int r = 0;
        for (int i = 0; i < 3; i++) {
            float v = rgb[i];
            int b = Float.isNaN(v) ? 0 : (int) (Math.max(0, Math.min(1, v)) * 255 + 0.5f);
            r = r << 8 | b;
        }
        return r;
    }

    private SlideElement image(Protobuf p) throws IOException {
        Protobuf drawable = empty();
        long data = -1;
        while (p.next()) {
            if (p.field() == 1 && p.wireType() == Protobuf.BYTES) {
                drawable = p.message();
            } else if (p.field() == 11) {
                data = ref(p);
            }
        }
        float[] g = geometry(drawable);
        String name = dataFile(data);
        if (name == null) {
            warn("An image's data file is missing from the Keynote package");
            return null;
        }
        String id = nextId("e");
        Picture pic = new Picture(id + "p", parts.get(name), mime(name), g[2], g[3], null);
        return new PictureElement(id, null, pic, g[0], g[1], g[2], g[3], g[4], false, false,
                0, 0, 0, 0, null);
    }

    private String dataFile(long id) {
        if (id < 0) {
            return null;
        }
        String n = dataNames.get(Long.valueOf(id));
        if (n != null && parts.containsKey("Data/" + n)) {
            return "Data/" + n;
        }
        String suffix = "-" + id + ".";
        Iterator<String> it = parts.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.startsWith("Data/") && k.indexOf(suffix) >= 5) {
                return k;
            }
        }
        return null;
    }

    /// A `TSWP.StorageArchive`: the text in field 3, paragraphs broken at U+2029 or a line
    /// break, character style runs from the attribute table in field 8.
    private TextFrame storage(long id) throws IOException {
        Protobuf s = object(id, T_STORAGE);
        if (s == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        List<int[]> runs = new ArrayList<int[]>();
        List<TextStyle> styles = new ArrayList<TextStyle>();
        while (s.next()) {
            if (s.field() == 3 && s.wireType() == Protobuf.BYTES) {
                text.append(s.string());
            } else if (s.field() == 8 && s.wireType() == Protobuf.BYTES) {
                readRuns(s.message(), runs, styles);
            }
        }
        List<Block> blocks = new ArrayList<Block>();
        String all = text.toString();
        int start = 0;
        int len = all.length();
        for (int i = 0; i <= len; i++) {
            char c = i < len ? all.charAt(i) : '\n';
            if (c == '\n' || c == PARAGRAPH_SEPARATOR || c == '\r') {
                RichText.Builder b = new RichText.Builder();
                int pos = start;
                while (pos < i) {
                    int k = styleIndex(runs, pos);
                    int next = i;
                    if (k + 1 < runs.size() && runs.get(k + 1)[0] < next) {
                        next = runs.get(k + 1)[0];
                    }
                    TextStyle ts = k < 0 ? TextStyle.DEFAULT : styles.get(k);
                    b.append(all.substring(pos, next), ts);
                    pos = next;
                }
                blocks.add(new Paragraph(nextId("p"), b.build()));
                start = i + 1;
            }
        }
        return new TextFrame(blocks);
    }

    private void readRuns(Protobuf table, List<int[]> runs, List<TextStyle> styles)
            throws IOException {
        while (table.next()) {
            if (table.field() == 1 && table.wireType() == Protobuf.BYTES) {
                Protobuf e = table.message();
                long at = 0;
                long styleId = -1;
                while (e.next()) {
                    if (e.field() == 1 && e.wireType() == Protobuf.VARINT) {
                        at = e.varint();
                    } else if (e.field() == 2) {
                        styleId = ref(e);
                    }
                }
                int last = runs.isEmpty() ? -1 : runs.get(runs.size() - 1)[0];
                // runs must ascend; an out of order or absurd entry is dropped
                if (at > last && at < Integer.MAX_VALUE) {
                    runs.add(new int[] {(int) at});
                    styles.add(charStyle(styleId));
                }
            }
        }
    }

    private static int styleIndex(List<int[]> runs, int pos) {
        // the last run starting at or before pos; runs are in ascending order, so a binary
        // search keeps a storage of many runs linear overall
        int lo = 0;
        int hi = runs.size() - 1;
        int k = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (runs.get(mid)[0] <= pos) {
                k = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return k;
    }

    /// A `TSWP.CharacterStyleArchive`: properties in field 11 -- bold 1, italic 2, size 3,
    /// font name 5, colour 7.
    private TextStyle charStyle(long id) throws IOException {
        TextStyle t = TextStyle.DEFAULT;
        Protobuf s = object(id, T_CHAR_STYLE);
        Protobuf props = s == null ? null : sub(s, 11);
        if (props == null) {
            return t;
        }
        while (props.next()) {
            int f = props.field();
            int w = props.wireType();
            if (f == 1 && w == Protobuf.VARINT) {
                t = t.withBold(props.varint() != 0);
            } else if (f == 2 && w == Protobuf.VARINT) {
                t = t.withItalic(props.varint() != 0);
            } else if (f == 3 && w == Protobuf.FIXED32) {
                float v = props.float32();
                if (v > 0 && v < 2000) {
                    t = t.withSize(v);
                }
            } else if (f == 5 && w == Protobuf.BYTES) {
                String font = props.string();
                if (font.length() > 0) {
                    t = t.withFont(font);
                }
            } else if (f == 7 && w == Protobuf.BYTES) {
                t = t.withColor(color(props.message()));
            }
        }
        return t;
    }
}
