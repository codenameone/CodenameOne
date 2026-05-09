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
        series.add(1, 5, 10);
        series.add(2, 8, 18);
        series.add(3, 12, 9);
        series.add(4, 15, 24);
        series.add(5, 18, 15);
        series.add(6, 21, 30);
        series.add(7, 24, 12);
        series.add(8, 27, 26);
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
