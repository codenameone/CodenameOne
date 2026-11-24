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

import com.codename1.impl.office.CompoundFile;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads a PowerPoint 97-2003 (`.ppt`) file straight into the presentation model, following
/// the public [MS-PPT] and [MS-ODRAW] specifications.
///
/// The `Current User` stream points at the newest `UserEditAtom`; the chain of edits and
/// their persist directories maps persist ids to offsets in the `PowerPoint Document`
/// stream. Slides are read from their `SlideContainer`s: the OfficeArt drawing gives the
/// shapes, their text comes from the client text boxes, pictures from the BLIP store and
/// the `Pictures` stream. What is not converted is reported as a warning. Every record is
/// checked against its container, and the record count and nesting depth are capped, so a
/// damaged file fails with an `IOException` instead of hanging.
final class PptReader {
    private static final int MAX_INPUT = 256 * 1024 * 1024;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_RECORDS = 2000000;
    private static final int MAX_EDITS = 4096;
    private static final int MAX_SLIDES = 10000;
    private static final int MAX_WARNINGS = 200;
    private static final int MAX_TEXT = 4000000;

    private static final int RT_DOCUMENT = 0x03E8;
    private static final int RT_DOCUMENT_ATOM = 0x03E9;
    private static final int RT_SLIDE = 0x03EE;
    private static final int RT_SLIDE_ATOM = 0x03EF;
    private static final int RT_NOTES = 0x03F0;
    private static final int RT_ENVIRONMENT = 0x03F2;
    private static final int RT_SLIDE_PERSIST = 0x03F3;
    private static final int RT_MAIN_MASTER = 0x03F8;
    private static final int RT_SLIDE_SHOW_INFO = 0x03F9;
    private static final int RT_PP_DRAWING_GROUP = 0x040B;
    private static final int RT_PP_DRAWING = 0x040C;
    private static final int RT_FONT_COLLECTION = 0x07D5;
    private static final int RT_COLOR_SCHEME = 0x07F0;
    private static final int RT_PLACEHOLDER = 0x0BC3;
    private static final int RT_OUTLINE_TEXT_REF = 0x0F9E;
    private static final int RT_TEXT_HEADER = 0x0F9F;
    private static final int RT_TEXT_CHARS = 0x0FA0;
    private static final int RT_STYLE_TEXT_PROP = 0x0FA1;
    private static final int RT_TEXT_BYTES = 0x0FA8;
    private static final int RT_FONT_ENTITY = 0x0FB7;
    private static final int RT_SLIDE_LIST = 0x0FF0;
    private static final int RT_ANIMATION_INFO = 0x1014;
    private static final int RT_USER_EDIT = 0x0FF5;
    private static final int RT_CURRENT_USER = 0x0FF6;
    private static final int RT_PERSIST_DIRECTORY = 0x1772;
    private static final int RT_CRYPT_SESSION = 0x2F14;
    private static final int RT_PROG_TAGS = 0x1388;

    private static final int OA_DGG = 0xF000;
    private static final int OA_BSTORE = 0xF001;
    private static final int OA_DG = 0xF002;
    private static final int OA_SPGR = 0xF003;
    private static final int OA_SP = 0xF004;
    private static final int OA_FBSE = 0xF007;
    private static final int OA_FSPGR = 0xF009;
    private static final int OA_FSP = 0xF00A;
    private static final int OA_FOPT = 0xF00B;
    private static final int OA_CLIENT_TEXTBOX = 0xF00D;
    private static final int OA_CHILD_ANCHOR = 0xF00F;
    private static final int OA_CLIENT_ANCHOR = 0xF010;
    private static final int OA_CLIENT_DATA = 0xF011;

    private static final int HEADER_TOKEN = 0xE391C05F;
    private static final int HEADER_TOKEN_ENCRYPTED = 0xF3D1C4DF;
    private static final int[] DEFAULT_SCHEME = {0xffffff, 0x000000, 0x808080, 0x000000,
        0xbbe0e3, 0x333399, 0x009999, 0x99cc00};

    private final byte[] doc;
    private final byte[] pictures;
    private final List<String> warnings = new ArrayList<String>();
    private final Map<Integer, Integer> persist = new HashMap<Integer, Integer>();
    private final List<String> fonts = new ArrayList<String>();
    private final List<Picture> blips = new ArrayList<Picture>();
    private final Map<Integer, Integer> masters = new HashMap<Integer, Integer>();
    private int records;
    private long seq;
    private int textChars;
    private int[] docScheme = DEFAULT_SCHEME;
    private boolean warnedBuilds;
    private boolean warnedMaster;

    /// One text body: its header type, characters, and the style atom that follows it.
    private static final class Text {
        int type = -1;
        String chars;
        int styleOff = -1;
        int styleLen;
    }

    /// A slide from the slide list: its persist reference and the outline text after it.
    private static final class SlideRef {
        int persistRef;
        final List<Text> texts = new ArrayList<Text>();
    }

    /// Maps raw anchor coordinates to points.
    private static final class Frame {
        final float ox;
        final float oy;
        final float sx;
        final float sy;

        Frame(float ox, float oy, float sx, float sy) {
            this.ox = ox;
            this.oy = oy;
            this.sx = sx;
            this.sy = sy;
        }
    }

    private PptReader(byte[] doc, byte[] pictures) {
        this.doc = doc;
        this.pictures = pictures == null ? new byte[0] : pictures;
    }

