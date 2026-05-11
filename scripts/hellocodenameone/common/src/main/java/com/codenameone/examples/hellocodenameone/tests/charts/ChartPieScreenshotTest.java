package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.PieChart;

public class ChartPieScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        CategorySeries series = new CategorySeries("Tickets");
        series.add("New", 14);
        series.add("Open", 26);
        series.add("In progress", 38);
        series.add("Resolved", 22);

        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLabelsTextSize(22);
        renderer.setLegendTextSize(22);
        renderer.setShowLabels(true);
        renderer.setLabelsColor(ColorUtil.BLACK);

        int[] colors = new int[]{
                ColorUtil.rgb(0xef, 0x4f, 0x4f),
                ColorUtil.rgb(0xf2, 0xb1, 0x40),
                ColorUtil.rgb(0x47, 0xa1, 0xe0),
                ColorUtil.rgb(0x4d, 0xc6, 0x8f)
        };
        for (int color : colors) {
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(color);
            renderer.addSeriesRenderer(r);
        }
        return new PieChart(series, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-pie";
    }
}
