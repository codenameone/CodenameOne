package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.BarChart.Type;

public class ChartBarScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        CategorySeries q1 = new CategorySeries("Q1");
        q1.add("Region 1", 32);
        q1.add("Region 2", 25);
        q1.add("Region 3", 18);
        q1.add("Region 4", 41);
        dataset.addSeries(q1.toXYSeries());

        CategorySeries q2 = new CategorySeries("Q2");
        q2.add("Region 1", 28);
        q2.add("Region 2", 30);
        q2.add("Region 3", 24);
        q2.add("Region 4", 36);
        dataset.addSeries(q2.toXYSeries());

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setBarSpacing(0.4);
        renderer.setShowGrid(true);
        renderer.setXTitle("Region");
        renderer.setYTitle("Value");

        SimpleSeriesRenderer r1 = new XYSeriesRenderer();
        r1.setColor(ColorUtil.rgb(0x2c, 0xa5, 0x80));
        renderer.addSeriesRenderer(r1);

        SimpleSeriesRenderer r2 = new XYSeriesRenderer();
        r2.setColor(ColorUtil.rgb(0xff, 0xa6, 0x2b));
        renderer.addSeriesRenderer(r2);

        return new BarChart(dataset, renderer, Type.DEFAULT);
    }

    @Override
    protected String screenshotName() {
        return "chart-bar";
    }
}
