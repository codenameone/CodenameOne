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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.util.ImageIO;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentView;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Paragraph and page layout: every alignment including justified text, indents and line
/// spacing, nested lists that restart, superscript and subscript, an inline picture, a table
/// with merged cells across rows and columns, and a page break before a kept-together heading.
/// The view scrolls to show where the first page ends and the second begins.
public class DocumentLayoutScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=DocumentLayout status=SKIPPED reason=document-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "DocumentLayout";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    private static final String LONG = "Codename One lays a document out the same way on every "
            + "platform, so a paragraph breaks into the same lines on a phone, a desktop and in the "
            + "exported PDF. Justified text stretches the spaces of every line but the last.";

    static Paragraph p(String id, String align, float indent, RichText t) {
        return new Paragraph(id, "Normal", align, Paragraph.NO_LIST, 0, 0, 0, 6, indent, 1.15f, false,
                false, t);
    }

    static Paragraph item(String id, String kind, int level, String text) {
        return new Paragraph(id, "Normal", Paragraph.LEFT, kind, level, 0, 0, 2, 0, 1.15f, false, false,
                RichText.of(text));
    }

    static List<Block> cell(String id, String text, boolean bold, String align) {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph(id, "Normal", align, Paragraph.NO_LIST, 0, 0, 0, 0, 0, 1f, false, false,
                RichText.of(text, TextStyle.DEFAULT.withBold(bold))));
        return b;
    }

    /// A small bar chart, drawn and encoded at run time so the test needs no resource file.
    static byte[] chart() {
        Image img = Image.createImage(120, 72, 0xfff4f6f8);
        Graphics g = img.getGraphics();
        int[] bars = {30, 52, 41, 64};
        int[] colors = {0x2f6fb3, 0x3a8f5c, 0xc7782a, 0x8e44ad};
        for (int i = 0; i < bars.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(10 + i * 27, 68 - bars[i], 20, bars[i]);
        }
        g.setColor(0x333333);
        g.drawLine(4, 68, 116, 68);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.getImageIO().save(img, out, ImageIO.FORMAT_PNG, 1);
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
        return out.toByteArray();
    }

    static Document document() {
        TextStyle plain = TextStyle.DEFAULT;
        TextStyle h = plain.withBold(true).withSize(18);
        List<Block> b = new ArrayList<Block>();
        b.add(p("h1", Paragraph.LEFT, 0, RichText.of("Layout", h)));
        b.add(p("j", Paragraph.JUSTIFY, 0, RichText.of(LONG)));
        b.add(p("c", Paragraph.CENTER, 0, RichText.of("A centered line", plain.withItalic(true))));
        b.add(p("r", Paragraph.RIGHT, 0, RichText.of("A right aligned line")));
        b.add(p("ind", Paragraph.LEFT, 36, RichText.of("An indented paragraph with double line spacing "
                + "that wraps onto a second line to show the spacing between them.")).withSpacing(0, 6, 2f));
        b.add(p("sc", Paragraph.LEFT, 0, new RichText.Builder()
                .append("Water is H", plain).append("2", plain.withScript(TextStyle.SUBSCRIPT))
                .append("O and E = mc", plain).append("2", plain.withScript(TextStyle.SUPERSCRIPT))
                .append(", in ", plain).append("monospace", plain.withFont("Courier New"))
                .append(" and ", plain).append("highlighted", plain.withBackground(0xfff2a8))
                .append(" text.", plain).build()));
        b.add(p("pic", Paragraph.LEFT, 0, new RichText.Builder()
                .append("Sales by quarter ", plain)
                .append(new Picture("chart", chart(), "image/png", 120, 72, "Sales chart"), plain)
                .append(" sits on the line like a large glyph.", plain).build()));
        b.add(item("l1", Paragraph.NUMBERED, 0, "Plan"));
        b.add(item("l2", Paragraph.BULLET, 1, "Gather requirements"));
        b.add(item("l3", Paragraph.BULLET, 1, "Agree on scope"));
        b.add(item("l4", Paragraph.NUMBERED, 2, "Write it down"));
        b.add(item("l5", Paragraph.NUMBERED, 0, "Build"));
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        rows.add(Arrays.asList(cell("a0", "Quarter", true, Paragraph.LEFT),
                cell("a1", "Revenue", true, Paragraph.CENTER), cell("a2", "", true, Paragraph.LEFT)));
        rows.add(Arrays.asList(cell("b0", "", true, Paragraph.LEFT),
                cell("b1", "Plan", true, Paragraph.RIGHT), cell("b2", "Actual", true, Paragraph.RIGHT)));
        rows.add(Arrays.asList(cell("c0", "Q1", false, Paragraph.LEFT),
                cell("c1", "1,200", false, Paragraph.RIGHT), cell("c2", "1,310", false, Paragraph.RIGHT)));
        rows.add(Arrays.asList(cell("d0", "Q2 and Q3", false, Paragraph.LEFT),
                cell("d1", "2,600", false, Paragraph.RIGHT), cell("d2", "2,480", false, Paragraph.RIGHT)));
        rows.add(Arrays.asList(cell("e0", "", false, Paragraph.LEFT),
                cell("e1", "Q3 merged into Q2", false, Paragraph.CENTER), cell("e2", "", false, Paragraph.LEFT)));
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        merges.add(new Table.Merge(0, 0, 2, 1));
        merges.add(new Table.Merge(0, 1, 1, 2));
        merges.add(new Table.Merge(3, 0, 2, 1));
        merges.add(new Table.Merge(4, 1, 1, 2));
        b.add(new Table("t", rows, merges, 2));
        b.add(p("h2", Paragraph.LEFT, 0, RichText.of("Second page", h)).withPageBreak(true).withKeepNext(true));
        b.add(p("end", Paragraph.LEFT, 0, RichText.of("A page break put this heading at the top of a "
                + "new page.")));
        return new Document(b, null, "Layout", 100);
    }

    @Override
    protected void populate(Form form, String suffix) {
        DocumentView v = new DocumentView(document());
        v.setEditable(false);
        v.setFitWidth(true);
        form.add(BorderLayout.CENTER, v);
    }
}
