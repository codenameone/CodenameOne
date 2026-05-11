package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.AreaSeries;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.RadarChart;

/// Mirrors the canonical RadarChartSample (two CategorySeries with five axes).
public class ChartRadarScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        AreaSeries dataset = new AreaSeries();

        CategorySeries may = new CategorySeries("May");
        may.add("Health", 0.8);
        may.add("Attack", 0.6);
        may.add("Defense", 0.4);
        may.add("Critical", 0.2);
        may.add("Speed", 1.0);
        dataset.addSeries(may);

        CategorySeries chang = new CategorySeries("Chang");
        chang.add("Health", 0.3);
        chang.add("Attack", 0.7);
        chang.add("Defense", 0.5);
        chang.add("Critical", 0.1);
        chang.add("Speed", 0.3);
        dataset.addSeries(chang);

        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLegendTextSize(22);
        renderer.setLabelsTextSize(20);
        renderer.setLabelsColor(ColorUtil.BLACK);
        renderer.setShowLabels(true);

        SimpleSeriesRenderer mayR = new SimpleSeriesRenderer();
        mayR.setColor(ColorUtil.MAGENTA);
        renderer.addSeriesRenderer(mayR);

        SimpleSeriesRenderer changR = new SimpleSeriesRenderer();
        changR.setColor(ColorUtil.CYAN);
        renderer.addSeriesRenderer(changR);

        return new RadarChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-radar";
    }
}
