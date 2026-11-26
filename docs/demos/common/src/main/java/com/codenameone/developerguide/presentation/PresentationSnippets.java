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
package com.codenameone.developerguide.presentation;

import com.codename1.io.FileSystemStorage;
import com.codename1.presentation.ChartElement;
import com.codename1.presentation.DiagramElement;
import com.codename1.presentation.Fill;
import com.codename1.presentation.LineStyle;
import com.codename1.presentation.Presentation;
import com.codename1.presentation.PresentationModel;
import com.codename1.presentation.ShapeElement;
import com.codename1.presentation.Slide;
import com.codename1.presentation.SlideAnimation;
import com.codename1.presentation.SlideElement;
import com.codename1.presentation.SlideRenderer;
import com.codename1.presentation.SlideShow;
import com.codename1.presentation.SlideView;
import com.codename1.presentation.TableElement;
import com.codename1.presentation.TextFrame;
import com.codename1.presentation.Theme;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
// tag::slides-imports[]
// These names are also classes elsewhere in Codename One (annotations.buildhints,
// ui.animations, ui.table, ui.editor), so they are imported by name.
import com.codename1.presentation.Build;
import com.codename1.presentation.Transition;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;
// end::slides-imports[]

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The presentation snippets the developer guide includes. Each method returns what its
/// snippet builds, and `PresentationFigures` photographs that return value.
public class PresentationSnippets {

    /// A slide of shapes, built in code.
    public Slide shapesSlide() {
        // tag::slides-shapes[]
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(ShapeElement.textBox("a1", 50, 20, 860, 80, TextFrame.of("a1t", "Shapes and fills",
                TextStyle.DEFAULT.withSize(34).withBold(true).withColor(0x1b2a41), Paragraph.LEFT)
                .withAnchor(TextFrame.MIDDLE)));
        e.add(new ShapeElement("a2", "Star", "star5", 70, 140, 220, 220, 0, false, false,
                Fill.gradient(0xff8a00, 0xffd84d, 45), new LineStyle(0x7a4500, 2),
                null, null, null));
        e.add(new ShapeElement("a3", "Card", "roundRect", 340, 150, 260, 140, 0, false, false,
                Fill.gradient(0x2f6fb3, 0x58b4e8, 90), LineStyle.NONE,
                TextFrame.of("a3t", "Gradient card", TextStyle.DEFAULT.withSize(22)
                        .withColor(0xffffff).withBold(true), Paragraph.CENTER)
                        .withAnchor(TextFrame.MIDDLE), null, null));
        e.add(new ShapeElement("a4", "Badge", "ellipse", 660, 140, 220, 220, 0, false, false,
                Fill.solid(0x3a8f5c), new LineStyle(0x1e5532, 4).withDash(LineStyle.DASH),
                null, null, null));
        e.add(ShapeElement.line("a5", 120, 470, 840, 470, new LineStyle(0x1b2a41, 3)
                .withArrows(LineStyle.ARROW_OVAL, LineStyle.ARROW_TRIANGLE)));

        // the star zooms in with the card; the badge fades in after them
        List<Build> builds = new ArrayList<Build>();
        builds.add(Build.entrance("a2", Build.ZOOM));
        builds.add(Build.entrance("a3", Build.FLY).withTrigger(Build.WITH_PREVIOUS));
        builds.add(Build.entrance("a4", Build.FADE).withTrigger(Build.AFTER_PREVIOUS));

        // the speaker notes are blocks, as in a document
        List<Block> notes = new ArrayList<Block>();
        notes.add(new Paragraph("n1", RichText.of("Start with the shapes.")));
        Slide slide = new Slide("s1", null, Fill.solid(0xf6f8fb), e, notes, Transition.DEFAULT,
                builds, false);
        // end::slides-shapes[]
        return slide;
    }

    /// A slide of charts.
    public Slide chartsSlide() {
        // tag::slides-charts[]
        String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
        String[] regions = {"North", "South"};
        double[][] sales = {{12, 17, 21, 26}, {9, 11, 16, 22}};
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(ShapeElement.textBox("b1", 50, 20, 860, 80, TextFrame.of("b1t", "Charts",
                TextStyle.DEFAULT.withSize(34).withBold(true).withColor(0x1b2a41), Paragraph.LEFT)
                .withAnchor(TextFrame.MIDDLE)));
        e.add(new ChartElement("b2", ChartElement.COLUMN, 40, 100, 430, 210,
                quarters, regions, sales).withTitle("Revenue"));
        e.add(new ChartElement("b3", ChartElement.LINE, 490, 100, 430, 210,
                quarters, regions, sales).withTitle("Trend"));
        e.add(new ChartElement("b4", ChartElement.DOUGHNUT, 40, 320, 430, 210,
                quarters, regions, sales).withTitle("Share"));
        e.add(new ChartElement("b5", ChartElement.AREA, 490, 320, 430, 210,
                quarters, regions, sales).withTitle("Stacked").withStacked(true));
        List<Block> notes = new ArrayList<Block>();
        notes.add(new Paragraph("n2", RichText.of("The numbers.")));
        Slide slide = new Slide("s2", null, Fill.solid(0xffffff), e, notes,
                Transition.of(Transition.PUSH, 700).withDirection(Transition.FROM_RIGHT),
                null, false);
        // end::slides-charts[]
        return slide;
    }

