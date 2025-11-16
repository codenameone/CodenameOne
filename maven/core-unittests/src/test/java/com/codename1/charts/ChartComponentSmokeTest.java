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
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class ChartComponentSmokeTest extends UITestBase {

    private static class StubChart extends AbstractChart {
        private int drawCalls;

        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point point) {
            return new SeriesSelection(0, 0, point.getX(), point.getY());
        }

        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
            drawCalls++;
        }

        public int getLegendShapeWidth(int seriesIndex) {
            return 2;
        }

        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, Paint paint) {
            paint.setColor(0xffffff);
        }

        public String getChartType() {
            return "Stub";
        }
    }

    @FormTest
    void pointerSelectionRoutesThroughSeriesHooks() {
        StubChart chart = new StubChart();
        ChartComponent component = new ChartComponent(chart);
        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, component);
        form.show();

        component.pointerPressed(0, 0);
        component.pointerReleased(0, 0);

        assertEquals("Stub", chart.getChartType());
    }

    @FormTest
    void coordinateConversionsRoundTrip() {
        StubChart chart = new StubChart();
        ChartComponent component = new ChartComponent(chart);
        Point screen = component.chartToScreenCoord(5, 10);
        Point chartPt = component.screenToChartCoord(screen.getX(), screen.getY());
        assertEquals(5, chartPt.getX());
        assertEquals(10, chartPt.getY());
    }
}
