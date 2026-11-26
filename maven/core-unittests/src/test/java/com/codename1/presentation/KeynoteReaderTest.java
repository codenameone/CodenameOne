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

import com.codename1.junit.UITestBase;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.TextStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Builds Keynote packages by hand -- protobuf, literal-only Snappy, IWA chunks, zip -- since
/// no real fixture may be shipped.
class KeynoteReaderTest extends UITestBase {

    /// A minimal protobuf writer.
    static final class Pb {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        static void varint(ByteArrayOutputStream o, long v) {
            while ((v & ~0x7fL) != 0) {
                o.write((int) (v & 0x7f) | 0x80);
                v >>>= 7;
            }
            o.write((int) v);
        }

        Pb v(int field, long value) {
            varint(out, field << 3);
            varint(out, value);
            return this;
        }

        Pb b(int field, byte[] value) {
            varint(out, field << 3 | 2);
            varint(out, value.length);
            out.write(value, 0, value.length);
            return this;
        }

        Pb s(int field, String value) {
            try {
                return b(field, value.getBytes("UTF-8"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Pb m(int field, Pb value) {
            return b(field, value.bytes());
        }

        Pb f(int field, float value) {
            varint(out, field << 3 | 5);
            int bits = Float.floatToIntBits(value);
            for (int i = 0; i < 4; i++) {
                out.write(bits >>> (8 * i) & 0xff);
            }
            return this;
        }

        Pb ref(int field, long id) {
            return m(field, new Pb().v(1, id));
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    static Pb pair(float a, float b) {
        return new Pb().f(1, a).f(2, b);
    }

    static Pb drawable(float x, float y, float w, float h) {
        return new Pb().m(1, new Pb().m(1, pair(x, y)).m(2, pair(w, h)));
    }

    static Pb color(float r, float g, float b) {
        return new Pb().v(1, 1).f(3, r).f(4, g).f(5, b).f(6, 1);
    }

    /// Objects of one archive file, in order: id, type, payload.
    static final class Archive {
        final List<Object[]> objects = new ArrayList<Object[]>();

        Archive add(long id, int type, Pb payload) {
            objects.add(new Object[] {Long.valueOf(id), Integer.valueOf(type),
                payload.bytes()});
            return this;
        }

        byte[] raw() {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            for (Object[] obj : objects) {
                byte[] p = (byte[]) obj[2];
                byte[] info = new Pb().v(1, (Long) obj[0])
                        .m(2, new Pb().v(1, (Integer) obj[1]).v(3, p.length)).bytes();
                Pb.varint(o, info.length);
                o.write(info, 0, info.length);
                o.write(p, 0, p.length);
            }
            return o.toByteArray();
        }
    }

    /// Valid Snappy using only literals, in IWA chunk framing.
    static byte[] iwa(byte[] raw) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        Pb.varint(s, raw.length);
        int pos = 0;
        while (pos < raw.length) {
            int n = Math.min(60, raw.length - pos);
            s.write((n - 1) << 2);
            s.write(raw, pos, n);
            pos += n;
        }
        byte[] block = s.toByteArray();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0);
        o.write(block.length & 0xff);
        o.write(block.length >> 8 & 0xff);
        o.write(block.length >> 16 & 0xff);
        o.write(block, 0, block.length);
        return o.toByteArray();
    }

    static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(o);
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            z.putNextEntry(new ZipEntry(e.getKey()));
            z.write(e.getValue());
            z.closeEntry();
        }
        z.close();
        return o.toByteArray();
    }

    static Presentation read(byte[] data) throws IOException {
        return Presentation.read(new ByteArrayInputStream(data), Presentation.Format.KEY);
    }

    static final byte[] IMAGE = new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    static final byte[] THUMB = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 9};

