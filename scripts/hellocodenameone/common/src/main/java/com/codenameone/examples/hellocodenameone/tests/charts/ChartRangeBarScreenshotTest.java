package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.RangeCategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BarChart.Type;
import com.codename1.charts.views.RangeBarChart;

public class ChartRangeBarScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        RangeCategorySeries cycleA = new RangeCategorySeries("Cycle A");
        cycleA.add(15, 32);
        cycleA.add(20, 38);
        cycleA.add(18, 36);
        cycleA.add(22, 40);
        dataset.addSeries(cycleA.toXYSeries());

        RangeCategorySeries cycleB = new RangeCategorySeries("Cycle B");
        cycleB.add(8, 26);
        cycleB.add(12, 32);
        cycleB.add(14, 30);
        cycleB.add(11, 27);
        dataset.addSeries(cycleB.toXYSeries());

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer aR = new XYSeriesRenderer();
        aR.setColor(ColorUtil.rgb(0xff, 0x80, 0x33));
        renderer.addSeriesRenderer(aR);

        XYSeriesRenderer bR = new XYSeriesRenderer();
        bR.setColor(ColorUtil.rgb(0x33, 0xa9, 0xff));
        renderer.addSeriesRenderer(bR);

        return new RangeBarChart(dataset, renderer, Type.DEFAULT);
    }

    @Override
    protected String screenshotName() {
        return "chart-range-bar";
    }
}