    static Presentation read(InputStream in) throws IOException {
        byte[] bytes = readAll(in);
        Map<String, byte[]> streams = CompoundFile.read(bytes);
        if (streams.containsKey("EncryptedSummary")) {
            throw new IOException("encrypted .ppt files are not supported");
        }
        byte[] d = streams.get("PowerPoint Document");
        if (d == null) {
            throw new IOException("Not a PowerPoint document: no PowerPoint Document stream");
        }
        byte[] cu = streams.get("Current User");
        PptReader r = new PptReader(d, streams.get("Pictures"));
        try {
            return r.run(cu);
        } catch (RuntimeException ex) {
            IOException e = new IOException("Unreadable .ppt file: " + ex.getMessage());
            e.initCause(ex);
            throw e;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        while (true) {
            int n = in.read(buf);
            if (n < 0) {
                break;
            }
            total += n;
            if (total > MAX_INPUT) {
                throw new IOException(".ppt file too large");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- records

    private static int u8(byte[] b, int o) {
        return b[o] & 0xff;
    }

    private static int u16(byte[] b, int o) {
        return (b[o] & 0xff) | (b[o + 1] & 0xff) << 8;
    }

    private static int s16(byte[] b, int o) {
        return (short) u16(b, o);
    }

    private static int i32(byte[] b, int o) {
        return (b[o] & 0xff) | (b[o + 1] & 0xff) << 8 | (b[o + 2] & 0xff) << 16 | (b[o + 3] & 0xff) << 24;
    }

    private static IOException damaged(String what) {
        return new IOException("Damaged .ppt file: " + what);
    }

    private static int type(byte[] b, int o) {
        return u16(b, o + 2);
    }

    private static int instance(byte[] b, int o) {
        return u16(b, o) >> 4;
    }

    private static boolean isContainer(byte[] b, int o) {
        return (u16(b, o) & 0xf) == 0xf;
    }

    /// The length of the record at `o`, checked against the end of its container.
    private int len(byte[] b, int o, int end) throws IOException {
        if (o < 0 || o + 8 > end || end > b.length) {
            throw damaged("record header past its container");
        }
        int n = i32(b, o + 4);
        if (n < 0 || n > end - o - 8) {
            throw damaged("record 0x" + Integer.toHexString(type(b, o)) + " runs past its container");
        }
        records++;
        if (records > MAX_RECORDS) {
            throw damaged("too many records");
        }
        return n;
    }

    /// The offsets of the child records of the container at `o`.
    private List<Integer> children(byte[] b, int o, int end) throws IOException {
        int n = len(b, o, end);
        List<Integer> out = new ArrayList<Integer>();
        int p = o + 8;
        int stop = o + 8 + n;
        while (p + 8 <= stop) {
            out.add(Integer.valueOf(p));
            p += 8 + len(b, p, stop);
        }
        return out;
    }

    private static int end(byte[] b, int o) {
        return o + 8 + i32(b, o + 4);
    }

    /// The first child of the given type, or -1.
    private int child(byte[] b, int o, int t) throws IOException {
        List<Integer> c = children(b, o, end(b, o));
        for (Integer atP : c) {
            int p = atP.intValue();
            if (type(b, p) == t) {
                return p;
            }
        }
        return -1;
    }

    private void warn(String s) {
        if (warnings.size() < MAX_WARNINGS && !warnings.contains(s)) {
            warnings.add(s);
        }
    }

    private String id(char prefix) {
        seq++;
        return String.valueOf(prefix) + seq;
    }

    // ---------------------------------------------------------------- document

    private Presentation run(byte[] currentUser) throws IOException {
        int edit = currentEdit(currentUser);
        int docRef = readEdits(edit);
        Integer docOff = persist.get(Integer.valueOf(docRef));
        if (docOff == null) {
            throw damaged("no document container");
        }
        int dc = docOff.intValue();
        if (dc + 8 > doc.length || type(doc, dc) != RT_DOCUMENT) {
            throw damaged("no document container");
        }
        int docEnd = dc + 8 + len(doc, dc, doc.length);
        float w = Presentation.WIDE_WIDTH;
        float h = Presentation.WIDE_HEIGHT;
        List<SlideRef> slideRefs = new ArrayList<SlideRef>();
        List<Integer> kids = children(doc, dc, docEnd);
        for (Integer atP : kids) {
            int p = atP.intValue();
            int t = type(doc, p);
            if (t == RT_DOCUMENT_ATOM && i32(doc, p + 4) >= 8) {
                float sw = i32(doc, p + 8) / 8f;
                float sh = i32(doc, p + 12) / 8f;
                if (sw > 0 && sh > 0 && sw <= 100000 && sh <= 100000) {
                    w = sw;
                    h = sh;
                }
            } else if (t == RT_ENVIRONMENT) {
                readFonts(p);
            } else if (t == RT_PP_DRAWING_GROUP) {
                readBlipStore(p, 0);
            } else if (t == RT_SLIDE_LIST) {
                int inst = instance(doc, p);
                if (inst == 0) {
                    readSlideList(p, slideRefs);
                } else if (inst == 1) {
                    readMasterList(p);
                }
            } else if (t == RT_CRYPT_SESSION) {
                throw new IOException("encrypted .ppt files are not supported");
            }
        }
        List<Slide> slides = new ArrayList<Slide>();
        for (int i = 0; i < slideRefs.size() && i < MAX_SLIDES; i++) {
            slides.add(slide(slideRefs.get(i), i + 1));
        }
        if (slides.isEmpty()) {
            warn("The presentation has no slides");
        }
        return new Presentation(w, h, slides, Theme.DEFAULT, null, seq + 1,
                warnings.isEmpty() ? null : warnings);
    }

    private int currentEdit(byte[] cu) throws IOException {
        if (cu == null || cu.length < 20) {
            throw damaged("no Current User stream");
        }
        if (type(cu, 0) != RT_CURRENT_USER) {
            throw damaged("bad Current User stream");
        }
        int token = i32(cu, 12);
        if (token == HEADER_TOKEN_ENCRYPTED) {
            throw new IOException("encrypted .ppt files are not supported");
        }
        if (token != HEADER_TOKEN) {
            throw damaged("bad Current User header");
        }
        return i32(cu, 16);
    }

    /// Walks the user edits newest first; returns the document persist id.
    private int readEdits(int first) throws IOException {
        int off = first;
        int docRef = -1;
        List<Integer> seen = new ArrayList<Integer>();
        while (true) {
            if (seen.contains(Integer.valueOf(off))) {
                throw damaged("cyclic user edit chain");
            }
            if (seen.size() >= MAX_EDITS) {
                throw damaged("too many user edits");
            }
            seen.add(Integer.valueOf(off));
            if (off < 0 || off + 8 > doc.length || type(doc, off) != RT_USER_EDIT) {
                throw damaged("bad user edit offset");
            }
            int n = len(doc, off, doc.length);
            if (n < 28) {
                throw damaged("short user edit");
            }
            if (n >= 32) {
                throw new IOException("encrypted .ppt files are not supported");
            }
            int last = i32(doc, off + 16);
            int dir = i32(doc, off + 20);
            if (docRef < 0) {
                docRef = i32(doc, off + 24);
            }
            readPersistDirectory(dir);
            if (last == 0) {
                break;
            }
            off = last;
        }
        return docRef;
    }

    private void readPersistDirectory(int off) throws IOException {
        if (off < 0 || off + 8 > doc.length || type(doc, off) != RT_PERSIST_DIRECTORY) {
            throw damaged("bad persist directory offset");
        }
        int n = len(doc, off, doc.length);
        int p = off + 8;
        int stop = p + n;
        while (p + 4 <= stop) {
            int v = i32(doc, p);
            int id = v & 0xfffff;
            int count = v >>> 20;
            p += 4;
            if (count > (stop - p) / 4) {
                throw damaged("persist directory entry past its atom");
            }
            for (int i = 0; i < count; i++) {
                Integer key = Integer.valueOf(id + i);
                if (!persist.containsKey(key)) {
                    persist.put(key, Integer.valueOf(i32(doc, p)));
                }
                p += 4;
            }
        }
    }

    /// The offset of a persist object, or -1 when it is missing or out of range.
    private int object(int ref) {
        Integer o = persist.get(Integer.valueOf(ref));
        if (o == null || o.intValue() < 0 || o.intValue() + 8 > doc.length) {
            return -1;
        }
        return o.intValue();
    }

    private void readFonts(int env) throws IOException {
        int fc = child(doc, env, RT_FONT_COLLECTION);
        if (fc < 0) {
            return;
        }
        List<Integer> c = children(doc, fc, end(doc, fc));
        for (Integer atP : c) {
            int p = atP.intValue();
            if (type(doc, p) == RT_FONT_ENTITY && i32(doc, p + 4) >= 64) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < 32; k++) {
                    int ch = u16(doc, p + 8 + k * 2);
                    if (ch == 0) {
                        break;
                    }
                    sb.append((char) ch);
                }
                fonts.add(sb.toString());
            }
        }
    }

    private void readSlideList(int p, List<SlideRef> out) throws IOException {
        List<Integer> c = children(doc, p, end(doc, p));
        SlideRef cur = null;
        Text text = null;
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            int n = i32(doc, q + 4);
            if (t == RT_SLIDE_PERSIST && n >= 4) {
                cur = new SlideRef();
                cur.persistRef = i32(doc, q + 8);
                out.add(cur);
                text = null;
            } else if (cur != null && t == RT_TEXT_HEADER && n >= 4) {
                text = new Text();
                text.type = i32(doc, q + 8);
                cur.texts.add(text);
            } else if (text != null && (t == RT_TEXT_CHARS || t == RT_TEXT_BYTES)) {
                text.chars = chars(doc, q);
            } else if (text != null && t == RT_STYLE_TEXT_PROP) {
                text.styleOff = q + 8;
                text.styleLen = n;
            }
        }
    }

    private void readMasterList(int p) throws IOException {
        List<Integer> c = children(doc, p, end(doc, p));
        for (Integer atQ : c) {
            int q = atQ.intValue();
            if (type(doc, q) == RT_SLIDE_PERSIST && i32(doc, q + 4) >= 16) {
                masters.put(Integer.valueOf(i32(doc, q + 20)), Integer.valueOf(i32(doc, q + 8)));
            }
        }
        if (!masters.isEmpty()) {
            int m = object(masters.values().iterator().next().intValue());
            if (m >= 0 && type(doc, m) == RT_MAIN_MASTER) {
                int[] s = scheme(m);
                if (s != null) {
                    docScheme = s;
                }
            }
        }
    }

    private int[] scheme(int container) throws IOException {
        int cs = child(doc, container, RT_COLOR_SCHEME);
        if (cs < 0 || i32(doc, cs + 4) < 32) {
            return null;
        }
        int[] s = new int[8];
        for (int i = 0; i < 8; i++) {
            s[i] = u8(doc, cs + 8 + i * 4) << 16 | u8(doc, cs + 9 + i * 4) << 8 | u8(doc, cs + 10 + i * 4);
        }
        return s;
    }

    /// Decodes TextChars (UTF-16LE) or TextBytes (Latin-1) by hand.
    private String chars(byte[] b, int q) throws IOException {
        int n = i32(b, q + 4);
        boolean wide = type(b, q) == RT_TEXT_CHARS;
        int count = wide ? n / 2 : n;
        textChars += count;
        if (textChars > MAX_TEXT) {
            throw damaged("too much text");
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            int ch = wide ? u16(b, q + 8 + i * 2) : u8(b, q + 8 + i);
            sb.append((char) ch);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- pictures

    private void readBlipStore(int p, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw damaged("nesting too deep");
        }
        List<Integer> c = children(doc, p, end(doc, p));
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            if (t == OA_DGG && isContainer(doc, q)) {
                readBlipStore(q, depth + 1);
            } else if (t == OA_BSTORE) {
                List<Integer> e = children(doc, q, end(doc, q));
                for (Integer atF : e) {
                    int f = atF.intValue();
                    blips.add(type(doc, f) == OA_FBSE ? fbse(f) : null);
                }
            }
        }
    }

    private Picture fbse(int f) throws IOException {
        int n = i32(doc, f + 4);
        if (n < 36) {
            return null;
        }
        int size = i32(doc, f + 8 + 20);
        int delay = i32(doc, f + 8 + 28);
        int nameLen = u8(doc, f + 8 + 33);
        int embedded = f + 8 + 36 + nameLen;
        if (embedded + 8 <= f + 8 + n) {
            return blip(doc, embedded, f + 8 + n);
        }
        if (size <= 0 || delay < 0 || delay + 8 > pictures.length) {
            warn("A picture is missing from the Pictures stream");
            return null;
        }
        return blip(pictures, delay, pictures.length);
    }

    private Picture blip(byte[] b, int o, int limit) throws IOException {
        int n = len(b, o, limit);
        int t = type(b, o);
        int inst = instance(b, o);
        int uids = (inst & 1) == 1 ? 32 : 16;
        int start = o + 8 + uids;
        int stop = o + 8 + n;
        String mime;
        byte[] data;
        if (t == 0xF01A || t == 0xF01B || t == 0xF01C) {
            start += 34;
            if (start > stop) {
                return null;
            }
            mime = t == 0xF01A ? "image/emf" : t == 0xF01B ? "image/wmf" : "image/pict";
            if (u8(b, start - 2) != 0xFE) {
                warn("Compressed metafile pictures are kept compressed and may not display");
            } else {
                warn("Metafile pictures (EMF, WMF, PICT) are kept but may not display");
            }
            data = slice(b, start, stop);
        } else {
            start += 1;
            if (start > stop) {
                return null;
            }
            data = slice(b, start, stop);
            if (t == 0xF01D || t == 0xF02A) {
                mime = "image/jpeg";
            } else if (t == 0xF01E) {
                mime = "image/png";
            } else if (t == 0xF01F) {
                mime = "image/bmp";
                data = bmp(data);
                if (data == null) {
                    warn("A damaged bitmap picture was skipped");
                    return null;
                }
            } else if (t == 0xF029) {
                mime = "image/tiff";
                warn("TIFF pictures are kept but may not display");
            } else {
                warn("A picture of an unknown kind was skipped");
                return null;
            }
        }
        if (data.length == 0) {
            return null;
        }
        return new Picture(id('p'), data, mime, 100, 100, null);
    }

    private static byte[] slice(byte[] b, int s, int e) {
        byte[] out = new byte[e - s];
        System.arraycopy(b, s, out, 0, out.length);
        return out;
    }

    /// Wraps a device independent bitmap in a BMP file header.
    private static byte[] bmp(byte[] dib) {
        if (dib.length < 40) {
            return null;
        }
        int hs = i32(dib, 0);
        if (hs < 12 || hs > dib.length) {
            return null;
        }
        int bits = hs == 12 ? u16(dib, 10) : u16(dib, 14);
        int used = hs >= 36 ? i32(dib, 32) : 0;
        int entry = hs == 12 ? 3 : 4;
        int palette;
        if (used > 0 && used <= 256) {
            palette = used * entry;
        } else {
            palette = bits <= 8 ? (1 << bits) * entry : 0;
        }
        if (hs == 40 && i32(dib, 16) == 3) {
            palette += 12;
        }
        int off = 14 + hs + palette;
        byte[] out = new byte[dib.length + 14];
        out[0] = 'B';
        out[1] = 'M';
        put32(out, 2, out.length);
        put32(out, 10, off);
        System.arraycopy(dib, 0, out, 14, dib.length);
        return out;
    }

    private static void put32(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
        b[o + 2] = (byte) (v >> 16);
        b[o + 3] = (byte) (v >> 24);
    }

    // ---------------------------------------------------------------- slides

    private Slide slide(SlideRef ref, int number) throws IOException {
        String sid = id('s');
        int sc = object(ref.persistRef);
        List<SlideElement> elements = new ArrayList<SlideElement>();
        List<Block> notes = null;
        Transition transition = null;
        boolean hidden = false;
        if (sc < 0 || type(doc, sc) != RT_SLIDE) {
            warn("Slide " + number + " is missing; only its outline text was kept");
        } else {
            int[] scheme = scheme(sc);
            if (scheme == null) {
                scheme = docScheme;
            }
            List<Integer> c = children(doc, sc, end(doc, sc));
            for (Integer atP : c) {
                int p = atP.intValue();
                int t = type(doc, p);
                int n = i32(doc, p + 4);
                if (t == RT_SLIDE_ATOM && n >= 20) {
                    int notesRef = i32(doc, p + 8 + 16);
                    if (notesRef != 0) {
                        notes = notes(notesRef);
                    }
                    masterShapes(i32(doc, p + 8 + 12));
                } else if (t == RT_SLIDE_SHOW_INFO && n >= 12) {
                    int flags = u16(doc, p + 8 + 10);
                    hidden = (flags & 0x4) != 0;
                    transition = transition(p + 8, number);
                } else if (t == RT_PP_DRAWING) {
                    drawing(p, ref, scheme, elements);
                } else if (t == RT_PROG_TAGS) {
                    buildsWarning();
                }
            }
        }
        if (!hasText(elements)) {
            outlineFallback(ref, elements);
        }
        if (elements.isEmpty()) {
            warn("Slide " + number + " has no content that could be converted");
        }
        return new Slide(sid, null, null, elements, notes, transition, null, hidden);
    }

    private void buildsWarning() {
        if (!warnedBuilds) {
            warnedBuilds = true;
            warn("Animations and builds are not converted");
        }
    }

    private void masterShapes(int masterId) throws IOException {
        if (warnedMaster) {
            return;
        }
        Integer ref = masters.get(Integer.valueOf(masterId));
        int m = ref == null ? -1 : object(ref.intValue());
        if (m < 0) {
            return;
        }
        int d = child(doc, m, RT_PP_DRAWING);
        if (d >= 0 && countShapes(d, 0) > 0) {
            warnedMaster = true;
            warn("Graphics on the slide master are not converted");
        }
    }

    /// Counts the shapes of a drawing that are not placeholders.
    private int countShapes(int p, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw damaged("nesting too deep");
        }
        int count = 0;
        List<Integer> c = children(doc, p, end(doc, p));
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            if (t == OA_SP) {
                int cd = child(doc, q, OA_CLIENT_DATA);
                int fsp = child(doc, q, OA_FSP);
                boolean placeholder = cd >= 0 && child(doc, cd, RT_PLACEHOLDER) >= 0;
                boolean special = fsp < 0 || (i32(doc, fsp + 12) & 0x405) != 0;
                if (!placeholder && !special) {
                    count++;
                }
            } else if (isContainer(doc, q) && (t == OA_DG || t == OA_SPGR)) {
                count += countShapes(q, depth + 1);
            }
        }
        return count;
    }

    private static boolean hasText(List<SlideElement> elements) {
        for (SlideElement e : elements) {
            if (e.plainText().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private void outlineFallback(SlideRef ref, List<SlideElement> elements) throws IOException {
        float y = 40;
        for (Text t : ref.texts) {
            if (t.chars == null || t.chars.length() == 0) {
                continue;
            }
            boolean title = t.type == 0 || t.type == 6;
            TextFrame tf = new TextFrame(paragraphs(t, title ? 32 : 18));
            elements.add(new ShapeElement(id('e'), null, "rect", 40, y, 640, title ? 80 : 300, 0,
                    false, false, null, null, tf, title ? ShapeElement.TITLE : ShapeElement.BODY,
                    null));
            y += title ? 100 : 320;
        }
    }

    private Transition transition(int a, int number) {
        int time = i32(doc, a);
        int dir = u8(doc, a + 8);
        int effect = u8(doc, a + 9);
        int flags = u16(doc, a + 10);
        int speed = u8(doc, a + 12);
        int duration = speed == 0 ? 1000 : speed == 1 ? 750 : 500;
        int after = (flags & 0x400) != 0 ? Math.max(0, time) : -1;
        boolean click = (flags & 0x1) != 0 || after < 0;
        String kind;
        String direction = Transition.FROM_RIGHT;
        switch (effect) {
            case 0:
                if (after < 0) {
                    return null;
                }
                return new Transition(Transition.NONE, 0, direction, click, after);
            case 4:
                kind = Transition.COVER;
                direction = direction(dir);
                break;
            case 7:
                kind = Transition.UNCOVER;
                direction = direction(dir);
                break;
            case 10:
                kind = Transition.WIPE;
                direction = direction(dir);
                break;
            case 20:
                kind = Transition.PUSH;
                direction = direction(dir);
                break;
            case 11:
                kind = Transition.ZOOM;
                break;
            case 6:
            case 23:
                kind = Transition.FADE;
                break;
            default:
                kind = Transition.FADE;
                warn("Slide " + number + " uses a transition that is shown as a fade");
                break;
        }
        return new Transition(kind, duration, direction, click, after);
    }

    private static String direction(int d) {
        switch (d) {
            case 0:
                return Transition.FROM_RIGHT;
            case 1:
                return Transition.FROM_BOTTOM;
            case 2:
                return Transition.FROM_LEFT;
            case 3:
                return Transition.FROM_TOP;
            default:
                return Transition.FROM_RIGHT;
        }
    }

    private List<Block> notes(int ref) throws IOException {
        int nc = object(ref);
        if (nc < 0 || type(doc, nc) != RT_NOTES) {
            return null;
        }
        List<Block> out = new ArrayList<Block>();
        collectText(nc, out, 0);
        return out.isEmpty() ? null : out;
    }

    private void collectText(int p, List<Block> out, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw damaged("nesting too deep");
        }
        List<Integer> c = children(doc, p, end(doc, p));
        Text cur = null;
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            if (t == RT_TEXT_HEADER && i32(doc, q + 4) >= 4) {
                cur = new Text();
                cur.type = i32(doc, q + 8);
            } else if (t == RT_TEXT_CHARS || t == RT_TEXT_BYTES) {
                if (cur == null) {
                    cur = new Text();
                }
                cur.chars = chars(doc, q);
            } else if (t == RT_STYLE_TEXT_PROP && cur != null) {
                cur.styleOff = q + 8;
                cur.styleLen = i32(doc, q + 4);
            } else if (isContainer(doc, q)) {
                collectText(q, out, depth + 1);
            }
        }
        if (cur != null && cur.chars != null && cur.chars.length() > 0) {
            out.addAll(paragraphs(cur, 12));
        }
    }

    // ---------------------------------------------------------------- drawing

    private void drawing(int p, SlideRef ref, int[] scheme, List<SlideElement> out) throws IOException {
        int dg = child(doc, p, OA_DG);
        if (dg < 0) {
            return;
        }
        int spgr = child(doc, dg, OA_SPGR);
        if (spgr < 0) {
            return;
        }
        group(spgr, ref, scheme, new Frame(0, 0, 1 / 8f, 1 / 8f), out, 1);
    }

    /// Reads a group container; the first shape describes the group itself.
    private void group(int spgr, SlideRef ref, int[] scheme, Frame frame, List<SlideElement> out,
            int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw damaged("nesting too deep");
        }
        List<Integer> c = children(doc, spgr, end(doc, spgr));
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            if (q == c.get(0).intValue() && t == OA_SP) {
                continue;
            }
            if (t == OA_SP) {
                SlideElement e = shape(q, ref, scheme, frame);
                if (e != null) {
                    out.add(e);
                }
            } else if (t == OA_SPGR) {
                SlideElement g = nested(q, ref, scheme, frame, depth + 1);
                if (g != null) {
                    out.add(g);
                }
            }
        }
    }

    private SlideElement nested(int spgr, SlideRef ref, int[] scheme, Frame frame, int depth)
            throws IOException {
        if (depth > MAX_DEPTH) {
            throw damaged("nesting too deep");
        }
        List<Integer> c = children(doc, spgr, end(doc, spgr));
        if (c.isEmpty() || type(doc, c.get(0).intValue()) != OA_SP) {
            return null;
        }
        int first = c.get(0).intValue();
        float[] r = anchor(first, frame);
        int fspgr = child(doc, first, OA_FSPGR);
        Frame inner = frame;
        if (r != null && fspgr >= 0 && i32(doc, fspgr + 4) >= 16) {
            int gl = i32(doc, fspgr + 8);
            int gt = i32(doc, fspgr + 12);
            int gw = i32(doc, fspgr + 16) - gl;
            int gh = i32(doc, fspgr + 20) - gt;
            float sx = gw > 0 ? r[2] / gw : 1;
            float sy = gh > 0 ? r[3] / gh : 1;
            inner = new Frame(r[0] - gl * sx, r[1] - gt * sy, sx, sy);
        }
        List<SlideElement> kids = new ArrayList<SlideElement>();
        group(spgr, ref, scheme, inner, kids, depth);
        if (kids.isEmpty()) {
            return null;
        }
        GroupElement g = GroupElement.of(id('g'), kids);
        int fsp = child(doc, first, OA_FSP);
        if (fsp >= 0) {
            int flags = i32(doc, fsp + 12);
            boolean fh = (flags & 0x40) != 0;
            boolean fv = (flags & 0x80) != 0;
            float rot = rotation(first);
            if (fh || fv || rot != 0) {
                return new GroupElement(g.id, null, kids, g.x, g.y, g.width, g.height, rot, fh, fv);
            }
        }
        return g;
    }

    /// The shape's rectangle in points, or null when it has no anchor.
    private float[] anchor(int sp, Frame frame) throws IOException {
        List<Integer> c = children(doc, sp, end(doc, sp));
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            int n = i32(doc, q + 4);
            int l;
            int tp;
            int r;
            int b;
            if (t == OA_CLIENT_ANCHOR && n >= 8 && n < 16) {
                tp = s16(doc, q + 8);
                l = s16(doc, q + 10);
                r = s16(doc, q + 12);
                b = s16(doc, q + 14);
            } else if (t == OA_CLIENT_ANCHOR && n >= 16) {
                tp = i32(doc, q + 8);
                l = i32(doc, q + 12);
                r = i32(doc, q + 16);
                b = i32(doc, q + 20);
            } else if (t == OA_CHILD_ANCHOR && n >= 16) {
                l = i32(doc, q + 8);
                tp = i32(doc, q + 12);
                r = i32(doc, q + 16);
                b = i32(doc, q + 20);
            } else {
                continue;
            }
            float x = frame.ox + l * frame.sx;
            float y = frame.oy + tp * frame.sy;
            return new float[]{x, y, Math.max(0, (r - l) * frame.sx), Math.max(0, (b - tp) * frame.sy)}; // NOPMD AvoidBranchingStatementAsLastInLoop - the first anchor wins
        }
        return null;
    }

    private float rotation(int sp) throws IOException {
        return prop(props(sp), 0x0004, 0) / 65536f;
    }

    /// The simple properties of the shape's OfficeArtFOPT, keyed by property id.
    private Map<Integer, Integer> props(int sp) throws IOException {
        Map<Integer, Integer> m = new HashMap<Integer, Integer>();
        int f = child(doc, sp, OA_FOPT);
        if (f < 0) {
            return m;
        }
        int count = instance(doc, f);
        int n = i32(doc, f + 4);
        if (count * 6 > n) {
            throw damaged("shape properties past their record");
        }
        for (int i = 0; i < count; i++) {
            int o = f + 8 + i * 6;
            int pid = u16(doc, o) & 0x3fff;
            m.put(Integer.valueOf(pid), Integer.valueOf(i32(doc, o + 2)));
        }
        return m;
    }

    private static int prop(Map<Integer, Integer> m, int id, int def) {
        Integer v = m.get(Integer.valueOf(id));
        return v == null ? def : v.intValue();
    }

    private int color(int v, int[] scheme) {
        int flags = v >>> 24;
        if ((flags & 0x08) != 0) {
            int idx = v & 0xff;
            return idx < scheme.length ? scheme[idx] : 0;
        }
        return (v & 0xff) << 16 | (v >> 8 & 0xff) << 8 | (v >> 16 & 0xff);
    }

    private SlideElement shape(int sp, SlideRef ref, int[] scheme, Frame frame) throws IOException {
        int fsp = child(doc, sp, OA_FSP);
        if (fsp < 0 || i32(doc, fsp + 4) < 8) {
            return null;
        }
        int spt = instance(doc, fsp);
        int flags = i32(doc, fsp + 12);
        if ((flags & 0x408) != 0) {
            return null;
        }
        float[] r = anchor(sp, frame);
        if (r == null) {
            warn("A shape without a position was skipped");
            return null;
        }
        Map<Integer, Integer> p = props(sp);
        boolean fh = (flags & 0x40) != 0;
        boolean fv = (flags & 0x80) != 0;
        float rot = prop(p, 0x0004, 0) / 65536f;
        String placeholder = null;
        int cd = child(doc, sp, OA_CLIENT_DATA);
        if (cd >= 0) {
            int ph = child(doc, cd, RT_PLACEHOLDER);
            if (ph >= 0 && i32(doc, ph + 4) >= 5) {
                placeholder = placeholder(u8(doc, ph + 12));
            }
            if (child(doc, cd, RT_ANIMATION_INFO) >= 0) {
                buildsWarning();
            }
        }
        TextFrame text = null;
        int tb = child(doc, sp, OA_CLIENT_TEXTBOX);
        if (tb >= 0) {
            text = textbox(tb, ref, placeholder);
        }
        int pib = prop(p, 0x0104, 0);
        if (spt == 75 || pib > 0 && prop(p, 0x0180, 0) != 3) {
            Picture pic = pib > 0 && pib <= blips.size() ? blips.get(pib - 1) : null;
            if (pic == null) {
                warn("A picture could not be found and was skipped");
                return null;
            }
            float w = Math.max(1, r[2]);
            float h = Math.max(1, r[3]);
            pic = pic.withSize(w, h);
            return new PictureElement(id('e'), null, pic, r[0], r[1], w, h, rot, fh, fv, 0, 0, 0, 0,
                    line(p, scheme, false));
        }
        String geometry = geometry(spt);
        if (geometry == null) {
            warn("Shapes of kind " + spt + " are drawn as rectangles");
            geometry = "rect";
        }
        boolean plain = spt == 202 || placeholder != null;
        boolean isLine = ShapeGeometry.isLine(geometry);
        Fill fill = isLine ? Fill.NO_FILL : fill(p, scheme, !plain);
        LineStyle line = line(p, scheme, !plain);
        return new ShapeElement(id('e'), null, geometry, r[0], r[1], r[2], r[3], rot, fh, fv, fill,
                line, text, placeholder, null);
    }

    private static String placeholder(int t) {
        switch (t) {
            case 0x0D:
            case 0x0F:
            case 0x11:
            case 0x13:
                return ShapeElement.TITLE;
            case 0x12:
                return ShapeElement.SUBTITLE;
            case 0x0E:
            case 0x10:
            case 0x14:
                return ShapeElement.BODY;
            default:
                return null;
        }
    }

    private Fill fill(Map<Integer, Integer> p, int[] scheme, boolean byDefault) {
        int bools = prop(p, 0x01BF, -1);
        boolean filled = byDefault;
        if (bools != -1 && (bools & 0x100000) != 0) {
            filled = (bools & 0x10) != 0;
        } else if (bools == -1 && p.containsKey(Integer.valueOf(0x0181))) {
            filled = true;
        }
        if (!filled) {
            return Fill.NO_FILL;
        }
        int c = color(prop(p, 0x0181, 0xffffff), scheme);
        int type = prop(p, 0x0180, 0);
        Fill f;
        if (type >= 4 && type <= 8) {
            int c2 = color(prop(p, 0x0183, 0xffffff), scheme);
            float angle = prop(p, 0x018B, 0) / 65536f;
            f = Fill.gradient(c, c2, angle);
        } else {
            if (type != 0) {
                warn("Pattern, texture and picture fills are shown as solid colors");
            }
            f = Fill.solid(c);
        }
        int opacity = prop(p, 0x0182, 65536);
        if (opacity >= 0 && opacity < 65536) {
            f = f.withAlpha(opacity / 65536f);
        }
        return f;
    }

    private LineStyle line(Map<Integer, Integer> p, int[] scheme, boolean byDefault) {
        int bools = prop(p, 0x01FF, -1);
        boolean on = byDefault;
        if (bools != -1 && (bools & 0x80000) != 0) {
            on = (bools & 0x8) != 0;
        } else if (bools == -1 && p.containsKey(Integer.valueOf(0x01C0))) {
            on = true;
        }
        if (!on) {
            return null;
        }
        int c = color(prop(p, 0x01C0, 0), scheme);
        float w = prop(p, 0x01CB, 9525) / 12700f;
        int dash = prop(p, 0x01CE, 0);
        String d = dash == 0 ? LineStyle.SOLID : dash == 2 || dash == 5 ? LineStyle.DOT : LineStyle.DASH;
        return new LineStyle(c, w, d, 1, arrow(prop(p, 0x01D0, 0)), arrow(prop(p, 0x01D1, 0)));
    }

    private static String arrow(int v) {
        switch (v) {
            case 1:
            case 2:
            case 3:
                return LineStyle.ARROW_TRIANGLE;
            case 4:
                return LineStyle.ARROW_OVAL;
            case 5:
                return LineStyle.ARROW_OPEN;
            default:
                return LineStyle.ARROW_NONE;
        }
    }

    /// Maps an MSOSPT shape type to a preset geometry name, or null when there is none.
    private static String geometry(int spt) {
        switch (spt) {
            case 1:
            case 202:
                return "rect";
            case 2:
                return "roundRect";
            case 3:
                return "ellipse";
            case 4:
                return "diamond";
            case 5:
                return "triangle";
            case 6:
                return "rtTriangle";
            case 7:
                return "parallelogram";
            case 8:
                return "trapezoid";
            case 9:
                return "hexagon";
            case 10:
                return "octagon";
            case 11:
                return "plus";
            case 12:
                return "star5";
            case 13:
                return "rightArrow";
            case 15:
                return "homePlate";
            case 20:
                return "line";
            case 22:
                return "can";
            case 23:
                return "donut";
            case 32:
                return "straightConnector1";
            case 33:
            case 34:
                return "bentConnector3";
            case 37:
            case 38:
                return "curvedConnector3";
            case 55:
                return "chevron";
            case 56:
                return "pentagon";
            case 61:
                return "wedgeRectCallout";
            case 62:
                return "wedgeRoundRectCallout";
            case 65:
                return "foldedCorner";
            case 66:
                return "leftArrow";
            case 67:
                return "downArrow";
            case 68:
                return "upArrow";
            case 69:
                return "leftRightArrow";
            case 74:
                return "heart";
            case 109:
                return "flowChartProcess";
            case 110:
                return "flowChartDecision";
            case 112:
                return "flowChartPredefinedProcess";
            case 114:
                return "flowChartDocument";
            case 116:
                return "flowChartTerminator";
            case 187:
                return "star4";
            default:
                return null;
        }
    }

    // ---------------------------------------------------------------- text

    private TextFrame textbox(int tb, SlideRef ref, String placeholder) throws IOException {
        List<Integer> c = children(doc, tb, end(doc, tb));
        Text cur = new Text();
        for (Integer atQ : c) {
            int q = atQ.intValue();
            int t = type(doc, q);
            int n = i32(doc, q + 4);
            if (t == RT_TEXT_HEADER && n >= 4) {
                cur.type = i32(doc, q + 8);
            } else if (t == RT_TEXT_CHARS || t == RT_TEXT_BYTES) {
                cur.chars = chars(doc, q);
            } else if (t == RT_STYLE_TEXT_PROP) {
                cur.styleOff = q + 8;
                cur.styleLen = n;
            } else if (t == RT_OUTLINE_TEXT_REF && n >= 4) {
                int idx = i32(doc, q + 8);
                if (idx >= 0 && idx < ref.texts.size()) {
                    cur = ref.texts.get(idx);
                }
            }
        }
        if (cur.chars == null) {
            return null;
        }
        boolean title = ShapeElement.TITLE.equals(placeholder);
        return new TextFrame(paragraphs(cur, title ? 32 : 18));
    }

    /// Splits text at carriage returns and applies the style atom's runs.
    private List<Block> paragraphs(Text t, float defaultSize) {
        String s = t.chars;
        int len = s.length();
        int[] align = new int[len + 1];
        boolean[] bullet = new boolean[len + 1];
        TextStyle base = new TextStyle(fonts.isEmpty() ? "Arial" : fonts.get(0), defaultSize, 0,
                false, false, false, false, TextStyle.NO_BACKGROUND, null, null);
        TextStyle[] styles = new TextStyle[len + 1];
        for (int i = 0; i <= len; i++) {
            styles[i] = base;
        }
        if (t.styleOff >= 0) {
            try {
                styleRuns(t.styleOff, t.styleOff + t.styleLen, len + 1, align, bullet, styles, base);
            } catch (IOException ex) {
                warn("Damaged text formatting was ignored");
            }
        }
        List<Block> out = new ArrayList<Block>();
        int start = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || s.charAt(i) == '\r') {
                RichText.Builder b = new RichText.Builder();
                int k = start;
                while (k < i) {
                    int e = k + 1;
                    while (e < i && styles[e].equals(styles[k])) {
                        e++;
                    }
                    b.append(clean(s.substring(k, e)), styles[k]);
                    k = e;
                }
                Paragraph p = new Paragraph(id('t'), b.build());
                p = p.withAlign(alignment(align[start]));
                if (bullet[start]) {
                    p = p.withList(Paragraph.BULLET, 0);
                }
                out.add(p);
                start = i + 1;
            }
        }
        return out;
    }

    private static String clean(String s) {
        return s.replace('\u000b', ' ');
    }

    private static String alignment(int a) {
        switch (a) {
            case 1:
                return Paragraph.CENTER;
            case 2:
                return Paragraph.RIGHT;
            case 3:
                return Paragraph.JUSTIFY;
            default:
                return Paragraph.LEFT;
        }
    }

    private void need(int p, int n, int end) throws IOException {
        if (p + n > end) {
            throw damaged("text style past its record");
        }
    }

    private void styleRuns(int p, int end, int total, int[] align, boolean[] bullet,
            TextStyle[] styles, TextStyle base) throws IOException {
        if (end > doc.length) {
            throw damaged("text style past the stream");
        }
        int at = 0;
        while (at < total) {
            need(p, 10, end);
            int count = i32(doc, p);
            int mask = i32(doc, p + 6);
            p += 10;
            if (count <= 0) {
                throw damaged("empty paragraph run");
            }
            boolean hasBullet = false;
            int a = 0;
            if ((mask & 0xF) != 0) {
                need(p, 2, end);
                hasBullet = (u16(doc, p) & 1) != 0;
                p += 2;
            }
            p += skip(mask, 0x80, 2) + skip(mask, 0x10, 2) + skip(mask, 0x40, 2) + skip(mask, 0x20, 4);
            if ((mask & 0x800) != 0) {
                need(p, 2, end);
                a = u16(doc, p);
                p += 2;
            }
            p += skip(mask, 0x1000, 2) + skip(mask, 0x2000, 2) + skip(mask, 0x4000, 2)
                    + skip(mask, 0x100, 2) + skip(mask, 0x400, 2) + skip(mask, 0x8000, 2);
            if ((mask & 0x100000) != 0) {
                need(p, 2, end);
                p += 2 + u16(doc, p) * 4;
            }
            p += skip(mask, 0x10000, 2) + skip(mask, 0xE0000, 2) + skip(mask, 0x200000, 2);
            need(p, 0, end);
            for (int i = at; i < Math.min(total, at + count); i++) {
                align[i] = a;
                bullet[i] = hasBullet;
            }
            at += count;
        }
        at = 0;
        while (at < total && p < end) {
            need(p, 8, end);
            int count = i32(doc, p);
            int mask = i32(doc, p + 4);
            p += 8;
            if (count <= 0) {
                throw damaged("empty character run");
            }
            TextStyle st = base;
            if ((mask & 0xFFFF) != 0) {
                need(p, 2, end);
                int fs = u16(doc, p);
                p += 2;
                st = st.withBold((fs & 1) != 0).withItalic((fs & 2) != 0).withUnderline((fs & 4) != 0);
            }
            if ((mask & 0x10000) != 0) {
                need(p, 2, end);
                int f = u16(doc, p);
                p += 2;
                if (f < fonts.size()) {
                    st = st.withFont(fonts.get(f));
                }
            }
            p += skip(mask, 0x200000, 2) + skip(mask, 0x400000, 2) + skip(mask, 0x800000, 2);
            if ((mask & 0x20000) != 0) {
                need(p, 2, end);
                int size = u16(doc, p);
                p += 2;
                if (size > 0 && size < 4000) {
                    st = st.withSize(size);
                }
            }
            if ((mask & 0x40000) != 0) {
                need(p, 4, end);
                int idx = u8(doc, p + 3);
                if (idx == 0xFE) {
                    st = st.withColor(u8(doc, p) << 16 | u8(doc, p + 1) << 8 | u8(doc, p + 2));
                } else if (idx < 8) {
                    st = st.withColor(docScheme[idx]);
                }
                p += 4;
            }
            p += skip(mask, 0x80000, 2) + skip(mask, 0x100000, 4) + skip(mask, 0x1000000, 2)
                    + skip(mask, 0x2000000, 2) + skip(mask, 0x4000000, 4);
            need(p, 0, end);
            for (int i = at; i < Math.min(total, at + count); i++) {
                styles[i] = st;
            }
            at += count;
        }
    }

    private static int skip(int mask, int bits, int n) {
        return (mask & bits) != 0 ? n : 0;
    }
}
