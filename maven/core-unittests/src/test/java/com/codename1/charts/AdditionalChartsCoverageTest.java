package com.codename1.charts;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.models.XYValueSeries;
import com.codename1.charts.renderers.DialRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BubbleChart;
import com.codename1.charts.views.ChartComponent;
import com.codename1.charts.views.CombinedXYChart;
import com.codename1.charts.views.CubicLineChart;
import com.codename1.charts.views.DialChart;
import com.codename1.charts.views.LineChart;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdditionalChartsCoverageTest extends UITestBase {

    @FormTest
    void chartComponentHandlesPanAndPinchZoomLimits() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setPanEnabled(true, true);
        renderer.setZoomEnabled(true, true);
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(10);
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(10);
        renderer.setPanLimits(new double[]{0, 6, 0, 6});
        renderer.setZoomLimits(new double[]{4, 8, 4, 8});

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(new XYSeries("one"));

        TrackingXYChart chart = new TrackingXYChart(dataset, renderer);
        ChartComponent component = new ChartComponent(chart);
        component.setPreferredW(120);
        component.setPreferredH(120);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, component);
        form.show();

        component.pointerDragged(new int[]{20, 60}, new int[]{20, 60});
        component.pointerDragged(new int[]{30, 50}, new int[]{30, 50});
        assertTrue(chart.boundsChangedCount > 0);
        double[] rangeAfterZoom = renderer.getRange();
        assertTrue(rangeAfterZoom[0] >= 0d);
        assertTrue(rangeAfterZoom[1] <= 6d);
        assertTrue(rangeAfterZoom[2] >= 0d);
        assertTrue(rangeAfterZoom[3] <= 6d);

        component.pointerDragged(new int[]{40}, new int[]{40});
        component.pointerDragged(new int[]{30}, new int[]{30});
        double[] rangeAfterPan = renderer.getRange();
        assertTrue(rangeAfterPan[0] >= 0d);
        assertTrue(rangeAfterPan[1] <= 6d);
        assertTrue(rangeAfterPan[2] >= 0d);
        assertTrue(rangeAfterPan[3] <= 6d);

        ChartComponent replacement = new ChartComponent(chart);
        component.setChart(chart);
        assertSame(chart, component.getChart());
        assertEquals(replacement.getChart().getChartType(), component.getChart().getChartType());
    }

    @FormTest
    void nonXYChartDragAppliesTransform() {
        TransformChart chart = new TransformChart();
        ChartComponent component = new ChartComponent(chart);
        component.setPanEnabled(true);
        component.setPreferredW(80);
        component.setPreferredH(80);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, component);
        form.show();

        component.pointerDragged(new int[]{10}, new int[]{12});
        component.pointerDragged(new int[]{20}, new int[]{22});

        assertNotNull(component.getTransform());
        assertTrue(chart.dragged);
    }

    @FormTest
    void bubbleChartRendersAndProvidesClickableAreas() {
        XYValueSeries values = new XYValueSeries("values");
        values.add(1, 2, 3);
        values.add(2, 4, 1);
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(values);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(new XYSeriesRenderer());

        RecordingBubbleChart chart = new RecordingBubbleChart(dataset, renderer);
        Canvas canvas = createCanvas();
        Paint paint = new Paint();
        List<Float> points = Arrays.asList(10f, 20f, 30f, 40f);
        chart.drawSeries(canvas, paint, points, (XYSeriesRenderer) renderer.getSeriesRendererAt(0), 0, 0, 0);

        assertEquals(2, chart.drawnCenters.size());
        assertTrue(chart.drawnRadii.get(0) >= 2);
        assertEquals(BubbleChart.TYPE, chart.getChartType());
        assertEquals(10, chart.getLegendShapeWidth(0));

        List<Double> valuesList = Arrays.asList(1d, 2d, 3d, 4d);
        assertEquals(2, chart.clickableAreasForPoints(points, valuesList, 0, 0, 0).length);
    }

    @FormTest
    void combinedChartDelegatesDrawing() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries seriesA = new XYSeries("A");
        seriesA.add(1, 1);
        dataset.addSeries(seriesA);

        XYSeriesRenderer rendererA = new XYSeriesRenderer();
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(rendererA);

        CombinedXYChart.XYCombinedChartDef[] defs = new CombinedXYChart.XYCombinedChartDef[]{
                new CombinedXYChart.XYCombinedChartDef(LineChart.TYPE, 0)
        };

        CombinedXYChart chart = new CombinedXYChart(dataset, renderer, defs);
        Canvas canvas = createCanvas();
        Paint paint = new Paint();
        List<Float> points = new ArrayList<Float>();
        points.add(5f);
        points.add(5f);

        chart.drawSeries(canvas, paint, points, rendererA, 0f, 0, 0);
        assertEquals(LineChart.TYPE, chart.getChartType());
        assertTrue(chart.getLegendShapeWidth(0) > 0);
        assertNotNull(chart.clickableAreasForPoints(points, Arrays.asList(1d, 1d), 0f, 0, 0));
    }

    @FormTest
    void cubicLineChartDrawsPathAndPoints() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(new XYSeries("curve"));
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(new XYSeriesRenderer());
        CubicLineChart chart = new CubicLineChart(dataset, renderer, 0.5f);

        Canvas canvas = createCanvas();
        Paint paint = new Paint();
        List<Float> points = Arrays.asList(0f, 0f, 10f, 10f, 20f, 5f);
        chart.drawPath(canvas, points, paint, true);
        chart.drawPoints(canvas, paint, points, (XYSeriesRenderer) renderer.getSeriesRendererAt(0), 0f, 0, 0);
        assertEquals(CubicLineChart.TYPE, chart.getChartType());
    }

    @FormTest
    void dialChartComputesAnglesAndTicks() {
        CategorySeries series = new CategorySeries("dial");
        series.add("first", 50);
        DialRenderer renderer = new DialRenderer();
        renderer.setMinValue(0);
        renderer.setMaxValue(100);
        renderer.addSeriesRenderer(new SimpleSeriesRenderer());

        DialChart chart = new DialChart(series, renderer);
        Canvas canvas = createCanvas();
        Paint paint = new Paint();

        chart.draw(canvas, 0, 0, 80, 80, paint);
        assertEquals(DialChart.TYPE, chart.getChartType());
        double angle = chart.getAngleForValue(0, 0, 100, -150, 150, true);
        assertFalse(Double.isNaN(angle));
        assertTrue(angle <= 150d);
    }

    @FormTest
    void pathMeasureReturnsLengthAndPositions() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.lineTo(10, 0);
        path.lineTo(10, 10);
        com.codename1.charts.compat.PathMeasure measure = new com.codename1.charts.compat.PathMeasure(path, false);

        assertTrue(measure.getLength() > 0);
        float[] pos = new float[2];
        float[] tan = new float[2];
        measure.getPosTan(1, pos, tan);
        assertEquals(2, pos.length);
        assertEquals(2, tan.length);
    }

    @FormTest
    void categorySeriesAndMultipleCategorySeriesMutations() {
        CategorySeries single = new CategorySeries("titles");
        single.add("a", 1);
        single.add(2);
        single.set(1, "b", 3);
        assertEquals(2, single.getItemCount());
        assertEquals("b", single.getCategory(1));
        assertEquals(1, single.getValue(0), 0.001);
        single.remove(0);
        assertEquals(1, single.getItemCount());
        assertNotNull(single.toXYSeries());

        MultipleCategorySeries multiple = new MultipleCategorySeries("multi");
        multiple.add(new String[]{"row"}, new double[]{1});
        multiple.add("row2", new String[]{"c1", "c2"}, new double[]{2, 3});
        assertEquals(2, multiple.getCategoriesCount());
        assertEquals(2, multiple.getItemCount(1));
        assertEquals("row2", multiple.getCategory(1));
        assertEquals(3d, multiple.getValues(1)[1], 0.001);
        assertNotNull(multiple.toXYSeries());

        assertTrue(StringUtil.tokenize(multiple.toString(), " ").size() > 0);
    }

    private Canvas createCanvas() {
        Canvas canvas = new Canvas();
        canvas.g = Image.createImage(120, 120).getGraphics();
        return canvas;
    }

    private static class TrackingXYChart extends com.codename1.charts.views.XYChart {
        int boundsChangedCount;

        TrackingXYChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
            super(dataset, renderer);
        }

        @Override
        protected void chartBoundsChanged() {
            boundsChangedCount++;
        }

        @Override
        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
        }

        @Override
        public void drawSeries(Canvas canvas, Paint paint, List<Float> points, XYSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex, int startIndex) {
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, int seriesIndex, Paint paint) {
        }

        @Override
        protected com.codename1.charts.views.ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values, float yAxisValue, int seriesIndex, int startIndex) {
            return new com.codename1.charts.views.ClickableArea[0];
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public String getChartType() {
            return "Tracking";
        }

        @Override
        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point screenPoint) {
            return new SeriesSelection(0, 0, screenPoint.getX(), screenPoint.getY());
        }
    }

    private static class TransformChart extends AbstractChart {
        boolean dragged;

        @Override
        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
        }

        @Override
        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point screenPoint) {
            dragged = true;
            return null;
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, Paint paint) {
        }

        @Override
        public String getChartType() {
            return "Transform";
        }
    }

    private static class RecordingBubbleChart extends BubbleChart {
        final List<Point> drawnCenters = new ArrayList<Point>();
        final List<Float> drawnRadii = new ArrayList<Float>();

        RecordingBubbleChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
            super(dataset, renderer);
        }

        @Override
        public void drawCircle(Canvas canvas, Paint paint, float x, float y, float radius) {
            drawnCenters.add(new Point(x, y));
            drawnRadii.add(radius);
        }
    }
}
