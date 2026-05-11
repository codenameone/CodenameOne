package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.PointStyle;
import com.codename1.charts.views.ScatterChart;

public class ChartScatterScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        XYSeries cohortA = new XYSeries("Cohort A");
        cohortA.add(1, 14);
        cohortA.add(2, 16);
        cohortA.add(3, 11);
        cohortA.add(4, 19);
        cohortA.add(5, 15);
        cohortA.add(6, 22);
        cohortA.add(7, 18);
        cohortA.add(8, 24);
        dataset.addSeries(cohortA);

        XYSeries cohortB = new XYSeries("Cohort B");
        cohortB.add(1, 4);
        cohortB.add(2, 7);
        cohortB.add(3, 9);
        cohortB.add(4, 6);
        cohortB.add(5, 12);
        cohortB.add(6, 8);
        cohortB.add(7, 14);
        cohortB.add(8, 11);
        dataset.addSeries(cohortB);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer aR = new XYSeriesRenderer();
        aR.setColor(ColorUtil.rgb(0xed, 0x3f, 0x3f));
        aR.setPointStyle(PointStyle.CIRCLE);
        aR.setFillPoints(true);
        renderer.addSeriesRenderer(aR);

        XYSeriesRenderer bR = new XYSeriesRenderer();
        bR.setColor(ColorUtil.rgb(0x3f, 0x65, 0xed));
        bR.setPointStyle(PointStyle.SQUARE);
        bR.setFillPoints(true);
        renderer.addSeriesRenderer(bR);

        return new ScatterChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-scatter";
    }
}
