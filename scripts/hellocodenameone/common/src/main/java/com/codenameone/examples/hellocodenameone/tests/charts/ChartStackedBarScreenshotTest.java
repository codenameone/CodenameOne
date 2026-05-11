package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.BarChart.Type;

/// BarChart in STACKED mode -- the bars are placed at the same X position so
/// the renderer's per-series x-position composition path differs from the
/// side-by-side DEFAULT bars.
public class ChartStackedBarScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        CategorySeries staff = new CategorySeries("Staff");
        staff.add("Mar", 24);
        staff.add("Apr", 30);
        staff.add("May", 28);
        staff.add("Jun", 33);
        dataset.addSeries(staff.toXYSeries());

        CategorySeries software = new CategorySeries("Software");
        software.add("Mar", 12);
        software.add("Apr", 14);
        software.add("May", 18);
        software.add("Jun", 22);
        dataset.addSeries(software.toXYSeries());

        CategorySeries hardware = new CategorySeries("Hardware");
        hardware.add("Mar", 8);
        hardware.add("Apr", 6);
        hardware.add("May", 10);
        hardware.add("Jun", 12);
        dataset.addSeries(hardware.toXYSeries());

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);
        renderer.setXTitle("Month");
        renderer.setYTitle("Cost");

        XYSeriesRenderer staffR = new XYSeriesRenderer();
        staffR.setColor(ColorUtil.rgb(0x6c, 0x3a, 0xb6));
        renderer.addSeriesRenderer(staffR);

        XYSeriesRenderer softwareR = new XYSeriesRenderer();
        softwareR.setColor(ColorUtil.rgb(0x42, 0xa7, 0x6f));
        renderer.addSeriesRenderer(softwareR);

        XYSeriesRenderer hardwareR = new XYSeriesRenderer();
        hardwareR.setColor(ColorUtil.rgb(0xe9, 0x6e, 0x33));
        renderer.addSeriesRenderer(hardwareR);

        return new BarChart(dataset, renderer, Type.STACKED);
    }

    @Override
    protected String screenshotName() {
        return "chart-bar-stacked";
    }
}