    /// Document 1 -> show 2 -> root node 10 with children 11 and 12 (hidden).
    static Archive deck(int secondSlideType) {
        Archive a = new Archive();
        a.add(1, 1, new Pb().ref(2, 2));
        // KN.ShowArchive: 3 the slide tree, whose field 2 lists the slide nodes; 4 the size
        a.add(2, 2, new Pb().m(3, new Pb().ref(2, 11).ref(2, 12)).m(4, pair(1920, 1080)));
        a.add(10, 4, new Pb().ref(1, 11).ref(1, 12));
        a.add(11, 4, new Pb().ref(2, 21));
        a.add(12, 4, new Pb().ref(2, 22).v(4, 1));
        a.add(21, 5, new Pb().ref(5, 31).ref(7, 32).ref(7, 33).ref(27, 40));
        a.add(31, 7, new Pb().m(1, new Pb().m(1, new Pb().m(1, drawable(10, 20, 300, 50)))
                .ref(2, 51)));
        a.add(51, 2001, new Pb().s(3, "Hello Keynote").m(8,
                new Pb().m(1, new Pb().v(1, 0).ref(2, 61))));
        a.add(61, 2021, new Pb().m(11, new Pb().v(1, 1).f(3, 40).m(7, color(1, 0, 0))));
        a.add(32, 3004, new Pb().m(1, drawable(100, 200, 50, 60)).ref(2, 71)
                .m(3, new Pb().m(2, new Pb().v(1, 0).f(2, 0))));
        a.add(71, 3016, new Pb().m(11, new Pb().m(1, new Pb().m(1, color(0, 0, 1)))
                .m(2, new Pb().m(1, color(0, 1, 0)).f(2, 2))));
        a.add(33, 3005, new Pb().m(1, drawable(400, 300, 80, 40)).ref(11, 900));
        a.add(40, 15, new Pb().ref(1, 52));
        a.add(52, 2001, new Pb().s(3, "Speaker notes"));
        a.add(22, secondSlideType, new Pb().ref(7, 34));
        a.add(34, 2011, new Pb().m(1, new Pb().m(1, drawable(0, 0, 100, 100))).ref(2, 53));
        a.add(53, 2001, new Pb().s(3, "Second"));
        a.add(80, 11006, new Pb().m(3, new Pb().v(1, 900).s(3, "pic.png")));
        return a;
    }

    static byte[] pack(Archive a, boolean thumbs) throws IOException {
        Map<String, byte[]> e = new LinkedHashMap<String, byte[]>();
        e.put("Index/Document.iwa", iwa(a.raw()));
        e.put("Data/pic.png", IMAGE);
        if (thumbs) {
            e.put("Data/st-0001.jpg", THUMB);
            e.put("Data/st-0002.jpg", THUMB);
        }
        e.put("preview.jpg", THUMB);
        return zip(e);
    }

    @Test
    void readsShapesTextImagesAndNotes() throws IOException {
        Presentation p = read(pack(deck(5), false));
        assertEquals(1920f, p.width);
        assertEquals(1080f, p.height);
        assertEquals(2, p.getSlides().size());
        Slide s1 = p.getSlides().get(0);
        assertFalse(s1.hidden);
        assertEquals(3, s1.elements.size());

        ShapeElement title = (ShapeElement) s1.elements.get(0);
        assertEquals(ShapeElement.TITLE, title.placeholder);
        assertEquals(10f, title.x);
        assertEquals(300f, title.width);
        Paragraph para = (Paragraph) title.text.blocks.get(0);
        assertEquals("Hello Keynote", para.text.plainText());
        TextStyle ts = para.text.styleAt(0);
        assertTrue(ts.bold);
        assertEquals(40f, ts.size);
        assertEquals(0xff0000, ts.color);

        ShapeElement box = (ShapeElement) s1.elements.get(1);
        assertEquals("rect", box.geometry);
        assertEquals(0x0000ff, box.fill.color);
        assertEquals(0x00ff00, box.line.color);
        assertEquals(2f, box.line.width);

        PictureElement pic = (PictureElement) s1.elements.get(2);
        assertEquals(400f, pic.x);
        assertEquals(IMAGE.length, pic.picture.data().length);

        assertNotNull(s1.notes);
        assertEquals("Speaker notes", ((Paragraph) s1.notes.get(0)).text.plainText());

        Slide s2 = p.getSlides().get(1);
        assertTrue(s2.hidden);
        assertEquals("Second", s2.plainText().trim());
    }

