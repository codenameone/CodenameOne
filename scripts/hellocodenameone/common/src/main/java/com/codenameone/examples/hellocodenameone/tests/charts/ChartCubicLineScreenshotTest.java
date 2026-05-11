package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.CubicLineChart;

/// CubicLineChart smooths the line through the data points using cubic
/// interpolation -- different curve renderer than the LineChart test, so
/// regressions in the curve generation path are caught separately.
public class ChartCubicLineScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Latency");
        series.add(0, 12);
        series.add(1, 18);
        series.add(2, 14);
        series.add(3, 22);
        series.add(4, 20);
        series.add(5, 28);
        series.add(6, 18);
        series.add(7, 26);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);
        renderer.setXTitle("t");
        renderer.setYTitle("ms");

        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(ColorUtil.rgb(0x4f, 0xa8, 0x6e));
        seriesRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(seriesRenderer);

        return new CubicLineChart(dataset, renderer, 0.33f);
    }

    @Override
    protected String screenshotName() {
        return "chart-cubic-line";
    }
}
