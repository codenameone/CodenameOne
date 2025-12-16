package com.codename1.charts.views;

import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class ScatterChartTest extends UITestBase {

    @FormTest
    public void testScatterChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

        ScatterChart chart = new ScatterChart(dataset, renderer);

        Assertions.assertEquals("Scatter", chart.getChartType());

        // Coverage for other methods if any specific to ScatterChart
        // ScatterChart mostly inherits from XYChart, but implements getChartType and drawSeries
    }
}
