package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.DoughnutChart;

public class ChartDoughnutScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        MultipleCategorySeries dataset = new MultipleCategorySeries("Sales");
        dataset.add("2023",
                new String[]{"Online", "Retail", "Wholesale"},
                new double[]{40, 35, 25});
        dataset.add("2024",
                new String[]{"Online", "Retail", "Wholesale"},
                new double[]{55, 28, 17});

        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setLabelsColor(ColorUtil.BLACK);
        renderer.setShowLabels(true);

        int[] colors = new int[]{
                ColorUtil.rgb(0xb8, 0x40, 0xa6),
                ColorUtil.rgb(0x42, 0xa7, 0x6f),
                ColorUtil.rgb(0xe9, 0x6e, 0x33),
                ColorUtil.rgb(0x6c, 0x3a, 0xb6),
                ColorUtil.rgb(0x47, 0xa1, 0xe0),
                ColorUtil.rgb(0xf2, 0xb1, 0x40)
        };
        for (int color : colors) {
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(color);
            renderer.addSeriesRenderer(r);
        }

        // Use synthetic CategorySeries derived from dataset for renderer count
        // -- each DoughnutChart segment needs its own renderer instance.
        CategorySeries unused = new CategorySeries("colors");
        for (int color : colors) {
            unused.add("c", color);
        }

        return new DoughnutChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-doughnut";
    }
}
