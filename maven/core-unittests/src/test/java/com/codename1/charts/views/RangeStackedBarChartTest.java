package com.codename1.charts.views;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class RangeStackedBarChartTest extends UITestBase {

    @FormTest
    public void testGetChartType() {
        RangeStackedBarChart chart = new RangeStackedBarChart();
        Assertions.assertEquals("RangeStackedBar", chart.getChartType());
    }

}
