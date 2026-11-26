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
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Navigating a slide show.
class SlideShowTest extends UITestBase {
    @Test
    void clicksPlayBuildsThenAdvanceAndSkipHiddenSlides() {
        SlideShow show = new SlideShow(Decks.everything());
        Form f = new Form("Show", new BorderLayout());
        f.add(BorderLayout.CENTER, show);
        f.show();
        assertEquals(0, show.getSlideIndex());
        assertTrue(show.next());
        assertEquals(0, show.getSlideIndex(), "the first click plays the build");
        assertEquals(1, show.getBuildStep());
        assertTrue(show.next());
        assertEquals(1, show.getSlideIndex());
        assertFalse(show.next(), "slide 3 is hidden, so slide 2 is the last");
        assertTrue(show.previous());
        assertEquals(0, show.getSlideIndex());
        assertEquals(1, show.getBuildStep(), "going back shows the slide fully built");
        show.setPresenterMode(true);
        f.revalidate();
        show.goTo(2);
        assertEquals(2, show.getSlideIndex());
    }

    @Test
    void liveChartsAreComponents() {
        Presentation p = Decks.everything();
        SlideShow show = new SlideShow(p);
        Form f = new Form("Show", new BorderLayout());
        f.add(BorderLayout.CENTER, show);
        f.show();
        show.goTo(1);
        assertEquals(1, show.getComponentCount());
        assertTrue(show.getComponentAt(0) instanceof com.codename1.ui.chart.Chart);
        show.setLiveCharts(false);
        assertEquals(0, show.getComponentCount());
    }

    @Test
    void liveChartTextScalesWithTheSlideLikeThePaintedChart() {
        com.codename1.ui.chart.Chart c = new com.codename1.ui.chart.Chart();
        com.codename1.ui.plaf.Style all = c.getAllStyles();
        all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        all.setPadding(20, 20, 20, 20);
        float k = SlideRenderer.chartPixelsPerPoint();
        SlideShow.fitText(c, k / 2);
        assertEquals(10, c.getStyle().getPaddingTop(), "half the real size, half the padding");
        SlideShow.fitText(c, k / 2);
        assertEquals(10, c.getStyle().getPaddingTop(), "the same scale changes nothing");
        SlideShow.fitText(c, k / 4);
        assertEquals(5, c.getStyle().getPaddingTop(), "measured from the base, not compounded");
        SlideShow.fitText(c, k);
        assertEquals(20, c.getStyle().getPaddingTop(), "the real size restores the theme's");
    }

    @Test
    void liveChartKeepsAStyleTheAppChangesAfterAFit() {
        com.codename1.ui.chart.Chart c = new com.codename1.ui.chart.Chart();
        com.codename1.ui.plaf.Style all = c.getAllStyles();
        all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        all.setPadding(20, 20, 20, 20);
        float k = SlideRenderer.chartPixelsPerPoint();
        SlideShow.fitText(c, k / 2);
        c.getUnselectedStyle().setPadding(40, 40, 40, 40);
        SlideShow.fitText(c, k / 4);
        assertEquals(10, c.getUnselectedStyle().getPaddingTop(),
                "the app's later padding is the new base, not the stale theme one");
    }

    @Test
    void liveChartScalesEachStateFromItsOwnStyle() {
        com.codename1.ui.chart.Chart c = new com.codename1.ui.chart.Chart();
        com.codename1.ui.plaf.Style all = c.getAllStyles();
        all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        c.getUnselectedStyle().setPadding(20, 20, 20, 20);
        c.getSelectedStyle().setPadding(8, 8, 8, 8);
        SlideShow.fitText(c, SlideRenderer.chartPixelsPerPoint() / 2);
        assertEquals(10, c.getUnselectedStyle().getPaddingTop(), "unselected from its own base");
        assertEquals(4, c.getSelectedStyle().getPaddingTop(), "selected from its own base");
    }

    @Test
    void layingOutAShowFitsItsLiveCharts() {
        SlideShow show = new SlideShow(Decks.everything());
        Form f = new Form("Show", new BorderLayout());
        f.add(BorderLayout.CENTER, show);
        f.show();
        show.goTo(1);
        f.revalidate();
        com.codename1.ui.Component chart = show.getComponentAt(0);
        Object ratio = chart.getClientProperty("cn1$textRatio");
        assertTrue(ratio instanceof Float, "the layout fitted the chart");
        float expected = show.slideArea()[4] / SlideRenderer.chartPixelsPerPoint();
        assertEquals(expected, ((Float) ratio).floatValue(), 0.001f);
    }
}