    @Test
    void unknownSlideFallsBackToThumbnailWithWarning() throws IOException {
        Presentation p = read(pack(deck(9999), true));
        Slide s2 = p.getSlides().get(1);
        assertEquals(1, s2.elements.size());
        assertTrue(s2.elements.get(0) instanceof PictureElement);
        assertTrue(s2.hidden);
        assertFalse(p.getWarnings().isEmpty());
    }

    @Test
    void noShowFallsBackToPreview() throws IOException {
        Archive a = new Archive().add(5, 9999, new Pb().v(1, 1));
        Presentation p = read(pack(a, false));
        assertEquals(1, p.getSlides().size());
        assertTrue(p.getSlides().get(0).elements.get(0) instanceof PictureElement);
        assertFalse(p.getWarnings().isEmpty());
    }

    @Test
    void cyclesAndDeepNestingTerminate() throws IOException {
        Archive a = deck(5);
        a.add(11, 4, new Pb().ref(2, 21).ref(1, 12).ref(1, 11));
        Presentation p = read(pack(a, false));
        assertEquals(2, p.getSlides().size());

        Archive d = new Archive();
        d.add(1, 1, new Pb().ref(2, 2));
        d.add(2, 2, new Pb().m(3, new Pb().ref(2, 1000)));
        for (int i = 0; i < 500; i++) {
            d.add(1000 + i, 4, new Pb().ref(1, 1001 + i).ref(2, 21));
        }
        d.add(21, 5, new Pb().ref(7, 53));
        d.add(53, 3004, new Pb().m(1, drawable(0, 0, 10, 10)));
        Presentation q = read(pack(d, false));
        assertTrue(q.getSlides().size() < 100);
        assertFalse(q.getWarnings().isEmpty());
    }

    @Test
    void hostileArchivesThrow() throws IOException {
        Map<String, byte[]> e = new LinkedHashMap<String, byte[]>();
        byte[] good = iwa(deck(5).raw());
        byte[] cut = new byte[good.length / 2];
        System.arraycopy(good, 0, cut, 0, cut.length);
        e.put("Index/Document.iwa", cut);
        final byte[] truncated = zip(e);
        assertThrows(IOException.class, () -> read(truncated));

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] info = new Pb().v(1, 1).m(2, new Pb().v(1, 1).v(3, 1L << 40)).bytes();
        Pb.varint(o, info.length);
        o.write(info, 0, info.length);
        e.put("Index/Document.iwa", iwa(o.toByteArray()));
        final byte[] giant = zip(e);
        assertThrows(IOException.class, () -> read(giant));

        ByteArrayOutputStream h = new ByteArrayOutputStream();
        Pb.varint(h, 1L << 40);
        e.put("Index/Document.iwa", iwa(h.toByteArray()));
        final byte[] giantHeader = zip(e);
        assertThrows(IOException.class, () -> read(giantHeader));
    }

    @Test
    void oldAndEncryptedPackagesAreRefused() throws IOException {
        Map<String, byte[]> e = new LinkedHashMap<String, byte[]>();
        e.put("index.apxl", new byte[] {'<', '>'});
        final byte[] old = zip(e);
        IOException x = assertThrows(IOException.class, () -> read(old));
        assertTrue(x.getMessage().contains("Keynote 09"));

        e.clear();
        e.put("Index/Document.iwa", iwa(deck(5).raw()));
        e.put(".iwph", new byte[] {1});
        final byte[] enc = zip(e);
        x = assertThrows(IOException.class, () -> read(enc));
        assertTrue(x.getMessage().contains("encrypted"));
    }
}
