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
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.layout.SankeyLayout;
import com.codename1.ui.chart.layout.Squarify;
import com.codename1.ui.chart.series.PieSeries;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Counts work rather than time: the counters are package-private hooks.
public class ChartReviewRound28Test extends UITestBase {

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
            return new java.util.ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    @FormTest
    public void hugeHorizontalLegendLeavesRoomForThePie() {
        final int n = 60000;
        String[] labels = new String[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "slice " + i;
            values[i] = i == 0 ? n : 1;
        }
        PieSeries s = new PieSeries("p");
        s.setData(labels, values);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> calls = paint(c);
        int sectors = 0;
        for (Object[] call : calls) {
            if ("fillShape".equals(call[0])) {
                sectors++;
            }
        }
        assertTrue(sectors > 0, "the pie is drawn");
        int top = Integer.MAX_VALUE;
        int bottom = 0;
        for (int[] hit : c.legendHits) {
            top = Math.min(top, hit[1]);
            bottom = Math.max(bottom, hit[1] + hit[3]);
        }
        assertTrue(c.legendHits.size() > 0 && c.legendHits.size() < n, "some entries, summed up: " + c.legendHits.size());
        assertTrue(bottom - top <= H * 35 / 100, "legend height " + (bottom - top));
        assertTrue(c.legendRowMeasures < 2000, "rows measured from their own start: " + c.legendRowMeasures);
    }

    @FormTest
    public void branchSumsAreCached() {
        TreeNode root = new TreeNode("root");
        TreeNode leaf = null;
        for (int i = 0; i < 10000; i++) {
            leaf = root.add(new TreeNode("n" + i, 1));
        }
        assertEquals(10000, root.getValue(), 0);
        assertEquals(10000, root.getValue(), 0);
        assertEquals(1, root.sumComputations, "summed once");
        leaf.setValue(2);
        assertEquals(10001, root.getValue(), 0);
        assertEquals(2, root.sumComputations, "a change invalidates the sum once");
        assertEquals(9999, leaf.getIndexInParent());
        TreeNode other = new TreeNode("other");
        other.add(root.getChild(0));
        assertEquals(10000, root.getValue(), 0);
        assertEquals(0, root.getChild(0).getIndexInParent());
        assertEquals(9998, leaf.getIndexInParent(), "siblings renumbered after a move");
    }

    @FormTest
    public void squarifyKeepsAreasAndOrder() {
        double[] v = {1, 6, 2, 6, 4, 3, 2};
        float[][] r = Squarify.layout(v, 0, 0, 6, 4);
        for (int i = 0; i < v.length; i++) {
            assertEquals(v[i], r[i][2] * r[i][3], 1e-3, "area of " + i);
        }
        // the two sixes, biggest first and stable, fill the first column
        assertEquals(0, r[1][0], 1e-4);
        assertEquals(0, r[1][1], 1e-4);
        assertEquals(0, r[3][0], 1e-4);
        assertEquals(2, r[3][1], 1e-4);
        double[] big = new double[10000];
        for (int i = 0; i < big.length; i++) {
            big[i] = 1 + i % 7;
        }
        float[][] rb = Squarify.layout(big, 0, 0, 1000, 1000);
        double area = 0;
        for (float[] cell : rb) {
            area += cell[2] * cell[3];
        }
        assertEquals(1e6, area, 1);
    }

    @FormTest
    public void sankeyColumnOfManyNodesKeepsLinkWidths() {
        int n = 26;
        int[] src = new int[n - 1];
        int[] dst = new int[n - 1];
        double[] val = new double[n - 1];
        for (int l = 0; l < n - 1; l++) {
            src[l] = 0;
            dst[l] = l + 1;
            val[l] = 1;
        }
        SankeyLayout sl = new SankeyLayout(n, src, dst, val, 0, 0, 400, 300, 10, 20);
        for (int l = 0; l < n - 1; l++) {
            assertTrue(sl.getLinkWidth(l) > 0, "link " + l + " width " + sl.getLinkWidth(l));
            assertTrue(sl.getNodeY(l + 1) + sl.getNodeHeight(l + 1) <= 300.5f, "node " + (l + 1) + " inside");
        }
    }

    @FormTest
    public void smaSurvivesHugeMagnitudes() {
        double[] r = Indicators.sma(new double[]{1e17, 1, 1, 1}, 2);
        assertEquals(5e16, r[1], 1e3);
        assertEquals(1, r[2], 0);
        assertEquals(1, r[3], 0);
    }
}
