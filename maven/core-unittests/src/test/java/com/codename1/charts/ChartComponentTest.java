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
import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartComponentTest extends UITestBase {
    private TestCodenameOneImplementation testImplementation;

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

        Rectangle screenRect = new Rectangle(component.getAbsoluteX(), component.getAbsoluteY(), 40, 50);
        Shape chartShape = component.screenToChartShape(screenRect);
        Rectangle chartBounds = chartShape.getBounds();
        Shape roundTripScreenShape = component.chartToScreenShape(chartShape);
        Rectangle roundTripScreenBounds = roundTripScreenShape.getBounds();
        assertEquals(screenRect.getX(), roundTripScreenBounds.getX());
        assertEquals(screenRect.getY(), roundTripScreenBounds.getY());
        assertEquals(screenRect.getWidth(), chartBounds.getWidth());
        assertEquals(screenRect.getHeight(), chartBounds.getHeight());
        assertEquals(screenRect.getWidth(), roundTripScreenBounds.getWidth());
        assertEquals(screenRect.getHeight(), roundTripScreenBounds.getHeight());

        Rectangle chartRect = new Rectangle(0, 0, 40, 50);
        Rectangle screenBounds = component.chartToScreenShape(chartRect).getBounds();
        assertEquals(component.getAbsoluteX(), screenBounds.getX());
        assertEquals(component.getAbsoluteY(), screenBounds.getY());
        assertEquals(40, screenBounds.getWidth());
        assertEquals(50, screenBounds.getHeight());

        Shape roundTripChartShape = component.screenToChartShape(component.chartToScreenShape(chartRect));
        Rectangle roundTripChartBounds = roundTripChartShape.getBounds();
        assertEquals(chartRect.getX(), roundTripChartBounds.getX());
        assertEquals(chartRect.getY(), roundTripChartBounds.getY());
        assertEquals(chartRect.getWidth(), roundTripChartBounds.getWidth());
        assertEquals(chartRect.getHeight(), roundTripChartBounds.getHeight());
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

    /// Regression for [#1449](https://github.com/codenameone/CodenameOne/issues/1449)
    /// -- the 2015 reporter asked for a way to "freeze the y axis panning and
    /// only allow panning left to right" because vertical panning was
    /// disrupting the chart. The XYMultipleSeriesRenderer already exposes
    /// setPanEnabled(boolean,boolean); these tests pin down the end-to-end
    /// behaviour through ChartComponent.pointerDragged so the locked axis
    /// truly cannot drift during a drag.
    @Test
    void dragWithOnlyXPanEnabledLeavesYRangeUnchanged() {
        ChartComponent component = newPanTestChart(true /*x*/, false /*y*/);
        XYMultipleSeriesRenderer renderer = renderer(component);

        component.pointerDragged(new int[]{50}, new int[]{50});
        component.pointerDragged(new int[]{20}, new int[]{20});

        assertNotEquals(0.0, renderer.getXAxisMin(),
                "Pan on X should have moved the X axis range");
        assertNotEquals(10.0, renderer.getXAxisMax(),
                "Pan on X should have moved the X axis range");
        assertEquals(0.0, renderer.getYAxisMin(),
                "Pan on Y was disabled but the Y axis minimum drifted");
        assertEquals(10.0, renderer.getYAxisMax(),
                "Pan on Y was disabled but the Y axis maximum drifted");
    }

    @Test
    void dragWithOnlyYPanEnabledLeavesXRangeUnchanged() {
        ChartComponent component = newPanTestChart(false /*x*/, true /*y*/);
        XYMultipleSeriesRenderer renderer = renderer(component);

        component.pointerDragged(new int[]{50}, new int[]{50});
        component.pointerDragged(new int[]{20}, new int[]{20});

        assertEquals(0.0, renderer.getXAxisMin(),
                "Pan on X was disabled but the X axis minimum drifted");
        assertEquals(10.0, renderer.getXAxisMax(),
                "Pan on X was disabled but the X axis maximum drifted");
        assertNotEquals(0.0, renderer.getYAxisMin(),
                "Pan on Y should have moved the Y axis range");
        assertNotEquals(10.0, renderer.getYAxisMax(),
                "Pan on Y should have moved the Y axis range");
    }

    @Test
    void dragWithBothAxesPanEnabledMovesBoth() {
        ChartComponent component = newPanTestChart(true, true);
        XYMultipleSeriesRenderer renderer = renderer(component);

        component.pointerDragged(new int[]{50}, new int[]{50});
        component.pointerDragged(new int[]{20}, new int[]{20});

        assertNotEquals(0.0, renderer.getXAxisMin());
        assertNotEquals(10.0, renderer.getXAxisMax());
        assertNotEquals(0.0, renderer.getYAxisMin());
        assertNotEquals(10.0, renderer.getYAxisMax());
    }

    private static XYMultipleSeriesRenderer renderer(ChartComponent component) {
        return (XYMultipleSeriesRenderer) ((XYChart) component.getChart()).getRenderer();
    }

    /// Build a 100x100 ChartComponent at the origin with a zero-padding/margin
    /// renderer and an initial range of [0,10] on both axes. This gives the
    /// drag math a clean 1 chart-unit per 10 screen-pixel scale factor.
    private static ChartComponent newPanTestChart(boolean panX, boolean panY) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setMargins(new int[]{0, 0, 0, 0});
        renderer.setRange(new double[]{0, 10, 0, 10});
        StubXYChart chart = new StubXYChart(renderer);
        ChartComponent component = new PositionedChartComponent(chart, 0, 0);
        component.setWidth(100);
        component.setHeight(100);
        component.getAllStyles().setPadding(0, 0, 0, 0);
        // setPanEnabled(boolean) and the boolean-pair version both flow through
        // the renderer; call the per-axis version last so it wins.
        component.setPanEnabled(panX, panY);
        return component;
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
