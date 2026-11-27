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

import com.codename1.presentation.Build;
import com.codename1.presentation.ChartElement;
import com.codename1.presentation.DiagramElement;
import com.codename1.presentation.Fill;
import com.codename1.presentation.LineStyle;
import com.codename1.presentation.Presentation;
import com.codename1.presentation.ShapeElement;
import com.codename1.presentation.Slide;
import com.codename1.presentation.SlideElement;
import com.codename1.presentation.TableElement;
import com.codename1.presentation.TextFrame;
import com.codename1.presentation.Theme;
import com.codename1.presentation.Transition;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;

import java.util.ArrayList;
import java.util.List;

/// The deck the presentation screenshot tests show: charts drawn by `com.codename1.ui.chart`,
/// diagrams arranged by `com.codename1.graph`, preset shapes with gradients and dashes, a
/// table, builds and transitions. Built in code so the tests need no resource file.
final class SampleSlides {
    private SampleSlides() {
    }

    static ShapeElement title(String id, String text) {
        TextFrame f = TextFrame.of(id + "t", text, TextStyle.DEFAULT.withSize(34).withBold(true)
                .withColor(0x1b2a41), Paragraph.LEFT).withAnchor(TextFrame.MIDDLE);
        return ShapeElement.textBox(id, 50, 20, 860, 80, f).withPlaceholder(ShapeElement.TITLE);
    }

    static ChartElement chart(String id, String kind, float x, float y, float w, float h,
            String title) {
        return new ChartElement(id, kind, x, y, w, h, new String[] {"Q1", "Q2", "Q3", "Q4"},
                new String[] {"North", "South"},
                new double[][] {{12, 17, 21, 26}, {9, 11, 16, 22}}).withTitle(title);
    }

    static List<Block> notes(String s) {
        List<Block> l = new ArrayList<Block>();
        l.add(new Paragraph("n" + s.length(), RichText.of(s)));
        return l;
    }

    /// Slide 1: shapes, gradients, a dashed connector and an arrow.
    static Slide shapes() {
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(title("a1", "Shapes and fills"));
        e.add(new ShapeElement("a2", "Star", "star5", 70, 140, 220, 220, 0, false, false,
                Fill.gradient(0xff8a00, 0xffd84d, 45), new LineStyle(0x7a4500, 2), null, null,
                null));
        e.add(new ShapeElement("a3", "Card", "roundRect", 340, 150, 260, 140, 0, false, false,
                Fill.gradient(0x2f6fb3, 0x58b4e8, 90), LineStyle.NONE,
                TextFrame.of("a3t", "Gradient card", TextStyle.DEFAULT.withSize(22)
                        .withColor(0xffffff).withBold(true), Paragraph.CENTER)
                        .withAnchor(TextFrame.MIDDLE), null, null));
        e.add(new ShapeElement("a4", "Badge", "ellipse", 660, 140, 220, 220, 0, false, false,
                Fill.solid(0x3a8f5c), new LineStyle(0x1e5532, 4).withDash(LineStyle.DASH),
                TextFrame.of("a4t", "Dashed", TextStyle.DEFAULT.withSize(24).withColor(0xffffff),
                        Paragraph.CENTER).withAnchor(TextFrame.MIDDLE), null, null));
        e.add(new ShapeElement("a5", "Arrow", "rightArrow", 340, 330, 260, 110, 0, false, false,
                Fill.solid(0xc7782a), LineStyle.NONE, null, null, null));
        e.add(ShapeElement.line("a6", 120, 470, 840, 470, new LineStyle(0x1b2a41, 3)
                .withArrows(LineStyle.ARROW_OVAL, LineStyle.ARROW_TRIANGLE)
                .withDash(LineStyle.DASH)));
        List<Build> b = new ArrayList<Build>();
        b.add(Build.entrance("a2", Build.ZOOM));
        b.add(Build.entrance("a3", Build.FLY).withTrigger(Build.WITH_PREVIOUS));
        b.add(Build.entrance("a4", Build.FADE).withTrigger(Build.AFTER_PREVIOUS));
        b.add(Build.entrance("a6", Build.WIPE).withTrigger(Build.AFTER_PREVIOUS));
        return new Slide("s1", null, Fill.solid(0xf6f8fb), e, notes("Shapes first."),
                Transition.DEFAULT, b, false);
    }

    /// Slide 2: four charts, one of each family.
    static Slide charts() {
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(title("b1", "Charts"));
        e.add(chart("b2", ChartElement.COLUMN, 40, 100, 430, 210, "Revenue"));
        e.add(chart("b3", ChartElement.LINE, 490, 100, 430, 210, "Trend"));
        e.add(chart("b4", ChartElement.DOUGHNUT, 40, 320, 430, 210, "Share"));
        e.add(chart("b5", ChartElement.AREA, 490, 320, 430, 210, "Stacked").withStacked(true));
        return new Slide("s2", null, Fill.solid(0xffffff), e, notes("The numbers."),
                Transition.of(Transition.PUSH, 700).withDirection(Transition.FROM_RIGHT), null,
                false);
    }

    /// Slide 3: diagrams laid out by the graph package, and a table.
    static Slide diagrams() {
        List<SlideElement> e = new ArrayList<SlideElement>();
        e.add(title("c1", "Diagrams"));
        e.add(DiagramElement.process("c2", 40, 110, 880, 110, "Plan", "Design", "Build",
                "Ship"));
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
        e.add(new DiagramElement("c3", DiagramElement.HIERARCHY, 40, 250, 520, 270, nodes,
                edges, 0x2f6fb3, 0xffffff));
        e.add(new TableElement("c4", Table.ofText("t1", new String[][] {{"Stage", "Weeks"},
            {"Plan", "2"}, {"Design", "3"}, {"Build", "6"}, {"Ship", "1"}}), 600, 260, 320,
                200));
        return new Slide("s3", null, Fill.gradient(0xffffff, 0xe3ebf5, 90), e,
                notes("How we work."), Transition.of(Transition.ZOOM, 600), null, false);
    }

    static Presentation deck() {
        List<Slide> s = new ArrayList<Slide>();
        s.add(shapes());
        s.add(charts());
        s.add(diagrams());
        return new Presentation(Presentation.WIDE_WIDTH, Presentation.WIDE_HEIGHT, s,
                Theme.DEFAULT, "Sample deck", 1000, null);
    }
}
