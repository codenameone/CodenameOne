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

import com.codename1.util.Base64;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Presentations used by the tests: one deck with every kind of element, which every format
/// test reads back and compares.
final class Decks {
    /// A 2x2 red PNG.
    static final byte[] PNG = Base64.decode(("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEklEQVR4"
            + "nGP4z8DAwMDAAAEFAQB6TwWXAAAAAElFTkSuQmCC").getBytes());

    private Decks() {
    }

    static Picture picture(String id) {
        return new Picture(id, PNG, "image/png", 40, 40, "red square");
    }

    static List<Block> text(String id, String s) {
        List<Block> l = new ArrayList<Block>();
        l.add(new Paragraph(id, RichText.of(s)));
        return l;
    }

    static ShapeElement title(String id, String text) {
        TextFrame f = TextFrame.of(id + "t", text, TextStyle.DEFAULT.withSize(36).withBold(true),
                Paragraph.CENTER).withAnchor(TextFrame.MIDDLE);
        return ShapeElement.textBox(id, 60, 30, 840, 90, f).withPlaceholder(ShapeElement.TITLE);
    }

    static ChartElement chart(String id, String kind) {
        return new ChartElement(id, kind, 80, 140, 400, 300,
                new String[] {"Q1", "Q2", "Q3", "Q4"}, new String[] {"North", "South"},
                new double[][] {{10, 14, 18, 22}, {8, 9, 15, 19}}).withTitle("Revenue");
    }

    /// Every element kind, a transition, builds and notes.
    static Presentation everything() {
        List<Slide> slides = new ArrayList<Slide>();
        List<SlideElement> e1 = new ArrayList<SlideElement>();
        e1.add(title("e1", "Quarterly review"));
        ShapeElement star = new ShapeElement("e2", "Star", "star5", 100, 160, 160, 160, 15, false,
                false, Fill.gradient(0xff8800, 0xffdd00, 45), new LineStyle(0x333333, 2), null,
                null, null);
        e1.add(star);
        e1.add(ShapeElement.line("e3", 300, 200, 600, 260,
                new LineStyle(0x1f6feb, 3).withArrows(LineStyle.ARROW_NONE,
                        LineStyle.ARROW_TRIANGLE).withDash(LineStyle.DASH)));
        e1.add(new PictureElement("e4", picture("p1"), 640, 160, 200, 200)
                .withCrop(0.1f, 0, 0.1f, 0));
        List<Build> b1 = new ArrayList<Build>();
        b1.add(Build.entrance("e2", Build.FADE));
        b1.add(Build.entrance("e3", Build.WIPE).withTrigger(Build.AFTER_PREVIOUS));
        slides.add(new Slide("s1", "title", Fill.solid(0xf4f6f8), e1,
                text("n1", "Open with the headline number."),
                Transition.of(Transition.PUSH, 600).withDirection(Transition.FROM_RIGHT), b1,
                false));
        List<SlideElement> e2 = new ArrayList<SlideElement>();
        e2.add(title("e5", "Numbers"));
        e2.add(chart("e6", ChartElement.COLUMN));
        e2.add(new TableElement("e7", Table.ofText("t1", new String[][] {{"Region", "Total"},
            {"North", "64"}, {"South", "51"}}), 520, 160, 360, 120));
        slides.add(new Slide("s2", null, Fill.NO_FILL, e2, null, Transition.of(Transition.FADE,
                400), null, false));
        List<SlideElement> e3 = new ArrayList<SlideElement>();
        e3.add(DiagramElement.process("e8", 60, 160, 840, 200, "Plan", "Build", "Ship"));
        List<SlideElement> kids = new ArrayList<SlideElement>();
        kids.add(new ShapeElement("e10", "ellipse", 100, 400, 80, 80, Fill.solid(0x2ecc71),
                LineStyle.NONE, null));
        kids.add(new ShapeElement("e11", "roundRect", 200, 400, 120, 80, Fill.solid(0x9b59b6),
                LineStyle.NONE, TextFrame.of("g", "Grouped", TextStyle.DEFAULT, Paragraph.LEFT)));
        e3.add(GroupElement.of("e9", kids));
        slides.add(new Slide("s3", null, Fill.gradient(0xffffff, 0xdde6f0, 90), e3, null,
                Transition.DEFAULT, null, true));
        return new Presentation(Presentation.WIDE_WIDTH, Presentation.WIDE_HEIGHT, slides,
                Theme.DEFAULT, "Deck & co", 100, null);
    }

    static List<SlideElement> list(SlideElement... e) {
        return new ArrayList<SlideElement>(Arrays.asList(e));
    }
}
