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
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reads `.ppt` files assembled here record by record, since no real fixture exists.
class PptReaderTest extends UITestBase {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4};

    // ------------------------------------------------------------ record builder

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.write(p, 0, p.length);
        }
        return out.toByteArray();
    }

    private static byte[] le(int bytes, long v) {
        byte[] b = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            b[i] = (byte) (v >> (8 * i));
        }
        return b;
    }

    private static byte[] u16(int v) {
        return le(2, v);
    }

    private static byte[] u32(long v) {
        return le(4, v);
    }

    private static byte[] rec(int ver, int inst, int type, byte[] body) {
        return cat(u16(ver | inst << 4), u16(type), u32(body.length), body);
    }

    private static byte[] atom(int type, byte[] body) {
        return rec(0, 0, type, body);
    }

    private static byte[] box(int type, byte[]... kids) {
        return rec(0xF, 0, type, cat(kids));
    }

    private static byte[] chars(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            out.write(s.charAt(i));
            out.write(s.charAt(i) >> 8);
        }
        return atom(0x0FA0, out.toByteArray());
    }

    private static byte[] header(int textType) {
        return atom(0x0F9F, u32(textType));
    }

    private static byte[] anchor(int left, int top, int right, int bottom) {
        return atom(0xF010, cat(u16(top), u16(left), u16(right), u16(bottom)));
    }

    private static byte[] fsp(int shapeType, int flags) {
        return rec(2, shapeType, 0xF00A, cat(u32(1024), u32(flags)));
    }

    private static byte[] fopt(int[][] props) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int[] p : props) {
            byte[] b = cat(u16(p[0]), u32(p[1]));
            out.write(b, 0, b.length);
        }
        return rec(3, props.length, 0xF00B, out.toByteArray());
    }

    private static byte[] groupShape() {
        return box(0xF004, atom(0xF009, cat(u32(0), u32(0), u32(0), u32(0))), fsp(0, 0x5));
    }

    private static byte[] drawing(byte[]... shapes) {
        return box(0x040C, box(0xF002, atom(0xF008, cat(u32(4), u32(1024))),
                box(0xF003, groupShape(), cat(shapes))));
    }

    private static byte[] titleStyle() {
        byte[] para = cat(u32(12), u16(0), u32(0x800), u16(1));
        byte[] run1 = cat(u32(5), u32(0x1 | 0x20000 | 0x40000), u16(1), u16(40),
                new byte[]{0x11, 0x22, 0x33, (byte) 0xFE});
        byte[] run2 = cat(u32(7), u32(0x2), u16(2));
        return atom(0x0FA1, cat(para, run1, run2));
    }

    private static byte[] slideAtom(int notesRef) {
        return atom(0x03EF, cat(u32(0), new byte[8], u32(0), u32(notesRef), u16(0), u16(0)));
    }

    private static byte[] showInfo(int effect, int dir, int flags) {
        return atom(0x03F9, cat(u32(0), u32(0), new byte[]{(byte) dir, (byte) effect}, u16(flags),
                new byte[]{1, 0, 0, 0}));
    }

    private static byte[] persistAtom(int ref) {
        return atom(0x03F3, cat(u32(ref), u32(0), u32(0), u32(256 + ref), u32(0)));
    }

    /// Options for the hostile variants.
    private static final class Opts {
        boolean cyclic;
        int nesting;
        boolean badLength;
        boolean encrypted;
    }

    private static Map<String, byte[]> streams(Opts o) {
        byte[] font = new byte[68];
        String face = "Tahoma";
        for (int i = 0; i < face.length(); i++) {
            font[i * 2] = (byte) face.charAt(i);
        }
        byte[] fbse = cat(new byte[]{6, 6}, new byte[16], u16(0xff), u32(8 + 16 + 1 + PNG.length),
                u32(1), u32(0), new byte[4]);
        byte[] document = box(0x03E8,
                atom(0x03E9, cat(u32(5760), u32(4320), u32(4320), u32(5760), new byte[24])),
                box(0x03F2, box(0x07D5, atom(0x0FB7, font))),
                box(0x040B, box(0xF000, rec(0xF, 1, 0xF001, rec(2, 6, 0xF007, fbse)))),
                box(0x0FF0, persistAtom(2), header(0), chars("Hello World"), persistAtom(3)));
        if (o.badLength) {
            System.arraycopy(u32(0x7ffff), 0, document, 4, 4);
        }
        byte[] title = box(0xF004, fsp(1, 0xA00), anchor(400, 200, 5360, 1000),
                box(0xF011, atom(0x0BC3, cat(u32(0), new byte[]{0x0F, 0, 0, 0}))),
                box(0xF00D, header(0), chars("Hello World"), titleStyle()));
        byte[] ellipse = box(0xF004, fsp(3, 0xA00),
                fopt(new int[][]{{0x0181, 0x0000FF}, {0x01BF, 0x100010}, {0x01C0, 0x00FF00},
                    {0x01CB, 25400}}), anchor(800, 1600, 2400, 3200));
        byte[] picture = box(0xF004, fsp(75, 0xA00), fopt(new int[][]{{0x0104, 1}}),
                anchor(3200, 1600, 4800, 3200));
        byte[] deep = new byte[0];
        if (o.nesting > 0) {
            byte[] inner = box(0xF004, fsp(1, 0xA00), anchor(0, 0, 80, 80));
            for (int i = 0; i < o.nesting; i++) {
                inner = box(0xF003, groupShape(), inner);
            }
            deep = inner;
        }
        byte[] slide1 = box(0x03EE, slideAtom(4), showInfo(20, 0, 0x1),
                drawing(title, ellipse, picture, deep));
        byte[] rect = box(0xF004, fsp(1, 0xA00), anchor(80, 80, 800, 800));
        byte[] slide2 = box(0x03EE, slideAtom(0), showInfo(0, 0, 0x5), drawing(rect));
        byte[] notes = box(0x03F0, atom(0x03F1, cat(u32(256 + 2), u32(0))), header(2),
                chars("Speaker notes"));
        int o1 = 0;
        int o2 = document.length;
        int o3 = o2 + slide1.length;
        int o4 = o3 + slide2.length;
        int dir = o4 + notes.length;
        byte[] persist = atom(0x1772, cat(u32(1 | 4 << 20), u32(o1), u32(o2), u32(o3), u32(o4)));
        int edit = dir + persist.length;
        byte[] userEdit = atom(0x0FF5, cat(u32(256 + 2), u16(0), new byte[]{0, 3},
                u32(o.cyclic ? edit : 0), u32(dir), u32(1), u32(5), u16(1), u16(0)));
        byte[] ppt = cat(document, slide1, slide2, notes, persist, userEdit);
        byte[] cu = atom(0x0FF6, cat(u32(0x14), u32(o.encrypted ? 0xF3D1C4DFL : 0xE391C05FL),
                u32(edit), u16(0), u16(0x3F4), new byte[]{3, 0}, u16(0)));
        byte[] pics = rec(0, 0x6E0, 0xF01E, cat(new byte[17], PNG));
        Map<String, byte[]> m = new HashMap<String, byte[]>();
        m.put("PowerPoint Document", ppt);
        m.put("Current User", cu);
        m.put("Pictures", pics);
        return m;
    }

    private static Presentation read(Map<String, byte[]> streams) throws IOException {
        return Presentation.read(new ByteArrayInputStream(CompoundFileWriter.write(streams)),
                Presentation.Format.PPT);
    }

    // ------------------------------------------------------------ tests

    @Test
    void readsSlidesShapesTextAndPictures() throws IOException {
        Presentation p = read(streams(new Opts()));
        assertEquals(720f, p.width, 0.01f);
        assertEquals(540f, p.height, 0.01f);
        assertEquals(2, p.slides.size());
        Slide s1 = p.slides.get(0);
        assertEquals(3, s1.elements.size());

        ShapeElement title = (ShapeElement) s1.elements.get(0);
        assertEquals(ShapeElement.TITLE, title.placeholder);
        assertEquals(50f, title.x, 0.01f);
        assertEquals(25f, title.y, 0.01f);
        assertEquals(620f, title.width, 0.01f);
        assertEquals("Hello World", title.text.plainText().trim());
        Paragraph para = (Paragraph) title.text.blocks.get(0);
        assertEquals(Paragraph.CENTER, para.align);
        RichText rt = para.text;
        TextStyle hello = rt.styleAt(0);
        assertTrue(hello.bold);
        assertEquals(40f, hello.size, 0.01f);
        assertEquals(0x112233, hello.color);
        assertEquals("Tahoma", hello.font);
        TextStyle world = rt.styleAt(7);
        assertTrue(world.italic);
        assertFalse(world.bold);

        ShapeElement ellipse = (ShapeElement) s1.elements.get(1);
        assertEquals("ellipse", ellipse.geometry);
        assertEquals(Fill.SOLID, ellipse.fill.kind);
        assertEquals(0xff0000, ellipse.fill.color);
        assertNotNull(ellipse.line);
        assertEquals(0x00ff00, ellipse.line.color);
        assertEquals(2f, ellipse.line.width, 0.01f);
        assertEquals(100f, ellipse.x, 0.01f);
        assertEquals(200f, ellipse.width, 0.01f);

        PictureElement pic = (PictureElement) s1.elements.get(2);
        assertEquals("image/png", pic.picture.mimeType);
        assertEquals(PNG.length, pic.picture.dataLength());
        assertEquals(200f, pic.width, 0.01f);

        assertNotNull(s1.notes);
        assertEquals("Speaker notes", ((Paragraph) s1.notes.get(0)).plainText());
        assertNotNull(s1.transition);
        assertEquals(Transition.PUSH, s1.transition.type);
        assertFalse(s1.hidden);

        Slide s2 = p.slides.get(1);
        assertTrue(s2.hidden);
        assertEquals(1, s2.elements.size());
        assertEquals("rect", ((ShapeElement) s2.elements.get(0)).geometry);
    }

    @Test
    void refusesTruncatedStream() {
        final Map<String, byte[]> m = streams(new Opts());
        byte[] d = m.get("PowerPoint Document");
        byte[] cut = new byte[d.length / 2];
        System.arraycopy(d, 0, cut, 0, cut.length);
        m.put("PowerPoint Document", cut);
        assertThrows(IOException.class, () -> read(m));
    }

    @Test
    void refusesCyclicUserEdits() {
        Opts o = new Opts();
        o.cyclic = true;
        IOException e = assertThrows(IOException.class, () -> read(streams(o)));
        assertTrue(e.getMessage().contains("cyclic"), e.getMessage());
    }

    @Test
    void refusesRecordPastContainer() {
        Opts o = new Opts();
        o.badLength = true;
        assertThrows(IOException.class, () -> read(streams(o)));
    }

    @Test
    void refusesDeepNesting() {
        Opts o = new Opts();
        o.nesting = 60;
        assertThrows(IOException.class, () -> read(streams(o)));
    }

    @Test
    void readsShallowGroups() throws IOException {
        Opts o = new Opts();
        o.nesting = 2;
        Presentation p = read(streams(o));
        assertEquals(4, p.slides.get(0).elements.size());
        assertTrue(p.slides.get(0).elements.get(3) instanceof GroupElement);
    }

    @Test
    void refusesEncrypted() {
        Opts o = new Opts();
        o.encrypted = true;
        IOException e = assertThrows(IOException.class, () -> read(streams(o)));
        assertTrue(e.getMessage().contains("encrypted"), e.getMessage());
    }

    @Test
    void aQuarterTurnedAnchorIsSwappedBack() {
        float[] r = PptReader.upright(new float[] {100, 100, 50, 200}, 90);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] {25, 175, 200, 50}, r, 0);
        float[] s = PptReader.upright(new float[] {100, 100, 50, 200}, 30);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] {100, 100, 50, 200}, s, 0);
    }
}
