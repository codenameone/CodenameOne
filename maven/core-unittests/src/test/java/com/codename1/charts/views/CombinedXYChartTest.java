package com.codename1.charts.views;

import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class CombinedXYChartTest extends UITestBase {

    @FormTest
    public void testCombinedXYChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Data");
        series.add(1, 10);
        series.add(2, 20);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        XYSeriesRenderer r = new XYSeriesRenderer();
        renderer.addSeriesRenderer(r);

        // Use XYCombinedChartDef array as required by constructor
        CombinedXYChart.XYCombinedChartDef[] types = new CombinedXYChart.XYCombinedChartDef[] {
            new CombinedXYChart.XYCombinedChartDef(BarChart.TYPE, 0)
        };

        CombinedXYChart chart = new CombinedXYChart(dataset, renderer, types);

        Assertions.assertEquals("Combined", chart.getChartType());
    }
}
