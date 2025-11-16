package com.codename1.charts;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChartComponentInteractionTest extends UITestBase {

    @FormTest
    void chartComponentForwardsPointerEventsToSeries() {
        TrackingChart chart = new TrackingChart();
        ChartComponent component = new ChartComponent(chart) {
            protected void seriesPressed(SeriesSelection sel) {
                chart.markPressed(sel);
            }

            protected void seriesReleased(SeriesSelection sel) {
                chart.markReleased(sel);
            }
        };
        component.setName("chart");
        component.setPreferredW(120);
        component.setPreferredH(120);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, component);
        form.show();

        component.pointerPressed(5, 5);
        component.pointerReleased(5, 5);

        assertTrue(chart.pressed.get());
        assertTrue(chart.released.get());
        assertEquals("Counting", chart.getChartType());
    }

    @FormTest
    void chartComponentZoomTransitionsAndCoordinateTransforms() {
        TrackingChart chart = new TrackingChart();
        ChartComponent component = new ChartComponent(chart);
        component.setPreferredW(100);
        component.setPreferredH(80);
        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, component);
        form.show();

        component.zoomToShapeInChartCoords(new Rectangle(0, 0, 10, 10), 50);
        component.zoomTo(0, 5, 0, 5, 50);
        Point screen = component.chartToScreenCoord(5, 5);
        Point chartCoord = component.screenToChartCoord(screen.getX(), screen.getY());
        assertEquals(5, chartCoord.getX());
        assertEquals(5, chartCoord.getY());

        // paint invokes AbstractChart.draw through ChartUtil
        component.paint(component.getGraphics());
        assertEquals(1, chart.drawCount.get());
    }

    private static class TrackingChart extends AbstractChart {
        private final AtomicInteger drawCount = new AtomicInteger();
        private final AtomicBoolean pressed = new AtomicBoolean();
        private final AtomicBoolean released = new AtomicBoolean();

        void markPressed(SeriesSelection sel) {
            pressed.set(sel != null);
        }

        void markReleased(SeriesSelection sel) {
            released.set(sel != null);
        }

        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point point) {
            return new SeriesSelection(0, 0, point.getX(), point.getY());
        }

        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
            drawCount.incrementAndGet();
        }

        public int getLegendShapeWidth(int seriesIndex) {
            return 1;
        }

        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, Paint paint) {
            paint.setColor(0xffffff);
        }

        public String getChartType() {
            return "Counting";
        }
    }
}
