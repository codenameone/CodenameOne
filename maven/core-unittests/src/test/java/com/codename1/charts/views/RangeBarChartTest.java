package com.codename1.charts.views;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.ui.Image;
import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;

public class RangeBarChartTest extends UITestBase {

    @FormTest
    public void testRangeBarChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series 1");

        series.add(1, 10); // Min
        series.add(1, 20); // Max
        series.add(2, 15);
        series.add(2, 25);

        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        XYSeriesRenderer r = new XYSeriesRenderer();
        renderer.addSeriesRenderer(r);

        RangeBarChart chart = new RangeBarChart(dataset, renderer, BarChart.Type.DEFAULT);

        Assertions.assertEquals(RangeBarChart.TYPE, chart.getChartType());

        // Test drawSeries
        Image img = Image.createImage(100, 100);
        Canvas canvas = new Canvas();
        canvas.g = img.getGraphics(); // Direct field access
        Paint paint = new Paint();

        List<Float> points = new ArrayList<>();
        points.add(10f); // xMin
        points.add(10f); // yMin
        points.add(20f); // xMax
        points.add(50f); // yMax

        chart.drawSeries(canvas, paint, points, r, 0, 0, 0);

        Assertions.assertEquals(0.5f, chart.getCoeficient(), 0.001f);
    }
}
