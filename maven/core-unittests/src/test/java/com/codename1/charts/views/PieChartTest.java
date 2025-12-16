package com.codename1.charts.views;

import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.models.CategorySeries;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class PieChartTest extends UITestBase {

    @FormTest
    public void testPieChart() {
        CategorySeries dataset = new CategorySeries("Series 1");
        dataset.add("Item 1", 10);
        dataset.add("Item 2", 20);

        DefaultRenderer renderer = new DefaultRenderer();

        PieChart chart = new PieChart(dataset, renderer);

        Assertions.assertNotNull(chart);
        Assertions.assertEquals(renderer, chart.getRenderer());
        Assertions.assertEquals(Integer.MAX_VALUE, chart.getCenterX());
        Assertions.assertEquals(Integer.MAX_VALUE, chart.getCenterY());

        chart.setCenterX(100);
        chart.setCenterY(100);
        Assertions.assertEquals(100, chart.getCenterX());
        Assertions.assertEquals(100, chart.getCenterY());

        // Removed call to getChartType() as PieChart (and RoundChart) does not implement it.
        // It is defined in XYChart, but PieChart extends RoundChart.
        // RoundChart does not seem to have this abstract method.
    }
}