    /// A slide of diagrams and a table.
    public Slide diagramsSlide() {
        // tag::slides-diagrams[]
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(ShapeElement.textBox("c1", 50, 20, 860, 80, TextFrame.of("c1t", "Diagrams",
                TextStyle.DEFAULT.withSize(34).withBold(true).withColor(0x1b2a41), Paragraph.LEFT)
                .withAnchor(TextFrame.MIDDLE)));
        e.add(DiagramElement.process("c2", 40, 110, 880, 110,
                "Plan", "Design", "Build", "Ship"));

        List<DiagramElement.Node> nodes = new ArrayList<DiagramElement.Node>();
        nodes.add(new DiagramElement.Node("ceo", "CEO", "roundRect", 0x1b2a41));
        nodes.add(new DiagramElement.Node("cto", "CTO", "roundRect", 0x2f6fb3));
        nodes.add(new DiagramElement.Node("cfo", "CFO", "roundRect", 0x3a8f5c));
        nodes.add(new DiagramElement.Node("eng", "Engineering", "rect", 0x58b4e8));
        nodes.add(new DiagramElement.Node("ops", "Operations", "rect", 0x7cc49a));
        List<String[]> edges = new ArrayList<String[]>();
        edges.add(new String[] {"ceo", "cto"});
        edges.add(new String[] {"ceo", "cfo"});
        edges.add(new String[] {"cto", "eng"});
        edges.add(new String[] {"cfo", "ops"});
        e.add(new DiagramElement("c3", DiagramElement.HIERARCHY, 40, 250, 520, 270,
                nodes, edges, 0x2f6fb3, 0xffffff));

        e.add(new TableElement("c4", Table.ofText("t1", new String[][] {
            {"Stage", "Weeks"}, {"Plan", "2"}, {"Design", "3"}, {"Build", "6"}, {"Ship", "1"}}),
                600, 260, 320, 200));
        List<Block> notes = new ArrayList<Block>();
        notes.add(new Paragraph("n3", RichText.of("How we work.")));
        Slide slide = new Slide("s3", null, Fill.gradient(0xffffff, 0xe3ebf5, 90), e, notes,
                Transition.of(Transition.ZOOM, 600), null, false);
        // end::slides-diagrams[]
        return slide;
    }

    /// The three slides as a deck.
    public Presentation deck() {
        return deck(shapesSlide(), chartsSlide(), diagramsSlide());
    }

    // Each snippet that works on something it did not build is a whole method, so it
    // compiles on its own -- the way the guide's "Try it" button runs it -- with what it
    // works on as a parameter.

    // tag::slides-deck[]
    public Presentation deck(Slide shapes, Slide charts, Slide diagrams) {
        List<Slide> slides = new ArrayList<Slide>();
        slides.add(shapes);
        slides.add(charts);
        slides.add(diagrams);
        return new Presentation(Presentation.WIDE_WIDTH, Presentation.WIDE_HEIGHT, slides,
                Theme.DEFAULT, "Sample deck", 1000, null);
    }
    // end::slides-deck[]

    // tag::slides-show[]
    public Form show(Presentation deck) {
        Form form = new Form("Deck", new BorderLayout());
        SlideShow show = new SlideShow(deck);
        form.add(BorderLayout.CENTER, show);
        return form;
    }
    // end::slides-show[]

    // tag::slides-presenter[]
    public Form presenter(Presentation deck) {
        Form form = new Form("Presenter", new BorderLayout());
        SlideShow show = new SlideShow(deck);
        show.setPresenterMode(true);
        form.add(BorderLayout.CENTER, show);
        return form;
    }
    // end::slides-presenter[]

    // tag::slides-editor[]
    public Form editor(Presentation deck) {
        PresentationModel model = new PresentationModel(deck);
        SlideView editor = new SlideView(model);
        Form form = new Form("Editor", new BorderLayout());
        form.add(BorderLayout.CENTER, editor);
        editor.setSelection("a3");
        return form;
    }
    // end::slides-editor[]

    // tag::slides-frame[]
    public void paintHalfway(Graphics g, Presentation deck, Slide slide, int x, int y,
            float scale) {
        SlideRenderer renderer = new SlideRenderer();
        // 400 milliseconds into the slide's first build step
        long elapsed = 400;
        Map<String, SlideRenderer.ElementState> states = SlideAnimation.states(slide, 0, elapsed);
        renderer.paint(g, deck, slide, x, y, scale, states);
    }
    // end::slides-frame[]

    // tag::slides-convert[]
    public void convert(String path) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        Presentation deck;
        try (InputStream in = fs.openInputStream(path)) {
            // .pptx, .odp, .key, .ppt or .cn1slides, by the file's extension
            deck = Presentation.read(in, Presentation.Format.forFileName(path));
        }
        for (String warning : deck.getWarnings()) {
            System.out.println("While reading: " + warning);
        }
        try (OutputStream out = fs.openOutputStream(path + ".pptx")) {
            deck.write(out, Presentation.Format.PPTX);
        }
        try (OutputStream out = fs.openOutputStream(path + ".pdf")) {
            deck.write(out, Presentation.Format.PDF);
        }
    }
    // end::slides-convert[]

    // tag::slides-model[]
    public void edits(PresentationModel model) {
        model.beginGroup();
        model.addSlide(model.getSlideCount());
        int at = model.getSlideCount() - 1;
        String id = model.newId('e');
        model.addElement(at, ShapeElement.textBox(id, 50, 20, 860, 80, TextFrame.of(id + "t",
                "Next steps", TextStyle.DEFAULT.withSize(34).withBold(true), Paragraph.LEFT)));
        model.setTransition(at, Transition.of(Transition.FADE, 500));
        model.endGroup();
        // one step undoes the slide and everything on it
        model.undo();
    }
    // end::slides-model[]
}
