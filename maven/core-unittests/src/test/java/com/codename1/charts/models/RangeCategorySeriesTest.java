package com.codename1.charts.models;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class RangeCategorySeriesTest extends UITestBase {

    @FormTest
    public void testRangeCategorySeries() {
        RangeCategorySeries series = new RangeCategorySeries("Test Series");
        Assertions.assertEquals("Test Series", series.getTitle());

        series.add(10.0, 20.0);
        series.add("Category1", 5.0, 15.0);

        Assertions.assertEquals(2, series.getItemCount());

        Assertions.assertEquals(10.0, series.getMinimumValue(0), 0.001);
        Assertions.assertEquals(20.0, series.getMaximumValue(0), 0.001);

        Assertions.assertEquals(5.0, series.getMinimumValue(1), 0.001);
        Assertions.assertEquals(15.0, series.getMaximumValue(1), 0.001);

        series.remove(0);
        Assertions.assertEquals(1, series.getItemCount());
        Assertions.assertEquals(5.0, series.getMinimumValue(0), 0.001);

        series.clear();
        Assertions.assertEquals(0, series.getItemCount());
    }

    @FormTest
    public void testToXYSeries() {
        RangeCategorySeries series = new RangeCategorySeries("Test Series");
        series.add(10.0, 20.0);
        series.add(30.0, 40.0);

        XYSeries xy = series.toXYSeries();
        Assertions.assertEquals("Test Series", xy.getTitle());
        // Each add in RangeCategorySeries adds 2 points in XYSeries (min and max)
        Assertions.assertEquals(4, xy.getItemCount());

        // Check values (implementation uses hack k+1 and k+1.000001)
        Assertions.assertEquals(1.0, xy.getX(0), 0.001);
        Assertions.assertEquals(10.0, xy.getY(0), 0.001);

        Assertions.assertEquals(1.000001, xy.getX(1), 0.001);
        Assertions.assertEquals(20.0, xy.getY(1), 0.001);

        Assertions.assertEquals(2.0, xy.getX(2), 0.001);
        Assertions.assertEquals(30.0, xy.getY(2), 0.001);
    }
}
