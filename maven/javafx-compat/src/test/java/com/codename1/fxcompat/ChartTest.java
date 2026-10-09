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
import javafx.geometry.Side;
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
    private static final Color PLOT = Color.web("#f4f4f4");
    private static final Color AXIS = Color.web("#c3c3c3");
    private static final Color GRID = Color.web("#dbdbdb");
    private static final Color LEGEND = Color.web("#f5f5f5");
    /// The line height and character width of the headless font.
    private static final double LINE = 32;
    private static final double CHAR = 16;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> operations = new ArrayList<String>();
    private final List<double[]> bounds = new ArrayList<double[]>();
    private final List<Paint> paints = new ArrayList<Paint>();
    private final List<String> words = new ArrayList<String>();
    private final List<String> texts = new ArrayList<String>();

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
        paint(chart, 800, 600);
    }

    private void paint(Chart chart, int width, int height) {
        operations.clear();
        bounds.clear();
        paints.clear();
        words.clear();
        texts.clear();
        chart.resize(width, height);
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                operations.add(operation);
                bounds.add(deviceBounds);
                paints.add(paint);
                texts.add(text);
                if (text != null) {
                    words.add(text);
                }
            }
        });
        chart.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(width, height, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
    }

    /// The bounds of everything drawn by an operation in a colour.
    private List<double[]> drawn(String operation, Color color) {
        List<double[]> out = new ArrayList<double[]>();
        for (int i = 0; i < operations.size(); i++) {
            if (operation.equals(operations.get(i)) && color.equals(paints.get(i)) && bounds.get(i) != null) {
                out.add(bounds.get(i));
            }
        }
        return out;
    }

    /// The box of the legend, or `null` when none was drawn.
    private double[] legendBox() {
        List<double[]> boxes = drawn("fill", LEGEND);
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    private static boolean inside(double[] inner, double[] outer) {
        return outer != null && inner[0] >= outer[0] - 1 && inner[1] >= outer[1] - 1 && inner[2] <= outer[2] + 1
                && inner[3] <= outer[3] + 1;
    }

    /// The bounds of everything filled in a colour outside the legend.
    private List<double[]> filled(Color color) {
        List<double[]> out = new ArrayList<double[]>();
        double[] legend = legendBox();
        List<double[]> all = drawn("fill", color);
        for (int i = 0; i < all.size(); i++) {
            if (!inside(all.get(i), legend)) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /// The bounds of everything filled in a colour inside the legend.
    private List<double[]> legendFilled(Color color) {
        List<double[]> out = new ArrayList<double[]>();
        double[] legend = legendBox();
        List<double[]> all = drawn("fill", color);
        for (int i = 0; i < all.size(); i++) {
            if (inside(all.get(i), legend)) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /// The rectangle of the plot of a chart of two axes.
    private double[] plot() {
        return drawn("fill", PLOT).get(0);
    }

    /// Where a text was drawn: the device place its drawing starts at,
    /// which is its top left corner while it is upright.
    private double[] word(String text) {
        for (int i = 0; i < operations.size(); i++) {
            if (text.equals(texts.get(i))) {
                return bounds.get(i);
            }
        }
        fail("not drawn: " + text);
        return null;
    }

    /// The strokes in the colour of the axes of a width and height.
    private List<double[]> marks(double width, double height) {
        List<double[]> out = new ArrayList<double[]>();
        List<double[]> all = drawn("stroke", AXIS);
        for (int i = 0; i < all.size(); i++) {
            double[] b = all.get(i);
            if (Math.abs(b[2] - b[0] - width) < 0.01 && Math.abs(b[3] - b[1] - height) < 0.01) {
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

        // The range comes from the data, holds zero and ends on a tick
        // beyond the data.
        assertEquals(0, y.getLowerBound(), 0);
        assertEquals(22.5, y.getUpperBound(), 0);
        assertEquals(2.5, y.getTickUnit(), 0);

        List<double[]> first = filled(FIRST);
        List<double[]> second = filled(SECOND);
        assertEquals(2, first.size());
        assertEquals(2, second.size());
        double a = first.get(0)[3] - first.get(0)[1];
        double b = first.get(1)[3] - first.get(1)[1];
        assertEquals(2, b / a, 0.02);
        // All bars stand on the same line.
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
        assertTrue(words.contains("0.0"));
        assertTrue(words.contains("20.0"));

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
        assertEquals(107.5, y.getLowerBound(), 0);
        assertEquals(132.5, y.getUpperBound(), 0);
        // A fixed axis keeps its bounds, and its labels have the decimals
        // each needs.
        assertEquals(1, x.getUpperBound(), 0);
        assertTrue(words.contains("0.25"));
        assertTrue(words.contains("1"));
        assertTrue(words.contains("110.0"));
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
        assertEquals(1, count("fill", Color.color(FIRST.getRed(), FIRST.getGreen(), FIRST.getBlue(), 0.2)));

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

    private static XYChart.Series<Number, Number> numbers(String name, double x1, double y1, double x2,
            double y2) {
        XYChart.Series<Number, Number> s = new XYChart.Series<Number, Number>();
        s.setName(name);
        s.getData().add(new XYChart.Data<Number, Number>(Double.valueOf(x1), Double.valueOf(y1)));
        s.getData().add(new XYChart.Data<Number, Number>(Double.valueOf(x2), Double.valueOf(y2)));
        return s;
    }

    private static double size(double[] b, int axis) {
        return b[axis + 2] - b[axis];
    }

    @Test
    public void anAxisIsDrawnOnItsSideOfThePlotWithItsTicksOutside() {
        NumberAxis x = new NumberAxis(0, 4, 1);
        NumberAxis y = new NumberAxis(10, 50, 10);
        LineChart<Number, Number> chart = new LineChart<Number, Number>(x, y);
        chart.getData().add(numbers("s", 0, 10, 4, 50));
        chart.setLegendVisible(false);
        // An axis that was given no side is at the bottom or on the left.
        assertSame(Side.BOTTOM, x.getSide());
        assertSame(Side.LEFT, y.getSide());
        paint(chart);
        // Five pixels of padding and ten around the plot and its axes;
        // an axis takes a tick of 8, a gap of 3 and its labels.
        double[] plot = plot();
        assertEquals(15 + 8 + 3 + 2 * CHAR, plot[0], 0.01);
        assertEquals(15, plot[1], 0.01);
        assertEquals(785, plot[2], 0.01);
        assertEquals(585 - 8 - 3 - LINE, plot[3], 0.01);
        assertEquals(plot[3] + 11, word("4")[1], 0.01);
        assertEquals(plot[0] - 11, word("50")[0] + 2 * CHAR, 0.01);
        List<double[]> down = marks(1, 9);
        List<double[]> left = marks(9, 1);
        assertEquals(5, down.size());
        assertEquals(5, left.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(down.get(i)[1] >= plot[3] - 0.5);
            assertTrue(left.get(i)[2] <= plot[0] + 0.5);
        }

        x.setSide(Side.TOP);
        y.setSide(Side.RIGHT);
        x.setLabel("X");
        paint(chart);
        plot = plot();
        // The plot gave up its top and its right instead, and the label
        // of the axis is at the far edge, four pixels from the ticks.
        assertEquals(15, plot[0], 0.01);
        assertEquals(15 + LINE + 4 + LINE + 3 + 8, plot[1], 0.01);
        assertEquals(785 - 8 - 3 - 2 * CHAR, plot[2], 0.01);
        assertEquals(585, plot[3], 0.01);
        assertEquals(plot[1] - 11, word("4")[1] + LINE, 0.01);
        assertEquals(plot[2] + 11, word("50")[0], 0.01);
        assertEquals(15, word("X")[1], 0.01);
        List<double[]> up = marks(1, 9);
        List<double[]> right = marks(9, 1);
        assertEquals(5, up.size());
        assertEquals(5, right.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(up.get(i)[3] <= plot[1] + 0.5);
            assertTrue(right.get(i)[0] >= plot[2] - 0.5);
        }

        // A side across the direction of an axis is taken as its usual one.
        x.setSide(Side.LEFT);
        x.setLabel(null);
        paint(chart);
        assertEquals(15, plot()[1], 0.01);
        assertEquals(plot()[3] + 11, word("4")[1], 0.01);
    }

    @Test
    public void tickLabelsAreTurnedAndTheAxisMakesRoomForThem() {
        CategoryAxis x = new CategoryAxis(FXCollections.observableArrayList("January", "February"));
        NumberAxis y = new NumberAxis(0, 10, 5);
        BarChart<String, Number> chart = new BarChart<String, Number>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<String, Number>();
        s.getData().add(new XYChart.Data<String, Number>("January", Integer.valueOf(4)));
        s.getData().add(new XYChart.Data<String, Number>("February", Integer.valueOf(8)));
        chart.getData().add(s);
        chart.setLegendVisible(false);
        paint(chart);
        double upright = plot()[3];
        double tick = marks(1, 9).get(0)[0];
        assertEquals(585 - 11 - LINE, upright, 0.01);
        // Upright, a label is centred under its tick.
        assertEquals(tick - 7 * CHAR / 2, word("January")[0], 0.01);
        assertEquals(upright + 11, word("January")[1], 0.01);

        x.setTickLabelRotation(90);
        paint(chart);
        double turned = plot()[3];
        // The axis is as deep as its longest label is wide now.
        assertEquals(585 - 11 - 8 * CHAR, turned, 0.01);
        // A label turned a quarter clockwise about its middle starts at
        // the top of its box, half a line right of its tick.
        assertEquals(tick + LINE / 2, word("January")[0], 0.01);
        assertEquals(turned + 11, word("January")[1], 0.01);
        // The shorter label is centred in the room of the longer one...
        assertEquals(turned + 11, word("February")[1], 0.01);

        x.setTickLabelRotation(45);
        paint(chart);
        double diagonal = (8 * CHAR + LINE) * Math.sqrt(0.5);
        assertEquals(585 - Math.ceil(11 + diagonal), plot()[3], 0.01);

        // The labels of a vertical axis are turned about their middles too.
        x.setTickLabelRotation(0);
        y.setTickLabelRotation(90);
        paint(chart);
        // "10" turned is a line wide and two characters high.
        assertEquals(15 + 11 + LINE, plot()[0], 0.01);
        double level = marks(9, 1).get(marks(9, 1).size() - 1)[1];
        assertEquals(plot()[0] - 11, word("10")[0], 0.01);
        assertEquals(level - CHAR, word("10")[1], 0.01);
    }

    @Test
    public void categoryLabelsThatDoNotFitSideBySideAreStoodOnEnd() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 10, 5);
        LineChart<String, Number> chart = new LineChart<String, Number>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<String, Number>();
        for (int i = 0; i < 8; i++) {
            s.getData().add(new XYChart.Data<String, Number>("Category " + i, Integer.valueOf(i)));
        }
        chart.getData().add(s);
        chart.setLegendVisible(false);
        paint(chart);
        double[] plot = plot();
        assertEquals(585 - 11 - 10 * CHAR, plot[3], 0.01);
        double tick = marks(1, 9).get(0)[0];
        assertEquals(tick + LINE / 2, word("Category 0")[0], 0.01);
        assertEquals(plot[3] + 11, word("Category 0")[1], 0.01);
        // Every category has a band of the axis between its margins of
        // five pixels, and its tick in the middle of the band.
        double band = (plot[2] - plot[0] - 10) / 8;
        assertEquals(plot[0] + 5 + band / 2, tick, 0.51);
        assertEquals(8, marks(1, 9).size());
        assertEquals(plot[0] + 5 + band * 7.5, marks(1, 9).get(7)[0], 0.51);
    }

    @Test
    public void minorTicksCutTheDistanceBetweenTwoTicks() {
        NumberAxis x = new NumberAxis(0, 10, 5);
        NumberAxis y = new NumberAxis(0, 10, 5);
        y.setMinorTickVisible(false);
        assertEquals(5, x.getMinorTickCount());
        assertEquals(5, x.getMinorTickLength(), 0);
        assertTrue(x.isMinorTickVisible());
        LineChart<Number, Number> chart = new LineChart<Number, Number>(x, y);
        chart.getData().add(numbers("s", 0, 0, 10, 10));
        chart.setLegendVisible(false);
        paint(chart);
        double[] plot = plot();
        // Three ticks, and four minor ones of five pixels in each of the
        // two gaps; none on the axis that turned them off.
        assertEquals(3, marks(1, 9).size());
        List<double[]> minor = marks(1, 6);
        assertEquals(8, minor.size());
        assertEquals(0, marks(6, 1).size());
        double width = plot[2] - plot[0];
        for (int i = 0; i < 4; i++) {
            assertEquals(plot[0] + width * (i + 1) / 10, minor.get(i)[0], 0.51);
            assertEquals(plot[0] + width * (i + 6) / 10, minor.get(i + 4)[0], 0.51);
            assertEquals(plot[3] - 0.5, minor.get(i)[1], 0.01);
        }

        x.setMinorTickCount(2);
        x.setMinorTickLength(3);
        paint(chart);
        minor = marks(1, 4);
        assertEquals(2, minor.size());
        assertEquals(plot[0] + width / 4, minor.get(0)[0], 0.51);
        assertEquals(0, marks(1, 6).size());

        // On the top they point up, away from the plot.
        x.setSide(Side.TOP);
        paint(chart);
        minor = marks(1, 4);
        assertEquals(2, minor.size());
        assertEquals(plot()[1] + 0.5, minor.get(0)[3], 0.01);

        x.setMinorTickVisible(false);
        paint(chart);
        assertEquals(0, marks(1, 4).size());
        assertEquals(3, marks(1, 9).size());
    }

    @Test
    public void autoRangingChoosesTheBoundsAndTheTickUnitJavaFxDoes() {
        // The data of the area chart sample: X from 0 to 10, Y from 2 to 9.
        NumberAxis x = new NumberAxis();
        x.setLabel("X Values");
        NumberAxis y = new NumberAxis();
        y.setLabel("Y Values");
        AreaChart<Number, Number> area = new AreaChart<Number, Number>(x, y);
        area.getData().add(numbers("Series 1", 0, 2, 10, 9));
        paint(area, 1280, 860);
        // Padded by a hundredth at each end but never across zero, then
        // rounded outwards to the unit: half a unit beyond 9, one beyond 10.
        assertEquals(0, y.getLowerBound(), 0);
        assertEquals(9.5, y.getUpperBound(), 0);
        assertEquals(0.5, y.getTickUnit(), 0);
        assertEquals(0, x.getLowerBound(), 0);
        assertEquals(11, x.getUpperBound(), 0);
        assertEquals(1, x.getTickUnit(), 0);
        // The labels have the decimals of the unit.
        assertTrue(words.contains("9.5"));
        assertTrue(words.contains("9.0"));
        assertTrue(words.contains("11"));
        assertFalse(words.contains("11.0"));

        // The data of the category line chart sample.
        NumberAxis v = new NumberAxis();
        v.setLabel("Y Axis");
        CategoryAxis c = new CategoryAxis();
        c.setLabel("X Axis");
        LineChart<String, Number> line = new LineChart<String, Number>(c, v);
        line.setTitle("LineChart with Category Axis");
        XYChart.Series<String, Number> s = new XYChart.Series<String, Number>();
        s.setName("Data Series 1");
        String[] names = {"Alpha", "Beta", "RC1", "RC2", "1.0", "1.1"};
        int[] values = {50, 80, 90, 30, 122, 10};
        for (int i = 0; i < names.length; i++) {
            s.getData().add(new XYChart.Data<String, Number>(names[i], Integer.valueOf(values[i])));
        }
        line.getData().add(s);
        paint(line, 1280, 860);
        assertEquals(0, v.getLowerBound(), 0);
        assertEquals(130, v.getUpperBound(), 0);
        assertEquals(10, v.getTickUnit(), 0);
        assertTrue(words.contains("130"));
        assertTrue(words.contains("Alpha"));

        // A range that does not hold zero is padded at both ends, and a
        // unit that would make more than twenty ticks is doubled.
        NumberAxis free = new NumberAxis();
        free.setForceZeroInRange(false);
        LineChart<Number, Number> hug = new LineChart<Number, Number>(new NumberAxis(0, 1, 1), free);
        hug.getData().add(numbers("s", 0, 1000, 1, 3000));
        paint(hug, 1280, 860);
        assertEquals(750, free.getLowerBound(), 0);
        assertEquals(3250, free.getUpperBound(), 0);
        assertEquals(250, free.getTickUnit(), 0);
        // Thousands are grouped from a unit of a hundred on.
        assertTrue(words.contains("3,000"));

        // Where the labels of a unit would touch, the unit is doubled
        // until they do not.
        paint(hug, 1280, 300);
        assertEquals(1000, free.getTickUnit(), 0);
        assertEquals(0, free.getLowerBound(), 0);
        assertEquals(4000, free.getUpperBound(), 0);

        // A fixed range groups its thousands and writes the decimals a
        // label needs.
        NumberAxis fixed = new NumberAxis("Units Sold", 0, 3000, 1000);
        BarChart<String, Number> bars = new BarChart<String, Number>(new CategoryAxis(), fixed);
        bars.getData().add(series("Apples", 567, 1292));
        paint(bars);
        assertTrue(words.contains("3,000"));
        assertTrue(words.contains("0"));
        assertEquals(3000, fixed.getUpperBound(), 0);
    }

    @Test
    public void theLegendShowsTheSymbolOfEachSeriesOnItsSide() {
        BarChart<String, Number> bars = new BarChart<String, Number>(new CategoryAxis(), new NumberAxis());
        bars.getData().add(series("2023", 10, 20));
        bars.getData().add(series("2024", 5, 15));
        paint(bars);
        double[] box = legendBox();
        // Below the plot and in the middle: six pixels of padding around
        // two tiles five pixels apart, each a square of 16, a gap of 4
        // and the name.
        double tile = 16 + 4 + 4 * CHAR;
        assertEquals(595, box[3] + 1, 0.01);
        assertEquals(LINE + 12, box[3] - box[1] + 2, 0.01);
        assertEquals(2 * tile + 5 + 12, box[2] - box[0] + 2, 0.01);
        assertEquals(400, (box[0] + box[2]) / 2, 1);
        assertTrue(plot()[3] < box[1]);
        List<double[]> first = legendFilled(FIRST);
        List<double[]> second = legendFilled(SECOND);
        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals(16, size(first.get(0), 0), 0.01);
        assertEquals(16, size(first.get(0), 1), 0.01);
        assertEquals(first.get(0)[0] + tile + 5, second.get(0)[0], 0.01);
        assertEquals(first.get(0)[1], second.get(0)[1], 0.01);
        assertEquals(first.get(0)[2] + 4, word("2023")[0], 0.01);

        bars.setLegendSide(Side.TOP);
        paint(bars);
        box = legendBox();
        assertEquals(5, box[1] - 1, 0.01);
        assertTrue(plot()[1] > box[3]);

        bars.setLegendSide(Side.LEFT);
        paint(bars);
        box = legendBox();
        // Beside the plot the entries are one under the other.
        assertEquals(5, box[0] - 1, 0.01);
        assertEquals(300, (box[1] + box[3]) / 2, 1);
        assertEquals(legendFilled(FIRST).get(0)[0], legendFilled(SECOND).get(0)[0], 0.01);
        assertEquals(legendFilled(FIRST).get(0)[1] + LINE + 5, legendFilled(SECOND).get(0)[1], 0.01);
        assertTrue(plot()[0] > box[2]);

        bars.setLegendSide(Side.RIGHT);
        paint(bars);
        box = legendBox();
        assertEquals(795, box[2] + 1, 0.01);
        assertTrue(plot()[2] < box[0]);

        // Entries that do not fit in a row go on to the next.
        bars.setLegendSide(Side.BOTTOM);
        paint(bars, 180, 600);
        box = legendBox();
        assertEquals(2 * LINE + 5 + 12, box[3] - box[1] + 2, 0.01);
        assertEquals(legendFilled(FIRST).get(0)[0], legendFilled(SECOND).get(0)[0], 0.01);

        // A line has a ring of ten pixels with a white middle of six.
        LineChart<Number, Number> line = new LineChart<Number, Number>(new NumberAxis(), new NumberAxis());
        line.getData().add(numbers("one", 0, 1, 10, 5));
        paint(line);
        double[] ring = legendFilled(FIRST).get(0);
        double[] hole = legendFilled(Color.WHITE).get(0);
        assertEquals(10, size(ring, 0), 0.5);
        assertEquals(6, size(hole, 0), 0.5);
        assertEquals((ring[0] + ring[2]) / 2, (hole[0] + hole[2]) / 2, 0.01);
        assertEquals((ring[1] + ring[3]) / 2, (hole[1] + hole[3]) / 2, 0.01);

        // An area has a ring of twelve.
        AreaChart<Number, Number> area = new AreaChart<Number, Number>(new NumberAxis(), new NumberAxis());
        area.getData().add(numbers("one", 0, 1, 10, 5));
        paint(area);
        assertEquals(12, size(legendFilled(FIRST).get(0), 0), 0.5);
        assertEquals(6, size(legendFilled(Color.WHITE).get(0), 0), 0.5);

        // A pie has a disc for each slice.
        PieChart pie = new PieChart(FXCollections.observableArrayList(new PieChart.Data("a", 1),
                new PieChart.Data("b", 3)));
        paint(pie);
        assertEquals(14, size(legendFilled(FIRST).get(0), 0), 0.5);
        assertEquals(14, size(legendFilled(FIRST).get(0), 1), 0.5);
        assertEquals(14, size(legendFilled(SECOND).get(0), 0), 0.5);
        assertEquals(0, legendFilled(Color.WHITE).size());

        // Scattered points have the shape of their series: a disc for
        // the first, a square for the second, a diamond for the third.
        ScatterChart<Number, Number> scatter = new ScatterChart<Number, Number>(new NumberAxis(), new NumberAxis());
        scatter.getData().add(numbers("one", 0, 1, 10, 5));
        scatter.getData().add(numbers("two", 1, 2, 9, 4));
        scatter.getData().add(numbers("three", 2, 3, 8, 3));
        paint(scatter);
        Color third = Color.web("#57b757");
        assertEquals(10, size(legendFilled(FIRST).get(0), 1), 0.5);
        assertEquals(10, size(legendFilled(SECOND).get(0), 1), 0.01);
        assertEquals(14, size(legendFilled(third).get(0), 1), 0.01);
        assertEquals(2, filled(SECOND).size());
        assertEquals(10, size(filled(SECOND).get(0), 0), 0.01);
        assertEquals(10, size(filled(SECOND).get(0), 1), 0.01);
        assertEquals(10, size(filled(third).get(0), 0), 0.01);
        assertEquals(14, size(filled(third).get(0), 1), 0.01);
    }

    @Test
    public void thePlotHasDashedGridLinesAndCutsOffWhatLeavesIt() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 10, 5);
        LineChart<String, Number> chart = new LineChart<String, Number>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<String, Number>();
        s.getData().add(new XYChart.Data<String, Number>("A", Integer.valueOf(0)));
        s.getData().add(new XYChart.Data<String, Number>("B", Integer.valueOf(10)));
        s.getData().add(new XYChart.Data<String, Number>("C", Integer.valueOf(5)));
        chart.getData().add(s);
        chart.setLegendVisible(false);
        paint(chart);
        double[] plot = plot();
        // A line up from each category, and one across at 5 and at 10;
        // the one at zero is the zero line, which the axis lies on.
        List<double[]> grid = drawn("stroke", GRID);
        assertEquals(5, grid.size());
        double band = (plot[2] - plot[0] - 10) / 3;
        for (int i = 0; i < 3; i++) {
            assertEquals(1, size(grid.get(i), 0), 0.01);
            assertEquals(plot[0] + 5 + band * (i + 0.5), grid.get(i)[0], 0.51);
            assertEquals(plot[1], grid.get(i)[1], 0.51);
        }
        assertEquals(1, size(grid.get(3), 1), 0.01);
        assertEquals((plot[1] + plot[3]) / 2, grid.get(3)[1], 0.51);
        assertEquals(plot[1], grid.get(4)[1], 0.01);
        // The series is drawn inside a clip of the plot and a pixel more.
        int clips = 0;
        for (int i = 0; i < operations.size(); i++) {
            if ("clip".equals(operations.get(i))) {
                clips++;
                assertEquals(plot[0], bounds.get(i)[0], 0.01);
                assertEquals(plot[1], bounds.get(i)[1], 0.01);
                assertEquals(plot[2] + 1, bounds.get(i)[2], 0.01);
                assertEquals(plot[3] + 1, bounds.get(i)[3], 0.01);
            }
        }
        assertEquals(1, clips);
        // The symbol of a line is a ring of ten pixels with a hole of six.
        assertEquals(10, size(filled(FIRST).get(0), 0), 0.5);
        assertEquals(6, size(filled(Color.WHITE).get(0), 0), 0.5);

        chart.setVerticalGridLinesVisible(false);
        paint(chart);
        assertEquals(2, drawn("stroke", GRID).size());
        chart.setHorizontalGridLinesVisible(false);
        paint(chart);
        assertEquals(0, drawn("stroke", GRID).size());
    }

    @Test
    public void barsShareTheBandOfTheirCategoryLessItsGaps() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 20, 10);
        BarChart<String, Number> chart = new BarChart<String, Number>(x, y);
        chart.getData().add(series("2023", 10, 20));
        chart.getData().add(series("2024", 5, 15));
        chart.setLegendVisible(false);
        chart.setCategoryGap(25);
        paint(chart);
        double[] plot = plot();
        double band = (plot[2] - plot[0] - 10) / 2;
        double bar = (band - 25 - 4) / 2 - 4;
        List<double[]> first = filled(FIRST);
        List<double[]> second = filled(SECOND);
        assertEquals(bar, size(first.get(0), 0), 1.01);
        assertEquals(bar, size(second.get(0), 0), 1.01);
        // Half the gap between two categories is before the first bar,
        // and the gap between two bars after each.
        assertEquals(plot[0] + 5 + 12.5, first.get(0)[0], 0.51);
        assertEquals(first.get(0)[0] + bar + 4, second.get(0)[0], 1.01);
        assertEquals(first.get(0)[0] + band, first.get(1)[0], 1.01);
        // A bar of the whole range is as tall as the plot.
        assertEquals(plot[1], first.get(1)[1], 0.01);
        assertEquals(plot[3], first.get(1)[3], 0.01);

        // The symbol of an area is a ring of six pixels with a hole of four.
        AreaChart<Number, Number> area = new AreaChart<Number, Number>(new NumberAxis(), new NumberAxis());
        area.getData().add(numbers("one", 0, 1, 10, 5));
        area.setLegendVisible(false);
        paint(area);
        assertEquals(2, filled(FIRST).size());
        assertEquals(6, size(filled(FIRST).get(0), 0), 0.5);
        assertEquals(4, size(filled(Color.WHITE).get(0), 0), 0.5);
    }
}
