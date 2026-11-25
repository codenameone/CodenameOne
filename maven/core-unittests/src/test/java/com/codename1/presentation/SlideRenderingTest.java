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
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Painting slides, PDF export and the timing of builds.
class SlideRenderingTest extends UITestBase {
    @Test
    void everySlideRendersWithoutError() {
        Presentation p = Decks.everything();
        SlideRenderer r = new SlideRenderer();
        for (Slide s : p.slides) {
            Image img = r.toImage(p, s, 480);
            assertEquals(480, img.getWidth());
            assertEquals(270, img.getHeight());
        }
    }

    @Test
    void pdfHasOnePagePerVisibleSlide() throws IOException {
        Presentation p = Decks.everything();
        byte[] pdf = p.toBytes(Presentation.Format.PDF);
        String s = new String(pdf, "ISO-8859-1");
        assertTrue(s.startsWith("%PDF-"));
        assertEquals(2, SlidePdfExport.toPdf(p).getPageCount(), "the hidden slide is left out");
        assertTrue(s.contains("/MediaBox [0 0 960 540]") || s.contains("/MediaBox[0 0 960 540]"),
                "pages are slide sized");
    }

    @Test
    void everyPresetHasAnOutline() {
        for (String name : ShapeGeometry.presets()) {
            float[] b = ShapeGeometry.path(name, 100, 50).getBounds2D();
            assertTrue(b[2] > 0 || b[3] > 0, name);
            assertTrue(ShapeGeometry.isKnown(name));
        }
        assertFalse(ShapeGeometry.isKnown("noSuchShape"));
        assertTrue(ShapeGeometry.isLine("straightConnector1"));
        assertNotNull(ShapeGeometry.arrowHead(LineStyle.ARROW_TRIANGLE, 10, 10, 0, 0, 6));
    }

    @Test
    void buildsPlayInSteps() {
        Slide s = Decks.everything().slides.get(0);
        assertEquals(1, SlideAnimation.clickSteps(s));
        // before the click, both built elements are hidden
        Map<String, SlideRenderer.ElementState> before = SlideAnimation.states(s, 0, 100000);
        assertTrue(before.get("e2").hidden);
        assertTrue(before.get("e3").hidden);
        // halfway through the fade the star is half visible and the wipe has not started
        Build fade = s.builds.get(0);
        Map<String, SlideRenderer.ElementState> mid = SlideAnimation.states(s, 1,
                fade.duration / 2);
        assertFalse(mid.get("e2").hidden);
        assertEquals(0.5f, mid.get("e2").alpha, 0.01f);
        assertTrue(mid.get("e3").hidden, "the wipe waits for the fade");
        Map<String, SlideRenderer.ElementState> done = SlideAnimation.states(s, 1, 100000);
        assertEquals(1, done.get("e2").alpha, 0);
        assertEquals(1, done.get("e3").reveal, 0);
        assertEquals(fade.duration + s.builds.get(1).duration,
                SlideAnimation.stepDuration(s, 1));
    }

    @Test
    void transitionFramesPaint() {
        Presentation p = Decks.everything();
        SlideRenderer r = new SlideRenderer();
        String[] types = {Transition.FADE, Transition.PUSH, Transition.COVER, Transition.UNCOVER,
            Transition.WIPE, Transition.ZOOM};
        for (String t : types) {
            Slide to = p.slides.get(1).withTransition(Transition.of(t, 500));
            Image img = Image.createImage(320, 180, 0);
            for (float f = 0; f <= 1; f += 0.25f) {
                SlideAnimation.paintTransition(img.getGraphics(), r, p, p.slides.get(0), null, to,
                        null, 0, 0, 320 / p.width, f);
            }
        }
    }

    @Test
    void textShrinksToFit() {
        TextFrame f = TextFrame.of("x", "A long title that cannot fit in so small a box at its "
                + "size", com.codename1.wordprocessing.TextStyle.DEFAULT.withSize(40),
                com.codename1.wordprocessing.Paragraph.LEFT).withAutofit(TextFrame.FIT_SHRINK);
        TextBoxLayout l = TextBoxLayout.of(f, 200, 60, new com.codename1.wordprocessing.PdfMeasurer());
        assertTrue(l.shrink < 1);
        assertTrue(l.usedHeight <= 60 * 1.05f, "used " + l.usedHeight);
    }

    @Test
    void scatterPlotsEverySeriesAgainstTheCategories() {
        ChartElement c = new ChartElement("c", null, ChartElement.SCATTER, 0, 0, 200, 100, 0,
                null, false, false, new String[] {"1", "2.5", "x"}, new String[] {"a"},
                new double[][] {{5, 6, 7}}, null);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] {1, 2.5, 3},
                c.xValues(), 0);
        org.junit.jupiter.api.Assertions.assertEquals(1, c.toChart().getSeries().size(),
                "a one series scatter keeps its series");
    }
}
