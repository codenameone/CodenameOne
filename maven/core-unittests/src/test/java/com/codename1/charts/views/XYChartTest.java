package com.codename1.charts.views;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(1d, selection.getValue(), 1e-6);
        assertEquals(2d, selection.getXValue(), 1e-6);
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
    }
}
