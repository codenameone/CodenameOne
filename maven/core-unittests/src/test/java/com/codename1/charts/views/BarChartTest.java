package com.codename1.charts.views;

import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BarChartTest {
    private XYMultipleSeriesDataset dataset;
    private XYMultipleSeriesRenderer renderer;

    @BeforeEach
    public void setup() {
        dataset = new XYMultipleSeriesDataset();
        XYSeries series1 = new XYSeries("s1");
        series1.add(1, 10);
        XYSeries series2 = new XYSeries("s2");
        series2.add(1, 20);
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        renderer = new XYMultipleSeriesRenderer();
        XYSeriesRenderer r1 = new XYSeriesRenderer();
        r1.setColor(ColorUtil.BLUE);
        XYSeriesRenderer r2 = new XYSeriesRenderer();
        r2.setColor(ColorUtil.GREEN);
        renderer.addSeriesRenderer(r1);
        renderer.addSeriesRenderer(r2);
    }

    private ExposedBarChart createChart(BarChart.Type type) {
        return new ExposedBarChart(dataset, renderer, type);
    }

    @Test
    public void testChartDefaults() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        assertEquals("Bar", chart.getChartType());
        assertEquals(0.0, chart.getDefaultMinimum(), 1e-6);
        assertTrue(chart.isRenderNullValues());
        assertEquals(12, chart.getLegendShapeWidth(0));
        assertEquals(1f, chart.callGetCoeficient());
    }

    @Test
    public void testGetHalfDiffXUsesRendererWidth() {
        renderer.setBarWidth(8f);
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        List<Float> points = Arrays.asList(5f, 10f, 10f, 20f);
        float half = chart.callGetHalfDiffX(points, points.size(), dataset.getSeriesCount());
        assertEquals(4f, half, 1e-6f);
    }

    @Test
    public void testGetHalfDiffXWithAutomaticWidth() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        List<Float> points = Arrays.asList(10f, 10f, 30f, 20f, 50f, 30f);
        float half = chart.callGetHalfDiffX(points, points.size(), dataset.getSeriesCount());
        assertEquals(5f, half, 1e-6f);
    }

    @Test
    public void testClickableAreasDefaultType() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        List<Float> points = Arrays.asList(20f, 40f);
        List<Double> values = Arrays.asList(1d, 2d);
        ClickableArea[] areas = chart.callClickableAreas(points, values, 0f, 1, 0);
        assertEquals(1, areas.length);
        assertNotNull(areas[0]);
        double expectedLeft = 20 - dataset.getSeriesCount() * 5 + 1 * 10;
        assertEquals(expectedLeft, areas[0].getRect().getX(), 1e-6);
        assertEquals(10, areas[0].getRect().getWidth(), 1e-6);
    }

    @Test
    public void testClickableAreasStackedType() {
        ExposedBarChart chart = createChart(BarChart.Type.STACKED);
        List<Float> points = Arrays.asList(20f, 40f);
        List<Double> values = Arrays.asList(1d, 2d);
        ClickableArea[] areas = chart.callClickableAreas(points, values, 10f, 0, 0);
        assertEquals(1, areas.length);
        assertNotNull(areas[0]);
        assertEquals(10, areas[0].getRect().getX(), 1e-6);
        assertEquals(20, areas[0].getRect().getWidth(), 1e-6);
        assertEquals(10f, areas[0].getRect().getY(), 1e-6);
    }

    @Test
    public void testDrawBarPositionsForDefaultType() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        Paint paint = new Paint();
        chart.callDrawBar(canvas, 20f, 0f, 20f, 30f, 5f, dataset.getSeriesCount(), 1, paint);
        assertEquals(1, canvas.rectangles.size());
        float[] rect = canvas.rectangles.get(0);
        assertEquals(20f, rect[0], 1e-6f);
        assertEquals(0f, rect[1], 1e-6f);
        assertEquals(30f, rect[3], 1e-6f);
    }

    @Test
    public void testDrawBarNormalizesCoordinates() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        Paint paint = new Paint();
        chart.callDrawBarWithScale(canvas, 30f, 40f, 10f, 20f, 0, 0, paint);
        assertEquals(1, canvas.rectangles.size());
        float[] rect = canvas.rectangles.get(0);
        assertEquals(10f, rect[0], 1e-6f);
        assertEquals(20f, rect[1], 1e-6f);
        assertEquals(30f, rect[2], 1e-6f);
        assertEquals(40f, rect[3], 1e-6f);
    }

    @Test
    public void testGradientPartialColorBlendsChannels() {
        ExposedBarChart chart = createChart(BarChart.Type.DEFAULT);
        int minColor = ColorUtil.argb(255, 0, 0, 255);
        int maxColor = ColorUtil.argb(255, 255, 0, 0);
        int mixed = chart.callGetGradientPartialColor(minColor, maxColor, 0.25f);
        assertEquals(191, ColorUtil.red(mixed));
        assertEquals(0, ColorUtil.green(mixed));
        assertEquals(64, ColorUtil.blue(mixed));
    }

    @Test
    public void testDrawSeriesHeapedAdjustsPreviousValues() {
        ExposedBarChart chart = createChart(BarChart.Type.HEAPED);
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        Paint paint = new Paint();
        List<Float> firstSeriesPoints = new ArrayList<Float>(Arrays.asList(5f, 15f));
        chart.drawSeries(canvas, paint, firstSeriesPoints, (XYSeriesRenderer) renderer.getSeriesRendererAt(0), 0f, 0, 0);
        assertEquals(1, canvas.rectangles.size());
        float[] firstRect = canvas.rectangles.get(0);
        assertEquals(0f, firstRect[1], 1e-6f);
        assertEquals(15f, firstRect[3], 1e-6f);

        canvas.prepare();
        List<Float> secondSeriesPoints = new ArrayList<Float>(Arrays.asList(5f, 10f));
        chart.drawSeries(canvas, paint, secondSeriesPoints, (XYSeriesRenderer) renderer.getSeriesRendererAt(1), 0f, 1, 0);
        assertEquals(1, canvas.rectangles.size());
        float[] secondRect = canvas.rectangles.get(0);
        assertEquals(15f, secondRect[1], 1e-6f);
        assertEquals(25f, secondRect[3], 1e-6f);
        assertEquals(25f, secondSeriesPoints.get(1), 1e-6f);
    }

    private static class ExposedBarChart extends BarChart {
        ExposedBarChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type) {
            super(dataset, renderer, type);
        }

        float callGetHalfDiffX(List<Float> points, int length, int seriesNr) {
            return super.getHalfDiffX(points, length, seriesNr);
        }

        float callGetCoeficient() {
            return super.getCoeficient();
        }

        ClickableArea[] callClickableAreas(List<Float> points, List<Double> values, float yAxisValue, int seriesIndex, int startIndex) {
            return super.clickableAreasForPoints(points, values, yAxisValue, seriesIndex, startIndex);
        }

        void callDrawBar(ChartTestUtils.RecordingCanvas canvas, float xMin, float yMin, float xMax, float yMax,
                         float halfDiffX, int seriesNr, int seriesIndex, Paint paint) {
            super.drawBar(canvas, xMin, yMin, xMax, yMax, halfDiffX, seriesNr, seriesIndex, paint);
        }

        void callDrawBarWithScale(ChartTestUtils.RecordingCanvas canvas, float xMin, float yMin, float xMax, float yMax,
                                  int scale, int seriesIndex, Paint paint) {
            super.drawBar(canvas, xMin, yMin, xMax, yMax, scale, seriesIndex, paint);
        }

        int callGetGradientPartialColor(int minColor, int maxColor, float fraction) {
            return super.getGradientPartialColor(minColor, maxColor, fraction);
        }
    }
}
