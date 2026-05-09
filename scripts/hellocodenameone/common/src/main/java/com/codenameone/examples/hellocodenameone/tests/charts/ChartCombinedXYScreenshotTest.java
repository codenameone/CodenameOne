package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.CombinedXYChart;
import com.codename1.charts.views.LineChart;
import com.codename1.charts.views.PointStyle;
import com.codename1.charts.views.ScatterChart;

/// CombinedXYChart layers BarChart, LineChart, and ScatterChart on the same
/// dataset axes -- exercises the multi-renderer dispatch in CombinedXYChart
/// where each child chart's draw is invoked in sequence with the same g
/// state.
public class ChartCombinedXYScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        XYSeries bars = new XYSeries("Bars");
        bars.add(1, 12);
        bars.add(2, 18);
        bars.add(3, 15);
        bars.add(4, 22);
        bars.add(5, 17);
        dataset.addSeries(bars);

        XYSeries trend = new XYSeries("Trend");
        trend.add(1, 14);
        trend.add(2, 16);
        trend.add(3, 19);
        trend.add(4, 20);
        trend.add(5, 23);
        dataset.addSeries(trend);

        XYSeries markers = new XYSeries("Markers");
        markers.add(1, 8);
        markers.add(2, 11);
        markers.add(3, 13);
        markers.add(4, 14);
        markers.add(5, 18);
        dataset.addSeries(markers);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer barR = new XYSeriesRenderer();
        barR.setColor(ColorUtil.rgb(0x6c, 0x3a, 0xb6));
        renderer.addSeriesRenderer(barR);

        XYSeriesRenderer trendR = new XYSeriesRenderer();
        trendR.setColor(ColorUtil.rgb(0xee, 0x4a, 0x4a));
        trendR.setLineWidth(3f);
        renderer.addSeriesRenderer(trendR);

        XYSeriesRenderer markersR = new XYSeriesRenderer();
        markersR.setColor(ColorUtil.rgb(0x42, 0xa7, 0x6f));
        // ScatterChart paths default the point style to PointStyle.POINT,
        // which routes through Canvas.drawPoint() -- the chart-package compat
        // shim explicitly throws "Not supported yet." there. CombinedXY
        // includes a Scatter chart def that paints on top of the line/bar
        // ones, so we'd hit the unimplemented drawPoint and the whole
        // suite hangs waiting for done(). Pick CIRCLE explicitly so the
        // marker layer renders with a real shape primitive.
        markersR.setPointStyle(PointStyle.CIRCLE);
        markersR.setFillPoints(true);
        renderer.addSeriesRenderer(markersR);

        // CombinedXYChart matches against AbstractChart.getChartType() which
        // returns the bare-type string ("Bar", "Line", "Scatter") -- using
        // BarChart.TYPE etc. avoids hard-coding a string we'd have to
        // remember to keep in sync.
        CombinedXYChart.XYCombinedChartDef[] chartDefs =
                new CombinedXYChart.XYCombinedChartDef[]{
                        new CombinedXYChart.XYCombinedChartDef(BarChart.TYPE, 0),
                        new CombinedXYChart.XYCombinedChartDef(LineChart.TYPE, 1),
                        new CombinedXYChart.XYCombinedChartDef(ScatterChart.TYPE, 2)
                };

        return new CombinedXYChart(dataset, renderer, chartDefs);
    }

    @Override
    protected String screenshotName() {
        return "chart-combined-xy";
    }
}
