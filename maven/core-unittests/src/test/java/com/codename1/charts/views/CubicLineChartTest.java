package com.codename1.charts.views;

import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class CubicLineChartTest extends UITestBase {

    @FormTest
    public void testCubicLineChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Data");
        series.add(1, 10);
        series.add(2, 20);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

        CubicLineChart chart = new CubicLineChart(dataset, renderer, 0.5f);

        Assertions.assertEquals("Cubic", chart.getChartType());
    }
}
