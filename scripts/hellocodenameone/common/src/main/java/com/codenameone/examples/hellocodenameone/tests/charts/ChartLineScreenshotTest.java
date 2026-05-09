package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.LineChart;

/// Two-series LineChart with fixed deterministic data so the rendered
/// vertices, axis labels and legend are reproducible across platforms.
/// Drives the default ChartComponent.paint() path (no setTransform on the
/// component) so we have a baseline that catches regressions in the
/// no-transform branch of ChartComponent.paint -- the branch the platform
/// conjugation change does NOT touch.
public class ChartLineScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries north = new XYSeries("North");
        north.add(2018, 12);
        north.add(2019, 16);
        north.add(2020, 22);
        north.add(2021, 18);
        north.add(2022, 28);
        dataset.addSeries(north);

        XYSeries south = new XYSeries("South");
        south.add(2018, 8);
        south.add(2019, 11);
        south.add(2020, 13);
        south.add(2021, 16);
        south.add(2022, 19);
        dataset.addSeries(south);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setAxisTitleTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setXTitle("Year");
        renderer.setYTitle("Value");
        renderer.setXLabels(5);
        renderer.setYLabels(5);
        renderer.setShowGrid(true);

        XYSeriesRenderer northRenderer = new XYSeriesRenderer();
        northRenderer.setColor(ColorUtil.rgb(0x0a, 0x66, 0xff));
        northRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(northRenderer);

        XYSeriesRenderer southRenderer = new XYSeriesRenderer();
        southRenderer.setColor(ColorUtil.rgb(0xee, 0x4a, 0x4a));
        southRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(southRenderer);

        return new LineChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-line";
    }
}
