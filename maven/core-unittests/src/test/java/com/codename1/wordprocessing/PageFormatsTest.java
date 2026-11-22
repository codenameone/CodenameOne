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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Floating pictures, sections, columns, headers, footers, tab stops and indents survive a
/// write and read in every format that can hold them.
class PageFormatsTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13,
        'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0};

    private static Document sample() {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p1", RichText.of("Intro text")).withIndents(36, -18, 24)
                .withTabs(Arrays.asList(new TabStop(144, TabStop.RIGHT, '.'),
                        new TabStop(72, TabStop.CENTER, (char) 0))));
        b.add(new Picture("i1", PNG, "image/png", 60, 40, "fig")
                .withWrap(Picture.SQUARE, Picture.RIGHT, 12, 6));
        b.add(new Paragraph("p2", RichText.of("Wrapped body")));
        b.add(new SectionBreak("s1", PageSpec.A4.withColumns(2, 24), false));
        b.add(new Paragraph("p3", RichText.of("Two column text")));
        b.add(new SectionBreak("s2", PageSpec.A4.rotated(), true));
        b.add(new Paragraph("p4", RichText.of("Landscape")));
        Document d = new Document(b, PageSpec.A4, "Pages", 10);
        List<Block> head = new ArrayList<Block>();
        head.add(new Paragraph("h1", RichText.of("Running head")));
        List<Block> foot = new ArrayList<Block>();
        foot.add(new Paragraph("f1", RichText.of("Running foot")));
        List<Block> first = new ArrayList<Block>();
        first.add(new Paragraph("h2", RichText.of("Cover head")));
        return d.withHeaders(head, foot, first, null);
    }

    private static Document roundTrip(Document d, Document.Format f) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, f);
        return Document.read(new ByteArrayInputStream(out.toByteArray()), f);
    }

    private static String text(List<Block> blocks) {
        StringBuilder s = new StringBuilder();
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                s.append(((Paragraph) b).text.toString());
            }
        }
        return s.toString();
    }

    private static void check(Document back) {
        Paragraph p1 = null;
        Picture pic = null;
        List<SectionBreak> breaks = new ArrayList<SectionBreak>();
        for (Block b : back.blocks) {
            if (b instanceof Paragraph && p1 == null) {
                p1 = (Paragraph) b;
            } else if (b instanceof Picture) {
                pic = (Picture) b;
            } else if (b instanceof SectionBreak) {
                breaks.add((SectionBreak) b);
            }
        }
        assertNotNull(p1);
        assertEquals(36, p1.indent, 0.6);
        assertEquals(-18, p1.firstLine, 0.6);
        assertEquals(24, p1.rightIndent, 0.6);
        assertEquals(2, p1.tabs.size());
        assertEquals(72, p1.tabs.get(0).position, 0.6);
        assertEquals(TabStop.CENTER, p1.tabs.get(0).alignment);
        assertEquals(TabStop.RIGHT, p1.tabs.get(1).alignment);
        assertEquals('.', p1.tabs.get(1).leader);
        assertNotNull(pic, "the floating picture");
        assertEquals(Picture.SQUARE, pic.wrap);
        assertEquals(Picture.RIGHT, pic.align);
        assertEquals(12, pic.offsetY, 0.6);
        assertEquals(2, breaks.size(), "breaks: " + breaks.size());
        assertTrue(!breaks.get(0).newPage);
        assertEquals(2, breaks.get(0).page.columns);
        assertEquals(24, breaks.get(0).page.columnGap, 0.6);
        assertTrue(breaks.get(1).newPage);
        assertTrue(breaks.get(1).page.width > breaks.get(1).page.height);
        assertTrue(text(back.blocks).contains("Landscape"));
        assertEquals("Running head", text(back.header));
        assertEquals("Running foot", text(back.footer));
        assertNotNull(back.firstHeader);
        assertEquals("Cover head", text(back.firstHeader));
    }

    @Test
    void nativeKeepsEverything() throws IOException {
        Document d = sample();
        Document back = roundTrip(d, Document.Format.NATIVE);
        check(back);
        assertEquals(d, back);
    }

    @Test
    void docxKeepsPageFeatures() throws IOException {
        check(roundTrip(sample(), Document.Format.DOCX));
    }

    @Test
    void odtKeepsPageFeatures() throws IOException {
        Document back = roundTrip(sample(), Document.Format.ODT);
        // ODT closes a continuous section with a second break back to the page around it
        List<Block> b = new ArrayList<Block>();
        int seen = 0;
        for (Block x : back.blocks) {
            if (x instanceof SectionBreak && !((SectionBreak) x).newPage && seen++ > 0) {
                continue;
            }
            b.add(x);
        }
        check(new Document(b, back.page, back.title, back.nextId)
                .withHeaders(back.header, back.footer, back.firstHeader, back.firstFooter));
    }

    @Test
    void docxPartsAreAllPresent() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sample().write(out, Document.Format.DOCX);
        java.util.zip.ZipInputStream z = new java.util.zip.ZipInputStream(
                new ByteArrayInputStream(out.toByteArray()));
        java.util.Map<String, String> parts = new java.util.HashMap<String, String>();
        for (java.util.zip.ZipEntry e = z.getNextEntry(); e != null; e = z.getNextEntry()) {
            ByteArrayOutputStream p = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n = z.read(buf); n > 0; n = z.read(buf)) {
                p.write(buf, 0, n);
            }
            parts.put(e.getName(), new String(p.toByteArray(), "UTF-8"));
        }
        String types = parts.get("[Content_Types].xml");
        for (String name : parts.keySet()) {
            if (name.endsWith(".rels")) {
                String rels = parts.get(name);
                String dir = name.startsWith("word/") ? "word/" : "";
                for (int i = rels.indexOf("Target=\""); i >= 0; i = rels.indexOf("Target=\"", i + 1)) {
                    int end = rels.indexOf('"', i + 8);
                    String target = rels.substring(i + 8, end);
                    if (rels.substring(Math.max(0, i - 80), i).contains("External")
                            || target.startsWith("http")) {
                        continue;
                    }
                    String full = target.startsWith("/") ? target.substring(1) : dir + target;
                    assertTrue(parts.containsKey(full), name + " names a missing " + full);
                }
            }
            if (name.startsWith("word/header") || name.startsWith("word/footer")) {
                if (name.endsWith(".xml")) {
                    assertTrue(types.contains("/" + name), "no content type for " + name);
                }
            }
        }
    }
}
