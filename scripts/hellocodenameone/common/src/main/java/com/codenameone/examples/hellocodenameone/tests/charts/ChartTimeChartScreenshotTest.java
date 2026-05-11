package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.TimeSeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.TimeChart;

import java.util.Date;

/// TimeChart with a deterministic series anchored at a fixed epoch -- avoids
/// using `new Date()` so the rendered axis labels match across runs.
public class ChartTimeChartScreenshotTest extends AbstractChartScreenshotTest {

    private static final long ANCHOR_EPOCH_MS = 1709251200000L; // 2024-03-01 UTC

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        TimeSeries series = new TimeSeries("Visits");
        long day = 24L * 60L * 60L * 1000L;
        double[] values = {120, 134, 142, 158, 145, 168, 180, 175, 192, 205};
        for (int i = 0; i < values.length; i++) {
            series.add(new Date(ANCHOR_EPOCH_MS + i * day), values[i]);
        }
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 80, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(ColorUtil.rgb(0x14, 0x71, 0xc4));
        seriesRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(seriesRenderer);

        return new TimeChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-time";
    }
}
