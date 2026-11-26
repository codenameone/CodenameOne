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
import com.codename1.presentation.Build;
import com.codename1.presentation.ChartElement;
import com.codename1.presentation.DiagramElement;
import com.codename1.presentation.Fill;
import com.codename1.presentation.LineStyle;
import com.codename1.presentation.Presentation;
import com.codename1.presentation.PresentationModel;
import com.codename1.presentation.ShapeElement;
import com.codename1.presentation.Slide;
import com.codename1.presentation.SlideElement;
import com.codename1.presentation.SlideShow;
import com.codename1.presentation.SlideView;
import com.codename1.presentation.TableElement;
import com.codename1.presentation.TextFrame;
import com.codename1.presentation.Theme;
import com.codename1.presentation.Transition;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// The presentation snippets the developer guide includes. Each method returns what its
/// snippet builds, and `PresentationFigures` photographs that return value.
public class PresentationSnippets {

    static ShapeElement title(String id, String text) {
        TextFrame f = TextFrame.of(id + "t", text, TextStyle.DEFAULT.withSize(34).withBold(true)
                .withColor(0x1b2a41), Paragraph.LEFT).withAnchor(TextFrame.MIDDLE);
        return ShapeElement.textBox(id, 50, 20, 860, 80, f);
    }

    static List<Block> notes(String s) {
        List<Block> l = new ArrayList<Block>();
        l.add(new Paragraph("n" + s.length(), RichText.of(s)));
        return l;
    }

    /// A slide of shapes, built in code.
    public Slide shapesSlide() {
        // tag::slides-shapes[]
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(title("a1", "Shapes and fills"));
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

        Slide slide = new Slide("s1", null, Fill.solid(0xf6f8fb), e,
                notes("Start with the shapes."), Transition.DEFAULT, builds, false);
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
        e.add(title("b1", "Charts"));
        e.add(new ChartElement("b2", ChartElement.COLUMN, 40, 100, 430, 210,
                quarters, regions, sales).withTitle("Revenue"));
        e.add(new ChartElement("b3", ChartElement.LINE, 490, 100, 430, 210,
                quarters, regions, sales).withTitle("Trend"));
        e.add(new ChartElement("b4", ChartElement.DOUGHNUT, 40, 320, 430, 210,
                quarters, regions, sales).withTitle("Share"));
        e.add(new ChartElement("b5", ChartElement.AREA, 490, 320, 430, 210,
                quarters, regions, sales).withTitle("Stacked").withStacked(true));
        Slide slide = new Slide("s2", null, Fill.solid(0xffffff), e, notes("The numbers."),
                Transition.of(Transition.PUSH, 700).withDirection(Transition.FROM_RIGHT),
                null, false);
        // end::slides-charts[]
        return slide;
    }

    /// A slide of diagrams and a table.
    public Slide diagramsSlide() {
        // tag::slides-diagrams[]
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(title("c1", "Diagrams"));
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
        Slide slide = new Slide("s3", null, Fill.gradient(0xffffff, 0xe3ebf5, 90), e,
                notes("How we work."), Transition.of(Transition.ZOOM, 600), null, false);
        // end::slides-diagrams[]
        return slide;
    }

    /// The three slides as a deck.
    public Presentation deck() {
        // tag::slides-deck[]
        List<Slide> slides = new ArrayList<Slide>();
        slides.add(shapesSlide());
        slides.add(chartsSlide());
        slides.add(diagramsSlide());
        Presentation deck = new Presentation(Presentation.WIDE_WIDTH,
                Presentation.WIDE_HEIGHT, slides, Theme.DEFAULT, "Sample deck", 1000, null);
        // end::slides-deck[]
        return deck;
    }

    /// Plays a deck.
    public Form show(Presentation deck, int slide) {
        // tag::slides-show[]
        Form form = new Form("Deck", new BorderLayout());
        SlideShow show = new SlideShow(deck);
        form.add(BorderLayout.CENTER, show);
        // end::slides-show[]
        show.goTo(slide);
        return form;
    }

    /// Plays a deck in presenter mode.
    public Form presenter(Presentation deck) {
        // tag::slides-presenter[]
        Form form = new Form("Presenter", new BorderLayout());
        SlideShow show = new SlideShow(deck);
        show.setPresenterMode(true);
        form.add(BorderLayout.CENTER, show);
        // end::slides-presenter[]
        return form;
    }

    /// Edits a deck.
    public Form editor(Presentation deck) {
        // tag::slides-editor[]
        PresentationModel model = new PresentationModel(deck);
        SlideView editor = new SlideView(model);
        Form form = new Form("Editor", new BorderLayout());
        form.add(BorderLayout.CENTER, editor);
        editor.setSelection("a3");
        // end::slides-editor[]
        return form;
    }

    /// Reads a file in any supported format and saves it as PowerPoint and PDF.
    public void convert(String path) throws IOException {
        // tag::slides-convert[]
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
        // end::slides-convert[]
    }

    /// Undoable edits through the model.
    public void edits(PresentationModel model) {
        // tag::slides-model[]
        model.beginGroup();
        model.addSlide(model.getSlideCount());
        int at = model.getSlideCount() - 1;
        String id = model.newId('e');
        model.addElement(at, title(id, "Next steps"));
        model.setTransition(at, Transition.of(Transition.FADE, 500));
        model.endGroup();
        // one step undoes the slide and everything on it
        model.undo();
        // end::slides-model[]
    }

    /// For the figures: a component of a form.
    static Component center(Form f) {
        return ((BorderLayout) f.getContentPane().getLayout()).getCenter();
    }
}
