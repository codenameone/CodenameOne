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

import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.Ticks;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.SankeyLayout;
import com.codename1.ui.chart.layout.Squarify;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Layout invariants of the chart package that need no display: ticks, label collision,
/// squarified treemaps, sankey flows, axis viewports and colour scales.
public class ChartLayoutTest {

    @Test
    public void ticksAreRoundAndInsideTheRange() {
        double[][] ranges = {{0, 100}, {0.013, 0.97}, {-37, 512}, {1e6, 3.4e6}, {-1, 1}};
        for (double[] r : ranges) {
            for (int max = 2; max <= 12; max++) {
                double[] t = Ticks.linear(r[0], r[1], max);
                assertTrue(t.length <= max, "too many ticks for " + r[0] + ".." + r[1]);
                for (int i = 0; i < t.length; i++) {
                    assertTrue(t[i] >= r[0] - 1e-9 && t[i] <= r[1] + 1e-9);
                    if (i > 0) {
                        double step = t[i] - t[i - 1];
                        assertEquals(Ticks.niceStep(step), step, step * 1e-6, "step is not a nice number");
                    }
                }
            }
        }
        assertEquals(0, Ticks.decimalsFor(5));
        assertEquals(1, Ticks.decimalsFor(0.5));
        assertEquals(2, Ticks.decimalsFor(0.25));
    }

    @Test
    public void labelLayoutNeverPlacesOverlappingBoxes() {
        LabelLayout lay = new LabelLayout(0, 0, 400, 300);
        java.util.Random rnd = new java.util.Random(3);
        java.util.List<int[]> placed = new java.util.ArrayList<int[]>();
        int[] out = new int[2];
        for (int i = 0; i < 300; i++) {
            int w = 20 + rnd.nextInt(60);
            int h = 10 + rnd.nextInt(10);
            if (lay.place(rnd.nextInt(400), rnd.nextInt(300), w, h, 3, LabelLayout.ABOVE | LabelLayout.BELOW
                    | LabelLayout.LEFT | LabelLayout.RIGHT, out)) {
                placed.add(new int[]{out[0], out[1], w, h});
            }
        }
        assertTrue(placed.size() > 10);
        for (int i = 0; i < placed.size(); i++) {
            int[] a = placed.get(i);
            assertTrue(a[0] >= 0 && a[1] >= 0 && a[0] + a[2] <= 400 && a[1] + a[3] <= 300, "label outside bounds");
            for (int j = i + 1; j < placed.size(); j++) {
                int[] b = placed.get(j);
                boolean overlap = a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
                assertFalse(overlap, "labels " + i + " and " + j + " overlap");
            }
        }
    }

    @Test
    public void squarifyAreasAreProportionalAndInside() {
        double[] v = {6, 6, 4, 3, 2, 2, 1};
        float[][] r = Squarify.layout(v, 10, 20, 600, 400);
        double total = 0;
        for (double d : v) {
            total += d;
        }
        double area = 0;
        for (int i = 0; i < v.length; i++) {
            float[] c = r[i];
            assertTrue(c[0] >= 10 - 0.01f && c[1] >= 20 - 0.01f && c[0] + c[2] <= 610.01f && c[1] + c[3] <= 420.01f);
            assertEquals(v[i] / total * 600 * 400, c[2] * c[3], 600 * 400 * 0.005);
            area += c[2] * c[3];
        }
        assertEquals(600 * 400, area, 600 * 400 * 0.01);
    }

    @Test
    public void sankeyConservesFlowAndOrdersColumns() {
        // 0 -> 2, 1 -> 2, 2 -> 3, 2 -> 4
        int[] src = {0, 1, 2, 2};
        int[] dst = {2, 2, 3, 4};
        double[] val = {40, 60, 70, 30};
        SankeyLayout s = new SankeyLayout(5, src, dst, val, 0, 0, 500, 300, 10, 8);
        assertEquals(3, s.getColumns());
        assertEquals(0, s.getColumn(0));
        assertEquals(1, s.getColumn(2));
        assertEquals(2, s.getColumn(4));
        for (int l = 0; l < src.length; l++) {
            assertTrue(s.getNodeX(src[l]) < s.getNodeX(dst[l]));
        }
        // the middle node carries all the flow in and all the flow out
        float in = s.getLinkWidth(0) + s.getLinkWidth(1);
        float out = s.getLinkWidth(2) + s.getLinkWidth(3);
        assertEquals(in, out, 0.5f);
        assertEquals(s.getNodeHeight(2), in, 0.5f);
        for (int i = 0; i < 5; i++) {
            assertTrue(s.getNodeY(i) >= -0.5f && s.getNodeY(i) + s.getNodeHeight(i) <= 300.5f);
        }
        // nodes of a column do not overlap
        assertTrue(s.getNodeY(0) + s.getNodeHeight(0) <= s.getNodeY(1) + 0.5f
                || s.getNodeY(1) + s.getNodeHeight(1) <= s.getNodeY(0) + 0.5f);
    }

    @Test
    public void axisZoomAndPanStayInsideTheData() {
        Axis a = new NumberAxis(Axis.BOTTOM);
        a.setDataBounds(0, 100);
        double min0 = a.getVisibleMin();
        double max0 = a.getVisibleMax();
        a.zoom(4, 0.5);
        assertTrue(a.isZoomed());
        double span = a.getVisibleMax() - a.getVisibleMin();
        assertEquals((max0 - min0) / 4, span, 1e-6);
        a.pan(100);
        assertTrue(a.getVisibleMax() <= max0 + 1e-9, "panned past the data");
        assertEquals(span, a.getVisibleMax() - a.getVisibleMin(), 1e-6);
        a.pan(-100);
        assertTrue(a.getVisibleMin() >= min0 - 1e-9, "panned past the data");
        a.resetViewport();
        assertFalse(a.isZoomed());
        assertEquals(min0, a.getVisibleMin(), 1e-9);
    }

    @Test
    public void colorScaleInterpolatesAndClamps() {
        ColorScale s = new ColorScale(new double[]{0, 1}, new int[]{0x000000, 0xffffff});
        assertEquals(0x000000, s.colorAt(-1));
        assertEquals(0xffffff, s.colorAt(2));
        assertEquals(0x808080, s.colorAt(0.5));
        assertEquals(0x000000, s.colorAt(Double.NaN));
    }
}
