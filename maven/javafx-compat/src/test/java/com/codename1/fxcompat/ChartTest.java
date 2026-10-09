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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/// The charts draw themselves: what is asserted here is what reaches the
/// renderer - the bars, lines, slices and words - and the model the
/// application reads back.
public class ChartTest {

    private static final Color FIRST = Color.web("#f3622d");
    private static final Color SECOND = Color.web("#fba71b");

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> operations = new ArrayList<String>();
    private final List<double[]> bounds = new ArrayList<double[]>();
    private final List<Paint> paints = new ArrayList<Paint>();
    private final List<String> words = new ArrayList<String>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        Renderer.setTrace(null);
    }

    private void paint(Chart chart) {
        operations.clear();
        bounds.clear();
        paints.clear();
        words.clear();
        chart.resize(400, 300);
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                operations.add(operation);
                bounds.add(deviceBounds);
                paints.add(paint);
                if (text != null) {
                    words.add(text);
                }
            }
        });
        chart.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 300, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
    }

    /// The bounds of everything filled in a colour, but for the ten pixel
    /// squares of the legend.
    private List<double[]> filled(Color color) {
        List<double[]> out = new ArrayList<double[]>();
        for (int i = 0; i < operations.size(); i++) {
            double[] b = bounds.get(i);
            if (!"fill".equals(operations.get(i)) || !color.equals(paints.get(i)) || b == null) {
                continue;
            }
            boolean legend = Math.abs(b[2] - b[0] - 10) < 0.01 && Math.abs(b[3] - b[1] - 10) < 0.01;
            if (!legend) {
                out.add(b);
            }
        }
        return out;
    }

    private int count(String operation, Color color) {
        int n = 0;
        for (int i = 0; i < operations.size(); i++) {
            if (operation.equals(operations.get(i)) && color.equals(paints.get(i))) {
                n++;
            }
        }
        return n;
    }

    private static XYChart.Series<String, Number> series(String name, double a, double b) {
        XYChart.Series<String, Number> s = new XYChart.Series<String, Number>();
        s.setName(name);
        s.getData().add(new XYChart.Data<String, Number>("A", Double.valueOf(a)));
        s.getData().add(new XYChart.Data<String, Number>("B", Double.valueOf(b)));
        return s;
    }

    @Test
    public void barsGrowFromZeroInProportionToTheirValues() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<String, Number>(x, y);
        chart.setTitle("Sales");
        chart.getData().add(series("2023", 10, 20));
        chart.getData().add(series("2024", 5, 15));
        paint(chart);

        // The range comes from the data, holds zero and ends on a tick.
        assertEquals(0, y.getLowerBound(), 0);
        assertEquals(20, y.getUpperBound(), 0);

        List<double[]> first = filled(FIRST);
        List<double[]> second = filled(SECOND);
        assertEquals(2, first.size());
        assertEquals(2, second.size());
        double a = first.get(0)[3] - first.get(0)[1];
        double b = first.get(1)[3] - first.get(1)[1];
        assertEquals(2, b / a, 0.02);
        // All bars stand on the same line, and the tallest fills the plot.
        assertEquals(first.get(0)[3], first.get(1)[3], 0.01);
        assertEquals(first.get(0)[3], second.get(1)[3], 0.01);
        double c = second.get(1)[3] - second.get(1)[1];
        assertEquals(0.75, c / b, 0.02);
        // Category A is left of category B, and the second series is
        // beside the first within a category.
        assertTrue(first.get(0)[2] <= second.get(0)[0]);
        assertTrue(second.get(0)[2] < first.get(1)[0]);

        assertTrue(words.contains("Sales"));
        assertTrue(words.contains("A"));
        assertTrue(words.contains("B"));
        assertTrue(words.contains("2023"));
        assertTrue(words.contains("2024"));
        assertTrue(words.contains("0"));
        assertTrue(words.contains("20"));

        chart.setLegendVisible(false);
        chart.setTitle(null);
        paint(chart);
        assertFalse(words.contains("2023"));
        assertFalse(words.contains("Sales"));
    }

    @Test
    public void aBarChartLiesOnItsSideWhenTheCategoriesAreOnTheYAxis() {
        NumberAxis x = new NumberAxis();
        CategoryAxis y = new CategoryAxis(FXCollections.observableArrayList("A", "B"));
        BarChart<Number, String> chart = new BarChart<Number, String>(x, y);
        XYChart.Series<Number, String> s = new XYChart.Series<Number, String>();
        s.getData().add(new XYChart.Data<Number, String>(Integer.valueOf(4), "A"));
        s.getData().add(new XYChart.Data<Number, String>(Integer.valueOf(8), "B"));
        // A category the axis does not list has no place and no bar.
        s.getData().add(new XYChart.Data<Number, String>(Integer.valueOf(8), "C"));
        chart.getData().add(s);
        paint(chart);
        List<double[]> bars = filled(FIRST);
        assertEquals(2, bars.size());
        assertEquals(bars.get(0)[0], bars.get(1)[0], 0.01);
        assertEquals(2, (bars.get(1)[2] - bars.get(1)[0]) / (bars.get(0)[2] - bars.get(0)[0]), 0.02);
        // The first category is the lower one.
        assertTrue(bars.get(0)[1] > bars.get(1)[3]);
    }

    @Test
    public void aBarChartNeedsOneAxisOfEachKind() {
        try {
            new BarChart<Number, Number>(new NumberAxis(), new NumberAxis());
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("CategoryAxis") >= 0);
        }
    }

    @Test
    public void aSeriesKnowsItsChartAndFollowsTheListItIsGiven() {
        LineChart<Number, Number> chart = new LineChart<Number, Number>(new NumberAxis(), new NumberAxis());
        XYChart.Series<Number, Number> s = new XYChart.Series<Number, Number>();
        assertNull(s.getChart());
        chart.getData().add(s);
        assertSame(chart, s.getChart());
        assertSame(chart, s.chartProperty().get());
        ObservableList<XYChart.Series<Number, Number>> other = FXCollections.observableArrayList();
        chart.setData(other);
        assertNull(s.getChart());
        assertSame(other, chart.getData());
        other.add(s);
        assertSame(chart, s.getChart());
        other.remove(s);
        assertNull(s.getChart());
        XYChart.Data<Number, Number> d = new XYChart.Data<Number, Number>(Integer.valueOf(1), Integer.valueOf(2), "x");
        assertNull(d.getNode());
        assertEquals("x", d.getExtraValue());
        assertEquals(Integer.valueOf(1), d.XValueProperty().get());
        assertEquals(Integer.valueOf(2), d.YValueProperty().get());
    }

    @Test
    public void aLineJoinsThePointsOfASeriesAndMarksEach() {
        NumberAxis x = new NumberAxis(0, 1, 0.25);
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(false);
        LineChart<Number, Number> chart = new LineChart<Number, Number>(x, y);
        XYChart.Series<Number, Number> s = new XYChart.Series<Number, Number>();
        s.setName("one");
        s.getData().add(new XYChart.Data<Number, Number>(Double.valueOf(1), Integer.valueOf(130)));
        s.getData().add(new XYChart.Data<Number, Number>(Double.valueOf(0), Integer.valueOf(110)));
        s.getData().add(new XYChart.Data<Number, Number>(Double.valueOf(0.5), Integer.valueOf(120)));
        chart.getData().add(s);
        chart.setLegendVisible(false);
        paint(chart);
        // Zero is not forced into the range, so it hugs the data.
        assertEquals(110, y.getLowerBound(), 0);
        assertEquals(130, y.getUpperBound(), 0);
        // A fixed axis keeps its bounds, and its labels have the decimals
        // of its tick unit.
        assertEquals(1, x.getUpperBound(), 0);
        assertTrue(words.contains("0.25"));
        assertTrue(words.contains("1.00"));
        assertTrue(words.contains("110"));
        assertEquals(1, count("stroke", FIRST));
        assertEquals(3, count("fill", FIRST));
        // The line runs the whole width of the plot whatever the order of
        // the data, and rises to the right.
        List<double[]> marks = new ArrayList<double[]>();
        for (int i = 0; i < operations.size(); i++) {
            if ("fill".equals(operations.get(i)) && FIRST.equals(paints.get(i))) {
                marks.add(bounds.get(i));
            }
        }
        double left = Double.MAX_VALUE;
        double right = 0;
        for (int i = 0; i < marks.size(); i++) {
            left = Math.min(left, marks.get(i)[0]);
            right = Math.max(right, marks.get(i)[2]);
        }
        assertTrue(right - left > 250);

        chart.setCreateSymbols(false);
        paint(chart);
        assertEquals(0, count("fill", FIRST));
        assertEquals(1, count("stroke", FIRST));
    }

    @Test
    public void areasAndScatteredPointsDrawWhatTheirNamesSay() {
        AreaChart<Number, Number> area = new AreaChart<Number, Number>(new NumberAxis(), new NumberAxis());
        XYChart.Series<Number, Number> s = new XYChart.Series<Number, Number>();
        s.getData().add(new XYChart.Data<Number, Number>(Integer.valueOf(0), Integer.valueOf(1)));
        s.getData().add(new XYChart.Data<Number, Number>(Integer.valueOf(10), Integer.valueOf(5)));
        area.getData().add(s);
        area.setCreateSymbols(false);
        paint(area);
        assertEquals(1, count("stroke", FIRST));
        assertEquals(1, count("fill", Color.color(FIRST.getRed(), FIRST.getGreen(), FIRST.getBlue(), 0.25)));

        ScatterChart<Number, Number> scatter = new ScatterChart<Number, Number>(new NumberAxis(), new NumberAxis());
        XYChart.Series<Number, Number> t = new XYChart.Series<Number, Number>();
        t.getData().add(new XYChart.Data<Number, Number>(Integer.valueOf(0), Integer.valueOf(1)));
        t.getData().add(new XYChart.Data<Number, Number>(Integer.valueOf(10), Integer.valueOf(5)));
        scatter.getData().add(t);
        scatter.setLegendVisible(false);
        paint(scatter);
        assertEquals(0, count("stroke", FIRST));
        assertEquals(2, count("fill", FIRST));
    }

    @Test
    public void aPieGivesEachPositiveValueASliceOfItsShare() {
        PieChart.Data quarter = new PieChart.Data("Quarter", 1);
        PieChart.Data rest = new PieChart.Data("Rest", 3);
        PieChart.Data nothing = new PieChart.Data("Nothing", 0);
        PieChart chart = new PieChart(FXCollections.observableArrayList(quarter, rest, nothing));
        assertSame(chart, quarter.getChart());
        assertNull(quarter.getNode());
        chart.setLegendVisible(false);
        paint(chart);
        List<double[]> first = filled(FIRST);
        List<double[]> second = filled(SECOND);
        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals(0, filled(Color.web("#57b757")).size());
        // Clockwise from three o'clock: the first quarter is the lower
        // right one, and the rest reaches over the whole circle.
        double[] q = first.get(0);
        double[] r = second.get(0);
        double radius = (r[2] - r[0]) / 2;
        assertEquals(radius, q[2] - q[0], 0.6);
        assertEquals(radius, q[3] - q[1], 0.6);
        assertEquals(r[2], q[2], 0.6);
        assertEquals(r[3], q[3], 0.6);
        assertTrue(words.contains("Quarter"));
        assertTrue(words.contains("Rest"));
        assertFalse(words.contains("Nothing"));

        chart.setClockwise(false);
        paint(chart);
        q = filled(FIRST).get(0);
        r = filled(SECOND).get(0);
        // Counter clockwise it is the upper right one.
        assertEquals(r[2], q[2], 0.6);
        assertEquals(r[1], q[1], 0.6);

        chart.setLabelsVisible(false);
        paint(chart);
        assertFalse(words.contains("Quarter"));

        chart.getData().remove(quarter);
        assertNull(quarter.getChart());
        paint(chart);
        // The colours follow the place in the data, so "Rest" is first now.
        assertEquals(1, filled(FIRST).size());
        assertEquals(0, filled(SECOND).size());
    }
}
