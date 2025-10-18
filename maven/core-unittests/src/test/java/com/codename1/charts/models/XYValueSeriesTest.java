package com.codename1.charts.models;

import com.codename1.charts.util.MathHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XYValueSeriesTest {
    @Test
    void addTracksMinAndMaxValues() {
        XYValueSeries series = new XYValueSeries("values");
        series.add(1.0, 2.0, 5.0);
        series.add(2.0, 3.0, 1.0);
        series.add(3.0, 4.0, 7.0);

        assertEquals(3, series.getItemCount());
        assertEquals(5.0, series.getValue(0));
        assertEquals(1.0, series.getMinValue());
        assertEquals(7.0, series.getMaxValue());
    }

    @Test
    void addWithoutExplicitValueDefaultsToZero() {
        XYValueSeries series = new XYValueSeries("values");
        series.add(1.0, 2.0);

        assertEquals(0.0, series.getValue(0));
        assertEquals(0.0, series.getMinValue());
        assertEquals(0.0, series.getMaxValue());
    }

    @Test
    void removeReinitializesRangeWhenRemovingExtremes() {
        XYValueSeries series = new XYValueSeries("values");
        series.add(1.0, 1.0, 5.0);
        series.add(2.0, 2.0, -3.0);
        series.add(3.0, 3.0, 2.0);

        series.remove(1);

        assertEquals(2, series.getItemCount());
        assertEquals(2.0, series.getMinValue());
        assertEquals(5.0, series.getMaxValue());
    }

    @Test
    void clearResetsRangeAndValues() {
        XYValueSeries series = new XYValueSeries("values");
        series.add(1.0, 1.0, 5.0);
        series.add(2.0, 2.0, -3.0);

        series.clear();

        assertEquals(0, series.getItemCount());
        assertEquals(MathHelper.NULL_VALUE, series.getMinValue());
        assertEquals(MathHelper.NULL_VALUE, series.getMaxValue());
    }
}
