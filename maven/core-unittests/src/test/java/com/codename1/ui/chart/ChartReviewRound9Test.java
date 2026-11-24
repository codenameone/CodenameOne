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
package com.codename1.ui.chart;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Ninth review round of the chart package: one test per finding.
public class ChartReviewRound9Test extends UITestBase {

    private static ChartContext paint(Chart c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(600);
        c.setHeight(400);
        c.paint(Image.createImage(600, 400).getGraphics());
        return c.getLastContext();
    }

    private static LineSeries line(String name) {
        return (LineSeries) new LineSeries(name).setData(null, new double[]{1, 5, 2, 8, 3, 9, 4});
    }

    private static void tap(Chart c, int x, int y) {
        c.pointerPressed(x, y);
        c.pointerReleased(x, y);
    }

    @FormTest
    public void addingAnAxisMidZoomAnimationDoesNotThrow() {
        AnimationTime.setTime(50000);
        try {
            Chart c = new Chart().addSeries(line("a"));
            c.setAnimationDuration(0);
            ChartContext ctx = paint(c);
            int x = ctx.getPlotX() + ctx.getPlotWidth() / 2;
            int y = ctx.getPlotY() + ctx.getPlotHeight() / 2;
            tap(c, x, y);
            AnimationTime.setTime(50050);
            tap(c, x, y);
            AnimationTime.setTime(50100);
            c.setSecondaryYAxis(new NumberAxis(Axis.RIGHT));
            c.animate();
            AnimationTime.setTime(50500);
            c.animate();
            assertTrue(c.getXAxis().isZoomed(), "the zoom still lands on the axes both snapshots have");
            c.setSecondaryYAxis(null);
            c.resetZoom();
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void removingSeriesClearsLegendHits() {
        Chart c = new Chart().addSeries(line("a")).addSeries(line("b"));
        c.setAnimationDuration(0);
        paint(c);
        assertFalse(c.legendHits.isEmpty(), "positive control: the legend was laid out");
        int[] last = c.legendHits.get(c.legendHits.size() - 1);
        c.clearSeries();
        assertTrue(c.legendHits.isEmpty());
        // a tap where the legend was must not reach a series that is gone
        tap(c, last[0] + 2, last[1] + 2);
        c.addSeries(line("c")).addSeries(line("d"));
        paint(c);
        assertFalse(c.legendHits.isEmpty());
        c.removeSeries(c.getSeries().get(0));
        assertTrue(c.legendHits.isEmpty());
    }

    @FormTest
    public void legendListenerAndTogglesSwitch() {
        LineSeries a = line("a");
        LineSeries b = line("b");
        Chart c = new Chart().addSeries(a).addSeries(b);
        c.setAnimationDuration(0);
        paint(c);
        final List<Series> tapped = new ArrayList<Series>();
        c.addLegendListener((chart, s, entry) -> tapped.add(s));
        int[] hit = c.legendHits.get(1);
        assertSame(b, c.getSeries().get(hit[4]));
        c.setLegendToggles(false);
        tap(c, hit[0] + 2, hit[1] + 2);
        assertEquals(1, tapped.size());
        assertSame(b, tapped.get(0));
        assertTrue(b.isVisible(), "toggling is off");
        c.setLegendToggles(true);
        AnimationTime.setTime(AnimationTime.now() + 1000);
        try {
            tap(c, hit[0] + 2, hit[1] + 2);
        } finally {
            AnimationTime.reset();
        }
        assertEquals(2, tapped.size());
        assertFalse(b.isVisible(), "positive control: toggling on hides it");
    }

    @FormTest
    public void programmaticSelectionIsQuietAndKeysContinueFromIt() {
        LineSeries a = line("a");
        Chart c = new Chart().addSeries(a);
        c.setAnimationDuration(0);
        final List<ChartPoint> fired = new ArrayList<ChartPoint>();
        c.addSelectionListener((chart, p) -> fired.add(p));
        c.setSelection(a, 3);
        assertNull(c.getSelection(), "applied by the first paint");
        paint(c);
        assertNotNull(c.getSelection());
        assertEquals(3, c.getSelection().getIndex());
        assertTrue(fired.isEmpty(), "programmatic selection fires no listener");
        c.keyReleased(Display.getInstance().getKeyCode(Display.GAME_RIGHT));
        assertEquals(4, c.getSelection().getIndex(), "arrow keys step on from the programmatic selection");
        assertEquals(1, fired.size());
        c.setSelection(a, 3);
        c.setSelection(a, 99);
        assertEquals(3, c.getSelection().getIndex(), "an index out of range is ignored");
        c.setSelection(null, 0);
        assertNull(c.getSelection());
    }

    @FormTest
    public void showTooltipAtXInSharedMode() {
        Chart c = new Chart().addSeries(line("a"));
        c.setAnimationDuration(0);
        paint(c);
        c.showTooltipAtX(2);
        assertTrue(Double.isNaN(c.getTooltipX()), "not shared mode");
        c.setTooltipMode(Chart.TOOLTIP_SHARED);
        Chart other = new Chart().addSeries(line("b"));
        c.syncWith(other);
        c.showTooltipAtX(2);
        assertEquals(2, c.getTooltipX(), 0);
        assertEquals(2, other.getTooltipX(), 0);
    }

    @FormTest
    public void secondTapOnSelectedPointDeselectsWhenZoomIsOff() {
        AnimationTime.setTime(60000);
        try {
            LineSeries a = line("a");
            Chart c = new Chart().addSeries(a);
            c.setAnimationDuration(0);
            c.setZoomEnabled(false);
            ChartContext ctx = paint(c);
            ChartPoint p = a.getPoint(ctx, 3);
            tap(c, p.getScreenX(), p.getScreenY());
            assertNotNull(c.getSelection(), "positive control: the tap selected");
            AnimationTime.setTime(60100);
            tap(c, p.getScreenX(), p.getScreenY());
            assertNull(c.getSelection());
            assertFalse(c.getXAxis().isZoomed());
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void denseScatterSkipsMarkersOnTheSamePixel() {
        int n = 5000;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i % 2;
            y[i] = i % 2;
        }
        ScatterSeries s = new ScatterSeries("s");
        s.setData(x, y);
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        c.setLegendPosition(Chart.LEGEND_NONE);
        paint(c);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(600, 400).getGraphics());
            int arcs = 0;
            for (Object[] call : implementation.getDrawCalls()) {
                if ("fillArc".equals(call[0])) {
                    arcs++;
                }
            }
            assertTrue(arcs > 0 && arcs < 10, "arcs " + arcs);
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private List<String> calls(Chart c) {
        implementation.setRecordDrawCalls(true);
        try {
            paint(c);
            List<String> out = new ArrayList<String>();
            for (Object[] call : implementation.getDrawCalls()) {
                StringBuilder sb = new StringBuilder();
                for (Object o : call) {
                    sb.append(o).append(',');
                }
                out.add(sb.toString());
            }
            return out;
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    @FormTest
    public void anAbandonedBrushIsClearedByTheNextTouch() {
        Chart fresh = new Chart().setBrushEnabled(true);
        fresh.addSeries(line("a"));
        ChartContext ctx = paint(fresh);
        List<String> clean = calls(fresh);

        Chart c = new Chart().setBrushEnabled(true);
        c.addSeries(line("a"));
        paint(c);
        int y = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        int from = ctx.getPlotX() + ctx.getPlotWidth() / 4;
        int to = ctx.getPlotX() + ctx.getPlotWidth() * 3 / 4;
        c.pointerPressed(from, y);
        c.pointerDragged(new int[]{(from + to) / 2}, new int[]{y});
        c.pointerDragged(new int[]{to}, new int[]{y});
        // positive control: the band is drawn mid-drag
        assertFalse(clean.equals(calls(c)), "the brush band should paint while dragging");
        // the release never arrives; the chart leaves its form
        c.deinitialize();
        assertEquals(clean, calls(c), "a brush whose release was lost must not stay on screen");
    }
}
