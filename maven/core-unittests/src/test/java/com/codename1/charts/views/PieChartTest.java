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

        // PieChart does not implement getChartType() (inherits from RoundChart which doesn't enforce it)
        Assertions.assertNotNull(chart);
        // mDataset is protected in RoundChart, but no public getter. We can only verify constructor behavior indirectly or via renderer.
        Assertions.assertEquals(renderer, chart.getRenderer());
        Assertions.assertEquals(Integer.MAX_VALUE, chart.getCenterX());
        Assertions.assertEquals(Integer.MAX_VALUE, chart.getCenterY());

        chart.setCenterX(100);
        chart.setCenterY(100);
        Assertions.assertEquals(100, chart.getCenterX());
        Assertions.assertEquals(100, chart.getCenterY());

        // Verify segment shape (basic check)
        // getSegmentShape relies on internal mapper which might need drawing first or setup
        // But we can check it doesn't crash on invalid index or something if we didn't draw yet.
        // Actually PieMapper is initialized in constructor.
    }
}
