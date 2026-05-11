package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYValueSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BubbleChart;

public class ChartBubbleScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYValueSeries series = new XYValueSeries("Throughput");
        // Use explicit double literals for every argument: with bare int
        // literals Java resolves `series.add(1, 5, 10)` to the inherited
        // XYSeries.add(int index, double x, double y) signature (insert at
        // an explicit list index) instead of XYValueSeries' three-double
        // bubble add. The list is empty when the first call lands at index
        // 1, so that picks IndexOutOfBoundsException instead of the chart
        // we wanted.
        series.add(1d, 5d, 10d);
        series.add(2d, 8d, 18d);
        series.add(3d, 12d, 9d);
        series.add(4d, 15d, 24d);
        series.add(5d, 18d, 15d);
        series.add(6d, 21d, 30d);
        series.add(7d, 24d, 12d);
        series.add(8d, 27d, 26d);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer seriesR = new XYSeriesRenderer();
        seriesR.setColor(ColorUtil.argb(0xc0, 0x9a, 0x4d, 0xff));
        renderer.addSeriesRenderer(seriesR);

        return new BubbleChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-bubble";
    }
}
