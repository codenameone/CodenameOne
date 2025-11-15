package com.codename1.charts;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.LineChart;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartComponentInteractionTest extends UITestBase {

    @FormTest
    void panAndZoomFlagsPropagateToRenderer() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 1);
        series.add(1, 2);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(new XYSeriesRenderer());
        ChartComponent component = new ChartComponent(new LineChart(dataset, renderer));

        Form form = new Form();
        form.add(component);
        form.show();

        assertFalse(component.isZoomEnabled());
        assertFalse(renderer.isZoomEnabled());

        component.setZoomEnabled(true);
        assertTrue(component.isZoomEnabled());
        assertTrue(renderer.isZoomEnabled());

        component.setZoomEnabled(false, true);
        assertTrue(component.isZoomEnabled());
        assertFalse(renderer.isZoomXEnabled());
        assertTrue(renderer.isZoomYEnabled());

        component.setPanEnabled(true);
        assertTrue(renderer.isPanEnabled());
        component.setPanEnabled(false, true);
        assertTrue(renderer.isPanYEnabled());
        assertFalse(renderer.isPanXEnabled());
    }

    @FormTest
    void chartUtilDelegatesToChartDraw() {
        RecordingChart chart = new RecordingChart();
        ChartUtil util = new ChartUtil();
        Image buffer = Image.createImage(40, 30);
        Graphics graphics = buffer.getGraphics();
        Rectangle bounds = new Rectangle(5, 6, 20, 10);

        util.paintChart(graphics, chart, bounds, 7, 9);

        assertSame(graphics, chart.lastGraphics);
        assertEquals(bounds, chart.lastBounds);
        assertEquals(7, chart.lastAbsX);
        assertEquals(9, chart.lastAbsY);
    }

    @FormTest
    void transformSetterAndGetterPersistState() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(new XYSeries("Series"));
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(new XYSeriesRenderer());
        ChartComponent component = new ChartComponent(new LineChart(dataset, renderer));
        Form form = new Form();
        form.add(component);
        form.show();

        Transform transform = Transform.makeIdentity();
        transform.translate(5, 7);
        component.setTransform(transform);
        assertSame(transform, component.getTransform());
    }

    private static class RecordingChart extends AbstractChart {
        private Graphics lastGraphics;
        private Rectangle lastBounds;
        private int lastAbsX;
        private int lastAbsY;

        @Override
        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
            lastGraphics = canvas.g;
            lastBounds = canvas.bounds;
            lastAbsX = canvas.absoluteX;
            lastAbsY = canvas.absoluteY;
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, int seriesIndex, Paint paint) {
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public void drawSeries(Canvas canvas, Paint paint, List<Float> points, SimpleSeriesRenderer seriesRenderer, float yAxisValue,
                               int seriesIndex, int startIndex) {
        }

        @Override
        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point point) {
            return null;
        }

        @Override
        public String getChartType() {
            return "Recording";
        }
    }
}
