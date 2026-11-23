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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound32Test extends UITestBase {

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

    private static int at(Object[] call, int k) {
        return ((Integer) call[k]).intValue();
    }

    @FormTest
    public void logAxisLabelsKeepSmallValues() {
        LogAxis a = new LogAxis(Axis.LEFT);
        assertEquals("0.0001", a.format(0.0001, 0));
        assertEquals("0.001", a.format(0.001, 0));
        assertEquals("0.005", a.format(0.005, 0));
        assertEquals("0.01", a.format(0.01, 0));
        assertEquals("1", a.format(1, 0));
        assertEquals("20", a.format(20, 0));
    }

    @FormTest
    public void barWithNaNXIsNotDrawnAtTheEdge() {
        BarSeries s = new BarSeries("s");
        s.setData(new double[]{0, Double.NaN, 2}, new double[]{3, 5, 4});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> calls = paint(c);
        int plotX = c.getLastContext().getPlotX();
        int bars = 0;
        for (Object[] call : calls) {
            if ("fillRect".equals(call[0]) && at(call, 3) < W / 2 && at(call, 4) > 4) {
                assertFalse(at(call, 1) < plotX, "a bar painted left of the plot at x=" + at(call, 1));
                bars++;
            }
        }
        assertTrue(bars >= 2, "the two real bars are painted");
        ChartContext ctx = c.getLastContext();
        int midY = ctx.getPlotY() + ctx.getPlotHeight() / 2 - ctx.getOriginY();
        ChartPoint p = s.hitTest(ctx, 1 - ctx.getOriginX(), midY, 2);
        assertTrue(p == null || p.getIndex() != 1, "the NaN-x bar has no hit area");
    }

    @FormTest
    public void candlestickLegendUsesSetColors() {
        CandlestickSeries s = new CandlestickSeries("s");
        s.setColors(0x0000ff, 0xffff00);
        Image img = Image.createImage(20, 20, 0xffffffff);
        s.paintLegendSymbol(img.getGraphics(), 0, 0, 20);
        int[] rgb = img.getRGB();
        assertEquals(0x0000ff, rgb[10 * 20 + 2] & 0xffffff, "up half");
        assertEquals(0xffff00, rgb[10 * 20 + 15] & 0xffffff, "down half");
    }

    @FormTest
    public void radarTableOmitsValuesPastTheSpokes() {
        RadarSeries a = new RadarSeries("a", "A", "B", "C");
        a.setValues(1, 2, 3);
        RadarSeries b = new RadarSeries("b");
        b.setValues(1, 2, 3, 4, 5);
        Chart c = new Chart().addSeries(a).addSeries(b);
        paint(c);
        List<String[]> rows = new ArrayList<String[]>();
        b.collectTableRows(rows);
        assertEquals(3, rows.size());
    }

    @FormTest
    public void waterfallConnectorsBridgeTheGap() {
        WaterfallSeries s = new WaterfallSeries("w");
        s.setSteps(new double[]{10, 5, -3, 12}, new boolean[]{false, false, false, true});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> calls = paint(c);
        ChartContext ctx = c.getLastContext();
        int spacing = ctx.getPlotWidth() / 4;
        int connectors = 0;
        for (Object[] call : calls) {
            if ("drawLine".equals(call[0]) && at(call, 2) == at(call, 4)) {
                int len = Math.abs(at(call, 3) - at(call, 1));
                if (len < ctx.getPlotWidth() / 2) {
                    connectors++;
                    assertTrue(len < spacing / 2, "connector of " + len + "px crosses the bars (spacing " + spacing + ")");
                }
            }
        }
        assertNotEquals(0, connectors);
    }
}
