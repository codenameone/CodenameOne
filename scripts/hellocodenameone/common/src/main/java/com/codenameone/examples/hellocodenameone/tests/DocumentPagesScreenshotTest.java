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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentView;
import com.codename1.wordprocessing.Field;
import com.codename1.wordprocessing.PageSpec;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.SectionBreak;
import com.codename1.wordprocessing.TabStop;
import com.codename1.wordprocessing.TextStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Page furniture and page geometry: a running header with a page field and a footer, a
/// table of contents whose page numbers sit on a right tab stop behind a dot leader, a
/// picture floating at the right with text wrapping around it, a hanging indent, and a
/// continuous section set in two balanced columns before one column resumes.
public class DocumentPagesScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=DocumentPages status=SKIPPED reason=document-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "DocumentPages";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    private static final String WRAP = "The chart floats at the right edge of the column and the "
            + "text flows around it, line by line, keeping a small gap from the picture. Once the "
            + "text passes the bottom of the picture the lines take the whole width again, as they "
            + "do in a word processor. Nothing here is a table or a fixed layout: the paragraph "
            + "is broken into lines that are each as wide as the space beside the float.";

    private static final String COLUMNS = "A continuous section break changes the page in the "
            + "middle of it. These paragraphs are set in two columns that the layout balances, so "
            + "both end at about the same height and the single column that follows starts right "
            + "below them rather than on a new page. Columns are a property of the section, so "
            + "the same document opens with the same columns in Word and in LibreOffice.";

    static Paragraph p(String id, RichText t) {
        return new Paragraph(id, "Normal", Paragraph.LEFT, Paragraph.NO_LIST, 0, 0, 0, 6, 0, 1.15f,
                false, false, t);
    }

    static Paragraph toc(String id, String title, String page) {
        return p(id, RichText.of(title + "\t" + page))
                .withTabs(Arrays.asList(new TabStop(300, TabStop.RIGHT, '.')));
    }

    static Document document() {
        TextStyle plain = TextStyle.DEFAULT;
        TextStyle h = plain.withBold(true).withSize(18);
        TextStyle small = plain.withSize(9).withColor(0x666666);
        List<Block> b = new ArrayList<Block>();
        b.add(p("h1", RichText.of("Field notes", h)));
        b.add(toc("t1", "Floating pictures", "1"));
        b.add(toc("t2", "Hanging indents", "1"));
        b.add(toc("t3", "Columns", "2"));
        b.add(new Picture("chart", DocumentLayoutScreenshotTest.chart(), "image/png", 120, 72,
                "Sales chart").withWrap(Picture.SQUARE, Picture.RIGHT, 0, 8));
        b.add(p("w", RichText.of(WRAP)).withAlign(Paragraph.JUSTIFY));
        b.add(p("hang", new RichText.Builder().append("Hanging indent. ", plain.withBold(true))
                .append("The first line starts at the margin and every following line is indented, "
                        + "the way a bibliography or a definition list is set.", plain).build())
                .withIndents(36, -36, 0));
        TextStyle code = plain.withFont("Courier New").withSize(10).withBackground(0xf2f4f7);
        b.add(p("code", RichText.of("int total = 0;\nfor (int v : values) {\n    total += v;\n}",
                code)));
        b.add(new SectionBreak("s1", PageSpec.A4.withColumns(2, 18), false));
        b.add(p("c1", RichText.of(COLUMNS)));
        b.add(p("c2", RichText.of("The second paragraph continues in whichever column has room.")));
        b.add(new SectionBreak("s2", PageSpec.A4, false));
        b.add(p("after", RichText.of("One column again, below the balanced columns.")));
        List<Block> head = new ArrayList<Block>();
        head.add(p("hd", new RichText.Builder().append("Field notes - page ", small)
                .append(new Field("PAGE", "1", false), small).append(" of ", small)
                .append(new Field("NUMPAGES", "1", false), small).build())
                .withAlign(Paragraph.RIGHT));
        List<Block> foot = new ArrayList<Block>();
        foot.add(p("ft", RichText.of("Codename One word processing", small))
                .withAlign(Paragraph.CENTER));
        return new Document(b, PageSpec.A4, "Pages", 100).withHeaders(head, foot, null, null);
    }

    @Override
    protected void populate(Form form, String suffix) {
        DocumentView v = new DocumentView(document());
        v.setEditable(false);
        v.setFitWidth(true);
        form.add(BorderLayout.CENTER, v);
    }
}
