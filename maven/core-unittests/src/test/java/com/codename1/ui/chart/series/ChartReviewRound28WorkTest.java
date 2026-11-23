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
package com.codename1.ui.chart.series;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.TreeNode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Counts work rather than time: the counters are package-private hooks in the series.
public class ChartReviewRound28WorkTest extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
        implementation.setRecordDrawnStrings(false);
    }

    private void paint(com.codename1.ui.Component c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
    }

    @FormTest
    public void hiddenStateFollowsTheKthDuplicateInLinearWork() {
        PieSeries s = new PieSeries("p");
        s.setData(new String[]{"a", "b", "a", "c"}, new double[]{1, 2, 3, 4});
        s.toggleLegendEntry(2);
        s.setData(new String[]{"c", "a", "a", "b"}, new double[]{4, 1, 3, 2});
        assertTrue(s.isLegendEntryVisible(1), "first a stays shown");
        assertTrue(!s.isLegendEntryVisible(2), "second a stays hidden");
        assertTrue(s.isLegendEntryVisible(0) && s.isLegendEntryVisible(3), "others shown");
        int n = 5000;
        String[] labels = new String[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "l" + i;
            values[i] = 1;
        }
        PieSeries big = new PieSeries("big");
        big.setData(labels, values);
        big.toggleLegendEntry(n - 1);
        big.hiddenLookups = 0;
        big.setData(labels, values);
        assertTrue(!big.isLegendEntryVisible(n - 1), "kept hidden");
        assertTrue(big.hiddenLookups <= 2 * n + 2, "linear: " + big.hiddenLookups);
        big.toggleLegendEntry(n - 1);
        big.hiddenLookups = 0;
        big.setData(labels, values);
        assertEquals(0, big.hiddenLookups, "nothing hidden, nothing looked up");
    }

    @FormTest
    public void setDataHonoursMaxPoints() {
        LineSeries s = new LineSeries("s");
        s.setMaxPoints(3);
        s.setValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(3, s.size());
        assertEquals(8, s.getY(0), 0);
        assertEquals(7, s.getX(0), 0);
        assertEquals(10, s.getY(2), 0);
        ScatterSeries sc = new ScatterSeries("sc");
        sc.setMaxPoints(2);
        sc.setData(new double[]{1, 2, 3}, new double[]{10, 20, 30}, new double[]{5, 6, 7});
        assertEquals(2, sc.size());
        assertEquals(2, sc.getX(0), 0);
        assertEquals(30, sc.getY(1), 0);
    }

    @FormTest
    public void hiddenSliceLabelAndTooltipAgree() {
        PieSeries s = new PieSeries("p");
        s.setData(new String[]{"a", "b", "c"}, new double[]{30, 30, 40});
        s.setLabelMode(PieSeries.LABELS_INSIDE);
        s.toggleLegendEntry(2);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        implementation.setRecordDrawnStrings(true);
        paint(c);
        int fifty = 0;
        for (Object[] d : implementation.getDrawnStrings()) {
            if ("50%".equals(d[0])) {
                fifty++;
            }
        }
        assertEquals(2, fifty, "both shown slices labelled 50%");
        String tip = s.formatTooltip(c.getLastContext(), new ChartPoint(s, 0, 0, 30, 0, 0, "a"));
        assertTrue(tip.contains("(50"), tip);
        PieSeries h = new PieSeries("h");
        h.setData(new String[]{"a", "b"}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE});
        String t2 = h.formatTooltip(null, new ChartPoint(h, 0, 0, 1, 0, 0, "a"));
        assertTrue(t2.contains("(50"), t2);
    }

    @FormTest
    public void outliersIndexedOncePerChange() {
        BoxPlotSeries s = new BoxPlotSeries("b");
        int n = 200;
        for (int i = 0; i < n; i++) {
            s.addSamples(i, new double[]{1, 2, 3, 4, 5, 100 + i});
        }
        assertEquals(1, s.getOutliers(7).length);
        assertEquals(107, s.getOutliers(7)[0], 0);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        s.outlierRowVisits = 0;
        paint(c);
        paint(c);
        paint(c);
        assertTrue(s.outlierRowVisits <= n, "one pass over the rows: " + s.outlierRowVisits);
        double[] b = s.getYBounds();
        assertTrue(b != null && b[1] >= 100 + n - 1, "bounds include outliers");
    }

    @FormTest
    public void outsideLabelsMeasuredOnlyForLabelledSlices() {
        int n = 2000;
        String[] labels = new String[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "s" + i;
            values[i] = i == 0 ? 1e6 : 1e-3;
        }
        PieSeries s = new PieSeries("p");
        s.setData(labels, values);
        s.setLabelMode(PieSeries.LABELS_OUTSIDE);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setLegendPosition(Chart.LEGEND_NONE);
        s.labelMeasures = 0;
        paint(c);
        assertEquals(1, s.labelMeasures, "only the one slice wide enough for a label");
    }

    @FormTest
    public void treeChangesAnimate() {
        TreeNode root = new TreeNode("root");
        TreeNode a = root.add(new TreeNode("a", 1));
        root.add(new TreeNode("b", 1));
        final int[] changes = new int[1];
        TreemapSeries s = new TreemapSeries("t", root) {
            @Override
            protected void dataChanged() {
                changes[0]++;
                super.dataChanged();
            }
        };
        new Chart().addSeries(s).setAnimationDuration(0);
        changes[0] = 0;
        a.setValue(5);
        assertEquals(1, changes[0], "setValue notifies the series");
        root.add(new TreeNode("c", 2));
        assertEquals(2, changes[0], "add notifies the series");
        TreeNode other = new TreeNode("other");
        s.setRoot(other);
        changes[0] = 0;
        a.setValue(7);
        assertEquals(0, changes[0], "the old tree no longer notifies");
    }
}
