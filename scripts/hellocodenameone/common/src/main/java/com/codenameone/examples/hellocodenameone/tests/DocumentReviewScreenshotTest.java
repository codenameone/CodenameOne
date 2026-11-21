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

import com.codename1.spellcheck.GrammarChecker;
import com.codename1.spellcheck.HunspellDictionary;
import com.codename1.spellcheck.Proofreader;
import com.codename1.spellcheck.SpellChecker;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentEditor;
import com.codename1.wordprocessing.DocumentModel;
import com.codename1.wordprocessing.EmbeddedComponent;
import com.codename1.wordprocessing.EmbeddedComponents;
import com.codename1.wordprocessing.Field;
import com.codename1.wordprocessing.Note;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TableSheets;
import com.codename1.wordprocessing.TextPosition;
import com.codename1.wordprocessing.TextStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// A paged document under review: a heading, styled runs with a link and a footnote, bulleted
/// and numbered lists, a table whose total is a Word formula the spreadsheet engine computes, a
/// live slider embedded in the text, a tracked insertion and deletion, a review comment, and
/// spelling and grammar squiggles.
public class DocumentReviewScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=DocumentReview status=SKIPPED reason=document-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "DocumentReview";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    static Paragraph para(String id, String style, String list, RichText t) {
        return new Paragraph(id, style, Paragraph.LEFT, list, 0, 0, 0, 6, 0, 1.15f, false, false, t);
    }

    static List<Block> cell(String id, String text, boolean bold) {
        List<Block> b = new ArrayList<Block>();
        b.add(para(id, "Normal", Paragraph.NO_LIST, RichText.of(text, TextStyle.DEFAULT.withBold(bold))));
        return b;
    }

    static Document document() {
        TextStyle plain = TextStyle.DEFAULT;
        List<Block> note = new ArrayList<Block>();
        note.add(para("n1", "Normal", Paragraph.NO_LIST, RichText.of("Figures are in thousands.")));
        RichText intro = new RichText.Builder()
                .append("This quarterly documnet compares ", plain)
                .append("revenue", plain.withBold(true))
                .append(" by region, ", plain)
                .append("in detail", plain.withItalic(true))
                .append(", with sources on ", plain)
                .append("the wiki", plain.withLink("https://www.codenameone.com/"))
                .append(".", plain)
                .append(new Note("fn1", Note.FOOTNOTE, null, note), plain)
                .append(" Growth was was strong.", plain.withColor(0x1b7f3b))
                .build();
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        rows.add(Arrays.asList(cell("h0", "Region", true), cell("h1", "Q1", true), cell("h2", "Q2", true)));
        String[][] data = {{"North", "120", "135"}, {"South", "98", "110"}, {"West", "143", "151"}};
        for (int r = 0; r < data.length; r++) {
            rows.add(Arrays.asList(cell("c" + r + "0", data[r][0], false), cell("c" + r + "1", data[r][1], false),
                    cell("c" + r + "2", data[r][2], false)));
        }
        List<List<Block>> total = new ArrayList<List<Block>>();
        total.add(cell("s0", "Total", true));
        for (int c = 1; c <= 2; c++) {
            List<Block> b = new ArrayList<Block>();
            b.add(para("s" + c, "Normal", Paragraph.NO_LIST,
                    RichText.of(new Field("=SUM(ABOVE)", "", false), plain.withBold(true))));
            total.add(b);
        }
        rows.add(total);
        Table table = new Table("t1", rows, new ArrayList<Table.Merge>(), 1);
        RichText live = new RichText.Builder().append("Confidence: ", plain)
                .append(new EmbeddedComponent("cn1ss-slider", "70", 140, 22, null), plain).build();
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(para("title", "Heading1", Paragraph.NO_LIST, RichText.of("Quarterly Review")));
        blocks.add(para("intro", "Normal", Paragraph.NO_LIST, intro));
        blocks.add(para("b1", "Normal", Paragraph.BULLET, RichText.of("North led every month")));
        blocks.add(para("b2", "Normal", Paragraph.BULLET, RichText.of("West recovered after a slow start")));
        blocks.add(para("k1", "Normal", Paragraph.NUMBERED, RichText.of("Confirm the West numbers")));
        blocks.add(para("k2", "Normal", Paragraph.NUMBERED, RichText.of("Publish the summary")));
        blocks.add(table);
        blocks.add(para("live", "Normal", Paragraph.NO_LIST, live));
        blocks.add(para("end", "Normal", Paragraph.NO_LIST, RichText.of("Prepared by the finance team.")));
        return TableSheets.recalculate(new Document(blocks, null, "Quarterly Review", 100));
    }

    static Proofreader proofreader() {
        // a tiny dictionary keeps the squiggles the same on every platform
        String words = "this quarterly compares revenue by region in detail with sources on the wiki "
                + "growth was strong north led every month west recovered after a slow start confirm "
                + "numbers publish summary confidence prepared finance team figures are thousands "
                + "total review south";
        StringBuilder dic = new StringBuilder();
        String[] w = words.split(" ");
        dic.append(w.length).append('\n');
        for (String s : w) {
            dic.append(s).append('\n');
        }
        HunspellDictionary d = new HunspellDictionary("SET UTF-8\n", dic.toString());
        return new Proofreader(SpellChecker.create(d), GrammarChecker.english());
    }

    @Override
    protected void restoreAfterCapture() {
        // later tests in the runner must not see this test's component type
        EmbeddedComponents.register("cn1ss-slider", null);
    }

    @Override
    protected void populate(Form form, String suffix) {
        EmbeddedComponents.register("cn1ss-slider", new EmbeddedComponents.Factory() {
            @Override
            public Component create(EmbeddedComponent e) {
                Slider s = new Slider();
                s.setEditable(true);
                s.setProgress(Integer.parseInt(e.state));
                return s;
            }

            @Override
            public String state(Component c) {
                return c instanceof Slider ? String.valueOf(((Slider) c).getProgress()) : "70";
            }
        });
        DocumentModel model = new DocumentModel(document());
        model.setAuthor("Reviewer");
        model.setTrackChanges(true);
        model.insertText(new TextPosition("end", 12), "and reviewed ", null);
        model.delete(new TextPosition("b2", 4), new TextPosition("b2", 14));
        model.setTrackChanges(false);
        model.addComment(new TextPosition("k1", 12), new TextPosition("k1", 24), "Due Friday");
        DocumentEditor editor = new DocumentEditor(model);
        editor.getView().setProofreader(proofreader());
        editor.getView().setFitWidth(true);
        form.add(BorderLayout.CENTER, editor);
    }
}
