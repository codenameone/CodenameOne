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
package com.codename1.charts.views;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class XYChartTest {
    private XYMultipleSeriesDataset dataset;
    private XYMultipleSeriesRenderer renderer;
    private TestXYChart chart;

    @BeforeEach
    public void setup() {
        dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 0);
        series.add(10, 10);
        dataset.addSeries(series);
        renderer = new XYMultipleSeriesRenderer();
        XYSeriesRenderer r = new XYSeriesRenderer();
        renderer.addSeriesRenderer(r);
        chart = new TestXYChart(dataset, renderer);
    }

    @Test
    public void testGetRendererAndDataset() {
        assertEquals(renderer, chart.getRenderer());
        assertEquals(dataset, chart.getDataset());
    }

    @Test
    public void testCalcRangeRoundTrip() {
        double[] range = new double[]{0, 100, -50, 50};
        chart.setCalcRange(range, 0);
        assertArrayEquals(range, chart.getCalcRange(0), 1e-6);
    }

    @Test
    public void testToRealPointUsesScreenRectangle() throws Exception {
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(100);
        renderer.setYAxisMin(-50);
        renderer.setYAxisMax(50);

        Rectangle screen = new Rectangle(10, 20, 200, 400);
        Field field = XYChart.class.getDeclaredField("mScreenR");
        field.setAccessible(true);
        field.set(chart, screen);

        chart.setCalcRange(new double[]{0, 100, -50, 50}, 0);
        double[] real = chart.toRealPoint(110f, 220f);
        assertEquals(50.0, real[0], 1e-6);
        assertEquals(0.0, real[1], 1e-6);
    }

    @Test
    public void testToScreenPointUsesCalculatedRange() throws Exception {
        Rectangle screen = new Rectangle(5, 10, 300, 150);
        Field field = XYChart.class.getDeclaredField("mScreenR");
        field.setAccessible(true);
        field.set(chart, screen);

        chart.setCalcRange(new double[]{0, 60, 0, 30}, 0);
        double[] screenPoint = chart.toScreenPoint(new double[]{30, 15});
        assertEquals(155.0, screenPoint[0], 1e-6);
        assertEquals(85.0, screenPoint[1], 1e-6);
    }

    @Test
    public void testSeriesSelectionUsesClickableAreas() throws Exception {
        Map<Integer, List<ClickableArea>> map = new HashMap<Integer, List<ClickableArea>>();
        List<ClickableArea> list = new LinkedList<ClickableArea>();
        list.add(new ClickableArea(new Rectangle2D(10, 10, 10, 10), 1d, 2d));
        map.put(0, list);

        Field field = XYChart.class.getDeclaredField("clickableAreas");
        field.setAccessible(true);
        field.set(chart, map);

        SeriesSelection selection = chart.getSeriesAndPointForScreenCoordinate(new Point(12, 12));
        assertNotNull(selection);
        assertEquals(0, selection.getSeriesIndex());
        assertEquals(0, selection.getPointIndex());
        assertEquals(2d, selection.getValue(), 1e-6);
        assertEquals(1d, selection.getXValue(), 1e-6);
    }

    /// Rotated text must leave the canvas with the very transform it found, by
    /// putting that transform back. Rotating by the opposite angle instead is
    /// only approximately an undo: the angle is a float, so the round trip
    /// leaves a matrix a few units in the last place away from the one it
    /// started with. The JavaScript port draws text as DOM nodes under an exact
    /// identity and on the canvas otherwise, so that residue moved the title
    /// of every form showing an XY chart with a Y axis title by three pixels.
    @Test
    public void testRotatedTextRestoresTheTransformItFound() {
        RecordingCanvas canvas = new RecordingCanvas();
        chart.drawText(canvas, null, 36f, 256f, new Paint(), -90);

        assertEquals(3, canvas.calls.size(), canvas.calls.toString());
        assertEquals("getTransform", canvas.calls.get(0));
        assertEquals("rotate -90.0 36.0 256.0", canvas.calls.get(1));
        assertEquals("setTransform", canvas.calls.get(2));
        assertNotNull(canvas.captured);
        assertSame(canvas.captured, canvas.restored);
    }

    @Test
    public void testUnrotatedTextLeavesTheTransformAlone() {
        RecordingCanvas canvas = new RecordingCanvas();
        chart.drawText(canvas, null, 36f, 256f, new Paint(), 0);
        assertEquals(0, canvas.calls.size(), canvas.calls.toString());
    }

    private static class RecordingCanvas extends Canvas {
        final List<String> calls = new ArrayList<String>();
        Transform captured;
        Transform restored;

        @Override
        public void getTransform(Transform out) {
            calls.add("getTransform");
            captured = out;
        }

        @Override
        public void setTransform(Transform t) {
            calls.add("setTransform");
            restored = t;
        }

        @Override
        public void rotate(float angle, float x, float y) {
            calls.add("rotate " + angle + " " + x + " " + y);
        }
    }

    private static class TestXYChart extends XYChart {
        TestXYChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
            super(dataset, renderer);
        }

        @Override
        public void drawSeries(Canvas canvas, Paint paint, List<Float> points, XYSeriesRenderer seriesRenderer, float yAxisValue,
                               int seriesIndex, int startIndex) {
            // no-op for testing
        }

        @Override
        protected ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values, float yAxisValue,
                                                           int seriesIndex, int startIndex) {
            return new ClickableArea[0];
        }

        @Override
        public String getChartType() {
            return "Test";
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer seriesRenderer, float x, float y, int seriesIndex,
                                    Paint paint) {
            // no-op for testing
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 10;
        }
    }
}
