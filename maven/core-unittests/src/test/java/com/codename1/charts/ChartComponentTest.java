package com.codename1.charts;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.ClickableArea;
import com.codename1.charts.views.XYChart;
import com.codename1.test.UITestBase;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartComponentTest extends UITestBase {
    @Test
    void constructorCopiesPanAndZoomSettingsFromXYChart() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setPanEnabled(true);
        renderer.setZoomEnabled(true, true);
        StubXYChart chart = new StubXYChart(renderer);

        ChartComponent component = new ChartComponent(chart);

        assertTrue(component.isPanEnabled());
        assertTrue(component.isZoomEnabled());
    }

    @Test
    void coordinateConversionsHonorCurrentTransform() throws Exception {
        RecordingChart chart = new RecordingChart();
        ChartComponent component = new PositionedChartComponent(chart, 5, 10);

        Transform translation = Transform.makeTranslation(3, 4);
        setCurrentTransform(component, translation);

        Point chartPoint = component.screenToChartCoord(20, 30);
        assertEquals(12f, chartPoint.getX());
        assertEquals(16f, chartPoint.getY());

        Point screenPoint = component.chartToScreenCoord(12, 16);
        assertEquals(20f, screenPoint.getX());
        assertEquals(30f, screenPoint.getY());
    }

    @Test
    void shapeConversionsTranslateBetweenSpaces() throws Exception {
        RecordingChart chart = new RecordingChart();
        ChartComponent component = new PositionedChartComponent(chart, 10, 15);

        Point expectedChartCorner = component.screenToChartCoord(20, 30);
        Shape chartShape = component.screenToChartShape(new Rectangle(20, 30, 40, 50));
        Rectangle chartBounds = chartShape.getBounds();
        assertEquals(Math.round(expectedChartCorner.getX()), chartBounds.getX());
        assertEquals(Math.round(expectedChartCorner.getY()), chartBounds.getY());

        Shape screenShape = component.chartToScreenShape(new Rectangle(10, 15, 40, 50));
        Rectangle screenBounds = screenShape.getBounds();
        Point expectedScreenCorner = component.chartToScreenCoord(10, 15);
        assertEquals(Math.round(expectedScreenCorner.getX()), screenBounds.getX());
        assertEquals(Math.round(expectedScreenCorner.getY()), screenBounds.getY());
    }

    @Test
    void pointerEventsInvokeSeriesCallbacksWhenSelectionExists() {
        RecordingChart chart = new RecordingChart();
        chart.selectionToReturn = new SeriesSelection(1, 2, 3, 4);
        TestChartComponent component = new TestChartComponent(chart);

        component.pointerPressed(5, 7);
        assertEquals(chart.selectionToReturn, component.lastPressedSelection);
        assertEquals(5f, chart.lastPoint.getX());
        assertEquals(7f, chart.lastPoint.getY());

        component.pointerReleased(5, 7);
        assertEquals(chart.selectionToReturn, component.lastReleasedSelection);
    }

    @Test
    void panConfigurationUpdatesRendererState() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        StubXYChart chart = new StubXYChart(renderer);
        ChartComponent component = new ChartComponent(chart);

        component.setPanEnabled(false);
        assertFalse(component.isPanEnabled());
        assertFalse(renderer.isPanEnabled());

        component.setPanEnabled(true, false);
        assertTrue(component.isPanEnabled());
        assertTrue(renderer.isPanXEnabled());
        assertFalse(renderer.isPanYEnabled());
    }

    @Test
    void panLimitsAreAppliedToRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        StubXYChart chart = new StubXYChart(renderer);
        ChartComponent component = new ChartComponent(chart);

        component.setPanLimits(1, 2, 3, 4);
        assertArrayEquals(new double[]{1, 2, 3, 4}, renderer.getPanLimits());

        component.clearPanLimits();
        assertNull(renderer.getPanLimits());
    }

    @Test
    void settingPanLimitsOnNonXYChartThrows() {
        ChartComponent component = new ChartComponent(new RecordingChart());
        assertThrows(RuntimeException.class, () -> component.setPanLimits(0, 1, 0, 1));
    }

    @Test
    void zoomConfigurationKeepsComponentFocusable() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        StubXYChart chart = new StubXYChart(renderer);
        ChartComponent component = new ChartComponent(chart);

        component.setZoomEnabled(true);
        assertTrue(component.isZoomEnabled());
        assertTrue(component.isFocusable());
        assertTrue(renderer.isZoomEnabled());

        component.setZoomEnabled(false, true);
        assertTrue(component.isZoomEnabled());
        assertTrue(renderer.isZoomYEnabled());
        assertFalse(renderer.isZoomXEnabled());
    }

    @Test
    void zoomLimitsRequireXYChart() {
        ChartComponent component = new ChartComponent(new RecordingChart());
        assertThrows(RuntimeException.class, () -> component.setZoomLimits(1, 2, 3, 4));
    }

    private void setCurrentTransform(ChartComponent component, Transform transform) throws Exception {
        Field field = ChartComponent.class.getDeclaredField("currentTransform");
        field.setAccessible(true);
        field.set(component, transform);
    }

    private static class RecordingChart extends AbstractChart {
        private SeriesSelection selectionToReturn;
        private Point lastPoint;

        @Override
        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
        }

        @Override
        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point screenPoint) {
            lastPoint = screenPoint;
            return selectionToReturn;
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, int seriesIndex, Paint paint) {
        }
    }

    private static class StubXYChart extends XYChart {
        StubXYChart(XYMultipleSeriesRenderer renderer) {
            super(new XYMultipleSeriesDataset(), renderer);
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
        protected ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values, float yAxisValue, int seriesIndex, int startIndex) {
            return new ClickableArea[0];
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public String getChartType() {
            return "Stub";
        }

        @Override
        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point screenPoint) {
            return null;
        }
    }

    private static class PositionedChartComponent extends ChartComponent {
        private final int absoluteX;
        private final int absoluteY;

        PositionedChartComponent(AbstractChart chart, int absoluteX, int absoluteY) {
            super(chart);
            this.absoluteX = absoluteX;
            this.absoluteY = absoluteY;
            setX(absoluteX);
            setY(absoluteY);
        }

        @Override
        public int getAbsoluteX() {
            return absoluteX;
        }

        @Override
        public int getAbsoluteY() {
            return absoluteY;
        }
    }

    private static class TestChartComponent extends ChartComponent {
        private SeriesSelection lastPressedSelection;
        private SeriesSelection lastReleasedSelection;

        TestChartComponent(AbstractChart chart) {
            super(chart);
        }

        @Override
        protected void seriesPressed(SeriesSelection sel) {
            lastPressedSelection = sel;
        }

        @Override
        protected void seriesReleased(SeriesSelection sel) {
            lastReleasedSelection = sel;
        }
    }
}
