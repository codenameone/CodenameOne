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
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound31Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private List<Object[]> paint(com.codename1.ui.Component c) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(W, H).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private static Form show(Chart c) {
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        return f;
    }

    private static void key(Chart c, int game) {
        c.keyReleased(Display.getInstance().getKeyCode(game));
    }

    @FormTest
    public void scatterPointsCanBeSelectedAndStepped() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 2, 3, 4, 5}, new double[]{5, 3, 4, 1, 2});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        c.setSelection(s, 3);
        assertNotNull(c.getSelection(), "a scatter point is selectable");
        assertEquals(3, c.getSelection().getIndex());
        key(c, Display.GAME_RIGHT);
        assertEquals(4, c.getSelection().getIndex(), "arrow keys reach scatter points");
        key(c, Display.GAME_LEFT);
        key(c, Display.GAME_LEFT);
        assertEquals(2, c.getSelection().getIndex());
    }

    @FormTest
    public void selectionOfAHiddenTreemapWaitsUntilItIsShown() {
        TreeNode root = new TreeNode("r");
        root.add(new TreeNode("a", 3)).add(new TreeNode("b", 2)).add(new TreeNode("c", 1));
        TreemapSeries t = new TreemapSeries("t", root);
        t.setVisible(false);
        Chart c = new Chart().addSeries(t).setAnimationDuration(0);
        paint(c);
        c.setSelection(t, 1);
        assertNull(c.getSelection(), "nothing selected while hidden");
        paint(c);
        assertNull(c.getSelection(), "still waiting");
        t.setVisible(true);
        paint(c);
        assertNotNull(c.getSelection(), "applied by the first paint that shows the series");
        assertEquals(1, c.getSelection().getIndex());
    }

    @FormTest
    public void selectionOfAHiddenLineWaitsAndIsNotAnnounced() {
        LineSeries a = new LineSeries("a");
        a.setValues(1, 2, 3, 4);
        LineSeries keep = new LineSeries("k");
        keep.setValues(4, 3, 2, 1);
        a.setVisible(false);
        Chart c = new Chart().addSeries(keep).addSeries(a).setAnimationDuration(0);
        paint(c);
        c.setSelection(keep, 1);
        assertNotNull(c.getSelection());
        c.setSelection(a, 2);
        assertNull(c.getSelection(), "a hidden series is not selected, and the old point is cleared");
        paint(c);
        assertNull(c.getSelection());
        a.setVisible(true);
        paint(c);
        assertNotNull(c.getSelection());
        assertEquals(a, c.getSelection().getSeries());
        assertEquals(2, c.getSelection().getIndex());
    }

    @FormTest
    public void rejectedWaterfallDataKeepsTheOffset() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setMaxPoints(2);
        w.setSteps(new double[]{1, 2, 3}, null);
        assertEquals(1, w.getBase(0), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> w.setData(new double[]{1, 2}, new double[]{10, 10, 10}));
        w.add(1);
        assertEquals(3, w.getBase(0), 1e-9, "the offset of the kept rows survived the bad data");
        assertEquals(6, w.getBase(1), 1e-9);
    }

    static final class CountingTreemap extends TreemapSeries {
        int changes;

        CountingTreemap(TreeNode root) {
            super("t", root);
        }

        @Override
        protected void dataChanged() {
            changes++;
            super.dataChanged();
        }
    }

    @FormTest
    public void removedOrUnshownHierarchyStopsListeningToItsTree() {
        TreeNode root = new TreeNode("r");
        TreeNode leaf = new TreeNode("a", 3);
        root.add(leaf).add(new TreeNode("b", 2));
        CountingTreemap t = new CountingTreemap(root);
        Chart c = new Chart().addSeries(t).setAnimationDuration(0);
        Form f = show(c);
        int before = t.changes;
        leaf.setValue(4);
        assertTrue(t.changes > before, "positive control: a shown series follows its tree");
        c.removeSeries(t);
        before = t.changes;
        leaf.setValue(5);
        assertEquals(before, t.changes, "a removed series no longer listens");
        c.addSeries(t);
        leaf.setValue(6);
        assertTrue(t.changes > before, "re-added, it listens again");
        f.removeComponent(c);
        new Form().show();
        before = t.changes;
        leaf.setValue(7);
        assertEquals(before, t.changes, "a chart off screen does not keep listening");
    }

    @FormTest
    public void sankeyPointsWaitForTheRebuiltLayout() {
        SankeySeries s = new SankeySeries("s");
        s.addLink("a", "b", 3).addLink("b", "c", 2);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertNotNull(s.getPoint(ctx, 0), "positive control");
        s.clear();
        s.addLink("x", "y", 1).addLink("y", "z", 9);
        assertNull(s.getPoint(ctx, 0), "the old layout is not read for the new nodes");
        paint(c);
        assertNotNull(s.getPoint(c.getLastContext(), 0));
    }

    @FormTest
    public void radarLabelsAndSpokesComeFromTheOwnerWhateverIsHidden() {
        RadarSeries a = new RadarSeries("a", "A", "B", "C", "D", "E");
        a.setValues(5, 5, 5, 5, 5);
        RadarSeries b = new RadarSeries("b", "X", "Y", "Z");
        b.setValues(4, 4, 4, 4, 4);
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        paint(c);
        assertEquals("A", b.getPoint(c.getLastContext(), 0).getLabel(), "labels follow the grid's spokes");
        a.setVisible(false);
        paint(c);
        assertNotNull(b.getPoint(c.getLastContext(), 3), "hiding the owner keeps five spokes");
        assertEquals("D", b.getPoint(c.getLastContext(), 3).getLabel());
    }

    // the square-off rect of a rounded bar is only the corner radius tall
    private static int shortFills(List<Object[]> calls) {
        int n = 0;
        for (Object[] call : calls) {
            if ("fillRect".equals(call[0]) && ((Integer) call[4]).intValue() < 10) {
                n++;
            }
        }
        return n;
    }

    @FormTest
    public void aBarUnderAFadingLayerIsNotRounded() {
        BarSeries a = new BarSeries("a");
        a.setValues(100, 100, 100).setStackGroup("g");
        a.setCornerRadius(3);
        BarSeries b = new BarSeries("b");
        b.setValues(100, 100, 100).setStackGroup("g");
        b.setCornerRadius(3);
        Chart c = new Chart().addSeries(a).addSeries(b);
        AnimationTime.setTime(100000);
        c.setAnimationDuration(1000);
        show(c);
        paint(c);
        AnimationTime.setTime(102000);
        c.animate();
        assertEquals(3, shortFills(paint(c)), "positive control: only the top layer is rounded");
        AnimationTime.setTime(110000);
        b.setVisible(false);
        AnimationTime.setTime(110300);
        c.animate();
        List<Object[]> mid = paint(c);
        assertTrue(c.getLastContext().getProgress() > 0 && c.getLastContext().getProgress() < 1, "mid fade");
        assertEquals(3, shortFills(mid), "the fading layer still caps the stack");
    }
}
